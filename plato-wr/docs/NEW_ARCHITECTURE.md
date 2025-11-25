# Storage Server 新架构文档

## 架构概览

基于"Micro-Batching + TiDB Transaction"的消息存储系统，实现高吞吐、低延迟的消息持久化。

### 核心特性

1. **Micro-Batching（微批处理）**：将短时间内的多条消息聚合成批次，减少数据库事务次数
2. **原子事务**：seq_id 生成与消息落库在同一事务中，消除 ID 空洞
3. **Redis 缓存**：实现 Read-Aside Cache + Gap Detection 策略
4. **原子防重**：使用 Redis SETNX 实现幂等保护

## 核心组件

### 1. Micro-Batching 组件

#### SessionBuffer
- **职责**：会话级别的消息缓冲队列
- **实现**：基于 BlockingQueue 的 RingBuffer
- **触发条件**：
  - 容量阈值：50 条消息
  - 时间阈值：10ms
  - **策略**："满员发车 + 定时发车"

#### MessageBatchManager
- **职责**：管理所有会话的 SessionBuffer，协调批量处理
- **线程池**：
  - 定时扫描线程：单线程，每 5ms 扫描一次
  - 批处理工作线程池：CPU 核心数 * 2

#### MessageBatchProcessor
- **职责**：执行批量事务处理
- **事务流程**：
  ```sql
  -- Step 1: 锁定会话元数据
  SELECT max_seq FROM t_conversation_meta WHERE session_id = ? FOR UPDATE;
  
  -- Step 2: 内存计算 seq_id（batch_size = 5）
  -- 假设 max_seq = 100，分配：101, 102, 103, 104, 105
  
  -- Step 3: 批量插入消息
  INSERT INTO message (session_id, seq_id, msg_id, ...) VALUES
    (?, 101, ?), (?, 102, ?), (?, 103, ?), (?, 104, ?), (?, 105, ?);
  
  -- Step 4: 更新最大序列号
  UPDATE t_conversation_meta SET max_seq = 105 WHERE session_id = ?;
  
  -- Step 5: 提交事务
  COMMIT;
  ```

### 2. SaveMessage 流程

```
┌─────────────┐
│ gRPC 请求   │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Step 1: Redis 原子防重                   │
│ SETNX dedup:{session_id}:{client_msg_id}│
│ TTL: 300s                                │
└──────┬──────────────────────────────────┘
       │ (通过)
       ▼
┌─────────────────────────────────────────┐
│ Step 2: 生成 msg_id (Snowflake)         │
│ msgId = idGenerator.gen(BID_MESSAGE)    │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Step 3: 进入缓冲队列                     │
│ SessionBuffer.offer(request)            │
│ - 如果达到容量阈值 → 立即触发            │
│ - 否则等待定时扫描触发                   │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Step 4: 批量事务处理                     │
│ MessageBatchProcessor.processBatch()    │
│ - SELECT ... FOR UPDATE                 │
│ - 批量 INSERT                           │
│ - UPDATE max_seq                        │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Step 5: 异步写入 Redis 缓存              │
│ MessageCacheService.cacheMessages()     │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Step 6: 返回响应                         │
│ SaveMessageResponse {                   │
│   msg_id, seq_id, server_time_ms        │
│ }                                       │
└─────────────────────────────────────────┘
```

### 3. GetMessages 流程

```
┌─────────────┐
│ gRPC 请求   │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Step 1: 判断查询类型                     │
│ - 热数据（较新消息） → 尝试缓存           │
│ - 冷数据（历史消息） → 直接查 DB          │
└──────┬──────────────────────────────────┘
       │ (热数据)
       ▼
┌─────────────────────────────────────────┐
│ Step 2: 查询 Redis 公共缓存              │
│ ZRANGEBYSCORE cache:msgs:{session_id}   │
│   (start_seq +inf LIMIT limit            │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Step 3: Gap Detection（空洞检测）        │
│ expected_seq = start_seq + 1            │
│ actual_seq = first_message.seq_id       │
│                                         │
│ if actual_seq > expected_seq:           │
│   → Gap Detected，降级查 DB              │
│ else:                                   │
│   → Cache Hit，返回缓存数据              │
└──────┬──────────────────────────────────┘
       │ (Cache Miss / Gap)
       ▼
┌─────────────────────────────────────────┐
│ Step 4: 查询 TiDB                        │
│ SELECT * FROM message                   │
│ WHERE session_id = ? AND seq_id > ?     │
│ ORDER BY seq_id ASC LIMIT ?             │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Step 5: 异步回填 Redis 缓存（仅热数据）   │
│ CompletableFuture.runAsync(...)         │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Step 6: 返回响应                         │
│ GetMessagesResponse { messages, hasMore }│
└─────────────────────────────────────────┘
```

## Redis 数据结构

### 1. 消息缓存（公共缓存）

#### cache:msgs:{session_id}
```
Type: ZSET
Score: seq_id
Value: msg_id
TTL: 3天
Max Size: 1000 条（自动清理旧数据）
```

#### msg_meta:{msg_id}
```
Type: String (Binary)
Value: Protobuf 序列化的 Message 对象
TTL: 1天
```

### 2. 去重窗口

#### dedup:{session_id}:{client_message_id}
```
Type: String
Value: "processing"
TTL: 300秒
操作: SETNX（原子性）
```

