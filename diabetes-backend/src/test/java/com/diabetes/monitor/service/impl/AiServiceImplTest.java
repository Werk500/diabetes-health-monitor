package com.diabetes.monitor.service.impl;

import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.config.AiConfig;
import com.diabetes.monitor.service.AiChatHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
public class AiServiceImplTest {

    @Mock
    private AiConfig aiConfig;

    @Mock
    private AiChatHistoryService aiChatHistoryService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ThreadPoolTaskExecutor taskExecutor;

    @Mock
    private DashScopeClient dashScopeClient;

    private ObjectMapper objectMapper;

    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        aiService = new AiServiceImpl();

        ReflectionTestUtils.setField(aiService, "aiConfig", aiConfig);
        ReflectionTestUtils.setField(aiService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(aiService, "aiChatHistoryService", aiChatHistoryService);
        ReflectionTestUtils.setField(aiService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(aiService, "taskExecutor", taskExecutor);
        ReflectionTestUtils.setField(aiService, "dashScopeClient", dashScopeClient);
    }

    /**
     * 清理上下文
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    //用户输入空内容时，系统应该快速返回错误，不调用任何外部依赖。
    @Test
    void chat_emptyContent() {
        Map<String, String> body = new HashMap<>();
        body.put("content", "");

        Result result = aiService.chat(body);

        assertNotNull(result);
        assertEquals(500, result.getCode());
        verifyNoInteractions(dashScopeClient);
        verifyNoInteractions(aiChatHistoryService);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void chat_success() {

        //在测试环境中模拟用户登录状态，让后续业务代码能获取到用户 ID（值为 1），从而测试需要用户身份的接口逻辑。
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(1);
        SecurityContextHolder.getContext().setAuthentication(auth);

        /**
         * 创建 ValueOperations 的 Mock（负责 Redis 的 String 类型操作）
         * 让 redisTemplate.opsForValue() 返回这个 Mock
         * 当调用 valueOps.get(任意key) 时返回 null，表示没有旧的 session，需要创建新 session
         */
        ValueOperations<String,Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);// 没有旧 session，创建新 session

        ListOperations<String,Object> listOps = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.size(anyString())).thenReturn(0L);//没有历史上下文
        /**
         * doAnswer 用于自定义 Mock 方法的执行逻辑
         * 获取 execute 方法传入的 Runnable 任务
         * 直接调用 task.run()，让异步任务在当前测试线程中同步执行
         * 这样做是为了避免测试过程中线程异步执行导致 join() 卡住或断言时机不对
         */
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();// 让异步任务同步执行，否则 join() 会卡住
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));

        String userContent = "你好";
        String assistantReply = "你好";


        when(aiConfig.getApiKey()).thenReturn("test-key");
        when(aiConfig.getTemperature()).thenReturn(0.7);

        when(dashScopeClient.callBlocking(anyList(), anyString(), anyDouble()))
                .thenReturn(assistantReply);

        Result result = aiService.chat(Map.of("content", userContent));
        assertEquals(200,result.getCode());

        Map<?,?> dataMap = (Map<?, ?>) result.getData();
        assertEquals(assistantReply, dataMap.get("reply"));

        verify(aiChatHistoryService).saveMessage(eq(1), eq("user"), eq(userContent), anyString());
        verify(aiChatHistoryService).saveMessage(eq(1), eq("assistant"), eq(assistantReply), anyString());
        verify(listOps,times(2)).rightPush(anyString(), anyString());
        verify(listOps,times(2)).trim(anyString(), anyLong(), anyLong());
        verify(redisTemplate,times(2)).expire(anyString(), anyLong(), any(TimeUnit.class));


    }

    @Test
    void chat_withHistory() {
        // 模拟用户登录
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(1);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 模拟 Redis ValueOperations - 无旧session
        ValueOperations<String,Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        // 模拟 Redis ListOperations - 有历史上下文
        ListOperations<String,Object> listOps = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.size(anyString())).thenReturn(2L);

        String historyJson1 = "{\"role\":\"user\",\"content\":\"之前的问题\"}";
        String historyJson2 = "{\"role\":\"assistant\",\"content\":\"之前的回答\"}";
        when(listOps.range(anyString(), anyLong(), anyLong()))
                .thenReturn(Arrays.asList(historyJson1, historyJson2));

        // 异步任务同步执行
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));

        // Mock AI 配置
        when(aiConfig.getApiKey()).thenReturn("test-key");
        when(aiConfig.getTemperature()).thenReturn(0.7);

        // Mock AI 返回
        String userContent = "你好";
        String assistantReply = "你好，有什么可以帮助你的？";
        when(dashScopeClient.callBlocking(anyList(), anyString(), anyDouble()))
                .thenReturn(assistantReply);

        // 执行测试
        Result result = aiService.chat(Map.of("content", userContent));

        // 断言
        assertEquals(200, result.getCode());
        Map<?,?> dataMap = (Map<?, ?>) result.getData();
        assertEquals(assistantReply, dataMap.get("reply"));

        // 验证消息保存
        verify(aiChatHistoryService).saveMessage(eq(1), eq("user"), eq(userContent), anyString());
        verify(aiChatHistoryService).saveMessage(eq(1), eq("assistant"), eq(assistantReply), anyString());
        // 验证 Redis 操作（用户消息和助手回复各一次）
        verify(listOps, times(2)).rightPush(anyString(), anyString());
        verify(listOps, times(2)).trim(anyString(), anyLong(), anyLong());
        verify(redisTemplate, times(3)).expire(anyString(), anyLong(), any(TimeUnit.class));

        // 验证 AI 被调用时携带了历史上下文
        verify(dashScopeClient).callBlocking(
                argThat(messages -> {
                    // 验证传入的消息列表包含历史对话
                    return messages.size() >= 3; // 2条历史 + 当前用户消息
                }),
                anyString(),
                anyDouble()
        );
    }

    @Test
    void chat_userIdMissing() {
       SecurityContextHolder.clearContext();

        HashMap<String, String> body = new HashMap<>();
        body.put("content", "你好");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiService.chat(body));

        assertEquals("userId不能为空", exception.getMessage());

        verifyNoInteractions(dashScopeClient);
        verifyNoInteractions(aiChatHistoryService);
        verifyNoInteractions(redisTemplate);
    }
}
