package com.opencast.screencast.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opencast.screencast.entity.PublishTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发布任务Mapper
 */
@Mapper
public interface PublishTaskMapper extends BaseMapper<PublishTask> {
}
