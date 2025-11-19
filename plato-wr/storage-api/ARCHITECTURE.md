# Storage API 架构设计

## 一、系统定位

`storage-api` 是IM存储系统的**同步网关层**，是上层业务系统与存储层之间的唯一入口。

### 设计原则

1. **职责分离**：API只负责接收请求和返回响应，重量级处理交给Consumer
2. **高可用**：无状态设计，可水平扩展
3. **高性能**：写操作异步化，读操作优先热数据
4. **强一致**：事务性操作同步执行，确保数据一致性

## 二、核心设计模式

### 1. CQRS (命令查询职责分离)

```
写命令 (Command)          读查询 (Query)
     ↓                        ↓
  生成ID                  查询Redis
     ↓                        ↓
  发Kafka       ←异步→    查询TiDB
     ↓                        ↓
  立即返回                  返回数据
     ↓
[Consumer异步处理]
  - 写TiDB
  - 写Redis
  - 更新索引
```

### 2. 事件驱动架构

```
┌─────────────┐
│ Storage API │
└──────┬──────┘
       │ 发送事件
       ↓
┌─────────────┐
│   Kafka     │
└──────┬──────┘
       │ 订阅
       ↓
┌─────────────────────┐
│ Storage Consumer    │
│ ├─ Core Group       │  → TiDB + Redis (核心)
│ └─ Search Group     │  → Elasticsearch (搜索)
└─────────────────────┘
```

### 3. 双写一致性保障

对于事务性操作（如群成员管理），采用"事务同步 + 异步补偿"：

```
1. TiDB事务写入（同步）
   ↓
2. Redis缓存更新（同步）
   ↓
3. Kafka事件发送（异步，兜底）
   ↓
4. 返回成功
```

## 三、请求分类与处理策略

### A类：异步事件（如 SaveMessage）

**特征**：可以接受最终一致性

**流程**：
```java
1. 幂等检查（Redis）
2. 生成msgId（Snowflake）
3. 生成seqId（Redis INCR）
4. 发送Kafka事件
5. 立即返回（<10ms）
```

**优势**：
- 极低延迟
- 高吞吐量
- 解耦处理

### B类：同步查询（如 GetMessages）

**特征**：需要立即返回数据

**流程**：
```java
1. 判断请求范围
2. IF 热数据区 THEN
     查询Redis
   ELSE
     查询TiDB
3. 返回数据（<50ms）
```

**优化**：
- 热点数据缓存
- 连接池复用
- 索引优化

### C类：事务性命令（如 UpsertGroupMembers）

**特征**：需要强一致性

**流程**：
```java
@Transactional
1. TiDB事务写入
2. Redis缓存更新
3. Kafka事件发送（兜底）
4. 返回成功（<100ms）
```

**保障**：
- ACID事务
- 同步更新缓存
- 异步兜底机制

## 四、关键数据结构

### 1. SeqID生成机制

```
Redis Key: seq:{session_id}
操作: INCR
保证: 会话内严格递增

特点:
- 单调递增
- 无回溯
- 高性能（Redis单线程）
```

### 2. 消息热数据（Inbox）

```
# 用户收件箱
Key: inbox:{user_id}
Type: Sorted Set
Score: seq_id
Value: msg_id

优势:
- 按序排列
- 范围查询
- 写扩散实现
```

### 3. 会话列表（双层存储）

```
# 第一层：会话顺序
Key: user_sessions:{user_id}
Type: Sorted Set
Score: last_msg_time_ms
Value: session_id

# 第二层：会话元数据
Key: session_meta:{user_id}
Type: Hash
Field: session_id
Value: {
  unreadCount,
  lastMsgPreview,
  isPinned,
  isMuted
}

设计理由:
- 分离索引和数据
- 减少内存占用
- 提升查询性能
```

### 4. 群成员缓存（LRU）

```
Key: group_members:{session_id}
Type: Set
Value: user_id
TTL: 1小时

优化:
- 本地缓存（Caffeine）
- 批量查询
- 预热机制
```

## 五、性能优化策略

### 1. 写性能优化

```
异步化
├─ Kafka批量发送
├─ Redis Pipeline
└─ TiDB批量插入（Consumer）

去中心化
├─ 本地ID生成（Snowflake）
└─ 分布式SeqID（Redis分片）
```

### 2. 读性能优化

```
多级缓存 z
├─ 本地缓存（JVM）
├─ Redis缓存（热数据）
└─ TiDB存储（冷数据）

索引优化
├─ 主键索引：(session_id, seq_id)
├─ 唯一索引：msg_id
└─ 复合索引：(user_id, update_time)
```

### 3. 连接池优化

```
HikariCP (TiDB)
├─ maximum-pool-size: 20
├─ minimum-idle: 5
└─ connection-timeout: 30s

Lettuce (Redis)
├─ max-active: 20
├─ max-idle: 10
└─ max-wait: 3s
```

## 六、可靠性保障

### 1. 幂等保证

```
Redis Key: idempotent:{client_msg_id}
TTL: 5分钟

流程:
1. 检查client_msg_id是否存在
2. 如存在，返回原msg_id
3. 如不存在，执行并记录
```

### 2. 重试机制

```
Kafka Producer:
├─ retries: 3
├─ acks: all
└─ enable.idempotence: true
```

### 3. 降级策略

```
读降级:
Redis故障 → 直接查TiDB

写降级:
Kafka故障 → 记录本地日志 → 异步补偿
```

## 七、监控指标

### 关键SLA

| 指标 | 目标 | 监控方式 |
|-----|------|---------|
| SaveMessage P99延迟 | <10ms | Micrometer |
| GetMessages P99延迟 | <50ms | Micrometer |
| Kafka发送成功率 | >99.99% | Kafka Metrics |
| Redis连接可用性 | >99.9% | Spring Actuator |
| TiDB连接可用性 | >99.9% | Spring Actuator |

### 告警阈值

```yaml
警告:
- P99延迟 > 目标值2倍
- 错误率 > 0.1%
- 连接池使用率 > 80%

紧急:
- P99延迟 > 目标值5倍
- 错误率 > 1%
- 连接池耗尽
```

## 八、扩展性设计

### 水平扩展

```
Load Balancer (gRPC)
      ↓
┌─────────────────────┐
│  API Instance 1     │
│  API Instance 2     │
│  API Instance N     │
└─────────────────────┘
      ↓
┌─────────────────────┐
│  Kafka Cluster      │
│  (Partition: 32)    │
└─────────────────────┘
```

### 分片策略

```
TiDB分片键: session_id
Redis分片键: user_id
Kafka分区键: session_id

保证:
- 同一会话的消息有序
- 同一用户的数据亲和性
```

## 九、安全设计

### 鉴权（未实现，待扩展）

```java
@GrpcInterceptor
public class AuthInterceptor {
  // 1. 提取Token
  // 2. 验证签名
  // 3. 检查权限
}
```

### 限流（未实现，待扩展）

```java
@RateLimiter(
  qps = 1000,
  strategy = SLIDING_WINDOW
)
```

## 十、技术栈总结

| 层级 | 技术 | 作用 |
|-----|------|------|
| 协议层 | gRPC + Protobuf | 高效RPC通信 |
| 框架层 | Spring Boot | 快速开发框架 |
| 持久层 | MyBatis Plus | ORM映射 |
| 缓存层 | Redis (Lettuce) | 热数据缓存 |
| 消息层 | Kafka | 异步事件驱动 |
| 存储层 | TiDB | 分布式数据库 |
| ID生成 | Snowflake | 全局唯一ID |
| 监控层 | Spring Actuator | 健康检查 |

