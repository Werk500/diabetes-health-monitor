package com.diabetes.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_operation_log")
public class AdminOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer operatorId;
    private String operatorName;
    private String actionCode;
    private String targetType;
    private String targetId;
    private String requestMethod;
    private String requestPath;
    private String requestIp;
    private Integer success;
    private String detail;
    private LocalDateTime createTime;
}
