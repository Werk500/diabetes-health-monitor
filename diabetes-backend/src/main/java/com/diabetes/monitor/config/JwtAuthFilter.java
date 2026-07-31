package com.diabetes.monitor.config;


import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 每个请求进来时解析 JWT，设置 SecurityContext
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Resource
    private JwtUtil jwtUtil;


    /**
     * 过滤器内部处理逻辑
     * @param request HTTP请求
     * @param response HTTP响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 第一步：从请求头中获取 Authorization 头
        String authHeader = request.getHeader("Authorization");

        // 第二步：如果没有 Authorization 头或者不以 "Bearer " 开头，直接放行
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 第三步：截取 token 字符串（去掉 "Bearer " 前缀）
        String token = authHeader.substring(7);

        // 第四步：验证 token 是否有效，无效则放行（不设置认证信息）
        if (!jwtUtil.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 第五步：从 token 中提取用户信息
            Integer userId = jwtUtil.getUserId(token);        // 获取用户ID
            String username = jwtUtil.getUsername(token);    // 获取用户名
            String role = jwtUtil.getRole(token);            // 获取角色（如 ROLE_USER 或 ROLE_ADMIN）

            // 第六步：构造 Spring Security 认证令牌
            // principal：使用 userId 作为主体标识
            // credentials：设置为 null（JWT 无需密码）
            // authorities：将角色转换为 GrantedAuthority 权限列表
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,                                  // 主体：用户ID
                            null,                                    // 凭证：null
                            Collections.singletonList(new SimpleGrantedAuthority(role)) // 权限列表
                    );

            // 第七步：将认证信息设置到 SecurityContext 中
            // 这样后续的请求处理就能获取到当前登录用户的信息
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // 解析 token 失败时，记录警告日志，不设置认证信息
            // 让请求继续执行，但未认证的用户无法访问受保护的资源
            logger.warn("解析 JWT Token 失败: " + e.getMessage());
        }

        // 第八步：继续执行后续过滤器链
        filterChain.doFilter(request, response);


    }
}
