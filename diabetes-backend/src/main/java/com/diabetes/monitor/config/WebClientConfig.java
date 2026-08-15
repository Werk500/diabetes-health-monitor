package com.diabetes.monitor.config;


import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    private static final String DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com";
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int RESPONSE_TIMEOUT_SECONDS = 120;

    @Bean
    public WebClient dashScopeWebClient() {
        //配置httpClient(底层网络层)
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)//设置连接超时
                .responseTimeout(Duration.ofSeconds(RESPONSE_TIMEOUT_SECONDS))//设置整个请求的总超时时间。
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)));

        //构建WebClient
        return WebClient.builder()
                .baseUrl(DASHSCOPE_BASE_URL)//设置基础 URL
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))//把前面配置好的 HttpClient 塞进 WebClient。
                .build();

    }
}
