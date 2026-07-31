package com.diabetes.monitor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.diabetes.monitor.mapper")
public class DiabetesApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiabetesApplication.class, args);
    }
}