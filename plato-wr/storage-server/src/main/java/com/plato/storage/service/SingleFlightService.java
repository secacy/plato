package com.plato.storage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * SingleFlight 服务
 * 
 * 职责：防止缓存击穿 - 归并同一时刻的重复回源请求
 * 
 * 场景：当某个热点 Key (如 session_latest:{session_id}) 失效时，
 * 可能有成百上千个请求同时查询该 Key，导致大量请求同时打到数据库。
 * 
 * 解决方案：
 * 使用 ConcurrentHashMap + CompletableFuture 实现类似 Go singleflight 的机制。
 * 同一时刻，针对同一个 Key 的回源请求，应用层只发一个查询到数据库，
 * 其他请求等待这一个结果共享。
 * 
 * @author hc
 * @since 2025/11/25
 */
@Slf4j
@Component
public class SingleFlightService {

    /**
     * 用于存放正在进行的回源任务
     * Key: 唯一标识（例如 "sess_snap:1001"）
     * Value: 正在执行的异步任务
     */
    private final ConcurrentHashMap<String, CompletableFuture<Object>> taskCache = new ConcurrentHashMap<>();

    /**
     * 执行回源操作（带 SingleFlight 保护）
     * 
     * @param key      唯一标识，例如 "sess_snap:1001"
     * @param supplier 真正的回源逻辑（查 DB + 写 Redis）
     * @param <T>      返回类型
     * @return 回源结果
     */
    @SuppressWarnings("unchecked")
    public <T> T execute(String key, Supplier<T> supplier) {
        CompletableFuture<Object> future = taskCache.get(key);

        if (future != null) {
            // A. 如果已经有任务在运行，直接等待它的结果（复用）
            try {
                log.debug("SingleFlight: waiting for existing task, key={}", key);
                return (T) future.join();
            } catch (Exception e) {
                // 如果等待失败，降级为自己查
                log.warn("SingleFlight: waiting failed, fallback to self query, key={}", key, e);
                return supplier.get();
            }
        }

        // B. 自己发起任务
        CompletableFuture<Object> newFuture = new CompletableFuture<>();
        // 原子操作：尝试放入 Map
        CompletableFuture<Object> existingFuture = taskCache.putIfAbsent(key, newFuture);

        if (existingFuture != null) {
            // 刚巧有人抢先了一步，等待那个人的结果
            log.debug("SingleFlight: someone else started, waiting, key={}", key);
            return (T) existingFuture.join();
        }

        // C. 我是第一个，执行 DB 查询
        try {
            log.debug("SingleFlight: I'm the first, executing, key={}", key);
            T result = supplier.get();
            newFuture.complete(result);
            return result;
        } catch (Exception e) {
            log.error("SingleFlight: execution failed, key={}", key, e);
            newFuture.completeExceptionally(e);
            throw e;
        } finally {
            // 关键：任务结束，移除 Key，允许下一次新的请求进来
            taskCache.remove(key);
            log.debug("SingleFlight: task completed and removed, key={}", key);
        }
    }
}
