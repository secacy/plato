# Storage Consumer 快速启动指南

## 前置条件

确保以下服务已启动：

1. **Kafka** (localhost:9092)
2. **Redis** (localhost:6379)
3. **TiDB** (localhost:4000)
4. **TiCDC** (配置监听 message 表)

## 配置 TiCDC

### 1. 创建 Changefeed

```bash
# 创建 changefeed，监听 message 表变更
tiup cdc cli changefeed create \
  --server=http://127.0.0.1:8300 \
  --sink-uri="kafka://127.0.0.1:9092/MsgEvents?protocol=canal-json" \
  --changefeed-id="message-events" \
  --config=/path/to/changefeed.toml
```

### 2. Changefeed 配置文件 (changefeed.toml)

```toml
[filter]
rules = ['im_storage.message']

[sink]
protocol = "canal-json"
```

### 3. 验证 Changefeed

```bash
# 查看 changefeed 状态
tiup cdc cli changefeed list --server=http://127.0.0.1:8300

# 查看详细信息
tiup cdc cli changefeed query \
  --server=http://127.0.0.1:8300 \
  --changefeed-id="message-events"
```

## 修改配置

修改 `application-dev.yaml`：

```yaml
server:
  host: localhost  # 根据实际情况修改

redis:
  password: your_password  # 如果 Redis 有密码
```

## 启动应用

### 方式 1: Maven 启动

```bash
cd storage-consumer
mvn spring-boot:run
```

### 方式 2: JAR 启动

```bash
# 构建
mvn clean package -DskipTests

# 启动
java -jar target/storage-consumer-1.0.0-SNAPSHOT.jar
```

## 验证服务

### 1. 健康检查

```bash
curl http://localhost:8080/actuator/health
```

预期响应：

```json
{
  "status": "UP"
}
```

### 2. 查看指标

```bash
curl http://localhost:8080/actuator/metrics
```

### 3. 查看日志

应用启动后，查看日志输出：

```
2025-11-20 10:30:00.000 [main] INFO  c.p.c.StorageConsumerApplication - Starting StorageConsumerApplication
2025-11-20 10:30:01.000 [main] INFO  o.a.k.c.c.KafkaConsumer - [Consumer clientId=consumer-1, groupId=storage-consumer-message-group] Subscribed to topic(s): MsgEvents
2025-11-20 10:30:02.000 [main] INFO  c.p.c.StorageConsumerApplication - Started StorageConsumerApplication in 2.5 seconds
```

## 测试消息处理

### 1. 插入测试消息

在 storage-server 中调用 `SaveMessage` 接口，或直接在 TiDB 中插入消息：

```sql
INSERT INTO message (
  session_id, seq_id, msg_id, sender_id, 
  msg_type, content, status, create_time
) VALUES (
  123456, 1, 987654321, 1001, 
  1, '{"text":"Hello World"}', 0, NOW()
);
```

### 2. 查看 Consumer 日志

应用应该会打印类似以下的日志：

```
2025-11-20 10:30:10.000 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO  c.p.c.l.MessageEventListener - Received message event: topic=MsgEvents, partition=0, offset=1
2025-11-20 10:30:10.100 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO  c.p.c.l.MessageEventListener - Parsed message event: type=INSERT, sessionId=123456, seqId=1, msgId=987654321
2025-11-20 10:30:10.200 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO  c.p.c.s.WriteFanoutService - Write fanout completed: sessionId=123456, memberCount=5
2025-11-20 10:30:10.300 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO  c.p.c.s.ReadFanoutService - Read fanout completed: sessionId=123456, msgId=987654321
2025-11-20 10:30:10.400 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO  c.p.c.l.MessageEventListener - INSERT event handled: msgId=987654321
```

### 3. 验证 Redis 数据

```bash
# 连接 Redis
redis-cli

# 查看会话列表（假设用户ID是 1001）
ZRANGE user_sessions:1001 0 -1 WITHSCORES

# 查看会话元数据
HGETALL session_meta:1001

# 查看未读计数
HGETALL unread_count:1001

# 查看消息缓存
ZRANGE cache:msgs:123456 0 -1 WITHSCORES

# 查看消息元数据
GET msg_meta:987654321
```

## 常见问题

### 1. Kafka 连接失败

**错误信息：**
```
org.apache.kafka.common.errors.TimeoutException: Failed to update metadata
```

**解决方法：**
- 检查 Kafka 是否启动
- 检查 `application.yaml` 中的 `bootstrap-servers` 配置
- 检查网络连接

### 2. Redis 连接失败

**错误信息：**
```
io.lettuce.core.RedisConnectionException: Unable to connect to Redis
```

**解决方法：**
- 检查 Redis 是否启动
- 检查 Redis 密码配置
- 检查网络连接

### 3. 数据库连接失败

**错误信息：**
```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```

**解决方法：**
- 检查 TiDB 是否启动
- 检查数据库连接配置
- 检查数据库用户权限

### 4. TiCDC 未推送消息

**解决方法：**
- 检查 TiCDC changefeed 状态
- 检查 Kafka Topic 是否存在
- 使用 Kafka 消费者工具验证消息是否到达

```bash
# 验证 Kafka Topic
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic MsgEvents \
  --from-beginning
```

## 性能调优

### 1. 调整 Kafka Consumer 并发度

修改 `application.yaml`：

```yaml
spring:
  kafka:
    listener:
      concurrency: 5  # 增加并发度
```

### 2. 调整批量拉取大小

```yaml
spring:
  kafka:
    consumer:
      max-poll-records: 200  # 增加每次拉取的消息数
```

### 3. 调整 Redis 连接池

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 50  # 增加连接池大小
```

## 监控告警

### 1. Prometheus 集成

添加依赖（已包含在 pom.xml）：

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

暴露指标端点：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

访问指标：

```bash
curl http://localhost:8080/actuator/prometheus
```

### 2. 关键指标

- `kafka_consumer_lag`: Kafka 消费延迟
- `kafka_consumer_records_consumed_total`: 消费消息总数
- `redis_commands_duration_seconds`: Redis 命令耗时
- `hikaricp_connections_active`: 数据库连接池活跃连接数

## 部署建议

### 1. 资源配置

- **CPU**: 2-4 核
- **内存**: 2-4 GB
- **JVM 参数**: `-Xmx2g -Xms2g -XX:+UseG1GC`

### 2. 高可用部署

- 部署多个实例（使用相同的 Consumer Group）
- Kafka 会自动分配分区，实现负载均衡
- 确保每个实例都能访问 Redis 和 TiDB

### 3. 日志管理

- 使用 ELK 或 Loki 收集日志
- 配置日志级别（生产环境建议使用 INFO）
- 定期清理日志文件

## 下一步

- 阅读 [README.md](README.md) 了解详细架构
- 查看 [storage-server](../storage-server/README.md) 了解上游服务
- 配置监控和告警系统

