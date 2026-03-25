package com.opencast.screencast.controller;

import com.opencast.screencast.common.result.PageResult;
import com.opencast.screencast.common.result.Result;
import com.opencast.screencast.dto.publish.*;
import com.opencast.screencast.service.PublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 发布控制器
 */
@Tag(name = "发布管理", description = "内容发布、任务管理、播放记录")
@RestController
@RequestMapping("/api/v1/publish")
@RequiredArgsConstructor
public class PublishController {

    private final PublishService publishService;

    @Operation(summary = "创建发布任务", description = "向指定设备发布素材内容")
    @PostMapping("/task")
    public Result<List<PublishTaskVO>> createTask(
            @Valid @RequestBody PublishTaskRequest request,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return Result.success(publishService.createTask(request, userId));
    }

    @Operation(summary = "获取设备播放任务", description = "TV端调用，获取待播放的任务列表")
    @GetMapping("/tasks")
    public Result<List<DeviceTaskVO>> getDeviceTasks(
            @Parameter(description = "设备Token") @RequestParam String deviceToken) {
        return Result.success(publishService.getDeviceTasks(deviceToken));
    }

    @Operation(summary = "获取设备任务列表", description = "管理端调用，根据设备ID获取任务列表")
    @GetMapping("/device/{deviceId}")
    public Result<List<DeviceTaskVO>> getDeviceTasksById(
            @Parameter(description = "设备ID") @PathVariable Long deviceId) {
        return Result.success(publishService.getDeviceTasksByDeviceId(deviceId));
    }

    @Operation(summary = "确认任务完成", description = "TV端调用，确认任务播放完成")
    @PostMapping("/task/{taskId}/complete")
    public Result<Void> completeTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @Valid @RequestBody TaskCompleteRequest request,
            @Parameter(hidden = true) @RequestHeader("X-Device-Id") Long deviceId) {
        publishService.completeTask(taskId, request, deviceId);
        return Result.success();
    }

    @Operation(summary = "发布记录列表", description = "管理端查看发布历史记录")
    @GetMapping("/records")
    public Result<PageResult<PublishRecordVO>> getPublishRecords(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "设备ID筛选") @RequestParam(required = false) Long deviceId,
            @Parameter(description = "页码") @RequestParam(required = false) Long page,
            @Parameter(description = "每页大小") @RequestParam(required = false) Long size) {
        return Result.success(publishService.getPublishRecords(userId, deviceId, page, size));
    }

    @Operation(summary = "取消任务", description = "取消待发布或已发布的任务")
    @PostMapping("/{taskId}/cancel")
    public Result<Void> cancelTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        publishService.cancelTask(taskId, userId);
        return Result.success();
    }
}
