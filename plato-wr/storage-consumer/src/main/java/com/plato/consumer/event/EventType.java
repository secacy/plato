package com.plato.consumer.event;

/**
 * 事件类型枚举
 * 
 * @author hc
 * @since 2025/11/20
 */
public enum EventType {
    /**
     * 新增消息
     */
    INSERT,

    /**
     * 更新消息（撤回/删除）
     */
    UPDATE,

    /**
     * 删除消息（物理删除，一般不使用）
     */
    DELETE
}

