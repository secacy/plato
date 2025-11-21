package com.plato.search.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Elasticsearch 消息搜索 Repository
 * 
 * 提供基础的 CRUD 和简单查询功能
 * 复杂查询使用 ElasticsearchRestTemplate
 * 
 * @author hc
 * @since 2025/11/20
 */
@Repository
public interface MessageSearchRepository extends ElasticsearchRepository<MessageDocument, Long> {

    /**
     * 根据会话ID查询消息（用于测试）
     * 
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<MessageDocument> findBySessionId(Long sessionId);

    /**
     * 根据会话ID和状态查询消息
     * 
     * @param sessionId 会话ID
     * @param status 消息状态
     * @return 消息列表
     */
    List<MessageDocument> findBySessionIdAndStatus(Long sessionId, Integer status);
}

