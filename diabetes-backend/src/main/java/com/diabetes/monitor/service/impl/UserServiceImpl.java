package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.ResultCode;
import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.config.RsaUtil;
import com.diabetes.monitor.dto.*;
import com.diabetes.monitor.entity.HealthArticle;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.entity.SysUserArticle;
import com.diabetes.monitor.service.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private RsaUtil rsaUtil;

    @Resource
    private HealthArticleService healthArticleService;
    @Resource
    private SysUserArticleService sysUserArticleService;
    @Resource
    private AuthTokenService authTokenService;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private AdminOperationLogService adminOperationLogService;

    @Override
    public LoginResult login(LoginDTO loginDTO) {
        String decrypt = rsaUtil.decrypt(loginDTO.getPassword());

        SysUser user = sysUserService.login(loginDTO.getUsername(), decrypt);

        if (user == null) throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");

        TokenPair tokens = authTokenService.issue(user);
        user.setPassword(null);

        return new LoginResult(user, tokens);
    }

    @Override
    public Result register(SysUser user) {
        long count = sysUserService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) throw new BizException(ResultCode.BAD_REQUEST, "用户名已存在");
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
    public Result changePassword(Integer userId, ChangePasswordDTO dto) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户不存在");
        }

        String oldPassword = rsaUtil.decrypt(dto.getOldPassword());
        String newPassword = rsaUtil.decrypt(dto.getNewPassword());

        if (newPassword.length() < 6 || newPassword.length() > 32) {
            throw new BizException(ResultCode.BAD_REQUEST, "新密码长度需为6到32位");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException(ResultCode.BAD_REQUEST, "原密码错误");
        }
        if (oldPassword.equals(newPassword)) {
            throw new BizException(ResultCode.BAD_REQUEST, "新密码不能与原密码相同");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserService.updateById(user);

        // 密码变更后，所有设备都必须重新登录
        authTokenService.revokeAll(userId);
        return Result.ok("密码修改成功，请重新登录");
    }

    @Override
    public Result list() {
        List<SysUser> users = sysUserService.list();
        users.forEach(u -> u.setPassword(null));

        return Result.ok(users);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateStatus(Integer operatorId, Integer targetId, Integer targetStatus) {

        String actionCode = Integer.valueOf(0).equals(targetStatus)
                ? AdminOperationLogService.USER_DISABLE
                : AdminOperationLogService.USER_ENABLE;

        try {
            SysUser target = requireMutableUser(operatorId, targetId);

            if (Integer.valueOf(targetStatus).equals(target.getStatus())) {
                adminOperationLogService.recordSuccess(
                        operatorId,
                        actionCode,
                        AdminOperationLogService.TARGET_USER,
                        String.valueOf(targetId),
                        "用户状态未变化，当前状态为 " + targetStatus
                );
                return Result.ok("用户状态未变化");
            }

            int previousStatus = target.getStatus();
            target.setStatus(targetStatus);

            if (!sysUserService.updateById(target)) {
                throw new BizException(ResultCode.INTERNAL_ERROR, "用户状态更新失败");
            }

            if (Integer.valueOf(0).equals(targetStatus)) {
                authTokenService.revokeAll(targetId);
            }

            adminOperationLogService.recordSuccess(
                    operatorId,
                    actionCode,
                    AdminOperationLogService.TARGET_USER,
                    String.valueOf(targetId),
                    "用户状态由 " + previousStatus + " 变更为 " + targetStatus
            );

            return Result.ok(targetStatus == 1 ? "用户已启用" : "用户已禁用");
        } catch (RuntimeException e) {
            recordFailureSafely(
                    operatorId,
                    actionCode,
                    targetId,
                    e.getMessage()
            );
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteUser(Integer operatorId, Integer targetId) {
        try {
            requireMutableUser(operatorId, targetId);

            authTokenService.revokeAll(targetId);

            if (!sysUserService.removeById(targetId)) {
                throw new BizException(ResultCode.INTERNAL_ERROR, "用户删除失败");
            }

            adminOperationLogService.recordSuccess(
                    operatorId,
                    AdminOperationLogService.USER_DELETE,
                    AdminOperationLogService.TARGET_USER,
                    String.valueOf(targetId),
                    "用户已逻辑删除"
            );

            return Result.ok("用户已删除");
        } catch (RuntimeException e) {
            recordFailureSafely(
                    operatorId,
                    AdminOperationLogService.USER_DELETE,
                    targetId,
                    e.getMessage()
            );
            throw e;
        }
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

    @Override
    public Result updateProfile(Integer id, ProfileUpdateDTO dto) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户不存在");
        }
        user.setPhone(dto.getPhone());
        user.setRealName(dto.getRealName());

        if (!sysUserService.updateById(user)) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "资料更新失败");
        }

        return Result.ok("update success");
    }

    private SysUser requireMutableUser(Integer operatorId, Integer targetId) {
        SysUser target = sysUserService.getById(targetId);

        if (target == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }

        if (operatorId.equals(targetId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "不能操作当前登录管理员");
        }

        if (Integer.valueOf(1).equals(target.getRole())) {
            throw new BizException(ResultCode.BAD_REQUEST, "管理员账号受保护，不能启用、禁用或删除");
        }

        return target;
    }

    private void recordFailureSafely(Integer operatorId,
                                     String actionCode,
                                     Integer targetId,
                                     String errorMessage) {
        try {
            adminOperationLogService.recordFailure(
                    operatorId,
                    actionCode,
                    AdminOperationLogService.TARGET_USER,
                    String.valueOf(targetId),
                    errorMessage == null ? "操作失败" : errorMessage
            );
        } catch (Exception auditException) {
            // 审计写入失败不能覆盖原始业务异常
            log.error("记录管理员失败操作审计时发生异常", auditException);
        }
    }
}
