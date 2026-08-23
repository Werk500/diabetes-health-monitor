package com.diabetes.monitor.service;


import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.dto.DashboardDTO;

/**
 * 仪表盘服务接口
 * 负责聚合用户健康数据，生成仪表盘概览
 */
public interface DashboardService {

    /**
     * 获取用户仪表盘数据
     *
     * @param userId    用户ID
     * @param startDate 开始日期（格式：yyyy-MM-dd），可选
     * @param endDate   结束日期（格式：yyyy-MM-dd），可选
     * @return 仪表盘数据
     */
    Result<DashboardDTO> getDashboardData(Integer userId, String startDate, String endDate);
}
