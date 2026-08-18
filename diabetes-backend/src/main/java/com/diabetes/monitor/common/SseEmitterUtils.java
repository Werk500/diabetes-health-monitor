package com.diabetes.monitor.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * SSE 流式响应工具类
 * 统一 SSE 错误事件协议：event:error + data:{"code":...,"msg":...}
 */
@Slf4j
public final class SseEmitterUtils {

    private static final long DEFAULT_TIMEOUT_MILLIS = 300_000L;
    private static final String ERROR_EVENT = "error";

    private SseEmitterUtils() {
    }

    public static SseEmitter createEmitter() {
        return new SseEmitter(DEFAULT_TIMEOUT_MILLIS);
    }

    public static SseEmitter error(int code, String msg) {
        SseEmitter emitter = createEmitter();
        sendError(emitter, code, msg);
        return emitter;
    }

    public static void sendError(SseEmitter emitter, Throwable throwable) {
        if (throwable instanceof BizException bizException) {
            sendError(emitter, bizException.getCode(), bizException.getMessage());
            return;
        }
        log.error("SSE 流式异常，发送统一错误事件", throwable);
        sendError(emitter, 500, "AI 服务暂时不可用，请稍后重试");
    }

    public static void sendError(SseEmitter emitter, int code, String msg) {
        try {
            String safeMsg = msg == null ? "AI 服务暂时不可用，请稍后重试" : msg;
            Map<String, Object> payload = Map.of("code", code, "msg", safeMsg);
            emitter.send(SseEmitter.event()
                    .name(ERROR_EVENT)
                    .data(payload, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.warn("发送 SSE 错误事件失败: {}", e.getMessage());
        } finally {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("完成 SSE 连接失败: {}", e.getMessage());
            }
        }
    }
}
