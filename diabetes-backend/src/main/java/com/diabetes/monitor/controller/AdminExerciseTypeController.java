package com.diabetes.monitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.dto.AdminExerciseTypeQueryDTO;
import com.diabetes.monitor.dto.AdminExerciseTypeSaveDTO;
import com.diabetes.monitor.dto.UpdateExerciseTypeStatusDTO;
import com.diabetes.monitor.entity.SysExerciseType;
import com.diabetes.monitor.service.SysExerciseTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端运动配置")
@RestController
@RequestMapping("/api/admin/exerciseType")
public class AdminExerciseTypeController {

    @Resource
    private SysExerciseTypeService exerciseTypeService;

    @GetMapping("/list")
    @Operation(summary = "分页查询运动类型")
    public  Result<IPage<SysExerciseType>> list(
            @Valid AdminExerciseTypeQueryDTO query
    ) {
        return Result.ok(exerciseTypeService.pageQuery(query));
    }

    @Operation(summary = "新增运动类型")
    @PostMapping
    public Result<SysExerciseType> create(
            @Valid @RequestBody AdminExerciseTypeSaveDTO dto
    ) {
        return Result.ok(exerciseTypeService.createExerciseType(dto));
    }

    @Operation(summary = "更新运动类型")
    @PutMapping("/{id}")
    public Result<SysExerciseType> update(
            @PathVariable Integer id,
            @Valid @RequestBody AdminExerciseTypeSaveDTO dto
    ) {
        return Result.ok(
                exerciseTypeService.updateExerciseType(id, dto)
        );
    }

    @Operation(summary = "设置运动类型启用状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateExerciseTypeStatusDTO dto
    ) {
        exerciseTypeService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }

    @Operation(summary = "删除运动类型")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        exerciseTypeService.deleteExerciseType(id);
        return Result.ok();
    }

}
