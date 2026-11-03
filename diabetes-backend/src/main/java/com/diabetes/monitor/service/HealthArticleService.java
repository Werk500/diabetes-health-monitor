package com.diabetes.monitor.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.diabetes.monitor.dto.AdminArticleQueryDTO;
import com.diabetes.monitor.dto.AdminArticleSaveDTO;
import com.diabetes.monitor.dto.ArticlePushResultDTO;
import com.diabetes.monitor.entity.HealthArticle;

import java.util.Map;

public interface HealthArticleService extends IService<HealthArticle> {
    IPage<HealthArticle> pageQuery(AdminArticleQueryDTO query);

    HealthArticle createArticle(AdminArticleSaveDTO dto);

    HealthArticle updateArticle(Integer id, AdminArticleSaveDTO dto);

    ArticlePushResultDTO pushArticle(Integer articleId);

    Map<String, Long> categoryStats();
}
