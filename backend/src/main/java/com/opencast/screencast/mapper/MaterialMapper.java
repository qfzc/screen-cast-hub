package com.opencast.screencast.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opencast.screencast.entity.Material;
import org.apache.ibatis.annotations.Mapper;

/**
 * 素材Mapper
 */
@Mapper
public interface MaterialMapper extends BaseMapper<Material> {
}
