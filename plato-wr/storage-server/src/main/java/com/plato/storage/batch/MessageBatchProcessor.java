package com.plato.storage.batch;

import com.plato.storage.redis.MessageCacheService;
import com.plato.storage.repository.entity.ConversationMetaEntity;
import com.plato.storage.repository.entity.MessageEntity;
import com.plato.storage.repository.mapper.ConversationMetaMapper;
import com.plato.storage.repository.mapper.MessageMapper;
import com.plato.gateway.Message;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 消息批量事务处理器
 * 
 * 核心逻辑：
 * 1. SELECT max_seq FROM t_conversation_meta WHERE session_id = ? FOR UPDATE
 * 2. 在内存中计算每条消息的 seq_id
 * 3. 批量 INSERT INTO t_messages
 * 4. UPDATE t_conversation_meta SET max_seq = ?
 * 5. 异步写入 Redis 缓存
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageBatchProcessor {

    private final MessageMapper messageMapper;
    private final ConversationMetaMapper conversationMetaMapper;
    private final MessageCacheService messageCacheService;

    /**
     * 处理批量消息（事务内）
     * 
     * @param batch 批量消息请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void processBatch(List<MessageBatchRequest> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        // 获取 sessionId（同一批次的 sessionId 相同）
        Long sessionId = batch.get(0).getSessionId();
        int batchSize = batch.size();

        log.info("Processing batch for session {}, size: {}", sessionId, batchSize);

        try {
            // ========== Step 1: 锁定并获取 max_seq ==========
            ConversationMetaEntity meta = conversationMetaMapper.selectForUpdate(sessionId);
            long currentMaxSeq;

            if (meta == null) {
                // 首次写入，初始化元数据
                currentMaxSeq = 0L;
                meta = ConversationMetaEntity.builder()
                        .sessionId(sessionId)
                        .maxSeq(0L)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .build();
                conversationMetaMapper.insertIfNotExists(meta);
                log.info("Initialized conversation meta for session {}", sessionId);
            } else {
                currentMaxSeq = meta.getMaxSeq();
            }

            // ========== Step 2: 分配 seq_id（内存计算） ==========
            List<MessageEntity> entities = new ArrayList<>(batchSize);
            List<Message> protoMessages = new ArrayList<>(batchSize);

            for (int i = 0; i < batchSize; i++) {
                MessageBatchRequest request = batch.get(i);
                long seqId = currentMaxSeq + i + 1; // 严格递增

                // 构建数据库实体
                MessageEntity entity = MessageEntity.builder()
                        .sessionId(sessionId)
                        .seqId(seqId)
                        .msgId(request.getMsgId())
                        .senderId(request.getSenderId())
                        .msgType(request.getMsgType())
                        .content(new String(request.getContent())) // TODO: 根据实际需求决定是否转换
                        .status(0) // 0=SENT
                        .extra(request.getExtra())
                        .createTime(LocalDateTime.now())
                        .build();

                entities.add(entity);

                // 构建 Protobuf 消息（用于缓存）
                Message protoMessage = Message.newBuilder()
                        .setMsgId(request.getMsgId())
                        .setSessionId(sessionId)
                        .setSeqId(seqId)
                        .setSenderId(request.getSenderId())
                        .setMsgType(request.getMsgType())
                        .setContent(ByteString.copyFrom(request.getContent()))
                        .setStatus(0)
                        .setCreateTimeMs(request.getServerTimeMs())
                        .putAllExtra(request.getExtra() != null ? request.getExtra() : new HashMap<>())
                        .build();

                protoMessages.add(protoMessage);

                // 设置响应（用于返回给客户端）
                request.getResponseFuture().complete(
                        MessageBatchResponse.success(request.getMsgId(), seqId, request.getServerTimeMs()));
            }

            // ========== Step 3: 批量插入消息 ==========
            int insertCount = messageMapper.batchInsert(entities);
            log.info("Batch inserted {} messages for session {}", insertCount, sessionId);

            // ========== Step 4: 更新 max_seq ==========
            long newMaxSeq = currentMaxSeq + batchSize;
            conversationMetaMapper.updateMaxSeq(sessionId, newMaxSeq);
            log.info("Updated max_seq for session {} to {}", sessionId, newMaxSeq);

            // ========== Step 5: 异步写入 Redis 缓存 ==========
            // 注意：这里在事务提交后执行，避免阻塞事务
            // 使用 @TransactionalEventListener 或者这里直接调用（因为是非关键路径）
            messageCacheService.cacheMessages(protoMessages);

            log.info("Batch processing completed for session {}, allocated seq_id range: [{}, {}]",
                    sessionId, currentMaxSeq + 1, newMaxSeq);

        } catch (Exception e) {
            log.error("Failed to process batch for session {}", sessionId, e);

            // 设置所有响应为失败
            for (MessageBatchRequest request : batch) {
                request.getResponseFuture().complete(
                        MessageBatchResponse.failure(request.getMsgId(), e.getMessage()));
            }

            throw e; // 抛出异常触发事务回滚
        }
    }
}
