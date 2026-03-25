package com.opencast.screencast.common.security;

/**
 * 用户主体信息
 */
public record UserPrincipal(Long userId, String username) {
}
