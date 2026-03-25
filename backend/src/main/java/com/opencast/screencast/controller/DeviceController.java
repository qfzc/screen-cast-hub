package com.opencast.screencast.controller;

import com.opencast.screencast.common.result.PageResult;
import com.opencast.screencast.common.result.Result;
import com.opencast.screencast.dto.device.*;
import com.opencast.screencast.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 设备控制器
 */
@Tag(name = "设备管理", description = "设备绑定、心跳、状态管理")
@RestController
@RequestMapping("/api/v1/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "生成绑定码", description = "TV端调用，生成绑定二维码")
    @PostMapping("/bind-code")
    public Result<BindCodeResponse> generateBindCode(@Valid @RequestBody BindCodeRequest request) {
        return Result.success(deviceService.generateBindCode(request));
    }

    @Operation(summary = "扫码绑定设备", description = "小程序端调用，绑定设备到用户")
    @PostMapping("/bind")
    public Result<DeviceVO> bindDevice(@Valid @RequestBody BindDeviceRequest request,
                                       @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return Result.success(deviceService.getDeviceDetail(
                deviceService.bindDevice(request, userId).getId(), userId));
    }

    @Operation(summary = "查询绑定状态", description = "TV端轮询绑定状态")
    @GetMapping("/bind-status")
    public Result<BindStatusResponse> getBindStatus(
            @Parameter(description = "设备Token") @RequestParam String deviceToken) {
        return Result.success(deviceService.getBindStatus(deviceToken));
    }

    @Operation(summary = "设备心跳", description = "TV端上报心跳")
    @PostMapping("/heartbeat")
    public Result<Void> heartbeat(@Valid @RequestBody HeartbeatRequest request) {
        deviceService.heartbeat(request);
        return Result.success();
    }

    @Operation(summary = "设备列表", description = "管理端获取设备列表")
    @GetMapping("/list")
    public Result<PageResult<DeviceVO>> getDeviceList(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "状态筛选") @RequestParam(required = false) Integer status,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(required = false) Long page,
            @Parameter(description = "每页大小") @RequestParam(required = false) Long size) {
        return Result.success(deviceService.getDeviceList(userId, status, keyword, page, size));
    }

    @Operation(summary = "设备详情")
    @GetMapping("/{id}")
    public Result<DeviceVO> getDeviceDetail(
            @Parameter(description = "设备ID") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return Result.success(deviceService.getDeviceDetail(id, userId));
    }

    @Operation(summary = "解绑设备", description = "解除设备与用户的绑定关系")
    @PostMapping("/{id}/unbind")
    public Result<Void> unbindDevice(
            @Parameter(description = "设备ID") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        deviceService.unbindDevice(id, userId);
        return Result.success();
    }

    @Operation(summary = "重命名设备", description = "修改设备名称")
    @PostMapping("/{id}/rename")
    public Result<DeviceVO> renameDevice(
            @Parameter(description = "设备ID") @PathVariable Long id,
            @Valid @RequestBody RenameDeviceRequest request,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return Result.success(deviceService.renameDevice(id, request.getName(), userId));
    }

    @Operation(summary = "设置设备分组", description = "将设备添加到指定分组")
    @PostMapping("/{id}/group")
    public Result<Void> setDeviceGroup(
            @Parameter(description = "设备ID") @PathVariable Long id,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        deviceService.setDeviceGroup(id, groupId, userId);
        return Result.success();
    }
}
