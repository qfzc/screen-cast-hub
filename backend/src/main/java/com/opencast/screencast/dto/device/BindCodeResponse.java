package com.opencast.screencast.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 绑定码响应
 */
@Data
@Schema(description = "绑定码响应")
public class BindCodeResponse {

    @Schema(description = "绑定码")
    private String bindCode;

    @Schema(description = "二维码URL")
    private String qrCodeUrl;

    @Schema(description = "过期时间")
    private LocalDateTime expireAt;

    @Schema(description = "设备ID")
    private Long deviceId;
}
