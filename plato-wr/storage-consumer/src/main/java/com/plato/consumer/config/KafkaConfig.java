package com.plato.consumer.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 配置类
 * 
 * @author hc
 * @since 2025/11/20
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * 配置 Kafka Consumer Factory
     * 
     * @return ConsumerFactory
     */
    @Bean
    public ConsumerFactory<String, byte[]> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        
        // 手动提交偏移量
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        // 从最早的偏移量开始消费
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // 每次拉取的最大记录数
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        
        // 心跳间隔
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        
        // Session 超时时间
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * 配置 Kafka Listener Container Factory
     * 
     * @return ConcurrentKafkaListenerContainerFactory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        
        // 手动确认模式
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        
        // 并发度（从配置文件读取）
        factory.setConcurrency(3);
        
        return factory;
    }
}

