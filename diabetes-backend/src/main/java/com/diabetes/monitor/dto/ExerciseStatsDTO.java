package com.diabetes.monitor.dto;

import lombok.Data;
import java.util.List;

@Data
public class ExerciseStatsDTO {
    private List<String> dates;
    private List<Double> calorieBurnedValues;
    private List<Integer> durationValues;
    private List<String> exerciseTypes;
}
