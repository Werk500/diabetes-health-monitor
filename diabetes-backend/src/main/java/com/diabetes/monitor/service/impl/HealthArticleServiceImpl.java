package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.diabetes.monitor.entity.HealthArticle;
import com.diabetes.monitor.entity.SysUserArticle;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.mapper.HealthArticleMapper;
import com.diabetes.monitor.mapper.SysUserMapper;
import com.diabetes.monitor.mapper.SysUserArticleMapper;
import com.diabetes.monitor.service.HealthArticleService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HealthArticleServiceImpl extends ServiceImpl<HealthArticleMapper, HealthArticle> implements HealthArticleService {

    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private SysUserArticleMapper sysUserArticleMapper;

    @Override
    @Transactional
    public void pushArticle(Integer articleId) {
        HealthArticle article = getById(articleId);
        if (article == null) return;
        article.setPushStatus(1);
        updateById(article);

        List<SysUser> users = sysUserMapper.selectList(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, 0).eq(SysUser::getStatus, 1)
        );
        for (SysUser user : users) {
            SysUserArticle ua = new SysUserArticle();
            ua.setUserId(user.getId());
            ua.setArticleId(articleId);
            ua.setPushTime(LocalDateTime.now());
            sysUserArticleMapper.insert(ua);
        }
    }
}
