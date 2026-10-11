package com.diabetes.monitor.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        //key 用String序列化
        redisTemplate.setKeySerializer(new StringRedisSerializer());//StringRedisSerializer()将 Key 序列化为普通字符串

        //Value用JSON序列化
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // ⭐ 缓存对象里可能带 LocalDateTime（如指标结果的统计窗口），必须注册 JSR310 模块。
        // 否则 GenericJackson2JsonRedisSerializer 自带的默认 ObjectMapper 会抛
        // "Java 8 date/time type `java.time.LocalDateTime` not supported by default"，
        // 而写缓存的异常通常被 try/catch 吞掉 —— 表现为"缓存永远写不进去、只能靠日志发现"。
        ObjectMapper cacheMapper = new ObjectMapper();
        cacheMapper.registerModule(new JavaTimeModule());//让 Jackson 认识 LocalDateTime 等 JSR310 类型
        cacheMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);//日期写成 ISO 字符串（2026-10-11T00:00:00），而非时间戳数字，可读性更好
        // 保留 GenericJackson2JsonRedisSerializer 原本的默认类型信息（@class），
        // 否则反序列化无法还原具体类型；已有缓存键的数据格式保持不变。
        cacheMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(cacheMapper);//使用 Jackson 将对象序列化为 JSON 格式
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        //校验配置、初始化内部状态
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }


    @Bean
    public DefaultRedisScript<String> smsSendScript() {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/sms_send.lua"));
        script.setResultType(String.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<String> smsVerifyScript() {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/sms_verify.lua"));
        script.setResultType(String.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> aiRateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/ai_rate_limit.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
