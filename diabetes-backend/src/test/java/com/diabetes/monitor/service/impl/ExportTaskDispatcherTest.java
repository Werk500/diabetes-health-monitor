package com.diabetes.monitor.service.impl;


import com.diabetes.monitor.common.metrics.BusinessMetrics;
import com.diabetes.monitor.job.ExportExecutor;
import com.diabetes.monitor.service.ExportTaskDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ExportTaskDispatcherTest {

    @Mock
    private ExportExecutor exportExecutor;

    @Mock
    private BusinessMetrics businessMetrics;

    @InjectMocks
    private ExportTaskDispatcher dispatcher;

    @Test
    void dispatch_accepted(){

        Long taskId = 1L;

        boolean accepted = dispatcher.dispatch(taskId);
        assertThat(accepted).isTrue();

        verify(exportExecutor).run(taskId);

        verify(businessMetrics).exportDispatch("accepted");
    }

    @Test
    void dispatch_rejected(){
        Long taskId = 1L;

        doThrow(new TaskRejectedException("queue full"))
                .when(exportExecutor)
                .run(taskId);

        boolean rejected = dispatcher.dispatch(taskId);
        assertThat(rejected).isFalse();
        verify(businessMetrics).exportDispatch("rejected");
    }
}
