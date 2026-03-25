package com.opencast.screencast.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 播放列表项
 */
@Data
@TableName("playlist_item")
public class PlaylistItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long playlistId;
    
    private Long materialId;
    
    /**
     * 排序序号
     */
    private Integer sortOrder;
    
    /**
     * 平铺方式: FILL-填充, FIT-适应, ORIGINAL-原始, STRETCH-拉伸
     */
    private String fitMode;
    
    /**
     * 播放时长(秒) - 图片/PDF专用
     */
    private Integer duration;
    
    /**
     * 过渡效果: NONE-无, FADE-淡入淡出, SLIDE-滑动
     */
    private String transition;
}
