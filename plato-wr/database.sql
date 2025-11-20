-- 消息表
CREATE TABLE `message` (
                           `session_id` BIGINT UNSIGNED NOT NULL COMMENT '会话 ID (分片键)',
                           `seq_id` BIGINT UNSIGNED NOT NULL COMMENT '会话内序列ID (保证会话内严格递增)',
                           `msg_id` BIGINT UNSIGNED NOT NULL COMMENT '全局唯一ID',
                           `sender_id` BIGINT UNSIGNED NOT NULL COMMENT '发送者 User ID',
                           `msg_type` TINYINT NOT NULL COMMENT '消息类型',
                           `content`    JSON COMMENT '消息内容 (Payload)',
                           `status` TINYINT NOT NULL DEFAULT 0 COMMENT '消息状态 (0=SENT, 1=RECALLED, 2=DELETED)',
                           `extra`      JSON COMMENT '元数据扩展',
                           `create_time` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
                           PRIMARY KEY (`session_id`, `seq_id`),
                           UNIQUE KEY `uk_msg_id` (`msg_id`)
) COMMENT '消息表';

-- 会话成员表
CREATE TABLE `member` (
                          `session_id` BIGINT NOT NULL,
                          `user_id`    BIGINT NOT NULL,
                          PRIMARY KEY (`session_id`, `user_id`)
) COMMENT '群成员投递名单 (仅用于写扩散投递)';

-- 收件箱表
CREATE TABLE `inbox` (
                         `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                         `session_id` BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
                         `last_read_seq_id` BIGINT NOT NULL DEFAULT 0 COMMENT '已读到的位置',
    -- 这两个字段必须存，因为它们影响"未读数"的计算逻辑 (免打扰不计红点)
                         `is_pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
                         `is_muted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否免打扰',
    -- 用于列表排序 (通常是最新一条消息的时间)
                         `update_time` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
                         PRIMARY KEY (`user_id`, `session_id`),
    -- 索引：用于快速拉取"我的会话列表"并按时间排序
                         INDEX `idx_user_time` (`user_id`, `update_time` DESC)
) COMMENT '用户会话收件箱 (冷数据)';

-- ============================================================================
-- 新架构新增表：会话元数据表
-- 职责：维护每个会话的 max_seq_id，用于 Micro-Batching 中生成消息的 seq_id
-- ============================================================================
CREATE TABLE `t_conversation_meta` (
    `session_id`   BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
    `max_seq`      BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前最大序列号',
    `create_time`  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `update_time`  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (`session_id`)
) COMMENT '会话元数据表 - 用于 seq_id 原子生成';

-- ============================================================================
-- 索引说明
-- ============================================================================
-- message 表:
--   1. PRIMARY KEY (session_id, seq_id) - 主键，支持会话内消息范围查询
--   2. UNIQUE KEY uk_msg_id (msg_id) - 全局唯一消息 ID 索引
--
-- t_conversation_meta 表:
--   1. PRIMARY KEY (session_id) - 主键，用于 SELECT ... FOR UPDATE 锁定
--
-- ============================================================================
-- Micro-Batching 批量处理流程 (新架构)
-- ============================================================================
-- 1. SELECT max_seq FROM t_conversation_meta WHERE session_id = ? FOR UPDATE;
-- 2. 在内存中计算批次内的 seq_id (如: 101, 102, 103, 104, 105)
-- 3. INSERT INTO message (session_id, seq_id, msg_id, ...) VALUES (...), (...), (...);
-- 4. UPDATE t_conversation_meta SET max_seq = ? WHERE session_id = ?;
-- 5. COMMIT;
--
-- 核心优势：
-- - seq_id 生成与消息落库在同一事务中，消除 ID 空洞
-- - 批量插入提升吞吐量（50条/批次）
-- - 事务延迟低（< 10ms/批次）
