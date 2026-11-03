package com.diabetes.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.common.security.CurrentUser;
import com.diabetes.monitor.dto.UpdateUserStatusDTO;
import com.diabetes.monitor.entity.*;
import com.diabetes.monitor.service.HealthRecordBloodSugarService;
import com.diabetes.monitor.service.HealthRecordBodyService;
import com.diabetes.monitor.service.HealthRecordDietService;
import com.diabetes.monitor.service.HealthRecordExerciseService;
import com.diabetes.monitor.service.AiChatHistoryService;
import com.diabetes.monitor.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理后台", description = "用户管理、运动类型管理、文章管理")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Resource
    private SysUserService sysUserService;
    @Resource
    private SysExerciseTypeService exerciseTypeService;
    @Resource
    private HealthArticleService articleService;
    @Resource
    private SysUserArticleService userArticleService;
    @Resource
    private HealthRecordBloodSugarService bloodSugarService;
    @Resource
    private HealthRecordBodyService bodyService;
    @Resource
    private HealthRecordDietService dietService;
    @Resource
    private HealthRecordExerciseService exerciseService;
    @Resource
    private AiChatHistoryService aiChatHistoryService;
    @Resource
    private UserService userService;
    @Resource
    private CurrentUser currentUser;


    // ===== 用户管理 =====
    @Operation(summary = "分页查询用户列表")
    @GetMapping("/user/list")
    public Result userList(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size, @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysUser::getRealName, keyword).or().like(SysUser::getUsername, keyword);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = sysUserService.page(new Page<>(page, size), wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(result);
    }

    // ===== 管理端统计 =====
    @GetMapping("/stats")
    public Result stats() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("userCount", sysUserService.count());
        map.put("articleCount", articleService.count());
        map.put("pushedCount", articleService.count(new LambdaQueryWrapper<HealthArticle>().eq(HealthArticle::getPushStatus, 1)));
        map.put("exerciseTypeCount", exerciseTypeService.count());
        map.put("bloodSugarCount", bloodSugarService.count());
        map.put("bodyCount", bodyService.count());
        map.put("dietCount", dietService.count());
        map.put("exerciseCount", exerciseService.count());
        map.put("aiChatCount", aiChatHistoryService.count());
        return Result.ok(map);
    }

    @Operation(summary = "设置用户启用状态")
    @PutMapping("/user/{id}/status")
    public Result updateUserStatus(@PathVariable Integer id, @Valid @RequestBody UpdateUserStatusDTO dto){

        return userService.updateStatus(currentUser.id(),  id, dto.getStatus());
    }

    @Operation(summary = "逻辑删除用户")
    @DeleteMapping("/user/{id}")
    public Result deleteUser(@PathVariable Integer id) {
        return userService.deleteUser(currentUser.id(), id);
    }
}
