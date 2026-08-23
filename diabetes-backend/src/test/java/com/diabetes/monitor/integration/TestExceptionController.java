package com.diabetes.monitor.integration;

import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.ResultCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("test")
@Validated
@RestController
public class TestExceptionController {

    @GetMapping("/test/biz-error")
    public void bizError(@RequestParam(defaultValue = "400") int code) {
        switch (code) {
            case 401 -> throw new BizException(ResultCode.UNAUTHORIZED);
            case 403 -> throw new BizException(ResultCode.FORBIDDEN);
            case 404 -> throw new BizException(ResultCode.NOT_FOUND);
            case 500 -> throw new BizException(ResultCode.INTERNAL_ERROR);
            default -> throw new BizException(ResultCode.BAD_REQUEST);
        }
    }

    @GetMapping("/test/forbidden")
    public void forbidden() {
        throw new AccessDeniedException("test forbidden");
    }

    @GetMapping("/test/error")
    public void error() {
        throw new RuntimeException("test unknown error");
    }

    @PostMapping("/test/validate")
    public void validate(@Valid @RequestBody TestValidationRequest request) {
    }

    @GetMapping("/test/constraint")
    public void constraint(@RequestParam @NotBlank String name) {
    }

    @GetMapping("/test/bind")
    public void bind() throws BindException {
        BindException ex = new BindException(new Object(), "testForm");
        ex.addError(new FieldError("testForm", "age", "类型转换错误"));
        throw ex;
    }

    @Data
    public static class TestValidationRequest {
        @NotBlank(message = "name不能为空")
        private String name;

        @Min(value = 0, message = "age不能为负数")
        private Integer age;
    }
}
