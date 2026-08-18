package com.diabetes.monitor.common;

import lombok.Getter;

/**
 * 业务异常类
 * 用于处理业务逻辑层面的异常，如参数校验失败、数据不存在等
 */
@Getter// Lombok自动生成getter方法
public class BizException extends RuntimeException{

    private final Integer code;//业务错误码，默认500


    /**
     * 构造器1：只传入异常信息，使用默认错误码 500
     * @param resultCode 异常信息
     */
    public  BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 构造器2：传入自定义错误码和异常信息
     * @param resultCode 自定义错误码
     * @param message 异常信息
     */
    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    /**
     * 构造器3：使用自定义错误码和异常信息（兼容旧代码）
     * @param code 自定义错误码
     * @param message 异常信息
     */
    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造器4：只传入异常信息，使用默认错误码 500（兼容旧代码）
     * @param message 异常信息
     */
    public BizException(String message) {
        super(message);
        this.code = ResultCode.INTERNAL_ERROR.getCode();  // 500
    }

    // ========== 静态工厂方法（推荐使用） ==========

    /**
     * 使用 ResultCode 创建异常
     * @param resultCode 结果码枚举
     * @return BizException 实例
     */
    public static BizException of(ResultCode resultCode) {
        return new BizException(resultCode);
    }

    /**
     * 使用 ResultCode + 自定义消息 创建异常
     * @param resultCode 结果码枚举
     * @param message 自定义异常信息
     * @return BizException 实例
     */
    public static BizException of(ResultCode resultCode, String message) {
        return new BizException(resultCode, message);
    }

    /**
     * 使用自定义错误码 + 消息 创建异常（兼容旧代码）
     * @param code 自定义错误码
     * @param message 异常信息
     * @return BizException 实例
     */
    public static BizException of(Integer code, String message) {
        return new BizException(code, message);
    }

    /**
     * 只传入消息，使用默认500（兼容旧代码）
     * @param message 异常信息
     * @return BizException 实例
     */
    public static BizException of(String message) {
        return new BizException(message);
    }
}
