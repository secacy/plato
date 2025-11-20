package com.plato.storage.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 幂等服务
 * 
 * 职责：基于Redis实现消息去重
 * 使用 SETNX 实现原子防重
 * 
 * @author hc
 * @since 2025/11/19
 */
@Service
@RequiredArgsConstructor
public class IdempotentService {

    private final StringRedisTemplate redisTemplate;

    // 幂等窗口：5分钟（300秒）
    private static final long IDEMPOTENT_WINDOW_SECONDS = 300;

    /**
     * 原子防重检查
     * 使用 SETNX 命令实现原子性
     * 
     * @param sessionId       会话ID
     * @param clientMessageId 客户端消息ID
     * @return true=通过（首次请求），false=重复（已存在）
     */
    public boolean tryAcquire(Long sessionId, String clientMessageId) {
        String key = RedisKeyConstants.dedupKey(sessionId, clientMessageId);
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
                key,
                "processing",
                IDEMPOTENT_WINDOW_SECONDS,
                TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 保存消息的幂等信息（msgId 和 seqId）
     * 在消息成功保存后调用
     * 
     * @param sessionId       会话ID
     * @param clientMessageId 客户端消息ID
     * @param msgId           消息ID
     * @param seqId           序列号
     */
    public void saveMessageInfo(Long sessionId, String clientMessageId, long msgId, long seqId) {
        String key = RedisKeyConstants.dedupKey(sessionId, clientMessageId);
        // 格式：msgId:seqId
        String value = msgId + ":" + seqId;
        redisTemplate.opsForValue().set(key, value, IDEMPOTENT_WINDOW_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 获取已存在的消息信息
     * 用于幂等返回
     * 
     * @param sessionId       会话ID
     * @param clientMessageId 客户端消息ID
     * @return 格式为 "msgId:seqId"，如果不存在返回 null
     */
    public String getMessageInfo(Long sessionId, String clientMessageId) {
        String key = RedisKeyConstants.dedupKey(sessionId, clientMessageId);
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 释放去重锁（用于异常回滚）
     * 
     * @param sessionId       会话ID
     * @param clientMessageId 客户端消息ID
     */
    public void release(Long sessionId, String clientMessageId) {
        String key = RedisKeyConstants.dedupKey(sessionId, clientMessageId);
        redisTemplate.delete(key);
    }
}
