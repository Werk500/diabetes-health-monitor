package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.config.JwtUtil;
import com.diabetes.monitor.dto.LoginDTO;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.service.SysUserService;
import com.diabetes.monitor.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public Result login(LoginDTO loginDTO) {
        SysUser user = sysUserService.login(loginDTO.getUsername(), loginDTO.getPassword());
        if (user == null) throw new BizException("用户名或密码错误");;
        Map<String, Object> data = new HashMap<>();
        data.put("token", jwtUtil.generateToken(user.getId(), user.getUsername(),  user.getRole()));
        data.put("user", user);

        return Result.ok(data);
    }

    @Override
    public Result register(SysUser user) {
        long count = sysUserService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) throw new BizException("用户名已存在");;
        sysUserService.register(user);
        return Result.ok("register success");
    }

    @Override
    public Result info(Integer id) {
        SysUser user = sysUserService.getById(id);
        if (user != null) user.setPassword(null);
        return Result.ok(user);
    }

    @Override
    public Result update(SysUser user) {
        user.setPassword(null);
        sysUserService.updateById(user);
        return Result.ok("update success");
    }

    @Override
    public Result list() {
        List<SysUser> users = sysUserService.list();
        users.forEach(u -> u.setPassword(null));

        return Result.ok(users);
    }

    @Override
    public Result toggleStatus(Integer id) {
        SysUser user = sysUserService.getById(id);
        if (user != null) {
            user.setStatus(user.getStatus() == 1 ? 0 : 1);
            sysUserService.updateById(user);
        }
        return Result.ok();
    }
}