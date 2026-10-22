package com.diabetes.monitor.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenPair {
    /** 访问令牌（JWT） */
    private String accessToken;

    /** 刷新令牌（随机串，非 JWT，不透出用户信息） */
    private String refreshToken;

    /** accessToken 剩余有效期，单位：秒（供前端安排静默刷新时机） */
    private long expiresIn;
}
