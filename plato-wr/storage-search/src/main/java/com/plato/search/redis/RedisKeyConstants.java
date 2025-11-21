package com.plato.search.redis;

/**
 * Redis Key 常量
 * 
 * @author hc
 * @since 2025/11/20
 */
public class RedisKeyConstants {

    /**
     * 用户会话列表
     * 类型：ZSET
     * Score：最后消息时间戳
     * Value：sessionId
     */
    public static String userSessionsKey(Long userId) {
        return "user_sessions:" + userId;
    }
}

