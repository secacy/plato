package com.plato.storage.service;

import com.plato.gateway.GetInboxesRequest;
import com.plato.gateway.InboxItem;
import com.plato.gateway.SetInboxAttributesRequest;
import com.plato.gateway.SetInboxReadRequest;
import com.plato.storage.redis.InboxRedisService;
import com.plato.storage.redis.dto.SessionLatestDto;
import com.plato.storage.redis.dto.UserInboxMetaDto;
import com.plato.storage.repository.entity.InboxEntity;
import com.plato.storage.repository.entity.SessionMetaEntity;
import com.plato.storage.repository.mapper.InboxMapper;
import com.plato.storage.repository.mapper.SessionMetaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 收件箱业务服务 (新设计)
 * 
 * 核心流程：
 * 1. GetInboxes - 完整的缓存回源流程，包含缓存击穿、雪崩、穿透防护
 * 2. SetInboxRead - 更新已读位置
 * 3. SetInboxAttributes - 更新会话属性（置顶/免打扰），置顶需要更新 ZSET Score
 * 
 * @author hc
 * @since 2025/11/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxRedisService inboxRedisService;
    private final InboxMapper inboxMapper;
    private final SessionMetaMapper sessionMetaMapper;
    private final SingleFlightService singleFlightService;

    /**
     * 热数据窗口大小（用户收件箱 ZSET 只缓存最近 N 个会话）
     */
    private static final int HOT_DATA_WINDOW_SIZE = 100;

    /**
     * 获取用户的会话列表
     * 
     * 完整流程：
     * 1. 获取会话 ID 列表
     * 1.1 Redis 查询：从 user_inbox_zset:{user_id} 按 Score 倒序拉取
     * 1.2 Hit: 进入阶段二
     * 1.3 Miss: 回源查询 TiDB inbox 表并重建 Redis
     * 
     * 2. 获取会话详情
     * 2.1 并行拉取：user_inbox_meta:{user_id} 和 session_latest:{session_id}
     * 2.2 局部 Miss: 回源查询 TiDB 并回填 Redis
     * 
     * 3. 内存计算：计算未读数 (unread_count = maxSeq - readSeq)
     * 
     * @param request 请求
     * @return 会话列表
     */
    public List<InboxItem> getInboxes(GetInboxesRequest request) {
        long userId = request.getUserId();
        int limit = request.getLimit() > 0 ? request.getLimit() : 20;

        log.info("GetInboxes start, userId={}, anchorTimeMs={}, limit={}",
                userId, request.getAnchorTimeMs(), limit);

        // ========== 阶段一：获取会话 ID 列表 ==========
        List<Long> sessionIds = getSessionIdList(userId, request.getAnchorTimeMs(), limit);

        if (sessionIds.isEmpty()) {
            log.info("GetInboxes: no sessions found, userId={}", userId);
            return Collections.emptyList();
        }

        log.info("GetInboxes: got {} sessionIds, userId={}", sessionIds.size(), userId);

        // ========== 阶段二：获取会话详情 ==========
        Map<Long, UserInboxMetaDto> userMetaMap = getUserInboxMeta(userId, sessionIds);
        Map<Long, SessionLatestDto> sessionSnapshotMap = getSessionSnapshots(sessionIds);

        // ========== 阶段三：内存计算与清洗 ==========
        List<InboxItem> result = new ArrayList<>();
        for (Long sessionId : sessionIds) {
            UserInboxMetaDto userMeta = userMetaMap.get(sessionId);
            SessionLatestDto snapshot = sessionSnapshotMap.get(sessionId);

            // 如果快照不存在，跳过该会话（数据异常）
            if (snapshot == null) {
                log.warn("GetInboxes: session snapshot not found, sessionId={}", sessionId);
                continue;
            }

            // 如果用户 Meta 不存在，使用默认值
            if (userMeta == null) {
                userMeta = UserInboxMetaDto.builder()
                        .sessionId(sessionId)
                        .readSeq(0L)
                        .isPinned(false)
                        .isMuted(false)
                        .createTime(System.currentTimeMillis())
                        .build();
            }

            // 计算未读数
            long unreadCount = Math.max(0, snapshot.getMaxSeq() - userMeta.getReadSeq());

            // 构建返回对象
            InboxItem item = InboxItem.newBuilder()
                    .setSessionId(sessionId)
                    .setUnreadCount((int) unreadCount)
                    .setLastMsgContentPreview(snapshot.getLastMsgContent() != null ? snapshot.getLastMsgContent() : "")
                    .setLastMsgTimeMs(snapshot.getLastMsgTime() != null ? snapshot.getLastMsgTime() : 0L)
                    .setLastMsgSenderId(snapshot.getLastMsgSenderId() != null ? snapshot.getLastMsgSenderId() : 0L)
                    .setIsPinned(userMeta.getIsPinned() != null && userMeta.getIsPinned())
                    .setIsMuted(userMeta.getIsMuted() != null && userMeta.getIsMuted())
                    .build();

            result.add(item);
        }

        log.info("GetInboxes success, userId={}, resultSize={}", userId, result.size());
        return result;
    }

    /**
     * 获取会话 ID 列表（带回源逻辑）
     * 
     * @param userId       用户 ID
     * @param anchorTimeMs 锚点时间（0 表示从头开始）
     * @param limit        限制条数
     * @return 会话 ID 列表
     */
    private List<Long> getSessionIdList(Long userId, Long anchorTimeMs, int limit) {
        // 1. 尝试从 Redis 获取
        Double anchorScore = anchorTimeMs > 0 ? anchorTimeMs.doubleValue() : null;
        List<Long> sessionIds = inboxRedisService.getInboxSessionIds(userId, anchorScore, limit);

        // 2. 如果命中 Redis，直接返回
        if (!sessionIds.isEmpty()) {
            log.debug("GetInboxes: hit Redis, userId={}, size={}", userId, sessionIds.size());
            return sessionIds;
        }

        // 3. Redis Miss，回源查询 TiDB
        log.info("GetInboxes: Redis miss, fallback to DB, userId={}", userId);

        // 3.1 检查 ZSET 是否存在
        boolean zsetExists = inboxRedisService.existsInboxZSet(userId);

        if (!zsetExists) {
            // 情况1: Key 不存在，用户太久没登录，缓存过期
            // 重建"用户最近活跃列表（热数据窗口 Top N）"缓存
            return rebuildUserInboxZSet(userId, limit);
        } else {
            // 情况2: 数据截断，用户想拉 > N 条列表，但 Redis 只存了 Top N
            // 直接降级查询 TiDB，不回写 Redis
            return fallbackQueryFromDB(userId, limit);
        }
    }

    /**
     * 重建用户收件箱 ZSET（从数据库加载热数据窗口）
     * 
     * @param userId 用户 ID
     * @param limit  请求的 limit
     * @return 会话 ID 列表
     */
    private List<Long> rebuildUserInboxZSet(Long userId, int limit) {
        log.info("Rebuilding user inbox ZSET, userId={}", userId);

        // 从数据库查询热数据窗口（Top N）
        List<InboxEntity> inboxList = inboxMapper.selectByUserId(userId, HOT_DATA_WINDOW_SIZE);

        if (inboxList.isEmpty()) {
            log.info("User has no inbox data, userId={}", userId);
            return Collections.emptyList();
        }

        // 重建 Redis ZSET 和 Meta
        List<Long> sessionIds = new ArrayList<>();
        List<Double> scores = new ArrayList<>();

        for (InboxEntity entity : inboxList) {
            sessionIds.add(entity.getSessionId());
            double score = calculateScore(entity.getLastMsgTime(), entity.getIsPinned());
            scores.add(score);

            // 同时回填用户 Meta
            UserInboxMetaDto meta = UserInboxMetaDto.builder()
                    .sessionId(entity.getSessionId())
                    .readSeq(entity.getLastReadSeqId())
                    .isPinned(entity.getIsPinned())
                    .isMuted(entity.getIsMuted())
                    .createTime(System.currentTimeMillis())
                    .build();
            inboxRedisService.setUserInboxMeta(userId, meta);
        }

        // 批量写入 ZSET
        inboxRedisService.rebuildInboxZSet(userId, sessionIds, scores);

        // 返回前 limit 个
        int returnSize = Math.min(limit, sessionIds.size());
        return sessionIds.subList(0, returnSize);
    }

    /**
     * 降级查询数据库（不回写 Redis）
     * 
     * @param userId 用户 ID
     * @param limit  限制条数
     * @return 会话 ID 列表
     */
    private List<Long> fallbackQueryFromDB(Long userId, int limit) {
        log.info("Fallback query from DB, userId={}, limit={}", userId, limit);

        List<InboxEntity> inboxList = inboxMapper.selectByUserId(userId, limit);
        List<Long> sessionIds = new ArrayList<>();
        for (InboxEntity entity : inboxList) {
            sessionIds.add(entity.getSessionId());
        }
        return sessionIds;
    }

    /**
     * 获取用户会话偏好（带回源逻辑）
     * 
     * @param userId     用户 ID
     * @param sessionIds 会话 ID 列表
     * @return Map<sessionId, UserInboxMetaDto>
     */
    private Map<Long, UserInboxMetaDto> getUserInboxMeta(Long userId, List<Long> sessionIds) {
        // 1. 批量从 Redis 获取
        Map<Long, UserInboxMetaDto> metaMap = inboxRedisService.batchGetUserInboxMeta(userId, sessionIds);

        // 2. 检查是否有 Miss
        Set<Long> missedSessionIds = new HashSet<>();
        for (Long sessionId : sessionIds) {
            if (!metaMap.containsKey(sessionId)) {
                missedSessionIds.add(sessionId);
            }
        }

        // 3. 回源查询 Miss 的数据
        if (!missedSessionIds.isEmpty()) {
            log.info("UserInboxMeta miss, userId={}, missedSize={}", userId, missedSessionIds.size());

            for (Long sessionId : missedSessionIds) {
                InboxEntity entity = inboxMapper.selectByPrimaryKey(userId, sessionId);
                if (entity != null) {
                    UserInboxMetaDto meta = UserInboxMetaDto.builder()
                            .sessionId(sessionId)
                            .readSeq(entity.getLastReadSeqId())
                            .isPinned(entity.getIsPinned())
                            .isMuted(entity.getIsMuted())
                            .createTime(System.currentTimeMillis())
                            .build();

                    // 回填 Redis
                    inboxRedisService.setUserInboxMeta(userId, meta);
                    metaMap.put(sessionId, meta);
                } else {
                    log.warn("InboxEntity not found in DB, userId={}, sessionId={}", userId, sessionId);
                }
            }
        }

        return metaMap;
    }

    /**
     * 获取会话全局快照（带回源逻辑 + SingleFlight 保护）
     * 
     * @param sessionIds 会话 ID 列表
     * @return Map<sessionId, SessionLatestDto>
     */
    private Map<Long, SessionLatestDto> getSessionSnapshots(List<Long> sessionIds) {
        // 1. 批量从 Redis 获取
        Map<Long, SessionLatestDto> snapshotMap = inboxRedisService.batchGetSessionLatest(sessionIds);

        // 2. 检查是否有 Miss
        Set<Long> missedSessionIds = new HashSet<>();
        for (Long sessionId : sessionIds) {
            if (!snapshotMap.containsKey(sessionId)) {
                // 检查是否有空对象标记（防止缓存穿透）
                if (inboxRedisService.hasNullMarker("session", sessionId)) {
                    log.debug("Session has null marker, skip, sessionId={}", sessionId);
                    continue;
                }
                missedSessionIds.add(sessionId);
            }
        }

        // 3. 回源查询 Miss 的数据（使用 SingleFlight 防止缓存击穿）
        if (!missedSessionIds.isEmpty()) {
            log.info("SessionSnapshot miss, missedSize={}", missedSessionIds.size());

            for (Long sessionId : missedSessionIds) {
                // 使用 SingleFlight 归并同一时刻的重复请求
                SessionLatestDto snapshot = singleFlightService.execute(
                        "sess_snap:" + sessionId,
                        () -> loadSessionSnapshotFromDB(sessionId));

                if (snapshot != null) {
                    snapshotMap.put(sessionId, snapshot);
                }
            }
        }

        return snapshotMap;
    }

    /**
     * 从数据库加载会话快照（回源逻辑）
     * 
     * @param sessionId 会话 ID
     * @return 会话快照
     */
    private SessionLatestDto loadSessionSnapshotFromDB(Long sessionId) {
        log.info("Loading session snapshot from DB, sessionId={}", sessionId);

        SessionMetaEntity entity = sessionMetaMapper.selectById(sessionId);

        if (entity == null) {
            log.warn("SessionMeta not found in DB, sessionId={}", sessionId);
            // 设置空对象标记（防止缓存穿透）
            inboxRedisService.setNullMarker("session", sessionId);
            return null;
        }

        // 构建快照 DTO
        SessionLatestDto snapshot = SessionLatestDto.builder()
                .sessionId(sessionId)
                .maxSeq(entity.getMaxSeq())
                .lastMsgTime(entity.getLastMsgTime())
                .lastMsgSenderId(entity.getLastMsgSenderId())
                .lastMsgType(entity.getLastMsgType())
                .lastMsgContent(entity.getLastMsgContent())
                .build();

        // 回填 Redis
        inboxRedisService.setSessionLatest(snapshot);

        return snapshot;
    }

    /**
     * 设置已读位置
     * 
     * 逻辑：更新 TiDB inbox 表的 last_read_seq_id 和 Redis 中该用户的 last_read_seq_id
     * 
     * @param request 请求
     */
    public void setInboxRead(SetInboxReadRequest request) {
        long userId = request.getUserId();
        long sessionId = request.getSessionId();
        long readSeqId = request.getReadSeqId();

        log.info("SetInboxRead, userId={}, sessionId={}, readSeqId={}", userId, sessionId, readSeqId);

        // 1. 更新 TiDB（冷数据）
        int rows = inboxMapper.updateReadPosition(userId, sessionId, readSeqId);

        // 2. 更新 Redis Meta
        if (rows > 0) {
            inboxRedisService.updateReadSeq(userId, sessionId, readSeqId);
            log.info("SetInboxRead success, userId={}, sessionId={}, readSeqId={}", userId, sessionId, readSeqId);
        } else {
            log.warn("SetInboxRead: no rows updated in DB, userId={}, sessionId={}", userId, sessionId);
        }
    }

    /**
     * 设置会话属性（置顶 / 免打扰）
     * 
     * 逻辑修正：
     * - 更新 TiDB 和 Redis Meta 中的属性字段
     * - 如果是置顶操作，需要更新 Redis user_inbox_zset:{user_id} 中该 Session 的 Score
     * a. NewScore = current_timestamp + 1_000_000_000_000 (Magic Number)
     * b. 如果是取消置顶，需要查回该 Session 真实的 last_msg_time 并恢复 Score
     * 
     * @param request 请求
     */
    public void setInboxAttributes(SetInboxAttributesRequest request) {
        long userId = request.getUserId();
        long sessionId = request.getSessionId();

        log.info("SetInboxAttributes, userId={}, sessionId={}, attributeCase={}",
                userId, sessionId, request.getAttributeCase());

        switch (request.getAttributeCase()) {
            case IS_PINNED:
                handlePinnedAttribute(userId, sessionId, request.getIsPinned());
                break;

            case IS_MUTED:
                handleMutedAttribute(userId, sessionId, request.getIsMuted());
                break;

            default:
                log.warn("Unknown attribute type: {}", request.getAttributeCase());
        }
    }

    /**
     * 处理置顶属性
     * 
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param isPinned  是否置顶
     */
    private void handlePinnedAttribute(Long userId, Long sessionId, boolean isPinned) {
        // 1. 更新 TiDB
        int rows = inboxMapper.updatePinned(userId, sessionId, isPinned);
        if (rows == 0) {
            log.warn("UpdatePinned: no rows updated in DB, userId={}, sessionId={}", userId, sessionId);
            return;
        }

        // 2. 更新 Redis Meta
        Map<Long, UserInboxMetaDto> metaMap = inboxRedisService.batchGetUserInboxMeta(
                userId, Collections.singletonList(sessionId));
        UserInboxMetaDto meta = metaMap.get(sessionId);

        if (meta == null) {
            // 如果 Redis 没有，从数据库加载
            InboxEntity entity = inboxMapper.selectByPrimaryKey(userId, sessionId);
            if (entity != null) {
                meta = UserInboxMetaDto.builder()
                        .sessionId(sessionId)
                        .readSeq(entity.getLastReadSeqId())
                        .isPinned(isPinned)
                        .isMuted(entity.getIsMuted())
                        .createTime(System.currentTimeMillis())
                        .build();
            }
        } else {
            meta.setIsPinned(isPinned);
        }

        if (meta != null) {
            inboxRedisService.setUserInboxMeta(userId, meta);
        }

        // 3. 更新 Redis ZSET Score（关键逻辑）
        // 获取会话快照，拿到真实的 last_msg_time
        Map<Long, SessionLatestDto> snapshotMap = inboxRedisService.batchGetSessionLatest(
                Collections.singletonList(sessionId));
        SessionLatestDto snapshot = snapshotMap.get(sessionId);

        if (snapshot == null) {
            // 如果 Redis 没有，从数据库加载
            SessionMetaEntity entity = sessionMetaMapper.selectById(sessionId);
            if (entity != null) {
                snapshot = SessionLatestDto.builder()
                        .sessionId(sessionId)
                        .lastMsgTime(entity.getLastMsgTime())
                        .build();
            }
        }

        if (snapshot != null && snapshot.getLastMsgTime() != null) {
            inboxRedisService.updateInboxScore(userId, sessionId, snapshot.getLastMsgTime(), isPinned);
            log.info("UpdatePinned success and updated ZSET score, userId={}, sessionId={}, isPinned={}",
                    userId, sessionId, isPinned);
        } else {
            log.warn("UpdatePinned: cannot get last_msg_time, skip ZSET update, sessionId={}", sessionId);
        }
    }

    /**
     * 处理免打扰属性
     * 
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param isMuted   是否免打扰
     */
    private void handleMutedAttribute(Long userId, Long sessionId, boolean isMuted) {
        // 1. 更新 TiDB
        int rows = inboxMapper.updateMuted(userId, sessionId, isMuted);
        if (rows == 0) {
            log.warn("UpdateMuted: no rows updated in DB, userId={}, sessionId={}", userId, sessionId);
            return;
        }

        // 2. 更新 Redis Meta
        Map<Long, UserInboxMetaDto> metaMap = inboxRedisService.batchGetUserInboxMeta(
                userId, Collections.singletonList(sessionId));
        UserInboxMetaDto meta = metaMap.get(sessionId);

        if (meta == null) {
            // 如果 Redis 没有，从数据库加载
            InboxEntity entity = inboxMapper.selectByPrimaryKey(userId, sessionId);
            if (entity != null) {
                meta = UserInboxMetaDto.builder()
                        .sessionId(sessionId)
                        .readSeq(entity.getLastReadSeqId())
                        .isPinned(entity.getIsPinned())
                        .isMuted(isMuted)
                        .createTime(System.currentTimeMillis())
                        .build();
            }
        } else {
            meta.setIsMuted(isMuted);
        }

        if (meta != null) {
            inboxRedisService.setUserInboxMeta(userId, meta);
            log.info("UpdateMuted success, userId={}, sessionId={}, isMuted={}", userId, sessionId, isMuted);
        }
    }

    /**
     * 计算 ZSET Score
     * 
     * @param lastMsgTime 最新消息时间
     * @param isPinned    是否置顶
     * @return Score
     */
    private double calculateScore(Long lastMsgTime, Boolean isPinned) {
        if (Boolean.TRUE.equals(isPinned)) {
            return lastMsgTime + 1_000_000_000_000L;
        }
        return lastMsgTime;
    }
}
