package com.diabetes.monitor.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.ResultCode;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.mapper.SysUserMapper;
import com.diabetes.monitor.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public SysUser login(String username, String password) {

        // 1. 参数校验
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new BizException(ResultCode.BAD_REQUEST, "用户名或密码不能为空");
        }

        // 2. 查询用户（不包含密码条件）
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = getOne(wrapper);

        // 3. 统一错误信息（防止用户名枚举）
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        String lockKey = "login:lock:" + username;
        Integer failCount = (Integer) redisTemplate.opsForValue().get(lockKey);
        if (failCount != null && failCount >= 5) {
            throw new BizException(ResultCode.TOO_MANY_REQUESTS, "账户已被锁定，请30分钟后重试");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            // 记录失败次数
            redisTemplate.opsForValue().increment(lockKey);
            redisTemplate.expire(lockKey, 30, TimeUnit.MINUTES);
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        //登录成功，清除失败记录
        redisTemplate.delete(lockKey);

        return user;

    }

    @Override
    public SysUser register(SysUser user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        save(user);
        return user;
    }
}
