package com.opencast.screencast.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 素材类型枚举
 */
@Getter
@AllArgsConstructor
public enum MaterialType {

    IMAGE("image", "图片"),
    VIDEO("video", "视频"),
    PDF("pdf", "PDF"),
    PPT("ppt", "PPT");

    private final String code;
    private final String desc;

    public static MaterialType of(String code) {
        for (MaterialType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断是否需要转码
     */
    public boolean needConvert() {
        return this == PPT;
    }
}
