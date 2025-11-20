-- ============================================================================
-- Storage Server Database Schema
-- ============================================================================

-- ============================================================================
-- 表 1: t_conversation_meta（会话元数据表）
-- 职责：维护每个会话的 max_seq_id，用于生成消息的 seq_id
-- ============================================================================
CREATE TABLE IF NOT EXISTS t_conversation_meta (
    session_id   BIGINT       NOT NULL COMMENT '会话ID',
    max_seq      BIGINT       NOT NULL DEFAULT 0 COMMENT '当前最大序列号',
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话元数据表';

-- ============================================================================
-- 表 2: message（消息表）
-- 职责：存储所有会话的消息数据
-- 主键：(session_id, seq_id) - 保证会话内消息有序
-- ============================================================================
CREATE TABLE IF NOT EXISTS message (
    session_id  BIGINT       NOT NULL COMMENT '会话ID（分片键）',
    seq_id      BIGINT       NOT NULL COMMENT '会话内序列ID（严格递增）',
    msg_id      BIGINT       NOT NULL COMMENT '全局唯一消息ID',
    sender_id   BIGINT       NOT NULL COMMENT '发送者用户ID',
    msg_type    INT          NOT NULL COMMENT '消息类型（1=文本，2=图片，3=视频，4=文件...）',
    content     JSON         NOT NULL COMMENT '消息内容（JSON格式）',
    status      INT          NOT NULL DEFAULT 0 COMMENT '消息状态（0=正常，1=撤回，2=删除）',
    extra       JSON         DEFAULT NULL COMMENT '扩展字段（@列表、引用消息等）',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    PRIMARY KEY (session_id, seq_id),
    UNIQUE KEY uk_msg_id (msg_id),
    KEY idx_sender_time (sender_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- ============================================================================
-- 索引说明
-- ============================================================================
-- 1. PRIMARY KEY (session_id, seq_id)
--    - 主键，用于快速定位会话内的消息
--    - 支持范围查询：WHERE session_id = ? AND seq_id > ?
--
-- 2. UNIQUE KEY uk_msg_id (msg_id)
--    - 唯一索引，用于通过全局 msg_id 查询消息
--
-- 3. KEY idx_sender_time (sender_id, create_time)
--    - 复合索引，用于查询某个用户发送的消息历史

-- ============================================================================
-- TiDB 特定优化建议
-- ============================================================================
-- 1. 分区策略
--    如果数据量巨大，可以考虑对 message 表按 session_id 进行 HASH 分区
--    例如：PARTITION BY HASH(session_id) PARTITIONS 16;
--
-- 2. 列存储（列式存储引擎 TiFlash）
--    对于历史数据分析场景，可以为 message 表创建 TiFlash 副本
--    ALTER TABLE message SET TIFLASH REPLICA 1;
--
-- 3. Placement Rules
--    对于不同会话类型（如企业会话 vs 个人会话），可以使用 Placement Rules
--    将数据分布到不同的存储节点

-- ============================================================================
-- 性能优化提示
-- ============================================================================
-- 1. 避免全表扫描：始终带上 session_id 条件查询
-- 2. 批量插入：使用 INSERT INTO ... VALUES (...), (...), (...)
-- 3. 事务优化：使用 SELECT ... FOR UPDATE 锁定必要的行
-- 4. 索引选择：利用覆盖索引避免回表

