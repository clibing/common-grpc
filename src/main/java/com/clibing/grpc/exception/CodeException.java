package com.clibing.grpc.exception;

import lombok.NoArgsConstructor;

/**
 * 注意proto统一采用错误状态码封装
 * <pre>
 *   message CommonStatus{
 *     int32 code = 1; // 状态码
 *     string message = 2; // 描述信息
 *   }
 *   message DemoResponse {
 *     CommonStatus status = 1; // 状态
 *   }
 * </pre>
 * 在反射时会自动封装数据
 *
 * @author liubaixun
 */
@NoArgsConstructor
public abstract class CodeException extends RuntimeException {
    /**
     * 获取错误码
     */
    public abstract int getCode();

    protected CodeException(String message, Throwable cause) {
        super(message, cause);
    }

    protected CodeException(String message) {
        super(message);
    }

    protected CodeException(Throwable cause) {
        super(cause);
    }
}
