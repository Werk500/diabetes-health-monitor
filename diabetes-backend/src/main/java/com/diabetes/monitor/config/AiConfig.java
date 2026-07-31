package com.diabetes.monitor.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Component
public class AiConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Value("${spring.ai.dashscope.model}")
    private String model;

    @Value("${ai.temperature:0.7}")
    private Double temperature;


    public String getApiKey() {
        return apiKey;
    }

    public  String getModel() {
        return model;
    }
    public  Double getTemperature() {
        return temperature;
    }



}
