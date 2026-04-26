package com.dataforge.core.uniqueness;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于HashSet的唯一性过滤器实现。
 *
 * <p>使用Java标准库的HashSet实现，提供100%准确的唯一性检查。
 *
 * <p><strong>性能特点：</strong>
 *
 * <ul>
 *   <li>内存占用：1000万条约200MB
 *   <li>查询时间：O(1)平均时间复杂度
 *   <li>误判率：0%（完全准确）
 *   <li>支持清空操作
 * </ul>
 *
 * <p><strong>适用场景：</strong>
 *
 * <ul>
 *   <li>小到中等数据量生成（百万级以下）
 *   <li>需要100%准确性
 *   <li>内存充足环境
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class HashSetUniquenessFilter implements UniquenessFilter {

  private static final Logger logger = LoggerFactory.getLogger(HashSetUniquenessFilter.class);

  private final Set<String> uniqueValues;
  private final long expectedCapacity;

  /**
   * 创建HashSet唯一性过滤器。
   *
   * @param expectedCapacity 预期容量
   */
  public HashSetUniquenessFilter(long expectedCapacity) {
    if (expectedCapacity <= 0) {
      throw new IllegalArgumentException("Expected capacity must be positive");
    }

    this.expectedCapacity = expectedCapacity;

    // 创建线程安全的HashSet，初始容量设置为预期容量的1.5倍以减少rehash
    int initialCapacity = (int) Math.min(expectedCapacity * 3L / 2L, Integer.MAX_VALUE);
    this.uniqueValues = Collections.synchronizedSet(new HashSet<>(initialCapacity));

    logger.info(
        "Created HashSet with capacity={}, estimated memory={}MB",
        expectedCapacity,
        estimatedMemoryUsage() / 1024 / 1024);
  }

  /**
   * 创建默认配置的HashSet过滤器（容量100万）。
   *
   * @return HashSet过滤器实例
   */
  public static HashSetUniquenessFilter createDefault() {
    return new HashSetUniquenessFilter(1_000_000);
  }

  /**
   * 创建小容量HashSet过滤器（容量10万）。
   *
   * @return HashSet过滤器实例
   */
  public static HashSetUniquenessFilter createSmall() {
    return new HashSetUniquenessFilter(100_000);
  }

  @Override
  public boolean mightContain(String value) {
    if (value == null) {
      return false;
    }
    return uniqueValues.contains(value);
  }

  @Override
  public boolean put(String value) {
    if (value == null) {
      return false;
    }

    // HashSet.add() 返回true表示元素不存在并成功添加
    boolean added = uniqueValues.add(value);

    // 检查是否接近容量上限
    if (added && size() % 100000 == 0) {
      logger.debug(
          "HashSet size: {}, fill rate: {:.2f}%, memory: {}MB",
          size(), fillRate() * 100, estimatedMemoryUsage() / 1024 / 1024);
    }

    return added;
  }

  @Override
  public void clear() {
    uniqueValues.clear();
    logger.info("HashSet cleared");
  }

  @Override
  public long size() {
    return uniqueValues.size();
  }

  @Override
  public long expectedCapacity() {
    return expectedCapacity;
  }

  @Override
  public double falsePositiveProbability() {
    return 0.0; // HashSet无误判
  }

  @Override
  public long estimatedMemoryUsage() {
    // HashSet内存占用估算：
    // - 每个String对象：约40字节（对象头12字节 + char数组引用4字节 + 其他字段）
    // - 每个String的char数组：约2 * 平均长度字节
    // - HashSet的Entry对象：约32字节
    // - 总计：约72字节 + 2 * 平均长度
    // 假设平均字符串长度为10，则每个元素约92字节
    long avgStringLength = 10;
    long bytesPerElement = 72 + 2 * avgStringLength;
    return size() * bytesPerElement;
  }

  @Override
  public String getFilterType() {
    return "HashSet";
  }

  @Override
  public String getStatistics() {
    return String.format(
        "HashSet{size=%d, capacity=%d, fillRate=%.2f%%, fpp=0.00, memory=%dMB}",
        size(), expectedCapacity, fillRate() * 100, estimatedMemoryUsage() / 1024 / 1024);
  }

  @Override
  public boolean isFull() {
    // HashSet可以动态扩容，但超过预期容量时性能会下降
    if (size() >= expectedCapacity) {
      logger.warn(
          "HashSet has reached expected capacity: {}/{}. "
              + "Consider using a larger capacity or BloomFilter for better performance.",
          size(),
          expectedCapacity);
      return true;
    }
    return false;
  }
}
