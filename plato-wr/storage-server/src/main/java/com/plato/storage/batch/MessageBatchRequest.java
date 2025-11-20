package com.plato.storage.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 消息批量处理请求
 * 
 * 包含消息数据和异步响应的 Future
 * 
 * @author hc
 * @since 2025/11/20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageBatchRequest {

    /**
     * 全局唯一消息ID（预生成）
     */
    private Long msgId;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 客户端消息ID（用于幂等）
     */
    private String clientMessageId;

    /**
     * 消息类型
     */
    private Integer msgType;

    /**
     * 消息内容
     */
    private byte[] content;

    /**
     * 扩展字段
     */
    private Map<String, String> extra;

    /**
     * 服务器时间戳
     */
    private Long serverTimeMs;

    /**
     * 异步响应 Future
     */
    private CompletableFuture<MessageBatchResponse> responseFuture;
}
