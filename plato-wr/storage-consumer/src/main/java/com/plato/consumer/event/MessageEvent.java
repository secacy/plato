package com.plato.consumer.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 消息事件（从 TiCDC 推送到 Kafka）
 * 
 * TiCDC 监听 message 表的变更，推送到 Kafka Topic: MsgEvents
 * 
 * @author hc
 * @since 2025/11/20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEvent {

    /**
     * 事件类型（INSERT, UPDATE, DELETE）
     */
    private String eventType;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 会话内序列ID
     */
    private Long seqId;

    /**
     * 全局唯一消息ID
     */
    private Long msgId;

    /**
     * 发送者用户ID
     */
    private Long senderId;

    /**
     * 消息类型
     */
    private Integer msgType;

    /**
     * 消息内容（JSON格式）
     */
    private String content;

    /**
     * 消息状态（0=正常，1=撤回，2=删除）
     */
    private Integer status;

    /**
     * 扩展字段
     */
    private Map<String, String> extra;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 事件时间戳（TiCDC 推送时间）
     */
    private Long eventTimestamp;
}

