package com.opencast.screencast.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @Schema(description = "用户名/手机号/邮箱", required = true)
    @NotBlank(message = "账号不能为空")
    private String account;

    @Schema(description = "密码", required = true)
    @NotBlank(message = "密码不能为空")
    private String password;
}
