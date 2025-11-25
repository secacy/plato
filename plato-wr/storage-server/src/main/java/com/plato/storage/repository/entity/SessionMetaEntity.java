package com.plato.storage.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话元数据表实体
 * 
 * 对应数据库表: session_meta
 * 主键: session_id
 * 
 * 职责：维护会话的 max_seq_id（最大序列号）
 * 
 * @author hc
 * @since 2025/11/20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionMetaEntity {

    /**
     * 会话ID（主键）
     */
    private Long sessionId;

    /**
     * 当前会话的最大序列ID
     * 用于生成下一个 seq_id = max_seq + 1
     */
    private Long maxSeq;

    /**
     * 最新消息时间 (毫秒时间戳，用于排序)
     */
    private Long lastMsgTime;

    /**
     * 最新消息发送者ID (用于展示 "张三: ...")
     */
    private Long lastMsgSenderId;

    /**
     * 最新消息类型 (1:文本, 2:图片, 3:撤回...)
     */
    private Integer lastMsgType;

    /**
     * 最新消息内容 (截断的预览文本)
     */
    private String lastMsgContent;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
