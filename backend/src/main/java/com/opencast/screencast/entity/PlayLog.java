package com.opencast.screencast.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 播放日志实体
 */
@Data
@TableName("play_log")
public class PlayLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 素材ID
     */
    private Long materialId;

    /**
     * 素材名称
     */
    private String materialName;

    /**
     * 播放时间
     */
    private LocalDateTime playAt;

    /**
     * 播放时长(秒)
     */
    private Integer playDuration;

    /**
     * 播放次数
     */
    private Integer playCount;
}
