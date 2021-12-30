package com.clibing.grpc.exception;

import java.util.function.Supplier;

/**
 * 断言
 *
 * @author liubaixun
 * <pre>
 *   try{
 *     boolean primitive = obj.getClass().isPrimitive();
 *     // 如果基本类型 直接返回 基本类型有默认值
 *     if (primitive) {
 *       return;
 *     }
 *   } catch (NullPointerException e) {}
 * </pre>
 */
public class Assert {
  public static <T extends CodeException> void isTrue(
      boolean value, Supplier<T> supplier) {
    if (value) {
      return;
    }
    throw supplier.get();
  }

  public static <T extends CodeException> void isTrue(
      boolean value, T t) {
    if (value) {
      return;
    }
    throw t;
  }

  public static <T, E extends CodeException> void notNull(T obj, Supplier<E> supplier) {
    if (obj != null) {
      return;
    }
    throw supplier.get();
  }
}

