package com.diabetes.monitor.dto;

import lombok.Data;
import java.util.List;

@Data
public class DietStatsDTO {
    private List<String> mealNames;
    private List<Double> calorieValues;
    private List<Double> carbValues;
    private List<Double> proteinValues;
    private List<Double> fatValues;
}
