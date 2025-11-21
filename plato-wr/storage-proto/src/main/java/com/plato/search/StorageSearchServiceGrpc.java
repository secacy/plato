package com.plato.search;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.1)",
    comments = "Source: storage_search.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class StorageSearchServiceGrpc {

  private StorageSearchServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.plato.gateway.StorageSearchService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.plato.search.SearchMessagesRequest,
      com.plato.search.SearchMessagesResponse> getSearchMessagesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SearchMessages",
      requestType = com.plato.search.SearchMessagesRequest.class,
      responseType = com.plato.search.SearchMessagesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.plato.search.SearchMessagesRequest,
      com.plato.search.SearchMessagesResponse> getSearchMessagesMethod() {
    io.grpc.MethodDescriptor<com.plato.search.SearchMessagesRequest, com.plato.search.SearchMessagesResponse> getSearchMessagesMethod;
    if ((getSearchMessagesMethod = StorageSearchServiceGrpc.getSearchMessagesMethod) == null) {
      synchronized (StorageSearchServiceGrpc.class) {
        if ((getSearchMessagesMethod = StorageSearchServiceGrpc.getSearchMessagesMethod) == null) {
          StorageSearchServiceGrpc.getSearchMessagesMethod = getSearchMessagesMethod =
              io.grpc.MethodDescriptor.<com.plato.search.SearchMessagesRequest, com.plato.search.SearchMessagesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SearchMessages"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.search.SearchMessagesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.plato.search.SearchMessagesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new StorageSearchServiceMethodDescriptorSupplier("SearchMessages"))
              .build();
        }
      }
    }
    return getSearchMessagesMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static StorageSearchServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageSearchServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageSearchServiceStub>() {
        @java.lang.Override
        public StorageSearchServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageSearchServiceStub(channel, callOptions);
        }
      };
    return StorageSearchServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static StorageSearchServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageSearchServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageSearchServiceBlockingStub>() {
        @java.lang.Override
        public StorageSearchServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageSearchServiceBlockingStub(channel, callOptions);
        }
      };
    return StorageSearchServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static StorageSearchServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageSearchServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageSearchServiceFutureStub>() {
        @java.lang.Override
        public StorageSearchServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageSearchServiceFutureStub(channel, callOptions);
        }
      };
    return StorageSearchServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * 搜索消息
     * 场景：用户在搜索框输入关键词，搜索“当前会话”或“所有会话”的历史记录
     * 注意：存储层默认认为调用方（业务层）已完成鉴权。
     * 存储层只负责根据传入的 scope (session_id 或 owner_id) 圈定数据范围进行检索。
     * </pre>
     */
    default void searchMessages(com.plato.search.SearchMessagesRequest request,
        io.grpc.stub.StreamObserver<com.plato.search.SearchMessagesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSearchMessagesMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service StorageSearchService.
   */
  public static abstract class StorageSearchServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return StorageSearchServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service StorageSearchService.
   */
  public static final class StorageSearchServiceStub
      extends io.grpc.stub.AbstractAsyncStub<StorageSearchServiceStub> {
    private StorageSearchServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageSearchServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageSearchServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 搜索消息
     * 场景：用户在搜索框输入关键词，搜索“当前会话”或“所有会话”的历史记录
     * 注意：存储层默认认为调用方（业务层）已完成鉴权。
     * 存储层只负责根据传入的 scope (session_id 或 owner_id) 圈定数据范围进行检索。
     * </pre>
     */
    public void searchMessages(com.plato.search.SearchMessagesRequest request,
        io.grpc.stub.StreamObserver<com.plato.search.SearchMessagesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSearchMessagesMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service StorageSearchService.
   */
  public static final class StorageSearchServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<StorageSearchServiceBlockingStub> {
    private StorageSearchServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageSearchServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageSearchServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 搜索消息
     * 场景：用户在搜索框输入关键词，搜索“当前会话”或“所有会话”的历史记录
     * 注意：存储层默认认为调用方（业务层）已完成鉴权。
     * 存储层只负责根据传入的 scope (session_id 或 owner_id) 圈定数据范围进行检索。
     * </pre>
     */
    public com.plato.search.SearchMessagesResponse searchMessages(com.plato.search.SearchMessagesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSearchMessagesMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service StorageSearchService.
   */
  public static final class StorageSearchServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<StorageSearchServiceFutureStub> {
    private StorageSearchServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StorageSearchServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageSearchServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 搜索消息
     * 场景：用户在搜索框输入关键词，搜索“当前会话”或“所有会话”的历史记录
     * 注意：存储层默认认为调用方（业务层）已完成鉴权。
     * 存储层只负责根据传入的 scope (session_id 或 owner_id) 圈定数据范围进行检索。
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.plato.search.SearchMessagesResponse> searchMessages(
        com.plato.search.SearchMessagesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSearchMessagesMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEARCH_MESSAGES = 0;

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
        case METHODID_SEARCH_MESSAGES:
          serviceImpl.searchMessages((com.plato.search.SearchMessagesRequest) request,
              (io.grpc.stub.StreamObserver<com.plato.search.SearchMessagesResponse>) responseObserver);
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
          getSearchMessagesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.plato.search.SearchMessagesRequest,
              com.plato.search.SearchMessagesResponse>(
                service, METHODID_SEARCH_MESSAGES)))
        .build();
  }

  private static abstract class StorageSearchServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    StorageSearchServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.plato.search.StorageSearch.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("StorageSearchService");
    }
  }

  private static final class StorageSearchServiceFileDescriptorSupplier
      extends StorageSearchServiceBaseDescriptorSupplier {
    StorageSearchServiceFileDescriptorSupplier() {}
  }

  private static final class StorageSearchServiceMethodDescriptorSupplier
      extends StorageSearchServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    StorageSearchServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (StorageSearchServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new StorageSearchServiceFileDescriptorSupplier())
              .addMethod(getSearchMessagesMethod())
              .build();
        }
      }
    }
    return result;
  }
}
