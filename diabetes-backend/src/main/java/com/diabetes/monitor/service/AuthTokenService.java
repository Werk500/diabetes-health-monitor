package com.diabetes.monitor.service;

import com.diabetes.monitor.dto.TokenPair;
import com.diabetes.monitor.entity.SysUser;

public interface AuthTokenService {
    /**
     * 登录成功后签发一对 Token。
     */
    TokenPair issue(SysUser user);
    TokenPair rotate(String refreshToken);
    void revoke(String refreshToken);
    void revokeAll(Integer userId);
}
