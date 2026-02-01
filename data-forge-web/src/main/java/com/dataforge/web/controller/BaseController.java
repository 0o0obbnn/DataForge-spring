package com.dataforge.web.controller;

import com.dataforge.web.model.ApiResponse;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 控制器基类，提供公共的响应构建方法。
 *
 * <p>所有控制器应继承此类以获得统一的响应格式处理能力。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public abstract class BaseController {

  /**
   * 构建成功的API响应。
   *
   * @param data 响应数据
   * @param message 响应消息
   * @param status HTTP状态码
   * @param <T> 数据类型
   * @return ResponseEntity包装的ApiResponse对象
   */
  protected <T> ResponseEntity<ApiResponse<T>> buildResponse(
      T data, String message, HttpStatus status) {
    ApiResponse<T> response = new ApiResponse<>();
    response.setCode(status.value());
    response.setMessage(message);
    response.setData(data);
    response.setTimestamp(LocalDateTime.now());
    return ResponseEntity.status(status).body(response);
  }

  /**
   * 构建成功的API响应（HTTP 200）。
   *
   * @param data 响应数据
   * @param message 响应消息
   * @param <T> 数据类型
   * @return ResponseEntity包装的ApiResponse对象
   */
  protected <T> ResponseEntity<ApiResponse<T>> buildSuccessResponse(T data, String message) {
    return buildResponse(data, message, HttpStatus.OK);
  }

  /**
   * 构建创建成功的API响应（HTTP 201）。
   *
   * @param data 响应数据
   * @param message 响应消息
   * @param <T> 数据类型
   * @return ResponseEntity包装的ApiResponse对象
   */
  protected <T> ResponseEntity<ApiResponse<T>> buildCreatedResponse(T data, String message) {
    return buildResponse(data, message, HttpStatus.CREATED);
  }

  /**
   * 构建错误的API响应。
   *
   * @param message 错误消息
   * @param status HTTP状态码
   * @param <T> 数据类型
   * @return ResponseEntity包装的ApiResponse对象
   */
  protected <T> ResponseEntity<ApiResponse<T>> buildErrorResponse(
      String message, HttpStatus status) {
    return buildResponse(null, message, status);
  }

  /**
   * 构建内部服务器错误响应（HTTP 500）。
   *
   * @param message 错误消息
   * @param <T> 数据类型
   * @return ResponseEntity包装的ApiResponse对象
   */
  protected <T> ResponseEntity<ApiResponse<T>> buildInternalErrorResponse(String message) {
    return buildErrorResponse(message, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * 构建未找到资源响应（HTTP 404）。
   *
   * @param message 错误消息
   * @param <T> 数据类型
   * @return ResponseEntity包装的ApiResponse对象
   */
  protected <T> ResponseEntity<ApiResponse<T>> buildNotFoundResponse(String message) {
    return buildErrorResponse(message, HttpStatus.NOT_FOUND);
  }

  /**
   * 构建错误请求响应（HTTP 400）。
   *
   * @param message 错误消息
   * @param <T> 数据类型
   * @return ResponseEntity包装的ApiResponse对象
   */
  protected <T> ResponseEntity<ApiResponse<T>> buildBadRequestResponse(String message) {
    return buildErrorResponse(message, HttpStatus.BAD_REQUEST);
  }
}
