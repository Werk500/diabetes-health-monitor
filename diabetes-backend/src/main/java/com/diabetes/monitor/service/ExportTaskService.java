package com.diabetes.monitor.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.monitor.dto.ExportSubmitRequest;
import com.diabetes.monitor.entity.ExportTask;

import java.util.List;

public interface ExportTaskService {
    Long submit(Integer userId, ExportSubmitRequest req);
    ExportTask getTask(Long id, Integer userId);          // 含归属校验
    Page<ExportTask> listMyTasks(Integer userId, int page, int size);
    boolean claim(Long id);
    void markSuccess(Long id, String filePath, long fileSize);
    void markFailed(Long id, String errorMsg);
    boolean retry(Long id, Integer userId);
    int recoverStuckTasks(int timeoutMinutes);
    /** 删除超过 days 天的成功/失败任务记录，返回删除条数 */
    int deleteExpired(int days);

    List<Long> findDispatchablePendingIds(int staleSeconds, int limit);
    long countPending();
}
