package com.plato.storage.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 群成员Redis服务
 * 
 * 职责：缓存群成员列表，加速消息写扩散
 * 
 * @author hc
 * @since 2025/11/19
 */
@Service
@RequiredArgsConstructor
public class GroupMemberRedisService {

    private final StringRedisTemplate redisTemplate;

    // 缓存过期时间：1小时
    private static final long CACHE_TTL = 3600;

    /**
     * 缓存群成员列表
     * 
     * @param sessionId 会话ID
     * @param memberIds 成员ID列表
     */
    public void cacheMembers(Long sessionId, List<Long> memberIds) {
        String key = RedisKeyConstants.groupMembersKey(sessionId);

        // 先删除旧数据
        redisTemplate.delete(key);

        // 批量添加
        if (!memberIds.isEmpty()) {
            String[] members = memberIds.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
            redisTemplate.opsForSet().add(key, members);

            // 设置过期时间
            redisTemplate.expire(key, CACHE_TTL, TimeUnit.SECONDS);
        }
    }

    /**
     * 添加成员
     * 
     * @param sessionId 会话ID
     * @param memberIds 成员ID列表
     */
    public void addMembers(Long sessionId, List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return;
        }

        String key = RedisKeyConstants.groupMembersKey(sessionId);
        String[] members = memberIds.stream()
                .map(String::valueOf)
                .toArray(String[]::new);
        redisTemplate.opsForSet().add(key, members);

        // 刷新过期时间
        redisTemplate.expire(key, CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 移除成员
     * 
     * @param sessionId 会话ID
     * @param memberIds 成员ID列表
     */
    public void removeMembers(Long sessionId, List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return;
        }

        String key = RedisKeyConstants.groupMembersKey(sessionId);
        Object[] members = memberIds.stream()
                .map(String::valueOf)
                .toArray();
        redisTemplate.opsForSet().remove(key, members);
    }

    /**
     * 获取群成员列表
     * 
     * @param sessionId 会话ID
     * @return 成员ID列表，如果缓存不存在返回null
     */
    public List<Long> getMembers(Long sessionId) {
        String key = RedisKeyConstants.groupMembersKey(sessionId);
        Set<String> members = redisTemplate.opsForSet().members(key);

        if (members == null || members.isEmpty()) {
            return null; // 缓存未命中
        }

        List<Long> result = new ArrayList<>();
        for (String member : members) {
            result.add(Long.parseLong(member));
        }
        return result;
    }

    /**
     * 删除群成员缓存
     * 
     * @param sessionId 会话ID
     */
    public void evict(Long sessionId) {
        String key = RedisKeyConstants.groupMembersKey(sessionId);
        redisTemplate.delete(key);
    }
}
