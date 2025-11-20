package com.plato.storage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 健康检查
 * 
 * @author hc
 * @since 2025/11/19
 */
@Component("storageHealth")
@RequiredArgsConstructor
public class HealthController implements HealthIndicator {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Health health() {
        // 检查TiDB连接
        boolean tidbOk = checkTiDB();
        // 检查Redis连接
        boolean redisOk = checkRedis();

        if (tidbOk && redisOk) {
            return Health.up()
                    .withDetail("tidb", "ok")
                    .withDetail("redis", "ok")
                    .build();
        } else {
            return Health.down()
                    .withDetail("tidb", tidbOk ? "ok" : "down")
                    .withDetail("redis", redisOk ? "ok" : "down")
                    .build();
        }
    }

    private boolean checkTiDB() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(3);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkRedis() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            return "PONG".equals(pong);
        } catch (Exception e) {
            return false;
        }
    }
}
