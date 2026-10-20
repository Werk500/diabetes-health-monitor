package com.diabetes.monitor.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.ExportStatus;
import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.common.ResultCode;
import com.diabetes.monitor.dto.ExportSubmitRequest;
import com.diabetes.monitor.entity.ExportTask;
import com.diabetes.monitor.job.ExportExecutor;
import com.diabetes.monitor.service.ExportTaskDispatcher;
import com.diabetes.monitor.service.ExportTaskService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Resource
    private ExportTaskService exportTaskService;
    @Resource
    private ExportExecutor exportExecutor;
    @Resource
    private ExportTaskDispatcher exportTaskDispatcher;

    @PostMapping("/tasks")
    public Result<Long> submit(@RequestBody @Valid ExportSubmitRequest req) {
        Long id = exportTaskService.submit(currentUserId(), req);
        boolean dispatch = exportTaskDispatcher.dispatch(id);
        return Result.ok(id);
    }

    @GetMapping("/tasks/{id}")
    public Result<ExportTask> getTask(@PathVariable Long id) {
        ExportTask task = exportTaskService.getTask(id, currentUserId());
        return Result.ok(task);
    }

    /** 我的导出任务列表（分页） */
    @GetMapping("/tasks")
    public Result<Page<ExportTask>> list(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return Result.ok(exportTaskService.listMyTasks(currentUserId(), page, size));
    }

    /** 下载：只有 SUCCESS 且文件存在时才给 */
    @GetMapping("/tasks/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) throws IOException {
        // getTask 内部已做归属校验，A 用户拿不到 B 用户的文件
        ExportTask task = exportTaskService.getTask(id, currentUserId());

        if (!ExportStatus.SUCCESS.equals(task.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "任务尚未完成，无法下载");
        }

        Path path = Paths.get(task.getFilePath()).toAbsolutePath().normalize();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new BizException(ResultCode.NOT_FOUND, "文件已过期或不存在");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + path.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(path))
                .body(new FileSystemResource(path));
    }

    /** 重试：只有 FAILED 且未超过次数才能重跑 */
    @PostMapping("/tasks/{id}/retry")
    public Result<Long> retry(@PathVariable Long id) {
        boolean ok = exportTaskService.retry(id, currentUserId());
        if (!ok) {
            throw new BizException(ResultCode.BAD_REQUEST, "重试次数已用完或当前状态不允许重试");
        }
        exportTaskDispatcher.dispatch(id);          // 重新丢给 export-exec 线程池
        return Result.ok(id);
    }

    private Integer currentUserId() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
