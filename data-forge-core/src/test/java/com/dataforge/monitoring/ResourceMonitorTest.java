package com.dataforge.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dataforge.security.SecurityConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ResourceMonitor 测试类
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("ResourceMonitor 测试")
class ResourceMonitorTest {

  private SecurityConfiguration securityConfig;
  private ResourceMonitor monitor;

  @BeforeEach
  void setUp() {
    securityConfig = mock(SecurityConfiguration.class);
    when(securityConfig.isEnableResourceMonitoring()).thenReturn(true);
    when(securityConfig.getMemoryWarningThreshold()).thenReturn(80);
    when(securityConfig.getMemoryErrorThreshold()).thenReturn(95);
    when(securityConfig.getMaxThreadCount()).thenReturn(100);

    monitor = new ResourceMonitor(securityConfig);
  }

  @Nested
  @DisplayName("初始化测试")
  class InitializationTests {

    @Test
    @DisplayName("启用资源监控时应正确初始化")
    void shouldInitializeWithMonitoringEnabled() {
      // Given
      when(securityConfig.isEnableResourceMonitoring()).thenReturn(true);

      // When & Then - 不应抛出异常
      org.assertj.core.api.Assertions.assertThatCode(() -> monitor.initialize())
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("禁用资源监控时应正确初始化")
    void shouldInitializeWithMonitoringDisabled() {
      // Given
      when(securityConfig.isEnableResourceMonitoring()).thenReturn(false);

      // When & Then - 不应抛出异常
      org.assertj.core.api.Assertions.assertThatCode(() -> monitor.initialize())
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("内存使用检查测试")
  class MemoryUsageTests {

    @Test
    @DisplayName("禁用监控时内存检查应直接返回")
    void shouldReturnWhenMonitoringDisabled() {
      // Given
      when(securityConfig.isEnableResourceMonitoring()).thenReturn(false);

      // When & Then - 不应抛出异常
      org.assertj.core.api.Assertions.assertThatCode(() -> monitor.checkMemoryUsage())
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("正常内存使用不应抛出异常")
    void normalMemoryUsageShouldNotThrow() {
      // Given - 使用高阈值确保测试通过
      when(securityConfig.getMemoryWarningThreshold()).thenReturn(99);
      when(securityConfig.getMemoryErrorThreshold()).thenReturn(99);

      // When & Then
      org.assertj.core.api.Assertions.assertThatCode(() -> monitor.checkMemoryUsage())
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("临界内存状态检查应返回正确值")
    void criticalMemoryStateShouldReturnCorrectValue() {
      // Given - 初始状态
      assertThat(monitor.isCriticalMemoryState()).isFalse();
    }
  }

  @Nested
  @DisplayName("线程使用检查测试")
  class ThreadUsageTests {

    @Test
    @DisplayName("禁用监控时线程检查应直接返回")
    void shouldReturnWhenMonitoringDisabled() {
      // Given
      when(securityConfig.isEnableResourceMonitoring()).thenReturn(false);

      // When & Then - 不应抛出异常
      org.assertj.core.api.Assertions.assertThatCode(() -> monitor.checkThreadUsage())
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("正常线程数不应抛出异常")
    void normalThreadCountShouldNotThrow() {
      // Given - 设置一个很高的线程限制
      when(securityConfig.getMaxThreadCount()).thenReturn(10000);

      // When & Then
      org.assertj.core.api.Assertions.assertThatCode(() -> monitor.checkThreadUsage())
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("CPU 使用检查测试")
  class CpuUsageTests {

    @Test
    @DisplayName("禁用监控时 CPU 检查应直接返回")
    void shouldReturnWhenMonitoringDisabled() {
      // Given
      when(securityConfig.isEnableResourceMonitoring()).thenReturn(false);

      // When & Then - 不应抛出异常
      org.assertj.core.api.Assertions.assertThatCode(() -> monitor.checkCpuUsage())
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CPU 检查不应抛出异常")
    void cpuCheckShouldNotThrow() {
      // When & Then
      org.assertj.core.api.Assertions.assertThatCode(() -> monitor.checkCpuUsage())
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("资源使用报告测试")
  class UsageReportTests {

    @Test
    @DisplayName("获取资源使用报告应返回有效数据")
    void getUsageReportShouldReturnValidData() {
      // When
      ResourceMonitor.ResourceUsageReport report = monitor.getUsageReport();

      // Then
      assertThat(report).isNotNull();
      assertThat(report.getHeapUsed()).isGreaterThanOrEqualTo(0);
      assertThat(report.getHeapMax()).isGreaterThan(0);
      assertThat(report.getThreadCount()).isGreaterThan(0);
      assertThat(report.getAvailableProcessors()).isGreaterThan(0);
    }

    @Test
    @DisplayName("堆内存使用率计算应正确")
    void heapUsagePercentCalculationShouldBeCorrect() {
      // When
      ResourceMonitor.ResourceUsageReport report = monitor.getUsageReport();

      // Then
      double usagePercent = report.getHeapUsagePercent();
      assertThat(usagePercent).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("非堆内存使用率计算应正确")
    void nonHeapUsagePercentCalculationShouldBeCorrect() {
      // When
      ResourceMonitor.ResourceUsageReport report = monitor.getUsageReport();

      // Then
      double usagePercent = report.getNonHeapUsagePercent();
      assertThat(usagePercent).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("报告字符串表示应包含关键信息")
    void reportToStringShouldContainKeyInfo() {
      // When
      ResourceMonitor.ResourceUsageReport report = monitor.getUsageReport();
      String reportString = report.toString();

      // Then
      assertThat(reportString).contains("ResourceUsageReport");
      assertThat(reportString).contains("threads=");
      assertThat(reportString).contains("processors=");
    }

    @Test
    @DisplayName("守护线程数应小于等于总线程数")
    void daemonThreadCountShouldBeLessThanOrEqualToTotal() {
      // When
      ResourceMonitor.ResourceUsageReport report = monitor.getUsageReport();

      // Then
      assertThat(report.getDaemonThreadCount()).isLessThanOrEqualTo(report.getThreadCount());
    }

    @Test
    @DisplayName("临界内存状态应在报告中正确反映")
    void criticalMemoryStateShouldBeReflectedInReport() {
      // When
      ResourceMonitor.ResourceUsageReport report = monitor.getUsageReport();

      // Then
      assertThat(report.isCriticalMemory()).isFalse(); // 初始状态应为 false
    }
  }

  @Nested
  @DisplayName("定期监控测试")
  class PeriodicMonitoringTests {

    @Test
    @DisplayName("禁用监控时定期任务应直接返回")
    void shouldReturnWhenMonitoringDisabled() {
      // Given
      when(securityConfig.isEnableResourceMonitoring()).thenReturn(false);

      // When & Then - 不应抛出异常
      org.assertj.core.api.Assertions.assertThatCode(() -> monitor.periodicMonitoring())
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("定期监控任务不应抛出异常")
    void periodicMonitoringShouldNotThrow() {
      // Given - 使用高阈值确保测试通过
      when(securityConfig.getMemoryWarningThreshold()).thenReturn(99);
      when(securityConfig.getMemoryErrorThreshold()).thenReturn(99);
      when(securityConfig.getMaxThreadCount()).thenReturn(10000);

      // When & Then
      org.assertj.core.api.Assertions.assertThatCode(() -> monitor.periodicMonitoring())
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("ResourceUsageReport 类测试")
  class ResourceUsageReportTests {

    @Test
    @DisplayName("ResourceUsageReport 应正确存储所有值")
    void reportShouldStoreAllValues() {
      // Given
      long heapUsed = 100;
      long heapMax = 200;
      long nonHeapUsed = 50;
      long nonHeapMax = 100;
      int threadCount = 10;
      int daemonThreadCount = 5;
      double systemLoad = 0.5;
      int availableProcessors = 4;
      boolean criticalMemory = false;

      // When
      ResourceMonitor.ResourceUsageReport report =
          new ResourceMonitor.ResourceUsageReport(
              heapUsed,
              heapMax,
              nonHeapUsed,
              nonHeapMax,
              threadCount,
              daemonThreadCount,
              systemLoad,
              availableProcessors,
              criticalMemory);

      // Then
      assertThat(report.getHeapUsed()).isEqualTo(heapUsed);
      assertThat(report.getHeapMax()).isEqualTo(heapMax);
      assertThat(report.getNonHeapUsed()).isEqualTo(nonHeapUsed);
      assertThat(report.getNonHeapMax()).isEqualTo(nonHeapMax);
      assertThat(report.getThreadCount()).isEqualTo(threadCount);
      assertThat(report.getDaemonThreadCount()).isEqualTo(daemonThreadCount);
      assertThat(report.getSystemLoad()).isEqualTo(systemLoad);
      assertThat(report.getAvailableProcessors()).isEqualTo(availableProcessors);
      assertThat(report.isCriticalMemory()).isEqualTo(criticalMemory);
    }

    @Test
    @DisplayName("堆内存使用率为0当最大值为0")
    void heapUsagePercentShouldBeZeroWhenMaxIsZero() {
      // Given
      ResourceMonitor.ResourceUsageReport report =
          new ResourceMonitor.ResourceUsageReport(100, 0, 50, 100, 10, 5, 0.5, 4, false);

      // When
      double usagePercent = report.getHeapUsagePercent();

      // Then
      assertThat(usagePercent).isEqualTo(0);
    }

    @Test
    @DisplayName("非堆内存使用率为0当最大值为0")
    void nonHeapUsagePercentShouldBeZeroWhenMaxIsZero() {
      // Given
      ResourceMonitor.ResourceUsageReport report =
          new ResourceMonitor.ResourceUsageReport(100, 200, 50, 0, 10, 5, 0.5, 4, false);

      // When
      double usagePercent = report.getNonHeapUsagePercent();

      // Then
      assertThat(usagePercent).isEqualTo(0);
    }
  }
}
