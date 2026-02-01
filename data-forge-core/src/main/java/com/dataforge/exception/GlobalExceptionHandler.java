package com.dataforge.exception;

import com.dataforge.service.ConfigurationException;
import com.dataforge.service.DataForgeException;
import com.dataforge.service.GenerationException;
import com.dataforge.service.SecurityException;
import com.dataforge.service.ValidationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

/**
 * 全局异常处理器（核心模块）。
 *
 * <p>此类不应在core模块中使用@ControllerAdvice， 因为core模块是纯业务逻辑层，不应依赖Web层注解。 Web模块有自己的GlobalExceptionHandler。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");

  /**
   * 处理安全异常。
   *
   * <p>安全异常需要特别关注，记录详细的安全日志， 但不向客户端暴露敏感的系统信息。
   */
  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ErrorResponse> handleSecurityException(
      SecurityException ex, WebRequest request) {

    // 记录安全日志
    securityLogger.error(
        "Security violation detected: {} | Request: {} | Context: {}",
        ex.getMessage(),
        request.getDescription(false),
        ex.getContext());

    // 创建安全的错误响应（不暴露敏感信息）
    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Security Violation")
            .message("请求被安全策略拒绝")
            .errorCode(ex.getErrorCode())
            .path(extractPath(request))
            .build();

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
  }

  /**
   * 处理配置异常。
   *
   * <p>配置异常通常是用户错误，需要返回详细的错误信息 帮助用户理解和修正配置问题。
   */
  @ExceptionHandler(ConfigurationException.class)
  public ResponseEntity<ErrorResponse> handleConfigurationException(
      ConfigurationException ex, WebRequest request) {

    logger.warn("Configuration error: {} | Context: {}", ex.getMessage(), ex.getContext());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Configuration Error")
            .message(ex.getMessage())
            .errorCode(ex.getErrorCode())
            .path(extractPath(request))
            .details(createConfigurationErrorDetails(ex))
            .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /** 处理验证异常。 */
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      ValidationException ex, WebRequest request) {

    logger.warn("Validation error: {} | Context: {}", ex.getMessage(), ex.getContext());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Error")
            .message(ex.getMessage())
            .errorCode(ex.getErrorCode())
            .path(extractPath(request))
            .details(createValidationErrorDetails(ex))
            .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /** 处理生成异常。 */
  @ExceptionHandler(GenerationException.class)
  public ResponseEntity<ErrorResponse> handleGenerationException(
      GenerationException ex, WebRequest request) {

    logger.error("Generation error: {} | Context: {}", ex.getMessage(), ex.getContext(), ex);

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Generation Error")
            .message("数据生成过程中发生错误")
            .errorCode(ex.getErrorCode())
            .path(extractPath(request))
            .details(createGenerationErrorDetails(ex))
            .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  /** 处理通用DataForge异常。 */
  @ExceptionHandler(DataForgeException.class)
  public ResponseEntity<ErrorResponse> handleDataForgeException(
      DataForgeException ex, WebRequest request) {

    logger.error("DataForge error: {} | Context: {}", ex.getMessage(), ex.getContext(), ex);

    HttpStatus status = determineHttpStatus(ex);

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error("DataForge Error")
            .message(ex.getMessage())
            .errorCode(ex.getErrorCode())
            .path(extractPath(request))
            .details(Map.of("level", ex.getLevel().name()))
            .build();

    return ResponseEntity.status(status).body(errorResponse);
  }

  /** 处理所有其他异常。 */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {

    logger.error("Unexpected error occurred", ex);

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("服务器内部错误，请稍后重试")
            .errorCode("INTERNAL_ERROR")
            .path(extractPath(request))
            .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  /** 创建配置错误详情。 */
  private Map<String, Object> createConfigurationErrorDetails(ConfigurationException ex) {
    Map<String, Object> details = new HashMap<>();

    if (ex.getFieldName() != null) {
      details.put("fieldName", ex.getFieldName());
    }

    if (ex.getConfigPath() != null) {
      details.put("configPath", ex.getConfigPath());
    }

    if (ex.getErrorType() != null) {
      details.put("errorType", ex.getErrorType().name());
    }

    // 添加修复建议
    details.put("suggestion", getConfigurationFixSuggestion(ex));

    return details;
  }

  /** 创建验证错误详情。 */
  private Map<String, Object> createValidationErrorDetails(ValidationException ex) {
    Map<String, Object> details = new HashMap<>();

    if (ex.getFieldName() != null) {
      details.put("fieldName", ex.getFieldName());
    }

    if (ex.getFieldValue() != null) {
      details.put("fieldValue", ex.getFieldValue().toString());
    }

    details.put("suggestion", "请检查输入数据的格式和取值范围");

    return details;
  }

  /** 创建生成错误详情。 */
  private Map<String, Object> createGenerationErrorDetails(GenerationException ex) {
    Map<String, Object> details = new HashMap<>();

    if (ex.getRecordIndex() != null) {
      details.put("recordIndex", ex.getRecordIndex());
    }

    details.put("suggestion", "请检查生成器配置和系统资源状况");

    return details;
  }

  /** 获取配置修复建议。 */
  private String getConfigurationFixSuggestion(ConfigurationException ex) {
    if (ex.getErrorType() != null) {
      switch (ex.getErrorType()) {
        case MISSING_REQUIRED:
          return "请添加必需的配置项";
        case INVALID_FORMAT:
          return "请检查配置值的格式是否正确";
        case OUT_OF_RANGE:
          return "请确保配置值在允许的范围内";
        case CONFLICT:
          return "请解决配置项之间的冲突";
        case UNSUPPORTED:
          return "请使用支持的配置选项";
        default:
          return "请检查配置文件的正确性";
      }
    }
    return "请检查配置文件的正确性";
  }

  /** 根据异常类型确定HTTP状态码。 */
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

  /** 从请求中提取路径信息。 */
  private String extractPath(WebRequest request) {
    String description = request.getDescription(false);
    if (description.startsWith("uri=")) {
      return description.substring(4);
    }
    return description;
  }

  /** 错误响应类。 */
  public static class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String errorCode;
    private String path;
    private Map<String, Object> details;

    // 私有构造函数，使用Builder模式
    private ErrorResponse() {}

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private final ErrorResponse response = new ErrorResponse();

      public Builder timestamp(LocalDateTime timestamp) {
        response.timestamp = timestamp;
        return this;
      }

      public Builder status(int status) {
        response.status = status;
        return this;
      }

      public Builder error(String error) {
        response.error = error;
        return this;
      }

      public Builder message(String message) {
        response.message = message;
        return this;
      }

      public Builder errorCode(String errorCode) {
        response.errorCode = errorCode;
        return this;
      }

      public Builder path(String path) {
        response.path = path;
        return this;
      }

      public Builder details(Map<String, Object> details) {
        response.details = details;
        return this;
      }

      public ErrorResponse build() {
        return response;
      }
    }

    // Getters
    public LocalDateTime getTimestamp() {
      return timestamp;
    }

    public int getStatus() {
      return status;
    }

    public String getError() {
      return error;
    }

    public String getMessage() {
      return message;
    }

    public String getErrorCode() {
      return errorCode;
    }

    public String getPath() {
      return path;
    }

    public Map<String, Object> getDetails() {
      return details;
    }
  }
}
