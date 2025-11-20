package com.plato.storage.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plato.storage.config.KafkaConfig;
import com.plato.storage.event.GroupMemberEvent;
import com.plato.storage.event.MessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * 存储事件生产者
 * 
 * 职责：将事件发送到Kafka，供storage-consumer异步处理
 * 
 * @author hc
 * @since 2025/11/19
 */
@Slf4j
@Service
public class StorageEventProducer {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public StorageEventProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 发送消息事件
     * 
     * @param event 消息事件
     */
    public void sendMessageEvent(MessageEvent event) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(event);
            // Key使用sessionId保证同一会话的消息有序
            String key = String.valueOf(event.getSessionId());

            kafkaTemplate.send(KafkaConfig.TOPIC_MESSAGE_EVENTS, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send message event, msgId={}, sessionId={}",
                                    event.getMsgId(), event.getSessionId(), ex);
                            // TODO: 可以考虑重试或记录到死信队列
                        } else {
                            log.debug("Message event sent successfully, msgId={}, seqId={}",
                                    event.getMsgId(), event.getSeqId());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message event", e);
            throw new RuntimeException("Event serialization failed", e);
        }
    }

    /**
     * 发送群成员变更事件
     * 
     * @param event 群成员事件
     */
    public void sendGroupMemberEvent(GroupMemberEvent event) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(event);
            String key = String.valueOf(event.getSessionId());

            kafkaTemplate.send(KafkaConfig.TOPIC_GROUP_EVENTS, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send group member event, sessionId={}",
                                    event.getSessionId(), ex);
                        } else {
                            log.debug("Group member event sent successfully, sessionId={}, type={}",
                                    event.getSessionId(), event.getEventType());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize group member event", e);
            throw new RuntimeException("Event serialization failed", e);
        }
    }
}
