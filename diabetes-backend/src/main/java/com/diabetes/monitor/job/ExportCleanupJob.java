package com.diabetes.monitor.job;

import com.diabetes.monitor.service.ExportTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.stream.Stream;

@Slf4j
@Component
public class ExportCleanupJob {

    @Resource
    private ExportTaskService exportTaskService;

    @Value("${app.export.dir:./exports}")
    private String exportDir;

    /** 超过这么多分钟还在 RUNNING 的任务，视为卡死 */
    @Value("${app.export.stuck-minutes:10}")
    private int stuckMinutes;

    /** 任务记录和文件保留天数 */
    @Value("${app.export.keep-days:7}")
    private int keepDays;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanup() {
        int deleted = exportTaskService.deleteExpired(keepDays);            // 删记录
        int files = deleteExpiredFiles();                                    // 删文件
        log.info("导出清理完成：删除过期记录 {} 条，删除过期文件 {} 个",
                 deleted, files);
    }

    /** 删除目录里超过 keepDays 的文件，避免 exports 无限增长 */
    private int deleteExpiredFiles() {
        Path dir = Paths.get(exportDir).toAbsolutePath().normalize();
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        long cutoff = System.currentTimeMillis() - Duration.ofDays(keepDays).toMillis();
        int count = 0;
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path p : stream.toList()) {
                try {
                    if (Files.isRegularFile(p) && Files.getLastModifiedTime(p).toMillis() < cutoff) {
                        Files.deleteIfExists(p);
                        count++;
                    }
                } catch (IOException e) {
                    log.warn("删除过期导出文件失败: {}", p, e);
                }
            }
        } catch (IOException e) {
            log.warn("扫描导出目录失败: {}", dir, e);
        }
        return count;
    }
}
