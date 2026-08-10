package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.config.JwtUtil;
import com.diabetes.monitor.config.RsaUtil;
import com.diabetes.monitor.dto.LoginDTO;
import com.diabetes.monitor.entity.HealthArticle;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.entity.SysUserArticle;
import com.diabetes.monitor.service.HealthArticleService;
import com.diabetes.monitor.service.SysUserArticleService;
import com.diabetes.monitor.service.SysUserService;
import com.diabetes.monitor.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private RsaUtil rsaUtil;

    @Resource
    private HealthArticleService healthArticleService;
    @Resource
    private SysUserArticleService sysUserArticleService;

    @Override
    public Result login(LoginDTO loginDTO) {
        String decrypt = rsaUtil.decrypt(loginDTO.getPassword());
        SysUser user = sysUserService.login(loginDTO.getUsername(), decrypt);
        if (user == null) throw new BizException("用户名或密码错误");;
        Map<String, Object> data = new HashMap<>();
        data.put("token", jwtUtil.generateToken(user.getId(), user.getUsername(),  user.getRole()));
        data.put("user", user);

        return Result.ok(data);
    }

    @Override
    public Result register(SysUser user) {
        long count = sysUserService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) throw new BizException("用户名已存在");;
        user.setPassword(rsaUtil.decrypt(user.getPassword()));
        sysUserService.register(user);
        return Result.ok("register success");
    }

    @Override
    public Result info(Integer id) {
        SysUser user = sysUserService.getById(id);
        if (user != null) user.setPassword(null);
        return Result.ok(user);
    }

    @Override
    public Result update(SysUser user) {
        user.setPassword(null);
        sysUserService.updateById(user);
        return Result.ok("update success");
    }

    @Override
    public Result list() {
        List<SysUser> users = sysUserService.list();
        users.forEach(u -> u.setPassword(null));

        return Result.ok(users);
    }

    @Override
    public Result toggleStatus(Integer id) {
        SysUser user = sysUserService.getById(id);
        if (user != null) {
            user.setStatus(user.getStatus() == 1 ? 0 : 1);
            sysUserService.updateById(user);
        }
        return Result.ok();
    }

    @Override
    public Result articleList(Integer page, Integer size, Authentication authentication) {
        // ============ 1. 获取当前登录用户ID ============
        // 从Spring Security上下文中获取当前认证用户的主键ID
        Integer userId = (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // ============ 2. 查询用户关联的文章关系 ============
        List<SysUserArticle> userArticles = sysUserArticleService.getUserArticles(userId);

        // ============ 3. 提取文章ID列表 ============
        List<Integer> articleIds = userArticles.stream()
                .map(SysUserArticle::getArticleId)
                .toList();

        // ============ 4. 空值判断：提前返回 ============
        if (articleIds.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        // ============ 5. 批量查询文章详情 ============
        // 根据文章ID列表，批量查询文章详细信息
        // listByIds 是MyBatis-Plus提供的方法，会生成 IN 查询
        // 缺点：查询结果顺序不保证与传入ID顺序一致
        List<HealthArticle> articles = healthArticleService.listByIds(articleIds);

        // ============ 6. 重建文章顺序 ============
        // 因为 listByIds 不保证顺序，需要手动保持与 articleIds 一致
        // 第一步：将文章列表转换为 Map<文章ID, 文章对象>
        Map<Integer, HealthArticle> articleMap = articles.stream()
                .collect(Collectors.toMap(HealthArticle::getId, Function.identity()));

        // 第二步：按照 articleIds 的顺序重新排列文章
        // 这样保证了返回的文章顺序与用户关联顺序一致
        List<HealthArticle> sortedArticles = articleIds.stream()
                .map(articleMap::get) // 根据ID从Map中获取文章
                .filter(Objects::nonNull)// 过滤掉不存在的文章（防止空指针）
                .collect(Collectors.toList());

        // ============ 7. 内存分页处理 ============
        // 注意：这里是在内存中分页，适用于数据量小的场景
        int start = (page - 1) * size;
        int end = Math.min(start + size, sortedArticles.size());
        List<HealthArticle> pageList = sortedArticles.subList(start, end);

        // ============ 8. 构建分页响应对象 ============
        // 手动构建分页结果，包含：数据列表、总记录数、当前页、每页大小
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageList);
        result.put("total", sortedArticles.size());
        result.put("current", page);
        result.put("size", size);

        return Result.ok(result);
    }
}
