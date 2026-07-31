package com.diabetes.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_record_body")
public class HealthRecordBody {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Double weight;
    private Double bmi;
    private Double bodyFat;
    private Integer systolicPressure;
    private Integer diastolicPressure;
    private Integer heartRate;
    private Double waistline;
    private LocalDate recordDate;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
