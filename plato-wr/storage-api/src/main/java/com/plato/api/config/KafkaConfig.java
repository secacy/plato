package com.plato.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Kafka配置
 * 
 * @author hc
 * @since 2025/11/19
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    /**
     * Kafka Topic定义
     */
    public static final String TOPIC_MESSAGE_EVENTS = "storage.message.events";
    public static final String TOPIC_GROUP_EVENTS = "storage.group.events";
    public static final String TOPIC_INBOX_EVENTS = "storage.inbox.events";
}
