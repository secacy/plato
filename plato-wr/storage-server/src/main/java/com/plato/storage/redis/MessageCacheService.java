package com.plato.storage.redis;

import com.google.protobuf.InvalidProtocolBufferException;
import com.plato.gateway.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 消息缓存服务
 * 
 * 职责：实现 Read-Aside Cache + Gap Detection 策略
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageCacheService {

    private final RedisTemplate<String, byte[]> redisTemplate;

    /**
     * 消息索引缓存 TTL：3天
     */
    private static final long MESSAGE_CACHE_TTL_DAYS = 3;

    /**
     * 消息元数据缓存 TTL：1天
     */
    private static final long MESSAGE_META_TTL_DAYS = 1;

    /**
     * 缓存消息列表的最大条数（只保留最近 1000 条）
     */
    private static final int MAX_CACHED_MESSAGES = 1000;

    /**
     * 缓存消息到 Redis（写入索引 + 元数据）
     * 
     * @param message 消息（Protobuf 格式）
     */
    public void cacheMessage(Message message) {
        try {
            Long sessionId = message.getSessionId();
            Long seqId = message.getSeqId();
            Long msgId = message.getMsgId();

            // 1. 写入消息索引到 ZSET (cache:msgs:{session_id})
            String cacheKey = RedisKeyConstants.messageCacheKey(sessionId);
            redisTemplate.opsForZSet().add(cacheKey, String.valueOf(msgId).getBytes(), seqId.doubleValue());

            // 设置过期时间
            redisTemplate.expire(cacheKey, MESSAGE_CACHE_TTL_DAYS, TimeUnit.DAYS);

            // 限制缓存大小（只保留最近的 1000 条）
            Long size = redisTemplate.opsForZSet().zCard(cacheKey);
            if (size != null && size > MAX_CACHED_MESSAGES) {
                long removeCount = size - MAX_CACHED_MESSAGES;
                redisTemplate.opsForZSet().removeRange(cacheKey, 0, removeCount - 1);
            }

            // 2. 写入消息元数据 (msg_meta:{msg_id})
            String metaKey = RedisKeyConstants.messageMetaKey(msgId);
            byte[] messageBytes = message.toByteArray();
            redisTemplate.opsForValue().set(metaKey, messageBytes, MESSAGE_META_TTL_DAYS, TimeUnit.DAYS);

            log.debug("Cached message: sessionId={}, seqId={}, msgId={}", sessionId, seqId, msgId);

        } catch (Exception e) {
            log.error("Failed to cache message: {}", message.getMsgId(), e);
        }
    }

    /**
     * 批量缓存消息
     * 
     * @param messages 消息列表
     */
    public void cacheMessages(List<Message> messages) {
        for (Message message : messages) {
            cacheMessage(message);
        }
    }

    /**
     * 从缓存中查询消息（带 Gap Detection）
     * 
     * @param sessionId  会话ID
     * @param startSeqId 起始序列ID（不包含）
     * @param limit      限制条数
     * @return 消息列表（如果检测到 Gap 则返回 null，表示需要降级查 DB）
     */
    public List<Message> getMessagesFromCache(Long sessionId, Long startSeqId, int limit) {
        try {
            String cacheKey = RedisKeyConstants.messageCacheKey(sessionId);

            // 1. 从 ZSET 中查询消息 ID 列表
            Set<ZSetOperations.TypedTuple<byte[]>> tuples = redisTemplate.opsForZSet()
                    .rangeByScoreWithScores(cacheKey, startSeqId + 1, Double.POSITIVE_INFINITY, 0, limit);

            if (tuples == null || tuples.isEmpty()) {
                log.debug("Cache miss: sessionId={}, startSeqId={}", sessionId, startSeqId);
                return null; // Cache Miss
            }

            // 2. Gap Detection（空洞检测）
            ZSetOperations.TypedTuple<byte[]> firstTuple = tuples.iterator().next();
            long expectedSeqId = startSeqId + 1;
            long actualSeqId = firstTuple.getScore().longValue();

            if (actualSeqId > expectedSeqId) {
                log.warn("Gap detected: sessionId={}, expected={}, actual={}",
                        sessionId, expectedSeqId, actualSeqId);
                return null; // Gap Detected，降级查 DB
            }

            // 3. 查询消息元数据
            List<Message> messages = new ArrayList<>(tuples.size());
            for (ZSetOperations.TypedTuple<byte[]> tuple : tuples) {
                byte[] msgIdBytes = tuple.getValue();
                if (msgIdBytes == null) {
                    continue;
                }

                Long msgId = Long.parseLong(new String(msgIdBytes));
                Message message = getMessageMeta(msgId);

                if (message != null) {
                    messages.add(message);
                } else {
                    // 元数据缺失，降级查 DB
                    log.warn("Message meta missing: msgId={}", msgId);
                    return null;
                }
            }

            log.debug("Cache hit: sessionId={}, found {} messages", sessionId, messages.size());
            return messages;

        } catch (Exception e) {
            log.error("Failed to get messages from cache: sessionId={}", sessionId, e);
            return null; // 出错时降级查 DB
        }
    }

    /**
     * 获取消息元数据
     * 
     * @param msgId 消息ID
     * @return 消息（Protobuf 格式）
     */
    private Message getMessageMeta(Long msgId) {
        try {
            String metaKey = RedisKeyConstants.messageMetaKey(msgId);
            byte[] messageBytes = redisTemplate.opsForValue().get(metaKey);

            if (messageBytes == null) {
                return null;
            }

            return Message.parseFrom(messageBytes);

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse message meta: msgId={}", msgId, e);
            return null;
        }
    }

    /**
     * 删除消息缓存（用于消息撤回/删除）
     * 
     * @param sessionId 会话ID
     * @param msgId     消息ID
     * @param seqId     序列ID
     */
    public void removeMessage(Long sessionId, Long msgId, Long seqId) {
        try {
            // 1. 从 ZSET 中删除索引
            String cacheKey = RedisKeyConstants.messageCacheKey(sessionId);
            redisTemplate.opsForZSet().remove(cacheKey, String.valueOf(msgId).getBytes());

            // 2. 删除元数据
            String metaKey = RedisKeyConstants.messageMetaKey(msgId);
            redisTemplate.delete(metaKey);

            log.debug("Removed message from cache: sessionId={}, seqId={}, msgId={}",
                    sessionId, seqId, msgId);

        } catch (Exception e) {
            log.error("Failed to remove message from cache: msgId={}", msgId, e);
        }
    }

    /**
     * 清空会话的消息缓存
     * 
     * @param sessionId 会话ID
     */
    public void clearSessionCache(Long sessionId) {
        try {
            String cacheKey = RedisKeyConstants.messageCacheKey(sessionId);
            redisTemplate.delete(cacheKey);
            log.info("Cleared session cache: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("Failed to clear session cache: sessionId={}", sessionId, e);
        }
    }
}
