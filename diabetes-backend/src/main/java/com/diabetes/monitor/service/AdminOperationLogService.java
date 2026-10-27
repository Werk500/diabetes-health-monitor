package com.diabetes.monitor.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.diabetes.monitor.dto.AdminAuditQueryDTO;
import com.diabetes.monitor.entity.AdminOperationLog;

public interface AdminOperationLogService {

    String USER_ENABLE = "USER_ENABLE";
    String USER_DISABLE = "USER_DISABLE";
    String USER_DELETE = "USER_DELETE";

    String TARGET_USER = "USER";

    void recordSuccess(
            Integer operatorId,
            String actionCode,
            String targetType,
            String targetId,
            String detail
    );

    void recordFailure(
            Integer operatorId,
            String actionCode,
            String targetType,
            String targetId,
            String detail
    );

    IPage<AdminOperationLog> pageQuery(AdminAuditQueryDTO query);
}
