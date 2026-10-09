package com.diabetes.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("health_record_blood_sugar")
public class HealthRecordBloodSugar {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Double bloodSugar;
    private Integer measureType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime measureTime;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
