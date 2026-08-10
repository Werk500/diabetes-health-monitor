package com.diabetes.monitor.service;

import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.dto.LoginDTO;
import com.diabetes.monitor.entity.SysUser;
import org.springframework.security.core.Authentication;

public interface UserService {
    Result login(LoginDTO loginDTO);

    Result register(SysUser user);

    Result info(Integer id);

    Result update(SysUser user);

    Result list();

    Result toggleStatus(Integer id);

    Result articleList(Integer page, Integer size, Authentication authentication);
}
