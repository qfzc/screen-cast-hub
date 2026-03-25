package com.opencast.screencast.common.utils;

import cn.hutool.core.util.RandomUtil;

/**
 * 绑定码工具类
 */
public class BindCodeUtils {

    private static final int BIND_CODE_LENGTH = 6;

    /**
     * 生成绑定码（6位大写字母+数字）
     */
    public static String generateBindCode() {
        return RandomUtil.randomString("ABCDEFGHJKLMNPQRSTUVWXYZ23456789", BIND_CODE_LENGTH);
    }
}
