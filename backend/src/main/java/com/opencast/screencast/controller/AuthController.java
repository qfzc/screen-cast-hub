package com.opencast.screencast.controller;

import com.opencast.screencast.dto.auth.LoginRequest;
import com.opencast.screencast.dto.auth.LoginResponse;
import com.opencast.screencast.dto.auth.RegisterRequest;
import com.opencast.screencast.service.AuthService;
import com.opencast.screencast.common.result.Result;
import com.opencast.screencast.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Tag(name = "认证管理", description = "用户登录、注册、登出")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT 无状态，客户端删除 Token 即可
        return Result.success();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<LoginResponse.UserInfo> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        LoginResponse.UserInfo userInfo = authService.getUserInfo(principal.userId());
        return Result.success(userInfo);
    }
}
