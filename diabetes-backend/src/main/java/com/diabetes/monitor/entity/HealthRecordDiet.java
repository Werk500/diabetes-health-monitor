package com.diabetes.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime eatTime;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
