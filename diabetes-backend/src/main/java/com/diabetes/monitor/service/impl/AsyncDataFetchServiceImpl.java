package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import com.diabetes.monitor.entity.HealthRecordBody;
import com.diabetes.monitor.entity.HealthRecordDiet;
import com.diabetes.monitor.entity.HealthRecordExercise;
import com.diabetes.monitor.service.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 异步数据查询服务实现类
 * 使用 @Async 实现并行查询
 */
@Service
@Slf4j
public class AsyncDataFetchServiceImpl implements AsyncDataFetchService {
    @Resource
    private HealthRecordBloodSugarService bloodSugarService;

    @Resource
    private HealthRecordDietService dietService;

    @Resource
    private HealthRecordExerciseService exerciseService;

    @Resource
    private HealthRecordBodyService bodyService;

    @Override
    @Async("taskExecutor")
    public CompletableFuture<List<HealthRecordBloodSugar>> fetchSugarList(Integer userId, LocalDateTime todayStart) {
        log.debug("开始查询血糖数据，userId: {}", userId);

        long startTime = System.currentTimeMillis();
        QueryWrapper<HealthRecordBloodSugar> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .ge("measure_time", todayStart)
                .orderByAsc("measure_time");

        List<HealthRecordBloodSugar> result = bloodSugarService.list(wrapper);

        log.debug("血糖查询完成，userId: {}, 耗时: {}ms, 记录数: {}",
                userId, System.currentTimeMillis() - startTime, result.size());

        return CompletableFuture.completedFuture(result);

    }

    @Override
    @Async("taskExecutor")
    public CompletableFuture<List<HealthRecordDiet>> fetchDietList(Integer userId, LocalDateTime todayStart) {
        log.debug("开始查询饮食数据，userId: {}", userId);
        long startTime = System.currentTimeMillis();

        QueryWrapper<HealthRecordDiet> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .ge("eat_time", todayStart)
                .orderByAsc("eat_time");

        List<HealthRecordDiet> result = dietService.list(wrapper);

        log.debug("饮食查询完成，userId: {}, 耗时: {}ms, 记录数: {}",
                userId, System.currentTimeMillis() - startTime, result.size());

        return CompletableFuture.completedFuture(result);
    }

    @Override
    @Async("taskExecutor")
    public CompletableFuture<List<HealthRecordExercise>> fetchExerciseList(Integer userId, String today) {
        log.debug("开始查询运动数据，userId: {}, today: {}", userId, today);
        long startTime = System.currentTimeMillis();

        QueryWrapper<HealthRecordExercise> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("exercise_date", today);

        List<HealthRecordExercise> result = exerciseService.list(wrapper);

        log.debug("运动查询完成，userId: {}, 耗时: {}ms, 记录数: {}",
                userId, System.currentTimeMillis() - startTime, result.size());

        return CompletableFuture.completedFuture(result);
    }


    @Override
    public CompletableFuture<HealthRecordBody> fetchLatestBody(Integer userId) {
        log.debug("开始查询身体指标，userId: {}", userId);
        long startTime = System.currentTimeMillis();

        HealthRecordBody result = bodyService.getLatest(userId);

        log.debug("身体指标查询完成，userId: {}, 耗时: {}ms",
                userId, System.currentTimeMillis() - startTime);

        return CompletableFuture.completedFuture(result);
    }
}
