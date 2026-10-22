package com.diabetes.monitor.service;

import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.dto.ChangePasswordDTO;
import com.diabetes.monitor.dto.LoginDTO;
import com.diabetes.monitor.dto.LoginResult;
import com.diabetes.monitor.entity.SysUser;
import org.springframework.security.core.Authentication;

public interface UserService {
    LoginResult login(LoginDTO loginDTO);

    Result register(SysUser user);

    Result info(Integer id);

    Result update(SysUser user);

    Result changePassword(Integer userId, ChangePasswordDTO dto);

    Result list();

    Result toggleStatus(Integer id);

    Result articleList(Integer page, Integer size, Authentication authentication);
}
