package com.plato.gateway;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * =============================================================================
 * Service 3: 群组元数据服务 (Group Metadata Service)
 * 职责：存储层需要知道“群里有哪些人”才能进行写扩散 (Fan-out)。
 * 注意：这不是业务层的“加群”接口，而是业务层加群成功后，同步数据给存储层的接口。
 * =============================================================================
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.1)",
    comments = "Source: storage_gateway.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class StorageGroupServiceGrpc {

  private StorageGroupServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.plato.gateway.StorageGroupService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.plato.gateway.UpsertGroupMembersRequest,
      com.google.protobuf.Empty> getUpsertGroupMembersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpsertGroupMembers",
      requestType = com.plato.gateway.UpsertGroupMembersRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.plato.gateway.UpsertGroupMembersRequest,
      com.google.protobuf.Empty> getUpsertGroupMembersMethod() {
    io.grpc.MethodDescriptor<com.plato.gateway.UpsertGroupMembersRequest, com.google.protobuf.Empty> getUpsertGroupMembersMethod;
    if ((getUpsertGroupMembersMethod = StorageGroupServiceGrpc.getUpsertGroupMembersMethod) == null) {
      synchronized (StorageGroupServiceGrpc.class) {
        if ((getUpsertGroupMembersMethod = StorageGroupServiceGrpc.getUpsertGroupMembersMethod) == null) {
          StorageGroupServiceGrpc.getUpsertGroupMembersMethod = getUpsertGroupMembersMethod =
              io.grpc.MethodDescriptor.<com.plato.gateway.UpsertGroupMembersRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpsertGroupMembers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.gateway.UpsertGroupMembersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new StorageGroupServiceMethodDescriptorSupplier("UpsertGroupMembers"))
              .build();
        }
      }
    }
    return getUpsertGroupMembersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.plato.gateway.RemoveGroupMembersRequest,
      com.google.protobuf.Empty> getRemoveGroupMembersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RemoveGroupMembers",
      requestType = com.plato.gateway.RemoveGroupMembersRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.plato.gateway.RemoveGroupMembersRequest,
      com.google.protobuf.Empty> getRemoveGroupMembersMethod() {
    io.grpc.MethodDescriptor<com.plato.gateway.RemoveGroupMembersRequest, com.google.protobuf.Empty> getRemoveGroupMembersMethod;
    if ((getRemoveGroupMembersMethod = StorageGroupServiceGrpc.getRemoveGroupMembersMethod) == null) {
      synchronized (StorageGroupServiceGrpc.class) {
        if ((getRemoveGroupMembersMethod = StorageGroupServiceGrpc.getRemoveGroupMembersMethod) == null) {
          StorageGroupServiceGrpc.getRemoveGroupMembersMethod = getRemoveGroupMembersMethod =
              io.grpc.MethodDescriptor.<com.plato.gateway.RemoveGroupMembersRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RemoveGroupMembers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.gateway.RemoveGroupMembersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new StorageGroupServiceMethodDescriptorSupplier("RemoveGroupMembers"))
              .build();
        }
      }
    }
    return getRemoveGroupMembersMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static StorageGroupServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageGroupServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageGroupServiceStub>() {
        @java.lang.Override
        public StorageGroupServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageGroupServiceStub(channel, callOptions);
        }
      };
    return StorageGroupServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static StorageGroupServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageGroupServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageGroupServiceBlockingStub>() {
        @java.lang.Override
        public StorageGroupServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageGroupServiceBlockingStub(channel, callOptions);
        }
      };
    return StorageGroupServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static StorageGroupServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageGroupServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageGroupServiceFutureStub>() {
        @java.lang.Override
        public StorageGroupServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageGroupServiceFutureStub(channel, callOptions);
        }
      };
    return StorageGroupServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * =============================================================================
   * Service 3: 群组元数据服务 (Group Metadata Service)
   * 职责：存储层需要知道“群里有哪些人”才能进行写扩散 (Fan-out)。
   * 注意：这不是业务层的“加群”接口，而是业务层加群成功后，同步数据给存储层的接口。
   * =============================================================================
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * 同步群成员列表
     * 场景：建群、拉人、踢人
     * 逻辑：将成员列表写入 TiDB/Redis，供 SaveMessage 时查找目标用户
     * </pre>
     */
    default void upsertGroupMembers(com.plato.gateway.UpsertGroupMembersRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpsertGroupMembersMethod(), responseObserver);
    }

    /**
     * <pre>
     * 移除群成员
     * </pre>
     */
    default void removeGroupMembers(com.plato.gateway.RemoveGroupMembersRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRemoveGroupMembersMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service StorageGroupService.
   * <pre>
   * =============================================================================
   * Service 3: 群组元数据服务 (Group Metadata Service)
   * 职责：存储层需要知道“群里有哪些人”才能进行写扩散 (Fan-out)。
   * 注意：这不是业务层的“加群”接口，而是业务层加群成功后，同步数据给存储层的接口。
   * =============================================================================
   * </pre>
   */
  public static abstract class StorageGroupServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return StorageGroupServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service StorageGroupService.
   * <pre>
   * =============================================================================
   * Service 3: 群组元数据服务 (Group Metadata Service)
   * 职责：存储层需要知道“群里有哪些人”才能进行写扩散 (Fan-out)。
   * 注意：这不是业务层的“加群”接口，而是业务层加群成功后，同步数据给存储层的接口。
   * =============================================================================
   * </pre>
   */
  public static final class StorageGroupServiceStub
      extends io.grpc.stub.AbstractAsyncStub<StorageGroupServiceStub> {
    private StorageGroupServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageGroupServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageGroupServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 同步群成员列表
     * 场景：建群、拉人、踢人
     * 逻辑：将成员列表写入 TiDB/Redis，供 SaveMessage 时查找目标用户
     * </pre>
     */
    public void upsertGroupMembers(com.plato.gateway.UpsertGroupMembersRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpsertGroupMembersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 移除群成员
     * </pre>
     */
    public void removeGroupMembers(com.plato.gateway.RemoveGroupMembersRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRemoveGroupMembersMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service StorageGroupService.
   * <pre>
   * =============================================================================
   * Service 3: 群组元数据服务 (Group Metadata Service)
   * 职责：存储层需要知道“群里有哪些人”才能进行写扩散 (Fan-out)。
   * 注意：这不是业务层的“加群”接口，而是业务层加群成功后，同步数据给存储层的接口。
   * =============================================================================
   * </pre>
   */
  public static final class StorageGroupServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<StorageGroupServiceBlockingStub> {
    private StorageGroupServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageGroupServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageGroupServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 同步群成员列表
     * 场景：建群、拉人、踢人
     * 逻辑：将成员列表写入 TiDB/Redis，供 SaveMessage 时查找目标用户
     * </pre>
     */
    public com.google.protobuf.Empty upsertGroupMembers(com.plato.gateway.UpsertGroupMembersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpsertGroupMembersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 移除群成员
     * </pre>
     */
    public com.google.protobuf.Empty removeGroupMembers(com.plato.gateway.RemoveGroupMembersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRemoveGroupMembersMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service StorageGroupService.
   * <pre>
   * =============================================================================
   * Service 3: 群组元数据服务 (Group Metadata Service)
   * 职责：存储层需要知道“群里有哪些人”才能进行写扩散 (Fan-out)。
   * 注意：这不是业务层的“加群”接口，而是业务层加群成功后，同步数据给存储层的接口。
   * =============================================================================
   * </pre>
   */
  public static final class StorageGroupServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<StorageGroupServiceFutureStub> {
    private StorageGroupServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageGroupServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageGroupServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 同步群成员列表
     * 场景：建群、拉人、踢人
     * 逻辑：将成员列表写入 TiDB/Redis，供 SaveMessage 时查找目标用户
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> upsertGroupMembers(
        com.plato.gateway.UpsertGroupMembersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpsertGroupMembersMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * 移除群成员
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> removeGroupMembers(
        com.plato.gateway.RemoveGroupMembersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRemoveGroupMembersMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_UPSERT_GROUP_MEMBERS = 0;
  private static final int METHODID_REMOVE_GROUP_MEMBERS = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_UPSERT_GROUP_MEMBERS:
          serviceImpl.upsertGroupMembers((com.plato.gateway.UpsertGroupMembersRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_REMOVE_GROUP_MEMBERS:
          serviceImpl.removeGroupMembers((com.plato.gateway.RemoveGroupMembersRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getUpsertGroupMembersMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.plato.gateway.UpsertGroupMembersRequest,
              com.google.protobuf.Empty>(
                service, METHODID_UPSERT_GROUP_MEMBERS)))
        .addMethod(
          getRemoveGroupMembersMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.plato.gateway.RemoveGroupMembersRequest,
              com.google.protobuf.Empty>(
                service, METHODID_REMOVE_GROUP_MEMBERS)))
        .build();
  }

  private static abstract class StorageGroupServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    StorageGroupServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.plato.gateway.StorageGateway.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("StorageGroupService");
    }
  }

  private static final class StorageGroupServiceFileDescriptorSupplier
      extends StorageGroupServiceBaseDescriptorSupplier {
    StorageGroupServiceFileDescriptorSupplier() {}
  }

  private static final class StorageGroupServiceMethodDescriptorSupplier
      extends StorageGroupServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    StorageGroupServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (StorageGroupServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new StorageGroupServiceFileDescriptorSupplier())
              .addMethod(getUpsertGroupMembersMethod())
              .addMethod(getRemoveGroupMembersMethod())
              .build();
        }
      }
    }
    return result;
  }
}
