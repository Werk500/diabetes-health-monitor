package com.diabetes.monitor.dto;

import lombok.Data;

@Data
public class DashScopeRequest {
    private String model;
    private DashScopeInput input;
    private DashScopeParameters parameters;
}