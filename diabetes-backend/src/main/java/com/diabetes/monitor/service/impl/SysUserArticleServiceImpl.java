package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diabetes.monitor.entity.SysUserArticle;
import com.diabetes.monitor.mapper.SysUserArticleMapper;
import com.diabetes.monitor.service.SysUserArticleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysUserArticleServiceImpl extends ServiceImpl<SysUserArticleMapper, SysUserArticle> implements SysUserArticleService {

    @Override
    public List<SysUserArticle> getUserArticles(Integer userId) {
        LambdaQueryWrapper<SysUserArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserArticle::getUserId, userId)
               .orderByDesc(SysUserArticle::getPushTime);
        return list(wrapper);
    }
}
