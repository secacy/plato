# 收件箱服务重构总结

## 概述

根据 `changes_note.md` 中的新设计，完成了收件箱服务的全面重构。主要变化包括：

1. **Redis 数据结构优化** - 从"用户维度"改为"读扩散"模式
2. **缓存策略增强** - 实现了缓存击穿、雪崩、穿透的完整防护
3. **数据库表结构优化** - 添加了快照字段，减少跨表查询
4. **置顶逻辑修正** - 使用 Magic Number 实现置顶会话的排序

---

## 一、Redis 数据结构变化

### 旧设计 ❌
```
- user_sessions:{user_id} (ZSET) - 用户会话列表
- session_meta:{user_id} (HASH) - 用户会话元数据
- unread_count:{user_id} (HASH) - 未读计数
```

### 新设计 ✅
```
- user_inbox_zset:{user_id} (ZSET) - 用户收件箱列表（仅存 ID 和 Score）
- user_inbox_meta:{user_id} (HASH) - 用户会话偏好（read_seq、is_pinned、is_muted）
- session_latest:{session_id} (STRING) - 会话全局快照（所有成员共享）
```

**核心优势：**
- 发消息时，只需更新 1 个 Key（`session_latest:{session_id}`），而不是 N 个用户的 Key
- 会话快照在所有群成员之间共享，大幅减少 Redis 写入量
- 未读数实时计算（`unread_count = maxSeq - readSeq`），无需单独存储

---

## 二、数据库表结构变化

### 1. inbox 表（用户收件箱）

**新增字段：**
```sql
`last_msg_time` BIGINT NOT NULL DEFAULT 0 COMMENT '最新消息时间(用于排序)'
```

**索引优化：**
```sql
INDEX `idx_user_view` (`user_id`, `is_pinned` DESC, `last_msg_time` DESC)
```

### 2. session_meta 表（会话元数据）

**新增字段：**
```sql
`last_msg_time` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '最新消息时间'
`last_msg_sender_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '发送者UID'
`last_msg_type` INT NOT NULL DEFAULT 1 COMMENT '消息类型'
`last_msg_content` VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '消息预览内容'
```

**作用：** 避免查询 `message` 表来获取会话预览信息

---

## 三、核心服务重构

### 1. 新增文件

| 文件 | 说明 |
|------|------|
| `SingleFlightService.java` | 防止缓存击穿的 SingleFlight 机制 |
| `dto/UserInboxMetaDto.java` | 用户会话偏好 DTO |
| `dto/SessionLatestDto.java` | 会话全局快照 DTO |

### 2. 重构文件

| 文件 | 主要变化 |
|------|---------|
| `RedisKeyConstants.java` | 更新 Redis Key 命名规则 |
| `InboxEntity.java` | 添加 `lastMsgTime` 字段 |
| `SessionMetaEntity.java` | 添加快照相关字段（4 个） |
| `InboxRedisService.java` | **完全重构**，实现新的 Redis 数据结构操作 |
| `InboxService.java` | **完全重构**，实现完整的缓存回源流程 |
| `InboxMapper.java` | 更新查询方法，支持新字段 |
| `SessionMetaMapper.java` | 添加 `updateSnapshot()` 方法 |
| `InboxMapper.xml` | 添加 `last_msg_time` 字段映射 |
| `SessionMetaMapper.xml` | 添加快照字段映射 |

---

## 四、核心流程实现

### 1. GetInboxes 完整流程 ✅

```
阶段一：获取会话 ID 列表
  1.1 Redis 查询：user_inbox_zset:{user_id}
  1.2 Hit → 进入阶段二
  1.3 Miss → 回源查询 inbox 表
      - 情况1 (Key不存在): 重建热数据窗口缓存（Top 100）
      - 情况2 (数据截断): 降级查询 TiDB，不回写 Redis

