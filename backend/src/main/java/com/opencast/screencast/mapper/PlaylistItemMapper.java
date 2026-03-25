package com.opencast.screencast.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opencast.screencast.entity.PlaylistItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlaylistItemMapper extends BaseMapper<PlaylistItem> {
}
