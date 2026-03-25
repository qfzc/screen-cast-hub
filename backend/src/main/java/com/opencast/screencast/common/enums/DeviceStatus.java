package com.opencast.screencast.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备状态枚举
 */
@Getter
@AllArgsConstructor
public enum DeviceStatus {

    UNBOUND(0, "未绑定"),
    BOUND(1, "已绑定"),
    OFFLINE(2, "离线");

    private final Integer code;
    private final String desc;

    public static DeviceStatus of(Integer code) {
        for (DeviceStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
