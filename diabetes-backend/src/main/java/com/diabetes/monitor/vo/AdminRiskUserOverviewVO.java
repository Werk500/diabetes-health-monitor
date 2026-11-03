package com.diabetes.monitor.vo;

import lombok.Data;

@Data
public class AdminRiskUserOverviewVO {
    private Long highRiskUsers;
    private Long mediumRiskUsers;
    private Long observationUsers;
    private Long inactiveUsers;
}