阶段二：获取会话详情（并行）
  2.1 批量获取 user_inbox_meta:{user_id}（用户偏好）
  2.2 批量获取 session_latest:{session_id}（会话快照）
  2.3 局部 Miss → 回源查询 inbox 表和 session_meta 表
  2.4 使用 SingleFlight 防止缓存击穿

阶段三：内存计算
  3.1 计算未读数：unread_count = maxSeq - readSeq
  3.2 填充预览信息
  3.3 返回响应
```

### 2. SetInboxRead（设置已读）✅

```
1. 更新 TiDB inbox 表的 last_read_seq_id
2. 更新 Redis user_inbox_meta:{user_id} 中的 readSeq
```

### 3. SetInboxAttributes（设置会话属性）✅

**置顶逻辑：**
```
1. 更新 TiDB 和 Redis Meta 中的 is_pinned
2. 更新 Redis ZSET Score：
   - 置顶：Score = last_msg_time + 1_000_000_000_000
   - 取消置顶：Score = last_msg_time（恢复真实时间）
```

**免打扰逻辑：**
```
1. 更新 TiDB 和 Redis Meta 中的 is_muted
（不需要更新 ZSET）
```

---

## 五、缓存策略增强

### 1. 缓存击穿防护 ✅

**问题：** 万人群的 `session_latest:{session_id}` 突然过期，1000 人同时拉列表

**解决方案：** SingleFlight 机制
```java
@Component
public class SingleFlightService {
    private final ConcurrentHashMap<String, CompletableFuture<Object>> taskCache;
    
    public <T> T execute(String key, Supplier<T> supplier) {
        // 同一时刻，同一个 Key 只发一次 DB 查询
        // 其他请求等待并共享结果
    }
}
```

### 2. 缓存雪崩防护 ✅

**问题：** 大量 Key 同时过期，DB 瞬时压力激增

**解决方案：** 随机过期时间
```java
private void setExpireWithJitter(String key, long baseTtl, TimeUnit timeUnit) {
    // 基础 TTL：7 天
    // 随机抖动：0~12 小时
    long jitterSeconds = ThreadLocalRandom.current().nextLong(0, 12 * 60 * 60);
    long totalSeconds = timeUnit.toSeconds(baseTtl) + jitterSeconds;
    redisTemplate.expire(key, totalSeconds, TimeUnit.SECONDS);
}
```

### 3. 缓存穿透防护 ✅

**问题：** 攻击者循环请求不存在的 `session_id`

**解决方案：** 缓存空对象
```java
// 如果 DB 查不到，设置空对象标记
public void setNullMarker(String type, Long id) {
    String key = RedisKeyConstants.nullMarkerKey(type, id);
    redisTemplate.opsForValue().set(key, "1", 60, TimeUnit.SECONDS);
}

// 下次查询时先检查标记
public boolean hasNullMarker(String type, Long id) {
    return redisTemplate.hasKey(RedisKeyConstants.nullMarkerKey(type, id));
}
```

---

## 六、性能优化点

### 1. 批量查询 Redis
```java
// 旧设计：N 次 Redis 查询
for (Long sessionId : sessionIds) {
    meta = redisTemplate.opsForHash().get(key, sessionId);
}

// 新设计：1 次批量查询
List<Object> values = redisTemplate.opsForHash().multiGet(key, fields);
```

### 2. 热数据窗口
```java
// 只缓存用户最近 100 个会话
private static final int HOT_DATA_WINDOW_SIZE = 100;

// 超出部分降级查询 DB，不回写 Redis
```

### 3. 减少跨表查询
```sql
-- 旧设计：需要 JOIN message 表获取预览
SELECT i.*, m.content, m.create_time 
FROM inbox i 
LEFT JOIN message m ON ...

