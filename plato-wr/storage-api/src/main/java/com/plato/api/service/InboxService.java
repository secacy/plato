package com.plato.api.service;

import com.plato.api.redis.InboxRedisService;
import com.plato.api.redis.InboxRedisService.SessionMetaDto;
import com.plato.api.repository.mapper.InboxMapper;
import com.plato.gateway.GetInboxesRequest;
import com.plato.gateway.InboxItem;
import com.plato.gateway.SetInboxAttributesRequest;
import com.plato.gateway.SetInboxReadRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 收件箱业务服务
 * 
 * 职责：管理用户的会话列表和已读状态
 * 
 * @author hc
 * @since 2025/11/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxRedisService inboxRedisService;
    private final InboxMapper inboxMapper;

    /**
     * 获取用户的会话列表
     * 
     * @param request 请求
     * @return 会话列表
     */
    public List<InboxItem> getInboxes(GetInboxesRequest request) {
        // 1. 从Redis获取会话ID列表（已按时间排序）
        List<Long> sessionIds = inboxRedisService.getSessionList(
                request.getUserId(),
                request.getAnchorTimeMs(),
                request.getLimit());

        // 2. 获取每个会话的元数据
        List<InboxItem> result = new ArrayList<>();
        for (Long sessionId : sessionIds) {
            SessionMetaDto meta = inboxRedisService.getSessionMeta(
                    request.getUserId(), sessionId);

            if (meta != null) {
                InboxItem item = InboxItem.newBuilder()
                        .setSessionId(sessionId)
                        .setUnreadCount(meta.getUnreadCount())
                        .setLastMsgContentPreview(meta.getLastMsgContentPreview())
                        .setLastMsgTimeMs(meta.getLastMsgTimeMs())
                        .setLastMsgSenderId(meta.getLastMsgSenderId())
                        .setIsPinned(meta.getIsPinned())
                        .setIsMuted(meta.getIsMuted())
                        .build();
                result.add(item);
            }
        }

        return result;
    }

    /**
     * 设置已读位置
     * 
     * @param request 请求
     */
    public void setInboxRead(SetInboxReadRequest request) {
        // 1. 更新TiDB（冷数据）
        inboxMapper.updateReadPosition(
                request.getUserId(),
                request.getSessionId(),
                request.getReadSeqId());

        // 2. 更新Redis元数据中的未读数
        // TODO: 需要查询当前最大seqId来计算新的未读数
        // 这里先简化处理

        log.info("Inbox read position updated, userId={}, sessionId={}, readSeqId={}",
                request.getUserId(), request.getSessionId(), request.getReadSeqId());
    }

    /**
     * 设置会话属性
     * 
     * @param request 请求
     */
    public void setInboxAttributes(SetInboxAttributesRequest request) {
        long userId = request.getUserId();
        long sessionId = request.getSessionId();

        // 根据属性类型更新
        switch (request.getAttributeCase()) {
            case IS_PINNED:
                inboxMapper.updatePinned(userId, sessionId, request.getIsPinned());

                // 更新Redis元数据
                SessionMetaDto meta = inboxRedisService.getSessionMeta(userId, sessionId);
                if (meta != null) {
                    meta.setIsPinned(request.getIsPinned());
                    inboxRedisService.updateSessionMeta(userId, meta);
                }

                log.info("Inbox pinned status updated, userId={}, sessionId={}, isPinned={}",
                        userId, sessionId, request.getIsPinned());
                break;

            case IS_MUTED:
                inboxMapper.updateMuted(userId, sessionId, request.getIsMuted());

                // 更新Redis元数据
                meta = inboxRedisService.getSessionMeta(userId, sessionId);
                if (meta != null) {
                    meta.setIsMuted(request.getIsMuted());
                    inboxRedisService.updateSessionMeta(userId, meta);
                }

                log.info("Inbox muted status updated, userId={}, sessionId={}, isMuted={}",
                        userId, sessionId, request.getIsMuted());
                break;

            default:
                log.warn("Unknown attribute type: {}", request.getAttributeCase());
        }
    }
}
