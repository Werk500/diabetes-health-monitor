package com.diabetes.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.config.JwtUtil;
import com.diabetes.monitor.config.RsaUtil;
import com.diabetes.monitor.dto.LoginDTO;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.service.SmsService;
import com.diabetes.monitor.service.SysUserService;
import com.diabetes.monitor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "用户管理", description = "登录、注册、用户信息管理")
@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private SmsService smsService;
    @Resource
    private SysUserService sysUserService;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private RsaUtil rsaUtil;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginDTO loginDTO) {
        return userService.login(loginDTO);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result register(@Valid @RequestBody SysUser user) {
        return userService.register(user);
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/info/{id}")
    public Result info(@PathVariable Integer id) {
        return userService.info(id);
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/update")
    public Result update(@RequestBody SysUser user) {
        log.info("更新成功");
        return userService.update(user);
    }

    @Operation(summary = "获取用户列表")
    @GetMapping("/list")
    public Result list() {
        return userService.list();
    }

    @Operation(summary = "切换用户状态（启用/禁用）")
    @PutMapping("/status/{id}")
    public Result toggleStatus(@PathVariable Integer id) {
        return userService.toggleStatus(id);
    }

    @Operation(summary = "发送验证码")
    @PostMapping("/sms/send")
    public Result sendSms(@RequestBody Map<String, String> body) {
        smsService.sendCode(body.get("phone"));
        return Result.ok("验证码已发送");
    }


    @Operation(summary = "验证验证码")
    @PostMapping("/sms/login")
    public Result smsLogin(@RequestBody Map<String, String> body) {

        String code = rsaUtil.decrypt(body.get("code"));
        boolean verifyCode = smsService.verifyCode(body.get("phone"), code);
        if (!verifyCode) {
            return Result.error("验证失败");
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getPhone, body.get("phone"));
        SysUser user = sysUserService.getOne(wrapper);
        if (user == null) {
            return Result.error("该手机号未注册");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);

        return Result.ok(data);

    }

    @Operation(summary = "返回公钥")
    @GetMapping("/public-key")
    public Result getPublicKey() {
        return Result.ok(rsaUtil.getPublicKey());
    }

    @Operation(summary = "健康文章")
    @GetMapping("/article/list")
    public Result userArticleList(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "100") Integer size,
                                  Authentication authentication) {
        return userService.articleList(page,size,authentication);
    }


}