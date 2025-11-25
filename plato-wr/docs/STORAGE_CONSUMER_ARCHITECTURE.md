# Storage Consumer 架构文档

## 概述

Storage Consumer 是 IM 存储系统的异步处理引擎，基于 Kafka 消息队列实现**写扩散（Write Fanout）**和**读扩散（Read Fanout）**策略。

## 架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                          TiDB (message 表)                          │
│                     INSERT/UPDATE/DELETE 变更                       │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
                        ┌──────────────┐
                        │    TiCDC     │ (Change Data Capture)
                        │ Canal JSON   │
                        └──────┬───────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │  Kafka Topic        │
                    │  "MsgEvents"        │
                    └─────────┬───────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────────┐
        │      Storage Consumer (本模块)              │
        │                                              │
        │  ┌──────────────────────────────────────┐   │
        │  │  MessageEventListener                │   │
        │  │  - 监听 Kafka 消息                    │   │
        │  │  - 解析 TiCDC Canal JSON             │   │
        │  └──────────┬───────────────────────────┘   │
        │             │                                │
        │    ┌────────┴────────┐                      │
        │    ▼                 ▼                      │
        │  ┌──────────────┐ ┌──────────────┐         │
        │  │   Job A      │ │   Job B      │         │
        │  │ WriteFanout  │ │ ReadFanout   │         │
        │  └──────┬───────┘ └──────┬───────┘         │
        │         │                │                  │
        └─────────┼────────────────┼──────────────────┘
                  │                │
         ┌────────┴────────┐  ┌────┴─────────┐
         ▼                 ▼  ▼              ▼
    ┌─────────┐      ┌────────────┐   ┌──────────┐
    │  Redis  │      │   Redis    │   │   TiDB   │
    │ 会话列表 │      │ 消息缓存   │   │ member表 │
    │ 未读计数 │      │ (公共缓存) │   │         │
    └─────────┘      └────────────┘   └──────────┘
```

## 核心组件

### 1. 事件监听器 (MessageEventListener)

**职责：**
- 监听 Kafka Topic `MsgEvents`
- 解析 TiCDC Canal JSON 格式的事件
- 路由到不同的处理器（Job A / Job B）

**关键特性：**
- **手动确认模式**：确保消息不丢失
- **并发消费**：支持多线程并发处理（可配置）
- **错误处理**：处理失败时不确认消费，等待重试

**代码路径：**
```
com.plato.consumer.listener.MessageEventListener
```

### 2. 事件解析器 (EventParser)

**职责：**
- 解析 TiCDC Canal JSON 格式
- 将 CDC 事件转换为内部 `MessageEvent`
- 处理不同的时间格式

**支持的事件类型：**
- `INSERT`: 新消息插入
- `UPDATE`: 消息状态更新（撤回/删除）
- `DELETE`: 消息物理删除（一般不使用）

**代码路径：**
```
com.plato.consumer.event.EventParser
com.plato.consumer.event.TiCDCEvent
com.plato.consumer.event.MessageEvent
```

### 3. Job A - 写扩散服务 (WriteFanoutService)

**职责：**
- 查询会话成员列表
- 批量更新所有成员的 Redis 会话列表
- 更新未读计数

**核心流程：**
```
1. 获取群成员列表（优先从 Redis 缓存）
   ├─ Cache Hit: 直接使用
   └─ Cache Miss: 查询 DB 并回填缓存

2. 批量更新会话列表
   ├─ user_sessions:{uid} (ZSET) - 按时间排序
   ├─ session_meta:{uid} (HASH) - 会话元数据
   └─ unread_count:{uid} (HASH) - 未读计数

3. 生成消息预览
   └─ 根据消息类型生成预览文本
