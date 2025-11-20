# Storage Consumer

## 简介

Storage Consumer 是 IM 存储系统的异步处理服务，负责消费 Kafka 消息事件并执行写扩散和读扩散操作。

## 架构设计

### 核心职责

1. **Job A (写扩散 - Write Fanout)**
   - 监听消息变更事件
   - 查询会话成员列表
   - 批量更新所有成员的 Redis 会话列表
   - 更新未读计数

2. **Job B (读扩散 - Read Fanout)**
   - 将新消息写入 Redis 公共消息缓存
   - 供 GetMessages 接口高频读取
   - 处理消息状态更新（撤回/删除）

3. **Job C (搜索/分析 - 预留)**
   - 同步消息到搜索引擎
   - 同步到数仓进行分析

### 数据流

```
TiDB (message 表)
    ↓ (TiCDC)
Kafka Topic: MsgEvents
    ↓ (Consumer)
Storage Consumer
    ├─→ Job A: 更新 Redis 会话列表
    ├─→ Job B: 缓存消息到 Redis
    └─→ Job C: 同步到搜索/数仓 (预留)
```

## Redis 数据结构

### 会话列表（写扩散）

1. **user_sessions:{user_id}** (ZSET)
   - Score: 最后一条消息时间戳
   - Value: session_id
   - 用途: 按时间排序的会话列表

2. **session_meta:{user_id}** (HASH)
   - Field: session_id
   - Value: JSON (包含 unread_count, last_seq_id, preview, timestamp)
   - 用途: 会话元数据（预览信息）

3. **unread_count:{user_id}** (HASH)
   - Field: session_id
   - Value: 未读数
   - 用途: 未读消息计数

### 消息缓存（读扩散）

1. **cache:msgs:{session_id}** (ZSET)
   - Score: seq_id
   - Value: msg_id
   - TTL: 3天
   - 用途: 消息索引（公共缓存）

2. **msg_meta:{msg_id}** (String - Binary)
   - Value: Protobuf 序列化的消息
   - TTL: 1天
   - 用途: 消息元数据

### 成员缓存

1. **session_members:{session_id}** (SET)
   - Value: user_id
   - TTL: 1小时
   - 用途: 会话成员列表缓存

## TiCDC 配置

TiCDC 需要监听 `message` 表的变更，并推送到 Kafka Topic `MsgEvents`。

### 事件格式

消息事件需要包含以下字段（JSON 格式）：

```json
{
  "eventType": "INSERT",
  "sessionId": 123456,
  "seqId": 101,
  "msgId": 987654321,
  "senderId": 1001,
  "msgType": 1,
  "content": "Hello World",
  "status": 0,
  "extra": {},
  "createTime": "2025-11-20T10:30:00",
  "eventTimestamp": 1732096200000
}
```

**字段说明：**

- `eventType`: 事件类型（INSERT/UPDATE/DELETE）
- `sessionId`: 会话ID
- `seqId`: 会话内序列ID
- `msgId`: 全局唯一消息ID
- `senderId`: 发送者用户ID
- `msgType`: 消息类型（1=文本，2=图片，3=视频等）
- `content`: 消息内容（JSON 字符串）
- `status`: 消息状态（0=正常，1=撤回，2=删除）
- `extra`: 扩展字段
- `createTime`: 创建时间
- `eventTimestamp`: 事件时间戳

## 配置说明

### application.yaml

```yaml
# Kafka 配置
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: storage-consumer-group

# Redis 配置
spring:
  data:
    redis:
      host: localhost
      port: 6379

# 数据库配置（查询 member 表）
spring:
  datasource:
    url: jdbc:mysql://localhost:4000/im_storage

# Consumer 配置
consumer:
  kafka:
    topic: MsgEvents
  redis:
    max-session-list-size: 200
    max-message-cache-size: 1000
```

## 启动方式

### 开发环境

```bash
# 确保 Kafka、Redis、TiDB 已启动

# 启动应用
mvn spring-boot:run
```

### 生产环境

```bash
# 构建 JAR
mvn clean package -DskipTests

# 启动服务
java -jar storage-consumer-1.0.0-SNAPSHOT.jar
```

## 监控指标

应用暴露了以下监控端点：

- `GET /actuator/health` - 健康检查
- `GET /actuator/metrics` - 性能指标
- `GET /actuator/info` - 应用信息

## 性能优化

### 批量处理

- Kafka Consumer 采用批量拉取（max.poll.records=100）
- Redis 操作使用 Pipeline 减少网络开销

### 缓存策略

- 成员列表缓存 1 小时，减少数据库查询
- 消息缓存 3 天，覆盖大部分读取场景

### 并发控制

- Kafka Listener 并发度为 3（可配置）
- 使用手动确认模式，确保消息不丢失

## 故障处理

### 消息重试

- 如果处理失败，不确认消费，Kafka 会重新投递
- 重试次数由 Kafka Consumer 配置控制

### 幂等性

- 写扩散操作天然幂等（覆盖写）
- 读扩散操作天然幂等（覆盖写）

### 监控告警

建议监控以下指标：

- Kafka Consumer Lag（消费延迟）
- 消息处理失败率
- Redis 连接池状态
- 数据库连接池状态

## 依赖服务

- **Kafka**: 消息队列（TiCDC 推送事件）
- **Redis**: 缓存存储（会话列表、消息缓存）
- **TiDB**: 数据库（查询成员列表）
- **storage-proto**: Protobuf 定义（消息模型）

## 注意事项

1. **TiCDC 推送格式**：当前代码假设 TiCDC 推送的是 JSON 格式，实际生产环境可能需要调整解析逻辑（例如 Canal JSON 格式）。

2. **成员列表查询**：大群场景下，成员列表可能很大，建议：
   - 使用缓存减少 DB 查询
   - 考虑分批写扩散（异步处理）

3. **Redis 内存管理**：
   - 设置合理的 TTL
   - 监控 Redis 内存使用情况
   - 考虑使用 Redis Cluster 分片

4. **消息顺序**：
   - Kafka 保证分区内消息有序
   - 建议按 session_id 分区，确保同一会话的消息顺序处理

## 后续优化

1. **Job C 实现**：同步消息到搜索引擎（Elasticsearch）
2. **数仓同步**：同步消息到数仓进行分析
3. **死信队列**：处理失败的消息写入死信队列
4. **指标监控**：集成 Prometheus + Grafana

