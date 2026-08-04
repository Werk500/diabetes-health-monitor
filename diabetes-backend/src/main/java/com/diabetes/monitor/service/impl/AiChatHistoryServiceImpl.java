package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.monitor.entity.AiChatHistory;
import com.diabetes.monitor.mapper.AiChatHistoryMapper;
import com.diabetes.monitor.service.AiChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;


@Slf4j
@Service
public class AiChatHistoryServiceImpl implements AiChatHistoryService {

    @Resource
    private AiChatHistoryMapper aiChatHistoryMapper;

    public AiChatHistoryServiceImpl(AiChatHistoryMapper aiChatHistoryMapper) {
        this.aiChatHistoryMapper = aiChatHistoryMapper;
    }

    /**
     * 保存一条消息
     * @param userId 用户ID
     * @param role 角色（user/assistant/system）
     * @param content 消息内容
     * @param sessionId 会话ID
     */
    @Override
    @Transactional
    public void saveMessage(Integer userId, String role, String content, String sessionId) {
        //构建实体
        AiChatHistory history = new AiChatHistory();
        history.setUserId(userId);
        history.setRole(role);
        history.setContent(content);
        history.setSessionId(sessionId);

        int insert = aiChatHistoryMapper.insert(history);
        if (insert > 0) {
            log.info("消息保存成功：userId={}, role={}, sessionId={}", userId, role, sessionId);
        }else {
            log.warn("消息保存失败：userId={}, role={}", userId, role);
        }
    }

    /**
     * 获取用户最近 N 条历史（按时间升序），用于前端展示
     * @param userId 用户ID
     * @param limit 限制条数
     * @return 聊天记录列表（时间升序）
     */
    @Override
    public List<AiChatHistory> getRecentMessages(Integer userId, int limit) {

        QueryWrapper<AiChatHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .orderByDesc("create_time")
                .last("limit " + limit); //限制N条
        List<AiChatHistory> list = aiChatHistoryMapper.selectList(wrapper);

        Collections.reverse(list);

        return list;
    }

    @Override
    public Page<AiChatHistory> pageByUserId(Integer userId, int pageNum, int size) {
        Page<AiChatHistory> page = new Page<>(pageNum, size);
        QueryWrapper<AiChatHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .orderByDesc("create_time");

        return aiChatHistoryMapper.selectPage(page,wrapper);
    }
}
