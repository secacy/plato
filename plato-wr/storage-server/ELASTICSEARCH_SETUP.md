# Elasticsearch 消息搜索功能部署指南

## 概述

本文档说明如何部署和配置 Elasticsearch 消息搜索功能。

## 架构说明

### 数据流

```
TiDB (t_messages) 
    ↓ (TiCDC监听)
Kafka (MsgEvents Topic)
    ↓ (storage-consumer消费)
Elasticsearch (im_messages索引)
    ↑ (storage-server查询)
客户端 (搜索请求)
```

### 核心组件

1. **storage-server**
   - `StorageSearchGrpcService`: gRPC 搜索接口
   - `SearchService`: 搜索业务逻辑
   - `MessageDocument`: ES 文档模型
   - `MessageSearchRepository`: ES Repository

2. **storage-consumer**
   - `SearchFanoutService`: 消息同步到 ES (Job C)
   - `MessageEventListener`: Kafka 事件监听器

## 部署步骤

### 1. 安装 Elasticsearch

#### 使用 Docker

```bash
# 拉取 Elasticsearch 镜像 (8.x)
docker pull elasticsearch:8.11.0

# 启动 Elasticsearch（开发环境，关闭安全认证）
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms1g -Xmx1g" \
  elasticsearch:8.11.0

# 验证安装
curl http://localhost:9200
```

#### 生产环境配置

```bash
# 启动 Elasticsearch（生产环境，启用安全认证）
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=true" \
  -e "ELASTIC_PASSWORD=your_password" \
  -e "ES_JAVA_OPTS=-Xms4g -Xmx4g" \
  -v es-data:/usr/share/elasticsearch/data \
  elasticsearch:8.11.0
```

### 2. 安装 IK 分词器（支持中文分词）

```bash
# 进入容器
docker exec -it elasticsearch bash

# 安装 IK 分词器（版本需与 ES 版本匹配）
./bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.11.0/elasticsearch-analysis-ik-8.11.0.zip

# 退出容器
exit

# 重启 Elasticsearch
docker restart elasticsearch
```

### 3. 创建 Elasticsearch 索引

```bash
# 创建索引（带 IK 分词器配置）
curl -X PUT "http://localhost:9200/im_messages" -H 'Content-Type: application/json' -d'
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "analysis": {
      "analyzer": {
        "ik_max_word": {
          "type": "custom",
          "tokenizer": "ik_max_word"
        },
        "ik_smart": {
          "type": "custom",
          "tokenizer": "ik_smart"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "msgId": {
        "type": "long"
      },
      "sessionId": {
        "type": "long"
      },
      "seqId": {
        "type": "long"
      },
      "senderId": {
        "type": "long"
      },
      "msgType": {
        "type": "integer"
      },
      "content": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart",
        "fields": {
          "keyword": {
            "type": "keyword",
            "ignore_above": 256
          }
        }
      },
      "status": {
        "type": "integer"
      },
      "createTimeMs": {
        "type": "long"
      },
      "indexedTimeMs": {
        "type": "long"
      }
    }
  }
}
'

# 验证索引创建
curl -X GET "http://localhost:9200/im_messages"
```

### 4. 配置应用

#### storage-server/application-dev.yaml

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: elastic  # 如果启用了安全认证
    password: your_password  # 如果启用了安全认证
```

#### storage-consumer/application-dev.yaml

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: elastic  # 如果启用了安全认证
    password: your_password  # 如果启用了安全认证
```

### 5. 编译 Proto 文件

```bash
# 进入 storage-proto 目录
cd storage-proto

# 编译 proto 文件
mvn clean compile

# 安装到本地仓库
mvn clean install
```

### 6. 启动服务

```bash
# 启动 storage-server
cd storage-server
mvn spring-boot:run

# 启动 storage-consumer
cd storage-consumer
mvn spring-boot:run
```

## 测试

### 1. 测试 IK 分词器

```bash
curl -X POST "http://localhost:9200/im_messages/_analyze" -H 'Content-Type: application/json' -d'
{
  "analyzer": "ik_max_word",
  "text": "我爱北京天安门"
}
'
```

### 2. 测试搜索功能

使用 gRPC 客户端调用搜索接口：

```java
// 会话内搜索
SearchMessagesRequest request = SearchMessagesRequest.newBuilder()
    .setKeyword("项目")
    .setSessionId(12345L)
    .setPageNumber(1)
    .setPageSize(20)
    .build();

SearchMessagesResponse response = searchServiceStub.searchMessages(request);

// 全局搜索
SearchMessagesRequest globalRequest = SearchMessagesRequest.newBuilder()
    .setKeyword("项目")
    .setOwnerId(100L)
    .setPageNumber(1)
    .setPageSize(20)
    .build();

SearchMessagesResponse globalResponse = searchServiceStub.searchMessages(globalRequest);
```

