package com.diabetes.monitor.job;

import com.diabetes.monitor.dto.ExportSubmitRequest;
import com.diabetes.monitor.entity.ExportTask;
import com.diabetes.monitor.export.ExcelExportWriter;
import com.diabetes.monitor.mapper.ExportTaskMapper;
import com.diabetes.monitor.service.ExportTaskService;
import com.diabetes.monitor.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class ExportExecutor {

    private static final String TYPE_PDF = "PDF";

    @Resource
    private ExportTaskService exportTaskService;
    @Resource
    private ExportTaskMapper exportTaskMapper;
    /** 与 ExportTaskServiceImpl 序列化用的是同一个 Jackson，保证格式一致 */
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private ExcelExportWriter excelExportWriter;
    /** PDF 报告复用现有的 ReportService（它已经返回 byte[]） */
    @Resource
    private ReportService reportService;

    /** 导出文件落盘目录，application.yml 里的 app.export.dir */
    @Value("${app.export.dir:./exports}")
    private String exportDir;

    @Async("exportTaskExecutor")   // 线程池 Bean 名（类名 ExportExecutor 的 Bean 名是 exportExecutor，避免撞名）
    public void run(Long taskId){
        // ① 领取：抢不到说明已被别的线程/实例处理，直接放弃
        if(!exportTaskService.claim(taskId)){
            log.info("导出任务 {} 已被领取，跳过", taskId);
            return;
        }

        long start = System.currentTimeMillis();
        Path file = null;
        try {
            ExportTask task = exportTaskMapper.selectById(taskId);
            if (task == null) {
                log.warn("导出任务 {} 不存在", taskId);
                exportTaskService.markFailed(taskId, "任务不存在");
                return;
            }
            // ② 解析 task.getParams()（type + 日期区间）
            ExportSubmitRequest req = task.getParams() == null
                    ? new ExportSubmitRequest()
                    : objectMapper.readValue(task.getParams(), ExportSubmitRequest.class);

            boolean isPdf = TYPE_PDF.equalsIgnoreCase(req.getType());

            // ③ 准备目录和文件名：{taskId}_{时间戳}.xlsx / .pdf
            Path dir = Paths.get(exportDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            file = dir.resolve(taskId + "_" + System.currentTimeMillis() + (isPdf ? ".pdf" : ".xlsx"));

            // ④ 生成文件：同一个 Writer / 同一个 byte[]，只是输出目标换成文件流
            //    try-with-resources 结束时自动 close，数据才会真正落盘
            try (OutputStream out = new FileOutputStream(file.toFile())) {
                if (isPdf) {
                    // 把用户选的统计区间传进去；为 null 时服务端默认近 7 天
                    byte[] pdf = reportService.generateReport(
                            task.getUserId(), req.getStartDate(), req.getEndDate()).getBody();
                    if (pdf == null || pdf.length == 0) {
                        throw new IllegalStateException("PDF 生成结果为空");
                    }
                    out.write(pdf);
                } else {
                    excelExportWriter.write(task.getUserId(), req.getStartDate(), req.getEndDate(), out);
                }
            }

            // ⑤ 文件必须真的写成功了才标成功（size 在流关闭之后取）
            long size = Files.size(file);
            if (size <= 0) {
                throw new IllegalStateException("导出文件为空: " + file);
            }
            exportTaskService.markSuccess(taskId, file.toString(), size);

            log.info("导出任务 {} 完成，类型={}，耗时 {}ms，大小 {} bytes",
                    taskId, isPdf ? "PDF" : "EXCEL", System.currentTimeMillis() - start, size);
        } catch (Exception e) {
            // ⑤ 必须自己 catch：@Async 方法抛出的异常不会回到调用方，
            //    不处理的话任务会永远停在 RUNNING
            log.error("导出任务 {} 失败", taskId, e);
            exportTaskService.markFailed(taskId, e.getMessage());
            deleteQuietly(file);      // 清掉半成品，别在 exports 目录里留垃圾
        }

    }

    /** 失败时删除半成品文件 */
    private void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("删除半成品文件失败: {}", file, e);
        }
    }

}
