package com.opencast.screencast.dto.publish;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 任务完成请求（TV端调用）
 */
@Data
@Schema(description = "任务完成请求")
public class TaskCompleteRequest {

    @NotBlank(message = "设备Token不能为空")
    @Schema(description = "设备Token", required = true)
    private String deviceToken;

    @Schema(description = "播放时长(秒)")
    private Integer playDuration;

    @Schema(description = "播放次数")
    private Integer playCount;
}
