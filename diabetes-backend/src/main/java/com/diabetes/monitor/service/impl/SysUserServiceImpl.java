package com.diabetes.monitor.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.mapper.SysUserMapper;
import com.diabetes.monitor.service.SysUserService;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public SysUser login(String username, String password) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username)
               .eq(SysUser::getPassword, SecureUtil.md5(password))
               .eq(SysUser::getStatus, 1);
        return getOne(wrapper);
    }

    @Override
    public SysUser register(SysUser user) {
        user.setPassword(SecureUtil.md5(user.getPassword()));
        save(user);
        return user;
    }
}
