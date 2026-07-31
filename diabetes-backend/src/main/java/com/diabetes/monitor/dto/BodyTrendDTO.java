package com.diabetes.monitor.dto;

import lombok.Data;
import java.util.List;

@Data
public class BodyTrendDTO {
    private List<String> dates;
    private List<Double> weightValues;
    private List<Double> bmiValues;
    private List<Double> bodyFatValues;
    private List<Integer> systolicValues;
    private List<Integer> diastolicValues;
    private List<Integer> heartRateValues;
}