-- 新设计：session_meta 表已冗余预览字段
SELECT * FROM session_meta WHERE session_id = ?
```

---

## 七、关键配置参数

| 参数 | 值 | 说明 |
|------|---|------|
| `PINNED_MAGIC_NUMBER` | 1,000,000,000,000 | 置顶会话的 Score 偏移量 |
| `HOT_DATA_WINDOW_SIZE` | 100 | 用户收件箱热数据窗口大小 |
| `USER_INBOX_ZSET_TTL_DAYS` | 7 | 用户收件箱 ZSET 过期时间 |
| `SESSION_LATEST_TTL_DAYS` | 7 | 会话快照过期时间 |
| `NULL_MARKER_TTL_SECONDS` | 60 | 空对象标记过期时间 |
| `JITTER_MAX_HOURS` | 12 | 随机抖动最大值（小时） |

---

## 八、迁移注意事项

### 1. 数据库表结构变更

**需要执行的 SQL：**
```sql
-- 修改 inbox 表
ALTER TABLE inbox ADD COLUMN `last_msg_time` BIGINT NOT NULL DEFAULT 0 COMMENT '最新消息时间';
ALTER TABLE inbox ADD INDEX `idx_user_view` (`user_id`, `is_pinned` DESC, `last_msg_time` DESC);

-- 修改 session_meta 表
ALTER TABLE session_meta ADD COLUMN `last_msg_time` BIGINT UNSIGNED NOT NULL DEFAULT 0;
ALTER TABLE session_meta ADD COLUMN `last_msg_sender_id` BIGINT UNSIGNED NOT NULL DEFAULT 0;
ALTER TABLE session_meta ADD COLUMN `last_msg_type` INT NOT NULL DEFAULT 1;
ALTER TABLE session_meta ADD COLUMN `last_msg_content` VARCHAR(1024) NOT NULL DEFAULT '';
```

### 2. Redis 数据迁移

**方案：** 采用"双写 + 自然过期"策略
1. 部署新代码，开始写入新的 Redis Key
2. 旧的 Key 自然过期（TTL）
3. 用户首次访问时，自动从 DB 重建新缓存

**无需手动迁移数据！**

### 3. 回滚方案

如果需要回滚：
1. 保留旧代码分支
2. 数据库表字段向后兼容（新增字段不影响旧代码）
3. Redis 数据自然过期，无影响

---

## 九、测试建议

### 1. 功能测试
- [ ] GetInboxes - Redis Hit 路径
- [ ] GetInboxes - Redis Miss 路径（Key 不存在）
- [ ] GetInboxes - 数据截断路径（limit > 100）
- [ ] SetInboxRead - 更新已读位置
- [ ] SetInboxAttributes - 置顶/取消置顶
- [ ] SetInboxAttributes - 免打扰/取消免打扰

### 2. 性能测试
- [ ] 并发读取会话列表（1000 QPS）
- [ ] 缓存全部失效场景（压测 DB）
- [ ] SingleFlight 防护效果验证

### 3. 异常测试
- [ ] Redis 不可用时的降级
- [ ] DB 慢查询时的超时处理
- [ ] 数据不一致时的兜底逻辑

---

## 十、监控指标建议

### 关键指标
1. **缓存命中率**
   - `user_inbox_zset` 命中率
   - `session_latest` 命中率
   - 目标：> 95%

2. **回源频率**
   - DB 查询 QPS
   - SingleFlight 归并率
   - 目标：< 5% 流量回源

3. **性能指标**
   - GetInboxes 接口 P99 延迟
   - Redis 批量查询耗时
   - 目标：P99 < 100ms

4. **异常监控**
   - 空对象标记命中次数（防穿透）
   - 缓存重建次数
   - DB 查询失败率

---

## 总结

本次重构完成了以下核心目标：

✅ **架构优化** - 从"写扩散 + 用户维度"改为"读扩散 + 共享快照"  
✅ **缓存完善** - 实现了击穿、雪崩、穿透的完整防护  
✅ **性能提升** - 批量查询 + 热数据窗口 + 减少跨表查询  
✅ **逻辑修正** - 置顶使用 Score 偏移，未读数实时计算  

**预期效果：**
- Redis 写入量减少 90%+（万人群场景）
- 接口 P99 延迟 < 100ms
- 缓存命中率 > 95%
- 系统可承受缓存全量失效场景

