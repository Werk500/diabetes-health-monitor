package com.diabetes.monitor.controller;

import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.dto.LoginDTO;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理", description = "登录、注册、用户信息管理")
@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

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
}