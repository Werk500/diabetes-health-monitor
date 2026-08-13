package com.diabetes.monitor.service.impl;

import com.diabetes.monitor.common.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class SmsServiceImplTest {
    //创建一个 StringRedisTemplate 的假对象（模拟对象）
    @Mock
    private StringRedisTemplate redisTemplate;//(假Redis)
    //模拟两个 Redis Lua 脚本对象
    @Mock(name = "smsSendScript") private DefaultRedisScript<String> smsSendScript;//(假发送脚本)
    @Mock(name = "smsVerifyScript") private DefaultRedisScript<String> smsVerifyScript;//(假校验脚本)
    //创建 SmsServiceImpl 的真实对象，并把上面所有的 @Mock 对象注入进去
    @InjectMocks
    private SmsServiceImpl smsService;//(真实业务对象)

    @Test
    @DisplayName("发送验证码成功")
    void sendCode_success() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(Object[].class)))
                .thenReturn("OK");
        // 不抛异常即通过
        smsService.sendCode("13800000000");
    }

    @Test
    @DisplayName("发送验证码被限流")
    void sendCode_rateLimited() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(Object[].class)))//anyList()参数匹配器：接受任何 List 类型的参数,any()参数匹配器：接受任何参数
                .thenReturn("LIMIT:30");//当上面的方法被调用时，返回字符串 "LIMIT:30"
        //assertThatThrownBy--AssertJ 的断言方法：断言某个操作会抛出异常
        assertThatThrownBy(() -> smsService.sendCode("13800000000"))
                .isInstanceOf(BizException.class)//断言抛出的异常是 BizException 类型
                .hasMessageContaining("30");//断言异常消息中包含 "30" 这个字符串

    }

    @Test
    @DisplayName("发送验证码失败")
    void sendCode_failure() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(Object[].class)))
                .thenReturn("ERROR");
        assertThatThrownBy(() -> smsService.sendCode("13800000000"))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("校验验证码成功")
    void verifyCode_success() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(Object[].class)))
                .thenReturn("OK");
        assertThat(smsService.verifyCode("13800000000", "123456")).isTrue();
    }

    @Test
    @DisplayName("校验验证码失败")
    void verifyCode_failure() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(null);
        assertThat(smsService.verifyCode("13800000000", "123456")).isFalse();
    }

    @Test
    @DisplayName("生成六位数字验证码")
    void generateNumericCode() {
        for (int i = 0; i < 10; i++) {
            String code = smsService.generateNumericCode();
            assertThat(code).matches("\\d{6}");
            int num = Integer.parseInt(code);
            assertThat(num).isBetween(100000, 999999);
        }
    }


}
