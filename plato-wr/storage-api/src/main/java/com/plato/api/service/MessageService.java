package com.plato.api.service;

import com.plato.api.event.MessageEvent;
import com.plato.api.producer.StorageEventProducer;
import com.plato.api.redis.IdempotentService;
import com.plato.api.redis.SeqIdGenerator;
import com.plato.api.repository.entity.MessageEntity;
import com.plato.api.repository.mapper.MessageMapper;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息业务服务
 * 
 * 职责：处理消息的保存、查询和状态更新
 * 
 * @author hc
 * @since 2025/11/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final IdGenerator idGenerator;
    private final SeqIdGenerator seqIdGenerator;
    private final IdempotentService idempotentService;
    private final MessageMapper messageMapper;
    private final StorageEventProducer eventProducer;

    // 业务ID：消息 = 1
    private static final int BID_MESSAGE = 1;

    /**
     * 保存消息（异步模式）
     * 
     * 流程：
     * 1. 幂等检查
     * 2. 生成msgId和seqId
     * 3. 发送Kafka事件
     * 4. 立即返回
     * 
     * @param request 保存请求
     * @return 保存响应
     */
    public SaveMessageResponse saveMessage(SaveMessageRequest request) {
        // 1. 幂等检查
        Long existingMsgId = idempotentService.checkIdempotent(request.getClientMessageId());
        if (existingMsgId != null) {
            log.info("Duplicate message detected, clientMsgId={}, msgId={}",
                    request.getClientMessageId(), existingMsgId);
            // TODO: 需要返回原来的seqId，这里需要额外查询或在幂等记录中存储
            return SaveMessageResponse.newBuilder()
                    .setMsgId(existingMsgId)
                    .setSeqId(0) // 占位，实际应该返回原来的seqId
                    .setServerTimeMs(System.currentTimeMillis())
                    .build();
        }

        // 2. 生成全局唯一的msgId
        long msgId = idGenerator.gen(BID_MESSAGE);

        // 3. 生成会话内递增的seqId
        long seqId = seqIdGenerator.generateSeqId(request.getSessionId());

        // 4. 记录幂等
        idempotentService.recordIdempotent(request.getClientMessageId(), msgId);

        // 5. 构建事件并发送到Kafka
        long serverTimeMs = System.currentTimeMillis();
        MessageEvent event = MessageEvent.builder()
                .eventType(MessageEvent.EventType.SAVE)
                .msgId(msgId)
                .seqId(seqId)
                .sessionId(request.getSessionId())
                .senderId(request.getSenderId())
                .clientMessageId(request.getClientMessageId())
                .msgType(request.getMsgType())
                .content(request.getContent().toByteArray())
                .extra(new HashMap<>(request.getExtraMap()))
                .serverTimeMs(serverTimeMs)
                .build();

        eventProducer.sendMessageEvent(event);

        log.info("Message saved to Kafka, msgId={}, seqId={}, sessionId={}",
                msgId, seqId, request.getSessionId());

        // 6. 立即返回（不等待Kafka消费）
        return SaveMessageResponse.newBuilder()
                .setMsgId(msgId)
                .setSeqId(seqId)
                .setServerTimeMs(serverTimeMs)
                .build();
    }

    /**
     * 拉取消息历史（同步模式）
     * 
     * 流程：
     * 1. 判断是否在热数据区（Redis）
     * 2. 查询TiDB
     * 3. 返回数据
     * 
     * @param request 查询请求
     * @return 消息列表
     */
    public List<Message> getMessages(GetMessagesRequest request) {
        List<MessageEntity> entities;

        // 根据方向查询
        if (request.getDirection() == GetMessagesRequest.Direction.BACKWARD) {
            // 向旧消息拉取
            long anchorSeqId = request.getAnchorSeqId() == 0
                    ? Long.MAX_VALUE
                    : request.getAnchorSeqId();
            entities = messageMapper.selectBackward(
                    request.getSessionId(),
                    anchorSeqId,
                    request.getLimit());
        } else {
            // 向新消息拉取
            entities = messageMapper.selectForward(
                    request.getSessionId(),
                    request.getAnchorSeqId(),
                    request.getLimit());
        }

        // 转换为Proto消息
        return entities.stream()
                .map(this::toProtoMessage)
                .collect(Collectors.toList());
    }

    /**
     * 更新消息状态
     * 
     * @param request 更新请求
     */
    public void updateMessageStatus(UpdateMessageStatusRequest request) {
        // 1. 更新数据库
        messageMapper.updateStatus(
                request.getSessionId(),
                request.getSeqId(),
                request.getNewStatus());

        // 2. 发送事件（供Consumer更新缓存和搜索索引）
        MessageEvent event = MessageEvent.builder()
                .eventType(MessageEvent.EventType.UPDATE_STATUS)
                .sessionId(request.getSessionId())
                .seqId(request.getSeqId())
                .msgType(request.getNewStatus())
                .extra(new HashMap<>(request.getExtensionMap()))
                .serverTimeMs(System.currentTimeMillis())
                .build();

        eventProducer.sendMessageEvent(event);

        log.info("Message status updated, sessionId={}, seqId={}, newStatus={}",
                request.getSessionId(), request.getSeqId(), request.getNewStatus());
    }

    /**
     * 实体转Proto消息
     */
    private Message toProtoMessage(MessageEntity entity) {
        return Message.newBuilder()
                .setMsgId(entity.getMsgId())
                .setSessionId(entity.getSessionId())
                .setSeqId(entity.getSeqId())
                .setSenderId(entity.getSenderId())
                .setMsgType(entity.getMsgType())
                .setContent(ByteString.copyFromUtf8(entity.getContent()))
                .setStatus(entity.getStatus())
                .setCreateTimeMs(entity.getCreateTime().toInstant(ZoneOffset.UTC).toEpochMilli())
                .putAllExtra(entity.getExtra())
                .build();
    }
}
