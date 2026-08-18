package com.diabetes.monitor.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice//全局异常处理器
public class GlobalExceptionHandler {

    /**
     * 1. 处理业务异常
     * 根据错误码返回对应的HTTP状态码
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());

        // 使用Result统一格式
        Result<Void> result = Result.error(e.getCode(), e.getMessage());

        // 根据错误码获取对应的HTTP状态
        HttpStatus status = HttpStatus.resolve(e.getCode());
        if (status == null) {
            // 如果错误码没有对应的HTTP状态，默认500
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity.status(status).body(result);
    }

    /**
     * 2. 处理参数校验异常（@Valid 校验失败）
     * 统一返回 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("参数校验失败: {}", e.getMessage());

        // 收集所有错误信息
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(";"));


        // 使用 ResultCode 枚举
        Result<Void> result = Result.error(ResultCode.BAD_REQUEST, errorMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 3. 处理数据绑定异常（数据格式错误）
     * 统一返回 400
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        log.warn("数据绑定失败: {}", e.getMessage());

        String errorMsg = e.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        Result<Void> result = Result.error(ResultCode.BAD_REQUEST, errorMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 4. 处理约束校验异常（@Validated 校验失败）
     * 统一返回 400
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("约束校验失败: {}", e.getMessage());

        String errorMsg = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        Result<Void> result = Result.error(ResultCode.BAD_REQUEST, errorMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 5. 处理权限不足异常
     * 返回 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());

        Result<Void> result = Result.error(ResultCode.FORBIDDEN, "权限不足，请联系管理员");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }

    /**
     * 6. 处理所有未知异常
     * 返回 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常: ", e);

        Result<Void> result = Result.error(ResultCode.INTERNAL_ERROR, "系统繁忙，请稍后重试");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
}