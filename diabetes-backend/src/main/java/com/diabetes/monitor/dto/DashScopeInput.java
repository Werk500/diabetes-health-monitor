package com.diabetes.monitor.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashScopeInput {
    private List<Map<String, String>> messages;
}