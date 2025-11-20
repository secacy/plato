package com.plato.consumer.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话成员表实体
 * 
 * 对应数据库表: member
 * 主键: (session_id, user_id)
 * 
 * @author hc
 * @since 2025/11/20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberEntity {

    /**
     * 会话ID (联合主键之一)
     */
    private Long sessionId;

    /**
     * 用户ID (联合主键之一)
     */
    private Long userId;
}

