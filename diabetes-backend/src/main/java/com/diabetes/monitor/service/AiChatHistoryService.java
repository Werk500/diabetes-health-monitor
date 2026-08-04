package com.diabetes.monitor.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.monitor.entity.AiChatHistory;

import java.util.List;

public interface AiChatHistoryService {
    /**
     * 保存一条消息
     * @param userId
     * @param role
     * @param content
     * @param sessionId
     */
    void saveMessage(Integer userId, String role, String content, String sessionId);

    /**
     * 获取用户最近 N 条历史（按时间升序），用于前端展示
     * @param userId
     * @param limit
     * @return
     */
    List<AiChatHistory> getRecentMessages(Integer userId, int limit);

    /**
     * 分页查询
     * @param userId
     * @param page
     * @param size
     * @return
     */
    Page<AiChatHistory> pageByUserId(Integer userId, int page, int size);


}
