package com.opencast.screencast.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.opencast.screencast.cache.MaterialCacheManager;
import com.opencast.screencast.model.Material;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 媒体内容适配器 - 支持 Image/Video/PDF 三种类型
 * 支持 PDF 分页播放 - 将 PDF展开为多页轮播項
 */
public class MediaPagerAdapter extends RecyclerView.Adapter<MediaPagerAdapter.MediaViewHolder> {

    private static final String TAG = "MediaPagerAdapter";

    private final Context context;
    private final List<DisplayItem> displayItems = new ArrayList<>();
    private final MaterialCacheManager cacheManager;
    private final ExecutorService renderExecutor = Executors.newFixedThreadPool(2);

    // 共享的 ExoPlayer 实例
    private ExoPlayer sharedExoPlayer;

    // 当前活跃的 ViewHolder
    private MediaViewHolder activeViewHolder = null;
    private int activePosition = -1;

    // 视频播放回调
    private VideoPlaybackCallback videoCallback;

    /**
     * 显示项 - 内部封装素材和具体页码
     */
    private static class DisplayItem {
        final Material material;
        final int pageIndex; // 0-based

        DisplayItem(Material material, int pageIndex) {
            this.material = material;
            this.pageIndex = pageIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DisplayItem that = (DisplayItem) o;
            return pageIndex == that.pageIndex && Objects.equals(material.getId(), that.material.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(material.getId(), pageIndex);
        }
    }

    public interface VideoPlaybackCallback {
        void onVideoPrepared();
        void onVideoEnded();
    }

    public MediaPagerAdapter(Context context, MaterialCacheManager cacheManager) {
        this.context = context;
        this.cacheManager = cacheManager;
        setHasStableIds(true);  // 启用稳定 ID，提高 RecyclerView 效率
    }

