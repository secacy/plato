package com.plato.search.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch 配置类
 * 
 * @author hc
 * @since 2025/11/20
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.plato.search.elasticsearch")
public class ElasticsearchConfig {
    // Spring Boot Auto-Configuration 会自动配置 ElasticsearchRestTemplate
    // 我们只需要启用 Repository 扫描
}

