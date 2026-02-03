package com.dataforge.web.exception;

/**
 * 资源未找到异常，用于由 GlobalExceptionHandler 统一映射为 HTTP 404。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }

  public ResourceNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
