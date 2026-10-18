package com.diabetes.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("export_task")
public class ExportTask {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer userId;
    private String type;          // EXCEL / PDF
    private String status;        // ExportStatus.*
    private String params;        // JSON 字符串（MySQL JSON 列）
    private Integer progress;     // 0-100
    private String filePath;
    private Long fileSize;
    private String errorMsg;
    private Integer retryCount;

    private LocalDateTime createTime;   // DB 默认值，不用 fill
    private LocalDateTime startTime;
    private LocalDateTime finishTime;

}
