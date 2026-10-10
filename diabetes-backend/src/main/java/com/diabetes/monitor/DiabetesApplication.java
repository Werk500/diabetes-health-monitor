package com.diabetes.monitor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.diabetes.monitor.mapper")
@EnableScheduling   // 没有它 @Scheduled 不会生效（日聚合任务依赖它）
public class DiabetesApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiabetesApplication.class, args);
    }
}
