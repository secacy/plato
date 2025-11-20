package com.plato.consumer.service;

import com.plato.consumer.event.MessageEvent;
import com.plato.consumer.redis.MemberCacheRedisService;
import com.plato.consumer.redis.SessionListRedisService;
import com.plato.consumer.repository.mapper.MemberMapper;
import com.plato.consumer.util.MessagePreviewUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 写扩散服务 (Job A)
 * 
 * 职责：当新消息到达时，更新所有群成员的会话列表
 * 
 * 核心逻辑：
 * 1. 查询该会话的所有成员（优先从 Redis 缓存查询，Miss 时查 DB 并回填）
 * 2. 批量更新所有成员的 Redis 会话列表
 *    - user_sessions:{uid} (ZSET) - 按时间排序的会话列表
 *    - session_meta:{uid} (HASH) - 会话元数据（未读数、预览）
 *    - unread_count:{uid} (HASH) - 未读计数
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WriteFanoutService {

    private final MemberMapper memberMapper;
    private final MemberCacheRedisService memberCacheService;
    private final SessionListRedisService sessionListService;

    /**
     * 执行写扩散（更新所有成员的会话列表）
     * 
     * @param event 消息事件
     */
    public void fanout(MessageEvent event) {
        try {
            Long sessionId = event.getSessionId();
            Long seqId = event.getSeqId();
            Long senderId = event.getSenderId();

            // 生成消息预览文本
            String preview = generatePreview(event);

            // 获取创建时间戳
            Long timestamp = event.getCreateTime() != null 
                    ? event.getCreateTime().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
                    : System.currentTimeMillis();

            log.debug("Starting write fanout: sessionId={}, seqId={}", sessionId, seqId);

            // ========== Step 1: 获取群成员列表 ==========
            List<Long> memberIds = getMemberList(sessionId);

            if (memberIds == null || memberIds.isEmpty()) {
                log.warn("No members found for session: sessionId={}", sessionId);
                return;
            }

            log.debug("Found {} members for session {}", memberIds.size(), sessionId);

            // ========== Step 2: 批量更新会话列表 ==========
            sessionListService.batchUpdateSessionList(memberIds, sessionId, 
                    seqId, preview, timestamp);

            log.info("Write fanout completed: sessionId={}, memberCount={}", 
                     sessionId, memberIds.size());

        } catch (Exception e) {
            log.error("Write fanout failed: event={}", event, e);
        }
    }

    /**
     * 获取群成员列表（优先从 Redis 缓存）
     * 
     * @param sessionId 会话ID
     * @return 成员ID列表
     */
    private List<Long> getMemberList(Long sessionId) {
        // 1. 尝试从 Redis 缓存获取
        List<Long> memberIds = memberCacheService.getMemberList(sessionId);

        if (memberIds != null && !memberIds.isEmpty()) {
            log.debug("Member list cache hit: sessionId={}", sessionId);
            return memberIds;
        }

        // 2. 缓存未命中，查询数据库
        log.debug("Member list cache miss, querying DB: sessionId={}", sessionId);
        memberIds = memberMapper.selectMemberIds(sessionId);

        if (memberIds == null || memberIds.isEmpty()) {
            return null;
        }

        // 3. 回填缓存
        memberCacheService.cacheMemberList(sessionId, memberIds);

        return memberIds;
    }

    /**
     * 生成消息预览文本
     * 
     * @param event 消息事件
     * @return 预览文本
     */
    private String generatePreview(MessageEvent event) {
        return MessagePreviewUtil.generatePreview(event.getMsgType(), event.getContent());
    }
}

