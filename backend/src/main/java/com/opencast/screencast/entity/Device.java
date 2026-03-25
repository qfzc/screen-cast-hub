package com.opencast.screencast.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.opencast.screencast.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 设备实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device")
public class Device extends BaseEntity {

    /**
     * 绑定用户ID
     */
    private Long userId;

    /**
     * 设备分组ID
     */
    private Long groupId;

    /**
     * 设备唯一标识
     */
    private String deviceToken;

    /**
     * 绑定码
     */
    private String bindCode;

    /**
     * 绑定码过期时间
     */
    private LocalDateTime bindCodeExpire;

    /**
     * 设备名称
     */
    private String name;

    /**
     * 设备型号
     */
    private String model;

    /**
     * 系统版本
     */
    private String osVersion;

    /**
     * APP版本
     */
    private String appVersion;

    /**
     * 总存储(字节)
     */
    private Long storageTotal;

    /**
     * 已用存储(字节)
     */
    private Long storageUsed;

    /**
     * 状态: 0未绑定 1已绑定 2离线
     */
    private Integer status;

    /**
     * 最后在线时间
     */
    private LocalDateTime onlineAt;

    /**
     * 绑定时间
     */
    private LocalDateTime bindAt;
}
