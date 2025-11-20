package com.plato.consumer.redis;

/**
 * Redis Key 常量定义
 * 
 * @author hc
 * @since 2025/11/20
 */
public class RedisKeyConstants {

    // ========== 会话列表 ==========

    /**
     * 用户的会话列表 (按时间排序)
     * KEY: user_sessions:{user_id}
     * TYPE: Sorted Set
     * SCORE: last_msg_time_ms
     * VALUE: session_id
     */
    public static String userSessionsKey(Long userId) {
        return "user_sessions:" + userId;
    }

    /**
     * 会话的元数据 (预览信息)
     * KEY: session_meta:{user_id}
     * TYPE: Hash
     * FIELD: session_id
     * VALUE: JSON (unread_count, last_seq_id, last_msg_preview, timestamp)
     */
    public static String sessionMetaKey(Long userId) {
        return "session_meta:" + userId;
    }

    // ========== 群成员缓存 ==========

    /**
     * 群成员列表缓存
     * KEY: session_members:{session_id}
     * TYPE: Set
     * VALUE: user_id
     */
    public static String sessionMembersKey(Long sessionId) {
        return "session_members:" + sessionId;
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

    // ========== 未读计数 ==========

    /**
     * 用户的未读计数
     * KEY: unread_count:{user_id}
     * TYPE: HASH
     * FIELD: session_id
     * VALUE: count
     */
    public static String unreadCountKey(Long userId) {
        return "unread_count:" + userId;
    }
}

