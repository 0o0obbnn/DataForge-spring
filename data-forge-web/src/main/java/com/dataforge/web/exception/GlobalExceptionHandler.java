package com.dataforge.web.exception;

import com.dataforge.service.DataForgeException;
import com.dataforge.service.SecurityException;
import com.dataforge.util.SanitizationUtil;
import com.dataforge.web.model.ApiResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * 全局异常处理器 - 安全增强版。
 *
 * <p>支持敏感信息脱敏、错误追踪ID和详细日志记录。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String ERROR_ID_MDC_KEY = "errorId";

  /**
   * 生成唯一的错误追踪ID。
   *
   * @return 错误ID
   */
  private String generateErrorId() {
    return "ERR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  /**
   * 脱敏消息中的敏感信息。
   *
   * @param message 原始消息
   * @return 脱敏后的消息
   */
  private String sanitizeMessage(String message) {
    return SanitizationUtil.sanitize(message);
  }

  /**
   * 安全记录错误日志（脱敏敏感信息）。
   *
   * @param errorId 错误ID
   * @param exception 异常对象
   * @param request 请求信息
   */
  private void logErrorSecurity(String errorId, Exception exception, WebRequest request) {
    String sanitizedMessage = sanitizeMessage(exception.getMessage());
    String requestPath = request.getDescription(false).replace("uri=", "");

    log.error(
        "[{}] Error occurred: {} | Path: {} | Type: {}",
        errorId,
        sanitizedMessage,
        requestPath,
        exception.getClass().getSimpleName());

    // 记录异常栈（仅日志，不返回客户端）
    if (log.isDebugEnabled()) {
      log.debug("[{}] Stack trace:", errorId, exception);
    }
  }

  /**
   * 安全记录安全异常日志（不记录敏感信息）。
   *
   * @param errorId 错误ID
   * @param exception 安全异常对象
   * @param request 请求信息
   */
  private void logSecurityError(String errorId, SecurityException exception, WebRequest request) {
    // 安全异常不记录详细信息，避免泄露敏感配置
    String requestPath = request.getDescription(false).replace("uri=", "");

    log.error(
        "[{}] Security violation detected: Path: {} | ThreatType: {} | Level: {}",
        errorId,
        requestPath,
        exception.getThreatType() != null ? exception.getThreatType().name() : "UNKNOWN",
        exception.getLevel());
  }

  /**
   * 处理数据生成相关异常。
   *
   * @param ex DataForgeException 数据生成异常
   * @param request WebRequest 请求信息
   * @return ResponseEntity<ApiResponse<Object>> 错误响应
   */
  @ExceptionHandler(DataForgeException.class)
  public ResponseEntity<ApiResponse<Object>> handleDataForgeException(
      DataForgeException ex, WebRequest request) {

    String errorId = generateErrorId();
    MDC.put(ERROR_ID_MDC_KEY, errorId);

    try {
      logErrorSecurity(errorId, ex, request);

      Map<String, Object> context = new HashMap<>();
      context.put("errorId", errorId);
      context.put("errorType", ex.getClass().getSimpleName());
      context.put("requestPath", request.getDescription(false).replace("uri=", ""));

      // 检查是否为用户错误，返回详细错误信息
      String userMessage =
          ex.isUserError() ? ex.getMessage() : "Data generation failed. Please try again.";

      ApiResponse<Object> response =
          ApiResponse.error(determineHttpStatus(ex).value(), sanitizeMessage(userMessage), context);

      response.setTimestamp(LocalDateTime.now());

      return new ResponseEntity<>(response, determineHttpStatus(ex));
    } finally {
      MDC.remove(ERROR_ID_MDC_KEY);
    }
  }

  /**
   * 处理安全相关异常。
   *
   * <p>安全异常需要特别处理，不向客户端返回详细的错误信息。
   *
   * @param ex SecurityException 安全异常
   * @param request WebRequest 请求信息
   * @return ResponseEntity<ApiResponse<Object>> 错误响应
   */
  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ApiResponse<Object>> handleSecurityException(
      SecurityException ex, WebRequest request) {

    String errorId = generateErrorId();
    MDC.put(ERROR_ID_MDC_KEY, errorId);

    try {
      logSecurityError(errorId, ex, request);

      Map<String, Object> context = new HashMap<>();
      context.put("errorId", errorId);
      context.put("errorType", ex.getClass().getSimpleName());
      context.put("requestPath", request.getDescription(false).replace("uri=", ""));

      // 返回通用的安全错误消息，不泄露详细信息
      String userMessage =
          "Request rejected by security policy. If you believe this is an error, please contact support.";

      ApiResponse<Object> response =
          ApiResponse.error(HttpStatus.FORBIDDEN.value(), userMessage, context);

      response.setTimestamp(LocalDateTime.now());

      return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    } finally {
      MDC.remove(ERROR_ID_MDC_KEY);
    }
  }

  /**
   * 处理参数验证异常。
   *
   * @param ex MethodArgumentNotValidException 参数验证异常
   * @param request WebRequest 请求信息
   * @return ResponseEntity<ApiResponse<Object>> 错误响应
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Object>> handleValidationException(
      MethodArgumentNotValidException ex, WebRequest request) {

    String requestId = request.getHeader("X-Request-ID");
    log.debug(
        "Validation failed for request {}: {}", requestId, ex.getBindingResult().getAllErrors());

    Map<String, String> fieldErrors = new HashMap<>();

    // 收集字段验证错误
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.put(error.getField(), error.getDefaultMessage());
    }

    // 收集全局验证错误
    for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
      fieldErrors.put("global", error.getDefaultMessage());
    }

    Map<String, Object> context = new HashMap<>();
    context.put("errorType", ex.getClass().getSimpleName());
    context.put("fieldErrors", fieldErrors);
    context.put("requestPath", request.getDescription(false).replace("uri=", ""));

    ApiResponse<Object> response =
        ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Validation failed", context);

    response.setTimestamp(LocalDateTime.now());

    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * 处理通用异常。
   *
   * @param ex Exception 通用异常
   * @param request WebRequest 请求信息
   * @return ResponseEntity<ApiResponse<Object>> 错误响应
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> handleGlobalException(
      Exception ex, WebRequest request) {

    String errorId = generateErrorId();
    MDC.put(ERROR_ID_MDC_KEY, errorId);

    try {
      logErrorSecurity(errorId, ex, request);

      Map<String, Object> context = new HashMap<>();
      context.put("errorId", errorId);
      context.put("errorType", ex.getClass().getSimpleName());
      context.put("requestPath", request.getDescription(false).replace("uri=", ""));

      // 不向客户端暴露详细错误信息
      String userMessage = "An unexpected error occurred. Error ID: " + errorId;

      ApiResponse<Object> response =
          ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), userMessage, context);

      response.setTimestamp(LocalDateTime.now());

      return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    } finally {
      MDC.remove(ERROR_ID_MDC_KEY);
    }
  }

  /**
   * 处理API限流异常。
   *
   * @param ex RateLimitException 限流异常
   * @param request WebRequest 请求信息
   * @return ResponseEntity<ApiResponse<Object>> 429 Too Many Requests响应
   */
  @ExceptionHandler(com.dataforge.web.security.RateLimitAspect.RateLimitException.class)
  public ResponseEntity<ApiResponse<Object>> handleRateLimitException(
      com.dataforge.web.security.RateLimitAspect.RateLimitException ex, WebRequest request) {

    String errorId = generateErrorId();
    MDC.put(ERROR_ID_MDC_KEY, errorId);

    try {
      log.warn("[{}] Rate limit exceeded: Path: {}", errorId, request.getDescription(false));

      Map<String, Object> context = new HashMap<>();
      context.put("errorId", errorId);
      context.put("errorType", ex.getClass().getSimpleName());
      context.put("requestPath", request.getDescription(false).replace("uri=", ""));

      ApiResponse<Object> response =
          ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS.value(), ex.getMessage(), context);

      response.setTimestamp(LocalDateTime.now());

      return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
    } finally {
      MDC.remove(ERROR_ID_MDC_KEY);
    }
  }

  /**
   * 处理 CompletableFuture.join() 抛出的 CompletionException，解包后由对应 handler 处理（如 404/500）。
   *
   * @param ex CompletionException
   * @param request WebRequest
   * @return 由 cause 对应的 handler 返回的响应
   */
  @ExceptionHandler(CompletionException.class)
  public ResponseEntity<ApiResponse<Object>> handleCompletionException(
      CompletionException ex, WebRequest request) {
    Throwable cause = ex.getCause();
    if (cause instanceof RuntimeException runtimeException) {
      throw runtimeException;
    }
    if (cause instanceof Error error) {
      throw error;
    }
    throw new RuntimeException(cause != null ? cause : ex);
  }

  /**
   * 处理资源未找到异常。
   *
   * @param ex ResourceNotFoundException 资源未找到异常
   * @param request WebRequest 请求信息
   * @return ResponseEntity<ApiResponse<Object>> 404 Not Found响应
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
      ResourceNotFoundException ex, WebRequest request) {

    String errorId = generateErrorId();
    MDC.put(ERROR_ID_MDC_KEY, errorId);

    try {
      log.warn(
          "[{}] Resource not found: {} | Path: {}",
          errorId,
          sanitizeMessage(ex.getMessage()),
          request.getDescription(false));

      Map<String, Object> context = new HashMap<>();
      context.put("errorId", errorId);
      context.put("errorType", ex.getClass().getSimpleName());
      context.put("requestPath", request.getDescription(false).replace("uri=", ""));

      ApiResponse<Object> response =
          ApiResponse.error(
              HttpStatus.NOT_FOUND.value(), sanitizeMessage(ex.getMessage()), context);

      response.setTimestamp(LocalDateTime.now());

      return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    } finally {
      MDC.remove(ERROR_ID_MDC_KEY);
    }
  }

  /**
   * 处理非法参数异常。
   *
   * @param ex IllegalArgumentException 非法参数异常
   * @param request WebRequest 请求信息
   * @return ResponseEntity<ApiResponse<Object>> 400 Bad Request响应
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
      IllegalArgumentException ex, WebRequest request) {

    String errorId = generateErrorId();
    MDC.put(ERROR_ID_MDC_KEY, errorId);

    try {
      log.warn(
          "[{}] Invalid argument: {} | Path: {}",
          errorId,
          sanitizeMessage(ex.getMessage()),
          request.getDescription(false));

      Map<String, Object> context = new HashMap<>();
      context.put("errorId", errorId);
      context.put("errorType", ex.getClass().getSimpleName());
      context.put("requestPath", request.getDescription(false).replace("uri=", ""));

      ApiResponse<Object> response =
          ApiResponse.error(
              HttpStatus.BAD_REQUEST.value(), sanitizeMessage(ex.getMessage()), context);

      response.setTimestamp(LocalDateTime.now());

      return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    } finally {
      MDC.remove(ERROR_ID_MDC_KEY);
    }
  }

  /**
   * 根据异常类型确定HTTP状态码。
   *
   * @param ex 异常对象
   * @return HTTP状态码
   */
  private HttpStatus determineHttpStatus(DataForgeException ex) {
    if (ex.isUserError()) {
      return HttpStatus.BAD_REQUEST;
    } else if (ex.isBusinessError()) {
      return HttpStatus.UNPROCESSABLE_ENTITY;
    } else if (ex.isCritical()) {
      return HttpStatus.SERVICE_UNAVAILABLE;
    } else {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
  }
}
