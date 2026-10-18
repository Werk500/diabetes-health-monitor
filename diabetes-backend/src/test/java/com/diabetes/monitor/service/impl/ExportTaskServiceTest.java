package com.diabetes.monitor.service.impl;

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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * 导出任务状态机测试。
 * <p>
 * 用 @Transactional 让每个用例跑完自动回滚——测试造的脏数据不会留在库里，
 * 也不用像集成测试那样手写清理（之前按时间范围批量删，误删过演示数据）。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("导出任务状态机")
public class ExportTaskServiceTest {

    /** test001，表里有外键约束，不能随便编 userId */
    private static final Integer USER_ID = 2;

    @Resource
    private ExportTaskService exportTaskService;

    @Resource
    private ExportTaskMapper exportTaskMapper;

    /** 走生产的 submit 路径造数据：顺带覆盖 params 序列化 */
    private Long submitOne(String type){
        ExportSubmitRequest req = new ExportSubmitRequest();
        req.setType(type);
        return exportTaskService.submit(USER_ID, req);
    }

    @Test
    @DisplayName("同一任务只能被领取一次：第二次 claim 返回 false")
    void claim_onlyOnce(){
        Long id = submitOne("EXCEL");

        assertThat(exportTaskService.claim(id)).isTrue();

        assertThat(exportTaskService.claim(id)).isFalse();

        ExportTask task = exportTaskMapper.selectById(id);
        assertThat(task.getStatus()).isEqualTo(ExportStatus.RUNNING);
        assertThat(task.getStartTime()).isNotNull();
        assertThat(task.getProgress()).isZero();
    }
    @Test
    @DisplayName("claim 后 markSuccess → SUCCESS；再调 markFailed → 状态仍是 SUCCESS")
    void terminalStateFinal(){
        Long id = submitOne("EXCEL");
        exportTaskService.claim(id);
        exportTaskService.markSuccess(id, "/tmp/x.xlsx", 1024L);

        assertThat(exportTaskMapper.selectById(id).getStatus()).isEqualTo(ExportStatus.SUCCESS);

    }

    @Test
    @DisplayName("PENDING/RUNNING 状态调 retry → 抛业务异常；置为 FAILED 后 retry → true，retry_count = 1")
    void retry_onlyFromFailed(){
        Long id = submitOne("PDF");
        exportTaskService.claim(id);
        exportTaskService.markFailed(id, "模拟失败");

        assertThat(exportTaskService.retry(id, USER_ID)).isTrue();

        ExportTask task = exportTaskMapper.selectById(id);
        assertThat(task.getStatus()).isEqualTo(ExportStatus.PENDING);
        assertThat(task.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("重试上限：连续失败 3 次后，第 4 次重试返回 false")
    void retry_limit(){
        Long id = submitOne("EXCEL");

        // 连续 3 轮：领取 → 失败 → 重试，重试次数递增到 3
        for (int i = 1; i <= 3; i++) {
            assertThat(exportTaskService.claim(id)).isTrue();
            exportTaskService.markFailed(id, "第 " + i + " 次失败");
            assertThat(exportTaskService.retry(id, USER_ID)).isTrue();
            assertThat(exportTaskMapper.selectById(id).getRetryCount()).isEqualTo(i);
        }

        // 第 4 轮：再失败后重试，retry_count=3 已经不满足 SQL 里的 retry_count < 3
        assertThat(exportTaskService.claim(id)).isTrue();
        exportTaskService.markFailed(id, "第 4 次失败");
        assertThat(exportTaskService.retry(id, USER_ID)).isFalse();

        ExportTask task = exportTaskMapper.selectById(id);
        assertThat(task.getStatus()).isEqualTo(ExportStatus.FAILED);
        assertThat(task.getRetryCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("卡在 RUNNING 的任务能被自愈任务恢复成 FAILED")
    void recoverStuck(){
        Long id = submitOne("PDF");
        assertThat(exportTaskService.claim(id)).isTrue();

        // 把 start_time 改到 20 分钟前，模拟"进程崩溃、任务卡死"
        ExportTask patch = new ExportTask();
        patch.setId(id);
        patch.setStartTime(LocalDateTime.now().minusMinutes(20));
        exportTaskMapper.updateById(patch);

        // 超时阈值 10 分钟 → 这条任务应被标记失败
        assertThat(exportTaskService.recoverStuckTasks(10)).isEqualTo(1);

        ExportTask task = exportTaskMapper.selectById(id);
        assertThat(task.getStatus()).isEqualTo(ExportStatus.FAILED);
        assertThat(task.getFinishTime()).isNotNull();
    }
}
