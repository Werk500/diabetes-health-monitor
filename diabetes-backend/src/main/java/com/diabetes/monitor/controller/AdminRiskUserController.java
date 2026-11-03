package com.diabetes.monitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.dto.AdminRiskUserQueryDTO;
import com.diabetes.monitor.service.AdminRiskUserService;
import com.diabetes.monitor.vo.AdminRiskUserOverviewVO;
import com.diabetes.monitor.vo.AdminRiskUserVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/risk-user")
@Tag(name = "管理端风险用户")
public class AdminRiskUserController {

    @Resource
    private AdminRiskUserService riskUserService;

    @GetMapping("/overview")
    public Result<AdminRiskUserOverviewVO> overview() {
        return Result.ok(riskUserService.overview());
    }

    @GetMapping("/list")
    public Result<IPage<AdminRiskUserVO>> list(
            @Valid AdminRiskUserQueryDTO query
    ) {
        return Result.ok(riskUserService.pageQuery(query));
    }
}
