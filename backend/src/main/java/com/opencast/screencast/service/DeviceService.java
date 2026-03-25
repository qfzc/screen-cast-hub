package com.opencast.screencast.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opencast.screencast.common.constant.SystemConstants;
import com.opencast.screencast.common.enums.DeviceStatus;
import com.opencast.screencast.common.exception.BusinessException;
import com.opencast.screencast.common.result.ErrorCode;
import com.opencast.screencast.common.result.PageResult;
import com.opencast.screencast.common.utils.BindCodeUtils;
import com.opencast.screencast.dto.device.*;
import com.opencast.screencast.entity.Device;
import com.opencast.screencast.entity.DeviceGroup;
import com.opencast.screencast.mapper.DeviceGroupMapper;
import com.opencast.screencast.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备服务
 */
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceMapper deviceMapper;
    private final DeviceGroupMapper deviceGroupMapper;

    @Value("${app.bind-code.expire-minutes:10}")
    private int bindCodeExpireMinutes;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * 生成绑定码（TV端调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public BindCodeResponse generateBindCode(BindCodeRequest request) {
        // 查找或创建设备
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getDeviceToken, request.getDeviceToken());
        Device device = deviceMapper.selectOne(wrapper);

        if (device == null) {
            device = new Device();
            device.setDeviceToken(request.getDeviceToken());
            device.setStatus(DeviceStatus.UNBOUND.getCode());
            device.setDeleted(0);
        }

        // 更新设备信息
        if (StrUtil.isNotBlank(request.getDeviceName())) {
            device.setName(request.getDeviceName());
        }
        device.setModel(request.getModel());
        device.setOsVersion(request.getOsVersion());
        device.setAppVersion(request.getAppVersion());
        device.setOnlineAt(LocalDateTime.now());

        // 生成绑定码
        String bindCode = BindCodeUtils.generateBindCode();
        device.setBindCode(bindCode);
        device.setBindCodeExpire(LocalDateTime.now().plusMinutes(bindCodeExpireMinutes));

        if (device.getId() == null) {
            deviceMapper.insert(device);
        } else {
            deviceMapper.updateById(device);
        }

        // 构建响应
        BindCodeResponse response = new BindCodeResponse();
        response.setBindCode(bindCode);
        response.setQrCodeUrl(baseUrl + "/bind?code=" + bindCode + "&token=" + device.getDeviceToken());
        response.setExpireAt(device.getBindCodeExpire());
        response.setDeviceId(device.getId());

        return response;
    }

    /**
     * 绑定设备（小程序端调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public Device bindDevice(BindDeviceRequest request, Long userId) {
        // 查找设备
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getBindCode, request.getBindCode());
        Device device = deviceMapper.selectOne(wrapper);

        if (device == null) {
            throw new BusinessException(ErrorCode.BIND_CODE_INVALID);
        }

        // 检查绑定码是否过期
        if (device.getBindCodeExpire() != null && device.getBindCodeExpire().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BIND_CODE_EXPIRED);
        }

        // 检查设备是否已绑定
        if (DeviceStatus.BOUND.getCode().equals(device.getStatus())) {
            throw new BusinessException(ErrorCode.DEVICE_ALREADY_BOUND);
        }

        // 绑定设备
        device.setUserId(userId);
        device.setStatus(DeviceStatus.BOUND.getCode());
        device.setBindAt(LocalDateTime.now());
        if (StrUtil.isNotBlank(request.getDeviceName())) {
            device.setName(request.getDeviceName());
        }

        deviceMapper.updateById(device);

        return device;
    }

    /**
     * 查询绑定状态（TV端轮询）
     */
    public BindStatusResponse getBindStatus(String deviceToken) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getDeviceToken, deviceToken);
        Device device = deviceMapper.selectOne(wrapper);

        BindStatusResponse response = new BindStatusResponse();
        if (device == null || DeviceStatus.UNBOUND.getCode().equals(device.getStatus())) {
            response.setBindStatus(0);
            return response;
        }

        response.setBindStatus(device.getStatus());
        response.setUserId(device.getUserId());
        response.setBindAt(device.getBindAt());

        return response;
    }

    /**
     * 设备心跳
     */
    public void heartbeat(HeartbeatRequest request) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getDeviceToken, request.getDeviceToken());
        Device device = deviceMapper.selectOne(wrapper);

        if (device == null) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }

        // 更新设备状态
        device.setOnlineAt(LocalDateTime.now());
        device.setStorageUsed(request.getStorageUsed());
        device.setStorageTotal(request.getStorageTotal());

        if (DeviceStatus.OFFLINE.getCode().equals(device.getStatus())) {
            device.setStatus(DeviceStatus.BOUND.getCode());
        }

        deviceMapper.updateById(device);
    }

    /**
     * 获取设备列表
     */
    public PageResult<DeviceVO> getDeviceList(Long userId, Integer status, String keyword, Long page, Long size) {
        page = page == null ? SystemConstants.DEFAULT_PAGE : page;
        size = size == null ? SystemConstants.DEFAULT_SIZE : Math.min(size, SystemConstants.MAX_SIZE);

        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getUserId, userId);
        wrapper.eq(Device::getDeleted, 0);

        if (status != null) {
            wrapper.eq(Device::getStatus, status);
        }

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(Device::getName, keyword);
        }

        wrapper.orderByDesc(Device::getOnlineAt);

        Page<Device> pageResult = deviceMapper.selectPage(new Page<>(page, size), wrapper);

        List<DeviceVO> list = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(pageResult.getTotal(), list, page, size);
    }

    /**
     * 获取设备详情
     */
    public DeviceVO getDeviceDetail(Long deviceId, Long userId) {
        Device device = deviceMapper.selectById(deviceId);

        if (device == null || !userId.equals(device.getUserId())) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }

        return convertToVO(device);
    }

    /**
     * 更新设备在线状态（用于定时任务）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOfflineStatus() {
        // 超过3分钟未心跳的设备标记为离线
        LocalDateTime offlineThreshold = LocalDateTime.now().minusMinutes(3);

        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getStatus, DeviceStatus.BOUND.getCode());
        wrapper.lt(Device::getOnlineAt, offlineThreshold);

        Device updateDevice = new Device();
        updateDevice.setStatus(DeviceStatus.OFFLINE.getCode());

        deviceMapper.update(updateDevice, wrapper);
    }

    /**
     * 解绑设备
     */
    @Transactional(rollbackFor = Exception.class)
    public void unbindDevice(Long deviceId, Long userId) {
        Device device = deviceMapper.selectById(deviceId);

        if (device == null || !userId.equals(device.getUserId())) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }

        device.setUserId(null);
        device.setStatus(DeviceStatus.UNBOUND.getCode());
        device.setBindCode(null);
        device.setBindCodeExpire(null);

        deviceMapper.updateById(device);
    }

    /**
     * 重命名设备
     */
    @Transactional(rollbackFor = Exception.class)
    public DeviceVO renameDevice(Long deviceId, String name, Long userId) {
        Device device = deviceMapper.selectById(deviceId);

        if (device == null || !userId.equals(device.getUserId())) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }

        device.setName(name);
        deviceMapper.updateById(device);

        return convertToVO(device);
    }

    /**
     * 设置设备分组
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDeviceGroup(Long deviceId, Long groupId, Long userId) {
        Device device = deviceMapper.selectById(deviceId);

        if (device == null || !userId.equals(device.getUserId())) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }

        device.setGroupId(groupId);
        deviceMapper.updateById(device);
    }

    /**
     * 转换为VO
     */
    private DeviceVO convertToVO(Device device) {
        DeviceVO vo = new DeviceVO();
        vo.setId(device.getId());
        vo.setName(device.getName());
        vo.setModel(device.getModel());
        vo.setDeviceToken(device.getDeviceToken());
        vo.setBindCode(device.getBindCode());
        vo.setStatus(device.getStatus());

        DeviceStatus status = DeviceStatus.of(device.getStatus());
        vo.setStatusDesc(status != null ? status.getDesc() : "未知");

        vo.setOnlineAt(device.getOnlineAt());
        vo.setStorageUsed(device.getStorageUsed());
        vo.setStorageTotal(device.getStorageTotal());
        vo.setGroupId(device.getGroupId());
        vo.setBindAt(device.getBindAt());
        vo.setCreatedAt(device.getCreatedAt());
        vo.setAppVersion(device.getAppVersion());

        // 获取分组名称
        if (device.getGroupId() != null) {
            DeviceGroup group = deviceGroupMapper.selectById(device.getGroupId());
            if (group != null) {
                vo.setGroupName(group.getName());
            }
        }

        return vo;
    }
}
