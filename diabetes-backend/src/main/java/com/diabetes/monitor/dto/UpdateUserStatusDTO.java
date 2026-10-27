package com.diabetes.monitor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusDTO {

    @NotNull(message = "用户状态不能为空")
    @Min(value = 0, message = "用户状态只能为 0 或 1")
    @Max(value = 1, message = "用户状态只能为 0 或 1")
    private Integer status;
}
