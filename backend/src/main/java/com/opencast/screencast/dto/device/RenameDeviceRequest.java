package com.opencast.screencast.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 重命名设备请求
 */
@Data
@Schema(description = "重命名设备请求")
public class RenameDeviceRequest {

    @Schema(description = "新名称", required = true)
    @NotBlank(message = "设备名称不能为空")
    private String name;
}
