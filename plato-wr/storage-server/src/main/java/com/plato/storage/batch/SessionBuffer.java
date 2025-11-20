package com.plato.storage.batch;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 会话级别的缓冲队列
 * 
 * 基于 RingBuffer 思想，实现 Micro-Batching 机制
 * 触发条件："满员发车 + 定时发车"
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
public class SessionBuffer {

    /**
     * 会话ID
     */
    private final Long sessionId;

    /**
     * 缓冲队列（使用 BlockingQueue）
     */
    private final BlockingQueue<MessageBatchRequest> queue;

    /**
     * 容量阈值（默认50条）
     */
    private final int capacityThreshold;

    /**
     * 当前队列大小
     */
    private final AtomicInteger size;

    /**
     * 上次刷新时间
     */
    private volatile long lastFlushTime;

    /**
     * 构造函数
     * 
     * @param sessionId         会话ID
     * @param capacityThreshold 容量阈值
     */
    public SessionBuffer(Long sessionId, int capacityThreshold) {
        this.sessionId = sessionId;
        this.capacityThreshold = capacityThreshold;
        this.queue = new LinkedBlockingQueue<>(capacityThreshold * 2); // 预留2倍容量防止阻塞
        this.size = new AtomicInteger(0);
        this.lastFlushTime = System.currentTimeMillis();
    }

    /**
     * 添加消息到缓冲队列
     * 
     * @param request 消息请求
     * @return 是否达到容量阈值（需要立即触发刷新）
     */
    public boolean offer(MessageBatchRequest request) {
        try {
            queue.put(request);
            int currentSize = size.incrementAndGet();

            // 判断是否达到容量阈值
            if (currentSize >= capacityThreshold) {
                log.debug("SessionBuffer[{}] reached capacity threshold: {}", sessionId, currentSize);
                return true;
            }

            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to offer message to buffer, sessionId={}", sessionId, e);
            return false;
        }
    }

    /**
     * 取出所有待处理的消息（批量）
     * 
     * @return 消息列表
     */
    public List<MessageBatchRequest> drain() {
        List<MessageBatchRequest> batch = new ArrayList<>(size.get());
        queue.drainTo(batch);
        size.set(0);
        lastFlushTime = System.currentTimeMillis();

        log.debug("Drained {} messages from SessionBuffer[{}]", batch.size(), sessionId);
        return batch;
    }

    /**
     * 检查是否超过时间阈值（需要定时刷新）
     * 
     * @param timeThresholdMs 时间阈值（毫秒）
     * @return 是否超时
     */
    public boolean isTimeThresholdReached(long timeThresholdMs) {
        return !isEmpty() && (System.currentTimeMillis() - lastFlushTime >= timeThresholdMs);
    }

    /**
     * 队列是否为空
     */
    public boolean isEmpty() {
        return size.get() == 0;
    }

    /**
     * 获取当前队列大小
     */
    public int getSize() {
        return size.get();
    }

    /**
     * 获取会话ID
     */
    public Long getSessionId() {
        return sessionId;
    }
}
