package com.diabetes.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_record_exercise")
public class HealthRecordExercise {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer exerciseTypeId;
    private Integer durationMinutes;
    private Double caloriesBurned;
    private Integer heartRateAvg;
    private LocalDate exerciseDate;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
