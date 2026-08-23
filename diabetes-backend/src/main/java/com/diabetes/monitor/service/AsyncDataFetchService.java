package com.diabetes.monitor.service;


import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import com.diabetes.monitor.entity.HealthRecordBody;
import com.diabetes.monitor.entity.HealthRecordDiet;
import com.diabetes.monitor.entity.HealthRecordExercise;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 异步数据查询服务接口
 * 用于并行查询各类健康数据
 */
public interface AsyncDataFetchService {

    /**
     * 异步查询血糖数据
     */
    CompletableFuture<List<HealthRecordBloodSugar>> fetchSugarList(Integer userId, LocalDateTime todayStart);

    /**
     * 异步查询饮食数据
     */
    CompletableFuture<List<HealthRecordDiet>> fetchDietList(Integer userId, LocalDateTime todayStart);

    /**
     * 异步查询运动数据
     */
    CompletableFuture<List<HealthRecordExercise>> fetchExerciseList(Integer userId, String today);

    /**
     * 异步查询最新身体指标
     */
    CompletableFuture<HealthRecordBody> fetchLatestBody(Integer userId);
}
