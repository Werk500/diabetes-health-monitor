package com.diabetes.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_chat_history")
public class AiChatHistory {
    @TableId(type = IdType.AUTO)
    private Integer id;           // 主键自增
    private Integer userId;       // 用户ID（外键 sys_user.id）
    private String role;          // 'user' 或 'assistant'
    private String content;       // 消息内容
    private String sessionId;     // 会话ID（可选，同一轮对话生成一个UUID）
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;  // 创建时间
}
