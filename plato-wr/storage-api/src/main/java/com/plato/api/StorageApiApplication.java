package com.plato.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Storage API Application
 * 
 * 职责：IM存储系统的gRPC网关
 * - 同步写入：生成ID -> 发送Kafka -> 返回
 * - 同步读取：查询Redis/TiDB -> 返回
 * - 事务性命令：TiDB事务 -> Redis -> Kafka -> 返回
 * 
 * @since 2025/11/16 15:53
 * @author hc
 */
@SpringBootApplication
@EnableKafka
@MapperScan("com.plato.api.repository.mapper")
public class StorageApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageApiApplication.class, args);
    }
}
