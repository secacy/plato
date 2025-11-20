package com.plato;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 存储接口定义
 *
 * @interfaceName Storage
 * @author hc
 * @since 2025/10/22 23:42
 */
public interface Storage {
    /**
     * 初始化存储
     */
    void init() throws Exception;

    /**
     * 批量追加数据
     * @param key 键
     * @param values 要追加的值列表
     * @param strategyFunc 一个消费者函数，用于自定义此键的存储策略
     */
    void batchAppend(String key, List<Object> values, Consumer<StorageStrategy> strategyFunc) throws Exception;

    /**
     * 批量获取数据
     * @param keys 要获取的键列表
     * @return 包含键和对应值列表的Map
     */
    Map<String, List<Object>> mGet(List<String> keys) throws Exception;

    /**
     * 删除数据
     * @param key 要删除的键
     */
    void delete(String key) throws Exception;
}
