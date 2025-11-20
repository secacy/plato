package com.plato.consumer.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 成员缓存 Redis 服务
 * 
 * 职责：维护会话成员列表缓存
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberCacheRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 成员列表缓存 TTL：1小时
     */
    private static final long MEMBER_CACHE_TTL_HOURS = 1;

    /**
     * 缓存会话成员列表
     * 
     * @param sessionId 会话ID
     * @param memberIds 成员ID列表
     */
    public void cacheMemberList(Long sessionId, List<Long> memberIds) {
        try {
            if (memberIds == null || memberIds.isEmpty()) {
                return;
            }

            String membersKey = RedisKeyConstants.sessionMembersKey(sessionId);
            
            // 转换为 String 数组
            String[] members = memberIds.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);

            // 添加到 Redis Set
            redisTemplate.opsForSet().add(membersKey, members);

            // 设置过期时间
            redisTemplate.expire(membersKey, MEMBER_CACHE_TTL_HOURS, TimeUnit.HOURS);

            log.debug("Cached member list: sessionId={}, count={}", 
                      sessionId, memberIds.size());

        } catch (Exception e) {
            log.error("Failed to cache member list: sessionId={}", sessionId, e);
        }
    }

    /**
     * 获取会话成员列表（从缓存）
     * 
     * @param sessionId 会话ID
     * @return 成员ID列表（如果缓存不存在则返回 null）
     */
    public List<Long> getMemberList(Long sessionId) {
        try {
            String membersKey = RedisKeyConstants.sessionMembersKey(sessionId);
            Set<String> members = redisTemplate.opsForSet().members(membersKey);

            if (members == null || members.isEmpty()) {
                log.debug("Member list cache miss: sessionId={}", sessionId);
                return null;
            }

            // 转换为 Long 列表
            List<Long> memberIds = members.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            log.debug("Member list cache hit: sessionId={}, count={}", 
                      sessionId, memberIds.size());

            return memberIds;

        } catch (Exception e) {
            log.error("Failed to get member list from cache: sessionId={}", sessionId, e);
            return null;
        }
    }

    /**
     * 添加成员到缓存
     * 
     * @param sessionId 会话ID
     * @param userId    用户ID
     */
    public void addMember(Long sessionId, Long userId) {
        try {
            String membersKey = RedisKeyConstants.sessionMembersKey(sessionId);
            redisTemplate.opsForSet().add(membersKey, userId.toString());
            redisTemplate.expire(membersKey, MEMBER_CACHE_TTL_HOURS, TimeUnit.HOURS);

            log.debug("Added member to cache: sessionId={}, userId={}", sessionId, userId);

        } catch (Exception e) {
            log.error("Failed to add member to cache: sessionId={}, userId={}", 
                      sessionId, userId, e);
        }
    }

    /**
     * 从缓存中删除成员
     * 
     * @param sessionId 会话ID
     * @param userId    用户ID
     */
    public void removeMember(Long sessionId, Long userId) {
        try {
            String membersKey = RedisKeyConstants.sessionMembersKey(sessionId);
            redisTemplate.opsForSet().remove(membersKey, userId.toString());

            log.debug("Removed member from cache: sessionId={}, userId={}", 
                      sessionId, userId);

        } catch (Exception e) {
            log.error("Failed to remove member from cache: sessionId={}, userId={}", 
                      sessionId, userId, e);
        }
    }

    /**
     * 清空会话成员缓存
     * 
     * @param sessionId 会话ID
     */
    public void clearMemberCache(Long sessionId) {
        try {
            String membersKey = RedisKeyConstants.sessionMembersKey(sessionId);
            redisTemplate.delete(membersKey);

            log.debug("Cleared member cache: sessionId={}", sessionId);

        } catch (Exception e) {
            log.error("Failed to clear member cache: sessionId={}", sessionId, e);
        }
    }
}

