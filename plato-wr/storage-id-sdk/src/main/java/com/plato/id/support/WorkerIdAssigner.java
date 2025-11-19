package com.plato.id.support;

/**
 * @since 2025/11/19 1:11
 * @className WorkerIdAssigner
 * @author hc
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Worker ID 分配器
 * 职责：利用 Redis 为当前节点分配一个唯一的 Instance ID (0-255)
 */
public class WorkerIdAssigner {

    private static final Logger log = LoggerFactory.getLogger(WorkerIdAssigner.class);
    private static final String WORKER_ID_KEY = "im:infra:worker_ids"; // Redis Key

    private final StringRedisTemplate redisTemplate;

    public WorkerIdAssigner(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 申请 ID
     * 逻辑：利用 Redis INCR 和取模运算
     */
    public int assignWorkerId() {
        try {
            // 1. 自增获取 ID
            Long id = redisTemplate.opsForValue().increment(WORKER_ID_KEY);
            if (id == null) {
                throw new RuntimeException("Redis increment returned null");
            }

            // 2. 取模 (0-255)
            // 假设你的算法支持 8 bit instance id (256个)
            int workerId = (int) (id % 256);

            // 3. (可选) 在 Redis 记录一下 "worker_id:5 -> pod_ip" 以便运维排查
            // redisTemplate.opsForValue().set("im:infra:worker_owner:" + workerId, NetUtil.getLocalHost(), 24, TimeUnit.HOURS);

            log.info("[IdSDK] Assigned Worker ID: {}", workerId);
            return workerId;

        } catch (Exception e) {
            log.error("[IdSDK] Failed to assign worker ID from Redis", e);
            // 降级策略：如果在开发环境，可以随机生成一个；生产环境建议报错停止启动
            throw new RuntimeException("Failed to assign worker ID", e);
        }
    }
}
