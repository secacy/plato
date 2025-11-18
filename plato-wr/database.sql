-- 用户信息表
CREATE TABLE `user_info` (
                             `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                             `nickname` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '昵称',
                             `avatar` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '头像URL',
                             `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
                             `updated_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
                             PRIMARY KEY (`user_id`)
) COMMENT '用户信息-高频表';

-- 用户资料表
CREATE TABLE `user_profile` (
                                `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                                `nickname` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '昵称',
                                `avatar` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '头像URL',
                                `signature` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '个性签名',
                                `gender` VARCHAR(32) NOT NULL DEFAULT 'unknown' COMMENT '性别',
                                `location` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '地区',
                                `birthday` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '生日',
                                `extra` JSON COMMENT '扩展字段 (map<string, string>)',
                                `create_time` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '注册时间',
                                `updated_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
                                PRIMARY KEY (`user_id`)
) COMMENT '用户资料-低频表';

-- 用户设置表
CREATE TABLE `user_setting` (
                                `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                                `font_size` VARCHAR(32) NOT NULL DEFAULT 'normal' COMMENT '字体大小',
                                `dark_mode` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '暗黑模式',
                                `receive_notification` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '接收通知',
                                `language` VARCHAR(32) NOT NULL DEFAULT 'en' COMMENT '语言',
                                `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
                                `updated_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
                                PRIMARY KEY (`user_id`)
) COMMENT '用户个人设置表';

-- 用户关系表
CREATE TABLE `user_relationship` (
                                     `user_id` BIGINT UNSIGNED NOT NULL COMMENT '关系发起者/拥有者 (我)',
                                     `peer_id` BIGINT UNSIGNED NOT NULL COMMENT '关系对端 (对方)',
                                     `status` TINYINT NOT NULL COMMENT '关系状态 (FriendStatus: 1=好友, 2=拉黑, 3=被拉黑, 4=已申请, 5=被申请)',
                                     `alias` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '备注名',
                                     `extra` JSON COMMENT '扩展字段 (e.g., 来源)',
                                     `create_time` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '关系建立时间 ',
                                     `updated_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
                                     PRIMARY KEY (`user_id`, `peer_id`)
) COMMENT '用户关系表';

-- 会话元数据表
CREATE TABLE `session` (
                           `session_id` BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
                           `type` TINYINT NOT NULL COMMENT '会话类型 (SessionType: 1=单聊, 2=群聊, 3=直播间, 4=频道)',
                           `name` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '会话名称 (群名)',
                           `cover_image_url` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '会话封面',
                           `owner_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '拥有者/创建者 ID',
                           `extra` JSON COMMENT '扩展字段 (e.g., 群公告)',
                           `create_time` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
                           `updated_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',

    /* --- 为 Inbox 冗余的最新消息 --- */
                           `latest_message_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '最新消息ID (冗余)',
                           `last_msg_sender_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '最新消息发送者ID (冗余)',
                           `last_msg_snippet` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '最新消息摘要 (冗余)',
                           `last_msg_time` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '最新消息时间 (冗余)',

                           PRIMARY KEY (`session_id`)
) COMMENT '会话元数据表';

-- 会话成员表
CREATE TABLE `session_member` (
                                  `session_id` BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
                                  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                                  `role` TINYINT NOT NULL DEFAULT 3 COMMENT '成员角色 (MemberRole: 1=群主, 2=管理员, 3=成员)',
                                  `alias_in_session` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '群内昵称',
                                  `extra` JSON COMMENT '扩展字段 (e.g., 禁言截止时间)',
                                  `join_time` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '加入时间',
                                  `updated_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
                                  PRIMARY KEY (`session_id`, `user_id`)
) COMMENT '会话成员表';

-- 消息表
CREATE TABLE `message` (
                           `session_id` BIGINT UNSIGNED NOT NULL COMMENT '会话 ID (分片键)',
                           `msg_id` BIGINT UNSIGNED NOT NULL COMMENT '全局唯一ID',
                           `sender_id` BIGINT UNSIGNED NOT NULL COMMENT '发送者 User ID',
                           `client_message_id` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '客户端幂等ID',
                           `msg_type` TINYINT NOT NULL COMMENT '消息类型',
                           `body` MEDIUMBLOB COMMENT '消息体',
                           `status` TINYINT NOT NULL DEFAULT 0 COMMENT '消息状态 (0=SENT, 1=RECALLED)',
                           `send_time` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '服务器收到时间 ',
                           PRIMARY KEY (`session_id`, `msg_id`)
) COMMENT '消息表';

-- 用户会话状态表
CREATE TABLE `user_inbox_state` (
                                    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                                    `session_id` BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
                                    `last_read_message_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已读游标',
                                    `is_pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
                                    `is_muted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否免打扰',
                                    `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间 (首次订阅时间)',
                                    `updated_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
                                    PRIMARY KEY (`user_id`, `session_id`)
) COMMENT '用户会话状态表';


