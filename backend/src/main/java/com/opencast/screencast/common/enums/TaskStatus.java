package com.opencast.screencast.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 发布任务状态枚举
 */
@Getter
@AllArgsConstructor
public enum TaskStatus {

    PENDING(0, "待发布"),
    PUBLISHED(1, "已发布"),
    PLAYING(2, "播放中"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    private final Integer code;
    private final String desc;

    public static TaskStatus of(Integer code) {
        for (TaskStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
