package com.dataforge.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据模板响应 DTO。
 *
 * <p>用于返回模板信息的 API 响应。
 *
 * @author DataForge Team
 * @since 1.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据模板响应")
public class DataTemplateResponseDTO {

  @Schema(description = "模板 ID", example = "1")
  private Long id;

  @Schema(description = "模板名称", example = "user-data-template")
  private String name;

  @Schema(description = "模板描述", example = "用于生成用户测试数据的模板")
  private String description;

  @Schema(description = "模板配置，JSON 格式的字段配置")
  private String config;

  @Schema(description = "创建时间")
  private LocalDateTime createdAt;

  @Schema(description = "更新时间")
  private LocalDateTime updatedAt;

  @Schema(description = "是否激活", example = "true")
  private boolean active;

  @Schema(description = "版本号", example = "1")
  private Integer version;
}
