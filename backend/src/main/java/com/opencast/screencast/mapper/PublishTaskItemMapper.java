package com.opencast.screencast.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opencast.screencast.entity.PublishTaskItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发布任务明细Mapper
 */
@Mapper
public interface PublishTaskItemMapper extends BaseMapper<PublishTaskItem> {
}
