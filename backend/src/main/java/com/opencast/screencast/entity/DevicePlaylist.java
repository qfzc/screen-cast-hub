package com.opencast.screencast.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 设备播放列表
 */
@Data
@TableName("device_playlist")
public class DevicePlaylist {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long deviceId;
    
    /**
     * 播放模式: SEQUENCE-顺序播放, RANDOM-随机播放
     */
    private String playMode;
    
    /**
     * 播放间隔(秒)
     */
    private Integer playInterval;
    
    /**
     * 是否循环播放
     */
    private Boolean loopPlay;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
