package com.opencast.screencast.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opencast.screencast.common.constant.SystemConstants;
import com.opencast.screencast.common.enums.TaskStatus;
import com.opencast.screencast.common.exception.BusinessException;
import com.opencast.screencast.common.result.ErrorCode;
import com.opencast.screencast.common.result.PageResult;
import com.opencast.screencast.common.service.MqttService;
import com.opencast.screencast.entity.Device;
import com.opencast.screencast.mapper.DeviceMapper;
import com.opencast.screencast.entity.Material;
import com.opencast.screencast.mapper.MaterialMapper;
import com.opencast.screencast.dto.publish.*;
import com.opencast.screencast.entity.PlayLog;
import com.opencast.screencast.entity.PublishTask;
import com.opencast.screencast.entity.PublishTaskItem;
import com.opencast.screencast.mapper.PlayLogMapper;
import com.opencast.screencast.mapper.PublishTaskMapper;
import com.opencast.screencast.mapper.PublishTaskItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 发布服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublishService {

    private final PublishTaskMapper publishTaskMapper;
    private final PublishTaskItemMapper publishTaskItemMapper;
    private final PlayLogMapper playLogMapper;
    private final DeviceMapper deviceMapper;
    private final MaterialMapper materialMapper;

    @Autowired(required = false)
    private MqttService mqttService;

    /**
     * 创建发布任务
     */
    @Transactional(rollbackFor = Exception.class)
    public List<PublishTaskVO> createTask(PublishTaskRequest request, Long userId) {
        List<PublishTaskVO> results = new ArrayList<>();
        String batchId = IdUtil.fastSimpleUUID();
        String playMode = request.getPlayMode() != null ? request.getPlayMode() : "SEQUENCE";
        Integer interval = request.getPlayInterval() != null ? request.getPlayInterval() : 5;
        Boolean loopPlay = request.getLoopPlay() != null ? request.getLoopPlay() : false;

        // 判断是否立即发布：没有定时时间或定时时间已过
        LocalDateTime now = LocalDateTime.now();
        boolean isImmediatePublish = request.getScheduledAt() == null || request.getScheduledAt().isBefore(now);
        TaskStatus initialStatus = isImmediatePublish ? TaskStatus.PUBLISHED : TaskStatus.PENDING;

        // 为每个设备创建一个任务
        Map<Long, PublishTask> tasksByDevice = new HashMap<>();

        for (Long deviceId : request.getDeviceIds()) {
            // 创建主任务
            PublishTask task = new PublishTask();
            task.setBatchId(batchId);
            task.setDeviceId(deviceId);
            task.setUserId(userId);
            task.setName(request.getName());
            task.setPlayMode(playMode);
            task.setPlayInterval(interval);
            task.setLoopPlay(loopPlay);
            task.setAutoPlay(request.getAutoPlay() != null ? request.getAutoPlay() : true);
            task.setScheduledAt(request.getScheduledAt());
            task.setStatus(initialStatus.getCode());

            // 如果是立即发布，设置发布时间
            if (isImmediatePublish) {
                task.setPublishedAt(now);
            }

            publishTaskMapper.insert(task);
            tasksByDevice.put(deviceId, task);

            // 创建任务明细
            int sortOrder = 0;
            for (PublishTaskRequest.MaterialItemRequest materialItem : request.getEffectiveMaterials()) {
                PublishTaskItem item = new PublishTaskItem();
                item.setTaskId(task.getId());
                item.setMaterialId(materialItem.getMaterialId());
                item.setSortOrder(materialItem.getSortOrder() != null ? materialItem.getSortOrder() : sortOrder++);
                item.setDuration(materialItem.getDuration() != null ? materialItem.getDuration() : 30);
                item.setFitMode(materialItem.getFitMode() != null ? materialItem.getFitMode() : "FILL");
                item.setTransition(materialItem.getTransition() != null ? materialItem.getTransition() : "CUBE");
                item.setStatus(0);
                item.setPlayCount(0);
                item.setCreatedAt(now);

                publishTaskItemMapper.insert(item);
            }

            results.add(convertToVO(task));
        }

        // 如果是立即发布，按设备发送MQTT消息
        if (isImmediatePublish && mqttService != null) {
            for (Map.Entry<Long, PublishTask> entry : tasksByDevice.entrySet()) {
                Long deviceId = entry.getKey();
                PublishTask task = entry.getValue();

                try {
                    Device device = deviceMapper.selectById(deviceId);
                    if (device != null && device.getDeviceToken() != null) {
                        // 获取任务明细
                        List<PublishTaskItem> items = getTaskItems(task.getId());
                        BatchTaskVO batchTask = buildBatchTask(task, items);
                        mqttService.publishBatchTask(device.getDeviceToken(), batchTask);
                        log.info("Sent batch task to device {} with {} materials", device.getDeviceToken(), items.size());
                    }
                } catch (Exception e) {
                    log.warn("Failed to send MQTT batch notification for device: {}", deviceId, e);
                }
            }
        }

        return results;
    }

    /**
     * 获取任务明细列表
     */
    private List<PublishTaskItem> getTaskItems(Long taskId) {
        LambdaQueryWrapper<PublishTaskItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PublishTaskItem::getTaskId, taskId);
        wrapper.orderByAsc(PublishTaskItem::getSortOrder);
        return publishTaskItemMapper.selectList(wrapper);
    }

    /**
     * 构建批量任务VO
     */
    private BatchTaskVO buildBatchTask(PublishTask task, List<PublishTaskItem> items) {
        BatchTaskVO batch = new BatchTaskVO();
        batch.setTaskId(task.getId());
        batch.setBatchId(task.getBatchId());
        batch.setPlayMode(task.getPlayMode());
        batch.setPlayInterval(task.getPlayInterval());
        batch.setLoopPlay(task.getLoopPlay());
        batch.setAutoPlay(task.getAutoPlay());

        List<BatchTaskVO.TaskItemVO> itemVOs = new ArrayList<>();
        for (PublishTaskItem item : items) {
            Material material = materialMapper.selectById(item.getMaterialId());
            if (material != null) {
                BatchTaskVO.TaskItemVO itemVO = new BatchTaskVO.TaskItemVO();
                itemVO.setMaterialId(material.getId());
                itemVO.setMaterialName(material.getName());
                itemVO.setMaterialType(material.getType());
                itemVO.setMaterialUrl(material.getConvertedUrl() != null ? material.getConvertedUrl() : material.getOriginalUrl());
                itemVO.setThumbnailUrl(material.getThumbnailUrl());
                itemVO.setSortOrder(item.getSortOrder());
                itemVO.setDuration(item.getDuration());
                itemVO.setFitMode(item.getFitMode());
                itemVO.setTransition(item.getTransition());
                itemVO.setPageCount(material.getPageCount());
                itemVOs.add(itemVO);
            }
        }
        batch.setItems(itemVOs);

        return batch;
    }

    /**
     * 获取设备播放任务（TV端调用）
     */
    public List<DeviceTaskVO> getDeviceTasks(String deviceToken) {
        // 通过deviceToken查询设备
        LambdaQueryWrapper<Device> deviceWrapper = new LambdaQueryWrapper<>();
        deviceWrapper.eq(Device::getDeviceToken, deviceToken);
        Device device = deviceMapper.selectOne(deviceWrapper);

        if (device == null) {
            return new ArrayList<>();
        }

        return getDeviceTasksByDeviceId(device.getId());
    }

    /**
     * 获取设备播放任务（通过设备ID）
     */
    public List<DeviceTaskVO> getDeviceTasksByDeviceId(Long deviceId) {
        LambdaQueryWrapper<PublishTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PublishTask::getDeviceId, deviceId);
        wrapper.in(PublishTask::getStatus, TaskStatus.PUBLISHED.getCode(), TaskStatus.PLAYING.getCode());
        wrapper.orderByDesc(PublishTask::getCreatedAt);

        List<PublishTask> tasks = publishTaskMapper.selectList(wrapper);

        return tasks.stream()
                .map(this::convertToDeviceTaskVO)
                .collect(Collectors.toList());
    }

    /**
     * 确认任务完成（TV端调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId, TaskCompleteRequest request, Long deviceId) {
        PublishTask task = publishTaskMapper.selectById(taskId);

        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }

        if (!task.getDeviceId().equals(deviceId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 更新任务状态
        task.setStatus(TaskStatus.COMPLETED.getCode());
        task.setCompletedAt(LocalDateTime.now());

        publishTaskMapper.updateById(task);

        // 更新所有明细为已完成
        LambdaQueryWrapper<PublishTaskItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PublishTaskItem::getTaskId, taskId);
        List<PublishTaskItem> items = publishTaskItemMapper.selectList(itemWrapper);
        for (PublishTaskItem item : items) {
            item.setStatus(2); // 已完成
            publishTaskItemMapper.updateById(item);
        }

        // 记录播放日志
        for (PublishTaskItem item : items) {
            PlayLog playLog = new PlayLog();
            playLog.setDeviceId(deviceId);
            playLog.setTaskId(taskId);
            playLog.setMaterialId(item.getMaterialId());
            playLog.setPlayAt(LocalDateTime.now());
            playLog.setPlayDuration(item.getDuration());
            playLog.setPlayCount(item.getPlayCount());

            playLogMapper.insert(playLog);
        }
    }

    /**
     * 获取发布记录列表
     */
    public PageResult<PublishRecordVO> getPublishRecords(Long userId, Long deviceId, Long page, Long size) {
        page = page == null ? SystemConstants.DEFAULT_PAGE : page;
        size = size == null ? SystemConstants.DEFAULT_SIZE : Math.min(size, SystemConstants.MAX_SIZE);

        LambdaQueryWrapper<PublishTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PublishTask::getUserId, userId);

        if (deviceId != null) {
            wrapper.eq(PublishTask::getDeviceId, deviceId);
        }

        wrapper.orderByDesc(PublishTask::getCreatedAt);

        Page<PublishTask> pageResult = publishTaskMapper.selectPage(new Page<>(page, size), wrapper);

        List<PublishRecordVO> list = pageResult.getRecords().stream()
                .map(this::convertToRecordVO)
                .collect(Collectors.toList());

        return PageResult.of(pageResult.getTotal(), list, page, size);
    }

    /**
     * 获取任务详情
     */
    public PublishTaskVO getTaskDetail(Long taskId, Long userId) {
        PublishTask task = publishTaskMapper.selectById(taskId);

        if (task == null || !userId.equals(task.getUserId())) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }

        return convertToVO(task);
    }

    /**
     * 取消任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long taskId, Long userId) {
        PublishTask task = publishTaskMapper.selectById(taskId);

        if (task == null || !userId.equals(task.getUserId())) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }

        if (TaskStatus.COMPLETED.getCode().equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_COMPLETED);
        }

        task.setStatus(TaskStatus.CANCELLED.getCode());
        publishTaskMapper.updateById(task);

        // 发送MQTT取消消息到设备
        try {
            if (mqttService != null) {
                Device device = deviceMapper.selectById(task.getDeviceId());
                if (device != null && device.getDeviceToken() != null) {
                    mqttService.publishCommand(device.getDeviceToken(), "cancel_task:" + taskId);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to send MQTT cancel notification for task: {}", taskId, e);
        }
    }

    /**
     * 转换为VO
     */
    private PublishTaskVO convertToVO(PublishTask task) {
        PublishTaskVO vo = new PublishTaskVO();
        vo.setTaskId(task.getId());
        vo.setBatchId(task.getBatchId());
        vo.setName(task.getName());
        vo.setPlayMode(task.getPlayMode());
        vo.setPlayInterval(task.getPlayInterval());
        vo.setAutoPlay(task.getAutoPlay());
        vo.setLoopPlay(task.getLoopPlay());
        vo.setStatus(task.getStatus());
        vo.setDeviceId(task.getDeviceId());
        vo.setPublishedAt(task.getPublishedAt());
        vo.setCreatedAt(task.getCreatedAt());

        TaskStatus status = TaskStatus.of(task.getStatus());
        vo.setStatusDesc(status != null ? status.getDesc() : "未知");

        // 获取设备名称
        Device device = deviceMapper.selectById(task.getDeviceId());
        if (device != null) {
            vo.setDeviceName(device.getName());
        }

        // 获取任务明细
        List<PublishTaskItem> items = getTaskItems(task.getId());
        List<PublishTaskItemVO> itemVOs = items.stream()
                .map(this::convertToItemVO)
                .collect(Collectors.toList());
        vo.setItems(itemVOs);
        vo.setItemCount(itemVOs.size());

        return vo;
    }

    /**
     * 转换明细为VO
     */
    private PublishTaskItemVO convertToItemVO(PublishTaskItem item) {
        PublishTaskItemVO vo = new PublishTaskItemVO();
        vo.setId(item.getId());
        vo.setMaterialId(item.getMaterialId());
        vo.setSortOrder(item.getSortOrder());
        vo.setDuration(item.getDuration());
        vo.setFitMode(item.getFitMode());
        vo.setTransition(item.getTransition());
        vo.setStatus(item.getStatus());
        vo.setPlayCount(item.getPlayCount());

        // 状态描述
        String statusDesc = switch (item.getStatus()) {
            case 0 -> "待播放";
            case 1 -> "播放中";
            case 2 -> "已完成";
            default -> "未知";
        };
        vo.setStatusDesc(statusDesc);

        // 获取素材信息
        Material material = materialMapper.selectById(item.getMaterialId());
        if (material != null) {
            vo.setMaterialName(material.getName());
            vo.setMaterialType(material.getType());
            vo.setMaterialUrl(material.getConvertedUrl() != null ? material.getConvertedUrl() : material.getOriginalUrl());
            vo.setThumbnailUrl(material.getThumbnailUrl());
            vo.setPageCount(material.getPageCount());
            vo.setMaterialMd5(material.getFileMd5());
        }

        return vo;
    }

    /**
     * 转换为设备任务VO
     */
    private DeviceTaskVO convertToDeviceTaskVO(PublishTask task) {
        DeviceTaskVO vo = new DeviceTaskVO();
        vo.setTaskId(task.getId());
        vo.setBatchId(task.getBatchId());
        vo.setPlayMode(task.getPlayMode());
        vo.setPlayInterval(task.getPlayInterval());
        vo.setAutoPlay(task.getAutoPlay());
        vo.setLoopPlay(task.getLoopPlay());
        vo.setStatus(task.getStatus());
        vo.setCreatedAt(task.getCreatedAt());

        TaskStatus status = TaskStatus.of(task.getStatus());
        vo.setStatusDesc(status != null ? status.getDesc() : "未知");

        // 获取任务明细
        List<PublishTaskItem> items = getTaskItems(task.getId());
        List<DeviceTaskVO.TaskItemVO> itemVOs = new ArrayList<>();
        for (PublishTaskItem item : items) {
            Material material = materialMapper.selectById(item.getMaterialId());
            if (material != null) {
                DeviceTaskVO.TaskItemVO itemVO = new DeviceTaskVO.TaskItemVO();
                itemVO.setMaterialId(material.getId());
                itemVO.setMaterialName(material.getName());
                itemVO.setMaterialType(material.getType());
                itemVO.setMaterialUrl(material.getConvertedUrl() != null ? material.getConvertedUrl() : material.getOriginalUrl());
                itemVO.setThumbnailUrl(material.getThumbnailUrl());
                itemVO.setSortOrder(item.getSortOrder());
                itemVO.setDuration(item.getDuration());
                itemVO.setFitMode(item.getFitMode());
                itemVO.setTransition(item.getTransition());
                itemVO.setPageCount(material.getPageCount());
                itemVO.setMaterialMd5(material.getFileMd5());
                itemVOs.add(itemVO);
            }
        }
        vo.setItems(itemVOs);

        return vo;
    }

    /**
     * 转换为发布记录VO
     */
    private PublishRecordVO convertToRecordVO(PublishTask task) {
        PublishRecordVO vo = new PublishRecordVO();
        vo.setId(task.getId());
        vo.setDeviceId(task.getDeviceId());
        vo.setBatchId(task.getBatchId());
        vo.setName(task.getName());
        vo.setStatus(task.getStatus());
        vo.setPublishedAt(task.getPublishedAt());
        vo.setCompletedAt(task.getCompletedAt());
        vo.setCreatedAt(task.getCreatedAt());

        TaskStatus status = TaskStatus.of(task.getStatus());
        vo.setStatusDesc(status != null ? status.getDesc() : "未知");

        // 获取设备名称
        Device device = deviceMapper.selectById(task.getDeviceId());
        if (device != null) {
            vo.setDeviceName(device.getName());
        }

        // 获取素材数量
        List<PublishTaskItem> items = getTaskItems(task.getId());
        vo.setItemCount(items.size());

        return vo;
    }
}
