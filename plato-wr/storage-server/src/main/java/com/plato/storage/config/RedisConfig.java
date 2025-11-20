package com.plato.storage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 配置
 * 
 * 提供不同类型的 RedisTemplate
 * 
 * @author hc
 * @since 2025/11/20
 */
@Configuration
public class RedisConfig {

    /**
     * 字节数组类型的 RedisTemplate
     * 用于存储 Protobuf 序列化的二进制数据
     * 
     * @param connectionFactory Redis 连接工厂
     * @return RedisTemplate<String, byte[]>
     */
    @Bean
    public RedisTemplate<String, byte[]> byteArrayRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 使用 String 序列化
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());

        // Value 使用字节数组序列化（不转换）
        template.setValueSerializer(RedisSerializer.byteArray());
        template.setHashValueSerializer(RedisSerializer.byteArray());

        template.afterPropertiesSet();
        return template;
    }
}
