package com.opencast.screencast.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.opencast.screencast.cache.MaterialCacheManager;
import com.opencast.screencast.model.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 播放管理器 - 基于 ViewPager2 实现轮播播放
 */
public class PlayerManager implements Player.Listener {
    private static final String TAG = "PlayerManager";
    private static final int DEFAULT_IMAGE_INTERVAL = 5000; // 5 seconds
    private static final int VIDEO_LOADING_TIMEOUT = 30000; // 30秒超时

    private final Context context;
    private final BannerViewPager bannerViewPager;
    private final MaterialCacheManager cacheManager;

    private MediaPagerAdapter adapter;
    private ExoPlayer exoPlayer;

    private List<Material> playlist = new ArrayList<>();
    private String playMode = "SEQUENCE";
    private int interval = DEFAULT_IMAGE_INTERVAL;
    private String defaultTransition = "SLIDE";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPlaying = false;
    private int currentPosition = 0;
    private Runnable loadingTimeoutRunnable;  // 加载超时保护

    public PlayerManager(Context context, BannerViewPager bannerViewPager, MaterialCacheManager cacheManager) {
        this.context = context;
        this.bannerViewPager = bannerViewPager;
        this.cacheManager = cacheManager;

        initExoPlayer();
        initAdapter();
    }

    private void initExoPlayer() {
        exoPlayer = new ExoPlayer.Builder(context).build();
        exoPlayer.addListener(this);
    }

