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
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
