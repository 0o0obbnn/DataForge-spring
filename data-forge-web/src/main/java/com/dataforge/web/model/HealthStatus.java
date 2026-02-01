package com.dataforge.web.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统健康状态模型。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatus {

  /** 系统状态 */
  private String status;

  /** 生成器数量 */
  private int generatorCount;

  /** 系统运行时间（毫秒） */
  private long uptime;

  /** 系统内存使用情况 */
  private Map<String, Object> memoryUsage;

  /** 系统线程使用情况 */
  private Map<String, Object> threadUsage;

  /** 缓存状态 */
  private Map<String, Object> cacheStatus;

  /** 系统版本信息 */
  private String version;
}
