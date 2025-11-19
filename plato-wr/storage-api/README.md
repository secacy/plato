# Storage API

## 项目简介

`storage-api` 是IM存储系统的gRPC API网关服务。

### 核心职责

1. **异步写入**：生成ID → 发送Kafka → 立即返回
2. **同步查询**：查询Redis/TiDB → 返回数据
3. **事务性命令**：TiDB事务 → Redis更新 → Kafka事件 → 返回

## 架构设计

### 分层架构

```
grpc/              # gRPC服务层（接口实现）
  ├─ StorageMessageGrpcService
  ├─ StorageInboxGrpcService
  └─ StorageGroupGrpcService

service/           # 业务逻辑层
  ├─ MessageService
  ├─ InboxService
  └─ GroupMemberService

repository/        # 数据访问层
  ├─ entity/       # 实体类
  └─ mapper/       # MyBatis Mapper

redis/             # Redis服务层
  ├─ SeqIdGenerator
  ├─ InboxRedisService
  └─ GroupMemberRedisService

producer/          # Kafka生产者
  └─ StorageEventProducer

event/             # 事件模型
  ├─ MessageEvent
  └─ GroupMemberEvent
```

## 核心流程

### 1. 消息保存流程（异步）

```
客户端 
  → SaveMessage(request)
  → 幂等检查（Redis）
  → 生成msgId（Snowflake）
  → 生成seqId（Redis INCR）
  → 发送Kafka事件
  → 立即返回（msgId, seqId）
  
[异步]
Kafka Consumer
  → 写入TiDB
  → 写扩散到Redis Inbox
  → 更新Session列表
```

### 2. 消息查询流程（同步）

```
客户端
  → GetMessages(sessionId, anchorSeqId, direction)
  → 查询TiDB（按seqId分页）
  → 返回消息列表
```

### 3. 群成员同步流程（事务性）

```
客户端
  → UpsertGroupMembers(sessionId, memberIds)
  → TiDB事务写入
  → 更新Redis缓存
  → 发送Kafka事件（兜底）
  → 返回成功
```

## 依赖服务

- **TiDB**: 消息持久化存储
- **Redis**: 热数据缓存、SeqID生成、会话列表
- **Kafka**: 异步事件驱动
- **ID Generator**: 全局唯一ID生成（Snowflake算法）

## 配置说明

### application.yml

```yaml
# gRPC服务端口
grpc:
  server:
    port: 9090

# Kafka配置
spring:
  kafka:
    bootstrap-servers: localhost:9092

# TiDB配置（兼容MySQL协议）
  datasource:
    url: jdbc:mysql://localhost:4000/im_storage
    
# Redis配置
  data:
    redis:
      host: localhost
      port: 6379
```

## 启动步骤

### 1. 前置条件

确保以下服务已启动：
- TiDB (端口: 4000)
- Redis (端口: 6379)
- Kafka (端口: 9092)

### 2. 初始化数据库

```sql
-- 执行 database.sql 创建表结构
mysql -h 127.0.0.1 -P 4000 -u root < database.sql
```

### 3. 编译项目

```bash
# 先编译storage-proto（生成gRPC代码）
cd ../storage-proto
mvn clean install

# 再编译storage-id-sdk
cd ../storage-id-sdk
mvn clean install

# 最后编译storage-api
cd ../storage-api
mvn clean package
```

### 4. 启动服务

```bash
mvn spring-boot:run
```

或

```bash
java -jar target/storage-api-1.0-SNAPSHOT.jar
```

## 测试

### 使用grpcurl测试

```bash
# 安装grpcurl
# Mac: brew install grpcurl
# Linux: 下载二进制文件

# 测试SaveMessage
grpcurl -plaintext -d '{
  "session_id": 1001,
  "sender_id": 2001,
  "client_message_id": "uuid-123",
  "msg_type": 1,
  "content": "SGVsbG8gV29ybGQ="
}' localhost:9090 com.plato.gateway.StorageMessageService/SaveMessage

# 测试GetMessages
grpcurl -plaintext -d '{
  "session_id": 1001,
  "anchor_seq_id": 0,
  "direction": "BACKWARD",
  "limit": 20
}' localhost:9090 com.plato.gateway.StorageMessageService/GetMessages
```

## 监控指标

### 关键指标

- **SaveMessage P99延迟**: < 10ms（仅本地ID生成+Kafka发送）
- **GetMessages P99延迟**: < 50ms（TiDB查询）
- **Redis连接池**: 监控连接数、等待时间
- **Kafka发送成功率**: > 99.99%

### 日志级别

```yaml
logging:
  level:
    com.plato.api: INFO
    com.plato.api.grpc: INFO
    com.plato.api.producer: DEBUG  # 生产环境改为INFO
```

## 常见问题

### 1. Proto类找不到

确保先编译`storage-proto`：
```bash
cd ../storage-proto && mvn clean install
```

### 2. SeqID生成失败

检查Redis连接：
```bash
redis-cli ping
```

### 3. Kafka发送失败

检查Kafka连接和Topic是否创建：
```bash
kafka-topics.sh --list --bootstrap-server localhost:9092
```

## 后续优化

### 性能优化
- [ ] 消息批量写入TiDB
- [ ] Redis Pipeline批量操作
- [ ] 本地缓存群成员列表（Caffeine）

### 可靠性优化
- [ ] Kafka重试和死信队列
- [ ] 熔断和限流（Resilience4j）
- [ ] 分布式追踪（OpenTelemetry）

### 功能扩展
- [ ] 消息搜索（Elasticsearch）
- [ ] 消息审计日志
- [ ] 监控告警（Prometheus）

