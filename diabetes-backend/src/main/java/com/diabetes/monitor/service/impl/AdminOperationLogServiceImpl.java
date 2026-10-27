package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.ResultCode;
import com.diabetes.monitor.dto.AdminAuditQueryDTO;
import com.diabetes.monitor.entity.AdminOperationLog;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.mapper.AdminOperationLogMapper;
import com.diabetes.monitor.service.AdminOperationLogService;
import com.diabetes.monitor.service.SysUserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 管理员操作审计日志服务实现。
 *
 * <p>核心设计：用不同的事务传播级别区分"成功日志"与"失败日志"的写入策略——</p>
 * <ul>
 *     <li><b>成功日志</b>：{@link Propagation#MANDATORY}，必须挂在调用方的业务事务中，
 *         与业务操作同生共死。业务成功则日志提交，业务回滚则日志一并回滚，
 *         避免出现"业务回滚了、成功日志却留下"的脏数据。</li>
 *     <li><b>失败日志</b>：{@link Propagation#REQUIRES_NEW}，独立新事务提交。
 *         即使调用方业务事务回滚，失败记录也能持久化，保证"失败必留痕"。</li>
 * </ul>
 *
 * <p>此外，日志中冗余保存操作人用户名、请求方法/路径/IP 等快照信息，
 * 便于日后审计追溯（用户改名或删除后仍能还原当时上下文）。</p>
 */
@Service
public class AdminOperationLogServiceImpl implements AdminOperationLogService {


    @Resource
    private AdminOperationLogMapper adminOperationLogMapper;

    @Resource
    private SysUserService sysUserService;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)//强制要求当前方法必须在一个已经存在的事务中运行。如果调用该方法时没有活动事务，就会直接抛出异常。
    public void recordSuccess(Integer operatorId, String actionCode, String targetType, String targetId, String detail) {
        save(operatorId, actionCode, targetType, targetId, detail, true);
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            rollbackFor = Exception.class
    )
    public void recordFailure(Integer operatorId, String actionCode, String targetType, String targetId, String detail) {
        save(operatorId, actionCode, targetType, targetId, detail, false);
    }

    @Override
    public IPage<AdminOperationLog> pageQuery(AdminAuditQueryDTO query) {
        if(query.getStartTime() != null
                && query.getEndTime() != null
                && query.getStartTime().isAfter(query.getEndTime())) {
            throw new BizException(ResultCode.BAD_REQUEST, "开始时间不能晚于结束时间");
        }

        // 动态拼接等值/范围条件。
        // 使用 MyBatis-Plus 的三参重载 eq/ge/le(boolean condition, 列, 值)：
        // 第一个参数为 false 时该条件不拼接，天然实现动态 SQL，无需写一堆 if。
        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<AdminOperationLog>()
                .eq(StringUtils.hasText(query.getActionCode()),
                        AdminOperationLog::getActionCode,
                        query.getActionCode())
                .eq(query.getSuccess() != null,
                        AdminOperationLog::getSuccess,
                        query.getSuccess())
                .ge(query.getStartTime() != null,
                        AdminOperationLog::getCreateTime,
                        query.getStartTime())
                .le(query.getEndTime() != null,
                        AdminOperationLog::getCreateTime,
                        query.getEndTime());


        if(StringUtils.hasText(query.getKeyword())){
            String keyword = query.getKeyword().trim();
            wrapper.and(condition -> condition
                    .like(AdminOperationLog::getOperatorName, keyword)
                    .or()
                    .like(AdminOperationLog::getTargetId, keyword)
                    .or()
                    .like(AdminOperationLog::getRequestPath, keyword)
            );
        }

        wrapper.orderByDesc(AdminOperationLog::getCreateTime)
                .orderByDesc(AdminOperationLog::getId);

        // 执行分页查询。
        return adminOperationLogMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()),
                wrapper
        );
    }

    private void save(Integer operatorId, String actionCode, String targetType, String targetId, String detail, boolean success) {
        AdminOperationLog log = new AdminOperationLog();
        log.setOperatorId(operatorId);
        log.setOperatorName(resolveOperatorName(operatorId));
        log.setActionCode(actionCode);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setSuccess(success ? 1 : 0);
        // 详情限长 1000，防止超长文本撑爆列或写入失败
        log.setDetail(truncate(detail, 1000));

        HttpServletRequest request = currentRequest();
        if (request != null) {                        // 非 Web 上下文时优雅跳过
            log.setRequestMethod(truncate(request.getMethod(), 10));
            log.setRequestPath(truncate(request.getRequestURI(), 255));
            log.setRequestIp(truncate(request.getRemoteAddr(), 64));
        }
        adminOperationLogMapper.insert(log);
    }

    /**
     * 解析操作人用户名，用于日志快照。
     *
     * <p>查不到用户（已删除/不存在）时退化为 "unknown"，不因查不到而中断日志记录。</p>
     */
    private String resolveOperatorName(Integer operatorId) {

        SysUser operator = sysUserService.getById(operatorId);
        return operator == null ? "unknown": truncate(operator.getUsername(),50);
    }

    /**
     * 按最大长度截断字符串，避免超出数据库列长度导致写入失败。
     *
     * @param value     原始值，可为 null
     * @param maxLength 最大保留长度
     * @return 截断后的字符串；入参为 null 或未超长时原样返回
     */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 获取当前线程绑定的 HTTP 请求。
     *
     * <p>通过 {@link RequestContextHolder} 从线程上下文中取，使用 Java 16+ 的
     * instanceof 模式匹配安全向下转型；非 Web 环境返回 null。</p>
     */
    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }



}
