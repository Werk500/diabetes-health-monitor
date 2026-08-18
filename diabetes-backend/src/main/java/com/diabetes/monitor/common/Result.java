package com.diabetes.monitor.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;

    // ========== 成功响应 ==========
    public static <T> Result<T> ok() {
        return build(ResultCode.SUCCESS);
    }

    public static <T> Result<T> ok(T data) {
        return build(ResultCode.SUCCESS, data);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return build(ResultCode.SUCCESS.getCode(), msg, data);
    }

    // ========== 使用 ResultCode 的错误响应 ==========
    public static <T> Result<T> error(ResultCode resultCode) {
        return build(resultCode);
    }

    public static <T> Result<T> error(ResultCode resultCode, String msg) {
        return build(resultCode.getCode(), msg, null);
    }

    public static <T> Result<T> error(ResultCode resultCode, String msg, T data) {
        return build(resultCode.getCode(), msg, data);
    }

    /**
     * 使用自定义错误码和消息创建错误响应
     * @param code 错误码（可以是枚举的code，也可以是自定义的）
     * @param msg 错误消息
     * @return Result实例
     */
    public static <T> Result<T> error(Integer code, String msg) {
        return build(code, msg, null);
    }

    /**
     * 兼容旧代码：只传错误消息，使用默认 500
     * @param msg 错误消息
     * @return Result实例
     */
    public static <T> Result<T> error(String msg) {
        return build(ResultCode.INTERNAL_ERROR.getCode(), msg, null);
    }

    // ========== 便捷的错误响应方法 ==========
    public static <T> Result<T> badRequest(String msg) {
        return build(ResultCode.BAD_REQUEST.getCode(), msg, null);
    }

    public static <T> Result<T> unauthorized(String msg) {
        return build(ResultCode.UNAUTHORIZED.getCode(), msg, null);
    }

    public static <T> Result<T> forbidden(String msg) {
        return build(ResultCode.FORBIDDEN.getCode(), msg, null);
    }

    public static <T> Result<T> notFound(String msg) {
        return build(ResultCode.NOT_FOUND.getCode(), msg, null);
    }

    public static <T> Result<T> tooManyRequests(String msg) {
        return build(ResultCode.TOO_MANY_REQUESTS.getCode(), msg, null);
    }

    public static <T> Result<T> internalError(String msg) {
        return build(ResultCode.INTERNAL_ERROR.getCode(), msg, null);
    }

    public static <T> Result<T> gatewayTimeout(String msg) {
        return build(ResultCode.GATEWAY_TIMEOUT.getCode(), msg, null);
    }

    // ========== 通用构建方法 ==========
    private static <T> Result<T> build(ResultCode resultCode) {
        return build(resultCode.getCode(), resultCode.getMessage(), null);
    }

    private static <T> Result<T> build(ResultCode resultCode, T data) {
        return build(resultCode.getCode(), resultCode.getMessage(), data);
    }

    private static <T> Result<T> build(Integer code, String msg, T data) {
        Result<T> result = new Result<>();
        result.code = code;
        result.msg = msg;
        result.data = data;
        return result;
    }
}