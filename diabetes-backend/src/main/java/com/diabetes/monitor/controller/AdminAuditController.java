package com.diabetes.monitor.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.dto.AdminAuditQueryDTO;
import com.diabetes.monitor.entity.AdminOperationLog;
import com.diabetes.monitor.service.AdminOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端操作审计")
@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {

    @Resource
    private AdminOperationLogService adminOperationLogService;

    @Operation(summary = "分页查询管理员操作审计")
    @GetMapping("/list")
    public Result<IPage<AdminOperationLog>> list(
            @Valid AdminAuditQueryDTO query
    ) {
        return Result.ok(adminOperationLogService.pageQuery(query));
    }
}
