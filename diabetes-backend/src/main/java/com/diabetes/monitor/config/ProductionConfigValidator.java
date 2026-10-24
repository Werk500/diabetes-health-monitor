package com.diabetes.monitor.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 生产环境启动时尽早拒绝示例密钥和错误 CORS 配置。
 */
@Component
@Profile("prod")
public class ProductionConfigValidator {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.auth.refresh-cookie-secure:true}")
    private boolean refreshCookieSecure;

    @PostConstruct
    void validate() {
        if (jwtSecret.length() < 32
                || jwtSecret.contains("diabetes-monitor-jwt-secret")) {
            throw new IllegalStateException(
                    "生产环境 JWT_SECRET 必须是至少32位且不能使用示例值"
            );
        }

        if (!dashScopeApiKey.startsWith("sk-")
                || dashScopeApiKey.contains("replace-with")) {
            throw new IllegalStateException(
                    "生产环境必须配置有效的 SPRING_AI_DASHSCOPE_API_KEY"
            );
        }

        // HTTPS 生产入口必须使用正式域名；本地 Kubernetes HTTP 验收可显式关闭 Secure。
        if (allowedOrigins.contains("your-domain")
                || (refreshCookieSecure && allowedOrigins.contains("localhost"))) {
            throw new IllegalStateException(
                    "HTTPS 生产环境 CORS_ALLOWED_ORIGINS 必须配置真实域名"
            );
        }
    }
}
