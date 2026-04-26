package com.dataforge.core.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 性能监控切面。
 *
 * <p>使用AOP技术自动收集关键方法的执行时间和调用次数。 对标注了 @MonitorPerformance 注解的方法进行监控。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Aspect
@Component
public class PerformanceMonitoringAspect {

  private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);

  private final PerformanceMonitoringService monitoringService;

  public PerformanceMonitoringAspect(PerformanceMonitoringService monitoringService) {
    this.monitoringService = monitoringService;
  }

  /**
   * 监控标注了 @MonitorPerformance 注解的方法。
   *
   * @param joinPoint 连接点
   * @param monitorPerformance 监控注解
   * @return 方法执行结果
   * @throws Throwable 方法执行异常
   */
  @Around("@annotation(monitorPerformance)")
  public Object monitorMethodPerformance(
      ProceedingJoinPoint joinPoint, MonitorPerformance monitorPerformance) throws Throwable {
    String methodName =
        joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
    String metricName =
        monitorPerformance.value().isEmpty() ? methodName : monitorPerformance.value();

    long startTime = System.currentTimeMillis();
    boolean success = false;

    try {
      // 增加调用计数
      monitoringService.incrementCounter(metricName + ".calls");

      // 执行目标方法
      Object result = joinPoint.proceed();
      success = true;
      return result;

    } catch (Throwable throwable) {
      // 记录异常
      monitoringService.incrementCounter(metricName + ".errors");
      LOGGER.debug("Method {} threw exception: {}", methodName, throwable.getMessage());
      throw throwable;

    } finally {
      long duration = System.currentTimeMillis() - startTime;

      // 记录执行时间
      monitoringService.recordTiming(metricName + ".duration", duration);

      if (success) {
        monitoringService.incrementCounter(metricName + ".success");
      }

      // 记录慢查询（超过阈值的方法调用）
      if (duration > monitorPerformance.slowThreshold()) {
        monitoringService.incrementCounter(metricName + ".slow");
        LOGGER.warn(
            "Slow method detected: {} took {}ms (threshold: {}ms)",
            methodName,
            duration,
            monitorPerformance.slowThreshold());
      }

      LOGGER.trace("Method {} completed in {}ms", methodName, duration);
    }
  }

  /** 监控数据生成服务的所有公共方法。 */
  @Around("execution(public * com.dataforge.service.DataForgeService.*(..))")
  public Object monitorDataForgeService(ProceedingJoinPoint joinPoint) throws Throwable {
    String methodName = joinPoint.getSignature().getName();
    String metricName = "dataforge.service." + methodName;

    long startTime = System.currentTimeMillis();
    boolean success = false;

    try {
      monitoringService.incrementCounter(metricName + ".calls");
      Object result = joinPoint.proceed();
      success = true;
      return result;

    } catch (Throwable throwable) {
      monitoringService.incrementCounter(metricName + ".errors");
      throw throwable;

    } finally {
      long duration = System.currentTimeMillis() - startTime;
      monitoringService.recordTiming(metricName + ".duration", duration);

      if (success) {
        monitoringService.incrementCounter(metricName + ".success");
      }
    }
  }

  /** 监控生成器工厂的关键方法。 */
  @Around(
      "execution(* com.dataforge.core.GeneratorFactory.getGenerator(..)) || "
          + "execution(* com.dataforge.core.GeneratorFactory.initialize(..))")
  public Object monitorGeneratorFactory(ProceedingJoinPoint joinPoint) throws Throwable {
    String methodName = joinPoint.getSignature().getName();
    String metricName = "generator.factory." + methodName;

    long startTime = System.currentTimeMillis();
    boolean success = false;

    try {
      monitoringService.incrementCounter(metricName + ".calls");
      Object result = joinPoint.proceed();
      success = true;
      return result;

    } catch (Throwable throwable) {
      monitoringService.incrementCounter(metricName + ".errors");
      throw throwable;

    } finally {
      long duration = System.currentTimeMillis() - startTime;
      monitoringService.recordTiming(metricName + ".duration", duration);

      if (success) {
        monitoringService.incrementCounter(metricName + ".success");
      }
    }
  }

  /** 监控数据生成器的generate方法。 */
  @Around("execution(* com.dataforge.generators.spi.DataGenerator.generate(..))")
  public Object monitorDataGenerator(ProceedingJoinPoint joinPoint) throws Throwable {
    String className = joinPoint.getTarget().getClass().getSimpleName();
    String metricName = "generator." + className.toLowerCase();

    long startTime = System.currentTimeMillis();
    boolean success = false;

    try {
      monitoringService.incrementCounter(metricName + ".generates");
      Object result = joinPoint.proceed();
      success = true;
      return result;

    } catch (Throwable throwable) {
      monitoringService.incrementCounter(metricName + ".errors");
      throw throwable;

    } finally {
      long duration = System.currentTimeMillis() - startTime;
      monitoringService.recordTiming(metricName + ".duration", duration);

      if (success) {
        monitoringService.incrementCounter(metricName + ".success");
      }
    }
  }
}
