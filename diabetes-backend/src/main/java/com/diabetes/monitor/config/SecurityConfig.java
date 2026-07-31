package com.diabetes.monitor.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置类
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity// 开启 @PreAuthorize 等注解，支持方法级别的权限控制
public class SecurityConfig {

    @Resource
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（前后端分离 + JWT 无状态，不需要 CSRF 防护）
                .csrf(AbstractHttpConfigurer::disable)

                // 设置会话管理策略为无状态（使用 JWT 不需要 Session）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置请求授权规则
                .authorizeHttpRequests(auth -> auth
                        // 登录和注册接口放行，无需认证
                        .requestMatchers("/api/user/login", "/api/user/register").permitAll()
                        // Swagger/Knife4j 文档接口放行
                        .requestMatchers("/doc.html", "/swagger-ui/**", "/v3/**").permitAll()
                        // 管理员接口需要 ADMIN 角色才能访问
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 其他 /api/** 接口需要认证（登录后才能访问）
                        .requestMatchers("/api/**").authenticated()
                        // 其他所有请求（如静态资源）都放行
                        .anyRequest().permitAll()
                )

                // 在 UsernamePasswordAuthenticationFilter 之前添加 JWT 认证过滤器
                // 这样请求会先经过 JWT 认证，再进入 Spring Security 的认证流程
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    /**
     * 密码编码器
     * 使用 BCrypt 强哈希加密算法，用于密码加密和验证
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}
