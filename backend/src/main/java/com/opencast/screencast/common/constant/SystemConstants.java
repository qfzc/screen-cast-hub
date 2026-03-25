package com.opencast.screencast.common.constant;

/**
 * 系统常量
 */
public interface SystemConstants {

    /**
     * 默认页码
     */
    long DEFAULT_PAGE = 1L;

    /**
     * 默认每页大小
     */
    long DEFAULT_SIZE = 20L;

    /**
     * 最大每页大小
     */
    long MAX_SIZE = 100L;

    /**
     * 绑定码有效期（分钟）
     */
    int BIND_CODE_EXPIRE_MINUTES = 10;

    /**
     * Token前缀
     */
    String TOKEN_PREFIX = "Bearer ";

    /**
     * 请求头Authorization
     */
    String HEADER_AUTHORIZATION = "Authorization";

    /**
     * 用户ID请求头
     */
    String HEADER_USER_ID = "X-User-Id";

    /**
     * 设备Token请求头
     */
    String HEADER_DEVICE_TOKEN = "X-Device-Token";
}