    private void initAdapter() {
        adapter = new MediaPagerAdapter(context, cacheManager);
        adapter.setSharedExoPlayer(exoPlayer);
        bannerViewPager.setAdapter(adapter);

        // 设置页面切换监听
        bannerViewPager.setPageChangeListener(new BannerViewPager.BannerPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                handlePageSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // 可以在这里处理滚动状态变化
            }
        });
    }

    /**
     * 设置播放列表
     */
    public void setPlaylist(List<Material> materials, String playMode, int interval) {
        setPlaylist(materials, playMode, interval, "SLIDE");
    }

    /**
     * 设置播放列表
     */
    public void setPlaylist(List<Material> materials, String playMode, int interval, String transition) {
        this.playlist = new ArrayList<>(materials);
        this.playMode = playMode != null ? playMode : "SEQUENCE";
        this.interval = interval * 1000;
        this.defaultTransition = transition != null ? transition : "SLIDE";

        if ("RANDOM".equals(this.playMode)) {
            Collections.shuffle(this.playlist);
        }

        // 更新适配器并重置到中间位置，实现无限滚动
        adapter.setMaterials(this.playlist);
        bannerViewPager.resetToMiddlePosition();

        // 设置滚动间隔和动画
        bannerViewPager.setScrollInterval(this.interval);
        bannerViewPager.setPageTransformer(hasMultipleMaterials() ? this.defaultTransition : "NONE");

        // 开始播放
        startPlayback();
    }

    /**
     * 开始播放
     */
    private void startPlayback() {
        if (playlist.isEmpty()) {
            Log.w(TAG, "Playlist is empty");
            return;
        }

        isPlaying = true;
        boolean canAutoScroll = hasMultipleMaterials();
        bannerViewPager.setAutoScroll(canAutoScroll);

        Log.d(TAG, "Started playback with " + playlist.size() + " materials");

        // 处理第一个页面
        handler.postDelayed(() -> {
            int position = bannerViewPager.getCurrentItem();
            handlePageSelected(position);
        }, 500);
    }

    /**
     * 处理页面选中
     */
    private void handlePageSelected(int position) {
        currentPosition = position;
        Material material = adapter.getMaterialAt(position);

        if (material == null) {
            return;
        }

        Log.d(TAG, "Page selected: " + position + ", material: " + material.getName() + ", type: " + material.getType());

        // 先停止自动滚动，防止在判断素材类型期间触发切换
        bannerViewPager.stopAutoScroll();

        // 根据素材类型设置自动滚动行为
        if (material.isVideo()) {
            bannerViewPager.setCurrentPageVideo(true);
        } else {
            bannerViewPager.setCurrentPageVideo(false);
            // 非视频素材，启动自动滚动
            bannerViewPager.startAutoScroll();
        }

        // 获取当前可见的 ViewHolder，延迟一帧以确保 RecyclerView 完成了布局动画并且可以正确 findViewHolder
        handler.post(() -> {
            if (currentPosition != position) return; // 防止快速滑动导致的乱序
            MediaPagerAdapter.MediaViewHolder holder = bannerViewPager.getCurrentViewHolder();
            // 通知适配器页面已选中，开始视频播放
            adapter.onPageSelected(position, holder);
        });
    }

    /**
     * ExoPlayer 播放状态回调
     */
    @Override
    public void onPlaybackStateChanged(int playbackState) {
        switch (playbackState) {
            case Player.STATE_BUFFERING:
                Log.d(TAG, "Video buffering...");
                // 启动加载超时保护
                startLoadingTimeout();
                break;

            case Player.STATE_READY:
                Log.d(TAG, "Video ready to play");
                // 取消超时保护
                cancelLoadingTimeout();
                // 通知适配器隐藏加载指示器
                adapter.hideLoadingIndicator();
                break;

            case Player.STATE_ENDED:
                Log.d(TAG, "Video ended, scrolling to next immediately");
                cancelLoadingTimeout();
                // 视频播放完毕，稍微缓冲后立即切换到下一页
                handler.postDelayed(() -> {
                    if (!isPlaying || !hasMultipleMaterials()) {
                        return;
                    }
                    bannerViewPager.scrollToNext();
                    // 滚动完成后会触发 handlePageSelected，那里会决定是否启动自动滚动
                }, 500);
                break;

            case Player.STATE_IDLE:
                cancelLoadingTimeout();
                break;
        }
    }

    /**
     * 启动加载超时保护
     */
    private void startLoadingTimeout() {
        cancelLoadingTimeout();
        loadingTimeoutRunnable = () -> {
            Log.w(TAG, "Video loading timeout, skipping to next");
            adapter.hideLoadingIndicator();
            bannerViewPager.scrollToNext();
        };
        handler.postDelayed(loadingTimeoutRunnable, VIDEO_LOADING_TIMEOUT);
    }

    /**
     * 取消加载超时保护
     */
    private void cancelLoadingTimeout() {
        if (loadingTimeoutRunnable != null) {
            handler.removeCallbacks(loadingTimeoutRunnable);
            loadingTimeoutRunnable = null;
        }
    }

    /**
     * 清空并停止播放
     */
    public void clearPlaylist() {
        Log.d(TAG, "Clearing playlist and stopping playback");
        isPlaying = false;
        bannerViewPager.stopAutoScroll();
        if (exoPlayer != null) {
            exoPlayer.stop();
        }
        playlist = new ArrayList<>();
        if (adapter != null) {
            adapter.setMaterials(playlist);
            adapter.stopAll();
        }
    }

    /**
     * 暂停播放
     */
    public void pause() {
        isPlaying = false;
        bannerViewPager.stopAutoScroll();
        if (exoPlayer != null) {
            exoPlayer.pause();
        }
    }

    /**
     * 恢复播放
     */
    public void resume() {
        if (!playlist.isEmpty() && !isPlaying) {
            isPlaying = true;
            bannerViewPager.setAutoScroll(hasMultipleMaterials());

            // 处理当前页面
            handlePageSelected(bannerViewPager.getCurrentItem());
        }
    }

    private boolean hasMultipleMaterials() {
        return adapter != null && adapter.getRealCount() > 1;
    }

    /**
     * 释放资源
     */
    public void release() {
        cancelLoadingTimeout();
        handler.removeCallbacksAndMessages(null);
        isPlaying = false;
        bannerViewPager.stopAutoScroll();
        adapter.release();

        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}
