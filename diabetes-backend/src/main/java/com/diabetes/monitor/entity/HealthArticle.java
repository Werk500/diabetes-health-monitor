package com.diabetes.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("health_article")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthArticle {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String content;
    private String summary;
    private Integer category;
    private String coverImage;
    private String author;
    private Integer pushStatus;
    private Integer viewCount;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
