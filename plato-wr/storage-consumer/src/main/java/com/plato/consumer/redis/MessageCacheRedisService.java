package com.plato.consumer.redis;

import com.google.protobuf.ByteString;
import com.plato.gateway.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 消息缓存 Redis 服务
 * 
 * 职责：维护消息公共缓存（读扩散）
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageCacheRedisService {

    private final RedisTemplate<String, byte[]> redisTemplate;

    /**
     * 消息索引缓存 TTL：3天
     */
    @Value("${consumer.redis.message-cache-ttl-days:3}")
    private long messageCacheTtlDays;

    /**
     * 消息元数据缓存 TTL：1天
     */
    private static final long MESSAGE_META_TTL_DAYS = 1;

    /**
     * 缓存消息列表的最大条数
     */
    @Value("${consumer.redis.max-message-cache-size:1000}")
    private int maxCachedMessages;

    /**
     * 缓存消息到 Redis（写入索引 + 元数据）
     * 
     * @param sessionId 会话ID
     * @param seqId     序列ID
     * @param msgId     消息ID
     * @param content   消息内容
     * @param senderId  发送者ID
     * @param msgType   消息类型
     * @param status    消息状态
     * @param createTimeMs 创建时间（毫秒）
     */
    public void cacheMessage(Long sessionId, Long seqId, Long msgId, 
                             String content, Long senderId, Integer msgType, 
                             Integer status, Long createTimeMs) {
        try {
            // 1. 写入消息索引到 ZSET (cache:msgs:{session_id})
            String cacheKey = RedisKeyConstants.messageCacheKey(sessionId);
            redisTemplate.opsForZSet().add(cacheKey, 
                String.valueOf(msgId).getBytes(), seqId.doubleValue());

            // 设置过期时间
            redisTemplate.expire(cacheKey, messageCacheTtlDays, TimeUnit.DAYS);

            // 限制缓存大小（只保留最近的 N 条）
            Long size = redisTemplate.opsForZSet().zCard(cacheKey);
            if (size != null && size > maxCachedMessages) {
                long removeCount = size - maxCachedMessages;
                redisTemplate.opsForZSet().removeRange(cacheKey, 0, removeCount - 1);
            }

            // 2. 写入消息元数据 (msg_meta:{msg_id}) - 使用 Protobuf 序列化
            String metaKey = RedisKeyConstants.messageMetaKey(msgId);
            
            // 构建 Protobuf Message
            Message message = Message.newBuilder()
                    .setMsgId(msgId)
                    .setSessionId(sessionId)
                    .setSeqId(seqId)
                    .setSenderId(senderId)
                    .setMsgType(msgType)
                    .setContent(ByteString.copyFromUtf8(content))
                    .setStatus(status)
                    .setCreateTimeMs(createTimeMs)
                    .build();

            byte[] messageBytes = message.toByteArray();
            redisTemplate.opsForValue().set(metaKey, messageBytes, 
                MESSAGE_META_TTL_DAYS, TimeUnit.DAYS);

            log.debug("Cached message: sessionId={}, seqId={}, msgId={}", 
                      sessionId, seqId, msgId);

        } catch (Exception e) {
            log.error("Failed to cache message: msgId={}", msgId, e);
        }
    }

    /**
     * 删除消息缓存（用于消息撤回/删除）
     * 
     * @param sessionId 会话ID
     * @param msgId     消息ID
     */
    public void removeMessage(Long sessionId, Long msgId) {
        try {
            // 1. 从 ZSET 中删除索引
            String cacheKey = RedisKeyConstants.messageCacheKey(sessionId);
            redisTemplate.opsForZSet().remove(cacheKey, String.valueOf(msgId).getBytes());

            // 2. 删除元数据
            String metaKey = RedisKeyConstants.messageMetaKey(msgId);
            redisTemplate.delete(metaKey);

            log.debug("Removed message from cache: sessionId={}, msgId={}", 
                      sessionId, msgId);

        } catch (Exception e) {
            log.error("Failed to remove message from cache: msgId={}", msgId, e);
        }
    }

    /**
     * 更新消息状态（用于消息撤回/删除）
     * 
     * @param msgId     消息ID
     * @param newStatus 新状态
     */
    public void updateMessageStatus(Long msgId, Integer newStatus) {
        try {
            String metaKey = RedisKeyConstants.messageMetaKey(msgId);
            byte[] messageBytes = redisTemplate.opsForValue().get(metaKey);

            if (messageBytes == null) {
                log.warn("Message not found in cache: msgId={}", msgId);
                return;
            }

            // 解析 Protobuf 消息
            Message message = Message.parseFrom(messageBytes);

            // 更新状态
            Message updatedMessage = message.toBuilder()
                    .setStatus(newStatus)
                    .build();

            // 写回 Redis
            redisTemplate.opsForValue().set(metaKey, updatedMessage.toByteArray(), 
                MESSAGE_META_TTL_DAYS, TimeUnit.DAYS);

            log.debug("Updated message status in cache: msgId={}, newStatus={}", 
                      msgId, newStatus);

        } catch (Exception e) {
            log.error("Failed to update message status in cache: msgId={}", msgId, e);
        }
    }
}

