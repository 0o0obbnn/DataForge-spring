package com.dataforge.web.config;

import com.dataforge.core.GeneratorFactory;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 指标配置类。
 *
 * <p>配置 Micrometer 指标收集，包括： - 业务指标（数据生成数量、生成时间、错误率等） - 系统指标（生成器数量、缓存命中率等） - 自定义指标
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnBean(MeterRegistry.class)
public class MetricsConfig {

  private final MeterRegistry meterRegistry;
  private final GeneratorFactory generatorFactory;

  public MetricsConfig(MeterRegistry meterRegistry, GeneratorFactory generatorFactory) {
    this.meterRegistry = meterRegistry;
    this.generatorFactory = generatorFactory;
  }

  /**
   * 注册生成器数量指标。
   *
   * @return Gauge 生成器数量指标
   */
  @Bean
  public Gauge generatorCountGauge() {
    return Gauge.builder(
            "dataforge.generators.count", generatorFactory, factory -> factory.getGeneratorCount())
        .description("Number of registered data generators")
        .register(meterRegistry);
  }

  /**
   * 注册活跃任务数指标。
   *
   * @return Gauge 活跃任务数指标
   */
  @Bean
  public Gauge activeTasksGauge() {
    AtomicLong activeTasks = new AtomicLong(0);
    return Gauge.builder("dataforge.tasks.active", activeTasks, AtomicLong::get)
        .description("Number of active data generation tasks")
        .register(meterRegistry);
  }
}
