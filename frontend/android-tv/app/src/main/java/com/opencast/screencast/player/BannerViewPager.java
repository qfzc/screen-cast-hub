package com.opencast.screencast.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

/**
 * 轮播 ViewPager - 支持自动播放和无限循环
 * 基于 ViewPager2 实现
 */
public class BannerViewPager extends FrameLayout {
    private static final String TAG = "BannerViewPager";

    private ViewPager2 viewPager;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoScrollRunnable;

    private int scrollInterval = 5000; // 默认 5 秒
    private boolean isAutoScroll = true;
    private boolean isTouching = false;
    private boolean isScrolling = false;  // 防止滚动过程中重复触发
    private long lastScrollStateChangeTime = 0;
    private static final int SCROLL_TIMEOUT = 3000; // 3秒超时保护
    private boolean isCurrentPageVideo = false;  // 标记当前页面是否为视频

    private BannerPageChangeListener pageChangeListener;

    public interface BannerPageChangeListener {
        void onPageSelected(int position);
        void onPageScrollStateChanged(int state);
    }

    public BannerViewPager(@NonNull Context context) {
        super(context);
        init();
    }

    public BannerViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BannerViewPager(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        viewPager = new ViewPager2(getContext());
        viewPager.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        // 设置页面切换动画
        viewPager.setPageTransformer(new BannerPageTransformer());

        // 页面切换监听
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                int realPosition = getAdapter() != null ? getAdapter().getRealCount() : 0;
                realPosition = realPosition > 0 ? position % realPosition : position;
                Log.d(TAG, "Page selected: position=" + position + ", realPosition=" + realPosition);
                if (pageChangeListener != null) {
                    pageChangeListener.onPageSelected(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                String stateStr = state == ViewPager2.SCROLL_STATE_IDLE ? "IDLE" :
                        state == ViewPager2.SCROLL_STATE_DRAGGING ? "DRAGGING" : "SETTLING";
                Log.d(TAG, "Scroll state changed: " + stateStr);

                // 更新滚动状态标志
                isScrolling = (state != ViewPager2.SCROLL_STATE_IDLE);
                lastScrollStateChangeTime = System.currentTimeMillis();

                if (pageChangeListener != null) {
                    pageChangeListener.onPageScrollStateChanged(state);
                }

                // 只在滚动完全停止时管理自动滚动
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    // 只有当前页面不是视频时才启动自动滚动
                    if (isAutoScroll && hasMultipleItems() && !isCurrentPageVideo) {
                        Log.d(TAG, "Starting auto-scroll from IDLE state");
                        startAutoScroll();
                    }
                } else if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    // 用户手动滑动时暂停自动滚动
                    stopAutoScroll();
                } else {
                    // SETTLING 状态或其他，添加超时保护
                    handler.postDelayed(() -> {
                        if (isScrolling && System.currentTimeMillis() - lastScrollStateChangeTime >= SCROLL_TIMEOUT) {
                            Log.w(TAG, "Scroll state stuck detected, forcing IDLE");
                            isScrolling = false;
                            if (isAutoScroll && hasMultipleItems() && !isCurrentPageVideo) {
                                startAutoScroll();
                            }
                        }
                    }, SCROLL_TIMEOUT);
                }
            }
        });

        addView(viewPager);
    }

    /**
     * 设置适配器
     */
    public void setAdapter(MediaPagerAdapter adapter) {
        viewPager.setAdapter(adapter);
        resetToMiddlePosition();
    }

    /**
     * 重置到中间位置，实现无限滚动
     * 当数据更新后调用此方法
     */
    public void resetToMiddlePosition() {
        MediaPagerAdapter adapter = getAdapter();
        if (adapter == null) {
            Log.w(TAG, "resetToMiddlePosition: adapter is null");
            return;
        }

        int realCount = adapter.getRealCount();
        boolean hasMultipleItems = realCount > 1;
        viewPager.setUserInputEnabled(hasMultipleItems);

        Log.d(TAG, "resetToMiddlePosition: realCount=" + realCount + ", hasMultipleItems=" + hasMultipleItems);

        // 设置初始位置到中间，实现"无限向前"效果
        if (hasMultipleItems) {
            int startPosition = Integer.MAX_VALUE / 2;
            // 确保起始位置对齐到第一个素材
            startPosition = startPosition - (startPosition % realCount);
            Log.d(TAG, "Setting initial position to: " + startPosition);
            viewPager.setCurrentItem(startPosition, false);
        } else {
            stopAutoScroll();
            viewPager.setCurrentItem(0, false);
        }
    }

    /**
     * 获取 ViewPager2 实例
     */
    public ViewPager2 getViewPager2() {
        return viewPager;
    }

    /**
     * 获取当前位置
     */
    public int getCurrentItem() {
        return viewPager.getCurrentItem();
    }

    /**
     * 设置当前位置
     */
    public void setCurrentItem(int item) {
        setCurrentItem(item, true);
    }

    public void setCurrentItem(int item, boolean smoothScroll) {
        viewPager.setCurrentItem(item, smoothScroll);
    }

    /**
     * 设置滚动间隔（毫秒）
     */
    public void setScrollInterval(int intervalMs) {
        this.scrollInterval = intervalMs;
    }

    /**
     * 设置是否自动滚动
     */
    public void setAutoScroll(boolean autoScroll) {
        this.isAutoScroll = autoScroll;
        if (autoScroll && hasMultipleItems()) {
            startAutoScroll();
        } else {
            stopAutoScroll();
        }
    }

    /**
     * 设置当前页面是否为视频
     * 视频页面不自动滚动，等视频播放完毕后再切换
     */
    public void setCurrentPageVideo(boolean isVideo) {
        this.isCurrentPageVideo = isVideo;
        if (isVideo) {
            stopAutoScroll();
        }
    }

    /**
     * 获取当前页面是否为视频
     */
    public boolean isCurrentPageVideo() {
        return isCurrentPageVideo;
    }

    /**
     * 开始自动滚动
     */
    public void startAutoScroll() {
        if (!isAutoScroll || !hasMultipleItems() || isCurrentPageVideo) {
            stopAutoScroll();
            return;
        }

        if (autoScrollRunnable != null) {
            return;
        }

        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!hasMultipleItems() || isCurrentPageVideo || !isAutoScroll) {
                    stopAutoScroll();
                    return;
                }

                // 如果正在滚动或用户正在触摸，跳过本次滚动
                if (isScrolling || isTouching) {
                    //Log.d(TAG, "Skipping auto-scroll: isScrolling=" + isScrolling + ", isTouching=" + isTouching);
                    handler.postDelayed(this, scrollInterval);
                    return;
                }

                int currentItem = viewPager.getCurrentItem();
                int nextItem = currentItem + 1;
                MediaPagerAdapter adapter = getAdapter();
                int realCount = adapter != null ? adapter.getRealCount() : 1;
                int nextRealPosition = nextItem % realCount;
                //Log.d(TAG, "Auto-scrolling: " + currentItem + " -> " + nextItem + " (real: " + nextRealPosition + ")");
                viewPager.setCurrentItem(nextItem, true);

                handler.postDelayed(this, scrollInterval);
            }
        };

        handler.postDelayed(autoScrollRunnable, scrollInterval);
    }

    /**
     * 停止自动滚动
     */
    public void stopAutoScroll() {
        if (autoScrollRunnable != null) {
            handler.removeCallbacks(autoScrollRunnable);
            autoScrollRunnable = null;
        }
    }

    /**
     * 滚动到下一页
     */
    public void scrollToNext() {
        if (!hasMultipleItems()) {
            return;
        }
        int currentItem = viewPager.getCurrentItem();
        viewPager.setCurrentItem(currentItem + 1, true);
    }

    /**
     * 滚动到上一页
     */
    public void scrollToPrevious() {
        if (!hasMultipleItems()) {
            return;
        }
        int currentItem = viewPager.getCurrentItem();
        viewPager.setCurrentItem(currentItem - 1, true);
    }

    /**
     * 设置页面切换监听
     */
    public void setPageChangeListener(BannerPageChangeListener listener) {
        this.pageChangeListener = listener;
    }

    /**
     * 设置页面切换动画类型
     */
    public void setPageTransformer(String transitionType) {
        boolean hasMultipleItems = hasMultipleItems();
        viewPager.setUserInputEnabled(hasMultipleItems);
        if (!hasMultipleItems || "NONE".equalsIgnoreCase(transitionType)) {
            viewPager.setPageTransformer(null);
            resetPageTransforms();
            return;
        }
        viewPager.setPageTransformer(new BannerPageTransformer(transitionType));
    }

    /**
     * 获取适配器
     */
    public MediaPagerAdapter getAdapter() {
        RecyclerView.Adapter<?> adapter = viewPager.getAdapter();
        return adapter instanceof MediaPagerAdapter ? (MediaPagerAdapter) adapter : null;
    }

    private boolean hasMultipleItems() {
        MediaPagerAdapter adapter = getAdapter();
        return adapter != null && adapter.getRealCount() > 1;
    }

    private void resetPageTransforms() {
        RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0);
        if (recyclerView == null) {
            return;
        }

        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            if (child == null) {
                continue;
            }
            child.setAlpha(1f);
            child.setTranslationX(0f);
            child.setTranslationY(0f);
            child.setScaleX(1f);
            child.setScaleY(1f);
            child.setRotation(0f);
            child.setRotationX(0f);
            child.setRotationY(0f);
        }
    }

    /**
     * 获取当前可见的 ViewHolder
     */
    public MediaPagerAdapter.MediaViewHolder getCurrentViewHolder() {
        MediaPagerAdapter adapter = getAdapter();
        if (adapter == null) {
            return null;
        }

        // ViewPager2 内部使用 RecyclerView，需要获取它
        RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0);
        if (recyclerView == null) {
            return null;
        }

        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(viewPager.getCurrentItem());
        if (holder instanceof MediaPagerAdapter.MediaViewHolder) {
            return (MediaPagerAdapter.MediaViewHolder) holder;
        }
        return null;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouching = true;
                stopAutoScroll();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTouching = false;
                // 只有当前页面不是视频时才恢复自动滚动
                if (isAutoScroll && hasMultipleItems() && !isCurrentPageVideo) {
                    startAutoScroll();
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAutoScroll();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isAutoScroll && hasMultipleItems()) {
            startAutoScroll();
        }
    }

    /**
     * 页面切换动画
     */
    private static class BannerPageTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private final String transitionType;

        BannerPageTransformer() {
            this("SLIDE");
        }

        BannerPageTransformer(String transitionType) {
            this.transitionType = transitionType != null ? transitionType : "SLIDE";
        }

        @Override
        public void transformPage(@NonNull View page, float position) {
            switch (transitionType) {
                case "SLIDE":
                    // 默认滑动效果，不需要特殊处理
                    break;
                case "FADE":
                    transformFade(page, position);
                    break;
                case "ZOOM":
                    transformZoom(page, position);
                    break;
                case "DEPTH":
                    transformDepth(page, position);
                    break;
                default:
                    // 默认滑动
                    break;
            }
        }

        private void transformFade(View page, float position) {
            if (position < -1 || position > 1) {
                page.setAlpha(0f);
            } else if (position <= 0) {
                page.setAlpha(1 + position);
                page.setTranslationX(-position * page.getWidth());
            } else {
                page.setAlpha(1 - position);
                page.setTranslationX(-position * page.getWidth());
            }
        }

        private void transformZoom(View page, float position) {
            if (position < -1 || position > 1) {
                page.setAlpha(0f);
            } else {
                float scale = Math.max(MIN_SCALE, 1 - Math.abs(position));
                page.setScaleX(scale);
                page.setScaleY(scale);
                page.setAlpha(scale);
            }
        }

        private void transformDepth(View page, float position) {
            if (position < -1) {
                page.setAlpha(0f);
            } else if (position <= 0) {
                page.setAlpha(1f);
                page.setTranslationX(0f);
                page.setScaleX(1f);
                page.setScaleY(1f);
            } else if (position <= 1) {
                page.setAlpha(1 - position);
                page.setTranslationX(-position * page.getWidth());
                float scale = MIN_SCALE + (1 - MIN_SCALE) * (1 - position);
                page.setScaleX(scale);
                page.setScaleY(scale);
            } else {
                page.setAlpha(0f);
            }
        }
    }
}
