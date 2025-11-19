package com.plato.api.redis;

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
     * VALUE: JSON (unread_count, last_msg_preview, is_pinned, is_muted)
     */
    public static String sessionMetaKey(Long userId) {
        return "session_meta:" + userId;
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
     * KEY: idempotent:{client_msg_id}
     * TYPE: String
     * VALUE: msg_id
     * TTL: 5分钟
     */
    public static String idempotentKey(String clientMsgId) {
        return "idempotent:" + clientMsgId;
    }
}
