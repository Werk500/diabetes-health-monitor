package com.diabetes.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.diabetes.monitor.entity.SysUserArticle;
import java.util.List;

public interface SysUserArticleService extends IService<SysUserArticle> {
    List<SysUserArticle> getUserArticles(Integer userId);
}
