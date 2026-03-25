package com.opencast.screencast.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 绑定状态响应
 */
@Data
@Schema(description = "绑定状态响应")
public class BindStatusResponse {

    @Schema(description = "绑定状态: 0未绑定 1已绑定 2离线")
    private Integer bindStatus;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "绑定时间")
    private LocalDateTime bindAt;
}
