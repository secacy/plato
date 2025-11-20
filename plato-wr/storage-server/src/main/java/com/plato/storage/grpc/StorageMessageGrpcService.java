package com.plato.storage.grpc;

import com.google.protobuf.Empty;
import com.plato.storage.exception.MessageNotReadyException;
import com.plato.storage.service.MessageService;
import com.plato.gateway.GetMessagesRequest;
import com.plato.gateway.GetMessagesResponse;
import com.plato.gateway.Message;
import com.plato.gateway.SaveMessageRequest;
import com.plato.gateway.SaveMessageResponse;
import com.plato.gateway.SaveMessagesRequest;
import com.plato.gateway.SaveMessagesResponse;
import com.plato.gateway.SaveMessageResult;
import com.plato.gateway.StorageMessageServiceGrpc;
import com.plato.gateway.UpdateMessageStatusRequest;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

/**
 * 消息核心服务 gRPC实现
 * 
 * 职责：负责消息数据的落地、查询和状态变更
 * 基于 Micro-Batching 架构
 * 
 * @author hc
 * @since 2025/11/19
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class StorageMessageGrpcService extends StorageMessageServiceGrpc.StorageMessageServiceImplBase {

    private final MessageService messageService;

    @Override
    public void saveMessage(SaveMessageRequest request,
            StreamObserver<SaveMessageResponse> responseObserver) {
        try {
            log.info("SaveMessage called, sessionId={}, senderId={}, clientMsgId={}",
                    request.getSessionId(), request.getSenderId(), request.getClientMessageId());

            // 执行保存逻辑（Micro-Batching）
            SaveMessageResponse response = messageService.saveMessage(request);

            // 返回响应
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("SaveMessage failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void saveMessages(SaveMessagesRequest request,
            StreamObserver<SaveMessagesResponse> responseObserver) {
        try {
            log.info("SaveMessages called, count={}", request.getRequestsCount());

            SaveMessagesResponse.Builder responseBuilder = SaveMessagesResponse.newBuilder();

            // 批量处理（Micro-Batching）
            for (SaveMessageRequest req : request.getRequestsList()) {
                try {
                    SaveMessageResponse singleResponse = messageService.saveMessage(req);

                    // 构建成功结果
                    SaveMessageResult result = SaveMessageResult.newBuilder()
                            .setSuccess(singleResponse)
                            .build();

                    responseBuilder.putResults(req.getClientMessageId(), result);

                } catch (Exception e) {
                    log.error("Failed to save message in batch, clientMsgId={}",
                            req.getClientMessageId(), e);

                    // 构建失败结果
                    SaveMessageResult result = SaveMessageResult.newBuilder()
                            .setError(com.plato.gateway.Error.newBuilder()
                                    .setCode(500)
                                    .setMessage(e.getMessage())
                                    .build())
                            .build();

                    responseBuilder.putResults(req.getClientMessageId(), result);
                }
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("SaveMessages failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getMessages(GetMessagesRequest request,
            StreamObserver<GetMessagesResponse> responseObserver) {
        try {
            log.info("GetMessages called, sessionId={}, direction={}, limit={}",
                    request.getSessionId(), request.getDirection(), request.getLimit());

            // 执行查询逻辑（Redis Cache + Gap Detection）
            List<Message> messages = messageService.getMessages(request);

            // 构建响应
            GetMessagesResponse response = GetMessagesResponse.newBuilder()
                    .addAllMessages(messages)
                    .setHasMore(messages.size() >= request.getLimit())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("GetMessages failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateMessageStatus(UpdateMessageStatusRequest request,
            StreamObserver<Empty> responseObserver) {
        try {
            log.info("UpdateMessageStatus called, sessionId={}, seqId={}, newStatus={}",
                    request.getSessionId(), request.getSeqId(), request.getNewStatus());

            // 执行更新逻辑
            messageService.updateMessageStatus(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (MessageNotReadyException e) {
            // 消息尚未就绪（可能在批处理队列中），返回 UNAVAILABLE 状态码
            // 客户端应该稍后重试
            log.warn("UpdateMessageStatus failed - message not ready: {}", e.getMessage());
            responseObserver.onError(
                    Status.UNAVAILABLE
                            .withDescription(e.getMessage())
                            .withCause(e)
                            .asRuntimeException());

        } catch (Exception e) {
            log.error("UpdateMessageStatus failed", e);
            responseObserver.onError(e);
        }
    }
}
