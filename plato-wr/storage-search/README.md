# Storage Search Server

## 概述

`storage-search-server` 是 IM 消息存储系统的**独立搜索服务**，专门负责消息全文搜索功能。

## 为什么需要独立的搜索服务？

### 问题背景

SearchMessages (Elasticsearch) 具有以下特点：

1. **耗时不稳定**：简单查询可能 50ms，但复杂的 wildcard 或高亮查询可能飙升到 500ms 甚至 2s
2. **IO 密集**：线程大部分时间在等待 ES 返回
3. **资源竞争风险**：如果与核心接口共享线程池，可能导致以下灾难场景：

**灾难场景（未隔离）**:

```
假设 storage-server 的 gRPC 线程池有 200 个线程

1. ES 发生 Full GC 或网络抖动，查询延迟变高
2. 大量用户发起搜索请求，200 个线程全部堵在 "Waiting for ES" 状态
3. 新的 SaveMessage 请求进来（本来只需 5ms），但因为没有可用线程，
   它被放入队列等待，甚至超时报错

后果：因为搜索功能的抖动，导致全站用户无法发消息
      ⚠️ 这是绝对不可接受的 P0 级故障
```

### 解决方案：物理隔离

通过**独立微服务** `storage-search-server`：

- ✅ **物理隔离**：独立进程，独立部署
- ✅ **资源隔离**：独立线程池，独立内存
- ✅ **端口隔离**：9091（区别于 storage-server 的 9090）
- ✅ **故障隔离**：搜索服务宕机不影响核心发消息功能
- ✅ **扩展隔离**：搜索服务可独立扩容，按需分配资源

## 架构设计

### 服务拓扑

```
┌──────────────────────────────────────────────────────────┐
│                      客户端层                              │
│  (业务层通过 gRPC 调用存储层)                              │
└──────────────────────────────────────────────────────────┘
                            │
                            ├────────────┬───────────────┐
                            ↓            ↓               ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
│  storage     │  │  storage     │  │  storage-search  │
│  -server     │  │  -consumer   │  │  -server         │
│  (核心写读)   │  │  (异步处理)  │  │  (搜索服务)      │
│  Port: 9090  │  │              │  │  Port: 9091      │
└──────────────┘  └──────────────┘  └──────────────────┘
      │                   │                    │
      │                   │                    │
      ↓                   ↓                    ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
│  TiDB        │  │  Kafka       │  │  Elasticsearch   │
│  + Redis     │  │              │  │  + Redis         │
└──────────────┘  └──────────────┘  └──────────────────┘
```

### 职责划分

| 服务 | 职责 | 依赖 | 端口 |
|------|-----|------|------|
| `storage-server` | 核心读写（Save/GetMessages/Inbox） | TiDB, Redis | 9090 |
| `storage-consumer` | 异步扇出（写扩散/读扩散/搜索扇出） | TiDB, Redis, Kafka, ES | - |
| `storage-search-server` | 消息搜索（SearchMessages） | Elasticsearch, Redis | 9091 |

### 数据流

#### 写入流程

```
1. 客户端 → storage-server (SaveMessage)
2. storage-server → TiDB (写入消息)
3. TiDB → TiCDC → Kafka (变更推送)
4. storage-consumer 消费 Kafka 事件：
   ├─ Job A: 写扩散 (更新会话列表)
   ├─ Job B: 读扩散 (缓存到 Redis)
   └─ Job C: 搜索扇出 (同步到 Elasticsearch)
```

#### 搜索流程

```
1. 客户端 → storage-search-server (SearchMessages)
2. storage-search-server:
   ├─ 从 Redis 获取用户会话列表（全局搜索）
   └─ 查询 Elasticsearch (全文搜索)
3. storage-search-server → 客户端 (返回搜索结果)
```

## 核心功能

### 1. 搜索范围

- **会话内搜索** (`session_id`): 在指定会话中搜索消息
- **全局搜索** (`owner_id`): 搜索用户参与的所有会话

### 2. 搜索过滤器

- **消息类型**: 按消息类型过滤（文本、图片、文件等）
- **时间范围**: 按时间范围过滤
- **发送者**: 按发送者过滤
- **消息状态**: 自动过滤已撤回和已删除的消息

### 3. 搜索结果

- **高亮显示**: 返回带高亮标记的搜索片段
- **分页支持**: 支持分页查询
- **排序**: 按时间倒序排序
- **总数统计**: 返回总命中数

## 技术栈

- **框架**: Spring Boot 3.2.4
- **gRPC**: grpc-spring-boot-starter 3.1.0
- **搜索引擎**: Elasticsearch 8.x + IK 分词器
- **缓存**: Redis（获取用户会话列表）
- **序列化**: Protobuf

