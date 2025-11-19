package com.plato.api.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 群成员变更事件
 * 
 * @author hc
 * @since 2025/11/19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberEvent {

    /**
     * 事件类型
     */
    private EventType eventType;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 添加的成员ID列表
     */
    private List<Long> addedMemberIds;

    /**
     * 移除的成员ID列表
     */
    private List<Long> removedMemberIds;

    /**
     * 完整成员列表（用于RESET）
     */
    private List<Long> allMemberIds;

    /**
     * 事件时间戳
     */
    private Long timestamp;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        /** 增量更新 */
        PATCH,
        /** 全量重置 */
        RESET
    }
}
