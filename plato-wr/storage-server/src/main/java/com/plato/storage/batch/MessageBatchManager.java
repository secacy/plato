package com.plato.storage.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 消息批处理管理器
 * 
 * 核心职责：
 * 1. 管理所有会话的 SessionBuffer
 * 2. 实现"满员发车 + 定时发车"的触发机制
 * 3. 协调批量事务处理
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageBatchManager {

    /**
     * 会话级别的缓冲队列
     * Key: sessionId, Value: SessionBuffer
     */
    private final ConcurrentHashMap<Long, SessionBuffer> sessionBuffers = new ConcurrentHashMap<>();

    /**
     * 批处理执行器（负责执行实际的数据库操作）
     */
    private final MessageBatchProcessor batchProcessor;

    /**
     * 容量阈值：50条
     */
    private static final int CAPACITY_THRESHOLD = 50;

    /**
     * 时间阈值：10ms
     */
    private static final long TIME_THRESHOLD_MS = 10;

    /**
     * 定时扫描线程池
     */
    private ScheduledExecutorService scheduledExecutor;

    /**
     * 批处理工作线程池
     */
    private ExecutorService batchExecutor;

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        // 定时扫描线程（单线程即可）
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "batch-scanner");
            thread.setDaemon(true);
            return thread;
        });

        // 批处理工作线程池（根据CPU核心数调整）
        int processors = Runtime.getRuntime().availableProcessors();
        batchExecutor = new ThreadPoolExecutor(
                processors,
                processors * 2,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                r -> {
                    Thread thread = new Thread(r, "batch-worker");
                    thread.setDaemon(false);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());

        // 启动定时扫描任务（每5ms扫描一次）
        scheduledExecutor.scheduleAtFixedRate(
                this::scanAndFlush,
                TIME_THRESHOLD_MS,
                5,
                TimeUnit.MILLISECONDS);

        log.info("MessageBatchManager initialized: capacityThreshold={}, timeThresholdMs={}",
                CAPACITY_THRESHOLD, TIME_THRESHOLD_MS);
    }

    /**
     * 销毁
     */
    @PreDestroy
    public void destroy() {
        log.info("Shutting down MessageBatchManager...");

        // 先停止接收新任务
        scheduledExecutor.shutdown();

        // 刷新所有剩余数据
        sessionBuffers.values().forEach(this::flushBuffer);

        // 等待所有任务完成
        batchExecutor.shutdown();
        try {
            if (!batchExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                batchExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            batchExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("MessageBatchManager shutdown complete");
    }

    /**
     * 提交消息到批处理队列
     * 
     * @param request 消息请求
     * @return 异步响应 Future
     */
    public CompletableFuture<MessageBatchResponse> submit(MessageBatchRequest request) {
        Long sessionId = request.getSessionId();

        // 创建异步响应
        CompletableFuture<MessageBatchResponse> future = new CompletableFuture<>();
        request.setResponseFuture(future);

        // 获取或创建 SessionBuffer
        SessionBuffer buffer = sessionBuffers.computeIfAbsent(
                sessionId,
                id -> new SessionBuffer(id, CAPACITY_THRESHOLD));

        // 添加到缓冲队列
        boolean shouldFlush = buffer.offer(request);

        // 如果达到容量阈值，立即触发刷新（满员发车）
        if (shouldFlush) {
            log.debug("Capacity threshold reached for session {}, triggering immediate flush", sessionId);
            batchExecutor.submit(() -> flushBuffer(buffer));
        }

        return future;
    }

    /**
     * 定时扫描所有 SessionBuffer，检查是否超时（定时发车）
     */
    private void scanAndFlush() {
        try {
            for (SessionBuffer buffer : sessionBuffers.values()) {
                if (buffer.isTimeThresholdReached(TIME_THRESHOLD_MS)) {
                    log.debug("Time threshold reached for session {}, triggering scheduled flush",
                            buffer.getSessionId());
                    batchExecutor.submit(() -> flushBuffer(buffer));
                }
            }
        } catch (Exception e) {
            log.error("Error during scheduled scan and flush", e);
        }
    }

    /**
     * 刷新指定的 SessionBuffer（执行批量事务）
     * 
     * @param buffer 会话缓冲区
     */
    private void flushBuffer(SessionBuffer buffer) {
        try {
            List<MessageBatchRequest> batch = buffer.drain();
            if (batch.isEmpty()) {
                return;
            }

            log.info("Flushing buffer for session {}, batch size: {}", buffer.getSessionId(), batch.size());

            // 执行批量事务处理
            batchProcessor.processBatch(batch);

        } catch (Exception e) {
            log.error("Error flushing buffer for session {}", buffer.getSessionId(), e);
        }
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("bufferCount", sessionBuffers.size());
        stats.put("totalQueuedMessages", sessionBuffers.values().stream()
                .mapToInt(SessionBuffer::getSize)
                .sum());
        return stats;
    }
}
