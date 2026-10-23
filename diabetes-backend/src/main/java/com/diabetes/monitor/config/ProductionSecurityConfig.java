package com.diabetes.monitor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("prod")
public class ProductionSecurityConfig {

    /**
     * 生产环境不暴露接口文档及其静态资源。
     */
    @Bean
    @Order(0)
    SecurityFilterChain productionDocsSecurity(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(
                        "/doc.html",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/**",
                        "/webjars/**"
                )
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().denyAll())
                .build();
    }
}
