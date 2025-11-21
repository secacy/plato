package com.plato.search.redis;

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
 * 职责：获取用户会话列表（用于全局搜索）
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboxRedisService {

    private final StringRedisTemplate redisTemplate;

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
}

