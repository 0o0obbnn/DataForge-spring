package com.dataforge.web.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/**
 * 指标服务。
 *
 * <p>提供业务指标收集功能，包括： - 数据生成请求计数 - 数据生成耗时统计 - 生成记录数统计 - 成功/失败计数
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Service
public class MetricsService {

  private final Counter dataGenerationCounter;
  private final Counter dataGenerationSuccessCounter;
  private final Counter dataGenerationFailureCounter;
  private final Timer dataGenerationTimer;
  private final Counter recordsGeneratedCounter;

  public MetricsService(MeterRegistry meterRegistry) {
    this.dataGenerationCounter =
        Counter.builder("dataforge.generation.count")
            .description("Total number of data generation requests")
            .tag("type", "total")
            .register(meterRegistry);

    this.dataGenerationSuccessCounter =
        Counter.builder("dataforge.generation.success")
            .description("Number of successful data generation requests")
            .tag("status", "success")
            .register(meterRegistry);

    this.dataGenerationFailureCounter =
        Counter.builder("dataforge.generation.failure")
            .description("Number of failed data generation requests")
            .tag("status", "failure")
            .register(meterRegistry);

    this.dataGenerationTimer =
        Timer.builder("dataforge.generation.duration")
            .description("Data generation duration in milliseconds")
            .register(meterRegistry);

    this.recordsGeneratedCounter =
        Counter.builder("dataforge.records.generated")
            .description("Total number of records generated")
            .register(meterRegistry);
  }

  /** 记录数据生成请求。 */
  public void recordGenerationRequest() {
    dataGenerationCounter.increment();
  }

  /**
   * 记录数据生成成功。
   *
   * @param recordCount 生成的记录数
   * @param durationMs 生成耗时（毫秒）
   */
  public void recordGenerationSuccess(long recordCount, long durationMs) {
    dataGenerationSuccessCounter.increment();
    recordsGeneratedCounter.increment(recordCount);
    dataGenerationTimer.record(durationMs, TimeUnit.MILLISECONDS);
  }

  /** 记录数据生成失败。 */
  public void recordGenerationFailure() {
    dataGenerationFailureCounter.increment();
  }

  /**
   * 记录生成的记录数。
   *
   * @param count 记录数
   */
  public void recordRecordsGenerated(long count) {
    recordsGeneratedCounter.increment(count);
  }
}
