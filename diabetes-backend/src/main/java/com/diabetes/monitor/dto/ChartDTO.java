package com.diabetes.monitor.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChartDTO {
    private List<String> dates;
    private List<Double> values;
}
