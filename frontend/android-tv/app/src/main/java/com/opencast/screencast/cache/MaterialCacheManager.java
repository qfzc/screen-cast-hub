package com.opencast.screencast.cache;

import android.content.Context;
import android.util.Log;

import com.opencast.screencast.ScreencastApp;
import com.opencast.screencast.model.Material;
import com.opencast.screencast.model.PublishTask;
import com.opencast.screencast.model.PublishTaskItem;
import com.opencast.screencast.network.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 素材缓存管理器
 * 负责下载媒体文件到本地存储、管理缓存路径映射、清理过期素材
 */
public class MaterialCacheManager {
    private static final String TAG = "MaterialCacheManager";
    private static final String CACHE_DIR = "materials";
    private static final String KEY_CACHE_MAP = "cache_map_json";

    private final Context context;
    private final ExecutorService executor;
    private final OkHttpClient okHttpClient;
    private final Gson gson;

    // 缓存路径映射: materialId -> localPath
    private final Map<String, String> cacheMap = new ConcurrentHashMap<>();

    public MaterialCacheManager(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        this.okHttpClient = new OkHttpClient();
        this.gson = new Gson();
        loadCacheMap();
    }

    /**
     * 下载回调接口
     */
    public interface Callback {
        void onProgress(int current, int total);
        void onComplete(List<Material> cachedMaterials);
        void onError(List<Material> failedMaterials);
    }

    /**
     * 批量下载任务素材
     */
    public void downloadTaskMaterials(PublishTask task, Callback callback) {
        executor.execute(() -> {
            List<Material> materials = new ArrayList<>();
            List<Material> failedMaterials = new ArrayList<>();

            // 收集所有素材
            if (task != null && task.getItems() != null && !task.getItems().isEmpty()) {
                for (PublishTaskItem item : task.getItems()) {
                    materials.add(item.toMaterial());
                }
            }

            if (materials.isEmpty()) {
                if (callback != null) {
                    callback.onComplete(materials);
                }
                return;
            }

            // 逐个下载素材
            List<Material> cachedMaterials = new ArrayList<>();
            int totalCount = materials.size();
            int currentCount = 0;
            for (Material material : materials) {
                if (material.getId() == null || material.getUrl() == null) {
                    cachedMaterials.add(material);
                    currentCount++;
                    if (callback != null) {
                        callback.onProgress(currentCount, totalCount);
                    }
                    continue;
                }

                String materialId = String.valueOf(material.getId());
                String localPath = downloadMaterial(material);

                if (localPath != null) {
                    material.setLocalPath(localPath);
                    cacheMap.put(materialId, localPath);
                    cachedMaterials.add(material);
                    Log.d(TAG, "Material cached: " + materialId + " -> " + localPath);
                } else {
                    failedMaterials.add(material);
                    cachedMaterials.add(material); // 仍然使用原URL播放
                    Log.w(TAG, "Material download failed: " + materialId);
                }

                currentCount++;
                if (callback != null) {
                    callback.onProgress(currentCount, totalCount);
                }
            }

            // 保存缓存映射
            saveCacheMap();

            // 清理旧素材
            Set<String> activeIds = new HashSet<>();
            for (Material m : materials) {
                if (m.getId() != null) {
                    activeIds.add(String.valueOf(m.getId()));
                }
            }
            cleanOldMaterials(activeIds);

            // 回调
            if (callback != null) {
                if (failedMaterials.isEmpty()) {
                    callback.onComplete(cachedMaterials);
                } else {
                    callback.onError(failedMaterials);
                }
            }
        });
    }