    /**
     * 设置素材列表 - 关键逻辑：如果是 PDF 则展开
     */
    public void setMaterials(List<Material> materials) {
        this.displayItems.clear();
        if (materials != null) {
            for (Material material : materials) {
                if (material.isPdf() && material.getPageCount() != null && material.getPageCount() > 1) {
                    // 如果是多页 PDF，展开为多项
                    for (int i = 0; i < material.getPageCount(); i++) {
                        displayItems.add(new DisplayItem(material, i));
                    }
                } else {
                    // 普通素材或其他情况，添加单项
                    displayItems.add(new DisplayItem(material, 0));
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * 设置共享的 ExoPlayer
     */
    public void setSharedExoPlayer(ExoPlayer exoPlayer) {
        this.sharedExoPlayer = exoPlayer;
    }

    /**
     * 设置视频播放回调
     */
    public void setVideoCallback(VideoPlaybackCallback callback) {
        this.videoCallback = callback;
    }

    /**
     * 获取真实数量
     */
    public int getRealCount() {
        return displayItems.size();
    }

    @Override
    public int getItemCount() {
        if (displayItems.size() <= 1) {
            return displayItems.size();
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public long getItemId(int position) {
        int realPosition = getRealPosition(position);
        if (realPosition >= 0 && realPosition < displayItems.size()) {
            DisplayItem item = displayItems.get(realPosition);
            // 组合 ID 以确保不同页码有不同 ID
            return (item.material.getId() << 16) | (item.pageIndex & 0xFFFF);
        }
        return RecyclerView.NO_ID;
    }

    @Override
    public int getItemViewType(int position) {
        int realPosition = getRealPosition(position);
        if (realPosition >= 0 && realPosition < displayItems.size()) {
            Material material = displayItems.get(realPosition).material;
            if (material.isImage()) return 0;
            if (material.isVideo()) return 1;
            if (material.isPdf()) return 2;
        }
        return 0;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FrameLayout container = new FrameLayout(context);
        container.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return new MediaViewHolder(container);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        int realPosition = getRealPosition(position);
        if (realPosition < 0 || realPosition >= displayItems.size()) {
            return;
        }

        DisplayItem item = displayItems.get(realPosition);
        holder.bind(item, realPosition);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull MediaViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        Log.d(TAG, "View attached: position=" + holder.getBindingAdapterPosition());
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull MediaViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        Log.d(TAG, "View detached: position=" + holder.getBindingAdapterPosition());

        // 当视图分离时，如果是视频则暂停
        if (holder == activeViewHolder && holder.playerView != null) {
            detachPlayerFromView(holder);
        }
    }

    @Override
    public void onViewRecycled(@NonNull MediaViewHolder holder) {
        super.onViewRecycled(holder);
        holder.recycle();
    }

    /**
     * 计算真实位置
     */
    private int getRealPosition(int position) {
        if (displayItems.isEmpty()) {
            return -1;
        }
        return position % displayItems.size();
    }

    /**
     * 获取当前位置的素材
     */
    public Material getMaterialAt(int position) {
        int realPosition = getRealPosition(position);
        if (realPosition >= 0 && realPosition < displayItems.size()) {
            return displayItems.get(realPosition).material;
        }
        return null;
    }

    /**
     * 当页面选中时调用 - 由 PlayerManager 调用
     */
    public void onPageSelected(int position, @Nullable MediaViewHolder holder) {
        int realPosition = getRealPosition(position);
        Log.d(TAG, "Page selected: position=" + position + ", realPosition=" + realPosition);

        // 如果之前有活跃的视频，停止它
        if (activeViewHolder != null && activeViewHolder != holder) {
            stopVideo(activeViewHolder);
        }

        activeViewHolder = holder;
        activePosition = realPosition;

        // 如果当前页面是视频，开始播放
        if (holder != null && holder.currentMaterial != null && holder.currentMaterial.isVideo()) {
            playVideo(holder);
        }
    }

    /**
     * 播放视频
     */
    private void playVideo(MediaViewHolder holder) {
        if (holder.playerView == null || sharedExoPlayer == null) {
            Log.w(TAG, "playVideo: playerView or sharedExoPlayer is null, skipping");
            return;
        }

        // 先停止并清理旧的播放状态
        sharedExoPlayer.stop();
        sharedExoPlayer.clearMediaItems();

        // 将 player 附加到当前 PlayerView
        holder.playerView.setPlayer(sharedExoPlayer);

        // 设置媒体路径
        String path = getMaterialPath(holder.currentMaterial);
        if (path == null) {
            Log.w(TAG, "playVideo: material path is null");
            return;
        }

        boolean isLocalFile = path.startsWith("/");
        Uri uri = isLocalFile ? Uri.fromFile(new File(path)) : Uri.parse(path);
        MediaItem mediaItem = MediaItem.fromUri(uri);
        sharedExoPlayer.setMediaItem(mediaItem);

        // 关键：延迟到 PlayerView 完成下一次布局后再 prepare/play
        // 确保 TextureView 的 SurfaceTexture 已经异步创建完毕，ExoPlayer 才能正确输出帧
        holder.playerView.post(() -> {
            // 安全检查：确保还是同一个 holder 处于活跃状态
            if (activeViewHolder != holder || sharedExoPlayer == null) {
                Log.w(TAG, "playVideo.post: holder changed or player released, aborting");
                return;
            }
            sharedExoPlayer.prepare();
            sharedExoPlayer.setPlayWhenReady(true);
            Log.d(TAG, "Video started (after surface ready): " + path);
        });
    }

    /**
     * 停止视频
     */
    private void stopVideo(MediaViewHolder holder) {
        if (holder.playerView != null && sharedExoPlayer != null) {
            sharedExoPlayer.stop();
            sharedExoPlayer.clearMediaItems();
            holder.playerView.setPlayer(null);
        }
    }

    /**
     * 从视图分离播放器
     */
    private void detachPlayerFromView(MediaViewHolder holder) {
        if (holder.playerView != null) {
            holder.playerView.setPlayer(null);
        }
    }

    /**
     * 获取素材路径
     */
    private String getMaterialPath(Material material) {
        return cacheManager != null ? cacheManager.getLocalPath(material) : material.getUrl();
    }

    /**
     * 停止所有播放
     */
    public void stopAll() {
        if (sharedExoPlayer != null) {
            sharedExoPlayer.stop();
        }
        if (activeViewHolder != null && activeViewHolder.playerView != null) {
            activeViewHolder.playerView.setPlayer(null);
        }
    }

    /**
     * 隐藏加载指示器
     */
    public void hideLoadingIndicator() {
        if (activeViewHolder != null && activeViewHolder.loadingIndicator != null) {
            activeViewHolder.loadingIndicator.setVisibility(View.GONE);
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        stopAll();
        displayItems.clear();
        activeViewHolder = null;
        notifyDataSetChanged();
    }

    /**
     * 媒体视图持有者
     */
    class MediaViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout container;
        private ImageView imageView;
        private PlayerView playerView;
        private ImageView pdfImageView;  // 使用 ImageView 渲染 PDF 页
        private ProgressBar loadingIndicator;

        private Material currentMaterial;
        private int currentPageIndex = 0;

        MediaViewHolder(@NonNull FrameLayout container) {
            super(container);
            this.container = container;
        }

        void bind(DisplayItem item, int position) {
            this.currentMaterial = item.material;
            this.currentPageIndex = item.pageIndex;
            container.removeAllViews();

            if (currentMaterial.isImage()) {
                bindImage(currentMaterial);
            } else if (currentMaterial.isVideo()) {
                bindVideo(currentMaterial);
            } else if (currentMaterial.isPdf()) {
                bindPdf(currentMaterial, currentPageIndex);
            }
        }

        void recycle() {
            container.removeAllViews();
            if (playerView != null) {
                playerView.setPlayer(null);
                playerView = null;  // 置空，下次 bindVideo 时全新创建，确保 TextureView Surface 生命周期干净
            }
            if (loadingIndicator != null) {
                loadingIndicator.setVisibility(View.GONE);
                loadingIndicator = null;  // 同步置空，跟随 playerView 重新创建
            }
            if (pdfImageView != null) {
                pdfImageView.setImageBitmap(null);
            }
            currentMaterial = null;
            currentPageIndex = 0;
        }

        private void bindImage(Material material) {
            if (imageView == null) {
                imageView = new ImageView(context);
                imageView.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
            }

            applyFitMode(imageView, material);

            // 修复：清除之前的 Glide 请求，防止 ViewHolder 复用时图片加载错乱
            Glide.with(context).clear(imageView);

            String path = getMaterialPath(material);
            boolean isLocalFile = path != null && path.startsWith("/");

            Log.d(TAG, "Loading image from: " + path);

            Glide.with(context)
                    .load(isLocalFile ? new File(path) : path)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Image load failed: " + path, e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource dataSource,
                                                       boolean isFirstResource) {
                            Log.d(TAG, "Image loaded: " + path);
                            return false;
                        }
                    })
                    .into(imageView);

            // 确保视图没有父容器再添加，防止 IllegalStateException
            if (imageView.getParent() != null) {
                ((ViewGroup) imageView.getParent()).removeView(imageView);
            }
            container.addView(imageView);
        }

        private void bindVideo(Material material) {
            if (playerView == null) {
                playerView = (PlayerView) android.view.LayoutInflater.from(context)
                        .inflate(com.opencast.screencast.R.layout.item_video_player, container, false);
            }

            applyFitMode(playerView, material);

            // 创建加载指示器
            if (loadingIndicator == null) {
                loadingIndicator = new ProgressBar(context);
                loadingIndicator.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER));
            }

            // 确保视图没有父容器再添加
            if (playerView.getParent() != null) {
                ((ViewGroup) playerView.getParent()).removeView(playerView);
            }
            if (loadingIndicator.getParent() != null) {
                ((ViewGroup) loadingIndicator.getParent()).removeView(loadingIndicator);
            }

            // 不在这里设置 player，等页面选中时再设置
            container.addView(playerView);
            container.addView(loadingIndicator);  // 添加加载指示器
            loadingIndicator.setVisibility(View.VISIBLE);  // 默认显示加载指示器
        }

        private void bindPdf(Material material, int pageIndex) {
            // 初始化 ImageView（复用同一个实例）
            if (pdfImageView == null) {
                pdfImageView = new ImageView(context);
                pdfImageView.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
            } else {
                pdfImageView.setImageBitmap(null); // 清除旧内容，防止闪烁
            }

            applyFitMode(pdfImageView, material);

            // 初始化加载指示器
            if (loadingIndicator == null) {
                loadingIndicator = new ProgressBar(context);
                loadingIndicator.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER));
            }

            if (pdfImageView.getParent() != null) {
                ((ViewGroup) pdfImageView.getParent()).removeView(pdfImageView);
            }
            if (loadingIndicator.getParent() != null) {
                ((ViewGroup) loadingIndicator.getParent()).removeView(loadingIndicator);
            }
            container.addView(pdfImageView);
            container.addView(loadingIndicator);
            loadingIndicator.setVisibility(View.VISIBLE);

            String path = getMaterialPath(material);
            if (path == null || !path.startsWith("/")) {
                // 远程文件，无法用 PdfRenderer，降级显示占位
                Log.e(TAG, "PDF path is remote or null, PdfRenderer requires a local file: " + path);
                loadingIndicator.setVisibility(View.GONE);
                return;
            }

            File pdfFile = new File(path);
            int screenWidth = container.getResources().getDisplayMetrics().widthPixels;
            int screenHeight = container.getResources().getDisplayMetrics().heightPixels;

            Log.d(TAG, "Rendering PDF page " + pageIndex + " from: " + path);

            // 在后台线程渲染，防止主线程阻塞
            renderExecutor.execute(() -> {
                Bitmap bitmap = null;
                try {
                    ParcelFileDescriptor fd = ParcelFileDescriptor.open(
                            pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
                    PdfRenderer renderer = new PdfRenderer(fd);

                    if (pageIndex < 0 || pageIndex >= renderer.getPageCount()) {
                        Log.e(TAG, "Invalid page index " + pageIndex + ", total pages: " + renderer.getPageCount());
                        renderer.close();
                        fd.close();
                        return;
                    }

                    PdfRenderer.Page page = renderer.openPage(pageIndex);

                    // 保持页面宽高比，根据适配模式缩放
                    float pageWidth = page.getWidth();
                    float pageHeight = page.getHeight();
                    float scaleW = screenWidth / pageWidth;
                    float scaleH = screenHeight / pageHeight;

                    float scale;
                    String fitMode = material != null ? material.getFitMode() : "FIT";
                    if (fitMode == null) fitMode = "FIT";

                    switch (fitMode.toUpperCase()) {
                        case "FILL":
                            scale = Math.max(scaleW, scaleH);
                            break;
                        case "STRETCH":
                            // 拉伸模式下，宽度和高度独立缩放
                            scale = 1.0f; // 后面直接用 scaleW, scaleH
                            break;
                        case "ORIGINAL":
                            scale = 1.0f;
                            break;
                        case "FIT":
                        default:
                            scale = Math.min(scaleW, scaleH);
                            break;
                    }

                    int bitmapWidth;
                    int bitmapHeight;

                    if ("STRETCH".equalsIgnoreCase(fitMode)) {
                        bitmapWidth = screenWidth;
                        bitmapHeight = screenHeight;
                    } else {
                        bitmapWidth = Math.round(pageWidth * scale);
                        bitmapHeight = Math.round(pageHeight * scale);
                    }

                    bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
                    bitmap.eraseColor(android.graphics.Color.WHITE); // 白色背景
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    page.close();
                    renderer.close();
                    fd.close();

                    Log.d(TAG, "PDF page " + pageIndex + " rendered: " + bitmapWidth + "x" + bitmapHeight);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to render PDF page " + pageIndex, e);
                }

                final Bitmap finalBitmap = bitmap;
                // 切回主线程更新 UI
                container.post(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    if (finalBitmap != null) {
                        pdfImageView.setImageBitmap(finalBitmap);
                    } else {
                        Log.e(TAG, "Bitmap is null, PDF page render failed");
                    }
                });
            });
        }

        /**
         * 应用适配模式
         */
        private void applyFitMode(View view, Material material) {
            String fitMode = material != null ? material.getFitMode() : "FIT";
            if (fitMode == null) fitMode = "FIT";

            if (view instanceof ImageView) {
                ImageView imageView = (ImageView) view;
                switch (fitMode.toUpperCase()) {
                    case "FILL":
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        break;
                    case "FIT":
                        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        break;
                    case "STRETCH":
                        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                        break;
                    case "ORIGINAL":
                        imageView.setScaleType(ImageView.ScaleType.CENTER);
                        break;
                    default:
                        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        break;
                }
            } else if (view instanceof PlayerView) {
                PlayerView playerView = (PlayerView) view;
                switch (fitMode.toUpperCase()) {
                    case "FILL":
                        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                        break;
                    case "FIT":
                    case "ORIGINAL": // 视频原始通常指 FIT
                        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                        break;
                    case "STRETCH":
                        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
                        break;
                    default:
                        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                        break;
                }
            }
        }
    }
}
