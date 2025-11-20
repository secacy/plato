package com.plato.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 批处理配置
 * 
 * @author hc
 * @since 2025/11/20
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "storage.batch")
public class BatchConfig {

    /**
     * 容量阈值：多少条消息触发批处理
     * 默认：50 条
     */
    private int capacityThreshold = 50;

    /**
     * 时间阈值：多少毫秒触发批处理
     * 默认：10ms
     */
    private long timeThresholdMs = 10;

    /**
     * 定时扫描间隔
     * 默认：5ms
     */
    private long scanIntervalMs = 5;

    /**
     * 工作线程池核心线程数
     * 默认：CPU 核心数
     */
    private int corePoolSize = Runtime.getRuntime().availableProcessors();

    /**
     * 工作线程池最大线程数
     * 默认：CPU 核心数 * 2
     */
    private int maxPoolSize = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 缓存消息列表的最大条数
     * 默认：1000 条
     */
    private int maxCachedMessages = 1000;

    /**
     * 消息索引缓存 TTL（天）
     * 默认：3 天
     */
    private long messageCacheTtlDays = 3;

    /**
     * 消息元数据缓存 TTL（天）
     * 默认：1 天
     */
    private long messageMetaTtlDays = 1;
}
