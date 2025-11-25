-- ============================================================================
-- 收件箱服务重构 - 数据库迁移脚本 V2
-- 
-- 执行说明：
-- 1. 本脚本向后兼容，新增字段不影响旧代码
-- 2. 可以在线执行，不需要停服
-- 3. 建议在低峰期执行
-- ============================================================================

USE `im_storage`;

-- ============================================================================
-- 1. inbox 表 - 添加 last_msg_time 字段
-- ============================================================================

-- 检查字段是否已存在
SET @col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'im_storage' 
    AND TABLE_NAME = 'inbox' 
    AND COLUMN_NAME = 'last_msg_time'
);

-- 如果不存在，则添加
SET @sql = IF(
    @col_exists = 0,
    'ALTER TABLE inbox ADD COLUMN `last_msg_time` BIGINT NOT NULL DEFAULT 0 COMMENT ''最新消息时间(用于排序)'' AFTER is_muted',
    'SELECT ''Column last_msg_time already exists in inbox table'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================================
-- 2. inbox 表 - 添加优化索引
-- ============================================================================

-- 检查索引是否已存在
SET @idx_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'im_storage' 
    AND TABLE_NAME = 'inbox' 
    AND INDEX_NAME = 'idx_user_view'
);

-- 如果不存在，则添加
SET @sql = IF(
    @idx_exists = 0,
    'ALTER TABLE inbox ADD INDEX `idx_user_view` (`user_id`, `is_pinned` DESC, `last_msg_time` DESC)',
    'SELECT ''Index idx_user_view already exists in inbox table'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================================
-- 3. session_meta 表 - 添加快照相关字段
-- ============================================================================

-- 检查字段是否已存在
SET @col1_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'im_storage' 
    AND TABLE_NAME = 'session_meta' 
    AND COLUMN_NAME = 'last_msg_time'
);

SET @col2_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'im_storage' 
    AND TABLE_NAME = 'session_meta' 
    AND COLUMN_NAME = 'last_msg_sender_id'
);

SET @col3_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'im_storage' 
    AND TABLE_NAME = 'session_meta' 
    AND COLUMN_NAME = 'last_msg_type'
);

SET @col4_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'im_storage' 
    AND TABLE_NAME = 'session_meta' 
    AND COLUMN_NAME = 'last_msg_content'
);

-- 添加 last_msg_time
SET @sql = IF(
    @col1_exists = 0,
    'ALTER TABLE session_meta ADD COLUMN `last_msg_time` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''最新消息时间(用于排序)'' AFTER max_seq_id',
    'SELECT ''Column last_msg_time already exists in session_meta table'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 last_msg_sender_id
SET @sql = IF(
    @col2_exists = 0,
    'ALTER TABLE session_meta ADD COLUMN `last_msg_sender_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''发送者UID(用于展示 "张三: ...")'' AFTER last_msg_time',
    'SELECT ''Column last_msg_sender_id already exists in session_meta table'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 last_msg_type
SET @sql = IF(
    @col3_exists = 0,
    'ALTER TABLE session_meta ADD COLUMN `last_msg_type` INT NOT NULL DEFAULT 1 COMMENT ''消息类型 (1:文本, 2:图片, 3:撤回...)'' AFTER last_msg_sender_id',
    'SELECT ''Column last_msg_type already exists in session_meta table'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 last_msg_content
SET @sql = IF(
    @col4_exists = 0,
    'ALTER TABLE session_meta ADD COLUMN `last_msg_content` VARCHAR(1024) NOT NULL DEFAULT '''' COMMENT ''消息预览内容(截断)'' AFTER last_msg_type',
    'SELECT ''Column last_msg_content already exists in session_meta table'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================================
-- 4. 数据初始化（可选）
-- ============================================================================

-- 说明：如果需要回填历史数据的 last_msg_time，可以执行以下语句
-- 注意：这是一个重量级操作，建议在低峰期分批执行

-- 4.1 回填 inbox 表的 last_msg_time
-- UPDATE inbox i
-- INNER JOIN (
--     SELECT 
--         m.session_id,
--         MAX(m.create_time) as last_msg_time
--     FROM message m
--     GROUP BY m.session_id
-- ) latest ON i.session_id = latest.session_id
-- SET i.last_msg_time = UNIX_TIMESTAMP(latest.last_msg_time) * 1000;

-- 4.2 回填 session_meta 表的快照字段
-- UPDATE session_meta sm
-- INNER JOIN (
--     SELECT 
--         m.session_id,
--         UNIX_TIMESTAMP(m.create_time) * 1000 as last_msg_time,
--         m.sender_id as last_msg_sender_id,
--         m.msg_type as last_msg_type,
--         LEFT(JSON_UNQUOTE(JSON_EXTRACT(m.content, '$.text')), 100) as last_msg_content
--     FROM message m
--     INNER JOIN (
--         SELECT session_id, MAX(seq_id) as max_seq
--         FROM message
--         GROUP BY session_id
--     ) latest ON m.session_id = latest.session_id AND m.seq_id = latest.max_seq
-- ) latest ON sm.session_id = latest.session_id
-- SET 
--     sm.last_msg_time = latest.last_msg_time,
--     sm.last_msg_sender_id = latest.last_msg_sender_id,
--     sm.last_msg_type = latest.last_msg_type,
--     sm.last_msg_content = latest.last_msg_content;

-- ============================================================================
-- 5. 验证迁移结果
-- ============================================================================

-- 验证 inbox 表
SELECT 
    'inbox' as table_name,
    COUNT(*) as total_rows,
    COUNT(last_msg_time) as has_last_msg_time,
    COUNT(*) - COUNT(NULLIF(last_msg_time, 0)) as non_zero_last_msg_time
FROM inbox;

-- 验证 session_meta 表
SELECT 
    'session_meta' as table_name,
    COUNT(*) as total_rows,
    COUNT(last_msg_time) as has_last_msg_time,
    COUNT(last_msg_sender_id) as has_last_msg_sender_id,
    COUNT(last_msg_type) as has_last_msg_type,
    COUNT(last_msg_content) as has_last_msg_content
FROM session_meta;

-- 查看索引
SHOW INDEX FROM inbox WHERE Key_name = 'idx_user_view';

-- ============================================================================
-- 迁移完成！
-- ============================================================================
-- 下一步：
-- 1. 部署新版本代码
-- 2. 观察 Redis 缓存命中率
-- 3. 如需回填历史数据，请取消注释"数据初始化"部分并分批执行
-- ============================================================================

