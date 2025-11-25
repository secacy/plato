package com.plato.storage.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户会话收件箱实体
 * 
 * 对应数据库表: inbox
 * 主键: (user_id, session_id)
 * 
 * @author hc
 * @since 2025/11/19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboxEntity {

    /**
     * 用户ID (联合主键之一)
     */
    private Long userId;

    /**
     * 会话ID (联合主键之一)
     */
    private Long sessionId;

    /**
     * 已读到的位置
     */
    private Long lastReadSeqId;

    /**
     * 是否置顶
     */
    private Boolean isPinned;

    /**
     * 是否免打扰
     */
    private Boolean isMuted;

    /**
     * 最新消息时间 (毫秒时间戳，用于排序)
     * 新增字段，避免查询 Message 表
     */
    private Long lastMsgTime;

    /**
     * 更新时间 (用于列表排序)
     */
    private LocalDateTime updateTime;
}
