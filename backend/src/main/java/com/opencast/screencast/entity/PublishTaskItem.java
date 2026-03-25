package com.opencast.screencast.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 发布任务明细实体
 */
@Data
@TableName("publish_task_item")
public class PublishTaskItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 素材ID
     */
    private Long materialId;

    /**
     * 播放顺序
     */
    private Integer sortOrder;

    /**
     * 播放时长(秒)
     */
    private Integer duration;

    /**
     * 平铺方式: FILL-填充, FIT-适应, ORIGINAL-原始, STRETCH-拉伸
     */
    private String fitMode;

    /**
     * 过渡效果: NONE-无, FADE-淡入淡出, SLIDE-滑动, CUBE-立方体
     */
    private String transition;

    /**
     * 状态: 0待播放 1播放中 2已完成
     */
    private Integer status;

    /**
     * 播放次数
     */
    private Integer playCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
