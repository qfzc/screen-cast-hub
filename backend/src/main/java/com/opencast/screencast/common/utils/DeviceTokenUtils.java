package com.opencast.screencast.common.utils;

import cn.hutool.core.util.IdUtil;

/**
 * 设备Token工具类
 */
public class DeviceTokenUtils {

    private static final String PREFIX = "device_";

    /**
     * 生成设备Token
     */
    public static String generateDeviceToken() {
        return PREFIX + IdUtil.fastSimpleUUID();
    }
}
