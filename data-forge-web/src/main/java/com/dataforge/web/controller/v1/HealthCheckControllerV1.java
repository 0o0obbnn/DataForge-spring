package com.dataforge.web.controller.v1;

import com.dataforge.web.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health Check API Controller - V1.
 *
 * <p>API版本1的端点，向后兼容。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/health/legacy")
@Tag(name = "Health Check V1", description = "健康检查API V1版本")
@Validated
public class HealthCheckControllerV1 {

  @GetMapping
  @Operation(summary = "健康检查", description = "检查API服务健康状态（V1版本）")
  public ResponseEntity<ApiResponse<HealthStatusV1>> checkHealth() {
    HealthStatusV1 status =
        HealthStatusV1.builder()
            .status("healthy")
            .version("1.0.0")
            .timestamp(LocalDateTime.now())
            .service("data-forge-web")
            .build();

    return ResponseEntity.ok(
        ApiResponse.<HealthStatusV1>builder()
            .code(HttpStatus.OK.value())
            .message("Health check passed")
            .data(status)
            .timestamp(LocalDateTime.now())
            .build());
  }

  @GetMapping("/ping")
  @Operation(summary = "Ping检查", description = "简单的ping检查（V1版本）")
  public ResponseEntity<ApiResponse<String>> ping() {
    return ResponseEntity.ok(
        ApiResponse.<String>builder()
            .code(HttpStatus.OK.value())
            .message("pong")
            .data("pong")
            .timestamp(LocalDateTime.now())
            .build());
  }

  /** 健康状态模型 V1。 */
  @lombok.Data
  @lombok.Builder
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class HealthStatusV1 {
    private String status;
    private String version;
    private LocalDateTime timestamp;
    private String service;
  }
}
