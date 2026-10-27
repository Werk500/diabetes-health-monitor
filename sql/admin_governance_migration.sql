-- ============================================================
-- 管理端治理能力升级
-- 适用数据库：diabetes_monitor
-- 执行方式：mysql -u root -p < sql/admin_governance_migration.sql
-- 注意：本脚本面向已有数据库，应只执行一次。
-- ============================================================

USE diabetes_monitor;

-- 用户逻辑删除。
-- 现有健康数据均通过 ON DELETE CASCADE 关联 sys_user，继续物理删除会导致
-- 患者健康记录被一并删除，因此管理端删除操作必须改为逻辑删除。
ALTER TABLE sys_user
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0
        COMMENT '逻辑删除 0-未删除 1-已删除' AFTER status,
    ADD KEY idx_user_deleted_status (deleted, status),
    ADD KEY idx_user_create_time (create_time);

-- 管理员操作审计。
-- 不建立 operator_id 外键：即使用户被逻辑删除，历史审计记录仍必须保留。
CREATE TABLE admin_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '审计记录ID',
    operator_id INT NOT NULL COMMENT '操作管理员ID',
    operator_name VARCHAR(50) COMMENT '操作管理员用户名快照',
    action_code VARCHAR(50) NOT NULL COMMENT '动作编码，例如 USER_DISABLE',
    target_type VARCHAR(30) NOT NULL COMMENT '目标类型，例如 USER',
    target_id VARCHAR(64) COMMENT '目标ID',
    request_method VARCHAR(10) COMMENT 'HTTP方法',
    request_path VARCHAR(255) COMMENT '请求路径',
    request_ip VARCHAR(64) COMMENT '客户端IP',
    success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功 0-失败 1-成功',
    detail VARCHAR(1000) COMMENT '操作说明或失败原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_admin_audit_operator_time (operator_id, create_time),
    KEY idx_admin_audit_target (target_type, target_id),
    KEY idx_admin_audit_action_time (action_code, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作审计日志';
