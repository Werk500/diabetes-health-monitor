package com.diabetes.monitor.service.impl;


import com.diabetes.monitor.dto.FoodRecognitionResult;
import com.diabetes.monitor.service.AiChatHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FoodRecognitionServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOps;
    @Mock
    private MultipartFile file;
    @Mock
    private AiChatHistoryService aiChatHistoryService;


    private FoodRecognitionServiceImpl service;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new FoodRecognitionServiceImpl(objectMapper);
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
    }

    @Test
    void parseAiJson_markdownWrapper(){
        String aiText = "```json\n{\"foodName\":\"苹果\",\"calories\":52,\"carbs\":14,\"protein\":0.3,\"fat\":0.2,\"glycemicIndex\":\"低\",\"suggestion\":\"可以适量吃\"}\n```";

        FoodRecognitionResult result = ReflectionTestUtils.invokeMethod(service, "parseAiJson", aiText);


        assertEquals("苹果", result.getFoodName());
        assertEquals(52, result.getCalories());
        assertEquals(14, result.getCarbs());
        assertEquals(0.3, result.getProtein());
        assertEquals(0.2, result.getFat());
        assertEquals("低", result.getGlycemicIndex());
        assertEquals("可以适量吃", result.getSuggestion());
    }

    @Test
    void parseAiJson_arrayTakeFirst() {
        String aiText = "[{\"foodName\":\"香蕉\",\"calories\":89,\"carbs\":23,\"protein\":1.1,\"fat\":0.3,\"glycemicIndex\":\"中\",\"suggestion\":\"适量食用\"},{\"foodName\":\"苹果\",\"calories\":52}]";

        FoodRecognitionResult result = ReflectionTestUtils.invokeMethod(service, "parseAiJson", aiText);

        assertEquals("香蕉", result.getFoodName());
        assertEquals(89.0, result.getCalories(), 0.001);
        assertEquals("中", result.getGlycemicIndex());
        assertEquals(aiText, result.getRawResponse());
    }

    @Test
    void parseAiJson_invalidFallback() {
        String aiText = "AI返回失败";

        FoodRecognitionResult result = ReflectionTestUtils.invokeMethod(service, "parseAiJson", aiText);

        assertEquals("识别失败", result.getFoodName());
        assertEquals("AI 返回格式异常，请重试", result.getSuggestion());
        assertEquals(aiText, result.getRawResponse());
    }

    @Test
    void recognize_cacheHitObject() throws Exception {
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        FoodRecognitionResult cached = new FoodRecognitionResult();
        cached.setFoodName("缓存苹果");
        cached.setCalories(52.0);
        when(valueOps.get(anyString())).thenReturn(cached);

        FoodRecognitionResult result = service.recognize(file, 1);

        assertSame(cached, result);
        verifyNoInteractions(aiChatHistoryService);
    }

    @Test
    void recognize_cacheHitJson() throws Exception {
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        FoodRecognitionResult cached = new FoodRecognitionResult();
        cached.setFoodName("缓存米饭");
        cached.setCalories(116.0);
        String json = objectMapper.writeValueAsString(cached);
        when(valueOps.get(anyString())).thenReturn(json);

        FoodRecognitionResult result = service.recognize(file, 1);

        assertEquals("缓存米饭", result.getFoodName());
        assertEquals(116.0, result.getCalories(), 0.001);
        verifyNoInteractions(aiChatHistoryService);
    }
}
