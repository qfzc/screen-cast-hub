package com.opencast.screencast.common.service;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * MQTT 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true")
public class MqttService {

    private final MqttClient mqttClient;

    /**
     * 发布消息到指定主题
     */
    public void publish(String topic, Object payload) {
        try {
            String json = JSONUtil.toJsonStr(payload);
            MqttMessage message = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            message.setRetained(false);

            if (!mqttClient.isConnected()) {
                log.warn("MQTT client not connected, attempting to reconnect...");
                mqttClient.reconnect();
                Thread.sleep(1000); // 等待连接
            }

            mqttClient.publish(topic, message);
            log.info("Published message to topic: {}", topic);
        } catch (MqttException e) {
            log.error("Failed to publish message to topic: {}", topic, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for MQTT connection", e);
        }
    }

    /**
     * 发布任务到设备
     */
    public void publishTask(String deviceToken, Object task) {
        String topic = "device/" + deviceToken + "/task";
        publish(topic, task);
    }

    /**
     * 发布批量任务到设备
     */
    public void publishBatchTask(String deviceToken, Object batchTask) {
        String topic = "device/" + deviceToken + "/batch-task";
        publish(topic, batchTask);
    }

    /**
     * 发布命令到设备
     */
    public void publishCommand(String deviceToken, String command) {
        String topic = "device/" + deviceToken + "/command";
        publish(topic, new CommandMessage(command, System.currentTimeMillis()));
    }

    /**
     * 发布刷新命令
     */
    public void publishRefreshCommand(String deviceToken) {
        publishCommand(deviceToken, "refresh");
    }

    /**
     * 发布暂停命令
     */
    public void publishPauseCommand(String deviceToken) {
        publishCommand(deviceToken, "pause");
    }

    /**
     * 发布恢复命令
     */
    public void publishResumeCommand(String deviceToken) {
        publishCommand(deviceToken, "resume");
    }

    /**
     * 发布播放列表更新到设备
     */
    public void publishPlaylistUpdate(String deviceToken, Object playlist) {
        String topic = "device/" + deviceToken + "/playlist/update";
        publish(topic, playlist);
        log.info("Playlist update pushed to device: {}", deviceToken);
    }

    /**
     * 命令消息
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class CommandMessage {
        private String command;
        private long timestamp = System.currentTimeMillis();
    }
}
