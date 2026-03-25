package com.opencast.screencast.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.opencast.screencast.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备分组实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_group")
public class DeviceGroup extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 分组描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sortOrder;
}
