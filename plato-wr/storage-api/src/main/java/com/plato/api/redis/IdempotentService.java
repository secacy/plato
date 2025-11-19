package com.plato.api.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 幂等服务
 * 
 * 职责：基于Redis实现消息去重
 * 
 * @author hc
 * @since 2025/11/19
 */
@Service
@RequiredArgsConstructor
public class IdempotentService {

    private final StringRedisTemplate redisTemplate;

    // 幂等窗口：5分钟
    private static final long IDEMPOTENT_WINDOW = 300;

    /**
     * 检查消息是否已存在（幂等检查）
     * 
     * @param clientMsgId 客户端消息ID
     * @return 如果已存在返回msgId，否则返回null
     */
    public Long checkIdempotent(String clientMsgId) {
        String key = RedisKeyConstants.idempotentKey(clientMsgId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : null;
    }

    /**
     * 记录消息ID（用于幂等）
     * 
     * @param clientMsgId 客户端消息ID
     * @param msgId       服务器消息ID
     */
    public void recordIdempotent(String clientMsgId, Long msgId) {
        String key = RedisKeyConstants.idempotentKey(clientMsgId);
        redisTemplate.opsForValue().set(
                key,
                msgId.toString(),
                IDEMPOTENT_WINDOW,
                TimeUnit.SECONDS);
    }
}
