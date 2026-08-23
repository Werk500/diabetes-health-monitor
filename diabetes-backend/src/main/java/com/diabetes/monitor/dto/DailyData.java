package com.diabetes.monitor.dto;

import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import com.diabetes.monitor.entity.HealthRecordBody;
import com.diabetes.monitor.entity.HealthRecordDiet;
import com.diabetes.monitor.entity.HealthRecordExercise;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DailyData {
    private List<HealthRecordBloodSugar> sugarList;
    private List<HealthRecordDiet> dietList;
    private List<HealthRecordExercise> exerciseList;
    private HealthRecordBody latestBody;
}
