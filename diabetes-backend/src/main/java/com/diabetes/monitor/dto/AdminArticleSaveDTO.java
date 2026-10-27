package com.diabetes.monitor.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdminArticleSaveDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过100个字符")
    private String title;

    @NotBlank(message = "摘要不能为空")
    @Size(max = 500, message = "摘要不能超过500个字符")
    private String summary;

    @NotBlank(message = "内容不能为空")
    private String content;

    @NotNull(message = "分类不能为空")
    @Min(value = 1, message = "分类不正确")
    @Max(value = 4, message = "分类不正确")
    private Integer category;

    @Size(max = 255, message = "封面地址过长")
    private String coverImage;

    @NotBlank(message = "作者不能为空")
    @Size(max = 50, message = "作者不能超过50个字符")
    private String author;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status = 1;
}
