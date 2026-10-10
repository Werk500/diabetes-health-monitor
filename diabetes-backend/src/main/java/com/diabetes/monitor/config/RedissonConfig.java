package com.diabetes.monitor.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Redisson 客户端：与现有 Lettuce 的 RedisTemplate 并存，只用来提供分布式锁。
 * 连接参数复用 spring.data.redis.*，避免同一份连接信息写两处。
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        config.useSingleServer()
                .setAddress("redis://"+host + ":" + port)
                .setDatabase(database)
                .setPassword(password.isBlank() ? null : password)
                .setConnectionMinimumIdleSize(2)//最小空闲连接数
                .setConnectionPoolSize(8)//连接池上限
                .setConnectTimeout(3000)//建立连接超时
                .setTimeout(3000);//命令执行超时

        return Redisson.create(config);
    }
}
