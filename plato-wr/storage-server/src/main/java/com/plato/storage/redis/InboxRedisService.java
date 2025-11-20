package com.plato.storage.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 收件箱Redis服务
 * 
 * 职责：管理用户的会话列表和未读消息
 * 
 * @author hc
 * @since 2025/11/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboxRedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 会话元数据DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionMetaDto {
        private Long sessionId;
        private Integer unreadCount;
        private String lastMsgContentPreview;
        private Long lastMsgTimeMs;
        private Long lastMsgSenderId;
        private Boolean isPinned;
        private Boolean isMuted;
    }

    /**
     * 添加消息到用户收件箱
     * 
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param seqId     序列ID
     * @param msgId     消息ID
     */
    public void addToInbox(Long userId, Long sessionId, Long seqId, Long msgId) {
        String inboxKey = RedisKeyConstants.inboxKey(userId);
        // 使用seqId作为score，msgId作为value
        redisTemplate.opsForZSet().add(inboxKey, msgId.toString(), seqId.doubleValue());
    }

    /**
     * 批量添加消息到多个用户收件箱（写扩散）
     * 
     * @param userIds   用户ID列表
     * @param sessionId 会话ID
     * @param seqId     序列ID
     * @param msgId     消息ID
     */
    public void fanoutToInboxes(List<Long> userIds, Long sessionId, Long seqId, Long msgId) {
        for (Long userId : userIds) {
            addToInbox(userId, sessionId, seqId, msgId);
        }
    }

    /**
     * 更新用户会话列表
     * 
     * @param userId        用户ID
     * @param sessionId     会话ID
     * @param lastMsgTimeMs 最后消息时间
     */
    public void updateSessionList(Long userId, Long sessionId, Long lastMsgTimeMs) {
        String key = RedisKeyConstants.userSessionsKey(userId);
        redisTemplate.opsForZSet().add(key, sessionId.toString(), lastMsgTimeMs.doubleValue());
    }

    /**
     * 更新会话元数据
     * 
     * @param userId 用户ID
     * @param meta   会话元数据
     */
    public void updateSessionMeta(Long userId, SessionMetaDto meta) {
        try {
            String key = RedisKeyConstants.sessionMetaKey(userId);
            String field = meta.getSessionId().toString();
            String value = objectMapper.writeValueAsString(meta);
            redisTemplate.opsForHash().put(key, field, value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize session meta", e);
        }
    }

    /**
     * 获取用户的会话列表（分页）
     * 
     * @param userId       用户ID
     * @param anchorTimeMs 锚点时间戳
     * @param limit        限制条数
     * @return 会话ID列表
     */
    public List<Long> getSessionList(Long userId, Long anchorTimeMs, int limit) {
        String key = RedisKeyConstants.userSessionsKey(userId);

        // 按时间倒序获取
        Set<String> sessionIds;
        if (anchorTimeMs == 0) {
            // 从最新开始
            sessionIds = redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);
        } else {
            // 从锚点开始
            sessionIds = redisTemplate.opsForZSet().reverseRangeByScore(
                    key, 0, anchorTimeMs, 0, limit);
        }

        List<Long> result = new ArrayList<>();
        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                result.add(Long.parseLong(sessionId));
            }
        }
        return result;
    }

    /**
     * 获取会话元数据
     * 
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return 会话元数据
     */
    public SessionMetaDto getSessionMeta(Long userId, Long sessionId) {
        try {
            String key = RedisKeyConstants.sessionMetaKey(userId);
            String field = sessionId.toString();
            String value = (String) redisTemplate.opsForHash().get(key, field);

            if (value != null) {
                return objectMapper.readValue(value, SessionMetaDto.class);
            }
        } catch (Exception e) {
            log.error("Failed to get session meta", e);
        }
        return null;
    }
}
