package com.opencast.screencast.common.config;

import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Properties;

/**
 * 内嵌 MQTT Broker 配置
 * <p>
 * 使用 Moquette 实现，替代独立的 EMQX 服务
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "mqtt.embedded.enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddedMqttBroker {

    @Value("${mqtt.embedded.port:1883}")
    private int mqttPort;

    @Value("${mqtt.embedded.host:0.0.0.0}")
    private String mqttHost;

    @Value("${mqtt.embedded.persistent-store:}")
    private String persistentStore;

    private Server server;

    @Bean
    public Server mqttBroker() throws IOException {
        server = new Server();

        Properties props = new Properties();
        props.setProperty("port", String.valueOf(mqttPort));
        props.setProperty("host", mqttHost);
        props.setProperty("allow_anonymous", "true");

        // 配置持久化存储（可选）
        if (persistentStore != null && !persistentStore.isEmpty()) {
            props.setProperty("persistent_store", persistentStore);
        }

        MemoryConfig config = new MemoryConfig(props);

        server.startServer(config);
        log.info("Embedded MQTT Broker started on {}:{}", mqttHost, mqttPort);

        return server;
    }

    @PreDestroy
    public void shutdown() {
        if (server != null) {
            server.stopServer();
            log.info("Embedded MQTT Broker stopped");
        }
    }
}
