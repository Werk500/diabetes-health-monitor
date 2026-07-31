package com.diabetes.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("health_record_diet")
public class HealthRecordDiet {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String foodName;
    private Integer mealType;
    private Double calories;
    private Double carbs;
    private Double protein;
    private Double fat;
    private Double fiber;
    private Double portion;
    private LocalDateTime eatTime;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
