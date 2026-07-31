package com.diabetes.monitor.common;

import lombok.Data;

@Data
public class Result {
    private Integer code;
    private String msg;
    private Object data;

    public static Result ok() { Result result = new Result(); result.code = 200; result.msg = "success"; return result; }
    public static Result ok(Object data) { Result result = ok(); result.data = data; return result; }
    public static Result ok(String msg, Object data) { Result result = ok(data); result.msg = msg; return result; }
    public static Result error(String msg) { Result result = new Result(); result.code = 500; result.msg = msg; return result; }
    public static Result error(int code, String msg) { Result result = new Result(); result.code = code; result.msg = msg; return result; }
}
