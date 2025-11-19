# Storage API 项目结构

## 目录结构

```
storage-api/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/plato/api/
│   │   │       ├── StorageApiApplication.java      # 启动类
│   │   │       │
│   │   │       ├── config/                         # 配置类
│   │   │       │   ├── IdGeneratorConfig.java      # ID生成器配置
│   │   │       │   └── KafkaConfig.java            # Kafka配置
│   │   │       │
│   │   │       ├── grpc/                           # gRPC服务实现
│   │   │       │   ├── StorageMessageGrpcService.java       # 消息服务
│   │   │       │   ├── StorageInboxGrpcService.java         # 收件箱服务
│   │   │       │   ├── StorageGroupGrpcService.java         # 群组服务
│   │   │       │   ├── StorageGroupInternalGrpcService.java # 内部群组服务
│   │   │       │   └── StorageGovernanceGrpcService.java    # 治理服务
│   │   │       │
│   │   │       ├── service/                        # 业务逻辑层
│   │   │       │   ├── MessageService.java         # 消息业务逻辑
│   │   │       │   ├── InboxService.java           # 收件箱业务逻辑
│   │   │       │   └── GroupMemberService.java     # 群成员业务逻辑
│   │   │       │
│   │   │       ├── repository/                     # 数据访问层
│   │   │       │   ├── entity/                     # 实体类
│   │   │       │   │   ├── MessageEntity.java      # 消息实体
│   │   │       │   │   ├── MemberEntity.java       # 成员实体
│   │   │       │   │   └── InboxEntity.java        # 收件箱实体
│   │   │       │   │
│   │   │       │   └── mapper/                     # MyBatis Mapper
│   │   │       │       ├── MessageMapper.java      # 消息Mapper
│   │   │       │       ├── MemberMapper.java       # 成员Mapper
│   │   │       │       └── InboxMapper.java        # 收件箱Mapper
│   │   │       │
│   │   │       ├── redis/                          # Redis服务层
│   │   │       │   ├── RedisKeyConstants.java      # Redis Key常量
│   │   │       │   ├── SeqIdGenerator.java         # SeqID生成器
│   │   │       │   ├── InboxRedisService.java      # 收件箱Redis服务
│   │   │       │   ├── GroupMemberRedisService.java # 群成员Redis服务
│   │   │       │   └── IdempotentService.java      # 幂等服务
│   │   │       │
│   │   │       ├── producer/                       # Kafka生产者
│   │   │       │   └── StorageEventProducer.java   # 事件生产者
│   │   │       │
│   │   │       ├── event/                          # 事件模型
│   │   │       │   ├── MessageEvent.java           # 消息事件
│   │   │       │   └── GroupMemberEvent.java       # 群成员事件
│   │   │       │
│   │   │       └── controller/                     # 控制器（健康检查等）
│   │   │           └── HealthController.java       # 健康检查
│   │   │
│   │   └── resources/
│   │       └── application.yml                     # 配置文件
│   │
│   └── test/                                       # 测试代码
│       └── java/
│
├── pom.xml                                         # Maven配置
├── README.md                                       # 项目说明
├── ARCHITECTURE.md                                 # 架构设计文档
└── PROJECT_STRUCTURE.md                            # 本文件
```

## 分层架构

### 1. gRPC服务层 (`grpc/`)

**职责**：实现gRPC接口，处理请求响应

**关键类**：
- `StorageMessageGrpcService`: 消息核心服务（保存、查询、状态更新）
- `StorageInboxGrpcService`: 收件箱服务（会话列表、已读设置）
- `StorageGroupGrpcService`: 群组服务（成员同步）

**设计原则**：
- 薄层设计，只做参数解析和响应组装
- 异常统一处理
- 日志记录

### 2. 业务逻辑层 (`service/`)

**职责**：核心业务逻辑处理

**关键类**：
- `MessageService`: 消息保存、查询、状态更新逻辑
- `InboxService`: 会话列表管理、已读标记
- `GroupMemberService`: 群成员CRUD、缓存管理

**设计原则**：
- 事务边界控制
- 缓存策略
- 业务规则验证

### 3. 数据访问层 (`repository/`)

**职责**：数据库操作封装

