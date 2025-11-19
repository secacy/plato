package com.plato.api.grpc;

import com.google.protobuf.Empty;
import com.plato.api.service.InboxService;
import com.plato.gateway.GetInboxesRequest;
import com.plato.gateway.GetInboxesResponse;
import com.plato.gateway.InboxItem;
import com.plato.gateway.SetInboxAttributesRequest;
import com.plato.gateway.SetInboxReadRequest;
import com.plato.gateway.StorageInboxServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

/**
 * 收件箱服务 gRPC实现
 * 
 * 职责：提供用户视角的"会话列表"和"未读数"管理
 * 
 * @author hc
 * @since 2025/11/19
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class StorageInboxGrpcService extends StorageInboxServiceGrpc.StorageInboxServiceImplBase {

    private final InboxService inboxService;

    @Override
    public void getInboxes(GetInboxesRequest request,
            StreamObserver<GetInboxesResponse> responseObserver) {
        try {
            log.info("GetInboxes called, userId={}, limit={}",
                    request.getUserId(), request.getLimit());

            // 执行查询逻辑
            List<InboxItem> inboxes = inboxService.getInboxes(request);

            // 构建响应
            GetInboxesResponse response = GetInboxesResponse.newBuilder()
                    .addAllInboxes(inboxes)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("GetInboxes failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void setInboxRead(SetInboxReadRequest request,
            StreamObserver<Empty> responseObserver) {
        try {
            log.info("SetInboxRead called, userId={}, sessionId={}, readSeqId={}",
                    request.getUserId(), request.getSessionId(), request.getReadSeqId());

            // 执行设置已读逻辑
            inboxService.setInboxRead(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("SetInboxRead failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void setInboxAttributes(SetInboxAttributesRequest request,
            StreamObserver<Empty> responseObserver) {
        try {
            log.info("SetInboxAttributes called, userId={}, sessionId={}, attributeCase={}",
                    request.getUserId(), request.getSessionId(), request.getAttributeCase());

            // 执行设置属性逻辑
            inboxService.setInboxAttributes(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("SetInboxAttributes failed", e);
            responseObserver.onError(e);
        }
    }
}
