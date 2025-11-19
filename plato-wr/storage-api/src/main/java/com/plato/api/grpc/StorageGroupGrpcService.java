package com.plato.api.grpc;

import com.google.protobuf.Empty;
import com.plato.api.service.GroupMemberService;
import com.plato.gateway.RemoveGroupMembersRequest;
import com.plato.gateway.StorageGroupServiceGrpc;
import com.plato.gateway.UpsertGroupMembersRequest;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

/**
 * 群组元数据服务 gRPC实现
 * 
 * 职责：存储层需要知道"群里有哪些人"才能进行写扩散
 * 
 * @author hc
 * @since 2025/11/19
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class StorageGroupGrpcService extends StorageGroupServiceGrpc.StorageGroupServiceImplBase {

    private final GroupMemberService groupMemberService;

    @Override
    public void upsertGroupMembers(UpsertGroupMembersRequest request,
            StreamObserver<Empty> responseObserver) {
        try {
            log.info("UpsertGroupMembers called, sessionId={}, memberCount={}",
                    request.getSessionId(), request.getMemberIdsCount());

            // 执行同步逻辑
            List<Long> memberIds = request.getMemberIdsList();
            groupMemberService.upsertGroupMembers(request.getSessionId(), memberIds);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("UpsertGroupMembers failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void removeGroupMembers(RemoveGroupMembersRequest request,
            StreamObserver<Empty> responseObserver) {
        try {
            log.info("RemoveGroupMembers called, sessionId={}, memberCount={}",
                    request.getSessionId(), request.getMemberIdsCount());

            // 执行移除逻辑
            List<Long> memberIds = request.getMemberIdsList();
            groupMemberService.removeGroupMembers(request.getSessionId(), memberIds);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("RemoveGroupMembers failed", e);
            responseObserver.onError(e);
        }
    }
}
