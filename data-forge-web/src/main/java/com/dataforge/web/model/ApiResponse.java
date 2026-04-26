package com.dataforge.web.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一的API响应格式。
 *
 * @param <T> 响应数据类型
 * @author DataForge Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

  /** 响应状态码 */
  private int code;

  /** 响应消息 */
  private String message;

  /** 响应数据 */
  private T data;

  /** 响应时间戳 */
  private LocalDateTime timestamp;

  /** 可选的上下文信息 */
  private Map<String, Object> context;

  /**
   * 创建成功响应。
   *
   * @param <T> 数据类型
   * @param data 响应数据
   * @return ApiResponse 成功响应
   */
  public static <T> ApiResponse<T> success(T data) {
    return ApiResponse.<T>builder()
        .code(200)
        .message("success")
        .data(data)
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * 创建成功响应，不带数据。
   *
   * @param <T> 数据类型
   * @return ApiResponse 成功响应
   */
  public static <T> ApiResponse<T> success() {
    return ApiResponse.<T>builder()
        .code(200)
        .message("success")
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * 创建错误响应。
   *
   * @param <T> 数据类型
   * @param code 错误码
   * @param message 错误消息
   * @return ApiResponse 错误响应
   */
  public static <T> ApiResponse<T> error(int code, String message) {
    return ApiResponse.<T>builder()
        .code(code)
        .message(message)
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * 创建错误响应，带上下文信息。
   *
   * @param <T> 数据类型
   * @param code 错误码
   * @param message 错误消息
   * @param context 上下文信息
   * @return ApiResponse 错误响应
   */
  public static <T> ApiResponse<T> error(int code, String message, Map<String, Object> context) {
    return ApiResponse.<T>builder()
        .code(code)
        .message(message)
        .timestamp(LocalDateTime.now())
        .context(context)
        .build();
  }
}
