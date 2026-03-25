package com.opencast.screencast.task;

import com.opencast.screencast.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 设备状态定时任务
 * 定期检查设备心跳，将超时设备标记为离线
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceStatusScheduler {

    private final DeviceService deviceService;

    /**
     * 每分钟检查一次设备在线状态
     */
    @Scheduled(fixedRate = 60000)
    public void checkDeviceStatus() {
        log.debug("开始检查设备在线状态...");
        deviceService.updateOfflineStatus();
    }
}