**entity/**：数据库表实体映射
- `MessageEntity`: 对应 `message` 表
- `MemberEntity`: 对应 `member` 表
- `InboxEntity`: 对应 `inbox` 表

**mapper/**：MyBatis Mapper接口
- 使用注解SQL简化开发
- 自定义复杂查询方法

**设计原则**：
- 实体与表一一对应
- Mapper只负责CRUD
- 避免业务逻辑

### 4. Redis服务层 (`redis/`)

**职责**：Redis操作封装

**关键类**：
- `SeqIdGenerator`: 基于Redis INCR生成SeqID
- `InboxRedisService`: 收件箱热数据管理
- `GroupMemberRedisService`: 群成员缓存
- `IdempotentService`: 幂等去重

**设计原则**：
- Key命名规范化
- TTL合理设置
- 异常降级处理

### 5. 事件驱动层 (`producer/` + `event/`)

**职责**：异步事件发送

**event/**：事件模型定义
- `MessageEvent`: 消息相关事件（保存、更新、撤回）
- `GroupMemberEvent`: 群成员变更事件

**producer/**：Kafka生产者
- `StorageEventProducer`: 统一事件发送入口

**设计原则**：
- 事件结构化
- 发送异步化
- 失败重试

## 依赖关系

```
grpc/
  ↓ 依赖
service/
  ↓ 依赖
┌────────────┬────────────┬────────────┐
repository/  redis/       producer/
                            ↓ 使用
                          event/
```

**说明**：
- gRPC层只依赖Service层
- Service层可依赖Repository、Redis、Producer
- 各层之间低耦合，便于测试和维护

## 配置管理

### application.yml

```yaml
spring:
  application:
    name: storage-api
  
  # 数据源配置
  datasource:
    url: jdbc:mysql://localhost:4000/im_storage
    
  # Redis配置
  data:
    redis:
      host: localhost
      
  # Kafka配置
  kafka:
    bootstrap-servers: localhost:9092

# gRPC配置
grpc:
  server:
    port: 9090
```

## 核心流程

### 1. 消息保存流程

```
Client → gRPC
  ↓
StorageMessageGrpcService.saveMessage()
  ↓
MessageService.saveMessage()
  ├─ IdempotentService.checkIdempotent()       # 幂等检查
  ├─ IdGenerator.gen()                         # 生成msgId
  ├─ SeqIdGenerator.generateSeqId()            # 生成seqId
  ├─ IdempotentService.recordIdempotent()      # 记录幂等
  └─ StorageEventProducer.sendMessageEvent()   # 发送Kafka
  ↓
Return (msgId, seqId)
```

### 2. 消息查询流程

```
Client → gRPC
  ↓
StorageMessageGrpcService.getMessages()
  ↓
MessageService.getMessages()
  ├─ MessageMapper.selectBackward()  # 或 selectForward()
  └─ toProtoMessage()                # 实体转Proto
  ↓
Return List<Message>
```

### 3. 群成员同步流程

```
Client → gRPC
  ↓
StorageGroupGrpcService.upsertGroupMembers()
  ↓
GroupMemberService.upsertGroupMembers()
  ├─ @Transactional
  │   ├─ MemberMapper.insert()                    # TiDB写入
  │   ├─ GroupMemberRedisService.addMembers()     # Redis更新
  │   └─ StorageEventProducer.sendGroupMemberEvent() # Kafka兜底
  └─ COMMIT
  ↓
Return Success
```

## 扩展点

### 1. 新增gRPC服务

```java
@GrpcService
public class CustomGrpcService extends CustomServiceGrpc.CustomServiceImplBase {
    // 实现逻辑
}
```

### 2. 新增事件类型

```java
// 1. 定义事件
@Data
public class CustomEvent {
    private EventType eventType;
    // 字段...
}

// 2. Producer添加发送方法
public void sendCustomEvent(CustomEvent event) {
    // 发送逻辑
}
```

### 3. 新增Redis缓存

```java
@Service
public class CustomRedisService {
    // Key定义
    private String customKey(Long id) {
        return "custom:" + id;
    }
    
    // 操作方法
    public void cache(Long id, String value) {
        // 缓存逻辑
    }
}
```

## 代码规范

### 命名规范

- **类名**：大驼峰，如 `MessageService`
- **方法名**：小驼峰，如 `saveMessage()`
- **常量**：全大写下划线，如 `MAX_RETRY_COUNT`
- **包名**：全小写，如 `com.plato.api.service`

### 日志规范

```java
// INFO: 关键业务流程
log.info("Message saved, msgId={}, seqId={}", msgId, seqId);

// DEBUG: 详细调试信息
log.debug("Cache hit for sessionId={}", sessionId);

// WARN: 异常但不影响主流程
log.warn("Redis cache miss, fallback to DB");

// ERROR: 错误异常
log.error("Failed to save message", exception);
```

### 异常处理

```java
// 业务异常
throw new BusinessException("Invalid session");

// 系统异常
try {
    // 操作
} catch (Exception e) {
    log.error("Operation failed", e);
    throw new RuntimeException("System error", e);
}
```

## 测试策略

### 单元测试

```java
@SpringBootTest
class MessageServiceTest {
    @Test
    void testSaveMessage() {
        // 测试逻辑
    }
}
```

### 集成测试

```java
@TestContainers
class IntegrationTest {
    @Container
    static GenericContainer redis = new GenericContainer("redis:7");
    
    // 测试逻辑
}
```

## 部署运维

### 构建

```bash
mvn clean package
```

### 运行

```bash
java -jar target/storage-api-1.0-SNAPSHOT.jar
```

### 健康检查

```bash
curl http://localhost:8080/actuator/health
```

## 性能调优

### JVM参数

```bash
java -Xms2g -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar storage-api.jar
```

### 连接池调优

- **HikariCP**: maximum-pool-size = CPU核心数 * 2
- **Lettuce**: max-active = 业务并发数 / 实例数

### 缓存调优

- **热点数据**: TTL设置为1小时
- **冷数据**: 不缓存，直接查DB
- **缓存预热**: 启动时加载高频数据

