package com.plato.storage.service;

import com.plato.storage.batch.MessageBatchManager;
import com.plato.storage.batch.MessageBatchRequest;
import com.plato.storage.batch.MessageBatchResponse;
import com.plato.storage.exception.MessageNotReadyException;
import com.plato.storage.redis.IdempotentService;
import com.plato.storage.redis.MessageCacheService;
import com.plato.storage.repository.entity.MessageEntity;
import com.plato.storage.repository.mapper.MessageMapper;
import com.plato.gateway.GetMessagesRequest;
import com.plato.gateway.Message;
import com.plato.gateway.SaveMessageRequest;
import com.plato.gateway.SaveMessageResponse;
import com.plato.gateway.UpdateMessageStatusRequest;
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
public class MessageService {

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

        // ========== Step 1: Redis 原子防重 ==========
        boolean acquired = idempotentService.tryAcquire(sessionId, clientMessageId);
        if (!acquired) {
            log.warn("Duplicate message detected: sessionId={}, clientMessageId={}",
                    sessionId, clientMessageId);

            // 幂等处理：返回已存在的消息信息
            // 注意：handleDuplicateMessage 抛出的异常不需要释放锁（锁不属于当前请求）
            return handleDuplicateMessage(sessionId, clientMessageId);
        }

        // 以下逻辑只在成功获取锁后执行
        try {
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

            // ========== Step 5: 保存幂等信息 ==========
            idempotentService.saveMessageInfo(sessionId, clientMessageId, msgId, response.getSeqId());

            // ========== Step 6: 返回成功响应 ==========
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
            // 释放去重锁（异常回滚）
            idempotentService.release(sessionId, clientMessageId);
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
     * 处理重复消息（幂等返回）
     * 
     * 场景说明：
     * - Redis SETNX 失败，说明在幂等窗口内（5分钟）收到了重复请求
     * - 这是"至少一次投递"语义下的正常情况（网络重试、客户端重发等）
     * 
     * 处理策略：
     * 1. 尝试从 Redis 获取已保存的消息信息（msgId:seqId）
     * 2. 如果获取成功，返回原始响应（幂等成功）
     * 3. 如果 Redis 中的值还是 "processing"，说明原请求正在处理中，稍后重试
     * 4. 如果 Redis 值异常，记录错误
     * 
     * 注意：clientMessageId 不存储在数据库中，完全依赖 Redis 幂等窗口
     * 
     * @param sessionId       会话ID
     * @param clientMessageId 客户端消息ID
     * @return 已存在的消息响应
     */
    private SaveMessageResponse handleDuplicateMessage(Long sessionId, String clientMessageId) {
        // 从 Redis 获取消息信息
        String messageInfo = idempotentService.getMessageInfo(sessionId, clientMessageId);

        if (messageInfo == null) {
            // 理论上不应该发生：tryAcquire 返回 false，但 key 不存在
            log.error("Inconsistent state: tryAcquire failed but key not found, sessionId={}, clientMessageId={}",
                    sessionId, clientMessageId);
            throw new IllegalStateException("Duplicate message detected but no record found");
        }

        if ("processing".equals(messageInfo)) {
            // 原请求还在处理中（批处理队列或事务执行中）
            log.info("Duplicate request while original is still processing: sessionId={}, clientMessageId={}",
                    sessionId, clientMessageId);
            throw new IllegalStateException("Message is being processed, please retry later: " + clientMessageId);
        }

        if (messageInfo.contains(":")) {
            // Redis 命中：格式为 "msgId:seqId"
            String[] parts = messageInfo.split(":");
            try {
                long msgId = Long.parseLong(parts[0]);
                long seqId = Long.parseLong(parts[1]);

                log.info(
                        "Idempotent return (message already saved): msgId={}, seqId={}, sessionId={}, clientMessageId={}",
                        msgId, seqId, sessionId, clientMessageId);

                return SaveMessageResponse.newBuilder()
                        .setMsgId(msgId)
                        .setSeqId(seqId)
                        .setServerTimeMs(System.currentTimeMillis())
                        .build();
            } catch (NumberFormatException e) {
                log.error("Invalid message info format in Redis: {}", messageInfo, e);
                throw new IllegalStateException("Invalid message info in cache");
            }
        }

        // 未知的 Redis 值格式
        log.error("Unknown message info format in Redis: {}", messageInfo);
        throw new IllegalStateException("Invalid message state in cache");
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
     * 更新消息状态（撤回/删除）
     * 
     * 容错机制：
     * 1. 如果消息不存在，可能是还在批处理队列中（尚未落地）
     * 2. 短暂等待后重试一次查询
     * 3. 如果仍未找到，抛出 MessageNotReadyException（让客户端重试）
     * 
     * @param request 更新请求
     */
    public void updateMessageStatus(UpdateMessageStatusRequest request) {
        Long sessionId = request.getSessionId();
        Long seqId = request.getSeqId();
        Integer newStatus = request.getNewStatus();

        log.info("Updating message status: sessionId={}, seqId={}, newStatus={}",
                sessionId, seqId, newStatus);

        // ========== Step 1: 尝试更新数据库 ==========
        int affected = messageMapper.updateStatus(sessionId, seqId, newStatus);

        if (affected == 0) {
            log.warn("Message not found on first attempt: sessionId={}, seqId={}", sessionId, seqId);

            // ========== Step 2: 容错机制 - 短暂等待后重试查询 ==========
            // 原因：消息可能在批处理队列中，或刚完成批处理但事务尚未提交
            try {
                Thread.sleep(100); // 等待 100ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Sleep interrupted during message retry", e);
            }

            // 重新查询消息是否存在
            MessageEntity entity = messageMapper.selectByPrimaryKey(sessionId, seqId);
            if (entity != null) {
                // 消息已落地，再次尝试更新
                log.info("Message found on retry, attempting update again: sessionId={}, seqId={}",
                        sessionId, seqId);
                affected = messageMapper.updateStatus(sessionId, seqId, newStatus);

                if (affected == 0) {
                    // 理论上不应该发生（消息存在但更新失败）
                    log.error("Message exists but update failed: sessionId={}, seqId={}", sessionId, seqId);
                    throw new IllegalStateException("Message exists but cannot be updated");
                }
            } else {
                // ========== Step 3: 消息仍未找到，可能还在批处理队列中 ==========
                log.warn("Message still not found after retry, likely in batch queue: sessionId={}, seqId={}",
                        sessionId, seqId);
                throw new MessageNotReadyException(sessionId, seqId);
            }
        }

        // TODO: 发送事件到 Kafka（供 Consumer 更新缓存）
        // TODO: 更新 Redis 缓存

        log.info("Message status updated successfully: sessionId={}, seqId={}", sessionId, seqId);
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
