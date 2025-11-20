package com.plato.consumer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * 
 * @author hc
 * @since 2025/11/20
 */
@Configuration
public class RedisConfig {

    /**
     * 配置 RedisTemplate（用于 String 类型的值）
     * 
     * @param connectionFactory Redis 连接工厂
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用 String 序列化器
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置 RedisTemplate（用于 byte[] 类型的值，用于 Protobuf）
     * 
     * @param connectionFactory Redis 连接工厂
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, byte[]> byteRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 使用 String 序列化器
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        // Value 使用默认序列化器（byte[]）
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置 ObjectMapper（用于 JSON 序列化）
     * 
     * @return ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 注册 Java 8 时间模块
        mapper.registerModule(new JavaTimeModule());
        
        // 禁用将日期写成时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }
}

