package com.dataforge.web.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * DataForge 任务相关配置（最近任务列表默认分页大小等）。
 *
 * <p>集中管理任务列表等配置，避免魔法数字。
 *
 * <pre>
 * dataforge:
 *   tasks:
 *     default-page-size: 10
 * </pre>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "dataforge.tasks")
@Validated
public class DataForgeTasksProperties {

  /** 最近任务列表默认每页条数 */
  @Min(1)
  @Max(1000)
  private int defaultPageSize = 10;

  public int getDefaultPageSize() {
    return defaultPageSize;
  }

  public void setDefaultPageSize(int defaultPageSize) {
    this.defaultPageSize = defaultPageSize;
  }
}
