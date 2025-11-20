package com.plato.storage.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息批量处理响应
 * 
 * @author hc
 * @since 2025/11/20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageBatchResponse {

    /**
     * 全局唯一消息ID
     */
    private Long msgId;

    /**
     * 会话内序列ID
     */
    private Long seqId;

    /**
     * 服务器时间戳
     */
    private Long serverTimeMs;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;

    /**
     * 成功响应构建器
     */
    public static MessageBatchResponse success(Long msgId, Long seqId, Long serverTimeMs) {
        return MessageBatchResponse.builder()
                .msgId(msgId)
                .seqId(seqId)
                .serverTimeMs(serverTimeMs)
                .success(true)
                .build();
    }

    /**
     * 失败响应构建器
     */
    public static MessageBatchResponse failure(Long msgId, String errorMessage) {
        return MessageBatchResponse.builder()
                .msgId(msgId)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
