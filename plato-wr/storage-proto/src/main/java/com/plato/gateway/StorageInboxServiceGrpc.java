package com.plato.gateway;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * =============================================================================
 * Service 2: 收件箱服务 (Inbox Service)
 * 职责：提供用户视角的“会话列表”和“未读数”管理。
 * 数据源：主要来自 Redis (ZSET/Hash)，部分冷数据可能回源 TiDB。
 * =============================================================================
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.1)",
    comments = "Source: storage_gateway.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class StorageInboxServiceGrpc {

  private StorageInboxServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.plato.gateway.StorageInboxService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.plato.gateway.GetInboxesRequest,
      com.plato.gateway.GetInboxesResponse> getGetInboxesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetInboxes",
      requestType = com.plato.gateway.GetInboxesRequest.class,
      responseType = com.plato.gateway.GetInboxesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.plato.gateway.GetInboxesRequest,
      com.plato.gateway.GetInboxesResponse> getGetInboxesMethod() {
    io.grpc.MethodDescriptor<com.plato.gateway.GetInboxesRequest, com.plato.gateway.GetInboxesResponse> getGetInboxesMethod;
    if ((getGetInboxesMethod = StorageInboxServiceGrpc.getGetInboxesMethod) == null) {
      synchronized (StorageInboxServiceGrpc.class) {
        if ((getGetInboxesMethod = StorageInboxServiceGrpc.getGetInboxesMethod) == null) {
          StorageInboxServiceGrpc.getGetInboxesMethod = getGetInboxesMethod =
              io.grpc.MethodDescriptor.<com.plato.gateway.GetInboxesRequest, com.plato.gateway.GetInboxesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetInboxes"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.gateway.GetInboxesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.gateway.GetInboxesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new StorageInboxServiceMethodDescriptorSupplier("GetInboxes"))
              .build();
        }
      }
    }
    return getGetInboxesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.plato.gateway.SetInboxReadRequest,
      com.google.protobuf.Empty> getSetInboxReadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetInboxRead",
      requestType = com.plato.gateway.SetInboxReadRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.plato.gateway.SetInboxReadRequest,
      com.google.protobuf.Empty> getSetInboxReadMethod() {
    io.grpc.MethodDescriptor<com.plato.gateway.SetInboxReadRequest, com.google.protobuf.Empty> getSetInboxReadMethod;
    if ((getSetInboxReadMethod = StorageInboxServiceGrpc.getSetInboxReadMethod) == null) {
      synchronized (StorageInboxServiceGrpc.class) {
        if ((getSetInboxReadMethod = StorageInboxServiceGrpc.getSetInboxReadMethod) == null) {
          StorageInboxServiceGrpc.getSetInboxReadMethod = getSetInboxReadMethod =
              io.grpc.MethodDescriptor.<com.plato.gateway.SetInboxReadRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetInboxRead"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.gateway.SetInboxReadRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new StorageInboxServiceMethodDescriptorSupplier("SetInboxRead"))
              .build();
        }
      }
    }
    return getSetInboxReadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.plato.gateway.SetInboxAttributesRequest,
      com.google.protobuf.Empty> getSetInboxAttributesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetInboxAttributes",
      requestType = com.plato.gateway.SetInboxAttributesRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.plato.gateway.SetInboxAttributesRequest,
      com.google.protobuf.Empty> getSetInboxAttributesMethod() {
    io.grpc.MethodDescriptor<com.plato.gateway.SetInboxAttributesRequest, com.google.protobuf.Empty> getSetInboxAttributesMethod;
    if ((getSetInboxAttributesMethod = StorageInboxServiceGrpc.getSetInboxAttributesMethod) == null) {
      synchronized (StorageInboxServiceGrpc.class) {
        if ((getSetInboxAttributesMethod = StorageInboxServiceGrpc.getSetInboxAttributesMethod) == null) {
          StorageInboxServiceGrpc.getSetInboxAttributesMethod = getSetInboxAttributesMethod =
              io.grpc.MethodDescriptor.<com.plato.gateway.SetInboxAttributesRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetInboxAttributes"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.gateway.SetInboxAttributesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new StorageInboxServiceMethodDescriptorSupplier("SetInboxAttributes"))
              .build();
        }
      }
    }
    return getSetInboxAttributesMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static StorageInboxServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageInboxServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageInboxServiceStub>() {
        @java.lang.Override
        public StorageInboxServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageInboxServiceStub(channel, callOptions);
        }
      };
    return StorageInboxServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static StorageInboxServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageInboxServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageInboxServiceBlockingStub>() {
        @java.lang.Override
        public StorageInboxServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageInboxServiceBlockingStub(channel, callOptions);
        }
      };
    return StorageInboxServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static StorageInboxServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageInboxServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageInboxServiceFutureStub>() {
        @java.lang.Override
        public StorageInboxServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageInboxServiceFutureStub(channel, callOptions);
        }
      };
    return StorageInboxServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * =============================================================================
   * Service 2: 收件箱服务 (Inbox Service)
   * 职责：提供用户视角的“会话列表”和“未读数”管理。
   * 数据源：主要来自 Redis (ZSET/Hash)，部分冷数据可能回源 TiDB。
   * =============================================================================
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * 拉取会话列表 (Sync)
     * 场景：APP 首页“消息” Tab
     * </pre>
     */
    default void getInboxes(com.plato.gateway.GetInboxesRequest request,
        io.grpc.stub.StreamObserver<com.plato.gateway.GetInboxesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetInboxesMethod(), responseObserver);
    }

    /**
     * <pre>
     * 设置已读 (Mark as Read)
     * 场景：用户点进某个会话，或者在该会话停留
     * 逻辑：更新 Redis 中该用户的 last_read_seq_id，并重新计算 unread_count
     * </pre>
     */
    default void setInboxRead(com.plato.gateway.SetInboxReadRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetInboxReadMethod(), responseObserver);
    }

    /**
     * <pre>
     * 设置会话属性 (置顶 / 免打扰)
     * 逻辑：更新 Redis/DB 中的用户配置，影响未读数的小红点逻辑
     * </pre>
     */
    default void setInboxAttributes(com.plato.gateway.SetInboxAttributesRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetInboxAttributesMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service StorageInboxService.
   * <pre>
   * =============================================================================
   * Service 2: 收件箱服务 (Inbox Service)
   * 职责：提供用户视角的“会话列表”和“未读数”管理。
   * 数据源：主要来自 Redis (ZSET/Hash)，部分冷数据可能回源 TiDB。
   * =============================================================================
   * </pre>
   */
  public static abstract class StorageInboxServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return StorageInboxServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service StorageInboxService.
   * <pre>
   * =============================================================================
   * Service 2: 收件箱服务 (Inbox Service)
   * 职责：提供用户视角的“会话列表”和“未读数”管理。
   * 数据源：主要来自 Redis (ZSET/Hash)，部分冷数据可能回源 TiDB。
   * =============================================================================
   * </pre>
   */
  public static final class StorageInboxServiceStub
      extends io.grpc.stub.AbstractAsyncStub<StorageInboxServiceStub> {
    private StorageInboxServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageInboxServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageInboxServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 拉取会话列表 (Sync)
     * 场景：APP 首页“消息” Tab
     * </pre>
     */
    public void getInboxes(com.plato.gateway.GetInboxesRequest request,
        io.grpc.stub.StreamObserver<com.plato.gateway.GetInboxesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetInboxesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 设置已读 (Mark as Read)
     * 场景：用户点进某个会话，或者在该会话停留
     * 逻辑：更新 Redis 中该用户的 last_read_seq_id，并重新计算 unread_count
     * </pre>
     */
    public void setInboxRead(com.plato.gateway.SetInboxReadRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetInboxReadMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 设置会话属性 (置顶 / 免打扰)
     * 逻辑：更新 Redis/DB 中的用户配置，影响未读数的小红点逻辑
     * </pre>
     */
    public void setInboxAttributes(com.plato.gateway.SetInboxAttributesRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetInboxAttributesMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service StorageInboxService.
   * <pre>
   * =============================================================================
   * Service 2: 收件箱服务 (Inbox Service)
   * 职责：提供用户视角的“会话列表”和“未读数”管理。
   * 数据源：主要来自 Redis (ZSET/Hash)，部分冷数据可能回源 TiDB。
   * =============================================================================
   * </pre>
   */
  public static final class StorageInboxServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<StorageInboxServiceBlockingStub> {
    private StorageInboxServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageInboxServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageInboxServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 拉取会话列表 (Sync)
     * 场景：APP 首页“消息” Tab
     * </pre>
     */
    public com.plato.gateway.GetInboxesResponse getInboxes(com.plato.gateway.GetInboxesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetInboxesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 设置已读 (Mark as Read)
     * 场景：用户点进某个会话，或者在该会话停留
     * 逻辑：更新 Redis 中该用户的 last_read_seq_id，并重新计算 unread_count
     * </pre>
     */
    public com.google.protobuf.Empty setInboxRead(com.plato.gateway.SetInboxReadRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetInboxReadMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 设置会话属性 (置顶 / 免打扰)
     * 逻辑：更新 Redis/DB 中的用户配置，影响未读数的小红点逻辑
     * </pre>
     */
    public com.google.protobuf.Empty setInboxAttributes(com.plato.gateway.SetInboxAttributesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetInboxAttributesMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service StorageInboxService.
   * <pre>
   * =============================================================================
   * Service 2: 收件箱服务 (Inbox Service)
   * 职责：提供用户视角的“会话列表”和“未读数”管理。
   * 数据源：主要来自 Redis (ZSET/Hash)，部分冷数据可能回源 TiDB。
   * =============================================================================
   * </pre>
   */
  public static final class StorageInboxServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<StorageInboxServiceFutureStub> {
    private StorageInboxServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageInboxServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageInboxServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 拉取会话列表 (Sync)
     * 场景：APP 首页“消息” Tab
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.plato.gateway.GetInboxesResponse> getInboxes(
        com.plato.gateway.GetInboxesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetInboxesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * 设置已读 (Mark as Read)
     * 场景：用户点进某个会话，或者在该会话停留
     * 逻辑：更新 Redis 中该用户的 last_read_seq_id，并重新计算 unread_count
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> setInboxRead(
        com.plato.gateway.SetInboxReadRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetInboxReadMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * 设置会话属性 (置顶 / 免打扰)
     * 逻辑：更新 Redis/DB 中的用户配置，影响未读数的小红点逻辑
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> setInboxAttributes(
        com.plato.gateway.SetInboxAttributesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetInboxAttributesMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_INBOXES = 0;
  private static final int METHODID_SET_INBOX_READ = 1;
  private static final int METHODID_SET_INBOX_ATTRIBUTES = 2;

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
        case METHODID_GET_INBOXES:
          serviceImpl.getInboxes((com.plato.gateway.GetInboxesRequest) request,
              (io.grpc.stub.StreamObserver<com.plato.gateway.GetInboxesResponse>) responseObserver);
          break;
        case METHODID_SET_INBOX_READ:
          serviceImpl.setInboxRead((com.plato.gateway.SetInboxReadRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_SET_INBOX_ATTRIBUTES:
          serviceImpl.setInboxAttributes((com.plato.gateway.SetInboxAttributesRequest) request,
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
          getGetInboxesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.plato.gateway.GetInboxesRequest,
              com.plato.gateway.GetInboxesResponse>(
                service, METHODID_GET_INBOXES)))
        .addMethod(
          getSetInboxReadMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.plato.gateway.SetInboxReadRequest,
              com.google.protobuf.Empty>(
                service, METHODID_SET_INBOX_READ)))
        .addMethod(
          getSetInboxAttributesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.plato.gateway.SetInboxAttributesRequest,
              com.google.protobuf.Empty>(
                service, METHODID_SET_INBOX_ATTRIBUTES)))
        .build();
  }

  private static abstract class StorageInboxServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    StorageInboxServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.plato.gateway.StorageGateway.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("StorageInboxService");
    }
  }

  private static final class StorageInboxServiceFileDescriptorSupplier
      extends StorageInboxServiceBaseDescriptorSupplier {
    StorageInboxServiceFileDescriptorSupplier() {}
  }

  private static final class StorageInboxServiceMethodDescriptorSupplier
      extends StorageInboxServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    StorageInboxServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (StorageInboxServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new StorageInboxServiceFileDescriptorSupplier())
              .addMethod(getGetInboxesMethod())
              .addMethod(getSetInboxReadMethod())
              .addMethod(getSetInboxAttributesMethod())
              .build();
        }
      }
    }
    return result;
  }
}
