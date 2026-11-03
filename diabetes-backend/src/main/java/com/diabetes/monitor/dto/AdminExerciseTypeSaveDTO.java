package com.diabetes.monitor.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdminExerciseTypeSaveDTO {

    @NotBlank(message = "运动名称不能为空")
    @Size(max = 50, message = "运动名称不能超过50个字符")
    private String typeName;

    @NotNull(message = "每小时消耗不能为空")
    @DecimalMin(value = "0.0", message = "每小时消耗不能小于0")
    @DecimalMax(value = "5000.0", message = "每小时消耗数值过大")
    private Double caloriesPerHour;

    @NotNull(message = "运动强度不能为空")
    @Min(1)
    @Max(3)
    private Integer intensity;

    @Size(max = 255, message = "适用人群不能超过255个字符")
    private String suitableFor;

    @Size(max = 2000, message = "描述不能超过2000个字符")
    private String description;

    @NotNull(message = "状态不能为空")
    @Min(0)
    @Max(1)
    private Integer status = 1;
}
