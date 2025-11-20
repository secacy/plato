package com.plato.storage.grpc;

import com.google.protobuf.Empty;
import com.plato.admin.AdminRecallMessageRequest;
import com.plato.admin.StorageGovernanceServiceGrpc;
import com.plato.storage.event.MessageEvent;
import com.plato.storage.producer.StorageEventProducer;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据治理与合规服务 gRPC实现
 * 
 * 职责：处理"非用户行为"的数据操作
 * 
 * @author hc
 * @since 2025/11/19
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class StorageGovernanceGrpcService extends StorageGovernanceServiceGrpc.StorageGovernanceServiceImplBase {

    private final StorageEventProducer eventProducer;

    @Override
    public void adminRecallMessage(AdminRecallMessageRequest request,
            StreamObserver<Empty> responseObserver) {
        try {
            log.warn("AdminRecallMessage called, sessionId={}, seqId={}, reason={}, operator={}",
                    request.getSessionId(), request.getSeqId(),
                    request.getReason(), request.getOperator());

            // 构建扩展信息
            Map<String, String> extra = new HashMap<>();
            extra.put("reason", request.getReason());
            extra.put("operator", request.getOperator());
            extra.put("admin_action", "true");

            // 发送管理员撤回事件
            MessageEvent event = MessageEvent.builder()
                    .eventType(MessageEvent.EventType.ADMIN_RECALL)
                    .sessionId(request.getSessionId())
                    .seqId(request.getSeqId())
                    .extra(extra)
                    .serverTimeMs(System.currentTimeMillis())
                    .build();

            eventProducer.sendMessageEvent(event);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("AdminRecallMessage failed", e);
            responseObserver.onError(e);
        }
    }
}
