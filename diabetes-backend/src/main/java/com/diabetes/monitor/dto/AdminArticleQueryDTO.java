package com.diabetes.monitor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AdminArticleQueryDTO {

    /**
     * 当前页码，从 1 开始
     */
    @Min(value = 1, message = "页码不能小于1")
    private long page = 1;

    /**
     * 每页数量，默认 10，最大 100
     */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页最多查询100条")
    private long size = 10;

    /**
     * 关键词：匹配标题、摘要或作者
     */
    private String keyword;

    /**
     * 文章分类：
     * 1-血糖监测
     * 2-控糖饮食
     * 3-并发症预防
     * 4-运动建议
     */
    @Min(value = 1, message = "文章分类不正确")
    @Max(value = 4, message = "文章分类不正确")
    private Integer category;

    /**
     * 推送状态：
     * 0-未推送
     * 1-已推送
     */
    @Min(value = 0, message = "推送状态只能为0或1")
    @Max(value = 1, message = "推送状态只能为0或1")
    private Integer pushStatus;

    /**
     * 启用状态：
     * 0-停用
     * 1-启用
     */
    @Min(value = 0, message = "文章状态只能为0或1")
    @Max(value = 1, message = "文章状态只能为0或1")
    private Integer status;
}