package com.diabetes.monitor.common.security;

import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public Integer id(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null||!(authentication.getPrincipal() instanceof Integer userId)){
            throw new BizException(ResultCode.UNAUTHORIZED, "未登录");
        }

        return userId;
    }

    public boolean isAdmin(){
        return SecurityContextHolder.getContext()// ① 当前线程的 SecurityContext
                .getAuthentication() // ② 当前请求的认证对象
                .getAuthorities()// ③ 该用户拥有的权限集合 Collection<? extends GrantedAuthority>
                .stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));//是否存在权限
    }

    public void assertSelf(Integer targetUserId){
        if(targetUserId == null|| !id().equals(targetUserId)){
            throw new BizException(ResultCode.FORBIDDEN, "无权访问其他用户数据");
        }
    }
}
