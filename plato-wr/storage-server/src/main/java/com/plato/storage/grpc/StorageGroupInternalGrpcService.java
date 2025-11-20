package com.plato.storage.grpc;

import com.google.protobuf.Empty;
import com.plato.admin.PatchGroupMembersRequest;
import com.plato.admin.ResetGroupMembersRequest;
import com.plato.admin.StorageGroupInternalServiceGrpc;
import com.plato.storage.service.GroupMemberService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

/**
 * 内部群组同步服务 gRPC实现
 * 
 * 职责：维护存储层所需的"群成员列表"副本
 * 
 * @author hc
 * @since 2025/11/19
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class StorageGroupInternalGrpcService
        extends StorageGroupInternalServiceGrpc.StorageGroupInternalServiceImplBase {

    private final GroupMemberService groupMemberService;

    @Override
    public void resetGroupMembers(ResetGroupMembersRequest request,
            StreamObserver<Empty> responseObserver) {
        try {
            log.info("ResetGroupMembers called, sessionId={}, memberCount={}",
                    request.getSessionId(), request.getMemberIdsCount());

            // 全量重置
            List<Long> memberIds = request.getMemberIdsList();
            groupMemberService.resetGroupMembers(request.getSessionId(), memberIds);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("ResetGroupMembers failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void patchGroupMembers(PatchGroupMembersRequest request,
            StreamObserver<Empty> responseObserver) {
        try {
            log.info("PatchGroupMembers called, sessionId={}, added={}, removed={}",
                    request.getSessionId(),
                    request.getAddedMemberIdsCount(),
                    request.getRemovedMemberIdsCount());

            Long sessionId = request.getSessionId();

            // 处理添加
            if (request.getAddedMemberIdsCount() > 0) {
                List<Long> addedIds = request.getAddedMemberIdsList();
                groupMemberService.upsertGroupMembers(sessionId, addedIds);
            }

            // 处理移除
            if (request.getRemovedMemberIdsCount() > 0) {
                List<Long> removedIds = request.getRemovedMemberIdsList();
                groupMemberService.removeGroupMembers(sessionId, removedIds);
            }

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("PatchGroupMembers failed", e);
            responseObserver.onError(e);
        }
    }
}
