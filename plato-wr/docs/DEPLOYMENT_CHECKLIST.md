# 收件箱服务重构 - 部署检查清单

## 📋 部署前检查

### 1. 代码变更确认
- [x] ✅ 所有新代码已提交并通过 Code Review
- [x] ✅ 单元测试已通过（无 Lint 错误）
- [ ] 集成测试已完成
- [ ] 性能测试已完成（可选，建议先在测试环境验证）

### 2. 数据库变更准备
- [ ] 已备份生产数据库
- [ ] 已在测试环境执行迁移脚本 `migration_v2.sql`
- [ ] 已验证迁移脚本的执行时间（应该很快，因为是在线 DDL）
- [ ] 已确认 DBA 可用时间窗口（建议低峰期执行）

### 3. Redis 准备
- [ ] 确认 Redis 集群健康状态
- [ ] 确认 Redis 有足够的内存空间
- [ ] 准备监控 Redis Key 数量变化

### 4. 配置检查
- [ ] 确认 Redis 连接配置正确
- [ ] 确认数据库连接池配置合理
- [ ] 确认日志级别设置为 INFO（首次部署可设置为 DEBUG）

---

## 🚀 部署步骤

### 阶段一：数据库迁移（5-10 分钟）

#### Step 1: 备份数据库
```bash
# 备份关键表
mysqldump -h <host> -u <user> -p im_storage inbox session_meta > backup_before_migration.sql
```

#### Step 2: 执行迁移脚本
```bash
mysql -h <host> -u <user> -p im_storage < src/main/resources/db/migration_v2.sql
```

#### Step 3: 验证迁移结果
```sql
-- 检查 inbox 表
DESC inbox;
SHOW INDEX FROM inbox WHERE Key_name = 'idx_user_view';

-- 检查 session_meta 表
DESC session_meta;

-- 验证数据
SELECT COUNT(*) FROM inbox WHERE last_msg_time IS NULL;
SELECT COUNT(*) FROM session_meta WHERE last_msg_time IS NULL;
```

**预期结果：**
- ✅ `inbox` 表新增 `last_msg_time` 字段
- ✅ `inbox` 表新增 `idx_user_view` 索引
- ✅ `session_meta` 表新增 4 个快照字段
- ✅ 所有字段均不为 NULL

---

### 阶段二：应用部署（灰度发布）

#### Step 1: 灰度环境部署
```bash
# 构建项目
mvn clean package -DskipTests

# 部署到灰度服务器（1-2 台）
scp target/storage-server.jar user@gray-server:/path/to/app/
ssh user@gray-server "systemctl restart storage-server"
```

#### Step 2: 验证灰度环境
```bash
# 检查服务启动
curl http://gray-server:8080/health

# 检查日志
tail -f /var/log/storage-server/application.log | grep "GetInboxes"
```

**关键日志关键词：**
- `GetInboxes start` - 请求开始
- `hit Redis` - 缓存命中
- `Redis miss, fallback to DB` - 缓存未命中
- `Rebuilding user inbox ZSET` - 重建缓存
- `GetInboxes success` - 请求成功

#### Step 3: 功能测试
```bash
# 使用 grpcurl 测试（需要安装 grpcurl）
grpcurl -plaintext \
  -d '{"user_id": 1001, "limit": 20}' \
  gray-server:9090 \
  gateway.StorageInboxService/GetInboxes
```

**预期结果：**
- ✅ 返回会话列表
- ✅ 未读数计算正确
- ✅ 置顶会话排在前面
- ✅ 接口响应时间 < 100ms

#### Step 4: 观察指标（灰度期：2-4 小时）
```sql
-- 监控 Redis Key 数量
redis-cli INFO keyspace

-- 监控慢查询
SELECT * FROM information_schema.processlist 
WHERE db = 'im_storage' AND TIME > 1;
```

**关键指标：**
- Redis Key 数量：应该逐步增长（新用户请求时创建）
- 数据库 QPS：应该较低（大部分请求命中缓存）
- 接口 P99 延迟：< 100ms
- 错误率：0%

---

### 阶段三：全量部署

#### Step 1: 全量发布
```bash
# 逐台重启（建议使用自动化部署工具）
for server in server1 server2 server3; do
    echo "Deploying to $server..."
    scp target/storage-server.jar user@$server:/path/to/app/
    ssh user@$server "systemctl restart storage-server"
    sleep 30  # 等待服务启动
done
```

#### Step 2: 监控告警
- [ ] 监控接口成功率（应该 > 99.9%）
- [ ] 监控接口延迟（P99 < 100ms）
- [ ] 监控数据库慢查询（应该很少）
- [ ] 监控 Redis 内存使用率（应该缓慢增长）

