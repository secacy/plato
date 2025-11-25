package com.plato.storage.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plato.storage.redis.dto.SessionLatestDto;
import com.plato.storage.redis.dto.UserInboxMetaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 收件箱 Redis 服务 (新设计)
 * 
 * 核心数据结构：
 * 1. user_inbox_zset:{user_id} - 用户收件箱列表（仅存 ID 和排序 Score）
 * 2. user_inbox_meta:{user_id} - 用户会话偏好（存用户的 Seq 和设置）
 * 3. session_latest:{session_id} - 会话全局最新快照（存预览和 MaxSeq）
 * 
 * @author hc
 * @since 2025/11/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboxRedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 置顶会话的 Score 魔术数字
     * 置顶会话 Score = 实际时间 + PINNED_MAGIC_NUMBER
     */
    private static final long PINNED_MAGIC_NUMBER = 1_000_000_000_000L;

    /**
     * 缓存过期时间配置
     */
    private static final long USER_INBOX_ZSET_TTL_DAYS = 7; // 用户收件箱列表 TTL
    private static final long SESSION_LATEST_TTL_DAYS = 7; // 会话快照 TTL
    private static final long NULL_MARKER_TTL_SECONDS = 60; // 空对象标记 TTL

    // ========== 1. 用户收件箱列表 (ZSET) ==========

    /**
     * 添加会话到用户收件箱列表
     * 
     * @param userId      用户 ID
     * @param sessionId   会话 ID
     * @param lastMsgTime 最新消息时间（毫秒）
     * @param isPinned    是否置顶
     */
    public void addToInboxZSet(Long userId, Long sessionId, Long lastMsgTime, Boolean isPinned) {
        String key = RedisKeyConstants.userInboxZSetKey(userId);
        double score = calculateScore(lastMsgTime, isPinned);
        redisTemplate.opsForZSet().add(key, sessionId.toString(), score);

        // 设置过期时间（带随机值防止雪崩）
        setExpireWithJitter(key, USER_INBOX_ZSET_TTL_DAYS, TimeUnit.DAYS);
    }

    /**
     * 批量添加会话到用户收件箱列表（写扩散）
     * 
     * @param userIds     用户 ID 列表
     * @param sessionId   会话 ID
     * @param lastMsgTime 最新消息时间
     */
    public void fanoutToInboxZSet(List<Long> userIds, Long sessionId, Long lastMsgTime) {
        for (Long userId : userIds) {
            addToInboxZSet(userId, sessionId, lastMsgTime, false);
        }
    }

    /**
     * 获取用户收件箱列表（分页）
     * 
     * @param userId      用户 ID
     * @param anchorScore 锚点 Score（0 表示从头开始）
     * @param limit       限制条数
     * @return 会话 ID 列表（有序）
     */
    public List<Long> getInboxSessionIds(Long userId, Double anchorScore, int limit) {
        String key = RedisKeyConstants.userInboxZSetKey(userId);

        Set<String> sessionIds;
        if (anchorScore == null || anchorScore == 0) {
            // 从最新开始
            sessionIds = redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);
        } else {
            // 从锚点开始（不包含锚点本身）
            sessionIds = redisTemplate.opsForZSet().reverseRangeByScore(
                    key, 0, anchorScore, 0, limit);
        }

        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> result = new ArrayList<>();
        for (String sessionId : sessionIds) {
            result.add(Long.parseLong(sessionId));
        }
        return result;
    }

    /**
     * 更新会话在收件箱中的 Score（用于置顶/取消置顶）
     * 
     * @param userId      用户 ID
     * @param sessionId   会话 ID
     * @param lastMsgTime 实际最新消息时间
     * @param isPinned    是否置顶
     */
    public void updateInboxScore(Long userId, Long sessionId, Long lastMsgTime, Boolean isPinned) {
        String key = RedisKeyConstants.userInboxZSetKey(userId);
        double score = calculateScore(lastMsgTime, isPinned);
        redisTemplate.opsForZSet().add(key, sessionId.toString(), score);
    }

    /**
     * 检查用户收件箱 ZSET 是否存在
     * 
     * @param userId 用户 ID
     * @return true 存在，false 不存在
     */
    public boolean existsInboxZSet(Long userId) {
        String key = RedisKeyConstants.userInboxZSetKey(userId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 重建用户收件箱 ZSET（从数据库加载）
     * 
     * @param userId     用户 ID
     * @param sessionIds 会话 ID 列表
     * @param scores     对应的 Score 列表
     */
    public void rebuildInboxZSet(Long userId, List<Long> sessionIds, List<Double> scores) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }

        String key = RedisKeyConstants.userInboxZSetKey(userId);

        // 批量添加
        for (int i = 0; i < sessionIds.size(); i++) {
            redisTemplate.opsForZSet().add(key, sessionIds.get(i).toString(), scores.get(i));
        }

        // 设置过期时间
        setExpireWithJitter(key, USER_INBOX_ZSET_TTL_DAYS, TimeUnit.DAYS);
    }

    // ========== 2. 用户会话偏好 (HASH) ==========

    /**
     * 设置用户会话偏好
     * 
     * @param userId 用户 ID
     * @param meta   用户会话偏好
     */
    public void setUserInboxMeta(Long userId, UserInboxMetaDto meta) {
        try {
            String key = RedisKeyConstants.userInboxMetaKey(userId);
            String field = meta.getSessionId().toString();
            String value = objectMapper.writeValueAsString(meta);
            redisTemplate.opsForHash().put(key, field, value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize UserInboxMeta, userId={}, sessionId={}",
                    userId, meta.getSessionId(), e);
        }
    }

    /**
     * 批量获取用户会话偏好
     * 
     * @param userId     用户 ID
     * @param sessionIds 会话 ID 列表
     * @return Map<sessionId, UserInboxMetaDto>
     */
    public Map<Long, UserInboxMetaDto> batchGetUserInboxMeta(Long userId, List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String key = RedisKeyConstants.userInboxMetaKey(userId);
        List<Object> fields = new ArrayList<>(sessionIds.size());
        for (Long sessionId : sessionIds) {
            fields.add(sessionId.toString());
        }

        List<Object> values = redisTemplate.opsForHash().multiGet(key, fields);

        Map<Long, UserInboxMetaDto> result = new HashMap<>();
        for (int i = 0; i < sessionIds.size(); i++) {
            Object value = values.get(i);
            if (value != null) {
                try {
                    UserInboxMetaDto dto = objectMapper.readValue(value.toString(), UserInboxMetaDto.class);
                    result.put(sessionIds.get(i), dto);
                } catch (Exception e) {
                    log.error("Failed to deserialize UserInboxMeta, sessionId={}", sessionIds.get(i), e);
                }
            }
        }
        return result;
    }

    /**
     * 更新用户已读位置
     * 
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param readSeq   已读序列号
     */
    public void updateReadSeq(Long userId, Long sessionId, Long readSeq) {
        String key = RedisKeyConstants.userInboxMetaKey(userId);
        String field = sessionId.toString();

        // 先获取现有数据
        Object value = redisTemplate.opsForHash().get(key, field);
        UserInboxMetaDto meta;

        if (value != null) {
            try {
                meta = objectMapper.readValue(value.toString(), UserInboxMetaDto.class);
            } catch (Exception e) {
                log.error("Failed to deserialize UserInboxMeta, userId={}, sessionId={}", userId, sessionId, e);
                return;
            }
        } else {
            // 如果不存在，创建新的
            meta = UserInboxMetaDto.builder()
                    .sessionId(sessionId)
                    .readSeq(0L)
                    .isPinned(false)
                    .isMuted(false)
                    .createTime(System.currentTimeMillis())
                    .build();
        }

        // 更新已读位置
        meta.setReadSeq(readSeq);
        setUserInboxMeta(userId, meta);
    }

    // ========== 3. 会话全局快照 (STRING) ==========

    /**
     * 设置会话全局快照
     * 
     * @param snapshot 会话快照
     */
    public void setSessionLatest(SessionLatestDto snapshot) {
        try {
            String key = RedisKeyConstants.sessionLatestKey(snapshot.getSessionId());
            String value = objectMapper.writeValueAsString(snapshot);
            redisTemplate.opsForValue().set(key, value);

            // 设置过期时间（带随机值防止雪崩）
            setExpireWithJitter(key, SESSION_LATEST_TTL_DAYS, TimeUnit.DAYS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize SessionLatest, sessionId={}", snapshot.getSessionId(), e);
        }
    }

    /**
     * 批量获取会话全局快照
     * 
     * @param sessionIds 会话 ID 列表
     * @return Map<sessionId, SessionLatestDto>
     */
    public Map<Long, SessionLatestDto> batchGetSessionLatest(List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 构建 Key 列表
        List<String> keys = new ArrayList<>(sessionIds.size());
        for (Long sessionId : sessionIds) {
            keys.add(RedisKeyConstants.sessionLatestKey(sessionId));
        }

        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, SessionLatestDto> result = new HashMap<>();
        if (values != null) {
            for (int i = 0; i < sessionIds.size(); i++) {
                String value = values.get(i);
                if (value != null) {
                    try {
                        SessionLatestDto dto = objectMapper.readValue(value, SessionLatestDto.class);
                        result.put(sessionIds.get(i), dto);
                    } catch (Exception e) {
                        log.error("Failed to deserialize SessionLatest, sessionId={}", sessionIds.get(i), e);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 检查会话快照是否存在
     * 
     * @param sessionId 会话 ID
     * @return true 存在，false 不存在
     */
    public boolean existsSessionLatest(Long sessionId) {
        String key = RedisKeyConstants.sessionLatestKey(sessionId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // ========== 4. 缓存穿透防护（空对象标记） ==========

    /**
     * 设置空对象标记（用于防止缓存穿透）
     * 
     * @param type 类型（如 "session", "user_meta"）
     * @param id   ID
     */
    public void setNullMarker(String type, Long id) {
        String key = RedisKeyConstants.nullMarkerKey(type, id);
        redisTemplate.opsForValue().set(key, "1", NULL_MARKER_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 检查是否存在空对象标记
     * 
     * @param type 类型
     * @param id   ID
     * @return true 存在（表示该对象不存在于数据库），false 不存在
     */
    public boolean hasNullMarker(String type, Long id) {
        String key = RedisKeyConstants.nullMarkerKey(type, id);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // ========== 工具方法 ==========

    /**
     * 计算 ZSET Score
     * 
     * @param lastMsgTime 最新消息时间
     * @param isPinned    是否置顶
     * @return Score
     */
    private double calculateScore(Long lastMsgTime, Boolean isPinned) {
        if (Boolean.TRUE.equals(isPinned)) {
            return lastMsgTime + PINNED_MAGIC_NUMBER;
        }
        return lastMsgTime;
    }

    /**
     * 设置过期时间（带随机抖动，防止缓存雪崩）
     * 
     * @param key      Key
     * @param baseTtl  基础 TTL
     * @param timeUnit 时间单位
     */
    private void setExpireWithJitter(String key, long baseTtl, TimeUnit timeUnit) {
        // 添加 0~12 小时的随机抖动
        long jitterSeconds = ThreadLocalRandom.current().nextLong(0, 12 * 60 * 60);
        long totalSeconds = timeUnit.toSeconds(baseTtl) + jitterSeconds;
        redisTemplate.expire(key, totalSeconds, TimeUnit.SECONDS);
    }
}
