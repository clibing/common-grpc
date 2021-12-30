package com.clibing.grpc.exception;

import com.clibing.common.CommonCode;
import lombok.NoArgsConstructor;

/**
 * 通用状态码
 * 资源不存在
 *
 * @author liubaixun
 */
@NoArgsConstructor
public class ResourceNotFoundException extends CodeException {
  public ResourceNotFoundException(String message) {
    super(message);
  }

  @Override public int getCode() {
    return CommonCode.RESOURCE_NOT_FOUND_VALUE;
  }
}
