package com.opencast.screencast.common.config;

import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * MQTT 客户端配置类
 * <p>
 * 连接到内嵌的 Moquette Broker 或外部 MQTT Broker
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "mqtt")
public class MqttConfig {

    private String brokerUrl;
    private String clientId;
    private String username;
    private String password;

    private MqttClient mqttClient;

    /**
     * 创建 MQTT 客户端
     * <p>
     * DependsOn 确保内嵌 Broker 先启动
     */
    @Bean
    @DependsOn("mqttBroker")
    public MqttClient mqttClient() throws MqttException {
        mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);
        options.setAutomaticReconnect(true);

        if (username != null && !username.isEmpty()) {
            options.setUserName(username);
        }
        if (password != null && !password.isEmpty()) {
            options.setPassword(password.toCharArray());
        }

        mqttClient.connect(options);
        log.info("MQTT Client connected to {}", brokerUrl);

        return mqttClient;
    }

    @PreDestroy
    public void shutdown() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
                log.info("MQTT Client disconnected");
            } catch (MqttException e) {
                log.error("Error closing MQTT client", e);
            }
        }
    }
}
