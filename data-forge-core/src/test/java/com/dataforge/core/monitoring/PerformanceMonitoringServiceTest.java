package com.dataforge.core.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * PerformanceMonitoringService 测试类
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("PerformanceMonitoringService 测试")
class PerformanceMonitoringServiceTest {

  private PerformanceMonitoringService service;

  @BeforeEach
  void setUp() {
    service = new PerformanceMonitoringService();
  }

  @Nested
  @DisplayName("计数器功能测试")
  class CounterTests {

    @Test
    @DisplayName("增加计数器应正确累加")
    void incrementCounterShouldAccumulate() {
      // Given
      String counterName = "test.counter";

      // When
      service.incrementCounter(counterName);
      service.incrementCounter(counterName);
      service.incrementCounter(counterName);

      // Then
      assertThat(service.getCounterValue(counterName)).isEqualTo(3);
    }

    @Test
    @DisplayName("增加计数器带增量应正确累加")
    void incrementCounterWithDeltaShouldAccumulate() {
      // Given
      String counterName = "test.counter";

      // When
      service.incrementCounter(counterName, 5);
      service.incrementCounter(counterName, 3);

      // Then
      assertThat(service.getCounterValue(counterName)).isEqualTo(8);
    }

    @Test
    @DisplayName("获取不存在的计数器应返回0")
    void getNonExistentCounterShouldReturnZero() {
      // When
      long value = service.getCounterValue("non.existent");

      // Then
      assertThat(value).isEqualTo(0);
    }

    @Test
    @DisplayName("多个计数器应独立统计")
    void multipleCountersShouldBeIndependent() {
      // When
      service.incrementCounter("counter.a", 10);
      service.incrementCounter("counter.b", 20);

      // Then
      assertThat(service.getCounterValue("counter.a")).isEqualTo(10);
      assertThat(service.getCounterValue("counter.b")).isEqualTo(20);
    }
  }

  @Nested
  @DisplayName("计时器功能测试")
  class TimerTests {

    @Test
    @DisplayName("记录计时信息应正确统计")
    void recordTimingShouldAccumulateStats() {
      // Given
      String timerName = "test.timer";

      // When
      service.recordTiming(timerName, 100);
      service.recordTiming(timerName, 200);
      service.recordTiming(timerName, 300);

      // Then
      PerformanceMonitoringService.TimingStats stats = service.getTimingStats(timerName);
      assertThat(stats).isNotNull();
      assertThat(stats.getCount()).isEqualTo(3);
      assertThat(stats.getTotalTime()).isEqualTo(600);
      assertThat(stats.getAverageTime()).isEqualTo(200.0);
      assertThat(stats.getMinTime()).isEqualTo(100);
      assertThat(stats.getMaxTime()).isEqualTo(300);
    }

    @Test
    @DisplayName("获取不存在的计时器应返回null")
    void getNonExistentTimerShouldReturnNull() {
      // When
      PerformanceMonitoringService.TimingStats stats = service.getTimingStats("non.existent");

      // Then
      assertThat(stats).isNull();
    }

    @Test
    @DisplayName("单个计时记录应正确统计")
    void singleTimingRecordShouldHaveCorrectStats() {
      // Given
      String timerName = "test.timer";

      // When
      service.recordTiming(timerName, 150);

      // Then
      PerformanceMonitoringService.TimingStats stats = service.getTimingStats(timerName);
      assertThat(stats.getCount()).isEqualTo(1);
      assertThat(stats.getTotalTime()).isEqualTo(150);
      assertThat(stats.getAverageTime()).isEqualTo(150.0);
      assertThat(stats.getMinTime()).isEqualTo(150);
      assertThat(stats.getMaxTime()).isEqualTo(150);
    }
  }

  @Nested
  @DisplayName("直方图功能测试")
  class HistogramTests {

    @Test
    @DisplayName("记录直方图数据应正确统计")
    void recordHistogramShouldAccumulateStats() {
      // Given
      String histogramName = "test.histogram";

      // When
      service.recordHistogram(histogramName, 10.5);
      service.recordHistogram(histogramName, 20.5);
      service.recordHistogram(histogramName, 30.5);

      // Then
      PerformanceMonitoringService.HistogramStats stats = service.getHistogramStats(histogramName);
      assertThat(stats).isNotNull();
      assertThat(stats.getCount()).isEqualTo(3);
      assertThat(stats.getSum()).isEqualTo(61.5);
      assertThat(stats.getAverage()).isEqualTo(20.5);
      assertThat(stats.getMin()).isEqualTo(10.5);
      assertThat(stats.getMax()).isEqualTo(30.5);
    }

