package com.dataforge.core.uniqueness;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 唯一性过滤器工厂。
 *
 * <p>根据数据量和配置自动选择最优的过滤器实现。
 *
 * <p><strong>选择策略：</strong>
 *
 * <ul>
 *   <li>数据量 &lt; 100万：使用HashSet（准确性优先）
 *   <li>数据量 100万-1000万：使用BloomFilter（平衡性能和内存）
 *   <li>数据量 &gt; 1000万：使用BloomFilter（内存优先）
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class UniquenessFilterFactory {

  private static final Logger logger = LoggerFactory.getLogger(UniquenessFilterFactory.class);

  // 阈值配置
  private static final long SMALL_DATA_THRESHOLD = 1_000_000; // 100万
  private static final long MEDIUM_DATA_THRESHOLD = 10_000_000; // 1000万

  // 默认误判率配置
  private static final double DEFAULT_FPP = 0.01; // 1%
  private static final double LOW_FPP = 0.001; // 0.1%

  /** 过滤器类型枚举。 */
  public enum FilterType {
    /** HashSet实现 - 100%准确，内存占用大 */
    HASHSET,
    /** BloomFilter实现 - 概率性准确，内存占用小 */
    BLOOM_FILTER,
    /** 自动选择 - 根据数据量自动选择最优实现 */
    AUTO
  }

  /**
   * 创建唯一性过滤器（自动选择类型）。
   *
   * @param expectedCapacity 预期容量
   * @return 唯一性过滤器实例
   */
  public static UniquenessFilter create(long expectedCapacity) {
    return create(FilterType.AUTO, expectedCapacity, DEFAULT_FPP);
  }

  /**
   * 创建唯一性过滤器（指定类型）。
   *
   * @param filterType 过滤器类型
   * @param expectedCapacity 预期容量
   * @return 唯一性过滤器实例
   */
  public static UniquenessFilter create(FilterType filterType, long expectedCapacity) {
    return create(filterType, expectedCapacity, DEFAULT_FPP);
  }

  /**
   * 创建唯一性过滤器（完整配置）。
   *
   * @param filterType 过滤器类型
   * @param expectedCapacity 预期容量
   * @param falsePositiveProbability 误判率（仅对BloomFilter有效）
   * @return 唯一性过滤器实例
   */
  public static UniquenessFilter create(
      FilterType filterType, long expectedCapacity, double falsePositiveProbability) {

    if (expectedCapacity <= 0) {
      throw new IllegalArgumentException("Expected capacity must be positive");
    }

    FilterType actualType = filterType;

    // 自动选择类型
    if (filterType == FilterType.AUTO) {
      actualType = selectOptimalFilterType(expectedCapacity);
      logger.info("Auto-selected filter type: {} for capacity: {}", actualType, expectedCapacity);
    }

    // 创建过滤器
    switch (actualType) {
      case HASHSET:
        return new HashSetUniquenessFilter(expectedCapacity);

      case BLOOM_FILTER:
        return new BloomFilterUniquenessFilter(expectedCapacity, falsePositiveProbability);

      default:
        throw new IllegalArgumentException("Unsupported filter type: " + actualType);
    }
  }

  /**
   * 根据数据量选择最优的过滤器类型。
   *
   * @param expectedCapacity 预期容量
   * @return 过滤器类型
   */
  private static FilterType selectOptimalFilterType(long expectedCapacity) {
    if (expectedCapacity < SMALL_DATA_THRESHOLD) {
      // 小数据量：使用HashSet（准确性优先）
      return FilterType.HASHSET;
    } else {
      // 大数据量：使用BloomFilter（内存优先）
      return FilterType.BLOOM_FILTER;
    }
  }

  /**
   * 根据数据量推荐误判率。
   *
   * @param expectedCapacity 预期容量
   * @return 推荐的误判率
   */
  public static double recommendFalsePositiveProbability(long expectedCapacity) {
    if (expectedCapacity < SMALL_DATA_THRESHOLD) {
      return DEFAULT_FPP; // 1%
    } else if (expectedCapacity < MEDIUM_DATA_THRESHOLD) {
      return DEFAULT_FPP; // 1%
    } else {
      return LOW_FPP; // 0.1% - 大数据量使用更低的误判率
    }
  }

  /**
   * 估算过滤器的内存占用。
   *
   * @param filterType 过滤器类型
   * @param expectedCapacity 预期容量
   * @param falsePositiveProbability 误判率
   * @return 内存占用（字节）
   */
  public static long estimateMemoryUsage(
      FilterType filterType, long expectedCapacity, double falsePositiveProbability) {

    switch (filterType) {
      case HASHSET:
        // HashSet: 约92字节/元素（假设平均字符串长度10）
        return expectedCapacity * 92;

      case BLOOM_FILTER:
        // BloomFilter: m = -n * ln(p) / (ln(2)^2)
        double m =
            -expectedCapacity * Math.log(falsePositiveProbability) / Math.pow(Math.log(2), 2);
        return (long) (m / 8);

      default:
        return 0;
    }
  }

  /**
   * 打印过滤器选择建议。
   *
   * @param expectedCapacity 预期容量
   */
  public static void printRecommendation(long expectedCapacity) {
    FilterType recommended = selectOptimalFilterType(expectedCapacity);
    double fpp = recommendFalsePositiveProbability(expectedCapacity);

    long hashSetMemory = estimateMemoryUsage(FilterType.HASHSET, expectedCapacity, 0.0);
    long bloomFilterMemory = estimateMemoryUsage(FilterType.BLOOM_FILTER, expectedCapacity, fpp);

    System.out.println("=== Uniqueness Filter Recommendation ===");
    System.out.println("Expected Capacity: " + expectedCapacity);
    System.out.println("Recommended Type: " + recommended);
    System.out.println();
    System.out.println("Memory Comparison:");
    System.out.printf("  HashSet:     %dMB (100%% accurate)%n", hashSetMemory / 1024 / 1024);
    System.out.printf(
        "  BloomFilter: %dMB (%.2f%% FPP)%n", bloomFilterMemory / 1024 / 1024, fpp * 100);
    System.out.printf(
        "  Memory Saved: %dMB (%.1f%%)%n",
        (hashSetMemory - bloomFilterMemory) / 1024 / 1024,
        (1.0 - (double) bloomFilterMemory / hashSetMemory) * 100);
  }
}