```

**Redis 数据结构：**

1. **user_sessions:{user_id}** (ZSET)
   ```
   Score: last_msg_timestamp (消息时间戳)
   Value: session_id
   ```

2. **session_meta:{user_id}** (HASH)
   ```json
   {
     "sessionId": {
       "unread_count": 5,
       "last_seq_id": 101,
       "preview": "Hello World",
       "timestamp": 1732096200000,
       "is_pinned": false
     }
   }
   ```

3. **unread_count:{user_id}** (HASH)
   ```
   Field: session_id
   Value: count
   ```

**代码路径：**
```
com.plato.consumer.service.WriteFanoutService
com.plato.consumer.redis.SessionListRedisService
```

### 4. Job B - 读扩散服务 (ReadFanoutService)

**职责：**
- 将新消息写入 Redis 公共消息缓存
- 处理消息状态更新（撤回/删除）
- 供 GetMessages 接口高频读取

**核心流程：**
```
1. 缓存消息索引
   └─ cache:msgs:{session_id} (ZSET)
      Score: seq_id
      Value: msg_id

2. 缓存消息元数据
   └─ msg_meta:{msg_id} (String - Protobuf 二进制)
      Value: Protobuf 序列化的 Message

3. 限制缓存大小
   └─ 只保留最近 1000 条消息

4. 设置过期时间
   ├─ 消息索引: 3天
   └─ 消息元数据: 1天
```

**Redis 数据结构：**

1. **cache:msgs:{session_id}** (ZSET)
   ```
   Score: seq_id (序列ID)
   Value: msg_id (消息ID)
   TTL: 3天
   ```

2. **msg_meta:{msg_id}** (String - Binary)
   ```
   Value: Protobuf 序列化的 Message
   TTL: 1天
   ```

**代码路径：**
```
com.plato.consumer.service.ReadFanoutService
com.plato.consumer.redis.MessageCacheRedisService
```

### 5. 成员缓存服务 (MemberCacheRedisService)

**职责：**
- 缓存会话成员列表
- 减少数据库查询
- 提供成员列表的快速访问

**缓存策略：**
- **TTL**: 1小时
- **数据源**: TiDB `member` 表
- **缓存粒度**: 按会话 (session_id)

**Redis 数据结构：**

**session_members:{session_id}** (SET)
```
Value: user_id_1, user_id_2, user_id_3, ...
TTL: 1小时
```

**代码路径：**
```
com.plato.consumer.redis.MemberCacheRedisService
com.plato.consumer.repository.mapper.MemberMapper
```

## 数据流详解

### 新消息插入流程

```
1. 用户发送消息
   ↓
2. storage-server 写入 TiDB (message 表)
   ↓
3. TiCDC 监听到 INSERT 事件
   ↓
4. TiCDC 推送到 Kafka (MsgEvents)
   ↓
5. storage-consumer 消费消息
   ├─ Job A: 写扩散
   │   ├─ 查询群成员列表
   │   ├─ 更新所有成员的会话列表
   │   └─ 增加未读计数
   │
   └─ Job B: 读扩散
       ├─ 缓存消息到 Redis
       └─ 供 GetMessages 接口读取
```

### 消息撤回流程

```
1. 用户撤回消息
   ↓
2. storage-server 更新 TiDB (status = 1)
   ↓
3. TiCDC 监听到 UPDATE 事件
   ↓
4. TiCDC 推送到 Kafka (MsgEvents)
   ↓
5. storage-consumer 消费消息
   └─ Job B: 更新消息状态
       └─ 更新 Redis 缓存中的消息状态
```

## 性能优化策略

### 1. 批量处理

- **Kafka Consumer**: 每次拉取 100 条消息（可配置）
- **Redis Pipeline**: 批量操作减少网络开销

### 2. 缓存分层

```
L1: Redis 成员列表缓存 (1小时)
    ├─ 命中率: 95%+
    └─ 减少 DB 查询

L2: Redis 消息缓存 (3天)
    ├─ 命中率: 90%+
    └─ 覆盖大部分读取场景