## 配置说明

### 端口配置

- **gRPC 端口**: 9091（独立于 storage-server 的 9090）
- **管理端口**: 19091（健康检查、指标监控）

### 线程池配置

```yaml
grpc:
  server:
    executor:
      core-pool-size: 10    # 核心线程数
      max-pool-size: 50     # 最大线程数
      queue-capacity: 200   # 队列容量
```

### Elasticsearch 配置

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    connection-timeout: 5s
    socket-timeout: 30s
```

## 快速开始

### 1. 启动依赖服务

```bash
# Elasticsearch
docker run -d --name elasticsearch -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  elasticsearch:8.11.0

# Redis
docker run -d --name redis -p 6379:6379 redis:7
```

### 2. 创建索引

```bash
curl -X PUT "http://localhost:9200/im_messages" \
  -H 'Content-Type: application/json' \
  -d @elasticsearch-index.json
```

### 3. 启动服务

```bash
cd storage-search
mvn spring-boot:run
```

### 4. 测试

```bash
# 健康检查
curl http://localhost:19091/actuator/health

# gRPC 调用（使用 grpcurl）
grpcurl -plaintext \
  -d '{"keyword":"项目","session_id":12345,"page_number":1,"page_size":20}' \
  localhost:9091 \
  com.plato.gateway.StorageSearchService/SearchMessages
```

## API 文档

### SearchMessages

**gRPC 方法**:

```protobuf
rpc SearchMessages(SearchMessagesRequest) returns (SearchMessagesResponse);
```

**请求示例**:

```protobuf
// 会话内搜索
{
  "keyword": "项目进度",
  "session_id": 12345,
  "page_number": 1,
  "page_size": 20
}

// 全局搜索
{
  "keyword": "重要文件",
  "owner_id": 100,
  "msg_types": [4],
  "page_number": 1,
  "page_size": 20
}
```

**响应示例**:

```protobuf
{
  "total_hits": 25,
  "items": [
    {
      "msg_id": 123,
      "session_id": 12345,
      "seq_id": 100,
      "sender_id": 1001,
      "create_time_ms": 1700000000000,
      "highlight_content": "这是一个测试消息，包含<em>项目</em>关键词",
      "msg_type": 1
    }
  ]
}
```

## 监控指标

### 关键指标

- **搜索 QPS**: 每秒查询数
- **搜索延迟**: P50, P95, P99 延迟
- **ES 连接状态**: Elasticsearch 连接健康状态
- **线程池使用率**: 当前活跃线程数 / 最大线程数

### 查看指标

```bash
# Prometheus 指标
curl http://localhost:19091/actuator/prometheus

# 健康检查
curl http://localhost:19091/actuator/health
```

## 性能调优

### 1. 线程池调优

根据实际 QPS 调整线程池大小：

```yaml
grpc:
  server:
    executor:
      core-pool-size: 20      # 调整为 QPS / 10
      max-pool-size: 100      # 调整为 QPS / 2
```

### 2. Elasticsearch 调优

- **索引刷新间隔**: 调整 `refresh_interval` 提升写入性能
- **分片数量**: 根据数据量调整分片数
- **副本数量**: 生产环境至少 1 个副本

### 3. 查询优化

- 使用 `filter` 代替 `must`（不需要评分时）
- 限制高亮片段大小
- 合理设置分页大小（建议 ≤ 100）

## 故障处理

### Elasticsearch 宕机

**现象**: 搜索请求全部失败

**影响范围**: 仅影响搜索功能，**不影响核心发消息功能**

**恢复方案**:

1. 重启 Elasticsearch
2. 检查索引健康状态
3. 如有数据丢失，从 TiDB 重建索引

### 搜索服务宕机

**现象**: 搜索接口不可用

**影响范围**: 仅影响搜索功能，**不影响核心发消息功能**

**恢复方案**:

1. 重启 storage-search-server
2. 检查依赖服务（ES、Redis）状态
3. 查看日志排查问题

## 相关文档

- [Elasticsearch 部署指南](../ELASTICSEARCH_SETUP.md)
- [快速开始指南](../ELASTICSEARCH_QUICK_START.md)
- [实现文档](../ELASTICSEARCH_IMPLEMENTATION.md)
- [系统架构](../storage-consumer/ARCHITECTURE.md)

## 联系方式

如有问题，请联系开发团队。

---

**服务名称**: storage-search-server  
**版本**: 1.0.0  
**最后更新**: 2025/11/20

