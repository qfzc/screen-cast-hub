package com.opencast.screencast.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.opencast.screencast.common.exception.BusinessException;
import com.opencast.screencast.common.result.ErrorCode;
import com.opencast.screencast.common.service.MqttService;
import com.opencast.screencast.dto.device.DevicePlaylistRequest;
import com.opencast.screencast.dto.device.DevicePlaylistVO;
import com.opencast.screencast.entity.Device;
import com.opencast.screencast.entity.DevicePlaylist;
import com.opencast.screencast.entity.Material;
import com.opencast.screencast.entity.PlaylistItem;
import com.opencast.screencast.mapper.DeviceMapper;
import com.opencast.screencast.mapper.DevicePlaylistMapper;
import com.opencast.screencast.mapper.MaterialMapper;
import com.opencast.screencast.mapper.PlaylistItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 播放列表服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final DevicePlaylistMapper playlistMapper;
    private final PlaylistItemMapper itemMapper;
    private final DeviceMapper deviceMapper;
    private final MaterialMapper materialMapper;
    private final MqttService mqttService;

    /**
     * 获取设备播放列表
     */
    public DevicePlaylistVO getPlaylist(Long deviceId) {
        // 查找设备的播放列表
        LambdaQueryWrapper<DevicePlaylist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DevicePlaylist::getDeviceId, deviceId);
        DevicePlaylist playlist = playlistMapper.selectOne(wrapper);

        if (playlist == null) {
            // 返回空列表
            DevicePlaylistVO vo = new DevicePlaylistVO();
            vo.setDeviceId(deviceId);
            vo.setPlayMode("SEQUENCE");
            vo.setPlayInterval(5);
            vo.setLoopPlay(true);
            vo.setItems(new ArrayList<>());
            return vo;
        }

        return convertToVO(playlist);
    }

    /**
     * 更新设备播放列表并推送到TV端
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePlaylist(Long deviceId, DevicePlaylistRequest request) {
        // 查找或创建播放列表
        LambdaQueryWrapper<DevicePlaylist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DevicePlaylist::getDeviceId, deviceId);
        DevicePlaylist playlist = playlistMapper.selectOne(wrapper);

        if (playlist == null) {
            playlist = new DevicePlaylist();
            playlist.setDeviceId(deviceId);
        }

        // 更新播放列表
        playlist.setPlayMode(request.getPlayMode() != null ? request.getPlayMode() : "SEQUENCE");
        playlist.setPlayInterval(request.getPlayInterval() != null ? request.getPlayInterval() : 5);
        playlist.setLoopPlay(request.getLoopPlay() != null ? request.getLoopPlay() : true);

        if (playlist.getId() == null) {
            playlistMapper.insert(playlist);
        } else {
            playlistMapper.updateById(playlist);
            
            // 删除旧的播放项
            LambdaQueryWrapper<PlaylistItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(PlaylistItem::getPlaylistId, playlist.getId());
            itemMapper.delete(itemWrapper);
        }

        // 插入新的播放项
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (DevicePlaylistRequest.PlaylistItemRequest itemRequest : request.getItems()) {
                PlaylistItem item = new PlaylistItem();
                item.setPlaylistId(playlist.getId());
                item.setMaterialId(itemRequest.getMaterialId());
                item.setSortOrder(itemRequest.getSortOrder() != null ? itemRequest.getSortOrder() : 0);
                item.setFitMode(itemRequest.getFitMode() != null ? itemRequest.getFitMode() : "FILL");
                item.setDuration(itemRequest.getDuration() != null ? itemRequest.getDuration() : 10);
                item.setTransition(itemRequest.getTransition() != null ? itemRequest.getTransition() : "NONE");
                itemMapper.insert(item);
            }
        }

        // 推送到TV端
        pushToTv(deviceId, playlist);
    }

    /**
     * 推送播放列表到TV端
     */
    private void pushToTv(Long deviceId, DevicePlaylist playlist) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null || device.getDeviceToken() == null) {
            log.warn("Device not found or no device token: {}", deviceId);
            return;
        }

        DevicePlaylistVO vo = convertToVO(playlist);
        
        try {
            mqttService.publishPlaylistUpdate(device.getDeviceToken(), vo);
            log.info("Playlist pushed to device: {}", device.getDeviceToken());
        } catch (Exception e) {
            log.error("Failed to push playlist to device: {}", deviceId, e);
        }
    }

    /**
     * 转换为VO
     */
    private DevicePlaylistVO convertToVO(DevicePlaylist playlist) {
        DevicePlaylistVO vo = new DevicePlaylistVO();
        vo.setDeviceId(playlist.getDeviceId());
        vo.setPlayMode(playlist.getPlayMode());
        vo.setPlayInterval(playlist.getPlayInterval());
        vo.setLoopPlay(playlist.getLoopPlay());

        // 查询播放项
        LambdaQueryWrapper<PlaylistItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaylistItem::getPlaylistId, playlist.getId());
        wrapper.orderByAsc(PlaylistItem::getSortOrder);
        List<PlaylistItem> items = itemMapper.selectList(wrapper);

        List<DevicePlaylistVO.PlaylistItemVO> itemVOs = items.stream()
                .map(this::convertItemToVO)
                .collect(Collectors.toList());

        vo.setItems(itemVOs);
        return vo;
    }

    /**
     * 转换播放项为VO
     */
    private DevicePlaylistVO.PlaylistItemVO convertItemToVO(PlaylistItem item) {
        DevicePlaylistVO.PlaylistItemVO vo = new DevicePlaylistVO.PlaylistItemVO();
        vo.setId(item.getId());
        vo.setMaterialId(item.getMaterialId());
        vo.setSortOrder(item.getSortOrder());
        vo.setFitMode(item.getFitMode());
        vo.setDuration(item.getDuration());
        vo.setTransition(item.getTransition());

        // 获取素材信息
        if (item.getMaterialId() != null) {
            Material material = materialMapper.selectById(item.getMaterialId());
            if (material != null) {
                vo.setMaterialName(material.getName());
                vo.setMaterialType(material.getType());
                vo.setMaterialUrl(material.getConvertedUrl() != null ? material.getConvertedUrl() : material.getOriginalUrl());
                vo.setThumbnailUrl(material.getThumbnailUrl());
            }
        }

        return vo;
    }
}
