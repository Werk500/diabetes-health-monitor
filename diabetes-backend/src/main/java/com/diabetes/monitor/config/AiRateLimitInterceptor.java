package com.diabetes.monitor.config;

import cn.hutool.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiRateLimitInterceptor implements HandlerInterceptor {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource(name = "aiRateLimitScript")
    private DefaultRedisScript<Long> aiRateLimitScript;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${spring.ai.rate-limit.limit}")
    private int limit;

    @Value("${spring.ai.rate-limit.window-seconds}")
    private int windowSeconds;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        //跳过OPTIONS 请求
        if("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return  true;
        }

        // 从 JWT Token 中获取当前登录用户的 ID。
        Integer userId = getCurrentUserId();
        if(userId == null) {
            response.setStatus(HttpStatus.HTTP_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> error = new HashMap<>();
            error.put("code", 401);
            error.put("msg", "用户未登录");
            error.put("data", null);
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return false;
        }

        //构建Redis key
        String requestURI = request.getRequestURI();
        String key = "ai:rate:" + userId + ":" + requestURI;

        //执行脚本
        List<String> keys = List.of(key);
        Long remaining = stringRedisTemplate.execute(
                        aiRateLimitScript,
                        keys,
                        String.valueOf(limit),
                        String.valueOf(windowSeconds));

        //remaining = -1 表示超出限流
        if(remaining == -1L) {
            response.setStatus(HttpStatus.HTTP_TOO_MANY_REQUESTS);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> error = new HashMap<>();
            error.put("code", 429);
            error.put("msg", "请求过于频繁，请稍后再试");
            error.put("data", null);
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return false;
        }

        // 设置响应头
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining",
                String.valueOf(Math.max(0, limit-remaining.intValue())));
        return true;
    }

    /**
     *  从 Spring Security 的上下文中获取当前登录用户的 ID。
     * @return
     */
    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Integer) {
            return (Integer) principal;
        }
        return null;
    }


}
