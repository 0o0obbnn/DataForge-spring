package com.dataforge.monitoring;

import com.dataforge.security.SecurityConfiguration;
import com.dataforge.service.SecurityException;
import jakarta.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 资源监控器。
 *
 * <p>持续监控系统资源使用情况，包括内存、CPU、线程等， 在资源使用超过阈值时发出警告或抛出异常，防止资源耗尽攻击。
 *
 * <p><strong>监控指标：</strong>
 *
 * <ul>
 *   <li>堆内存使用率
 *   <li>非堆内存使用率
 *   <li>活跃线程数
 *   <li>CPU负载
 *   <li>GC频率和耗时
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class ResourceMonitor {

  private static final Logger logger = LoggerFactory.getLogger(ResourceMonitor.class);

  private final SecurityConfiguration securityConfig;
  private final MemoryMXBean memoryBean;
  private final ThreadMXBean threadBean;
  private final OperatingSystemMXBean osBean;

  // 监控状态
  private final AtomicBoolean memoryWarningIssued = new AtomicBoolean(false);
  private final AtomicBoolean criticalMemoryState = new AtomicBoolean(false);
  private final AtomicLong lastGcTime = new AtomicLong(0);
  private final AtomicLong lastMemoryCheck = new AtomicLong(0);

  public ResourceMonitor(SecurityConfiguration securityConfig) {
    this.securityConfig = securityConfig;
    this.memoryBean = ManagementFactory.getMemoryMXBean();
    this.threadBean = ManagementFactory.getThreadMXBean();
    this.osBean = ManagementFactory.getOperatingSystemMXBean();
  }

  @PostConstruct
  public void initialize() {
    if (securityConfig.isEnableResourceMonitoring()) {
      logger.info(
          "Resource monitoring enabled with warning threshold: {}%, error threshold: {}%",
          securityConfig.getMemoryWarningThreshold(), securityConfig.getMemoryErrorThreshold());
    } else {
      logger.info("Resource monitoring is disabled");
    }
  }

  /**
   * 检查内存使用情况。
   *
   * @throws SecurityException 当内存使用超过错误阈值时
   */
  public void checkMemoryUsage() throws SecurityException {
    if (!securityConfig.isEnableResourceMonitoring()) {
      return;
    }

    MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    double usageRatio = (double) heapUsage.getUsed() / heapUsage.getMax();
    int usagePercent = (int) (usageRatio * 100);

    lastMemoryCheck.set(System.currentTimeMillis());

    // 检查错误阈值
    if (usagePercent >= securityConfig.getMemoryErrorThreshold()) {
      criticalMemoryState.set(true);
      throw SecurityException.resourceExhaustion("内存", heapUsage.getUsed(), heapUsage.getMax())
          .withContext("usagePercent", usagePercent)
          .withContext("threshold", securityConfig.getMemoryErrorThreshold());
    }

    // 检查警告阈值
    if (usagePercent >= securityConfig.getMemoryWarningThreshold()) {
      if (!memoryWarningIssued.getAndSet(true)) {
        logger.warn(
            "Memory usage warning: {}% (threshold: {}%). " + "Used: {} MB, Max: {} MB",
            usagePercent,
            securityConfig.getMemoryWarningThreshold(),
            heapUsage.getUsed() / (1024 * 1024),
            heapUsage.getMax() / (1024 * 1024));

        // 建议进行垃圾回收
        suggestGarbageCollection();
      }
    } else {
      // 重置警告状态
      memoryWarningIssued.set(false);
      criticalMemoryState.set(false);
    }

    logger.trace("Memory check: {}% used", usagePercent);
  }

  /**
   * 检查线程使用情况。
   *
   * @throws SecurityException 当活跃线程数过多时
   */
  public void checkThreadUsage() throws SecurityException {
    if (!securityConfig.isEnableResourceMonitoring()) {
      return;
    }

    int activeThreads = threadBean.getThreadCount();
    int maxThreads = securityConfig.getMaxThreadCount();

    if (activeThreads > maxThreads) {
      throw SecurityException.resourceExhaustion("线程", activeThreads, maxThreads)
          .withContext("activeThreads", activeThreads)
          .withContext("daemonThreads", threadBean.getDaemonThreadCount());
    }

    // 警告阈值（80%）
    if (activeThreads > maxThreads * 0.8) {
      logger.warn("High thread usage: {} threads (max: {})", activeThreads, maxThreads);
    }

    logger.trace("Thread check: {} active threads", activeThreads);
  }

  /** 检查系统CPU负载。 */
  public void checkCpuUsage() {
    if (!securityConfig.isEnableResourceMonitoring()) {
      return;
    }

    double systemLoad = osBean.getSystemLoadAverage();
    int availableProcessors = osBean.getAvailableProcessors();

    if (systemLoad > 0) {
      double loadRatio = systemLoad / availableProcessors;
      if (loadRatio > 2.0) { // 负载超过2倍CPU核数
        logger.warn("High CPU load detected: {} (processors: {})", systemLoad, availableProcessors);
      }
    }

    logger.trace("CPU load: {}, processors: {}", systemLoad, availableProcessors);
  }

  /** 建议执行垃圾回收。 */
  private void suggestGarbageCollection() {
    long currentTime = System.currentTimeMillis();
    long lastGc = lastGcTime.get();

    // 限制GC频率，避免频繁GC影响性能
    if (currentTime - lastGc > 30_000) { // 30秒间隔
      if (lastGcTime.compareAndSet(lastGc, currentTime)) {
        logger.info("Suggesting garbage collection due to high memory usage");
        System.gc();
      }
    }
  }

  /**
   * 获取当前资源使用情况的详细报告。
   *
   * @return 资源使用报告
   */
  public ResourceUsageReport getUsageReport() {
    MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

    return new ResourceUsageReport(
        heapUsage.getUsed(),
        heapUsage.getMax(),
        nonHeapUsage.getUsed(),
        nonHeapUsage.getMax(),
        threadBean.getThreadCount(),
        threadBean.getDaemonThreadCount(),
        osBean.getSystemLoadAverage(),
        osBean.getAvailableProcessors(),
        criticalMemoryState.get());
  }

  /**
   * 检查是否处于临界内存状态。
   *
   * @return 如果内存使用临界则返回true
   */
  public boolean isCriticalMemoryState() {
    return criticalMemoryState.get();
  }

  /** 定期监控任务（每30秒执行一次）。 */
  @Scheduled(fixedRate = 30_000)
  public void periodicMonitoring() {
    if (!securityConfig.isEnableResourceMonitoring()) {
      return;
    }

    try {
      checkMemoryUsage();
      checkThreadUsage();
      checkCpuUsage();
    } catch (SecurityException e) {
      logger.error("Resource monitoring detected critical situation", e);
      // 这里可以触发告警或采取应急措施
    } catch (Exception e) {
      logger.error("Error during resource monitoring", e);
    }
  }

  /** 资源使用报告。 */
  public static class ResourceUsageReport {
    private final long heapUsed;
    private final long heapMax;
    private final long nonHeapUsed;
    private final long nonHeapMax;
    private final int threadCount;
    private final int daemonThreadCount;
    private final double systemLoad;
    private final int availableProcessors;
    private final boolean criticalMemory;

    public ResourceUsageReport(
        long heapUsed,
        long heapMax,
        long nonHeapUsed,
        long nonHeapMax,
        int threadCount,
        int daemonThreadCount,
        double systemLoad,
        int availableProcessors,
        boolean criticalMemory) {
      this.heapUsed = heapUsed;
      this.heapMax = heapMax;
      this.nonHeapUsed = nonHeapUsed;
      this.nonHeapMax = nonHeapMax;
      this.threadCount = threadCount;
      this.daemonThreadCount = daemonThreadCount;
      this.systemLoad = systemLoad;
      this.availableProcessors = availableProcessors;
      this.criticalMemory = criticalMemory;
    }

    // Getters
    public long getHeapUsed() {
      return heapUsed;
    }

    public long getHeapMax() {
      return heapMax;
    }

    public long getNonHeapUsed() {
      return nonHeapUsed;
    }

    public long getNonHeapMax() {
      return nonHeapMax;
    }

    public int getThreadCount() {
      return threadCount;
    }

    public int getDaemonThreadCount() {
      return daemonThreadCount;
    }

    public double getSystemLoad() {
      return systemLoad;
    }

    public int getAvailableProcessors() {
      return availableProcessors;
    }

    public boolean isCriticalMemory() {
      return criticalMemory;
    }

    public double getHeapUsagePercent() {
      return heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;
    }

    public double getNonHeapUsagePercent() {
      return nonHeapMax > 0 ? (double) nonHeapUsed / nonHeapMax * 100 : 0;
    }

    @Override
    public String toString() {
      return String.format(
          "ResourceUsageReport{heap=%.1f%% (%d/%d MB), nonHeap=%.1f%% (%d/%d MB), "
              + "threads=%d, load=%.2f, processors=%d, critical=%s}",
          getHeapUsagePercent(),
          heapUsed / (1024 * 1024),
          heapMax / (1024 * 1024),
          getNonHeapUsagePercent(),
          nonHeapUsed / (1024 * 1024),
          nonHeapMax / (1024 * 1024),
          threadCount,
          systemLoad,
          availableProcessors,
          criticalMemory);
    }
  }
}
