package com.plato.search.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.plato.search.SearchMessagesRequest;
import com.plato.search.SearchMessagesResponse;
import com.plato.search.SearchResultItem;
import com.plato.search.elasticsearch.MessageDocument;
import com.plato.search.redis.InboxRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索服务
 * 
 * 职责：基于 Elasticsearch 实现消息全文搜索
 * 
 * 核心逻辑：
 * 1. 范围解析：根据 session_id 或 owner_id 确定搜索范围
 * 2. 构建查询：关键词 + 过滤器 + 分页
 * 3. 执行搜索：调用 Elasticsearch
 * 4. 高亮处理：返回带高亮的搜索结果
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final InboxRedisService inboxRedisService;

    /**
     * 搜索消息
     * 
     * @param request 搜索请求
     * @return 搜索结果
     */
    public SearchMessagesResponse searchMessages(SearchMessagesRequest request) {
        try {
            log.info("Searching messages: keyword={}, scope={}", 
                    request.getKeyword(), request.getScopeCase());

            // ========== Step 1: 范围解析 (Scope Resolution) ==========
            List<Long> sessionIds = resolveScope(request);

            if (sessionIds == null || sessionIds.isEmpty()) {
                log.warn("No sessions found for search scope: {}", request.getScopeCase());
                return SearchMessagesResponse.newBuilder()
                        .setTotalHits(0)
                        .build();
            }

            log.debug("Resolved {} sessions for search", sessionIds.size());

            // ========== Step 2: 构建查询 ==========
            NativeQuery searchQuery = buildSearchQuery(request, sessionIds);

            // ========== Step 3: 执行搜索 ==========
            SearchHits<MessageDocument> searchHits = elasticsearchOperations.search(
                    searchQuery, MessageDocument.class);

            log.info("Search completed: totalHits={}, returnedHits={}", 
                    searchHits.getTotalHits(), searchHits.getSearchHits().size());

            // ========== Step 4: 构建响应 ==========
            return buildResponse(searchHits);

        } catch (Exception e) {
            log.error("Search failed: request={}", request, e);
            throw new RuntimeException("Search failed", e);
        }
    }

    /**
     * 范围解析：根据 session_id 或 owner_id 确定搜索范围
     * 
     * @param request 搜索请求
     * @return 会话ID列表
     */
    private List<Long> resolveScope(SearchMessagesRequest request) {
        List<Long> sessionIds = new ArrayList<>();

        switch (request.getScopeCase()) {
            case SESSION_ID:
                // Case A: 会话内搜索
                sessionIds.add(request.getSessionId());
                break;

            case OWNER_ID:
                // Case B: 全局搜索
                // 从 Redis 获取用户的所有会话列表
                sessionIds = inboxRedisService.getSessionList(
                        request.getOwnerId(), 
                        0L,  // 从最新开始
                        1000 // 最多取1000个会话
                );
                break;

            default:
                log.warn("Invalid scope case: {}", request.getScopeCase());
        }

        return sessionIds;
    }

    /**
     * 构建 Elasticsearch 查询
     * 
     * @param request 搜索请求
     * @param sessionIds 会话ID列表
     * @return Elasticsearch 查询对象
     */
    private NativeQuery buildSearchQuery(SearchMessagesRequest request, List<Long> sessionIds) {
        // ========== 1. Must 条件：关键词匹配 ==========
        Query keywordQuery = MatchQuery.of(m -> m
                .field("content")
                .query(request.getKeyword())
        )._toQuery();

        // ========== 2. Filter 条件集合 ==========
        List<Query> filterQueries = new ArrayList<>();

        // 2.1 会话范围过滤
        if (sessionIds.size() == 1) {
            filterQueries.add(TermQuery.of(t -> t
                    .field("sessionId")
                    .value(sessionIds.get(0))
            )._toQuery());
        } else {
            // 使用 FieldValue 列表
            List<FieldValue> sessionValues = sessionIds.stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList());
            
            filterQueries.add(TermsQuery.of(t -> t
                    .field("sessionId")
                    .terms(ts -> ts.value(sessionValues))
            )._toQuery());
        }

        // 2.2 消息状态过滤（只搜索正常消息）
        filterQueries.add(TermQuery.of(t -> t
                .field("status")
                .value(0)
        )._toQuery());

        // 2.3 消息类型过滤（可选）
        if (request.getMsgTypesCount() > 0) {
            List<FieldValue> msgTypeValues = request.getMsgTypesList().stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList());
            
            filterQueries.add(TermsQuery.of(t -> t
                    .field("msgType")
                    .terms(ts -> ts.value(msgTypeValues))
            )._toQuery());
        }

        // 2.4 时间范围过滤（可选）
        if (request.getStartTimeMs() > 0 || request.getEndTimeMs() > 0) {
            RangeQuery.Builder rangeBuilder = new RangeQuery.Builder()
                    .field("createTimeMs");
            
            if (request.getStartTimeMs() > 0) {
                rangeBuilder.gte(co.elastic.clients.json.JsonData.of(request.getStartTimeMs()));
            }
            if (request.getEndTimeMs() > 0) {
                rangeBuilder.lte(co.elastic.clients.json.JsonData.of(request.getEndTimeMs()));
            }
            
            filterQueries.add(rangeBuilder.build()._toQuery());
        }

        // 2.5 发送者过滤（可选）
        if (request.getSenderIdsCount() > 0) {
            List<FieldValue> senderValues = request.getSenderIdsList().stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList());
            
            filterQueries.add(TermsQuery.of(t -> t
                    .field("senderId")
                    .terms(ts -> ts.value(senderValues))
            )._toQuery());
        }

        // ========== 3. 组合 Bool 查询 ==========
        BoolQuery boolQuery = BoolQuery.of(b -> b
                .must(keywordQuery)
                .filter(filterQueries)
        );

        // ========== 4. 高亮配置 ==========
        HighlightField highlightField = new HighlightField("content");
        HighlightParameters highlightParams = HighlightParameters.builder()
                .withPreTags("<em>")
                .withPostTags("</em>")
                .withFragmentSize(150)
                .withNumberOfFragments(1)
                .build();
        Highlight highlight = new Highlight(highlightParams, List.of(highlightField));

        // ========== 5. 分页参数 ==========
        int pageNumber = request.getPageNumber() > 0 ? request.getPageNumber() : 1;
        int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 20;
        PageRequest pageRequest = PageRequest.of(pageNumber - 1, pageSize);

        // ========== 6. 构建 NativeQuery ==========
        return NativeQuery.builder()
                .withQuery(boolQuery._toQuery())
                .withPageable(pageRequest)
                .withHighlightQuery(new HighlightQuery(highlight, MessageDocument.class))
                .withSort(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                        .field(f -> f
                                .field("createTimeMs")
                                .order(SortOrder.Desc)
                        )
                ))
                .build();
    }

    /**
     * 构建搜索响应
     * 
     * @param searchHits Elasticsearch 搜索结果
     * @return 搜索响应
     */
    private SearchMessagesResponse buildResponse(SearchHits<MessageDocument> searchHits) {
        SearchMessagesResponse.Builder responseBuilder = SearchMessagesResponse.newBuilder();

        // 设置总命中数
        responseBuilder.setTotalHits(searchHits.getTotalHits());

        // 构建结果列表
        for (SearchHit<MessageDocument> hit : searchHits.getSearchHits()) {
            MessageDocument doc = hit.getContent();

            // 获取高亮内容
            String highlightContent = getHighlightContent(hit);

            SearchResultItem item = SearchResultItem.newBuilder()
                    .setMsgId(doc.getMsgId())
                    .setSessionId(doc.getSessionId())
                    .setSeqId(doc.getSeqId())
                    .setSenderId(doc.getSenderId())
                    .setCreateTimeMs(doc.getCreateTimeMs())
                    .setHighlightContent(highlightContent)
                    .setMsgType(doc.getMsgType())
                    .build();

            responseBuilder.addItems(item);
        }

        return responseBuilder.build();
    }

    /**
     * 提取高亮内容
     * 
     * @param hit 搜索命中结果
     * @return 高亮内容
     */
    private String getHighlightContent(SearchHit<MessageDocument> hit) {
        List<String> highlights = hit.getHighlightField("content");
        
        if (highlights != null && !highlights.isEmpty()) {
            return highlights.get(0);
        }

        // 如果没有高亮，返回原始内容的前150个字符
        String content = hit.getContent().getContent();
        if (content != null && content.length() > 150) {
            return content.substring(0, 150) + "...";
        }
        
        return content != null ? content : "";
    }
}

