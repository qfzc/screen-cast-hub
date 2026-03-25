package com.opencast.screencast.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备列表视图对象
 */
@Data
@Schema(description = "设备信息")
public class DeviceVO {

    @Schema(description = "设备ID")
    private Long id;

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "设备型号")
    private String model;

    @Schema(description = "设备唯一标识")
    private String deviceToken;

    @Schema(description = "绑定码")
    private String bindCode;

    @Schema(description = "状态: 0未绑定 1已绑定 2离线")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "最后在线时间")
    private LocalDateTime onlineAt;

    @Schema(description = "已用存储(字节)")
    private Long storageUsed;

    @Schema(description = "总存储(字节)")
    private Long storageTotal;

    @Schema(description = "当前播放内容")
    private String playingContent;

    @Schema(description = "分组ID")
    private Long groupId;

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "绑定时间")
    private LocalDateTime bindAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "APP版本")
    private String appVersion;
}