    /**
     * 下载单个素材
     * @return 本地文件路径，失败返回null
     */
    private String downloadMaterial(Material material) {
        String url = material.getUrl();
        if (!url.startsWith("http")) {
            url = RetrofitClient.getBaseUrl() + url;
        }

        String materialId = String.valueOf(material.getId());
        String md5 = material.getMd5();
        String extension = getFileExtension(url);
        
        String fileName;
        if (md5 != null && !md5.trim().isEmpty()) {
            fileName = materialId + "_" + md5 + extension;
        } else {
            fileName = materialId + extension;
        }
        
        File cacheFile = new File(getCacheDir(), fileName);

        // 如果已存在且大小相同，跳过下载
        if (cacheFile.exists() && cacheFile.length() > 0) {
            Log.d(TAG, "Material already cached (MD5 match): " + fileName);
            return cacheFile.getAbsolutePath();
        }

        // 下载前清理该素材的旧版本缓存
        File[] existingFiles = getCacheDir().listFiles();
        if (existingFiles != null) {
            for (File f : existingFiles) {
                if (materialId.equals(extractMaterialId(f.getName())) && !f.getName().equals(fileName)) {
                    if (f.delete()) {
                        Log.d(TAG, "Deleted old cached material: " + f.getName());
                    }
                }
            }
        }

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "Download failed: " + url + " code: " + response.code());
                return null;
            }

            try (InputStream is = response.body().byteStream();
                 FileOutputStream fos = new FileOutputStream(cacheFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.flush();
            }

            return cacheFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Download error: " + url, e);
            return null;
        }
    }

    /**
     * 获取本地缓存路径
     * 如果本地存在缓存返回本地路径，否则返回原URL
     */
    public String getLocalPath(Material material) {
        if (material == null || material.getId() == null) {
            return material != null ? material.getUrl() : null;
        }

        // 优先使用已设置的localPath
        if (material.getLocalPath() != null) {
            File file = new File(material.getLocalPath());
            if (file.exists()) {
                return material.getLocalPath();
            }
        }

        // 从缓存映射中查找
        String materialId = String.valueOf(material.getId());
        String cachedPath = cacheMap.get(materialId);
        if (cachedPath != null) {
            File file = new File(cachedPath);
            if (file.exists()) {
                return cachedPath;
            }
        }

        // 返回原URL
        return material.getUrl();
    }

    /**
     * 检查素材是否已缓存
     */
    public boolean isCached(Material material) {
        if (material == null || material.getId() == null) {
            return false;
        }
        String materialId = String.valueOf(material.getId());
        String cachedPath = cacheMap.get(materialId);
        return cachedPath != null && new File(cachedPath).exists();
    }

    /**
     * 清理不在任务中的旧素材
     */
    public void cleanOldMaterials(Set<String> activeMaterialIds) {
        File cacheDir = getCacheDir();
        if (!cacheDir.exists()) {
            return;
        }

        File[] files = cacheDir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            String fileName = file.getName();
            String materialId = extractMaterialId(fileName);
            if (materialId != null && !activeMaterialIds.contains(materialId)) {
                if (file.delete()) {
                    Log.d(TAG, "Cleaned old material: " + fileName);
                    cacheMap.remove(materialId);
                }
            }
        }

        saveCacheMap();
    }

    /**
     * 获取缓存目录
     */
    private File getCacheDir() {
        File cacheDir = new File(context.getFilesDir(), CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return cacheDir;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String url) {
        if (url == null) return "";

        int queryIndex = url.indexOf('?');
        if (queryIndex > 0) {
            url = url.substring(0, queryIndex);
        }

        int lastDot = url.lastIndexOf('.');
        if (lastDot > 0 && lastDot < url.length() - 1) {
            return url.substring(lastDot);
        }

        return "";
    }

    /**
     * 从文件名中提取素材ID
     */
    private String extractMaterialId(String fileName) {
        if (fileName == null) return null;
        
        int underscoreIndex = fileName.indexOf('_');
        int dotIndex = fileName.indexOf('.');
        
        if (underscoreIndex > 0) {
            return fileName.substring(0, underscoreIndex);
        } else if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return null;
    }

    /**
     * 保存缓存映射到SharedPreferences
     */
    private void saveCacheMap() {
        try {
            String json = gson.toJson(cacheMap);
            ScreencastApp.getInstance().getPreferences()
                    .edit()
                    .putString(KEY_CACHE_MAP, json)
                    .apply();
            Log.d(TAG, "Cache map saved: " + cacheMap.size() + " entries");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save cache map", e);
        }
    }

    /**
     * 从SharedPreferences加载缓存映射
     */
    @SuppressWarnings("unchecked")
    private void loadCacheMap() {
        try {
            String json = ScreencastApp.getInstance().getPreferences()
                    .getString(KEY_CACHE_MAP, "{}");
            Map<String, String> loaded = gson.fromJson(json,
                    new TypeToken<Map<String, String>>(){}.getType());
            if (loaded != null) {
                cacheMap.clear();
                cacheMap.putAll(loaded);
                Log.d(TAG, "Cache map loaded: " + cacheMap.size() + " entries");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load cache map", e);
        }
    }

    /**
     * 清空所有缓存
     */
    public void clearAllCache() {
        File cacheDir = getCacheDir();
        if (cacheDir.exists()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        cacheMap.clear();
        saveCacheMap();
        Log.d(TAG, "All cache cleared");
    }

    /**
     * 获取缓存大小（字节）
     */
    public long getCacheSize() {
        File cacheDir = getCacheDir();
        if (!cacheDir.exists()) {
            return 0;
        }

        long size = 0;
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                size += file.length();
            }
        }
        return size;
    }

    /**
     * 释放资源
     */
    public void release() {
        executor.shutdown();
    }
}
