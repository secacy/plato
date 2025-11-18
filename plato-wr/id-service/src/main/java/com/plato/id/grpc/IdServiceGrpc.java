package com.plato.id.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * ID 服务定义
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.1)",
    comments = "Source: id.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class IdServiceGrpc {

  private IdServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "idservice.IdService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.plato.id.grpc.IdServiceProto.GenerateIdRequest,
      com.plato.id.grpc.IdServiceProto.GenerateIdResponse> getGenerateIdMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GenerateId",
      requestType = com.plato.id.grpc.IdServiceProto.GenerateIdRequest.class,
      responseType = com.plato.id.grpc.IdServiceProto.GenerateIdResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.plato.id.grpc.IdServiceProto.GenerateIdRequest,
      com.plato.id.grpc.IdServiceProto.GenerateIdResponse> getGenerateIdMethod() {
    io.grpc.MethodDescriptor<com.plato.id.grpc.IdServiceProto.GenerateIdRequest, com.plato.id.grpc.IdServiceProto.GenerateIdResponse> getGenerateIdMethod;
    if ((getGenerateIdMethod = IdServiceGrpc.getGenerateIdMethod) == null) {
      synchronized (IdServiceGrpc.class) {
        if ((getGenerateIdMethod = IdServiceGrpc.getGenerateIdMethod) == null) {
          IdServiceGrpc.getGenerateIdMethod = getGenerateIdMethod =
              io.grpc.MethodDescriptor.<com.plato.id.grpc.IdServiceProto.GenerateIdRequest, com.plato.id.grpc.IdServiceProto.GenerateIdResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GenerateId"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.id.grpc.IdServiceProto.GenerateIdRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.id.grpc.IdServiceProto.GenerateIdResponse.getDefaultInstance()))
              .setSchemaDescriptor(new IdServiceMethodDescriptorSupplier("GenerateId"))
              .build();
        }
      }
    }
    return getGenerateIdMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static IdServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<IdServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<IdServiceStub>() {
        @java.lang.Override
        public IdServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new IdServiceStub(channel, callOptions);
        }
      };
    return IdServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static IdServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<IdServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<IdServiceBlockingStub>() {
        @java.lang.Override
        public IdServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new IdServiceBlockingStub(channel, callOptions);
        }
      };
    return IdServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static IdServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<IdServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<IdServiceFutureStub>() {
        @java.lang.Override
        public IdServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new IdServiceFutureStub(channel, callOptions);
        }
      };
    return IdServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * ID 服务定义
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * 定义一个 'GenerateId' 方法
     * </pre>
     */
    default void generateId(com.plato.id.grpc.IdServiceProto.GenerateIdRequest request,
        io.grpc.stub.StreamObserver<com.plato.id.grpc.IdServiceProto.GenerateIdResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGenerateIdMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service IdService.
   * <pre>
   * ID 服务定义
   * </pre>
   */
  public static abstract class IdServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return IdServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service IdService.
   * <pre>
   * ID 服务定义
   * </pre>
   */
  public static final class IdServiceStub
      extends io.grpc.stub.AbstractAsyncStub<IdServiceStub> {
    private IdServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected IdServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new IdServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 定义一个 'GenerateId' 方法
     * </pre>
     */
    public void generateId(com.plato.id.grpc.IdServiceProto.GenerateIdRequest request,
        io.grpc.stub.StreamObserver<com.plato.id.grpc.IdServiceProto.GenerateIdResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGenerateIdMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service IdService.
   * <pre>
   * ID 服务定义
   * </pre>
   */
  public static final class IdServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<IdServiceBlockingStub> {
    private IdServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected IdServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new IdServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 定义一个 'GenerateId' 方法
     * </pre>
     */
    public com.plato.id.grpc.IdServiceProto.GenerateIdResponse generateId(com.plato.id.grpc.IdServiceProto.GenerateIdRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGenerateIdMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service IdService.
   * <pre>
   * ID 服务定义
   * </pre>
   */
  public static final class IdServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<IdServiceFutureStub> {
    private IdServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected IdServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new IdServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 定义一个 'GenerateId' 方法
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.plato.id.grpc.IdServiceProto.GenerateIdResponse> generateId(
        com.plato.id.grpc.IdServiceProto.GenerateIdRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGenerateIdMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GENERATE_ID = 0;

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
        case METHODID_GENERATE_ID:
          serviceImpl.generateId((com.plato.id.grpc.IdServiceProto.GenerateIdRequest) request,
              (io.grpc.stub.StreamObserver<com.plato.id.grpc.IdServiceProto.GenerateIdResponse>) responseObserver);
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
          getGenerateIdMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.plato.id.grpc.IdServiceProto.GenerateIdRequest,
              com.plato.id.grpc.IdServiceProto.GenerateIdResponse>(
                service, METHODID_GENERATE_ID)))
        .build();
  }

  private static abstract class IdServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    IdServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.plato.id.grpc.IdServiceProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("IdService");
    }
  }

  private static final class IdServiceFileDescriptorSupplier
      extends IdServiceBaseDescriptorSupplier {
    IdServiceFileDescriptorSupplier() {}
  }

  private static final class IdServiceMethodDescriptorSupplier
      extends IdServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    IdServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (IdServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new IdServiceFileDescriptorSupplier())
              .addMethod(getGenerateIdMethod())
              .build();
        }
      }
    }
    return result;
  }
}
