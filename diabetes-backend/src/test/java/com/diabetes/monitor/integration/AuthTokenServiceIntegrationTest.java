package com.diabetes.monitor.integration;

import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.config.JwtUtil;
import com.diabetes.monitor.dto.TokenPair;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.service.AuthTokenService;
import com.diabetes.monitor.service.SysUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Refresh Token 并发与吊销")
public class AuthTokenServiceIntegrationTest {

    private static final Integer USER_ID = 2;

    @Resource
    private AuthTokenService authTokenService;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("同一个 Refresh Token 并发轮换，只能成功一次")
    void rotate_sameTokenConcurrently_onlyOneSucceeds() throws Exception {
        SysUser user = sysUserService.getById(USER_ID);
        TokenPair original = authTokenService.issue(user);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicReference<TokenPair> winner = new AtomicReference<>();

        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger successCount = new AtomicInteger();
            AtomicInteger rejectedCount = new AtomicInteger();
            AtomicInteger errorCount = new AtomicInteger();

            Runnable rotateTask = () -> {
                ready.countDown();
                try {
                    start.await(5, TimeUnit.SECONDS);
                    TokenPair next = authTokenService.rotate(original.getRefreshToken());
                    winner.set(next);
                    successCount.incrementAndGet();
                } catch (BizException e) {
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            };

            Future<?> first = pool.submit(rotateTask);
            Future<?> second = pool.submit(rotateTask);

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);

            assertThat(successCount.get()).isEqualTo(1);
            assertThat(rejectedCount.get()).isEqualTo(1);
            assertThat(errorCount.get()).isZero();
            assertThat(winner.get()).isNotNull();// 成功者确实拿到了新 TokenPair
        } finally {
            pool.shutdownNow();
            authTokenService.revoke(original.getRefreshToken());

            TokenPair winningPair = winner.get();
            if (winningPair != null) {
                authTokenService.revoke(winningPair.getRefreshToken());
            }
        }
    }

    @Test
    @DisplayName("revokeAll 后旧 Refresh Token 失效，新登录 Token 可以继续刷新")
    void revokeAll_invalidatesOldSessionButAllowsNewLogin() {
        SysUser user = sysUserService.getById(USER_ID);
        TokenPair oldPair = authTokenService.issue(user);

        authTokenService.revokeAll(USER_ID);

        assertThatThrownBy(() -> authTokenService.rotate(oldPair.getRefreshToken()))
                .isInstanceOf(BizException.class);

        TokenPair newPair = authTokenService.issue(user);
        TokenPair rotated = authTokenService.rotate(newPair.getRefreshToken());

        try {
            assertThat(rotated.getAccessToken()).isNotBlank();
            assertThat(rotated.getRefreshToken()).isNotBlank();
            assertThat(rotated.getRefreshToken()).isNotEqualTo(newPair.getRefreshToken());
        } finally {
            authTokenService.revoke(rotated.getRefreshToken());
            authTokenService.revoke(newPair.getRefreshToken());
        }
    }

    @Test
    @DisplayName("Refresh Token 版本落后时不能继续轮换")
    void rotate_rejectsStaleTokenVersion() {
        SysUser user = sysUserService.getById(USER_ID);
        TokenPair pair = authTokenService.issue(user);

        // 模拟 revokeAll 只完成了版本递增、旧 Token Key 尚未清理的并发窗口
        stringRedisTemplate.opsForValue()
                .increment("auth:user-version:" + USER_ID);

        assertThatThrownBy(() -> authTokenService.rotate(pair.getRefreshToken()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("失效");

        authTokenService.revoke(pair.getRefreshToken());
    }

    @Test
    @DisplayName("Access Token 可用于认证，Refresh Token 不能冒充 Access Token")
    void onlyAccessTokenPassesJwtValidation() {
        SysUser user = sysUserService.getById(USER_ID);
        TokenPair pair = authTokenService.issue(user);

        try {
            assertThat(jwtUtil.validateAccessToken(pair.getAccessToken())).isTrue();
            assertThat(jwtUtil.validateAccessToken(pair.getRefreshToken())).isFalse();
        } finally {
            authTokenService.revoke(pair.getRefreshToken());
        }
    }
}
