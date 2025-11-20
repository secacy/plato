package com.plato.storage.service;

import com.plato.storage.batch.MessageBatchManager;
import com.plato.storage.batch.MessageBatchRequest;
import com.plato.storage.batch.MessageBatchResponse;
import com.plato.storage.redis.IdempotentService;
import com.plato.storage.redis.MessageCacheService;
import com.plato.storage.repository.entity.MessageEntity;
import com.plato.storage.repository.mapper.MessageMapper;
import com.plato.gateway.GetMessagesRequest;
import com.plato.gateway.Message;
import com.plato.gateway.SaveMessageRequest;
import com.plato.gateway.SaveMessageResponse;
import com.plato.id.core.IdGenerator;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 消息业务服务
 * 
 * 基于 Micro-Batching + TiDB Transaction 的存储策略
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceV2 {

    private final IdGenerator idGenerator;
    private final IdempotentService idempotentService;
    private final MessageMapper messageMapper;
    private final MessageBatchManager batchManager;
    private final MessageCacheService messageCacheService;

    // 业务ID：消息 = 1
    private static final int BID_MESSAGE = 1;

    /**
     * 保存消息
     * 
     * 流程：
     * 1. Redis 原子防重（SETNX）
     * 2. 生成全局唯一 msg_id
     * 3. 进入缓冲队列（Micro-Batching）
     * 4. 等待批量事务完成
     * 5. 返回结果（msg_id + seq_id）
     * 
     * @param request 保存请求
     * @return 保存响应
     */
    public SaveMessageResponse saveMessage(SaveMessageRequest request) {
        Long sessionId = request.getSessionId();
        String clientMessageId = request.getClientMessageId();

        try {
            // ========== Step 1: Redis 原子防重 ==========
            boolean acquired = idempotentService.tryAcquire(sessionId, clientMessageId);
            if (!acquired) {
                log.warn("Duplicate message detected: sessionId={}, clientMessageId={}",
                        sessionId, clientMessageId);
                // TODO: 理想情况下应该返回原来的 msgId 和 seqId，这里简化处理
                throw new IllegalStateException("Duplicate message: " + clientMessageId);
            }

            // ========== Step 2: 生成全局唯一 msg_id ==========
            long msgId = idGenerator.gen(BID_MESSAGE);
            long serverTimeMs = System.currentTimeMillis();

            log.debug("Generated msgId={} for clientMessageId={}", msgId, clientMessageId);

            // ========== Step 3: 构建批处理请求并提交到缓冲队列 ==========
            MessageBatchRequest batchRequest = MessageBatchRequest.builder()
                    .msgId(msgId)
                    .sessionId(sessionId)
                    .senderId(request.getSenderId())
                    .clientMessageId(clientMessageId)
                    .msgType(request.getMsgType())
                    .content(request.getContent().toByteArray())
                    .extra(request.getExtraMap())
                    .serverTimeMs(serverTimeMs)
                    .build();

            // 提交到批处理管理器（返回 Future）
            CompletableFuture<MessageBatchResponse> future = batchManager.submit(batchRequest);

            // ========== Step 4: 等待批量事务完成 ==========
            // 设置超时时间（避免无限等待）
            MessageBatchResponse response = future.get(3, TimeUnit.SECONDS);

            if (!response.getSuccess()) {
                log.error("Batch processing failed for msgId={}: {}", msgId, response.getErrorMessage());
                throw new RuntimeException("Failed to save message: " + response.getErrorMessage());
            }

            // ========== Step 5: 返回成功响应 ==========
            log.info("Message saved successfully: msgId={}, seqId={}, sessionId={}",
                    msgId, response.getSeqId(), sessionId);

            return SaveMessageResponse.newBuilder()
                    .setMsgId(msgId)
                    .setSeqId(response.getSeqId())
                    .setServerTimeMs(serverTimeMs)
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while saving message", e);
            throw new RuntimeException("Message save interrupted", e);

        } catch (Exception e) {
            log.error("Failed to save message: sessionId={}, clientMessageId={}",
                    sessionId, clientMessageId, e);

            // 释放去重锁（异常回滚）
            idempotentService.release(sessionId, clientMessageId);

            throw new RuntimeException("Failed to save message", e);
        }
    }

    /**
     * 拉取消息历史
     * 
     * 流程：
     * 1. 判断是否查询热数据（较新的消息）
     * 2. 尝试从 Redis 公共缓存查询（带 Gap Detection）
     * 3. 如果 Cache Miss 或 Gap Detected，降级查询 TiDB
     * 4. 如果查询 TiDB，异步回填 Redis 缓存
     * 
     * @param request 查询请求
     * @return 消息列表
     */
    public List<Message> getMessages(GetMessagesRequest request) {
        Long sessionId = request.getSessionId();
        Long anchorSeqId = request.getAnchorSeqId();
        int limit = request.getLimit();
        GetMessagesRequest.Direction direction = request.getDirection();

        log.debug("Getting messages: sessionId={}, anchorSeqId={}, direction={}, limit={}",
                sessionId, anchorSeqId, direction, limit);

        // ========== Step 1: 判断是否查询热数据 ==========
        boolean isHistoryQuery = direction == GetMessagesRequest.Direction.BACKWARD && anchorSeqId < 1000;
        // 简化判断：如果是向旧消息拉取且锚点很小，认为是冷数据，直接查 DB

        if (!isHistoryQuery && direction == GetMessagesRequest.Direction.FORWARD) {
            // ========== Step 2: 尝试从 Redis 查询（仅向新消息拉取） ==========
            List<Message> cachedMessages = messageCacheService.getMessagesFromCache(
                    sessionId, anchorSeqId, limit);

            if (cachedMessages != null && !cachedMessages.isEmpty()) {
                log.info("Cache hit: sessionId={}, found {} messages", sessionId, cachedMessages.size());
                return cachedMessages;
            }

            log.debug("Cache miss or gap detected, falling back to DB: sessionId={}", sessionId);
        }

        // ========== Step 3: 降级查询 TiDB ==========
        List<MessageEntity> entities;
        if (direction == GetMessagesRequest.Direction.BACKWARD) {
            // 向旧消息拉取
            long actualAnchor = anchorSeqId == 0 ? Long.MAX_VALUE : anchorSeqId;
            entities = messageMapper.selectBackward(sessionId, actualAnchor, limit);
        } else {
            // 向新消息拉取
            entities = messageMapper.selectForward(sessionId, anchorSeqId, limit);
        }

        List<Message> messages = entities.stream()
                .map(this::toProtoMessage)
                .collect(Collectors.toList());

        log.info("DB query completed: sessionId={}, found {} messages", sessionId, messages.size());

        // ========== Step 4: 异步回填 Redis 缓存（仅热数据） ==========
        if (!isHistoryQuery && !messages.isEmpty()) {
            // 异步回填（不阻塞返回）
            CompletableFuture.runAsync(() -> {
                try {
                    messageCacheService.cacheMessages(messages);
                    log.debug("Async cache refill completed: sessionId={}, {} messages",
                            sessionId, messages.size());
                } catch (Exception e) {
                    log.error("Failed to refill cache: sessionId={}", sessionId, e);
                }
            });
        }

        return messages;
    }

    /**
     * 实体转 Proto 消息
     */
    private Message toProtoMessage(MessageEntity entity) {
        return Message.newBuilder()
                .setMsgId(entity.getMsgId())
                .setSessionId(entity.getSessionId())
                .setSeqId(entity.getSeqId())
                .setSenderId(entity.getSenderId())
                .setMsgType(entity.getMsgType())
                .setContent(ByteString.copyFrom(entity.getContent().getBytes()))
                .setStatus(entity.getStatus())
                .setCreateTimeMs(entity.getCreateTime().toInstant(ZoneOffset.UTC).toEpochMilli())
                .putAllExtra(entity.getExtra() != null ? entity.getExtra() : new java.util.HashMap<>())
                .build();
    }
}
