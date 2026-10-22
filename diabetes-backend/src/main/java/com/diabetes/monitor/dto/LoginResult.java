package com.diabetes.monitor.dto;

import com.diabetes.monitor.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResult {
    /** 已脱敏的用户信息 */
    private SysUser user;

    /** Access Token + Refresh Token */
    private TokenPair tokens;


}
