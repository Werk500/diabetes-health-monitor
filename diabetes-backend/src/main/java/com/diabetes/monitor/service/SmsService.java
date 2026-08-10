package com.diabetes.monitor.service;

public interface SmsService {
    /**
     * 发送短信验证码
     * @param phone 手机号
     */
    void sendCode(String phone);

    /**
     * 校验短信验证码
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @return true=校验通过, false=验证码错误或已过期
     */
    boolean verifyCode(String phone, String code);
}