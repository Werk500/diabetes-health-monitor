package com.diabetes.monitor.common;

/**
 * 业务异常类
 * 用于处理业务逻辑层面的异常，如参数校验失败、数据不存在等
 */
public class BizException extends RuntimeException{

    private int code;//业务错误码，默认500


    /**
     * 构造器1：只传入异常信息，使用默认错误码 500
     * @param message 异常信息
     */
    public  BizException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 构造器2：传入自定义错误码和异常信息
     * @param code 自定义错误码
     * @param message 异常信息
     */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取业务错误码
     * @return 错误码
     */
    public int getCode() {
        return code;
    }

    /**
     * 静态工厂方法：快速创建业务异常（使用默认错误码 500）
     * @param msg 异常信息
     * @return BizException 实例
     */
    public static BizException of(String msg) {
        return new BizException(msg);
    }

    /**
     * 静态工厂方法：快速创建带自定义错误码的业务异常
     * @param code 自定义错误码
     * @param msg 异常信息
     * @return BizException 实例
     */
    public static BizException of(int code, String msg) {
        return new BizException(code, msg);
    }

}
