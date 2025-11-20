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
