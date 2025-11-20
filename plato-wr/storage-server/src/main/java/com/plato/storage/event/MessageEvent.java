package com.plato.storage.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 消息事件 - 发送到Kafka供Consumer处理
 * 
 * Consumer会根据此事件：
 * 1. 写入TiDB message表
 * 2. 写入Redis inbox (写扩散)
 * 3. 更新Redis session列表
 * 
 * @author hc
 * @since 2025/11/19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEvent {

    /**
     * 事件类型
     */
    private EventType eventType;

    /**
     * 消息ID (已生成)
     */
    private Long msgId;

    /**
     * 序列ID (已生成)
     */
    private Long seqId;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 客户端消息ID (幂等去重)
     */
    private String clientMessageId;

    /**
     * 消息类型
     */
    private Integer msgType;

    /**
     * 消息内容 (JSON bytes)
     */
    private byte[] content;

    /**
     * 扩展字段
     */
    private Map<String, String> extra;

    /**
     * 服务器时间戳 (毫秒)
     */
    private Long serverTimeMs;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        /** 新消息保存 */
        SAVE,
        /** 消息状态更新 */
        UPDATE_STATUS,
        /** 管理员撤回 */
        ADMIN_RECALL
    }
}