### 3. 会话列表（现有设计保持不变）

#### user_sessions:{user_id}
```
Type: ZSET
Score: last_msg_timestamp
Value: session_id
```

#### session_meta:{user_id}
```
Type: HASH
Field: session_id
Value: JSON {unread_count, last_seq_id, is_pinned, preview}
```

### 4. 未读计数

#### unread_count:{user_id}
```
Type: HASH
Field: session_id
Value: count
```

## 数据库表结构

### t_conversation_meta（会话元数据表）
```sql
CREATE TABLE t_conversation_meta (
    session_id   BIGINT PRIMARY KEY,
    max_seq      BIGINT NOT NULL DEFAULT 0,
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;
```

### message（消息表）
```sql
CREATE TABLE message (
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

## 性能指标

### 写入性能
- **批次大小**：50 条/批次（平均）
- **事务延迟**：< 10ms（单批次）
- **吞吐量**：~5000 条/秒（单实例）
- **并发数**：支持数千个会话同时写入

### 读取性能
- **缓存命中率**：> 95%（热数据）
- **缓存响应时间**：< 1ms
- **DB 查询时间**：< 5ms（利用索引）

## 配置参数

### application.yaml
```yaml
# Micro-Batching 配置（硬编码在代码中，可改为配置文件）
batch:
  capacity-threshold: 50      # 容量阈值
  time-threshold-ms: 10       # 时间阈值
  scan-interval-ms: 5         # 扫描间隔

# Redis 配置
spring:
  data:
    redis:
      host: ${server.host}
      port: 6379
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          
# TiDB 配置
spring:
  datasource:
    url: jdbc:mysql://${server.host}:4000/im_storage
    hikari:
      maximum-pool-size: 20
```

## 故障处理

### 1. 缓冲队列满
- **现象**：SessionBuffer 队列满
- **处理**：使用 CallerRunsPolicy，由调用线程执行（背压）

### 2. 事务失败
- **现象**：批量插入失败
- **处理**：整批回滚，释放去重锁，返回失败响应

### 3. Redis 故障
- **现象**：Redis 不可用
- **处理**：
  - 去重失败 → 允许通过（可能产生少量重复）
  - 缓存失败 → 降级查 DB

### 4. TiDB 故障
- **现象**：数据库不可用
- **处理**：返回错误，依赖上层重试机制

## 监控指标

### 核心指标
```java
// 批处理统计
- batch.buffer.count          // 当前缓冲队列数量
- batch.queued.messages       // 排队中的消息数
- batch.size.avg              // 平均批次大小
- batch.latency.p99           // 批处理延迟 P99

// 缓存统计
- cache.hit.rate              // 缓存命中率
- cache.gap.detected.count    // Gap 检测次数
- cache.refill.count          // 缓存回填次数

// 业务统计
- message.save.qps            // 消息保存 QPS
- message.save.latency.p99    // 保存延迟 P99
- message.get.qps             // 消息查询 QPS
- message.duplicate.count     // 重复消息拦截数
```

## 扩展性

### 水平扩展
- **无状态设计**：storage-server 实例间无共享状态
- **负载均衡**：通过 gRPC 负载均衡器分发请求
- **缓存一致性**：通过 TiCDC 保证最终一致性

### 容量规划
- **单实例**：支持 ~5000 msg/s
- **集群**：线性扩展（3 实例 = 15000 msg/s）
- **数据库**：TiDB 支持 PB 级存储

## 迁移指南

### 从旧架构迁移
1. 部署新版本 storage-server（保持旧版本运行）
2. 创建 t_conversation_meta 表
3. 初始化现有会话的 max_seq（从 message 表统计）
4. 切换流量到新版本
5. 观察监控指标
6. 下线旧版本

### 数据兼容性
- **message 表**：无需修改，完全兼容
- **新增表**：t_conversation_meta
- **Redis**：新增 key 前缀（cache:msgs:*, msg_meta:*, dedup:*）

## 最佳实践

### 1. 客户端集成
```java
// 生成唯一的 client_message_id
String clientMsgId = UUID.randomUUID().toString();

// 调用 SaveMessage
SaveMessageRequest request = SaveMessageRequest.newBuilder()
    .setSessionId(sessionId)
    .setSenderId(userId)
    .setClientMessageId(clientMsgId)  // 必填！
    .setMsgType(1)
    .setContent(ByteString.copyFromUtf8(content))
    .build();

SaveMessageResponse response = stub.saveMessage(request);
// response.getMsgId() - 全局唯一 ID
// response.getSeqId() - 会话内序列 ID
```

### 2. 消息查询
```java
// 拉取新消息（增量同步）
GetMessagesRequest request = GetMessagesRequest.newBuilder()
    .setSessionId(sessionId)
    .setAnchorSeqId(lastSeqId)  // 上次读到的位置
    .setDirection(Direction.FORWARD)
    .setLimit(50)
    .build();

GetMessagesResponse response = stub.getMessages(request);
```

## 总结

这套新架构通过 Micro-Batching 机制大幅提升了写入性能，同时保证了 seq_id 的严格递增和事务原子性。结合 Redis 缓存的 Gap Detection 策略，读写性能都得到了优化。系统设计充分考虑了扩展性和容错性，可支撑大规模 IM 场景。

