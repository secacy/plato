package com.plato.consumer.listener;

import com.plato.consumer.event.EventParser;
import com.plato.consumer.event.EventType;
import com.plato.consumer.event.MessageEvent;
import com.plato.consumer.service.ReadFanoutService;
import com.plato.consumer.service.WriteFanoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 消息事件监听器
 * 
 * 职责：监听 Kafka Topic (MsgEvents)，处理消息变更事件
 * 
 * TiCDC 会监听 message 表的变更（INSERT/UPDATE/DELETE），
 * 并将变更事件推送到 Kafka Topic: MsgEvents
 * 
 * Consumer 的职责：
 * 1. 解析事件
 * 2. 执行写扩散（Job A）
 * 3. 执行读扩散（Job B）
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventListener {

    private final EventParser eventParser;
    private final WriteFanoutService writeFanoutService;
    private final ReadFanoutService readFanoutService;

    /**
     * 监听消息事件
     * 
     * Consumer Group: storage-consumer-message-group
     * Topic: MsgEvents
     * 
     * @param record 消息记录
     * @param ack    手动确认
     */
    @KafkaListener(
            topics = "${consumer.kafka.topic:MsgEvents}",
            groupId = "storage-consumer-message-group",
            concurrency = "${spring.kafka.listener.concurrency:3}"
    )
    public void onMessage(ConsumerRecord<String, byte[]> record, Acknowledgment ack) {
        try {
            log.debug("Received message event: topic={}, partition={}, offset={}", 
                      record.topic(), record.partition(), record.offset());

            // ========== Step 1: 解析事件 ==========
            MessageEvent event = parseEvent(record.value());

            if (event == null) {
                log.error("Failed to parse message event, skipping");
                ack.acknowledge(); // 确认消费（避免阻塞）
                return;
            }

            log.info("Parsed message event: type={}, sessionId={}, seqId={}, msgId={}", 
                     event.getEventType(), event.getSessionId(), event.getSeqId(), event.getMsgId());

            // ========== Step 2: 根据事件类型处理 ==========
            String eventType = event.getEventType();

            if (EventType.INSERT.name().equals(eventType)) {
                // 新消息插入
                handleInsertEvent(event);

            } else if (EventType.UPDATE.name().equals(eventType)) {
                // 消息状态更新（撤回/删除）
                handleUpdateEvent(event);

            } else if (EventType.DELETE.name().equals(eventType)) {
                // 消息物理删除（一般不使用）
                handleDeleteEvent(event);

            } else {
                log.warn("Unknown event type: {}", eventType);
            }

            // ========== Step 3: 手动确认消费 ==========
            ack.acknowledge();

            log.debug("Message event processed successfully: msgId={}", event.getMsgId());

        } catch (Exception e) {
            log.error("Failed to process message event: record={}", record, e);
            
            // 注意：这里不确认消费（ack），会导致该消息重新投递
            // 如果是业务逻辑错误且不可恢复，可以考虑确认消费并记录到死信队列
            // 这里采用简单策略：不确认，等待重试
        }
    }

    /**
     * 处理消息插入事件（新消息）
     * 
     * @param event 消息事件
     */
    private void handleInsertEvent(MessageEvent event) {
        try {
            log.debug("Handling INSERT event: msgId={}", event.getMsgId());

            // Job A: 写扩散（更新所有成员的会话列表）
            writeFanoutService.fanout(event);

            // Job B: 读扩散（缓存消息到 Redis 公共缓存）
            readFanoutService.fanout(event);

            log.info("INSERT event handled: msgId={}", event.getMsgId());

        } catch (Exception e) {
            log.error("Failed to handle INSERT event: event={}", event, e);
            throw e; // 抛出异常，不确认消费
        }
    }

    /**
     * 处理消息更新事件（撤回/删除）
     * 
     * @param event 消息事件
     */
    private void handleUpdateEvent(MessageEvent event) {
        try {
            log.debug("Handling UPDATE event: msgId={}, status={}", 
                      event.getMsgId(), event.getStatus());

            // 只需要更新缓存中的消息状态
            readFanoutService.handleStatusUpdate(event);

            // 可选：如果需要更新会话列表的预览（比如显示"消息已撤回"）
            // 可以在这里调用 writeFanoutService 更新预览文本

            log.info("UPDATE event handled: msgId={}, status={}", 
                     event.getMsgId(), event.getStatus());

        } catch (Exception e) {
            log.error("Failed to handle UPDATE event: event={}", event, e);
            throw e; // 抛出异常，不确认消费
        }
    }

    /**
     * 处理消息删除事件（物理删除，一般不使用）
     * 
     * @param event 消息事件
     */
    private void handleDeleteEvent(MessageEvent event) {
        try {
            log.debug("Handling DELETE event: msgId={}", event.getMsgId());

            // 从缓存中删除消息
            readFanoutService.handleStatusUpdate(event);

            log.info("DELETE event handled: msgId={}", event.getMsgId());

        } catch (Exception e) {
            log.error("Failed to handle DELETE event: event={}", event, e);
            throw e; // 抛出异常，不确认消费
        }
    }

    /**
     * 解析消息事件
     * 
     * 使用 EventParser 解析 TiCDC Canal JSON 格式的事件
     * 
     * @param eventBytes 事件字节数组
     * @return 消息事件
     */
    private MessageEvent parseEvent(byte[] eventBytes) {
        return eventParser.parse(eventBytes);
    }
}

