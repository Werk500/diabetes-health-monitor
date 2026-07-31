package com.diabetes.monitor.service;

import com.diabetes.monitor.common.Result;

import java.util.Map;

public interface AiService {
    Result chat(Map<String, String> body);
}
