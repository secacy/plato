package com.plato.storage.redis;

/**
 * Redis Key常量定义
 * 
 * @author hc
 * @since 2025/11/19
 */
public class RedisKeyConstants {

    // ========== SeqID生成相关 ==========

    /**
     * 会话的SeqID计数器
     * KEY: seq:{session_id}
     * TYPE: String (INCR)
     */
    public static String seqKey(Long sessionId) {
        return "seq:" + sessionId;
    }

    // ========== 消息热数据 (Inbox) ==========

    /**
     * 用户的新消息收件箱 (写扩散)
     * KEY: inbox:{user_id}
     * TYPE: Sorted Set
     * SCORE: seq_id (保证有序)
     * VALUE: msg_id
     */
    public static String inboxKey(Long userId) {
        return "inbox:" + userId;
    }

    // ========== 收件箱列表 (新设计) ==========

    /**
     * 用户收件箱列表 (User Timeline) - 仅存 ID 和排序 Score
     * KEY: user_inbox_zset:{user_id}
     * TYPE: ZSET
     * SCORE:
     * - 普通会话: last_msg_time_ms
     * - 置顶会话: last_msg_time_ms + 1_000_000_000_000 (Magic Number)
     * MEMBER: session_id
     * 作用: 仅用于分页拉取 Session ID 列表
     */
    public static String userInboxZSetKey(Long userId) {
        return "user_inbox_zset:" + userId;
    }

    /**
     * 用户会话偏好 (User Session Meta) - 存用户的 Seq 和设置
     * KEY: user_inbox_meta:{user_id}
     * TYPE: HASH
     * FIELD: session_id
     * VALUE (JSON): read_seq、is_muted、is_pinned、create_time
     * 作用: 存储用户读到了哪里，以及是否免打扰
     */
    public static String userInboxMetaKey(Long userId) {
        return "user_inbox_meta:" + userId;
    }

    /**
     * 会话全局最新快照 (Session Global Snapshot) - 存预览和 MaxSeq
     * KEY: session_latest:{session_id}
     * TYPE: STRING (JSON 序列化)
     * VALUE: max_seq、last_msg_content、last_msg_type、last_msg_sender、last_msg_time
     * 作用: 所有群成员共享。发消息时，只需要更新这一个 Key
     */
    public static String sessionLatestKey(Long sessionId) {
        return "session_latest:" + sessionId;
    }

    /**
     * 空对象标记 (用于缓存穿透防护)
     * KEY: null_marker:{type}:{id}
     * TYPE: STRING
     * VALUE: "1"
     * TTL: 60 秒
     */
    public static String nullMarkerKey(String type, Long id) {
        return "null_marker:" + type + ":" + id;
    }

    // ========== 群成员缓存 ==========

    /**
     * 群成员列表缓存
     * KEY: group_members:{session_id}
     * TYPE: Set
     * VALUE: user_id
     */
    public static String groupMembersKey(Long sessionId) {
        return "group_members:" + sessionId;
    }

    // ========== 幂等去重 ==========

    /**
     * 客户端消息ID幂等窗口
     * KEY: dedup:{session_id}:{client_message_id}
     * TYPE: String
     * VALUE: "processing"
     * TTL: 5分钟
     */
    public static String dedupKey(Long sessionId, String clientMessageId) {
        return "dedup:" + sessionId + ":" + clientMessageId;
    }

    // ========== 消息缓存（公共缓存） ==========

    /**
     * 会话的消息索引（公共缓存）
     * KEY: cache:msgs:{session_id}
     * TYPE: ZSET
     * SCORE: seq_id
     * VALUE: msg_id
     * TTL: 3天
     */
    public static String messageCacheKey(Long sessionId) {
        return "cache:msgs:" + sessionId;
    }

    /**
     * 消息元数据（Protobuf 序列化）
     * KEY: msg_meta:{msg_id}
     * TYPE: String (Binary)
     * VALUE: Protobuf 序列化后的二进制数据
     * TTL: 1天
     */
    public static String messageMetaKey(Long msgId) {
        return "msg_meta:" + msgId;
    }
}
