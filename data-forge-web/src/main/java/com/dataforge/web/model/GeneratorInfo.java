package com.dataforge.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成器信息模型。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratorInfo {

  /** 生成器类型 */
  private String type;

  /** 生成器描述 */
  private String description;

  /** 生成器类名 */
  private String className;

  /** 生成器优先级 */
  private int priority;

  /** 生成器使用次数 */
  private long usageCount;
}
