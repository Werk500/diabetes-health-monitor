package com.diabetes.monitor.service.impl;

import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.service.SmsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource(name = "smsSendScript")
    private DefaultRedisScript<String> smsSendScript;

    @Resource(name = "smsVerifyScript")
    private DefaultRedisScript<String> smsVerifyScript;

    private static final int CODE_TTL = 300;           // 验证码有效期 5分钟
    private static final int LIMIT_TTL = 60;           // 频率限制 60秒

    private final SecureRandom random = new SecureRandom();

    /**
     * 发送短信验证码
     * @param phone 手机号
     */
    @Override
    public void sendCode(String phone) {
        String limitKey = "sms:limit:" + phone;
        String codeKey = "sms:code:" + phone;
        String code = generateNumericCode();

        String codeTTLStr = String.valueOf(CODE_TTL);   // 转为 String
        String limitTTLStr = String.valueOf(LIMIT_TTL); // 转为 String

        String executed = redisTemplate.execute(smsSendScript,
                Arrays.asList(limitKey, codeKey),
                code, codeTTLStr, limitTTLStr);
        if("OK".equals(executed)){
           log.info("验证码发送成功");
        }else if(executed.startsWith("LIMIT:")){
            String ttl = executed.substring(6);

            throw new BizException("操作频繁，请等待 " + ttl + " 秒后重试");
        }else{
            throw new BizException("验证码发送失败，请稍后重试");
        }



    }

    /**
     * 校验短信验证码
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @return true=校验通过, false=验证码错误或已过期
     */
    @Override
    public boolean verifyCode(String phone, String code) {

        String result = redisTemplate.execute(smsVerifyScript,
                Collections.singletonList("sms:code:" + phone),
                code.trim());
        return "OK".equals(result);
    }

    /**
     * 生成6位数字验证码
     */
    public String generateNumericCode() {

        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
