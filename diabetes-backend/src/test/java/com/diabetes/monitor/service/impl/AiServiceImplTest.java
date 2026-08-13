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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

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
}
