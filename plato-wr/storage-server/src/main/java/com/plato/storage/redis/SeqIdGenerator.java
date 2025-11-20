package com.plato.storage.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * SeqID生成器
 * 
 * 职责：为每个会话生成严格递增的序列号
 * 实现：基于Redis INCR命令
 * 
 * @author hc
 * @since 2025/11/19
 */
@Component
@RequiredArgsConstructor
public class SeqIdGenerator {

    private final StringRedisTemplate redisTemplate;

    /**
     * 生成会话的下一个SeqID
     * 
     * @param sessionId 会话ID
     * @return 新的SeqID
     */
    public Long generateSeqId(Long sessionId) {
        String key = RedisKeyConstants.seqKey(sessionId);
        Long seqId = redisTemplate.opsForValue().increment(key);

        if (seqId == null) {
            throw new RuntimeException("Failed to generate seq_id for session: " + sessionId);
        }

        return seqId;
    }

    /**
     * 获取会话当前最大SeqID（不递增）
     * 
     * @param sessionId 会话ID
     * @return 当前最大SeqID，如果不存在返回0
     */
    public Long getCurrentSeqId(Long sessionId) {
        String key = RedisKeyConstants.seqKey(sessionId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }
}
