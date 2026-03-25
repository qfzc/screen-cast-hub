package com.opencast.screencast.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import com.opencast.screencast.R;
import com.opencast.screencast.ScreencastApp;
import com.opencast.screencast.player.BannerViewPager;
import com.opencast.screencast.model.ApiResponse;
import com.opencast.screencast.model.Material;
import com.opencast.screencast.model.PublishTask;
import com.opencast.screencast.model.PublishTaskItem;
import com.opencast.screencast.mqtt.MqttManager;
import com.opencast.screencast.network.ApiService;
import com.opencast.screencast.network.RetrofitClient;
import com.opencast.screencast.player.PlayerManager;
import com.opencast.screencast.cache.MaterialCacheManager;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements MqttManager.OnTaskReceivedListener {
    private static final String TAG = "MainActivity";
    private static final int HEARTBEAT_INTERVAL = 30000; // 30 seconds

    private PlayerManager playerManager;
    private BannerViewPager bannerViewPager;
    private MqttManager mqttManager;
    private MaterialCacheManager cacheManager;
    private Handler heartbeatHandler = new Handler(Looper.getMainLooper());
    private Gson gson = new Gson();

    private ProgressBar progressBar;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hideSystemUI();
        initViews();
        checkDeviceBinding();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Window window = getWindow();
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                // Hide both status bar and navigation bar
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                // Set behavior to transient (user can swipe to see, then it auto-hides)
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // For older versions
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    private void initViews() {
        bannerViewPager = findViewById(R.id.banner_view_pager);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);

        cacheManager = new MaterialCacheManager(this);
        playerManager = new PlayerManager(this, bannerViewPager, cacheManager);
    }

    private void checkDeviceBinding() {
        String deviceToken = ScreencastApp.getInstance().getDeviceToken();

        if (deviceToken == null) {
            // 没有设备令牌，需要绑定
            goToBindActivity();
            return;
        }

        // 有设备令牌，需要向服务器验证绑定状态
        verifyBindStatusWithServer(deviceToken);
    }

    private void verifyBindStatusWithServer(String deviceToken) {
        statusText.setText(R.string.checking_bind_status);
        progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = RetrofitClient.getApiService();
        apiService.checkBindStatus(deviceToken).enqueue(new Callback<ApiResponse<ApiService.BindStatusResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiService.BindStatusResponse>> call,
                                   Response<ApiResponse<ApiService.BindStatusResponse>> response) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    if (response.isSuccessful() && response.body() != null) {
                        ApiService.BindStatusResponse status = response.body().getData();
                        if (status != null && status.isBound()) {
                            // 服务器确认已绑定，更新本地状态并进入主功能
                            ScreencastApp.getInstance().setBindStatus(true);
                            startMainFunctionality();
                        } else {
                            // 服务器显示未绑定，清除本地状态并跳转到绑定页面
                            Log.w(TAG, "Device unbound on server, clearing local status");
                            ScreencastApp.getInstance().clearBindStatus();
                            goToBindActivity();
                        }
                    } else {
                        // API 响应异常，检查本地缓存状态
                        Log.w(TAG, "Bind status check failed, falling back to local status");
                        if (ScreencastApp.getInstance().getBindStatus()) {
                            startMainFunctionality();
                        } else {
                            goToBindActivity();
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiService.BindStatusResponse>> call, Throwable t) {
                Log.e(TAG, "Failed to verify bind status", t);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    // 网络失败时，使用本地缓存状态（离线模式）
                    if (ScreencastApp.getInstance().getBindStatus()) {
                        Log.w(TAG, "Network failed, using cached bind status");
                        startMainFunctionality();
                    } else {
                        goToBindActivity();
                    }
                });
            }
        });
    }

    private void goToBindActivity() {
        startActivity(new Intent(this, BindActivity.class));
        finish();
    }

    private void startMainFunctionality() {
        statusText.setText(R.string.waiting_for_content);

        // Start heartbeat
        startHeartbeat();

        // Connect MQTT
        connectMqtt();

        // Load last task if exists
        loadLastTask();

        // Fallback: app重启或错过MQTT时，主动拉取当前任务
        fetchTasks();
    }

    private void loadLastTask() {
        String taskJson = ScreencastApp.getInstance().getLastTask();
        if (taskJson != null && !taskJson.isEmpty()) {
            try {
                PublishTask task = gson.fromJson(taskJson, PublishTask.class);
                boolean hasContent = task != null && task.getItems() != null && !task.getItems().isEmpty();
                if (hasContent) {
                    Log.d(TAG, "Loading last task: " + task.getTaskId());
                    onTaskReceived(task);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load last task", e);
            }
        }
    }

    private void startHeartbeat() {
        heartbeatHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendHeartbeat();
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL);
            }
        }, HEARTBEAT_INTERVAL);
    }

    private void sendHeartbeat() {
        String deviceToken = ScreencastApp.getInstance().getDeviceToken();
        if (deviceToken == null) return;

        ApiService apiService = RetrofitClient.getApiService();
        apiService.heartbeat(new ApiService.HeartbeatRequest(deviceToken))
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Log.d(TAG, "Heartbeat sent successfully");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Log.e(TAG, "Heartbeat failed", t);
                    }
                });
    }

    private void fetchTasks() {
        String deviceToken = ScreencastApp.getInstance().getDeviceToken();
        if (deviceToken == null) return;

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getDeviceTasks(deviceToken)
                .enqueue(new Callback<ApiResponse<List<PublishTask>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<PublishTask>>> call,
                                           Response<ApiResponse<List<PublishTask>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            List<PublishTask> tasks = response.body().getData();
                            if (tasks != null && !tasks.isEmpty()) {
                                handleTasks(tasks);
                            } else {
                                Log.d(TAG, "No active tasks returned from API");
                            }
                        } else {
                            Log.w(TAG, "Fetch tasks response invalid: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<PublishTask>>> call, Throwable t) {
                        Log.e(TAG, "Fetch tasks failed", t);
                    }
                });
    }

    private void handleTasks(List<PublishTask> tasks) {
        // 处理任务列表 - 使用第一个任务开始播放
        if (tasks != null && !tasks.isEmpty()) {
            PublishTask firstTask = tasks.get(0);
            Log.d(TAG, "Handling task from API: taskId=" + firstTask.getTaskId());
            onTaskReceived(firstTask);
        }
    }

    private void connectMqtt() {
        String deviceToken = ScreencastApp.getInstance().getDeviceToken();
        String mqttServer = "tcp://" + RetrofitClient.getBaseUrl()
                .replace("http://", "")
                .replace("https://", "")
                .replace(":8080", ":1883");

        mqttManager = new MqttManager(this, mqttServer);
        mqttManager.connect(deviceToken, this);
    }

    @Override
    public void onTaskReceived(PublishTask task) {
        Log.d(TAG, "Task received: batchId=" + task.getBatchId());

        // Save task to persistence
        try {
            String taskJson = gson.toJson(task);
            ScreencastApp.getInstance().saveLastTask(taskJson);
            Log.d(TAG, "Task saved to persistence");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save task", e);
        }

        // 立即清除当前播放（本地缓存不再全量清除，而是基于MD5和新任务进行清理）
        runOnUiThread(() -> {
            playerManager.clearPlaylist();
            bannerViewPager.setVisibility(View.GONE);
            statusText.setVisibility(View.VISIBLE);
            statusText.setText("正在准备最新素材...");
            progressBar.setVisibility(View.VISIBLE);
        });

        // 下载素材到本地（异步）
        cacheManager.downloadTaskMaterials(task, new MaterialCacheManager.Callback() {
            @Override
            public void onProgress(int current, int total) {
                runOnUiThread(() -> {
                    statusText.setText("正在下载新素材 (" + current + "/" + total + ")");
                });
            }

            @Override
            public void onComplete(List<Material> cachedMaterials) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    bannerViewPager.setVisibility(View.VISIBLE);
                    playMaterials(task, cachedMaterials);
                });
            }

            @Override
            public void onError(List<Material> failedMaterials) {
                // 部分失败时，仍然尝试播放（使用原URL）
                Log.w(TAG, "Some materials failed to download: " + failedMaterials.size());
                // 重新构建素材列表
                List<Material> materials = buildMaterialList(task);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    bannerViewPager.setVisibility(View.VISIBLE);
                    playMaterials(task, materials);
                });
            }
        });
    }

    /**
     * 构建素材列表
     */
    private List<Material> buildMaterialList(PublishTask task) {
        List<Material> materials = new ArrayList<>();

        if (task.getItems() != null && !task.getItems().isEmpty()) {
            for (PublishTaskItem item : task.getItems()) {
                materials.add(item.toMaterial());
            }
        }

        return materials;
    }

    /**
     * 播放素材
     */
    private void playMaterials(PublishTask task, List<Material> materials) {
        if (materials.isEmpty()) {
            Log.w(TAG, "No materials to play");
            return;
        }

        statusText.setVisibility(View.GONE);
        Log.d(TAG, "Starting playback with " + materials.size() + " materials");

        playerManager.setPlaylist(
                materials,
                task.getPlayMode(),
                task.getPlayInterval() != null ? task.getPlayInterval() : 5
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playerManager != null) {
            playerManager.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (playerManager != null) {
            playerManager.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        heartbeatHandler.removeCallbacksAndMessages(null);
        if (playerManager != null) {
            playerManager.release();
        }
        if (mqttManager != null) {
            mqttManager.disconnect();
        }
        if (cacheManager != null) {
            cacheManager.release();
        }
    }
}
