package com.diabetes.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diabetes.monitor.entity.ExportTask;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ExportTaskMapper extends BaseMapper<ExportTask> {

    /** 领取任务：0 行 = 已被别的线程/实例领走，或状态不是 PENDING */
    @Update("UPDATE export_task SET status = 'RUNNING',start_time = NOW() WHERE id=#{id} AND status = 'PENDING'")
    int claim(@Param("id") Long id);

    /** 完成：只有 RUNNING 才能变 SUCCESS（终态不可逆） */
    @Update("UPDATE export_task SET status = 'SUCCESS',progress=100,file_path=#{filePath},"+
            "file_size=#{fileSize},finish_time=NOW() WHERE id = #{id} AND status = 'RUNNING'")
    int markSuccess(@Param("id") Long id, @Param("filePath") String filePath, @Param("fileSize") Long fileSize);

    /** 失败：只有 RUNNING 才能变 FAILED */
    @Update("UPDATE export_task SET status='FAILED', error_msg=#{errorMsg}, finish_time=NOW() " +
            "WHERE id=#{id} AND status='RUNNING'")
    int markFailed(@Param("id") Long id, @Param("errorMsg") String errorMsg);

    /** 重试：只有 FAILED 且重试次数 < 3 才能回到 PENDING */
    @Update("UPDATE export_task SET status='PENDING', retry_count=retry_count+1, error_msg=NULL " +
            "WHERE id=#{id} AND status='FAILED' AND retry_count < 3")
    int resetForRetry(@Param("id") Long id);

    /** 恢复"卡在 RUNNING"的任务：进程崩溃/重启后靠它兜底 */
    @Update("UPDATE export_task SET status='FAILED',error_msg='执行超时，已自动标记失败',finish_time=NOW() "+
            "WHERE status = 'RUNNING' AND start_time < DATE_SUB(NOW(),INTERVAL #{minutes} MINUTE)")
    int markStuckAsFailed(@Param("minutes") int minutes);

    /** 清理过期的成功/失败任务（文件另行清理） */
    @Delete("DELETE FROM export_task WHERE status IN ('SUCCESS','FAILED') " +
            "AND create_time < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int deleteExpired(@Param("days") int days);
}
