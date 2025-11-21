package com.plato.search.grpc;

import com.plato.search.SearchMessagesRequest;
import com.plato.search.SearchMessagesResponse;
import com.plato.search.StorageSearchServiceGrpc;
import com.plato.search.service.SearchService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * 搜索服务 gRPC 接口实现
 * 
 * 实现 StorageSearchService 定义的所有 RPC 方法
 * 
 * 架构隔离：
 * - 独立微服务，独立端口 (9091)
 * - 独立线程池，资源隔离
 * - 避免影响 storage-server 的核心接口
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class StorageSearchGrpcService extends StorageSearchServiceGrpc.StorageSearchServiceImplBase {

    private final SearchService searchService;

    /**
     * 搜索消息
     * 
     * @param request          搜索请求
     * @param responseObserver 响应观察者
     */
    @Override
    public void searchMessages(SearchMessagesRequest request, 
                               StreamObserver<SearchMessagesResponse> responseObserver) {
        try {
            log.info("Received SearchMessages request: keyword={}, scope={}", 
                    request.getKeyword(), request.getScopeCase());

            // 参数校验
            if (request.getKeyword() == null || request.getKeyword().trim().isEmpty()) {
                log.warn("Invalid request: keyword is empty");
                responseObserver.onError(new IllegalArgumentException("Keyword cannot be empty"));
                return;
            }

            if (request.getScopeCase() == SearchMessagesRequest.ScopeCase.SCOPE_NOT_SET) {
                log.warn("Invalid request: scope not set");
                responseObserver.onError(new IllegalArgumentException("Scope must be set"));
                return;
            }

            // 执行搜索
            SearchMessagesResponse response = searchService.searchMessages(request);

            log.info("SearchMessages completed: totalHits={}, returnedCount={}", 
                    response.getTotalHits(), response.getItemsCount());

            // 返回结果
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.error("Invalid search request: {}", e.getMessage());
            responseObserver.onError(e);
        } catch (Exception e) {
            log.error("SearchMessages failed", e);
            responseObserver.onError(e);
        }
    }
}

