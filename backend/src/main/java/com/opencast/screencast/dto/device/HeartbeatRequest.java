package com.opencast.screencast.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 设备心跳请求
 */
@Data
@Schema(description = "设备心跳请求")
public class HeartbeatRequest {

    @Schema(description = "设备唯一标识", required = true)
    @NotBlank(message = "设备标识不能为空")
    private String deviceToken;

    @Schema(description = "已用存储(字节)")
    private Long storageUsed;

    @Schema(description = "总存储(字节)")
    private Long storageTotal;

    @Schema(description = "当前播放内容")
    private String playingContent;

    @Schema(description = "网络类型")
    private String networkType;
}
