package com.plato.consumer.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plato.consumer.model.SessionMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 会话列表 Redis 服务
 * 
 * 职责：维护用户的会话列表（写扩散）
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionListRedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 会话列表缓存 TTL：7天
     */
    private static final long SESSION_LIST_TTL_DAYS = 7;

    /**
     * 更新用户的会话列表（新消息到达时调用）
     * 
     * @param userId        用户ID
     * @param sessionId     会话ID
     * @param lastSeqId     最新消息的 seq_id
     * @param preview       消息预览文本
     * @param timestamp     消息时间戳（毫秒）
     */
    public void updateSessionList(Long userId, Long sessionId, Long lastSeqId, 
                                    String preview, Long timestamp) {
        try {
            // 1. 更新会话列表 ZSET（按时间排序）
            String sessionsKey = RedisKeyConstants.userSessionsKey(userId);
            redisTemplate.opsForZSet().add(sessionsKey, sessionId.toString(), timestamp.doubleValue());
            redisTemplate.expire(sessionsKey, SESSION_LIST_TTL_DAYS, TimeUnit.DAYS);

            // 2. 更新会话元数据 Hash
            String metaKey = RedisKeyConstants.sessionMetaKey(userId);
            SessionMetadata metadata = getSessionMetadata(userId, sessionId);

            if (metadata == null) {
                // 首次创建元数据
                metadata = SessionMetadata.builder()
                        .unreadCount(1)
                        .lastSeqId(lastSeqId)
                        .preview(preview)
                        .timestamp(timestamp)
                        .isPinned(false)
                        .build();
            } else {
                // 更新元数据
                metadata.setUnreadCount(metadata.getUnreadCount() + 1);
                metadata.setLastSeqId(lastSeqId);
                metadata.setPreview(preview);
                metadata.setTimestamp(timestamp);
            }

            String metadataJson = objectMapper.writeValueAsString(metadata);
            redisTemplate.opsForHash().put(metaKey, sessionId.toString(), metadataJson);
            redisTemplate.expire(metaKey, SESSION_LIST_TTL_DAYS, TimeUnit.DAYS);

            // 3. 增加未读计数
            incrementUnreadCount(userId, sessionId);

            log.debug("Updated session list for user {}: sessionId={}, seqId={}", 
                      userId, sessionId, lastSeqId);

        } catch (Exception e) {
            log.error("Failed to update session list: userId={}, sessionId={}", 
                      userId, sessionId, e);
        }
    }

    /**
     * 批量更新会话列表（用于群聊消息）
     * 
     * @param userIds       用户ID列表
     * @param sessionId     会话ID
     * @param lastSeqId     最新消息的 seq_id
     * @param preview       消息预览文本
     * @param timestamp     消息时间戳（毫秒）
     */
    public void batchUpdateSessionList(Iterable<Long> userIds, Long sessionId, 
                                        Long lastSeqId, String preview, Long timestamp) {
        for (Long userId : userIds) {
            updateSessionList(userId, sessionId, lastSeqId, preview, timestamp);
        }
    }

    /**
     * 获取会话元数据
     * 
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return 会话元数据
     */
    public SessionMetadata getSessionMetadata(Long userId, Long sessionId) {
        try {
            String metaKey = RedisKeyConstants.sessionMetaKey(userId);
            Object metadataObj = redisTemplate.opsForHash().get(metaKey, sessionId.toString());

            if (metadataObj == null) {
                return null;
            }

            return objectMapper.readValue(metadataObj.toString(), SessionMetadata.class);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse session metadata: userId={}, sessionId={}", 
                      userId, sessionId, e);
            return null;
        }
    }

    /**
     * 增加未读计数
     * 
     * @param userId    用户ID
     * @param sessionId 会话ID
     */
    private void incrementUnreadCount(Long userId, Long sessionId) {
        try {
            String unreadKey = RedisKeyConstants.unreadCountKey(userId);
            redisTemplate.opsForHash().increment(unreadKey, sessionId.toString(), 1);
            redisTemplate.expire(unreadKey, SESSION_LIST_TTL_DAYS, TimeUnit.DAYS);

        } catch (Exception e) {
            log.error("Failed to increment unread count: userId={}, sessionId={}", 
                      userId, sessionId, e);
        }
    }

    /**
     * 清空未读计数（用户读取消息后调用）
     * 
     * @param userId    用户ID
     * @param sessionId 会话ID
     */
    public void clearUnreadCount(Long userId, Long sessionId) {
        try {
            String unreadKey = RedisKeyConstants.unreadCountKey(userId);
            redisTemplate.opsForHash().put(unreadKey, sessionId.toString(), "0");

            log.debug("Cleared unread count: userId={}, sessionId={}", userId, sessionId);

        } catch (Exception e) {
            log.error("Failed to clear unread count: userId={}, sessionId={}", 
                      userId, sessionId, e);
        }
    }
}

