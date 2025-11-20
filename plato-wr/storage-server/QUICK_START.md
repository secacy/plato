# Storage Server 快速开始指南

## 前置要求

1. **Java 17+**
2. **TiDB 数据库**（或兼容 MySQL 5.7+ 的数据库）
3. **Redis 6.0+**
4. **Kafka 2.8+**（可选，用于事件发布）

## 1. 数据库初始化

### 创建数据库
```sql
CREATE DATABASE IF NOT EXISTS im_storage 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

### 执行 Schema
```bash
mysql -h 127.0.0.1 -P 4000 -u root < src/main/resources/db/schema.sql
```

或者直接连接 TiDB 执行：
```sql
USE im_storage;

-- 会话元数据表
CREATE TABLE IF NOT EXISTS t_conversation_meta (
    session_id   BIGINT PRIMARY KEY,
    max_seq      BIGINT NOT NULL DEFAULT 0,
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 消息表
CREATE TABLE IF NOT EXISTS message (
    session_id  BIGINT NOT NULL,
    seq_id      BIGINT NOT NULL,
    msg_id      BIGINT NOT NULL,
    sender_id   BIGINT NOT NULL,
    msg_type    INT NOT NULL,
    content     JSON NOT NULL,
    status      INT NOT NULL DEFAULT 0,
    extra       JSON,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (session_id, seq_id),
    UNIQUE KEY uk_msg_id (msg_id),
    KEY idx_sender_time (sender_id, create_time)
) ENGINE=InnoDB;
```

## 2. 配置文件

### application-dev.yaml
```yaml
server:
  host: 127.0.0.1  # 替换为你的服务器地址

redis:
  password: your_redis_password
```

### 关键配置项

#### TiDB 连接
```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:4000/im_storage
    username: root
    password: ""
```

#### Redis 连接
```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: ${redis.password}
```

#### 批处理参数（可调优）
```yaml
storage:
  batch:
    capacity-threshold: 50      # 批次大小
    time-threshold-ms: 10       # 批次超时时间
    max-cached-messages: 1000   # Redis 缓存消息数
```

## 3. 编译运行

### Maven 编译
```bash
cd storage-server
mvn clean package -DskipTests
```

### 运行服务
```bash
java -jar target/storage-server-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

或者使用 Maven 直接运行：
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 4. 验证服务

### 检查健康状态
```bash
curl http://localhost:8080/actuator/health
```

### 检查 gRPC 端口
```bash
netstat -an | grep 9090
```

## 5. 测试调用

### 使用 gRPC 客户端

#### SaveMessage 示例
```java
// 1. 创建 gRPC Channel
ManagedChannel channel = ManagedChannelBuilder
    .forAddress("localhost", 9090)
    .usePlaintext()
    .build();

StorageMessageServiceGrpc.StorageMessageServiceBlockingStub stub = 
    StorageMessageServiceGrpc.newBlockingStub(channel);

// 2. 构建请求
SaveMessageRequest request = SaveMessageRequest.newBuilder()
    .setSessionId(1001L)
    .setSenderId(2001L)
    .setClientMessageId(UUID.randomUUID().toString())
    .setMsgType(1)  // 1=Text
    .setContent(ByteString.copyFromUtf8("Hello, World!"))
    .putExtra("key1", "value1")
    .build();

// 3. 调用服务
SaveMessageResponse response = stub.saveMessage(request);

System.out.println("msgId: " + response.getMsgId());
System.out.println("seqId: " + response.getSeqId());
System.out.println("serverTime: " + response.getServerTimeMs());
```

#### GetMessages 示例
```java
// 拉取新消息
GetMessagesRequest request = GetMessagesRequest.newBuilder()
    .setSessionId(1001L)
    .setAnchorSeqId(0)  // 从头开始
    .setDirection(GetMessagesRequest.Direction.FORWARD)
    .setLimit(50)
    .build();

GetMessagesResponse response = stub.getMessages(request);

for (Message msg : response.getMessagesList()) {
    System.out.println("msgId: " + msg.getMsgId() + 
                       ", seqId: " + msg.getSeqId() +
                       ", content: " + msg.getContent().toStringUtf8());
}
```

## 6. 监控指标

### 查看批处理统计
```bash
curl http://localhost:8080/actuator/metrics/batch.buffer.count
```

### 查看 JVM 指标
```bash
curl http://localhost:8080/actuator/metrics
```

## 7. 性能调优

### 数据库连接池
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20     # 根据并发量调整
      minimum-idle: 5
      connection-timeout: 30000
```

### Redis 连接池
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20        # 根据并发量调整
          max-idle: 10
```

### 批处理参数
```yaml
storage:
  batch:
    capacity-threshold: 100     # 增大批次 → 提升吞吐，增加延迟
    time-threshold-ms: 5        # 减小超时 → 降低延迟，减少吞吐
    core-pool-size: 8           # 根据 CPU 核心数调整
```

## 8. 常见问题

### Q1: 消息重复怎么办？
**A**: 确保每次调用都传入唯一的 `client_message_id`（建议使用 UUID）。系统会自动在 5 分钟内去重。

### Q2: 批处理延迟过高？
**A**: 降低 `time-threshold-ms`（如改为 5ms），或增加 `capacity-threshold`。

### Q3: Redis 缓存命中率低？
**A**: 检查是否频繁查询冷数据（很久以前的消息），可以增加 `message-cache-ttl-days`。

### Q4: 数据库事务超时？
**A**: 检查批次大小是否过大，或数据库连接池是否饱和。

## 9. 故障排查

### 查看日志
```bash
tail -f logs/storage-server.log
```

### 关键日志
```
# 批处理触发
Flushing buffer for session 1001, batch size: 50

# 事务成功
Batch processing completed for session 1001, allocated seq_id range: [101, 150]

# 缓存命中
Cache hit: sessionId=1001, found 50 messages

# Gap 检测
Gap detected: sessionId=1001, expected=101, actual=200
```

### 监控 Redis
```bash
redis-cli
> KEYS cache:msgs:*
> ZCARD cache:msgs:1001
> TTL msg_meta:123456789
```

### 监控 TiDB
```sql
-- 查看会话元数据
SELECT * FROM t_conversation_meta WHERE session_id = 1001;

-- 查看消息数量
SELECT session_id, COUNT(*) as msg_count 
FROM message 
GROUP BY session_id;

-- 查看最近的消息
SELECT * FROM message 
WHERE session_id = 1001 
ORDER BY seq_id DESC 
LIMIT 10;
```

## 10. 生产部署建议

### 容器化部署
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/storage-server-1.0-SNAPSHOT.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Kubernetes 部署
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: storage-server
spec:
  replicas: 3
  selector:
    matchLabels:
      app: storage-server
  template:
    metadata:
      labels:
        app: storage-server
    spec:
      containers:
      - name: storage-server
        image: storage-server:latest
        ports:
        - containerPort: 9090
          name: grpc
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
```

### 负载均衡
使用 Kubernetes Service 或 Nginx 进行 gRPC 负载均衡。

## 11. 下一步

- 查看 [NEW_ARCHITECTURE.md](./NEW_ARCHITECTURE.md) 了解详细架构设计
- 查看 [ARCHITECTURE.md](./ARCHITECTURE.md) 了解整体系统架构
- 查看 [PROJECT_STRUCTURE.md](./PROJECT_STRUCTURE.md) 了解项目结构

## 联系方式

如有问题，请联系开发团队或提交 Issue。

