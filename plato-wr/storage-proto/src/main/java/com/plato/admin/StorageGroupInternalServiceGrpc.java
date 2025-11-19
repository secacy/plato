package com.plato.admin;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * =============================================================================
 * Service: 内部群组同步服务 (Internal Group Sync Service)
 * 职责：维护存储层所需的“群成员列表”副本，用于消息写扩散 (Fan-out)。
 * 调用方：业务层 (Business Layer) 在完成业务逻辑（如加群审批）后调用。
 * =============================================================================
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.1)",
    comments = "Source: storage_admin.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class StorageGroupInternalServiceGrpc {

  private StorageGroupInternalServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.plato.storage.admin.v1.StorageGroupInternalService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.plato.admin.ResetGroupMembersRequest,
      com.google.protobuf.Empty> getResetGroupMembersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ResetGroupMembers",
      requestType = com.plato.admin.ResetGroupMembersRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.plato.admin.ResetGroupMembersRequest,
      com.google.protobuf.Empty> getResetGroupMembersMethod() {
    io.grpc.MethodDescriptor<com.plato.admin.ResetGroupMembersRequest, com.google.protobuf.Empty> getResetGroupMembersMethod;
    if ((getResetGroupMembersMethod = StorageGroupInternalServiceGrpc.getResetGroupMembersMethod) == null) {
      synchronized (StorageGroupInternalServiceGrpc.class) {
        if ((getResetGroupMembersMethod = StorageGroupInternalServiceGrpc.getResetGroupMembersMethod) == null) {
          StorageGroupInternalServiceGrpc.getResetGroupMembersMethod = getResetGroupMembersMethod =
              io.grpc.MethodDescriptor.<com.plato.admin.ResetGroupMembersRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ResetGroupMembers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.admin.ResetGroupMembersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new StorageGroupInternalServiceMethodDescriptorSupplier("ResetGroupMembers"))
              .build();
        }
      }
    }
    return getResetGroupMembersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.plato.admin.PatchGroupMembersRequest,
      com.google.protobuf.Empty> getPatchGroupMembersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PatchGroupMembers",
      requestType = com.plato.admin.PatchGroupMembersRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.plato.admin.PatchGroupMembersRequest,
      com.google.protobuf.Empty> getPatchGroupMembersMethod() {
    io.grpc.MethodDescriptor<com.plato.admin.PatchGroupMembersRequest, com.google.protobuf.Empty> getPatchGroupMembersMethod;
    if ((getPatchGroupMembersMethod = StorageGroupInternalServiceGrpc.getPatchGroupMembersMethod) == null) {
      synchronized (StorageGroupInternalServiceGrpc.class) {
        if ((getPatchGroupMembersMethod = StorageGroupInternalServiceGrpc.getPatchGroupMembersMethod) == null) {
          StorageGroupInternalServiceGrpc.getPatchGroupMembersMethod = getPatchGroupMembersMethod =
              io.grpc.MethodDescriptor.<com.plato.admin.PatchGroupMembersRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PatchGroupMembers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.admin.PatchGroupMembersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new StorageGroupInternalServiceMethodDescriptorSupplier("PatchGroupMembers"))
              .build();
        }
      }
    }
    return getPatchGroupMembersMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static StorageGroupInternalServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageGroupInternalServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageGroupInternalServiceStub>() {
        @java.lang.Override
        public StorageGroupInternalServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageGroupInternalServiceStub(channel, callOptions);
        }
      };
    return StorageGroupInternalServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static StorageGroupInternalServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageGroupInternalServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageGroupInternalServiceBlockingStub>() {
        @java.lang.Override
        public StorageGroupInternalServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageGroupInternalServiceBlockingStub(channel, callOptions);
        }
      };
    return StorageGroupInternalServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static StorageGroupInternalServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageGroupInternalServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageGroupInternalServiceFutureStub>() {
        @java.lang.Override
        public StorageGroupInternalServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageGroupInternalServiceFutureStub(channel, callOptions);
        }
      };
    return StorageGroupInternalServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * =============================================================================
   * Service: 内部群组同步服务 (Internal Group Sync Service)
   * 职责：维护存储层所需的“群成员列表”副本，用于消息写扩散 (Fan-out)。
   * 调用方：业务层 (Business Layer) 在完成业务逻辑（如加群审批）后调用。
   * =============================================================================
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * 1. 全量同步/重置群成员 (Heavy Weight)
     * 场景：创建新群、群成员数据出现不一致时的修复、每日夜间校对
     * 逻辑：删除该 Session 在存储层的所有旧成员，替换为新列表
     * </pre>
     */
    default void resetGroupMembers(com.plato.admin.ResetGroupMembersRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getResetGroupMembersMethod(), responseObserver);
    }

    /**
     * <pre>
     * 2. 增量更新群成员 (Light Weight)
     * 场景：日常有人加入、退出、被踢
     * 逻辑：只对 diff 部分进行增删，性能更高
     * </pre>
     */
    default void patchGroupMembers(com.plato.admin.PatchGroupMembersRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPatchGroupMembersMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service StorageGroupInternalService.
   * <pre>
   * =============================================================================
   * Service: 内部群组同步服务 (Internal Group Sync Service)
   * 职责：维护存储层所需的“群成员列表”副本，用于消息写扩散 (Fan-out)。
   * 调用方：业务层 (Business Layer) 在完成业务逻辑（如加群审批）后调用。
   * =============================================================================
   * </pre>
   */
  public static abstract class StorageGroupInternalServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return StorageGroupInternalServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service StorageGroupInternalService.
   * <pre>
   * =============================================================================
   * Service: 内部群组同步服务 (Internal Group Sync Service)
   * 职责：维护存储层所需的“群成员列表”副本，用于消息写扩散 (Fan-out)。
   * 调用方：业务层 (Business Layer) 在完成业务逻辑（如加群审批）后调用。
   * =============================================================================
   * </pre>
   */
  public static final class StorageGroupInternalServiceStub
      extends io.grpc.stub.AbstractAsyncStub<StorageGroupInternalServiceStub> {
    private StorageGroupInternalServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageGroupInternalServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageGroupInternalServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 1. 全量同步/重置群成员 (Heavy Weight)
     * 场景：创建新群、群成员数据出现不一致时的修复、每日夜间校对
     * 逻辑：删除该 Session 在存储层的所有旧成员，替换为新列表
     * </pre>
     */
    public void resetGroupMembers(com.plato.admin.ResetGroupMembersRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getResetGroupMembersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 2. 增量更新群成员 (Light Weight)
     * 场景：日常有人加入、退出、被踢
     * 逻辑：只对 diff 部分进行增删，性能更高
     * </pre>
     */
    public void patchGroupMembers(com.plato.admin.PatchGroupMembersRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPatchGroupMembersMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service StorageGroupInternalService.
   * <pre>
   * =============================================================================
   * Service: 内部群组同步服务 (Internal Group Sync Service)
   * 职责：维护存储层所需的“群成员列表”副本，用于消息写扩散 (Fan-out)。
   * 调用方：业务层 (Business Layer) 在完成业务逻辑（如加群审批）后调用。
   * =============================================================================
   * </pre>
   */
  public static final class StorageGroupInternalServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<StorageGroupInternalServiceBlockingStub> {
    private StorageGroupInternalServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageGroupInternalServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageGroupInternalServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 1. 全量同步/重置群成员 (Heavy Weight)
     * 场景：创建新群、群成员数据出现不一致时的修复、每日夜间校对
     * 逻辑：删除该 Session 在存储层的所有旧成员，替换为新列表
     * </pre>
     */
    public com.google.protobuf.Empty resetGroupMembers(com.plato.admin.ResetGroupMembersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getResetGroupMembersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 2. 增量更新群成员 (Light Weight)
     * 场景：日常有人加入、退出、被踢
     * 逻辑：只对 diff 部分进行增删，性能更高
     * </pre>
     */
    public com.google.protobuf.Empty patchGroupMembers(com.plato.admin.PatchGroupMembersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPatchGroupMembersMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service StorageGroupInternalService.
   * <pre>
   * =============================================================================
   * Service: 内部群组同步服务 (Internal Group Sync Service)
   * 职责：维护存储层所需的“群成员列表”副本，用于消息写扩散 (Fan-out)。
   * 调用方：业务层 (Business Layer) 在完成业务逻辑（如加群审批）后调用。
   * =============================================================================
   * </pre>
   */
  public static final class StorageGroupInternalServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<StorageGroupInternalServiceFutureStub> {
    private StorageGroupInternalServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageGroupInternalServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageGroupInternalServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 1. 全量同步/重置群成员 (Heavy Weight)
     * 场景：创建新群、群成员数据出现不一致时的修复、每日夜间校对
     * 逻辑：删除该 Session 在存储层的所有旧成员，替换为新列表
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> resetGroupMembers(
        com.plato.admin.ResetGroupMembersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getResetGroupMembersMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * 2. 增量更新群成员 (Light Weight)
     * 场景：日常有人加入、退出、被踢
     * 逻辑：只对 diff 部分进行增删，性能更高
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> patchGroupMembers(
        com.plato.admin.PatchGroupMembersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPatchGroupMembersMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_RESET_GROUP_MEMBERS = 0;
  private static final int METHODID_PATCH_GROUP_MEMBERS = 1;

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
        case METHODID_RESET_GROUP_MEMBERS:
          serviceImpl.resetGroupMembers((com.plato.admin.ResetGroupMembersRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_PATCH_GROUP_MEMBERS:
          serviceImpl.patchGroupMembers((com.plato.admin.PatchGroupMembersRequest) request,
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
          getResetGroupMembersMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.plato.admin.ResetGroupMembersRequest,
              com.google.protobuf.Empty>(
                service, METHODID_RESET_GROUP_MEMBERS)))
        .addMethod(
          getPatchGroupMembersMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.plato.admin.PatchGroupMembersRequest,
              com.google.protobuf.Empty>(
                service, METHODID_PATCH_GROUP_MEMBERS)))
        .build();
  }

  private static abstract class StorageGroupInternalServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    StorageGroupInternalServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.plato.admin.StorageAdmin.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("StorageGroupInternalService");
    }
  }

  private static final class StorageGroupInternalServiceFileDescriptorSupplier
      extends StorageGroupInternalServiceBaseDescriptorSupplier {
    StorageGroupInternalServiceFileDescriptorSupplier() {}
  }

  private static final class StorageGroupInternalServiceMethodDescriptorSupplier
      extends StorageGroupInternalServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    StorageGroupInternalServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (StorageGroupInternalServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new StorageGroupInternalServiceFileDescriptorSupplier())
              .addMethod(getResetGroupMembersMethod())
              .addMethod(getPatchGroupMembersMethod())
              .build();
        }
      }
    }
    return result;
  }
}
