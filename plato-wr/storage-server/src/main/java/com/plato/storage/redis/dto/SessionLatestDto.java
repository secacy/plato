package com.plato.storage.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话全局最新快照 DTO
 * 
 * 对应 Redis Key: session_latest:{session_id}
 * Type: STRING (JSON 序列化)
 * 
 * 职责：存储会话的最新消息快照和最大序列号
 * 特性：所有群成员共享，发消息时只需要更新这一个 Key
 * 
 * @author hc
 * @since 2025/11/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionLatestDto {

    /**
     * 会话 ID
     */
    private Long sessionId;

    /**
     * 当前最大序列号
     */
    private Long maxSeq;

    /**
     * 最新消息时间（毫秒时间戳）
     */
    private Long lastMsgTime;

    /**
     * 最新消息发送者 ID
     */
    private Long lastMsgSenderId;

    /**
     * 最新消息类型（1:文本, 2:图片, 3:视频...）
     */
    private Integer lastMsgType;

    /**
     * 最新消息内容预览（截断）
     */
    private String lastMsgContent;
}
