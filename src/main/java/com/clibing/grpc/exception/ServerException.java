package com.clibing.grpc.exception;

import com.clibing.common.CommonCode;
import lombok.NoArgsConstructor;

/**
 * @author liubaixun
 */
@NoArgsConstructor
public class ServerException extends CodeException {

  public ServerException(String message) {
    super(message);
  }

  public ServerException(Throwable cause) {
    super(cause);
  }

  public ServerException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override public int getCode() {
    return CommonCode.SERVER_ERROR_VALUE;
  }
}
