package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.ExportStatus;
import com.diabetes.monitor.common.ResultCode;
import com.diabetes.monitor.common.metrics.BusinessMetrics;
import com.diabetes.monitor.dto.ExportSubmitRequest;
import com.diabetes.monitor.entity.ExportTask;
import com.diabetes.monitor.mapper.ExportTaskMapper;
import com.diabetes.monitor.service.ExportTaskService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class ExportTaskServiceImpl implements ExportTaskService {

    @Resource
    private ExportTaskMapper mapper;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private BusinessMetrics businessMetrics;

    @Override
    public Long submit(Integer userId, ExportSubmitRequest req) {

        String type = req.getType()
                .trim()//去掉首尾空白字符（空格、制表符、换行等）。
                .toUpperCase(Locale.ROOT);//转成全大写。

        if (!"PDF".equals(type) && !"EXCEL".equals(type)) {
            throw new BizException(ResultCode.BAD_REQUEST, "不支持的导出类型");
        }
        req.setType(type);

        // 参数在提交时就校验，别等异步任务跑起来才失败（否则用户只看到一条 FAILED 记录）
        if ((req.getStartDate() == null) != (req.getEndDate() == null)) {
            throw new BizException(ResultCode.BAD_REQUEST, "开始日期和结束日期需同时填写");
        }
        if (req.getStartDate() != null) {
            if (req.getStartDate().isAfter(req.getEndDate())) {
                throw new BizException(ResultCode.BAD_REQUEST, "开始日期不能晚于结束日期");
            }
            if (ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) > 89) {
                throw new BizException(ResultCode.BAD_REQUEST, "导出区间不能超过 90 天");
            }
        }

        ExportTask task = new ExportTask();
        task.setUserId(userId);
        task.setType(req.getType());
        task.setStatus(ExportStatus.PENDING);
        task.setProgress(0);
        task.setRetryCount(0);

        try {
            task.setParams(objectMapper.writeValueAsString(req));
        } catch (JsonProcessingException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "导出参数不合法");
        }

        mapper.insert(task);
        businessMetrics.exportTaskSubmitted(task.getType());
        return task.getId();
    }

    @Override
    public ExportTask getTask(Long id, Integer userId) {
        ExportTask task = mapper.selectById(id);
        if (task == null||!task.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "任务不存在或无权限");
        }
        return task;
    }

    @Override
    public Page<ExportTask> listMyTasks(Integer userId, int page, int size) {
        return mapper.selectPage(new Page<>(page,Math.min(size,50)),
                new LambdaQueryWrapper<ExportTask>()
                        .eq(ExportTask::getUserId,userId)
                        .orderByDesc(ExportTask::getId));
    }

    @Override
    public boolean claim(Long id) {
        boolean ok = mapper.claim(id) == 1;
        if (!ok) log.info("任务 {} 领取失败（已被领取或状态不是 PENDING）", id);
        return ok;
    }

    @Override
    public void markSuccess(Long id, String filePath, long fileSize) {
        if (mapper.markSuccess(id, filePath, fileSize) == 0) {
            log.warn("任务 {} 完成上报被忽略（状态不是 RUNNING）", id);
        }

    }

    @Override
    public void markFailed(Long id, String errorMsg) {
        String msg = errorMsg == null ? "" : errorMsg;
        if(msg.length() > 500)  msg = msg.substring(0, 500);
        mapper.markFailed(id, msg);

    }

    @Override
    public boolean retry(Long id, Integer userId) {
        ExportTask task = getTask(id, userId);
        if(!ExportStatus.FAILED.equals(task.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "只有失败的任务可以重试");
        }
        return mapper.resetForRetry(id) == 1;
    }

    @Override
    public int recoverStuckTasks(int timeoutMinutes) {
        int n = mapper.markStuckAsFailed(timeoutMinutes);
        if (n > 0) log.warn("恢复了 {} 个卡在 RUNNING 的导出任务", n);
        return n;
    }

    @Override
    public int deleteExpired(int days) {
        return mapper.deleteExpired(days);
    }

    @Override
    public List<Long> findDispatchablePendingIds(int staleSeconds, int limit) {

        if (staleSeconds <= 0 || limit <= 0) {
            return Collections.emptyList();
        }

        List<Long> ids = mapper.findDispatchablePendingIds(staleSeconds, limit);
        return ids != null ? ids : Collections.emptyList();
    }

    @Override
    public long countPending() {
        return mapper.selectCount(Wrappers.<ExportTask>lambdaQuery()
                .eq(ExportTask::getStatus, ExportStatus.PENDING));
    }
}
