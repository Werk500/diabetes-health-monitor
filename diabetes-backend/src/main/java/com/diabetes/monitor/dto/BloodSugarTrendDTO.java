package com.diabetes.monitor.dto;

import lombok.Data;
import java.util.List;

@Data
public class BloodSugarTrendDTO {
    private List<String> dates;
    private List<Double> fastingValues;
    private List<Double> beforeMealValues;
    private List<Double> afterMealValues;
    private List<Double> bedtimeValues;
}
