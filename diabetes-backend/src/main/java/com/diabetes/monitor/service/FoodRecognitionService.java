package com.diabetes.monitor.service;

import com.diabetes.monitor.dto.FoodRecognitionResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FoodRecognitionService {
    FoodRecognitionResult recognize(MultipartFile file, Integer userId) throws IOException;
}
