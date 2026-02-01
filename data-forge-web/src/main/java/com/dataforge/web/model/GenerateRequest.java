package com.dataforge.web.model;

import com.dataforge.config.FieldConfigWrapper;
import com.dataforge.config.OutputConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据生成请求模型。
 *
 * <p>用于接收数据生成请求的参数，包括字段配置、输出格式、记录数量等。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据生成请求，包含字段配置、输出格式、记录数量等参数")
public class GenerateRequest {

  /** 要生成的记录数量 */
  @Min(value = 1, message = "Count must be at least 1")
  @Schema(description = "要生成的记录数量，必须大于等于1", example = "100", minimum = "1")
  private int count;

  /** 输出配置 */
  @NotNull(message = "Output configuration cannot be null")
  @Valid
  @Schema(
      description = "输出配置，包括输出格式（CSV、JSON、SQL等）、文件路径、编码等",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private OutputConfig output;

  /** 字段配置列表 */
  @NotEmpty(message = "Fields configuration cannot be empty")
  @Valid
  @Schema(
      description = "字段配置列表，定义每个字段的类型和参数。支持60+种数据生成器类型",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private List<FieldConfigWrapper> fields;

  /** 是否启用数据校验 */
  @Builder.Default
  @Schema(
      description = "是否启用数据校验，默认true。启用后会验证生成的数据是否符合规则（如身份证号、银行卡号等）",
      example = "true",
      defaultValue = "true")
  private boolean validate = true;

  /** 并发线程数 */
  @Min(value = 1, message = "Thread count must be at least 1")
  @Builder.Default
  @Schema(
      description = "并发线程数，用于提高大数据量生成性能。建议根据CPU核心数设置，默认1",
      example = "4",
      minimum = "1",
      defaultValue = "1")
  private int threads = 1;

  /** 随机种子，用于可重现的数据生成 */
  @Schema(description = "随机种子，用于可重现的数据生成。设置相同种子会生成相同的数据序列", example = "12345")
  private Long seed;
}
