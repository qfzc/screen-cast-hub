package com.opencast.screencast.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 生成绑定码请求（TV端）
 */
@Data
@Schema(description = "生成绑定码请求")
public class BindCodeRequest {

    @Schema(description = "设备唯一标识", required = true)
    @NotBlank(message = "设备标识不能为空")
    private String deviceToken;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备型号")
    private String model;

    @Schema(description = "系统版本")
    private String osVersion;

    @Schema(description = "APP版本")
    private String appVersion;
}
