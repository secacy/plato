package com.plato.id.service.impl;

/**
 * @since 2025/11/18 11:43
 * @className IdServiceImpl
 * @author hc
 */

import com.plato.id.core.IdGenerator;
import com.plato.id.grpc.IdServiceGrpc;
import com.plato.id.grpc.IdServiceProto;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService; // <-- 导入新注解
import org.springframework.beans.factory.annotation.Autowired; // <-- 导入 Autowired

/**
 * gRPC 服务的 Spring Boot 实现
 *
 * 1. @GrpcService 标记此类为 gRPC 服务
 * 2. Spring 将通过 @Autowired 自动注入 IdGenerator
 */
@GrpcService // <-- 不再是 'extends'，而是注解
public class IdServiceImpl extends IdServiceGrpc.IdServiceImplBase {

    private final IdGenerator idGenerator;

    /**
     * Spring 自动调用此构造函数
     * 它会找到一个 IdGenerator 类型的 Bean 并传入
     */
    @Autowired
    public IdServiceImpl(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    /**
     * 这个方法内部的逻辑完全不变
     */
    @Override
    public void generateId(IdServiceProto.GenerateIdRequest request,
                           StreamObserver<IdServiceProto.GenerateIdResponse> responseObserver) {
        try {
            int bid = request.getBid();
            long newId = idGenerator.gen(bid); // 调用注入的 bean

            IdServiceProto.GenerateIdResponse response = IdServiceProto.GenerateIdResponse.newBuilder()
                    .setId(newId)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to generate ID: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }
}