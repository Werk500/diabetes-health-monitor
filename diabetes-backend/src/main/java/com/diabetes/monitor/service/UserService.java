package com.diabetes.monitor.service;

import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.dto.ChangePasswordDTO;
import com.diabetes.monitor.dto.LoginDTO;
import com.diabetes.monitor.dto.LoginResult;
import com.diabetes.monitor.dto.ProfileUpdateDTO;
import com.diabetes.monitor.entity.SysUser;
import org.springframework.security.core.Authentication;

public interface UserService {
    LoginResult login(LoginDTO loginDTO);

    Result register(SysUser user);

    Result info(Integer id);

    Result changePassword(Integer userId, ChangePasswordDTO dto);

    Result list();

    Result toggleStatus(Integer id);

    Result articleList(Integer page, Integer size, Authentication authentication);

    Result updateProfile(Integer id, ProfileUpdateDTO dto);
}
