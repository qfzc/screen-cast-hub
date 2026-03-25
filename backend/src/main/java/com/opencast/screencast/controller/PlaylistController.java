package com.opencast.screencast.controller;

import com.opencast.screencast.common.result.Result;
import com.opencast.screencast.dto.device.DevicePlaylistRequest;
import com.opencast.screencast.dto.device.DevicePlaylistVO;
import com.opencast.screencast.service.PlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 播放列表控制器
 */
@Tag(name = "播放列表管理")
@RestController
@RequestMapping("/api/v1/device/{deviceId}/playlist")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @Operation(summary = "获取设备播放列表")
    @GetMapping
    public Result<DevicePlaylistVO> getPlaylist(@PathVariable Long deviceId) {
        return Result.success(playlistService.getPlaylist(deviceId));
    }

    @Operation(summary = "更新设备播放列表并推送到TV端")
    @PutMapping
    public Result<Void> updatePlaylist(
            @PathVariable Long deviceId,
            @RequestBody DevicePlaylistRequest request) {
        playlistService.updatePlaylist(deviceId, request);
        return Result.success();
    }
}
