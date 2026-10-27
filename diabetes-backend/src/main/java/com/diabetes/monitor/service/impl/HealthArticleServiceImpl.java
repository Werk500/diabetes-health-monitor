package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.ResultCode;
import com.diabetes.monitor.dto.AdminArticleQueryDTO;
import com.diabetes.monitor.dto.AdminArticleSaveDTO;
import com.diabetes.monitor.dto.ArticlePushResultDTO;
import com.diabetes.monitor.entity.HealthArticle;
import com.diabetes.monitor.entity.SysUserArticle;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.mapper.HealthArticleMapper;
import com.diabetes.monitor.mapper.SysUserMapper;
import com.diabetes.monitor.mapper.SysUserArticleMapper;
import com.diabetes.monitor.service.HealthArticleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HealthArticleServiceImpl extends ServiceImpl<HealthArticleMapper, HealthArticle> implements HealthArticleService {

    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private SysUserArticleMapper sysUserArticleMapper;

    @Override
    public IPage<HealthArticle> pageQuery(AdminArticleQueryDTO query) {
        LambdaQueryWrapper<HealthArticle> wrapper =
                new LambdaQueryWrapper<HealthArticle>()
                        .eq(query.getCategory() != null,
                                HealthArticle::getCategory, query.getCategory())
                        .eq(query.getPushStatus() != null,
                                HealthArticle::getPushStatus, query.getPushStatus())
                        .eq(query.getStatus() != null,
                                HealthArticle::getStatus, query.getStatus());

        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(condition -> condition
                    .like(HealthArticle::getTitle, keyword)
                    .or()
                    .like(HealthArticle::getSummary, keyword)
                    .or()
                    .like(HealthArticle::getAuthor, keyword)
            );
        }

        wrapper.orderByDesc(HealthArticle::getCreateTime)
                .orderByDesc(HealthArticle::getId);

        return page(new Page<>(query.getPage(), query.getSize()), wrapper);
    }

    @Override
    public HealthArticle createArticle(AdminArticleSaveDTO dto) {
        HealthArticle article = new HealthArticle();
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary());
        article.setContent(dto.getContent());
        article.setCategory(dto.getCategory());
        article.setCoverImage(dto.getCoverImage());
        article.setAuthor(dto.getAuthor());
        article.setStatus(dto.getStatus());
        article.setPushStatus(0);
        article.setViewCount(0);

        if (!save(article)) {
            throw new BizException(
                    ResultCode.INTERNAL_ERROR,
                    "文章创建失败"
            );
        }

        return article;
    }

    @Override
    public HealthArticle updateArticle(Integer id, AdminArticleSaveDTO dto) {
        HealthArticle article = getById(id);
        if (article == null) {
            throw new BizException(ResultCode.NOT_FOUND, "文章不存在");
        }

        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary());
        article.setContent(dto.getContent());
        article.setCategory(dto.getCategory());
        article.setCoverImage(dto.getCoverImage());
        article.setAuthor(dto.getAuthor());
        article.setStatus(dto.getStatus());

        if (!updateById(article)) {
            throw new BizException(
                    ResultCode.INTERNAL_ERROR,
                    "文章更新失败"
            );
        }

        return article;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticlePushResultDTO pushArticle(Integer articleId) {
        //锁定文章行（FOR UPDATE），防两个管理员同时推送
        HealthArticle article = getOne(
                new LambdaQueryWrapper<HealthArticle>()
                        .eq(HealthArticle::getId, articleId)
                        .last("FOR UPDATE")
        );

        if (article == null) {
            throw new BizException(ResultCode.NOT_FOUND, "文章不存在");
        }

        //查目标用户
        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, 0)
                        .eq(SysUser::getStatus, 1)
        );

        List<Integer> userIds = users.stream()
                .map(SysUser::getId)
                .toList();

        int existingCount = userIds.isEmpty()
                ? 0
                : Math.toIntExact(//Math.toIntExact 把 Long 转 int，超出范围抛异常（数据量正常不会超）
                sysUserArticleMapper.selectCount(
                        new LambdaQueryWrapper<SysUserArticle>()
                                .eq(SysUserArticle::getArticleId, articleId)
                                .in(SysUserArticle::getUserId, userIds)
                )
        );


        LocalDateTime pushTime = LocalDateTime.now();

        List<SysUserArticle> records = users.stream()
                .map(user -> {
                    SysUserArticle relation = new SysUserArticle();
                    relation.setUserId(user.getId());
                    relation.setArticleId(articleId);
                    relation.setPushTime(pushTime);
                    return relation;
                })
                .toList();

        for (int start = 0; start < records.size(); start += 500) {
            int end = Math.min(start + 500, records.size());
            sysUserArticleMapper.insertIgnoreBatch(records.subList(start, end));
        }

        article.setPushStatus(1);
        if (!updateById(article)) {
            throw new BizException(
                    ResultCode.INTERNAL_ERROR,
                    "文章推送状态更新失败"
            );
        }


        int eligibleCount = users.size();

        return ArticlePushResultDTO.builder()
                .articleId(articleId)
                .eligibleCount(eligibleCount)// 目标用户总数
                .insertedCount(eligibleCount - existingCount) // 本次新插入数
                .skippedCount(existingCount)// 已存在被跳过数
                .build();
    }
}
