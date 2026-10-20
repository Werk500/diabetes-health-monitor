package com.diabetes.monitor.job;


import com.diabetes.monitor.common.metrics.BusinessMetrics;
import com.diabetes.monitor.mapper.ExportTaskMapper;
import com.diabetes.monitor.service.ExportTaskDispatcher;
import com.diabetes.monitor.service.ExportTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@ConditionalOnProperty(
        name = "app.export.dispatch.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ExportTaskDispatchJob {
    @Resource
    private ExportTaskService exportTaskService;
    @Resource
    private ExportTaskDispatcher exportTaskDispatcher;
    @Resource
    private BusinessMetrics businessMetrics;

    /** 超过该时间仍在 RUNNING，认为执行线程异常退出 */
    @Value("${app.export.stuck-minutes:10}")
    private int stuckMinutes;

    /** PENDING 超过这么久还没有被领取，才重新投递 */
    @Value("${app.export.dispatch.pending-stale-seconds:15}")
    private int pendingStaleSeconds;

    /** 每轮最多补偿多少条，避免一次性塞满线程池 */
    @Value("${app.export.dispatch.batch-size:20}")
    private int batchSize;
    /**
     * fixedDelay：上一轮执行完成后，再等待指定时间开始下一轮。
     * 同一个应用实例内不会出现上一轮还没结束、下一轮又启动的情况。
     */
    @Scheduled(
            fixedDelayString = "${app.export.dispatch.interval-ms:30000}",
            initialDelayString = "${app.export.dispatch.initial-delay-ms:15000}"
    )
    public void dispatchPendingTasks() {

        // 1. 恢复卡死的 RUNNING 任务
        int recovered = exportTaskService.recoverStuckTasks(stuckMinutes);

        // 2. 查询长时间没有被领取的 PENDING 任务
        List<Long> pendingIds = exportTaskService.findDispatchablePendingIds(
                pendingStaleSeconds,
                batchSize
        );

        int accepted = 0;
        int rejected = 0;
        int failed = 0;

        for (Long taskId : pendingIds) {
            try {
                if(exportTaskDispatcher.dispatch(taskId)){
                    accepted++;
                } else {
                    rejected++;
                }
            }catch (Exception e) {
                // 单条任务派发异常不能影响本轮其他任务
                failed++;
                log.error("导出任务 {} 补偿派发异常", taskId, e);
            }
        }

        // 4. 更新积压指标
        long backlog = exportTaskService.countPending();
        businessMetrics.setExportBacklog(backlog);

        if (recovered > 0 || !pendingIds.isEmpty() || backlog > 0) {
            log.info(
                    "导出补偿完成：恢复卡死={}，待派发={}，投递成功={}，队列拒绝={}，异常={}，当前积压={}",
                    recovered, pendingIds.size(), accepted, rejected, failed, backlog
            );
        }

    }


}
