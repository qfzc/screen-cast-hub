package com.opencast.screencast;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智屏管理系统启动类
 */
@SpringBootApplication
@EnableScheduling
@MapperScan({
    "com.opencast.screencast.mapper",
})
public class ScreencastApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScreencastApplication.class, args);
    }

}
