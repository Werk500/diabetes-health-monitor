package com.diabetes.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.entity.*;
import com.diabetes.monitor.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "健康记录", description = "身体指标、血糖、饮食、运动记录管理")
@RestController
@RequestMapping("/api/record")
public class HealthRecordController {

    @Autowired private HealthRecordBodyService bodyService;
    @Autowired private HealthRecordBloodSugarService bloodSugarService;
    @Autowired private HealthRecordDietService dietService;
    @Autowired private HealthRecordExerciseService exerciseService;
    @Autowired private SysExerciseTypeService exerciseTypeService;

    // ===== 身体指标 =====
    @Operation(summary = "新增身体指标记录")
    @PostMapping("/body")
    public Result addBody(@RequestBody HealthRecordBody record) {
        if (record.getWeight() != null && record.getUserId() != null && record.getRecordDate() == null) {
            record.setRecordDate(LocalDate.now());
        }
        bodyService.save(record);
        return Result.ok("记录成功");
    }

    @Operation(summary = "查询身体指标列表")
    @GetMapping("/body/list/{userId}")
    public Result bodyList(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<HealthRecordBody> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordBody::getUserId, userId)
               .ge(startDate != null && !startDate.isEmpty(), HealthRecordBody::getRecordDate, startDate)
               .le(endDate != null && !endDate.isEmpty(), HealthRecordBody::getRecordDate, endDate)
               .orderByDesc(HealthRecordBody::getRecordDate);
        return Result.ok(bodyService.list(wrapper));
    }

    @Operation(summary = "查询身体指标趋势")
    @GetMapping("/body/trend/{userId}")
    public Result bodyTrend(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        return Result.ok(bodyService.getBodyTrend(userId, startDate, endDate));
    }

    @Operation(summary = "查询最新身体指标")
    @GetMapping("/body/latest/{userId}")
    public Result bodyLatest(@PathVariable Integer userId) {
        return Result.ok(bodyService.getLatest(userId));
    }

    @Operation(summary = "删除身体指标记录")
    @DeleteMapping("/body/{id}")
    public Result deleteBody(@PathVariable Integer id) {
        bodyService.removeById(id);
        return Result.ok();
    }

    // ===== 血糖记录 =====
    @Operation(summary = "新增血糖记录")
    @PostMapping("/bloodSugar")
    public Result addBloodSugar(@RequestBody HealthRecordBloodSugar record) {
        bloodSugarService.save(record);
        return Result.ok("记录成功");
    }

    @Operation(summary = "查询血糖记录列表")
    @GetMapping("/bloodSugar/list/{userId}")
    public Result bloodSugarList(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<HealthRecordBloodSugar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordBloodSugar::getUserId, userId);
        if (startDate != null && !startDate.isEmpty()) wrapper.ge(HealthRecordBloodSugar::getMeasureTime, startDate + " 00:00:00");
        if (endDate != null && !endDate.isEmpty()) wrapper.le(HealthRecordBloodSugar::getMeasureTime, endDate + " 23:59:59");
        wrapper.orderByDesc(HealthRecordBloodSugar::getMeasureTime);
        return Result.ok(bloodSugarService.list(wrapper));
    }

    @Operation(summary = "查询血糖变化趋势")
    @GetMapping("/bloodSugar/trend/{userId}")
    public Result bloodSugarTrend(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        return Result.ok(bloodSugarService.getBloodSugarTrend(userId, startDate, endDate));
    }

    @Operation(summary = "查询最新血糖记录")
    @GetMapping("/bloodSugar/latest/{userId}")
    public Result bloodSugarLatest(@PathVariable Integer userId) {
        return Result.ok(bloodSugarService.getLatest(userId));
    }

    @Operation(summary = "删除血糖记录")
    @DeleteMapping("/bloodSugar/{id}")
    public Result deleteBloodSugar(@PathVariable Integer id) {
        bloodSugarService.removeById(id);
        return Result.ok();
    }

    // ===== 饮食记录 =====
    @Operation(summary = "新增饮食记录")
    @PostMapping("/diet")
    public Result addDiet(@RequestBody HealthRecordDiet record) {
        dietService.save(record);
        return Result.ok("记录成功");
    }

    @Operation(summary = "查询饮食记录列表")
    @GetMapping("/diet/list/{userId}")
    public Result dietList(@PathVariable Integer userId, @RequestParam(required = false) String date) {
        LambdaQueryWrapper<HealthRecordDiet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordDiet::getUserId, userId);
        if (date != null && !date.isEmpty()) {
            wrapper.ge(HealthRecordDiet::getEatTime, date + " 00:00:00")
                   .le(HealthRecordDiet::getEatTime, date + " 23:59:59");
        }
        wrapper.orderByDesc(HealthRecordDiet::getEatTime);
        return Result.ok(dietService.list(wrapper));
    }

    @Operation(summary = "查询饮食营养统计")
    @GetMapping("/diet/stats/{userId}")
    public Result dietStats(@PathVariable Integer userId, @RequestParam(required = false) String date) {
        return Result.ok(dietService.getDietStats(userId, date));
    }

    @Operation(summary = "删除饮食记录")
    @DeleteMapping("/diet/{id}")
    public Result deleteDiet(@PathVariable Integer id) {
        dietService.removeById(id);
        return Result.ok();
    }

    // ===== 运动记录 =====
    @Operation(summary = "新增运动记录")
    @PostMapping("/exercise")
    public Result addExercise(@RequestBody HealthRecordExercise record) {
        if (record.getExerciseDate() == null) record.setExerciseDate(LocalDate.now());
        exerciseService.save(record);
        return Result.ok("记录成功");
    }

    @Operation(summary = "查询运动记录列表")
    @GetMapping("/exercise/list/{userId}")
    public Result exerciseList(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<HealthRecordExercise> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordExercise::getUserId, userId)
               .ge(startDate != null && !startDate.isEmpty(), HealthRecordExercise::getExerciseDate, startDate)
               .le(endDate != null && !endDate.isEmpty(), HealthRecordExercise::getExerciseDate, endDate)
               .orderByDesc(HealthRecordExercise::getExerciseDate);
        return Result.ok(exerciseService.list(wrapper));
    }

    @Operation(summary = "查询运动消耗趋势")
    @GetMapping("/exercise/trend/{userId}")
    public Result exerciseTrend(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        return Result.ok(exerciseService.getExerciseStats(userId, startDate, endDate));
    }

    @Operation(summary = "删除运动记录")
    @DeleteMapping("/exercise/{id}")
    public Result deleteExercise(@PathVariable Integer id) {
        exerciseService.removeById(id);
        return Result.ok();
    }

    // ===== 运动类型 =====
    @Operation(summary = "获取运动类型列表")
    @GetMapping("/exerciseType/list")
    public Result exerciseTypeList() {
        return Result.ok(exerciseTypeService.list(new LambdaQueryWrapper<SysExerciseType>().eq(SysExerciseType::getStatus, 1)));
    }
}