package com.diabetes.monitor.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.common.ResultCode;
import com.diabetes.monitor.dto.AdminArticleQueryDTO;
import com.diabetes.monitor.dto.AdminArticleSaveDTO;
import com.diabetes.monitor.dto.ArticlePushResultDTO;
import com.diabetes.monitor.entity.HealthArticle;
import com.diabetes.monitor.service.HealthArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理端文章运营")
@RestController
@RequestMapping("/api/admin/article")
public class AdminArticleController {

    @Resource
    private HealthArticleService articleService;

    @Operation(summary = "分页查询文章")
    @GetMapping("/list")
    public Result<IPage<HealthArticle>> list(
            @Valid AdminArticleQueryDTO query
    ) {
        return Result.ok(articleService.pageQuery(query));
    }

    @Operation(summary = "更新文章")
    @PutMapping("/{id}")
    public Result<HealthArticle> update(
            @PathVariable Integer id,
            @Valid @RequestBody AdminArticleSaveDTO dto
    ) {
        return Result.ok(articleService.updateArticle(id, dto));
    }

    @Operation(summary = "删除文章")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        boolean removed = articleService.removeById(id);
        if (!removed) {
            throw new BizException(ResultCode.NOT_FOUND, "文章不存在");
        }
        return Result.ok();
    }

    @Operation(summary = "推送文章")
    @PutMapping("/push/{id}")
    public Result<ArticlePushResultDTO> push(@PathVariable Integer id) {
        return Result.ok(articleService.pushArticle(id));
    }

    @Operation(summary = "新增文章")
    @PostMapping
    public Result<HealthArticle> create(
            @Valid @RequestBody AdminArticleSaveDTO dto
    ) {
        return Result.ok(articleService.createArticle(dto));
    }

    @Operation(summary = "文章分类统计")
    @GetMapping("/categoryStats")
    public Result<Map<String, Long>> categoryStats() {
        return Result.ok(articleService.categoryStats());
    }
}
