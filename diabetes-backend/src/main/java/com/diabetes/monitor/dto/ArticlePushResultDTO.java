package com.diabetes.monitor.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticlePushResultDTO {

    private Integer articleId;

    // 当前符合条件的启用普通用户数
    private Integer eligibleCount;

    // 本次新增的推送关系数
    private Integer insertedCount;

    // 已经存在并跳过的关系数
    private Integer skippedCount;
}
