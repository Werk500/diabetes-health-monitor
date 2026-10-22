package com.diabetes.monitor.integration;

import com.diabetes.monitor.common.ExportStatus;
import com.diabetes.monitor.dto.ExportSubmitRequest;
import com.diabetes.monitor.entity.ExportTask;
import com.diabetes.monitor.mapper.ExportTaskMapper;
import com.diabetes.monitor.service.ExportTaskService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模拟两个实例同时领取同一条任务。
 *
 * <p>这里不能使用 @Transactional：任务必须提交后，两个并发线程才能同时看到它。
 * 清理数据时只删除本测试创建的任务 ID，避免影响已有演示数据。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("导出任务并发领取")
public class ExportTaskConcurrencyTest {

    private static final Integer USER_ID = 2;

    @Resource
    private ExportTaskService exportTaskService;

    @Resource
    private ExportTaskMapper exportTaskMapper;

    @Test
    @DisplayName("两个线程同时 claim，只有一个线程能成功")
    void claim_isAtomicWhenTwoWorkersCompete() throws Exception {
        ExportSubmitRequest request = new ExportSubmitRequest();
        request.setType("PDF");

        Long taskId = exportTaskService.submit(USER_ID, request);
        //用两个线程模拟两个应用实例，几乎同时去 claim 同一条任务。
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {

            // ready：两个线程都就绪后，主线程才放行，确保"同时"竞争而不是串行。
            CountDownLatch ready = new CountDownLatch(2);
            // start：主线程 countDown 后，两个线程才真正开始 claim，制造最大竞争窗口。
            CountDownLatch start = new CountDownLatch(1);
            // 记录 claim 成功的次数，期望恰好为 1（原子性：只能有一个赢家）。
            AtomicInteger successCount = new AtomicInteger();

            Runnable claimTask = () -> {
                ready.countDown();// 告知主线程：本线程已就绪
                try {

                    // 等待主线程统一发令；带超时避免测试因异常卡死
                    start.await(5, TimeUnit.SECONDS);
                    if (exportTaskService.claim(taskId)) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };

            Future<?> first = pool.submit(claimTask);
            Future<?> second = pool.submit(claimTask);

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            // 等待两个线程执行完毕；get 带超时，防止死锁时测试无限挂起
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);

            assertThat(successCount.get()).isEqualTo(1);

            ExportTask task = exportTaskMapper.selectById(taskId);
            assertThat(task.getStatus()).isEqualTo(ExportStatus.RUNNING);
            assertThat(task.getStartTime()).isNotNull();
        } finally {
            // 关闭线程池；shutdownNow 中断可能残留的任务，避免线程泄漏
            pool.shutdownNow();
            // 只清理本测试创建的任务，避免误删演示数据；即使断言失败也会执行
            exportTaskMapper.deleteById(taskId);
        }
    }
}
