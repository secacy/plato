package com.plato.consumer.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch 消息搜索 Repository
 * 
 * 提供基础的 CRUD 功能
 * 
 * @author hc
 * @since 2025/11/20
 */
@Repository
public interface MessageSearchRepository extends ElasticsearchRepository<MessageDocument, Long> {
}