    @Test
    @DisplayName("获取不存在的直方图应返回null")
    void getNonExistentHistogramShouldReturnNull() {
      // When
      PerformanceMonitoringService.HistogramStats stats = service.getHistogramStats("non.existent");

      // Then
      assertThat(stats).isNull();
    }

    @Test
    @DisplayName("单个直方图记录应正确统计")
    void singleHistogramRecordShouldHaveCorrectStats() {
      // Given
      String histogramName = "test.histogram";

      // When
      service.recordHistogram(histogramName, 42.0);

      // Then
      PerformanceMonitoringService.HistogramStats stats = service.getHistogramStats(histogramName);
      assertThat(stats.getCount()).isEqualTo(1);
      assertThat(stats.getSum()).isEqualTo(42.0);
      assertThat(stats.getAverage()).isEqualTo(42.0);
      assertThat(stats.getMin()).isEqualTo(42.0);
      assertThat(stats.getMax()).isEqualTo(42.0);
    }
  }

  @Nested
  @DisplayName("获取所有指标测试")
  class AllMetricsTests {

    @Test
    @DisplayName("获取所有指标应包含系统信息")
    void getAllMetricsShouldContainSystemInfo() {
      // When
      Map<String, Object> metrics = service.getAllMetrics();

      // Then
      assertThat(metrics).containsKey("system");
      @SuppressWarnings("unchecked")
      Map<String, Object> systemInfo = (Map<String, Object>) metrics.get("system");
      assertThat(systemInfo)
          .containsKeys(
              "uptime",
              "totalMemory",
              "freeMemory",
              "usedMemory",
              "maxMemory",
              "availableProcessors");
    }

    @Test
    @DisplayName("获取所有指标应包含计数器")
    void getAllMetricsShouldContainCounters() {
      // Given
      service.incrementCounter("test.counter", 5);

      // When
      Map<String, Object> metrics = service.getAllMetrics();

      // Then
      assertThat(metrics).containsKey("counters");
      @SuppressWarnings("unchecked")
      Map<String, Long> counters = (Map<String, Long>) metrics.get("counters");
      assertThat(counters).containsEntry("test.counter", 5L);
    }

    @Test
    @DisplayName("获取所有指标应包含计时器")
    void getAllMetricsShouldContainTimers() {
      // Given
      service.recordTiming("test.timer", 100);

      // When
      Map<String, Object> metrics = service.getAllMetrics();

      // Then
      assertThat(metrics).containsKey("timers");
      @SuppressWarnings("unchecked")
      Map<String, Map<String, Object>> timers =
          (Map<String, Map<String, Object>>) metrics.get("timers");
      assertThat(timers).containsKey("test.timer");
      assertThat(timers.get("test.timer"))
          .containsKeys("count", "totalTime", "averageTime", "minTime", "maxTime");
    }

    @Test
    @DisplayName("获取所有指标应包含直方图")
    void getAllMetricsShouldContainHistograms() {
      // Given
      service.recordHistogram("test.histogram", 50.0);

      // When
      Map<String, Object> metrics = service.getAllMetrics();

      // Then
      assertThat(metrics).containsKey("histograms");
      @SuppressWarnings("unchecked")
      Map<String, Map<String, Object>> histograms =
          (Map<String, Map<String, Object>>) metrics.get("histograms");
      assertThat(histograms).containsKey("test.histogram");
      assertThat(histograms.get("test.histogram"))
          .containsKeys("count", "min", "max", "average", "sum");
    }
  }

  @Nested
  @DisplayName("重置指标测试")
  class ResetMetricsTests {

    @Test
    @DisplayName("重置所有指标应清空所有数据")
    void resetAllMetricsShouldClearAllData() {
      // Given
      service.incrementCounter("test.counter", 10);
      service.recordTiming("test.timer", 100);
      service.recordHistogram("test.histogram", 50.0);

      // When
      service.resetAllMetrics();

      // Then
      assertThat(service.getCounterValue("test.counter")).isEqualTo(0);
      assertThat(service.getTimingStats("test.timer")).isNull();
      assertThat(service.getHistogramStats("test.histogram")).isNull();
    }
  }

  @Nested
  @DisplayName("TimingStats 类测试")
  class TimingStatsTests {

    @Test
    @DisplayName("TimingStats 应正确计算平均值")
    void timingStatsShouldCalculateAverage() {
      // Given
      PerformanceMonitoringService.TimingStats stats =
          new PerformanceMonitoringService.TimingStats();

      // When
      stats.record(100);
      stats.record(200);
      stats.record(300);

      // Then
      assertThat(stats.getAverageTime()).isEqualTo(200.0);
    }

