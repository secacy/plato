package com.plato.gateway;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * =============================================================================
 * Service 1: 消息核心服务 (Message Service)
 * 职责：负责消息数据的落地 (TiDB)、查询 (Timeline) 和 状态变更。
 * =============================================================================
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.1)",
    comments = "Source: storage_gateway.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class StorageMessageServiceGrpc {

  private StorageMessageServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.plato.gateway.StorageMessageService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.plato.gateway.SaveMessageRequest,
      com.plato.gateway.SaveMessageResponse> getSaveMessageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SaveMessage",
      requestType = com.plato.gateway.SaveMessageRequest.class,
      responseType = com.plato.gateway.SaveMessageResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.plato.gateway.SaveMessageRequest,
      com.plato.gateway.SaveMessageResponse> getSaveMessageMethod() {
    io.grpc.MethodDescriptor<com.plato.gateway.SaveMessageRequest, com.plato.gateway.SaveMessageResponse> getSaveMessageMethod;
    if ((getSaveMessageMethod = StorageMessageServiceGrpc.getSaveMessageMethod) == null) {
      synchronized (StorageMessageServiceGrpc.class) {
        if ((getSaveMessageMethod = StorageMessageServiceGrpc.getSaveMessageMethod) == null) {
          StorageMessageServiceGrpc.getSaveMessageMethod = getSaveMessageMethod =
              io.grpc.MethodDescriptor.<com.plato.gateway.SaveMessageRequest, com.plato.gateway.SaveMessageResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SaveMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.gateway.SaveMessageRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.gateway.SaveMessageResponse.getDefaultInstance()))
              .setSchemaDescriptor(new StorageMessageServiceMethodDescriptorSupplier("SaveMessage"))
              .build();
        }
      }
    }
    return getSaveMessageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.plato.gateway.SaveMessagesRequest,
      com.plato.gateway.SaveMessagesResponse> getSaveMessagesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SaveMessages",
      requestType = com.plato.gateway.SaveMessagesRequest.class,
      responseType = com.plato.gateway.SaveMessagesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.plato.gateway.SaveMessagesRequest,
      com.plato.gateway.SaveMessagesResponse> getSaveMessagesMethod() {
    io.grpc.MethodDescriptor<com.plato.gateway.SaveMessagesRequest, com.plato.gateway.SaveMessagesResponse> getSaveMessagesMethod;
    if ((getSaveMessagesMethod = StorageMessageServiceGrpc.getSaveMessagesMethod) == null) {
      synchronized (StorageMessageServiceGrpc.class) {
        if ((getSaveMessagesMethod = StorageMessageServiceGrpc.getSaveMessagesMethod) == null) {
          StorageMessageServiceGrpc.getSaveMessagesMethod = getSaveMessagesMethod =
              io.grpc.MethodDescriptor.<com.plato.gateway.SaveMessagesRequest, com.plato.gateway.SaveMessagesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SaveMessages"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.gateway.SaveMessagesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.gateway.SaveMessagesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new StorageMessageServiceMethodDescriptorSupplier("SaveMessages"))
              .build();
        }
      }
    }
    return getSaveMessagesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.plato.gateway.GetMessagesRequest,
      com.plato.gateway.GetMessagesResponse> getGetMessagesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetMessages",
      requestType = com.plato.gateway.GetMessagesRequest.class,
      responseType = com.plato.gateway.GetMessagesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.plato.gateway.GetMessagesRequest,
      com.plato.gateway.GetMessagesResponse> getGetMessagesMethod() {
    io.grpc.MethodDescriptor<com.plato.gateway.GetMessagesRequest, com.plato.gateway.GetMessagesResponse> getGetMessagesMethod;
    if ((getGetMessagesMethod = StorageMessageServiceGrpc.getGetMessagesMethod) == null) {
      synchronized (StorageMessageServiceGrpc.class) {
        if ((getGetMessagesMethod = StorageMessageServiceGrpc.getGetMessagesMethod) == null) {
          StorageMessageServiceGrpc.getGetMessagesMethod = getGetMessagesMethod =
              io.grpc.MethodDescriptor.<com.plato.gateway.GetMessagesRequest, com.plato.gateway.GetMessagesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetMessages"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.gateway.GetMessagesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.gateway.GetMessagesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new StorageMessageServiceMethodDescriptorSupplier("GetMessages"))
              .build();
        }
      }
    }
    return getGetMessagesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.plato.gateway.UpdateMessageStatusRequest,
      com.google.protobuf.Empty> getUpdateMessageStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateMessageStatus",
      requestType = com.plato.gateway.UpdateMessageStatusRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.plato.gateway.UpdateMessageStatusRequest,
      com.google.protobuf.Empty> getUpdateMessageStatusMethod() {
    io.grpc.MethodDescriptor<com.plato.gateway.UpdateMessageStatusRequest, com.google.protobuf.Empty> getUpdateMessageStatusMethod;
    if ((getUpdateMessageStatusMethod = StorageMessageServiceGrpc.getUpdateMessageStatusMethod) == null) {
      synchronized (StorageMessageServiceGrpc.class) {
        if ((getUpdateMessageStatusMethod = StorageMessageServiceGrpc.getUpdateMessageStatusMethod) == null) {
          StorageMessageServiceGrpc.getUpdateMessageStatusMethod = getUpdateMessageStatusMethod =
              io.grpc.MethodDescriptor.<com.plato.gateway.UpdateMessageStatusRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateMessageStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.gateway.UpdateMessageStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new StorageMessageServiceMethodDescriptorSupplier("UpdateMessageStatus"))
              .build();
        }
      }
    }
    return getUpdateMessageStatusMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static StorageMessageServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageMessageServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageMessageServiceStub>() {
        @java.lang.Override
        public StorageMessageServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageMessageServiceStub(channel, callOptions);
        }
      };
    return StorageMessageServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static StorageMessageServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageMessageServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageMessageServiceBlockingStub>() {
        @java.lang.Override
        public StorageMessageServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageMessageServiceBlockingStub(channel, callOptions);
        }
      };
    return StorageMessageServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static StorageMessageServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageMessageServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageMessageServiceFutureStub>() {
        @java.lang.Override
        public StorageMessageServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageMessageServiceFutureStub(channel, callOptions);
        }
      };
    return StorageMessageServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * =============================================================================
   * Service 1: 消息核心服务 (Message Service)
   * 职责：负责消息数据的落地 (TiDB)、查询 (Timeline) 和 状态变更。
   * =============================================================================
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * 单条保存 (最常用的接口)
     * 内部逻辑：Check Redis幂等 -&gt; Snowflake生成ID -&gt; Redis Incr Seq -&gt; TiDB Insert -&gt; Redis Inbox Update
     * </pre>
     */
    default void saveMessage(com.plato.gateway.SaveMessageRequest request,
        io.grpc.stub.StreamObserver<com.plato.gateway.SaveMessageResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSaveMessageMethod(), responseObserver);
    }

    /**
     * <pre>
     * 批量保存 (用于消息转发、数据迁移、系统广播)
     * 内部逻辑：批量执行上述流程，建议内部使用并发处理
     * </pre>
     */
    default void saveMessages(com.plato.gateway.SaveMessagesRequest request,
        io.grpc.stub.StreamObserver<com.plato.gateway.SaveMessagesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSaveMessagesMethod(), responseObserver);
    }

    /**
     * <pre>
     * 拉取消息历史 (Timeline Pull)
     * 场景：用户打开会话、下拉刷新、新设备同步
     * </pre>
     */
    default void getMessages(com.plato.gateway.GetMessagesRequest request,
        io.grpc.stub.StreamObserver<com.plato.gateway.GetMessagesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMessagesMethod(), responseObserver);
    }

    /**
     * <pre>
     * 更新消息状态 (撤回 / 删除)
     * 注意：这是软删除或状态位更新，不会物理删除 TiDB 里的数据
     * </pre>
     */
    default void updateMessageStatus(com.plato.gateway.UpdateMessageStatusRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateMessageStatusMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service StorageMessageService.
   * <pre>
   * =============================================================================
   * Service 1: 消息核心服务 (Message Service)
   * 职责：负责消息数据的落地 (TiDB)、查询 (Timeline) 和 状态变更。
   * =============================================================================
   * </pre>
   */
  public static abstract class StorageMessageServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return StorageMessageServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service StorageMessageService.
   * <pre>
   * =============================================================================
   * Service 1: 消息核心服务 (Message Service)
   * 职责：负责消息数据的落地 (TiDB)、查询 (Timeline) 和 状态变更。
   * =============================================================================
   * </pre>
   */
  public static final class StorageMessageServiceStub
      extends io.grpc.stub.AbstractAsyncStub<StorageMessageServiceStub> {
    private StorageMessageServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageMessageServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageMessageServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 单条保存 (最常用的接口)
     * 内部逻辑：Check Redis幂等 -&gt; Snowflake生成ID -&gt; Redis Incr Seq -&gt; TiDB Insert -&gt; Redis Inbox Update
     * </pre>
     */
    public void saveMessage(com.plato.gateway.SaveMessageRequest request,
        io.grpc.stub.StreamObserver<com.plato.gateway.SaveMessageResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSaveMessageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 批量保存 (用于消息转发、数据迁移、系统广播)
     * 内部逻辑：批量执行上述流程，建议内部使用并发处理
     * </pre>
     */
    public void saveMessages(com.plato.gateway.SaveMessagesRequest request,
        io.grpc.stub.StreamObserver<com.plato.gateway.SaveMessagesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSaveMessagesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 拉取消息历史 (Timeline Pull)
     * 场景：用户打开会话、下拉刷新、新设备同步
     * </pre>
     */
    public void getMessages(com.plato.gateway.GetMessagesRequest request,
        io.grpc.stub.StreamObserver<com.plato.gateway.GetMessagesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetMessagesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 更新消息状态 (撤回 / 删除)
     * 注意：这是软删除或状态位更新，不会物理删除 TiDB 里的数据
     * </pre>
     */
    public void updateMessageStatus(com.plato.gateway.UpdateMessageStatusRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateMessageStatusMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service StorageMessageService.
   * <pre>
   * =============================================================================
   * Service 1: 消息核心服务 (Message Service)
   * 职责：负责消息数据的落地 (TiDB)、查询 (Timeline) 和 状态变更。
   * =============================================================================
   * </pre>
   */
  public static final class StorageMessageServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<StorageMessageServiceBlockingStub> {
    private StorageMessageServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageMessageServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageMessageServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 单条保存 (最常用的接口)
     * 内部逻辑：Check Redis幂等 -&gt; Snowflake生成ID -&gt; Redis Incr Seq -&gt; TiDB Insert -&gt; Redis Inbox Update
     * </pre>
     */
    public com.plato.gateway.SaveMessageResponse saveMessage(com.plato.gateway.SaveMessageRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSaveMessageMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 批量保存 (用于消息转发、数据迁移、系统广播)
     * 内部逻辑：批量执行上述流程，建议内部使用并发处理
     * </pre>
     */
    public com.plato.gateway.SaveMessagesResponse saveMessages(com.plato.gateway.SaveMessagesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSaveMessagesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 拉取消息历史 (Timeline Pull)
     * 场景：用户打开会话、下拉刷新、新设备同步
     * </pre>
     */
    public com.plato.gateway.GetMessagesResponse getMessages(com.plato.gateway.GetMessagesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetMessagesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 更新消息状态 (撤回 / 删除)
     * 注意：这是软删除或状态位更新，不会物理删除 TiDB 里的数据
     * </pre>
     */
    public com.google.protobuf.Empty updateMessageStatus(com.plato.gateway.UpdateMessageStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateMessageStatusMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service StorageMessageService.
   * <pre>
   * =============================================================================
   * Service 1: 消息核心服务 (Message Service)
   * 职责：负责消息数据的落地 (TiDB)、查询 (Timeline) 和 状态变更。
   * =============================================================================
   * </pre>
   */
  public static final class StorageMessageServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<StorageMessageServiceFutureStub> {
    private StorageMessageServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageMessageServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageMessageServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 单条保存 (最常用的接口)
     * 内部逻辑：Check Redis幂等 -&gt; Snowflake生成ID -&gt; Redis Incr Seq -&gt; TiDB Insert -&gt; Redis Inbox Update
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.plato.gateway.SaveMessageResponse> saveMessage(
        com.plato.gateway.SaveMessageRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSaveMessageMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * 批量保存 (用于消息转发、数据迁移、系统广播)
     * 内部逻辑：批量执行上述流程，建议内部使用并发处理
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.plato.gateway.SaveMessagesResponse> saveMessages(
        com.plato.gateway.SaveMessagesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSaveMessagesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * 拉取消息历史 (Timeline Pull)
     * 场景：用户打开会话、下拉刷新、新设备同步
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.plato.gateway.GetMessagesResponse> getMessages(
        com.plato.gateway.GetMessagesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetMessagesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * 更新消息状态 (撤回 / 删除)
     * 注意：这是软删除或状态位更新，不会物理删除 TiDB 里的数据
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> updateMessageStatus(
        com.plato.gateway.UpdateMessageStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateMessageStatusMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SAVE_MESSAGE = 0;
  private static final int METHODID_SAVE_MESSAGES = 1;
  private static final int METHODID_GET_MESSAGES = 2;
  private static final int METHODID_UPDATE_MESSAGE_STATUS = 3;

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
        case METHODID_SAVE_MESSAGE:
          serviceImpl.saveMessage((com.plato.gateway.SaveMessageRequest) request,
              (io.grpc.stub.StreamObserver<com.plato.gateway.SaveMessageResponse>) responseObserver);
          break;
        case METHODID_SAVE_MESSAGES:
          serviceImpl.saveMessages((com.plato.gateway.SaveMessagesRequest) request,
              (io.grpc.stub.StreamObserver<com.plato.gateway.SaveMessagesResponse>) responseObserver);
          break;
        case METHODID_GET_MESSAGES:
          serviceImpl.getMessages((com.plato.gateway.GetMessagesRequest) request,
              (io.grpc.stub.StreamObserver<com.plato.gateway.GetMessagesResponse>) responseObserver);
          break;
        case METHODID_UPDATE_MESSAGE_STATUS:
          serviceImpl.updateMessageStatus((com.plato.gateway.UpdateMessageStatusRequest) request,
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
          getSaveMessageMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.plato.gateway.SaveMessageRequest,
              com.plato.gateway.SaveMessageResponse>(
                service, METHODID_SAVE_MESSAGE)))
        .addMethod(
          getSaveMessagesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.plato.gateway.SaveMessagesRequest,
              com.plato.gateway.SaveMessagesResponse>(
                service, METHODID_SAVE_MESSAGES)))
        .addMethod(
          getGetMessagesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.plato.gateway.GetMessagesRequest,
              com.plato.gateway.GetMessagesResponse>(
                service, METHODID_GET_MESSAGES)))
        .addMethod(
          getUpdateMessageStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.plato.gateway.UpdateMessageStatusRequest,
              com.google.protobuf.Empty>(
                service, METHODID_UPDATE_MESSAGE_STATUS)))
        .build();
  }

  private static abstract class StorageMessageServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    StorageMessageServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.plato.gateway.StorageGateway.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("StorageMessageService");
    }
  }

  private static final class StorageMessageServiceFileDescriptorSupplier
      extends StorageMessageServiceBaseDescriptorSupplier {
    StorageMessageServiceFileDescriptorSupplier() {}
  }

  private static final class StorageMessageServiceMethodDescriptorSupplier
      extends StorageMessageServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    StorageMessageServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (StorageMessageServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new StorageMessageServiceFileDescriptorSupplier())
              .addMethod(getSaveMessageMethod())
              .addMethod(getSaveMessagesMethod())
              .addMethod(getGetMessagesMethod())
              .addMethod(getUpdateMessageStatusMethod())
              .build();
        }
      }
    }
    return result;
  }
}
