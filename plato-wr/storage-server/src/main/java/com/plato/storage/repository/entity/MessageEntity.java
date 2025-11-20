package com.plato.storage.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 消息表实体
 * 
 * 对应数据库表: message
 * 主键: (session_id, seq_id)
 * 
 * @author hc
 * @since 2025/11/19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEntity {

    /**
     * 会话ID (分片键, 联合主键之一)
     */
    private Long sessionId;

    /**
     * 会话内序列ID (保证会话内严格递增, 联合主键之一)
     */
    private Long seqId;

    /**
     * 全局唯一ID
     */
    private Long msgId;

    /**
     * 发送者 User ID
     */
    private Long senderId;

    /**
     * 消息类型
     */
    private Integer msgType;

    /**
     * 消息内容 (Payload) - JSON类型
     */
    private String content;

    /**
     * 消息状态 (0=SENT, 1=RECALLED, 2=DELETED)
     */
    private Integer status;

    /**
     * 元数据扩展 - JSON类型
     */
    private Map<String, String> extra;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
