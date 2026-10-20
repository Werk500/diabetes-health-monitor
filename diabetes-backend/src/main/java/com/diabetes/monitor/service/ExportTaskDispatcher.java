package com.diabetes.monitor.service;

import com.diabetes.monitor.common.metrics.BusinessMetrics;
import com.diabetes.monitor.job.ExportExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExportTaskDispatcher {
    @Resource
    private ExportExecutor exportExecutor;
    @Resource
    private BusinessMetrics businessMetrics;

    /**
     * 把任务投递到 exportTaskExecutor。
     * 返回 false 表示线程池拒绝，但任务仍是 PENDING，后续由补偿任务重新派发。
     */
    public boolean dispatch(Long taskId)  {

        try {
            // ExportExecutor.run 上有 @Async，
            // 这里通过 Spring 代理调用，方法会立即返回
            exportExecutor.run(taskId);
            businessMetrics.exportDispatch("accepted");
            return true;
        }catch (TaskRejectedException e) {
            businessMetrics.exportDispatch("rejected");
            log.warn("导出任务 {} 投递被拒绝，任务保持 PENDING，等待补偿派发", taskId);
            return false;
        }
    }
}