    @Test
    @DisplayName("空 TimingStats 应返回0平均值")
    void emptyTimingStatsShouldReturnZeroAverage() {
      // Given
      PerformanceMonitoringService.TimingStats stats =
          new PerformanceMonitoringService.TimingStats();

      // Then
      assertThat(stats.getAverageTime()).isEqualTo(0.0);
      assertThat(stats.getMinTime()).isEqualTo(0);
      assertThat(stats.getMaxTime()).isEqualTo(0);
    }

    @Test
    @DisplayName("TimingStats 应正确追踪最小最大值")
    void timingStatsShouldTrackMinMax() {
      // Given
      PerformanceMonitoringService.TimingStats stats =
          new PerformanceMonitoringService.TimingStats();

      // When
      stats.record(500);
      stats.record(100);
      stats.record(1000);
      stats.record(200);

      // Then
      assertThat(stats.getMinTime()).isEqualTo(100);
      assertThat(stats.getMaxTime()).isEqualTo(1000);
    }
  }

  @Nested
  @DisplayName("HistogramStats 类测试")
  class HistogramStatsTests {

    @Test
    @DisplayName("HistogramStats 应正确计算平均值")
    void histogramStatsShouldCalculateAverage() {
      // Given
      PerformanceMonitoringService.HistogramStats stats =
          new PerformanceMonitoringService.HistogramStats();

      // When
      stats.record(10.0);
      stats.record(20.0);
      stats.record(30.0);

      // Then
      assertThat(stats.getAverage()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("空 HistogramStats 应返回0平均值")
    void emptyHistogramStatsShouldReturnZeroAverage() {
      // Given
      PerformanceMonitoringService.HistogramStats stats =
          new PerformanceMonitoringService.HistogramStats();

      // Then
      assertThat(stats.getAverage()).isEqualTo(0.0);
      assertThat(stats.getMin()).isEqualTo(0.0);
      assertThat(stats.getMax()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("HistogramStats 应正确追踪最小最大值")
    void histogramStatsShouldTrackMinMax() {
      // Given
      PerformanceMonitoringService.HistogramStats stats =
          new PerformanceMonitoringService.HistogramStats();

      // When
      stats.record(50.0);
      stats.record(10.0);
      stats.record(100.0);
      stats.record(25.0);

      // Then
      assertThat(stats.getMin()).isEqualTo(10.0);
      assertThat(stats.getMax()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("HistogramStats 应正确累加总和")
    void histogramStatsShouldAccumulateSum() {
      // Given
      PerformanceMonitoringService.HistogramStats stats =
          new PerformanceMonitoringService.HistogramStats();

      // When
      stats.record(5.5);
      stats.record(4.5);
      stats.record(10.0);

      // Then
      assertThat(stats.getSum()).isEqualTo(20.0);
    }
  }

  @Nested
  @DisplayName("并发测试")
  class ConcurrencyTests {

    @Test
    @DisplayName("并发增加计数器应保持正确性")
    void concurrentCounterIncrementShouldBeAccurate() throws InterruptedException {
      // Given
      String counterName = "concurrent.counter";
      int threadCount = 10;
      int incrementsPerThread = 100;

      // When
      java.util.concurrent.ExecutorService executor =
          java.util.concurrent.Executors.newFixedThreadPool(threadCount);
      java.util.concurrent.CountDownLatch latch =
          new java.util.concurrent.CountDownLatch(threadCount);

      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                for (int j = 0; j < incrementsPerThread; j++) {
                  service.incrementCounter(counterName);
                }
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
      executor.shutdown();

      // Then
      assertThat(service.getCounterValue(counterName)).isEqualTo(threadCount * incrementsPerThread);
    }

    @Test
    @DisplayName("并发记录计时器应保持正确性")
    void concurrentTimingRecordShouldBeAccurate() throws InterruptedException {
      // Given
      String timerName = "concurrent.timer";
      int threadCount = 5;
      int recordsPerThread = 20;

      // When
      java.util.concurrent.ExecutorService executor =
          java.util.concurrent.Executors.newFixedThreadPool(threadCount);
      java.util.concurrent.CountDownLatch latch =
          new java.util.concurrent.CountDownLatch(threadCount);

      for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        executor.submit(
            () -> {
              try {
                for (int j = 0; j < recordsPerThread; j++) {
                  service.recordTiming(timerName, (threadId + 1) * 10L);
                }
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
      executor.shutdown();

      // Then
      PerformanceMonitoringService.TimingStats stats = service.getTimingStats(timerName);
      assertThat(stats.getCount()).isEqualTo(threadCount * recordsPerThread);
    }
  }
}