### 3. 手动添加测试数据

```bash
curl -X POST "http://localhost:9200/im_messages/_doc/1" -H 'Content-Type: application/json' -d'
{
  "msgId": 1,
  "sessionId": 12345,
  "seqId": 100,
  "senderId": 1001,
  "msgType": 1,
  "content": "这是一个测试消息，包含项目关键词",
  "status": 0,
  "createTimeMs": 1700000000000,
  "indexedTimeMs": 1700000000000
}
'

# 搜索测试
curl -X GET "http://localhost:9200/im_messages/_search" -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "content": "项目"
          }
        }
      ],
      "filter": [
        {
          "term": {
            "sessionId": 12345
          }
        },
        {
          "term": {
            "status": 0
          }
        }
      ]
    }
  },
  "highlight": {
    "fields": {
      "content": {}
    }
  }
}
'
```

## 监控与维护

### 1. 查看索引状态

```bash
# 查看所有索引
curl -X GET "http://localhost:9200/_cat/indices?v"

# 查看索引详情
curl -X GET "http://localhost:9200/im_messages/_stats"

# 查看索引映射
curl -X GET "http://localhost:9200/im_messages/_mapping"
```

### 2. 查看搜索性能

```bash
# 查看慢查询日志
curl -X GET "http://localhost:9200/im_messages/_settings"

# 设置慢查询阈值
curl -X PUT "http://localhost:9200/im_messages/_settings" -H 'Content-Type: application/json' -d'
{
  "index.search.slowlog.threshold.query.warn": "2s",
  "index.search.slowlog.threshold.query.info": "1s"
}
'
```

### 3. 索引维护

```bash
# 刷新索引（使新文档立即可搜索）
curl -X POST "http://localhost:9200/im_messages/_refresh"

# 强制合并段（定期执行，提升查询性能）
curl -X POST "http://localhost:9200/im_messages/_forcemerge?max_num_segments=1"

# 清理缓存
curl -X POST "http://localhost:9200/im_messages/_cache/clear"
```

### 4. 数据备份

```bash
# 创建快照仓库
curl -X PUT "http://localhost:9200/_snapshot/my_backup" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/mount/backups/my_backup"
  }
}
'

# 创建快照
curl -X PUT "http://localhost:9200/_snapshot/my_backup/snapshot_1?wait_for_completion=true" -H 'Content-Type: application/json' -d'
{
  "indices": "im_messages",
  "ignore_unavailable": true,
  "include_global_state": false
}
'
```

## 故障排查

### 1. 服务连接失败

```bash
# 检查 Elasticsearch 是否运行
curl http://localhost:9200

# 检查日志
docker logs elasticsearch

# 检查网络
telnet localhost 9200
```

### 2. 搜索结果为空

1. 检查索引是否存在：`curl -X GET "http://localhost:9200/_cat/indices?v"`
2. 检查文档数量：`curl -X GET "http://localhost:9200/im_messages/_count"`
3. 检查 storage-consumer 日志，确认消息是否正常同步到 ES

### 3. 中文分词不生效

1. 确认 IK 分词器已安装：`./bin/elasticsearch-plugin list`
2. 测试分词器：使用上面的测试命令
3. 重建索引（如果之前创建索引时未配置 IK）

## 性能优化

### 1. 索引优化

- **分片数量**：根据数据量调整，建议每个分片 20-50GB
- **副本数量**：生产环境至少 1 个副本
- **刷新间隔**：默认 1s，可调整为 5s 或 30s 提升写入性能

```bash
curl -X PUT "http://localhost:9200/im_messages/_settings" -H 'Content-Type: application/json' -d'
{
  "index": {
    "refresh_interval": "30s",
    "number_of_replicas": 2
  }
}
'
```

### 2. 查询优化

- 使用 `filter` 代替 `must`（不需要评分时）
- 限制高亮片段大小
- 合理设置分页大小（建议 ≤ 100）
- 使用 `search_after` 代替深度分页

### 3. 硬件配置

- **内存**：至少 4GB，建议 16GB+
- **CPU**：至少 2 核，建议 4 核+
- **磁盘**：使用 SSD，预留足够空间

## 扩展建议

### 1. 集群部署

生产环境建议部署 Elasticsearch 集群（至少 3 个节点）。

### 2. 索引生命周期管理

对于历史消息，可以使用 ILM（Index Lifecycle Management）自动归档。

### 3. 搜索建议

可以添加搜索建议功能（Suggester），提升用户体验。

### 4. 同义词支持

配置同义词词典，支持更智能的搜索。

## 参考文档

- [Elasticsearch 官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [IK 分词器](https://github.com/medcl/elasticsearch-analysis-ik)
- [Spring Data Elasticsearch](https://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/)

