package com.diabetes.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface HealthRecordBloodSugarMapper extends BaseMapper<HealthRecordBloodSugar> {

    @Select("""
        SELECT DISTINCT user_id
        FROM health_record_blood_sugar
        WHERE measure_time >= #{from} AND measure_time < #{to}
        """)
    List<Integer> selectBloodSugarIdsBetween(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);
}