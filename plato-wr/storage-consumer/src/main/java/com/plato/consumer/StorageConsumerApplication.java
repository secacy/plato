package com.plato.consumer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Storage Consumer 应用启动类
 * 
 * 职责：异步处理消息事件（写扩散、缓存回写）
 * 
 * 核心功能：
 * 1. Job A (写扩散): 更新所有群成员的 Redis 会话列表
 * 2. Job B (读扩散): 将新消息写入 Redis 公共消息缓存
 * 3. Job C (搜索/分析): 同步到搜索和数仓（预留）
 * 
 * @author hc
 * @since 2025/11/20
 */
@SpringBootApplication
@MapperScan("com.plato.consumer.repository.mapper")
public class StorageConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageConsumerApplication.class, args);
    }
}

