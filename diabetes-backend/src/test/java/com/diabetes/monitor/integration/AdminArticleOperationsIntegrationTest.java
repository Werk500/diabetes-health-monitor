package com.diabetes.monitor.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.monitor.entity.HealthArticle;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.entity.SysUserArticle;
import com.diabetes.monitor.mapper.SysUserArticleMapper;
import com.diabetes.monitor.service.HealthArticleService;
import com.diabetes.monitor.service.SysUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("管理员文章运营")
public class AdminArticleOperationsIntegrationTest extends BaseIntegrationTest {

     @Resource
    private HealthArticleService articleService;
     @Resource
    private SysUserArticleMapper sysUserArticleMapper;
     @Resource
    private SysUserService sysUserService;


    @Test
    @DisplayName("普通用户不能访问文章管理接口")
    void articleAdminApis_requireAdmin() throws Exception{
         String userToken = getTestUserToken();

         mockMvc.perform(get("/api/admin/article/list")
                 .header("Authorization","Bearer " + userToken))
                 .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("文章只推送给启用状态的普通用户")
    void pushArticle_sendsOnlyToEnabledNormalUsers() throws Exception{
        // 1. 准备文章和启用普通用户
        HealthArticle article = createArticle("推送测试文章");
        SysUser enabledUserA = createUser("push_enabled_a_", 0, 1);
        SysUser enabledUserB = createUser("push_enabled_b_", 0, 1);
        SysUser disabledUser = createUser("push_disabled_", 0, 0);
        SysUser anotherAdmin = createUser("push_admin_", 1, 1);

        // Act：调用真实管理员推送接口
        mockMvc.perform(put("/api/admin/article/push/" + article.getId())
                        .header("Authorization", "Bearer " + getAdminToken()))
                .andExpect(status().isOk());

        HealthArticle updated = articleService.getById(article.getId());
        assertThat(updated).isNotNull();
        assertThat(updated.getPushStatus()).isEqualTo(1);

        List<SysUserArticle> relations = sysUserArticleMapper.selectList(
                new LambdaQueryWrapper<SysUserArticle>()
                        .eq(SysUserArticle::getArticleId, article.getId()));

        Set<Integer> pushedUserIds = relations.stream()
                .map(SysUserArticle::getUserId).collect(Collectors.toSet());

        assertThat(pushedUserIds)
                .contains(enabledUserA.getId(), enabledUserB.getId())
                .doesNotContain(disabledUser.getId(), anotherAdmin.getId());

    }

    @Test
    @DisplayName("重复推送同一篇文章不会产生重复关系")
    void pushArticle_isIdempotent() throws Exception{

        HealthArticle article = createArticle("幂等推送测试文章");
        SysUser enabledUser = createUser("idempotent_user_", 0, 1);
        String adminToken = getAdminToken();

        // Act：连续推送两次
        mockMvc.perform(put("/api/admin/article/push/" + article.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/admin/article/push/" + article.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        List<SysUserArticle> relations = sysUserArticleMapper.selectList(
                new LambdaQueryWrapper<SysUserArticle>()
                        .eq(SysUserArticle::getArticleId, article.getId())
                        .eq(SysUserArticle::getUserId, enabledUser.getId())
        );

        assertThat(relations).hasSize(1);
    }




    private HealthArticle createArticle(String title) {
         HealthArticle article = new HealthArticle();
         article.setTitle(title);
         article.setSummary("测试摘要");
         article.setContent("测试文章正文");
         article.setCategory(1);
         article.setAuthor("测试作者");
         article.setStatus(1);
         article.setPushStatus(0);
        article.setViewCount(0);

        articleService.save(article);
        return article;

    }

    private SysUser createUser(String prefix,int role,int userStatus){
         SysUser sysUser = new SysUser();
         sysUser.setUsername(prefix+System.nanoTime());
        sysUser.setPassword("123456");
        sysUser.setRole(role);
        sysUser.setStatus(userStatus);

        sysUserService.register(sysUser);
        return sysUser;
    }
}
