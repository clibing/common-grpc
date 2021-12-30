package com.clibing.grpc;

import com.clibing.common.CommonCode;
import com.clibing.common.CommonStatus;
import com.clibing.grpc.exception.CodeException;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author liubaixun
 */
public class GrpcUtils {
    /**
     * 包装grpc的stub，增加请求头，
     * 使用场景：在grpc的拦截器获取请求头
     *
     * @param stub  stub
     * @param key   扩展头的key
     * @param value 扩展头的value
     * @param <T>   stub 的类型
     * @return
     */
    public static <T extends AbstractStub<T>> T wrapHeader(T stub, String key, String value) {
        return wrapHeader(stub, Attribute.builder().key(key).value(value).build());
    }

    public static <T extends AbstractStub<T>> T wrapHeader(T stub, Attribute... pairs) {
        Metadata header = new Metadata();
        for (Attribute attribute : pairs) {
            Metadata.Key<String> key = Metadata.Key.of(attribute.getKey(), Metadata.ASCII_STRING_MARSHALLER);
            header.put(key, attribute.getValue());
        }
        return MetadataUtils.attachHeaders(stub, header);
    }

    /**
     * 将stub包装timeout， 用户自定义设置
     *
     * @param stub     stub
     * @param timeout  超时值
     * @param timeUnit 超时单位
     * @param <T>
     * @return
     */
    public static <T extends AbstractStub<T>> T wrapTimeout(T stub, long timeout, TimeUnit timeUnit) {
        return stub.withDeadlineAfter(timeout, timeUnit);
    }


    @Data
    @Builder
    public static class Attribute {
        private String key;
        private String value;
    }

    /**
     * @author liubaixun
     * <p>
     * message的响应定义格式为:
     * <pre>
     *    message SingleResponse {
     *      CommonStatus status = 1; // status名字不允许修改, 修改后需要通过statusFieldName(String)方式设置
     *    }
     * </pre>
     */
    @Slf4j
    @SuppressWarnings("unchecked")
    public abstract static class StreamObserverResponse<Request, Response> {
        /**
         * grpc 请求 message
         */
        private final Request request;
        /**
         * grpc 响应 message
         */
        private final StreamObserver<Response> streamObserver;
        /**
         * 用户自定义异常处理
         */
        List<Function<Exception, Response>> functions = new ArrayList<>(0);
        /**
         * 获取响应class类型
         */
        private final Class<Response> responseClass;
        /**
         * message中统一定义的状态码在java中的字段的名字
         * 此字段用于反射赋值
         */
        private String fieldName = "status_";

        public StreamObserverResponse(Request request, StreamObserver<Response> streamObserver) {
            Type type = getClass().getGenericSuperclass();
            Type[] parameter = ((ParameterizedType) type).getActualTypeArguments();
            this.responseClass = (Class<Response>) parameter[1];
            this.request = request;
            this.streamObserver = streamObserver;
        }

        /**
         * 增加异常处理
         * 默认：不增加自定义异常处理， 系统会通过反射设置`fieldName`对应的值，状态为CommonCode.SERVER_ERROR_VALUE
         * 自定义: 命中自定义处理异常，处理完后自动结束grpc请求
         *
         * @param function
         * @return
         */
        public final StreamObserverResponse exception(Function<Exception, Response> function) {
            functions.add(function);
            return this;
        }

        /**
         * 设置返回的状态在grpc中的字段的名字，主要用于反射设置值
         *
         * @param fieldName 名字， grpc自动生成的字段带有前缀， 当传入的时候需要调用者传入java生成的完整值
         * @return
         */
        public final StreamObserverResponse statusFieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        /**
         * grpc处理包装方法，具体由子类实现Do接口进行业务处理
         *
         * @param handle 子类实现
         */
        public final void execute(Do<Request, Response> handle) {
            try {
                Response value = handle.execute(request);
                streamObserver.onNext(value);
                streamObserver.onCompleted();
            } catch (Exception e) {
                log.warn("执行GRPC异常", e);
                if (functions != null && functions.size() > 0) {
                    for (Function<Exception, ?> function : functions) {
                        Response r = (Response) function.apply(e);
                        streamObserver.onNext(r);
                        streamObserver.onCompleted();
                    }
                    return;
                }
                try {
                    /**
                     * Response rr = responseClass.newInstance();
                     * 不能使用 responseClass.newInstance()，因为默认构造器是private
                     */
                    Class clz = Class.forName(responseClass.getName());
                    Constructor constructor = clz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    Response obj = (Response) constructor.newInstance();
                    Field f = responseClass.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    final String message = Optional.ofNullable(e.getMessage()).orElse("");
                    if (e instanceof CodeException) {
                        f.set(obj, CommonStatus.newBuilder().setCode(((CodeException) e).getCode()).setMessage(message).build());
                    } else {
                        f.set(obj, CommonStatus.newBuilder().setCode(CommonCode.SERVER_ERROR_VALUE).setMessage(e.getMessage()).build());
                    }

                    streamObserver.onNext(obj);
                    streamObserver.onCompleted();
                    return;
                } catch (Exception ex) {
                    log.error("通过反射创建Response异常", ex);
                }
                log.error("调用Rpc处理类异常", e);
                throw new RuntimeException(e);
            }
        }

        /**
         * 具体的业务实现类
         *
         * @author liubaixun
         */
        public interface Do<Request, Response> {
            /**
             * 处理request
             *
             * @param request
             * @return
             */
            Response execute(Request request);
        }
    }

}
