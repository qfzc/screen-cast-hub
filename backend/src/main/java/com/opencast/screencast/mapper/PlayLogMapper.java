package com.opencast.screencast.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opencast.screencast.entity.PlayLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 播放日志Mapper
 */
@Mapper
public interface PlayLogMapper extends BaseMapper<PlayLog> {
}
