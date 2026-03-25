package com.opencast.screencast.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 绑定设备请求（小程序端）
 */
@Data
@Schema(description = "绑定设备请求")
public class BindDeviceRequest {

    @Schema(description = "绑定码", required = true)
    @NotBlank(message = "绑定码不能为空")
    private String bindCode;

    @Schema(description = "设备Token")
    private String deviceToken;

    @Schema(description = "设备名称（可选，用于自定义设备名）")
    private String deviceName;
}
