package com.dataforge.core.monitoring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 性能监控服务。
 *
 * <p>提供系统性能指标的收集、统计和查询功能。 包括生成速度、内存使用、错误率等关键指标。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Service
public class PerformanceMonitoringService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceMonitoringService.class);

  /** 计数器映射，用于统计各种事件发生次数。 */
  private final Map<String, LongAdder> counters = new ConcurrentHashMap<>();

  /** 计时器映射，用于统计操作耗时。 */
  private final Map<String, TimingStats> timers = new ConcurrentHashMap<>();

  /** 直方图映射，用于统计数值分布。 */
  private final Map<String, HistogramStats> histograms = new ConcurrentHashMap<>();

  /** 监控开始时间。 */
  private final long startTime = System.currentTimeMillis();

  /**
   * 增加计数器。
   *
   * @param name 计数器名称
   * @param delta 增量，默认为1
   */
  public void incrementCounter(String name, long delta) {
    counters.computeIfAbsent(name, k -> new LongAdder()).add(delta);
  }

  /**
   * 增加计数器（增量为1）。
   *
   * @param name 计数器名称
   */
  public void incrementCounter(String name) {
    incrementCounter(name, 1);
  }

  /**
   * 记录计时信息。
   *
   * @param name 计时器名称
   * @param duration 持续时间（毫秒）
   */
  public void recordTiming(String name, long duration) {
    timers.computeIfAbsent(name, k -> new TimingStats()).record(duration);
  }

  /**
   * 记录直方图数据。
   *
   * @param name 直方图名称
   * @param value 数值
   */
  public void recordHistogram(String name, double value) {
    histograms.computeIfAbsent(name, k -> new HistogramStats()).record(value);
  }

  /**
   * 获取计数器值。
   *
   * @param name 计数器名称
   * @return 计数器值
   */
  public long getCounterValue(String name) {
    LongAdder counter = counters.get(name);
    return counter != null ? counter.sum() : 0;
  }

  /**
   * 获取计时统计信息。
   *
   * @param name 计时器名称
   * @return 计时统计信息
   */
  public TimingStats getTimingStats(String name) {
    return timers.get(name);
  }

  /**
   * 获取直方图统计信息。
   *
   * @param name 直方图名称
   * @return 直方图统计信息
   */
  public HistogramStats getHistogramStats(String name) {
    return histograms.get(name);
  }

  /**
   * 获取所有性能指标的汇总信息。
   *
   * @return 性能指标汇总
   */
  public Map<String, Object> getAllMetrics() {
    Map<String, Object> metrics = new ConcurrentHashMap<>();

    // 添加系统信息
    Map<String, Object> systemInfo = new ConcurrentHashMap<>();
    systemInfo.put("uptime", System.currentTimeMillis() - startTime);
    systemInfo.put("totalMemory", Runtime.getRuntime().totalMemory());
    systemInfo.put("freeMemory", Runtime.getRuntime().freeMemory());
    systemInfo.put(
        "usedMemory", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
    systemInfo.put("maxMemory", Runtime.getRuntime().maxMemory());
    systemInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
    metrics.put("system", systemInfo);

    // 添加计数器
    Map<String, Long> counterValues = new ConcurrentHashMap<>();
    counters.forEach((name, counter) -> counterValues.put(name, counter.sum()));
    metrics.put("counters", counterValues);

    // 添加计时器
    Map<String, Map<String, Object>> timerValues = new ConcurrentHashMap<>();
    timers.forEach(
        (name, stats) -> {
          Map<String, Object> timerData = new ConcurrentHashMap<>();
          timerData.put("count", stats.getCount());
          timerData.put("totalTime", stats.getTotalTime());
          timerData.put("averageTime", stats.getAverageTime());
          timerData.put("minTime", stats.getMinTime());
          timerData.put("maxTime", stats.getMaxTime());
          timerValues.put(name, timerData);
        });
    metrics.put("timers", timerValues);

    // 添加直方图
    Map<String, Map<String, Object>> histogramValues = new ConcurrentHashMap<>();
    histograms.forEach(
        (name, stats) -> {
          Map<String, Object> histogramData = new ConcurrentHashMap<>();
          histogramData.put("count", stats.getCount());
          histogramData.put("min", stats.getMin());
          histogramData.put("max", stats.getMax());
          histogramData.put("average", stats.getAverage());
          histogramData.put("sum", stats.getSum());
          histogramValues.put(name, histogramData);
        });
    metrics.put("histograms", histogramValues);

    return metrics;
  }

  /** 重置所有指标。 */
  public void resetAllMetrics() {
    counters.clear();
    timers.clear();
    histograms.clear();
    LOGGER.info("All performance metrics have been reset");
  }

  /** 计时统计信息内部类。 */
  public static class TimingStats {
    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong totalTime = new AtomicLong(0);
    private volatile long minTime = Long.MAX_VALUE;
    private volatile long maxTime = Long.MIN_VALUE;

    public synchronized void record(long duration) {
      count.incrementAndGet();
      totalTime.addAndGet(duration);
      minTime = Math.min(minTime, duration);
      maxTime = Math.max(maxTime, duration);
    }

    public long getCount() {
      return count.get();
    }

    public long getTotalTime() {
      return totalTime.get();
    }

    public double getAverageTime() {
      long countVal = count.get();
      return countVal > 0 ? (double) totalTime.get() / countVal : 0.0;
    }

    public long getMinTime() {
      return minTime == Long.MAX_VALUE ? 0 : minTime;
    }

    public long getMaxTime() {
      return maxTime == Long.MIN_VALUE ? 0 : maxTime;
    }
  }

  /** 直方图统计信息内部类。 */
  public static class HistogramStats {
    private final AtomicLong count = new AtomicLong(0);
    private volatile double sum = 0.0;
    private volatile double min = Double.MAX_VALUE;
    private volatile double max = Double.MIN_VALUE;

    public synchronized void record(double value) {
      count.incrementAndGet();
      sum += value;
      min = Math.min(min, value);
      max = Math.max(max, value);
    }

    public long getCount() {
      return count.get();
    }

    public double getSum() {
      return sum;
    }

    public double getAverage() {
      long countVal = count.get();
      return countVal > 0 ? sum / countVal : 0.0;
    }

    public double getMin() {
      return min == Double.MAX_VALUE ? 0.0 : min;
    }

    public double getMax() {
      return max == Double.MIN_VALUE ? 0.0 : max;
    }
  }
}
