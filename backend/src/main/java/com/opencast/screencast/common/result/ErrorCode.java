package com.opencast.screencast.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码定义
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用错误 4xx
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "方法不允许"),
    CONFLICT(409, "资源冲突"),
    PARAM_VALID_ERROR(422, "参数校验失败"),

    // 服务器错误 5xx
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // 业务错误 1xxxx
    // 设备相关 100xx
    DEVICE_NOT_FOUND(10001, "设备不存在"),
    DEVICE_NOT_BOUND(10002, "设备未绑定"),
    BIND_CODE_EXPIRED(10003, "绑定码已过期"),
    BIND_CODE_INVALID(10004, "绑定码无效"),
    DEVICE_ALREADY_BOUND(10005, "设备已被绑定"),
    DEVICE_OFFLINE(10006, "设备已离线"),

    // 素材相关 200xx
    FILE_FORMAT_NOT_SUPPORT(20001, "文件格式不支持"),
    FILE_SIZE_EXCEED(20002, "文件大小超限"),
    CONVERT_FAILED(20003, "转码失败"),
    MATERIAL_NOT_FOUND(20004, "素材不存在"),
    FILE_UPLOAD_FAILED(20005, "文件上传失败"),

    // 发布相关 300xx
    TASK_NOT_FOUND(30001, "任务不存在"),
    TASK_ALREADY_PUBLISHED(30002, "任务已发布"),
    TASK_ALREADY_COMPLETED(30003, "任务已完成"),
    TASK_CANCEL_FAILED(30004, "任务取消失败"),

    // 用户相关 400xx
    USER_NOT_FOUND(40001, "用户不存在"),
    USER_PASSWORD_ERROR(40002, "密码错误"),
    USER_DISABLED(40003, "用户已禁用"),
    USER_ALREADY_EXISTS(40004, "用户已存在"),
    LOGIN_EXPIRED(40005, "登录已过期"),
    TOKEN_INVALID(40006, "Token无效");

    private final Integer code;
    private final String message;
}
