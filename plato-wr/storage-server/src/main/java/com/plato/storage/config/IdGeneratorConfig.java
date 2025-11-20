package com.plato.storage.config;

import com.plato.id.core.IdGenerator;
import com.plato.id.support.WorkerIdAssigner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @since 2025/11/19 1:14
 * @className IdGeneratorConfig
 * @author hc
 */
@Configuration
public class IdGeneratorConfig {

    /**
     * 1. 创建 ID 生成器 Bean
     * 启动时自动去 Redis 申请 ID，然后初始化本地算法对象。
     */
    @Bean
    public IdGenerator idGenerator(StringRedisTemplate redisTemplate) {
        // 1. 初始化分配器
        WorkerIdAssigner assigner = new WorkerIdAssigner(redisTemplate);

        // 2. 申请 ID (网络 IO，仅在启动时发生一次)
        int workerId = assigner.assignWorkerId();

        // 3. 创建本地计算核心
        return new IdGenerator(workerId);
    }
}
