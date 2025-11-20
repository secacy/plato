package com.plato;

import java.time.Duration;

/**
 * 存储策略配置
 *
 * @since 2025/10/22 23:44
 * @className StorageStrategy
 * @author hc
 */
public class StorageStrategy {
    /**
     * 数据存活时间
     */
    public Duration ttl;
    /**
     * 每个消息链表的最大容量
     */
    public int capacity;
    /**
     * 淘汰策略
     */
    public EvictPolicy evictPolicy;

    public StorageStrategy(Duration ttl, int capacity, EvictPolicy evictPolicy) {
        this.ttl = ttl;
        this.capacity = capacity;
        this.evictPolicy = evictPolicy;
    }
}
