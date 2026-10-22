package com.diabetes.monitor.service.impl;


import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.ResultCode;
import com.diabetes.monitor.config.JwtUtil;
import com.diabetes.monitor.dto.TokenPair;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.service.AuthTokenService;
import com.diabetes.monitor.service.SysUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class AuthTokenServiceImpl implements AuthTokenService {

    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";
    private static final String USER_REFRESH_KEY_PREFIX = "auth:user-refresh:";
    private static final String USER_VERSION_KEY_PREFIX =
            "auth:user-version:";


    //提供一个密码学安全的随机数生成器。
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    //把随机的 32 字节二进制安全地转成可放进 HTTP / URL / JSON 的字符串。
    private static final Base64.Encoder TOKEN_ENCODER =
            Base64.getUrlEncoder().withoutPadding();

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SysUserService sysUserService;

    /**
     * 原子消费脚本：GET + DEL。
     * 并发刷新同一旧 token 时，只有一个请求能拿到 userId，另一个拿到 null，
     * 从而保证轮换的原子性，防止同一 refresh token 被使用两次。
     */
    @Resource
    private DefaultRedisScript<String> refreshTokenConsumeScript;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private long getTokenVersion(Integer userId) {
        String value = stringRedisTemplate.opsForValue()
                .get(USER_VERSION_KEY_PREFIX + userId);

        return value == null ? 0L : Long.parseLong(value);
    }

    private String userVersionKey(Integer userId) {
        return USER_VERSION_KEY_PREFIX + userId;
    }

    /**
     * 登录成功后签发一对 Token。
     */
    @Override
    public TokenPair issue(SysUser user) {

        if(user == null || !Integer.valueOf(1).equals(user.getStatus())){
            throw new BizException(ResultCode.UNAUTHORIZED, "用户不存在或已被禁用");
        }

        long version = getTokenVersion(user.getId());

        String refreshToken = generateRefreshToken();
        String tokenHash = sha256(refreshToken);

        String payload = user.getId() + ":" + version;

        stringRedisTemplate.opsForValue().set(
                refreshKey(tokenHash),
                payload,
                Duration.ofMillis(refreshExpiration)
        );


        stringRedisTemplate.opsForSet().add(
                userRefreshKey(user.getId()),
                tokenHash
        );

        stringRedisTemplate.expire(
                userRefreshKey(user.getId()),
                Duration.ofMillis(refreshExpiration)
        );


        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                version
        );

        return TokenPair.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessExpiration / 1000)
                .build();
    }

    @Override
    public TokenPair rotate(String refreshToken) {
        if(refreshToken == null || refreshToken.isBlank()) {
            throw new BizException(ResultCode.UNAUTHORIZED, "刷新令牌不能为空");
        }

        String oldHash = sha256(refreshToken);

        String payload = stringRedisTemplate.execute(
                refreshTokenConsumeScript,
                List.of(refreshKey(oldHash))
        );

        if (payload == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "刷新令牌无效或已过期");
        }

        RefreshPayload refreshPayload = parseRefreshPayload(payload);
        Integer userId = refreshPayload.userId();

        stringRedisTemplate.opsForSet().remove(
                userRefreshKey(userId),
                oldHash
        );

        long currentVersion = getTokenVersion(userId);
        if (refreshPayload.version() != currentVersion) {
            throw new BizException(
                    ResultCode.UNAUTHORIZED,
                    "刷新令牌已失效，请重新登录"
            );
        }

        SysUser user = sysUserService.getById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户不存在或已被禁用");
        }

        return issue(user);
    }

    /**
     * 主动吊销一个 refreshToken（登出场景）。
     * 不存在时静默成功——登出接口应保证幂等，避免重复登出报错。
     */
    @Override
    public void revoke(String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        String tokenHash = sha256(refreshToken);
        String payload = stringRedisTemplate.execute(
                refreshTokenConsumeScript,
                List.of(refreshKey(tokenHash))
        );

        if (payload != null) {
            RefreshPayload refreshPayload = parseRefreshPayload(payload);

            stringRedisTemplate.opsForSet().remove(
                    userRefreshKey(refreshPayload.userId()),
                    tokenHash
            );
        }

    }

    /**
     * 吊销某用户全部 refreshToken（改密、强制下线等场景）。
     *
     * <p>按用户 Set 拿到全部 hash，批量删单 token 凭据，再删用户 Set 本身。</p>
     */
    @Override
    public void revokeAll(Integer userId) {

        stringRedisTemplate.opsForValue()
                .increment(userVersionKey(userId));

        String userKey = userRefreshKey(userId);
        Set<String> hashes = stringRedisTemplate.opsForSet().members(userKey);

        if (hashes == null || hashes.isEmpty()) {
            return;
        }

        List<String> refreshKeys = hashes.stream()
                .map(this::refreshKey)
                .toList();

        stringRedisTemplate.delete(refreshKeys);
        stringRedisTemplate.delete(userKey);

    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return TOKEN_ENCODER.encodeToString(bytes);
    }

    /**
     * 把用户拿到的 Refresh Token 原文转成 hash，存进 Redis
     * @param value
     * @return
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String refreshKey(String tokenHash) {
        return REFRESH_KEY_PREFIX + tokenHash;
    }

    private String userRefreshKey(Integer userId) {
        return USER_REFRESH_KEY_PREFIX + userId;
    }

    private record RefreshPayload(Integer userId, long version) {}

    private RefreshPayload parseRefreshPayload(String value){
        int separator = value.indexOf(":");

        if(separator<= 0|| separator == value.length()-1){
            throw new BizException(
                    ResultCode.UNAUTHORIZED,
                    "刷新令牌数据异常"
            );
        }

        try {
            Integer userId = Integer.valueOf(value.substring(0, separator));
            long version = Long.parseLong(value.substring(separator + 1));
            return new RefreshPayload(userId, version);
        } catch (NumberFormatException e) {
            throw new BizException(
                    ResultCode.UNAUTHORIZED,
                    "刷新令牌数据异常"
            );
        }
    }
}
