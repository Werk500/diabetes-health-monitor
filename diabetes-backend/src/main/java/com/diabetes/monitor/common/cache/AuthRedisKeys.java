package com.diabetes.monitor.common.cache;

public class AuthRedisKeys {
    private AuthRedisKeys() {
    }

    public static String userVersion(Integer userId) {
        return "auth:user-version:" + userId;
    }
}
