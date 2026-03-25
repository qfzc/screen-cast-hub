package com.opencast.screencast.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.opencast.screencast.dto.auth.LoginRequest;
import com.opencast.screencast.dto.auth.LoginResponse;
import com.opencast.screencast.dto.auth.RegisterRequest;
import com.opencast.screencast.entity.User;
import com.opencast.screencast.mapper.UserMapper;
import com.opencast.screencast.common.exception.BusinessException;
import com.opencast.screencast.common.result.ErrorCode;
import com.opencast.screencast.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    @Value("${jwt.expiration:86400000}")
    private Long tokenExpiration;

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // 查找用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, request.getAccount())
                .or()
                .eq(User::getEmail, request.getAccount())
                .or()
                .eq(User::getName, request.getAccount());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 验证密码
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 生成 Token
        String token = jwtUtils.generateToken(user.getId(), user.getName());

        // 构建响应
        return buildLoginResponse(token, user);
    }

    /**
     * 用户注册
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        // 检查手机号是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, request.getPhone());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "手机号已注册");
        }

        // 创建用户
        User user = new User();
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setPassword(BCrypt.hashpw(request.getPassword()));
        user.setStatus(1);
        user.setDeleted(0);

        userMapper.insert(user);

        // 生成 Token
        String token = jwtUtils.generateToken(user.getId(), user.getName());

        return buildLoginResponse(token, user);
    }

    /**
     * 构建登录响应
     */
    private LoginResponse buildLoginResponse(String token, User user) {
        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        response.setExpiresIn(tokenExpiration / 1000);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setName(user.getName());
        userInfo.setPhone(user.getPhone());
        userInfo.setEmail(user.getEmail());
        userInfo.setAvatar(user.getAvatar());
        response.setUserInfo(userInfo);

        return response;
    }

    /**
     * 根据 Token 获取用户信息
     */
    public User getUserByToken(String token) {
        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        Long userId = jwtUtils.getUserId(token);
        return userMapper.selectById(userId);
    }

    /**
     * 根据用户ID获取用户信息
     */
    public LoginResponse.UserInfo getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setName(user.getName());
        userInfo.setPhone(user.getPhone());
        userInfo.setEmail(user.getEmail());
        userInfo.setAvatar(user.getAvatar());
        return userInfo;
    }
}
