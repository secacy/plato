package com.plato.id;

import com.plato.id.core.IdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @since 2025/11/18 12:51
 * @className IdServiceApplication
 * @author hc
 */
@SpringBootApplication
public class IdServiceApplication {

    public static void main(String[] args) {
        // Spring Boot 启动
        // 它会自动找到 @GrpcService 并启动 gRPC 服务器
        SpringApplication.run(IdServiceApplication.class, args);
    }

    /**
     * 创建 IdGenerator 的单一实例 (Singleton Bean)
     * * @param instanceId 从 application.yml 注入
     * @return 供整个应用使用的 IdGenerator 实例
     */
    @Bean
    public IdGenerator idGenerator(
            @Value("${id-service.instance-id}") int instanceId) {

        System.out.println("----------------------------------------------------");
        System.out.println("Initializing IdGenerator with Instance ID: " + instanceId);
        System.out.println("----------------------------------------------------");

        if (instanceId < 0 || instanceId > 255) {
            throw new IllegalArgumentException(
                    "Instance ID ('id-service.instance-id') 必须在 0 到 255 之间"
            );
        }

        // 使用配置中的 instanceId 初始化
        return new IdGenerator((byte) instanceId);
    }
}