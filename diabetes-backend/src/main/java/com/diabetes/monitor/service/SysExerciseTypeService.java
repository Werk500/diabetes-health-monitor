package com.diabetes.monitor.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.diabetes.monitor.dto.AdminExerciseTypeQueryDTO;
import com.diabetes.monitor.dto.AdminExerciseTypeSaveDTO;
import com.diabetes.monitor.entity.SysExerciseType;

public interface SysExerciseTypeService extends IService<SysExerciseType> {

    IPage<SysExerciseType> pageQuery(AdminExerciseTypeQueryDTO query);

    SysExerciseType createExerciseType(AdminExerciseTypeSaveDTO dto);

    SysExerciseType updateExerciseType(
            Integer id,
            AdminExerciseTypeSaveDTO dto
    );

    void updateStatus(Integer id, Integer status);

    void deleteExerciseType(Integer id);
}