```

### 3. 并发控制

- **Kafka Listener 并发度**: 3（可配置）
- **异步处理**: 写扩散和读扩散并行执行

### 4. 数据压缩

- **Protobuf 序列化**: 比 JSON 节省 50%+ 存储空间
- **Kafka 压缩**: Snappy 压缩（在 producer 端配置）

## 监控指标

### 关键指标

1. **消费延迟** (Kafka Consumer Lag)
   - 正常: < 100 条
   - 告警: > 1000 条

2. **消息处理耗时**
   - 正常: < 50ms
   - 告警: > 200ms

3. **Redis 命中率**
   - 成员列表缓存: > 95%
   - 消息缓存: > 90%

4. **错误率**
   - 正常: < 0.1%
   - 告警: > 1%

### Prometheus 指标

```
# Kafka 消费指标
kafka_consumer_lag
kafka_consumer_records_consumed_total

# Redis 操作指标
redis_commands_duration_seconds
redis_commands_total

# 数据库连接池
hikaricp_connections_active
hikaricp_connections_pending

# JVM 指标
jvm_memory_used_bytes
jvm_gc_pause_seconds
```

## 容错机制

### 1. 消息重试

- **策略**: 处理失败时不确认消费，Kafka 会自动重试
- **重试间隔**: 由 Kafka Consumer 配置控制
- **最大重试次数**: 无限制（直到成功或人工介入）

### 2. 幂等性保证

- **写扩散**: 覆盖写，天然幂等
- **读扩散**: 覆盖写，天然幂等
- **成员列表缓存**: 覆盖写，天然幂等

### 3. 故障恢复

```
场景 1: Redis 故障
  └─ 影响: 缓存操作失败
  └─ 恢复: 自动重试，Redis 恢复后自动回填

场景 2: TiDB 故障
  └─ 影响: 成员列表查询失败
  └─ 恢复: 使用 Redis 缓存兜底

场景 3: Kafka 故障
  └─ 影响: 消息消费暂停
  └─ 恢复: Kafka 恢复后继续消费

场景 4: Consumer 崩溃
  └─ 影响: 消息消费暂停
  └─ 恢复: 重启后从上次提交的偏移量继续
```

## 扩展性

### 水平扩展

- **部署多个实例**：使用相同的 Consumer Group
- **自动负载均衡**：Kafka 自动分配分区
- **无状态设计**：实例间无依赖，可随意扩缩容

### 分区策略

建议 Kafka Topic `MsgEvents` 按 `session_id` 分区：

```
分区数 = Consumer 实例数 × 2

例如：
- 3 个 Consumer 实例
- 6 个分区
- 每个实例处理 2 个分区
```

**好处：**
- 同一会话的消息顺序保证
- 负载均衡
- 避免热点

## 配置参考

### 生产环境推荐配置

```yaml
# Kafka Consumer
spring:
  kafka:
    consumer:
      max-poll-records: 200
      heartbeat-interval-ms: 3000
      session-timeout-ms: 30000
    listener:
      concurrency: 5

# Redis
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 50
          max-idle: 20
          min-idle: 10

# 数据库
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

# Consumer 配置
consumer:
  redis:
    max-session-list-size: 200
    max-message-cache-size: 1000
    message-cache-ttl-days: 3
```

### 资源配置

- **CPU**: 4 核
- **内存**: 4 GB
- **JVM**: `-Xmx3g -Xms3g -XX:+UseG1GC`
- **磁盘**: 无需持久化存储

## 总结

Storage Consumer 是 IM 存储系统的异步处理引擎，通过 Kafka + Redis 实现了高性能、高可用的写扩散和读扩散机制。核心设计原则：

1. **异步解耦**：通过 Kafka 解耦写入和扩散
2. **批量处理**：提升吞吐量
3. **缓存优先**：减少数据库压力
4. **幂等设计**：确保可重试
5. **水平扩展**：无状态设计，易于扩容

