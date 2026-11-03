package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.ResultCode;
import com.diabetes.monitor.dto.AdminExerciseTypeQueryDTO;
import com.diabetes.monitor.dto.AdminExerciseTypeSaveDTO;
import com.diabetes.monitor.entity.HealthRecordExercise;
import com.diabetes.monitor.entity.SysExerciseType;
import com.diabetes.monitor.mapper.SysExerciseTypeMapper;
import com.diabetes.monitor.service.HealthRecordExerciseService;
import com.diabetes.monitor.service.SysExerciseTypeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SysExerciseTypeServiceImpl extends ServiceImpl<SysExerciseTypeMapper, SysExerciseType> implements SysExerciseTypeService {

    @Resource
    private HealthRecordExerciseService exerciseService;


    @Override
    public IPage<SysExerciseType> pageQuery(AdminExerciseTypeQueryDTO query) {

        LambdaQueryWrapper<SysExerciseType> wrapper = new LambdaQueryWrapper<SysExerciseType>()
                .eq(query.getIntensity() != null,
                        SysExerciseType::getIntensity,
                        query.getIntensity())
                .eq(query.getStatus() != null,
                        SysExerciseType::getStatus,
                        query.getStatus());

        if(StringUtils.hasText(query.getKeyword())){
            String keyword = query.getKeyword().trim();

            wrapper.and(condition->condition
                    .like(SysExerciseType::getTypeName, keyword)
                    .or()
                    .like(SysExerciseType::getSuitableFor, keyword));
        }


        wrapper.orderByDesc(SysExerciseType::getCreateTime)
                .orderByDesc(SysExerciseType::getId);

        return page(
                new Page<>(query.getPage(), query.getSize()),
                wrapper
        );
    }

    @Override
    public SysExerciseType createExerciseType(AdminExerciseTypeSaveDTO dto) {
        String typeName = dto.getTypeName().trim();
        long duplicateCount = count(
                new LambdaQueryWrapper<SysExerciseType>()
                        .eq(SysExerciseType::getTypeName, typeName)
        );
        if (duplicateCount > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "运动类型名称已存在");
        }

        SysExerciseType sysExerciseType = new SysExerciseType();

        sysExerciseType.setTypeName(typeName);
        sysExerciseType.setSuitableFor(dto.getSuitableFor());
        sysExerciseType.setIntensity(dto.getIntensity());
        sysExerciseType.setStatus(dto.getStatus());
        sysExerciseType.setDescription(dto.getDescription());
        sysExerciseType.setCaloriesPerHour(dto.getCaloriesPerHour());

        if (!save(sysExerciseType)) {
            throw new BizException(
                    ResultCode.INTERNAL_ERROR,
                    "运动类型创建失败"
            );
        }

        return sysExerciseType;
    }

    @Override
    public SysExerciseType updateExerciseType(Integer id, AdminExerciseTypeSaveDTO dto) {
        SysExerciseType type = getById(id);

        if(type == null){
            throw new BizException(ResultCode.NOT_FOUND,"类型不存在");
        }

        String typeName = dto.getTypeName().trim();
        long duplicateCount = count(
                new LambdaQueryWrapper<SysExerciseType>()
                        .eq(SysExerciseType::getTypeName, typeName)
                        .ne(SysExerciseType::getId, id)
        );
        if (duplicateCount > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "运动类型名称已存在");
        }

        type.setTypeName(typeName);
        type.setSuitableFor(dto.getSuitableFor());
        type.setIntensity(dto.getIntensity());
        type.setStatus(dto.getStatus());
        type.setDescription(dto.getDescription());
        type.setCaloriesPerHour(dto.getCaloriesPerHour());

        if(!updateById(type)){
            throw new BizException(
                    ResultCode.INTERNAL_ERROR,
                    "运动类型更新失败"
            );
        }

        return type;
    }

    @Override
    public void updateStatus(Integer id, Integer status) {
        SysExerciseType type = getById(id);

        if(type == null){
            throw new BizException(ResultCode.NOT_FOUND,"类型不存在");
        }

        type.setStatus(status);

        if(!updateById(type)){
            throw new BizException(
                    ResultCode.INTERNAL_ERROR,
                    "运动类型更新失败"
            );
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteExerciseType(Integer id) {

        // 锁住运动类型行，避免检查引用后又被并发写入新记录
        SysExerciseType type = getOne(
                new LambdaQueryWrapper<SysExerciseType>()
                        .eq(SysExerciseType::getId, id)
                        .last("FOR UPDATE")
        );

        if (type == null) {
            throw new BizException(ResultCode.NOT_FOUND, "运动类型不存在");
        }


        long usedCount = exerciseService.count(
                new LambdaQueryWrapper<HealthRecordExercise>()
                        .eq(HealthRecordExercise::getExerciseTypeId, id)
        );

        if(usedCount > 0){
            throw new BizException(
                    ResultCode.BAD_REQUEST,
                    "该运动类型已被 " + usedCount + " 条运动记录引用，请改为停用"
            );
        }

        if (!removeById(id)) {
            throw new BizException(
                    ResultCode.INTERNAL_ERROR,
                    "运动类型删除失败"
            );
        }
    }
}
