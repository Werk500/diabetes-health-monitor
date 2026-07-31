package com.diabetes.monitor.config;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 的生成、解析、校验
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        //将字符串密钥转换为SecretKey
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    //1.生成token
    public String generateToken(Integer userId,String userName,Integer role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username",userName)
                .claim("role","ROLE_" + (role  == 1 ? "ADMIN" : "USER"))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    // 2. 解析 Claims（私有方法）
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 3. 获取用户ID
    public Integer getUserId(String token) {
        return Integer.parseInt(parseClaims(token).getSubject());
    }

    // 4. 获取用户名
    public String getUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    // 5. 获取角色
    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // 6. 判断 token 是否过期
    public boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    // 7. 验证 token
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

}
