package com.clibing.grpc;

import com.clibing.common.CommonCode;
import com.clibing.common.CommonStatus;
import com.clibing.grpc.exception.Assert;
import com.clibing.grpc.exception.ServerException;
import com.clibing.test.TestRequest;
import com.clibing.test.TestResponse;
import com.clibing.test.TestServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class GrpcUtilsTest {

    public static class TestService extends TestServiceGrpc.TestServiceImplBase {
        @Override
        public void test(TestRequest request, StreamObserver<TestResponse> responseObserver) {
            new GrpcUtils.StreamObserverResponse<TestRequest, TestResponse>(request, responseObserver) {
            }.execute(req -> {
                String token = req.getToken();
                Assert.isTrue(!"".equals(token), new ServerException("token empty"));
                return TestResponse.newBuilder()
                        .setStatus(CommonStatus.newBuilder().setCode(CommonCode.REQUEST_SUCCESS_VALUE).setMessage("request success").build())
                        .setData(TestResponse.Data.newBuilder().putAttribute("author", "clibing").putAttribute("token", token).build())
                        .build();
            });
        }
    }

    @BeforeClass
    public static void setup() {
        Server server = ServerBuilder.forPort(9100).addService(new TestService()).build();
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test() {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(String.format("%s:%s", "127.0.0.1", 9100)).usePlaintext().build();
        TestServiceGrpc.TestServiceBlockingStub testServiceBlockingStub = TestServiceGrpc.newBlockingStub(channel);
        TestResponse response = testServiceBlockingStub.test(TestRequest.newBuilder().setToken(UUID.randomUUID().toString()).build());
        org.junit.Assert.assertTrue(response.getStatus().getCode() == CommonCode.REQUEST_SUCCESS_VALUE);

        Map<String, String> attributeMap = response.getData().getAttributeMap();
        attributeMap.forEach((k, v) ->
                log.info("key: {}, value: {}", k, v)
        );
    }

    @Test
    public void testException() {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(String.format("%s:%s", "127.0.0.1", 9100)).usePlaintext().build();
        TestServiceGrpc.TestServiceBlockingStub testServiceBlockingStub = TestServiceGrpc.newBlockingStub(channel);
        TestResponse response = testServiceBlockingStub.test(TestRequest.newBuilder().build());
        org.junit.Assert.assertTrue(response.getStatus().getCode() == CommonCode.SERVER_ERROR_VALUE);

        log.info("message: {}", response.getStatus().getMessage());
    }
}