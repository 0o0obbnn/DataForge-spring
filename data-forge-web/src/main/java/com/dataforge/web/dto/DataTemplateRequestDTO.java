package com.dataforge.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据模板创建/更新请求 DTO。
 *
 * <p>用于接收创建或更新模板的 API 请求参数。
 *
 * @author DataForge Team
 * @since 1.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据模板创建/更新请求")
public class DataTemplateRequestDTO {

  @NotBlank(message = "Template name is required")
  @Size(min = 1, max = 100, message = "Template name must be between 1 and 100 characters")
  @Schema(
      description = "模板名称，必须唯一",
      example = "user-data-template",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Size(max = 500, message = "Description cannot exceed 500 characters")
  @Schema(description = "模板描述", example = "用于生成用户测试数据的模板")
  private String description;

  @NotBlank(message = "Template config is required")
  @Schema(description = "模板配置，JSON 格式的字段配置", requiredMode = Schema.RequiredMode.REQUIRED)
  private String config;

  @Schema(description = "是否激活模板，默认 true", example = "true", defaultValue = "true")
  @Builder.Default
  private boolean active = true;
}
