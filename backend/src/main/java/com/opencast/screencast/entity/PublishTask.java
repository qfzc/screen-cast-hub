package com.opencast.screencast.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.opencast.screencast.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 发布任务实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("publish_task")
public class PublishTask extends BaseEntity {

    /**
     * 批次ID (UUID，用于标识同一次发布的任务)
     */
    private String batchId;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 创建用户ID
     */
    private Long userId;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 播放模式: SEQUENCE-顺序播放, RANDOM-随机播放
     */
    private String playMode;

    /**
     * 播放间隔(秒)
     */
    private Integer playInterval;

    /**
     * 循环播放
     */
    private Boolean loopPlay;

    /**
     * 自动播放
     */
    private Boolean autoPlay;

    /**
     * 定时发布时间
     */
    private LocalDateTime scheduledAt;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 开始播放时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 状态: 0待发布 1已发布 2播放中 3已完成 4已取消
     */
    private Integer status;
}
