package com.plato.search.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Redis 配置类
 * 
 * 用于获取用户会话列表（全局搜索场景）
 * 
 * @author hc
 * @since 2025/11/20
 */
@Configuration
@EnableRedisRepositories
public class RedisConfig {
}

