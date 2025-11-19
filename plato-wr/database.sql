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


