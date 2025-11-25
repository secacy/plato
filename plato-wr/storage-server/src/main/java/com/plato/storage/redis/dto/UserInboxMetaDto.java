package com.plato.storage.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户会话偏好 DTO
 * 
 * 对应 Redis Key: user_inbox_meta:{user_id}
 * Type: HASH
 * Field: session_id
 * Value: 此 DTO 序列化后的 JSON
 * 
 * 职责：存储用户读到了哪里，以及个人设置（置顶、免打扰）
 * 
 * @author hc
 * @since 2025/11/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInboxMetaDto {

    /**
     * 会话 ID
     */
    private Long sessionId;

    /**
     * 用户已读到的序列号
     */
    private Long readSeq;

    /**
     * 是否置顶
     */
    private Boolean isPinned;

    /**
     * 是否免打扰
     */
    private Boolean isMuted;

    /**
     * 创建时间（毫秒时间戳）
     */
    private Long createTime;
}
