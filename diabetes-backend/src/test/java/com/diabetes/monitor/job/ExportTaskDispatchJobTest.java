package com.diabetes.monitor.job;


import com.diabetes.monitor.common.metrics.BusinessMetrics;
import com.diabetes.monitor.service.ExportTaskDispatcher;
import com.diabetes.monitor.service.ExportTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExportTaskDispatchJobTest {

    @Mock
    private ExportTaskService exportTaskService;

    @Mock
    private ExportTaskDispatcher exportTaskDispatcher;

    @Mock
    private BusinessMetrics businessMetrics;

    @InjectMocks
    private ExportTaskDispatchJob job;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(job, "stuckMinutes", 10);
        ReflectionTestUtils.setField(job, "pendingStaleSeconds", 15);
        ReflectionTestUtils.setField(job, "batchSize", 20);
    }


    @Test
    void dispatchPendingTasks_dispatchesAllIds(){
         when(exportTaskService.recoverStuckTasks(10)).thenReturn(1);
         when(exportTaskService.findDispatchablePendingIds(15,20)).thenReturn(List.of(101L,102L));
         when(exportTaskDispatcher.dispatch(101L)).thenReturn(true);
         when(exportTaskDispatcher.dispatch(102L)).thenReturn(true);
         when(exportTaskService.countPending()).thenReturn(0L);

         job.dispatchPendingTasks();

         verify(exportTaskDispatcher).dispatch(101L);
         verify(exportTaskDispatcher).dispatch(102L);
         verify(businessMetrics).setExportBacklog(0L);
    }

    @Test
    @DisplayName("任务被拒绝（返回 false）不应中断后续任务")
    void dispatchPendingTasks_rejectionDoesNotStopOtherTasks(){
        when(exportTaskService.recoverStuckTasks(10)).thenReturn(0);
        when(exportTaskService.findDispatchablePendingIds(15,20))
                .thenReturn(List.of(201L,202L));

        when(exportTaskDispatcher.dispatch(201L)).thenReturn(false);
        when(exportTaskDispatcher.dispatch(202L)).thenReturn(true);
        when(exportTaskService.countPending()).thenReturn(2L);

        job.dispatchPendingTasks();
        verify(exportTaskDispatcher).dispatch(201L);
        verify(exportTaskDispatcher).dispatch(202L);
        verify(exportTaskDispatcher, times(2)).dispatch(anyLong());

        verify(businessMetrics).setExportBacklog(2L);


    }
    @Test
    @DisplayName("单条任务派发抛异常不应中断后续任务")
    void dispatchPendingTasks_exceptionDoesNotStopOtherTasks(){
        when(exportTaskService.recoverStuckTasks(10)).thenReturn(0);
        when(exportTaskService.findDispatchablePendingIds(15,20)).thenReturn(List.of(301L,302L));

        when(exportTaskDispatcher.dispatch(301L))
                .thenThrow(new RuntimeException("线程池已满"));
        when(exportTaskDispatcher.dispatch(302L)).thenReturn(true);
        when(exportTaskService.countPending()).thenReturn(1L);

        // when：不应向外抛出异常
        assertDoesNotThrow(() -> job.dispatchPendingTasks());

        // then：异常被 catch 隔离，第二个任务仍被调用
        verify(exportTaskDispatcher).dispatch(301L);
        verify(exportTaskDispatcher).dispatch(302L);
        verify(exportTaskDispatcher, times(2)).dispatch(anyLong());
        // 异常隔离后流程继续，积压指标照常更新
        verify(businessMetrics).setExportBacklog(1L);

    }
    @Test
    @DisplayName("没有待派发任务时方法正常结束，仍更新一次积压指标")
    void dispatchPendingTasks_emptyList(){
        when(exportTaskService.recoverStuckTasks(10)).thenReturn(0);
        when(exportTaskService.findDispatchablePendingIds(15,20)).thenReturn(List.of());
        when(exportTaskService.countPending()).thenReturn(0L);

        assertDoesNotThrow(() -> job.dispatchPendingTasks());

        // 空列表时不应有任何派发调用
        verify(exportTaskDispatcher, never()).dispatch(anyLong());
        // 无论是否有任务，积压指标都应更新且仅更新一次
        verify(exportTaskService).countPending();
        verify(businessMetrics, times(1)).setExportBacklog(0L);
    }
}
