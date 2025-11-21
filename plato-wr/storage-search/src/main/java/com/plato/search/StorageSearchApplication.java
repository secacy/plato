package com.plato.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Storage Search Server
 * 
 * 职责：提供消息搜索服务
 * 
 * 架构隔离：
 * - 独立部署，与 storage-server 物理隔离
 * - 避免搜索查询影响核心发消息接口
 * - 线程池隔离，资源隔离，故障隔离
 * 
 * 依赖：
 * - Elasticsearch: 全文搜索引擎
 * - Redis: 获取用户会话列表（全局搜索场景）
 * 
 * @author hc
 * @since 2025/11/20
 */
@SpringBootApplication
public class StorageSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageSearchApplication.class, args);
        System.out.println("""
                
                ===================================================
                  Storage Search Server Started Successfully!
                ===================================================
                  gRPC Port: 9091
                  Service: StorageSearchService
                  Method: SearchMessages
                ===================================================
                """);
    }
}