---

## 🔍 验证清单

### 功能验证
- [ ] **GetInboxes** - 拉取会话列表成功
- [ ] **GetInboxes** - 未读数计算正确
- [ ] **GetInboxes** - 置顶会话排序正确
- [ ] **GetInboxes** - 分页功能正常
- [ ] **SetInboxRead** - 设置已读成功，未读数变化正确
- [ ] **SetInboxAttributes (Pinned)** - 置顶成功，会话移到列表顶部
- [ ] **SetInboxAttributes (Unpinned)** - 取消置顶成功，会话恢复正常排序
- [ ] **SetInboxAttributes (Muted)** - 免打扰设置成功

### 性能验证
- [ ] 缓存命中率 > 95%
- [ ] 接口 P99 延迟 < 100ms
- [ ] 数据库回源 QPS < 总 QPS 的 5%
- [ ] Redis 内存增长合理

### 异常验证
- [ ] Redis 临时不可用时，服务可降级到数据库查询
- [ ] 数据库慢查询时，接口不超时（设置合理超时时间）
- [ ] 缓存全部失效时，系统可承受（SingleFlight 保护生效）

---

## 📊 监控指标（持续观察 24-48 小时）

### 业务指标
| 指标 | 目标值 | 当前值 | 状态 |
|------|--------|--------|------|
| GetInboxes 成功率 | > 99.9% | - | - |
| GetInboxes P99 延迟 | < 100ms | - | - |
| SetInboxRead 成功率 | > 99.9% | - | - |
| SetInboxAttributes 成功率 | > 99.9% | - | - |

### 缓存指标
| 指标 | 目标值 | 当前值 | 状态 |
|------|--------|--------|------|
| user_inbox_zset 命中率 | > 95% | - | - |
| session_latest 命中率 | > 95% | - | - |
| Redis 内存使用率 | < 80% | - | - |

### 数据库指标
| 指标 | 目标值 | 当前值 | 状态 |
|------|--------|--------|------|
| inbox 表查询 QPS | < 100 | - | - |
| session_meta 表查询 QPS | < 200 | - | - |
| 慢查询数量（> 1s） | 0 | - | - |

---

## 🆘 回滚方案

如果出现严重问题，可以快速回滚：

### Step 1: 停止新版本服务
```bash
for server in server1 server2 server3; do
    ssh user@$server "systemctl stop storage-server"
done
```

### Step 2: 部署旧版本代码
```bash
for server in server1 server2 server3; do
    ssh user@$server "mv /path/to/app/storage-server.jar.backup /path/to/app/storage-server.jar"
    ssh user@$server "systemctl start storage-server"
done
```

### Step 3: 清理 Redis 新 Key（可选）
```bash
# 删除新增的 Redis Key（旧版本不会使用这些 Key）
redis-cli --scan --pattern "user_inbox_zset:*" | xargs redis-cli DEL
redis-cli --scan --pattern "user_inbox_meta:*" | xargs redis-cli DEL
redis-cli --scan --pattern "session_latest:*" | xargs redis-cli DEL
```

**注意：**
- 数据库表结构变更是向后兼容的，不需要回滚
- 新增的字段对旧代码无影响

---

## 📝 上线后任务

### 短期任务（1 周内）
- [ ] 每天检查错误日志，确认无异常
- [ ] 收集用户反馈，确认功能正常
- [ ] 持续观察性能指标，确保稳定

### 中期任务（1 个月内）
- [ ] 回填历史数据的 `last_msg_time` 字段（可选）
- [ ] 优化缓存过期时间策略
- [ ] 编写性能优化报告

### 长期任务
- [ ] 清理旧的 Redis Key（如果确认不再使用）
- [ ] 优化数据库索引（根据慢查询日志）
- [ ] 迭代缓存策略（根据实际命中率）

---

## ✅ 部署完成确认

部署完成后，请在此确认：

- [ ] 数据库迁移成功
- [ ] 应用全量部署成功
- [ ] 所有功能验证通过
- [ ] 所有性能指标达标
- [ ] 监控告警配置完成
- [ ] 团队成员已知晓变更内容

**部署完成时间：** _______________  
**部署负责人：** _______________  
**复核人：** _______________  

---

## 📞 联系方式

如有问题，请联系：
- **开发负责人：** [姓名] - [邮箱/电话]
- **DBA：** [姓名] - [邮箱/电话]
- **运维：** [姓名] - [邮箱/电话]

---

**祝部署顺利！🎉**

