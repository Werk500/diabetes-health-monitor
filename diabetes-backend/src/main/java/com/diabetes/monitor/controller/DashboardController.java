package com.diabetes.monitor.controller;

import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.dto.*;
import com.diabetes.monitor.entity.*;
import com.diabetes.monitor.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "数据看板", description = "用户健康数据概览仪表盘")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @Operation(summary = "获取用户仪表盘数据")
    @GetMapping("/{userId}")
    public Result<DashboardDTO> dashboard(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        return dashboardService.getDashboardData(userId,startDate,endDate);
    }
}