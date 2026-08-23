package com.diabetes.monitor.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.diabetes.monitor.entity.SysUser;

public interface SysUserService extends IService<SysUser> {
    SysUser login(String username, String password);
    SysUser register(SysUser user);
}
