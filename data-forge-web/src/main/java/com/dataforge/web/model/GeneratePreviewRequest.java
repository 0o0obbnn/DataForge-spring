package com.dataforge.web.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据生成预览请求模型。
 *
 * <p>用于在Web端直接预览生成的测试数据，支持选择生成器类型和设置生成条数。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据生成预览请求，用于在Web端直接预览生成的测试数据")
public class GeneratePreviewRequest {

  /**
   * 数据生成器类型标识符。
   *
   * <p>必须是系统中已注册的生成器类型，如 email、uuid、name 等。
   */
  @NotBlank(message = "Generator type cannot be blank")
  @Schema(
      description = "生成器类型标识符，如 email、uuid、name、phone 等",
      example = "email",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String generatorType;

  /**
   * 要生成的记录数量。
   *
   * <p>预览模式下限制最多1000条，避免内存和性能问题。
   */
  @Min(value = 1, message = "Count must be at least 1")
  @Max(value = 1000, message = "Preview count must not exceed 1000")
  @Schema(
      description = "要生成的记录数量，预览模式下最多1000条",
      example = "10",
      minimum = "1",
      maximum = "1000",
      defaultValue = "10")
  @Builder.Default
  private int count = 10;

  /**
   * 生成器参数映射。
   *
   * <p>用于配置生成器的具体行为，如字符串长度范围、日期范围等。参数因生成器类型而异。
   */
  @Schema(
      description = "生成器参数，用于配置生成行为。不同生成器支持的参数不同",
      example = "{\"minLength\": 5, \"maxLength\": 20}")
  private Map<String, Object> params;
}
