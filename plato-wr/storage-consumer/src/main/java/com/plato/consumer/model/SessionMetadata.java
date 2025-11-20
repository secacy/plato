package com.plato.consumer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话元数据（用于 Redis 存储）
 * 
 * 存储在 Redis Hash: session_meta:{user_id}
 * 
 * @author hc
 * @since 2025/11/20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionMetadata {

    /**
     * 未读消息数
     */
    @JsonProperty("unread_count")
    private Integer unreadCount;

    /**
     * 最新一条消息的 seq_id
     */
    @JsonProperty("last_seq_id")
    private Long lastSeqId;

    /**
     * 最新一条消息的预览文本
     */
    @JsonProperty("preview")
    private String preview;

    /**
     * 最新一条消息的时间戳（毫秒）
     */
    @JsonProperty("timestamp")
    private Long timestamp;

    /**
     * 是否置顶
     */
    @JsonProperty("is_pinned")
    private Boolean isPinned;
}

