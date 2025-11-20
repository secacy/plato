package com.plato.storage.exception;

/**
 * 消息尚未就绪异常
 * 
 * 场景：消息已提交但尚未落地到数据库（可能在批处理队列中）
 * 处理：客户端应稍后重试
 * 
 * @author hc
 * @since 2025/11/20
 */
public class MessageNotReadyException extends RuntimeException {

    private final Long sessionId;
    private final Long seqId;

    public MessageNotReadyException(Long sessionId, Long seqId) {
        super(String.format("Message not ready yet: sessionId=%d, seqId=%d. Please retry later.",
                sessionId, seqId));
        this.sessionId = sessionId;
        this.seqId = seqId;
    }

    public MessageNotReadyException(Long sessionId, Long seqId, String message) {
        super(message);
        this.sessionId = sessionId;
        this.seqId = seqId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getSeqId() {
        return seqId;
    }
}
