package com.opencast.screencast.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 转码状态枚举
 */
@Getter
@AllArgsConstructor
public enum ConvertStatus {

    NO_NEED(0, "无需转码"),
    CONVERTING(1, "转码中"),
    SUCCESS(2, "转码成功"),
    FAILED(3, "转码失败");

    private final Integer code;
    private final String desc;

    public static ConvertStatus of(Integer code) {
        for (ConvertStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
