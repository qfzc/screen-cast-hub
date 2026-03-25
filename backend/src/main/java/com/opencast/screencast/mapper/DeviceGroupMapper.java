package com.opencast.screencast.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opencast.screencast.entity.DeviceGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备分组 Mapper
 */
@Mapper
public interface DeviceGroupMapper extends BaseMapper<DeviceGroup> {
}
