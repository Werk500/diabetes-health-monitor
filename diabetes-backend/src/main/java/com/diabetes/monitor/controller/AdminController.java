package com.diabetes.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.monitor.common.Result;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Operation(summary = "删除用户")
    @DeleteMapping("/user/{id}")
    public Result deleteUser(@PathVariable Integer id) {
        sysUserService.removeById(id);
        return Result.ok();
    }

    // ===== 运动类型管理 =====
    @Operation(summary = "获取所有运动类型")
    @GetMapping("/exerciseType/list")
    public Result exerciseTypeList() {
        return Result.ok(exerciseTypeService.list());
    }

    @Operation(summary = "新增运动类型")
    @PostMapping("/exerciseType")
    public Result addExerciseType(@RequestBody SysExerciseType type) {
        exerciseTypeService.save(type);
        return Result.ok("添加成功");
    }

    @Operation(summary = "更新运动类型")
    @PutMapping("/exerciseType")
    public Result updateExerciseType(@RequestBody SysExerciseType type) {
        exerciseTypeService.updateById(type);
        return Result.ok("更新成功");
    }

    @Operation(summary = "删除运动类型")
    @DeleteMapping("/exerciseType/{id}")
    public Result deleteExerciseType(@PathVariable Integer id) {
        exerciseTypeService.removeById(id);
        return Result.ok();
    }

    // ===== 文章管理 =====
    @Operation(summary = "分页查询文章列表")
    @GetMapping("/article/list")
    public Result articleList(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size) {
        LambdaQueryWrapper<HealthArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(HealthArticle::getCreateTime);
        return Result.ok(articleService.page(new Page<>(page, size), wrapper));
    }

    @Operation(summary = "新增文章")
    @PostMapping("/article")
    public Result addArticle(@RequestBody HealthArticle article) {
        articleService.save(article);
        return Result.ok("添加成功");
    }

    @Operation(summary = "更新文章")
    @PutMapping("/article")
    public Result updateArticle(@RequestBody HealthArticle article) {
        articleService.updateById(article);
        return Result.ok("更新成功");
    }

    @Operation(summary = "删除文章")
    @DeleteMapping("/article/{id}")
    public Result deleteArticle(@PathVariable Integer id) {
        articleService.removeById(id);
        return Result.ok();
    }

    @Operation(summary = "推送文章给所有用户")
    @PutMapping("/article/push/{id}")
    public Result pushArticle(@PathVariable Integer id) {
        articleService.pushArticle(id);
        return Result.ok("推送成功");
    }

    // ===== 文章分类统计 =====
    @Operation(summary = "文章分类统计")
    @GetMapping("/article/categoryStats")
    public Result categoryStats() {
        List<HealthArticle> list = articleService.list();
        long monitor = list.stream().filter(a -> a.getCategory() == 1).count();
        long diet = list.stream().filter(a -> a.getCategory() == 2).count();
        long complication = list.stream().filter(a -> a.getCategory() == 3).count();
        long exercise = list.stream().filter(a -> a.getCategory() == 4).count();
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("bloodSugar", monitor);
        stats.put("dietControl", diet);
        stats.put("complication", complication);
        stats.put("exerciseSuggestion", exercise);
        return Result.ok(stats);
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
}