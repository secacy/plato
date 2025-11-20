package com.plato.consumer.service;

import com.plato.consumer.event.MessageEvent;
import com.plato.consumer.redis.MessageCacheRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 读扩散服务 (Job B)
 * 
 * 职责：将新消息写入 Redis 公共消息缓存
 * 
 * 核心逻辑：
 * 1. 将新消息写入 Redis 公共缓存
 *    - cache:msgs:{session_id} (ZSET) - 消息索引
 *    - msg_meta:{msg_id} (String) - 消息元数据（Protobuf 序列化）
 * 2. 供后续高频读取（GetMessages 接口）
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReadFanoutService {

    private final MessageCacheRedisService messageCacheService;

    /**
     * 执行读扩散（缓存消息到 Redis）
     * 
     * @param event 消息事件
     */
    public void fanout(MessageEvent event) {
        try {
            Long sessionId = event.getSessionId();
            Long seqId = event.getSeqId();
            Long msgId = event.getMsgId();
            Long senderId = event.getSenderId();
            Integer msgType = event.getMsgType();
            String content = event.getContent();
            Integer status = event.getStatus();

            // 获取创建时间戳
            Long createTimeMs = event.getCreateTime() != null 
                    ? event.getCreateTime().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
                    : System.currentTimeMillis();

            log.debug("Starting read fanout: sessionId={}, seqId={}, msgId={}", 
                      sessionId, seqId, msgId);

            // ========== 写入消息到 Redis 公共缓存 ==========
            messageCacheService.cacheMessage(sessionId, seqId, msgId, 
                    content, senderId, msgType, status, createTimeMs);

            log.info("Read fanout completed: sessionId={}, msgId={}", sessionId, msgId);

        } catch (Exception e) {
            log.error("Read fanout failed: event={}", event, e);
        }
    }

    /**
     * 处理消息状态更新（撤回/删除）
     * 
     * @param event 消息事件
     */
    public void handleStatusUpdate(MessageEvent event) {
        try {
            Long msgId = event.getMsgId();
            Integer newStatus = event.getStatus();

            log.debug("Handling message status update: msgId={}, newStatus={}", 
                      msgId, newStatus);

            // 如果是删除状态，直接从缓存中移除
            if (newStatus == 2) {
                messageCacheService.removeMessage(event.getSessionId(), msgId);
                log.info("Removed message from cache: msgId={}", msgId);
            } else {
                // 更新消息状态
                messageCacheService.updateMessageStatus(msgId, newStatus);
                log.info("Updated message status in cache: msgId={}, newStatus={}", 
                         msgId, newStatus);
            }

        } catch (Exception e) {
            log.error("Failed to handle message status update: event={}", event, e);
        }
    }
}

