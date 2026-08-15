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
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

/**
 * Spring Security 安全配置类
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Resource
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（前后端分离 + JWT 无状态，不需要 CSRF 防护）
                /**
                 * CSRF 是什么：跨站请求伪造攻击，恶意网站冒充用户发请求。
                 * 为什么禁用：项目用 JWT，每次请求都带 Token，服务器不依赖 Cookie，天然防 CSRF。
                 */
                .csrf(AbstractHttpConfigurer::disable)

                //显示禁用HTTP Basic 认证
                /**
                 * HTTP Basic 是什么：浏览器弹出一个小框让你输入用户名密码，每次请求都带着"用户名:密码"的 Base64 编码。
                 * 为什么禁用：不安全（密码可解码），而且用 JWT，不需要这种认证方式。
                 */
                .httpBasic(AbstractHttpConfigurer::disable)

                //显示禁用表单登录
                /**
                 * 表单登录是什么：Spring Security 默认提供 /login 页面，用户在页面上输入用户名密码登录。
                 * 为什么禁用：是前后端分离，前端是 Vue，不需要后端渲染登录页面。如果不禁用，访问未认证的接口可能会被重定向到 /login，返回 302 而不是 401。
                 */
                .formLogin(AbstractHttpConfigurer::disable)

                // 设置会话管理策略为无状态（使用 JWT 不需要 Session）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 使用 RequestAttributeSecurityContextRepository 解决 SSE 异步分派时 SecurityContext 丢失问题
                /**
                 * 这个配置解决什么问题：项目有 SSE（流式响应），SSE 是在另一个线程里执行的。默认的 SecurityContext 在线程间传递可能会丢失，导致认证信息找不到。
                 * RequestAttributeSecurityContextRepository：把 SecurityContext 存在请求属性里，确保在 SSE 这种异步场景下也能找到认证信息。
                 */
                .securityContext(securityContext ->
                        securityContext.securityContextRepository(new RequestAttributeSecurityContextRepository()))

                // 配置请求授权规则
                .authorizeHttpRequests(auth -> auth
                        // 登录、注册、短信验证码发送接口放行，无需认证
                        .requestMatchers("/api/user/login", "/api/user/register", "/api/user/sms/send","/api/user/sms/login","/api/user/public-key").permitAll()
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
                /**
                 * addFilterBefore：在 UsernamePasswordAuthenticationFilter 之前添加这个过滤器
                 * 为什么加在这里：Spring Security 默认的 UsernamePasswordAuthenticationFilter 会处理表单登录，JWT 过滤器要在它之前执行，这样如果能从 JWT 拿到用户信息，就跳过表单登录逻辑
                 */
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器
     * 使用 BCrypt 强哈希加密算法，用于密码加密和验证
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}