package com.diabetes.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.diabetes.monitor.entity.HealthArticle;

public interface HealthArticleService extends IService<HealthArticle> {
    void pushArticle(Integer articleId);
}
