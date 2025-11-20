package com.plato;

/**
 * 淘汰策略枚举
 *
 * @enumName EvictPolicy
 * @author hc
 * @since 2025/10/22 23:43
 */
public enum EvictPolicy {
    /**
     * 按时间淘汰
     */
    EVICT_BY_TTL,
    /**
     * 按容量淘汰（RingBuffer机制）
     */
    EVICT_BY_CAPACITY,
    /**
     * 永不淘汰
     */
    EVICT_NEVER
}
