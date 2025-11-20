package com.plato;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * 消息存储结构
 * 这是 Storage 接口的内存实现
 *
 * @since 2025/10/22 23:45
 * @className MessageStorage
 * @author hc
 */
public class MessageStorage implements Storage {
    /**
     * 使用可重入锁来模拟 Go 代码中的 sync.Mutex，实现对 stores 的线程安全访问
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 核心存储
     */
    private final Map<String, MessageStoreItem> stores;
    /**
     * 默认数据存活时间
     */
    private final Duration defaultTTL;

    /**
     * 内部类，用于存储每个键的实际数据和策略
     */
    private static class MessageStoreItem {
        List<Object> values;
        Duration ttl;
        int capacity;
        EvictPolicy evictPolicy;
        Instant expiry; // 过期时间点

        MessageStoreItem(Duration ttl, int capacity, EvictPolicy evictPolicy) {
            this.values = new ArrayList<>();
            this.ttl = ttl;
            this.capacity = capacity;
            this.evictPolicy = evictPolicy;
            // 初始 expiry 为 null 或 Instant.EPOCH，表示尚未设置
            this.expiry = null;
        }
    }

    /**
     * 初始化消息存储
     * @param defaultTTL 默认的数据存活时间
     */
    public MessageStorage(Duration defaultTTL) {
        this.stores = new HashMap<>();
        this.defaultTTL = defaultTTL;
    }

    @Override
    public void init() throws Exception {
        // 可以在这里进行连接分布式存储系统的初始化操作
        // 对于内存实现，这里不需要做什么
        System.out.println("Storage initialized.");
    }

    @Override
    public void batchAppend(String key, List<Object> values, Consumer<StorageStrategy> strategyFunc) throws Exception {
        // 1. 设置策略
        StorageStrategy strategy = new StorageStrategy(
                this.defaultTTL,
                1000, // 默认容量
                EvictPolicy.EVICT_BY_TTL // 默认按时间淘汰
        );

        if (strategyFunc != null) {
            strategyFunc.accept(strategy);
        }

        // 2. 加锁
        lock.lock();
        try {
            // 3. 获取或创建存储项
            MessageStoreItem item = stores.get(key);
            if (item == null) {
                item = new MessageStoreItem(
                        strategy.ttl,
                        strategy.capacity,
                        strategy.evictPolicy
                );
                stores.put(key, item);
            }

            // 4. 应用淘汰策略
            switch (item.evictPolicy) {
                case EVICT_BY_TTL:
                    // 检查是否过期
                    // 仅当 expiry 已设置 (非 null) 且当前时间在 expiry 之后
                    if (item.expiry != null && Instant.now().isAfter(item.expiry)) {
                        item.values.clear(); // 清空过期数据
                        // 重新设置过期时间
                        item.expiry = Instant.now().plus(item.ttl);
                    }
                    break;
                case EVICT_BY_CAPACITY:
                    // 检查是否超过容量限制
                    // (Go 代码的逻辑是保留后 capacity/2 的元素)
                    if (item.values.size() >= item.capacity) {
                        // 移除旧消息 (RingBuffer机制)
                        // 这段逻辑完全复制 Go 代码的行为
                        int startIndex = item.values.size() - (item.capacity / 2);
                        if (startIndex < 0) {
                            startIndex = 0;
                        }
                        // 创建一个新的子列表并替换
                        item.values = new ArrayList<>(item.values.subList(startIndex, item.values.size()));
                    }
                    break;
                case EVICT_NEVER:
                    // 不进行任何淘汰操作
                    break;
            }

            // 5. 追加新消息
            item.values.addAll(values);

            // 6. 设置过期时间（仅在按时间淘汰策略下有效)
            if (item.evictPolicy == EvictPolicy.EVICT_BY_TTL) {
                item.expiry = Instant.now().plus(item.ttl);
            }

        } finally {
            // 7. 解锁
            lock.unlock();
        }
    }

    @Override
    public Map<String, List<Object>> mGet(List<String> keys) throws Exception {
        Map<String, List<Object>> result = new HashMap<>();

        lock.lock();
        try {
            for (String key : keys) {
                MessageStoreItem item = stores.get(key);
                if (item != null) {
                    // 检查是否过期（仅在按时间淘汰策略下有效）
                    // 这是一种"惰性删除"
                    if (item.evictPolicy == EvictPolicy.EVICT_BY_TTL &&
                            item.expiry != null &&
                            Instant.now().isAfter(item.expiry)) {

                        item.values.clear(); // 清空过期数据
                    }
                    // 返回一个数据的副本，防止外部修改
                    result.put(key, new ArrayList<>(item.values));
                } else {
                    // 严格遵循 Go 代码的逻辑：任何一个 key 找不到，就立即返回错误
                    throw new Exception("key not found: " + key);
                }
            }
        } finally {
            lock.unlock();
        }

        return result;
    }

    @Override
    public void delete(String key) throws Exception {
        lock.lock();
        try {
            stores.remove(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Main 方法，用于演示
     */
    public static void main(String[] args) {
        try {
            // 创建一个消息存储实例，设置默认数据存活时间为10分钟
            Storage ms = new MessageStorage(Duration.ofMinutes(10));

            // 初始化存储
            ms.init();

            // 示例：批量追加消息，使用默认策略
            ms.batchAppend("conversation_123", Arrays.asList("message1", "message2"), null);

            // 示例：批量追加消息，使用自定义策略（按容量淘汰，容量为500）
            ms.batchAppend("conversation_456", Arrays.asList("message3", "message4"), (strategy) -> {
                strategy.evictPolicy = EvictPolicy.EVICT_BY_CAPACITY;
                strategy.capacity = 500;
            });

            // 示例：批量获取消息
            List<String> keys = Arrays.asList("conversation_123", "conversation_456");
            Map<String, List<Object>> result = ms.mGet(keys);

            System.out.println("Messages for conversation_123: " + result.get("conversation_123"));
            System.out.println("Messages for conversation_456: " + result.get("conversation_456"));

            // 示例：删除消息链表
            ms.delete("conversation_123");
            System.out.println("Deleted conversation_123");

            // 验证删除 (这会抛出异常，因为 key_not_found)
            try {
                ms.mGet(Arrays.asList("conversation_123"));
            } catch (Exception e) {
                System.out.println("Get conversation_123 after delete (expected error): " + e.getMessage());
            }

            // 验证 MGet 的 "key not found" 行为
            try {
                ms.mGet(Arrays.asList("conversation_456", "key_does_not_exist"));
            } catch (Exception e) {
                System.out.println("MGet with non-existent key (expected error): " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
