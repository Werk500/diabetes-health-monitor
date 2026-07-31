package com.diabetes.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_exercise_type")
public class SysExerciseType {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String typeName;
    private Double caloriesPerHour;
    private Integer intensity;
    private String suitableFor;
    private String description;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
