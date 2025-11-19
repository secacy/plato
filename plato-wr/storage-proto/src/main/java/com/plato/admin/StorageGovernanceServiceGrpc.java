package com.plato.admin;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * =============================================================================
 * Service: 数据治理与合规服务 (Governance Service)
 * 职责：处理“非用户行为”的数据操作，如后台强制删除、反垃圾处理。
 * =============================================================================
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.1)",
    comments = "Source: storage_admin.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class StorageGovernanceServiceGrpc {

  private StorageGovernanceServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.plato.storage.admin.v1.StorageGovernanceService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.plato.admin.AdminRecallMessageRequest,
      com.google.protobuf.Empty> getAdminRecallMessageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AdminRecallMessage",
      requestType = com.plato.admin.AdminRecallMessageRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.plato.admin.AdminRecallMessageRequest,
      com.google.protobuf.Empty> getAdminRecallMessageMethod() {
    io.grpc.MethodDescriptor<com.plato.admin.AdminRecallMessageRequest, com.google.protobuf.Empty> getAdminRecallMessageMethod;
    if ((getAdminRecallMessageMethod = StorageGovernanceServiceGrpc.getAdminRecallMessageMethod) == null) {
      synchronized (StorageGovernanceServiceGrpc.class) {
        if ((getAdminRecallMessageMethod = StorageGovernanceServiceGrpc.getAdminRecallMessageMethod) == null) {
          StorageGovernanceServiceGrpc.getAdminRecallMessageMethod = getAdminRecallMessageMethod =
              io.grpc.MethodDescriptor.<com.plato.admin.AdminRecallMessageRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AdminRecallMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.admin.AdminRecallMessageRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new StorageGovernanceServiceMethodDescriptorSupplier("AdminRecallMessage"))
              .build();
        }
      }
    }
    return getAdminRecallMessageMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static StorageGovernanceServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageGovernanceServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageGovernanceServiceStub>() {
        @java.lang.Override
        public StorageGovernanceServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageGovernanceServiceStub(channel, callOptions);
        }
      };
    return StorageGovernanceServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static StorageGovernanceServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageGovernanceServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageGovernanceServiceBlockingStub>() {
        @java.lang.Override
        public StorageGovernanceServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageGovernanceServiceBlockingStub(channel, callOptions);
        }
      };
    return StorageGovernanceServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static StorageGovernanceServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageGovernanceServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageGovernanceServiceFutureStub>() {
        @java.lang.Override
        public StorageGovernanceServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageGovernanceServiceFutureStub(channel, callOptions);
        }
      };
    return StorageGovernanceServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * =============================================================================
   * Service: 数据治理与合规服务 (Governance Service)
   * 职责：处理“非用户行为”的数据操作，如后台强制删除、反垃圾处理。
   * =============================================================================
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * 1. 强制物理删除/屏蔽消息
     * 场景：内容安全审核（涉黄/涉政），需要让消息对所有人都不可见
     * 区别：用户侧撤回是 UpdateStatus，这里可能是物理覆写或强标记
     * </pre>
     */
    default void adminRecallMessage(com.plato.admin.AdminRecallMessageRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAdminRecallMessageMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service StorageGovernanceService.
   * <pre>
   * =============================================================================
   * Service: 数据治理与合规服务 (Governance Service)
   * 职责：处理“非用户行为”的数据操作，如后台强制删除、反垃圾处理。
   * =============================================================================
   * </pre>
   */
  public static abstract class StorageGovernanceServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return StorageGovernanceServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service StorageGovernanceService.
   * <pre>
   * =============================================================================
   * Service: 数据治理与合规服务 (Governance Service)
   * 职责：处理“非用户行为”的数据操作，如后台强制删除、反垃圾处理。
   * =============================================================================
   * </pre>
   */
  public static final class StorageGovernanceServiceStub
      extends io.grpc.stub.AbstractAsyncStub<StorageGovernanceServiceStub> {
    private StorageGovernanceServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageGovernanceServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageGovernanceServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 1. 强制物理删除/屏蔽消息
     * 场景：内容安全审核（涉黄/涉政），需要让消息对所有人都不可见
     * 区别：用户侧撤回是 UpdateStatus，这里可能是物理覆写或强标记
     * </pre>
     */
    public void adminRecallMessage(com.plato.admin.AdminRecallMessageRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAdminRecallMessageMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service StorageGovernanceService.
   * <pre>
   * =============================================================================
   * Service: 数据治理与合规服务 (Governance Service)
   * 职责：处理“非用户行为”的数据操作，如后台强制删除、反垃圾处理。
   * =============================================================================
   * </pre>
   */
  public static final class StorageGovernanceServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<StorageGovernanceServiceBlockingStub> {
    private StorageGovernanceServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageGovernanceServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageGovernanceServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 1. 强制物理删除/屏蔽消息
     * 场景：内容安全审核（涉黄/涉政），需要让消息对所有人都不可见
     * 区别：用户侧撤回是 UpdateStatus，这里可能是物理覆写或强标记
     * </pre>
     */
    public com.google.protobuf.Empty adminRecallMessage(com.plato.admin.AdminRecallMessageRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAdminRecallMessageMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service StorageGovernanceService.
   * <pre>
   * =============================================================================
   * Service: 数据治理与合规服务 (Governance Service)
   * 职责：处理“非用户行为”的数据操作，如后台强制删除、反垃圾处理。
   * =============================================================================
   * </pre>
   */
  public static final class StorageGovernanceServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<StorageGovernanceServiceFutureStub> {
    private StorageGovernanceServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageGovernanceServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageGovernanceServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 1. 强制物理删除/屏蔽消息
     * 场景：内容安全审核（涉黄/涉政），需要让消息对所有人都不可见
     * 区别：用户侧撤回是 UpdateStatus，这里可能是物理覆写或强标记
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> adminRecallMessage(
        com.plato.admin.AdminRecallMessageRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAdminRecallMessageMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_ADMIN_RECALL_MESSAGE = 0;

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
        case METHODID_ADMIN_RECALL_MESSAGE:
          serviceImpl.adminRecallMessage((com.plato.admin.AdminRecallMessageRequest) request,
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
          getAdminRecallMessageMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.plato.admin.AdminRecallMessageRequest,
              com.google.protobuf.Empty>(
                service, METHODID_ADMIN_RECALL_MESSAGE)))
        .build();
  }

  private static abstract class StorageGovernanceServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    StorageGovernanceServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.plato.admin.StorageAdmin.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("StorageGovernanceService");
    }
  }

  private static final class StorageGovernanceServiceFileDescriptorSupplier
      extends StorageGovernanceServiceBaseDescriptorSupplier {
    StorageGovernanceServiceFileDescriptorSupplier() {}
  }

  private static final class StorageGovernanceServiceMethodDescriptorSupplier
      extends StorageGovernanceServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    StorageGovernanceServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (StorageGovernanceServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new StorageGovernanceServiceFileDescriptorSupplier())
              .addMethod(getAdminRecallMessageMethod())
              .build();
        }
      }
    }
    return result;
  }
}
