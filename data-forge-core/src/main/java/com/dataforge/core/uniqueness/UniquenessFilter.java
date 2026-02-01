package com.dataforge.core.uniqueness;

/**
 * 唯一性过滤器接口。
 *
 * <p>用于在数据生成过程中检查和保证数据的唯一性。 支持多种实现策略，包括基于内存的HashSet和基于概率的布隆过滤器。
 *
 * <p><strong>实现策略对比：</strong>
 *
 * <ul>
 *   <li><strong>HashSet</strong>: 100%准确，但内存占用大（1000万条约200MB）
 *   <li><strong>BloomFilter</strong>: 概率性准确（可配置误判率），内存占用小（1000万条约12MB）
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public interface UniquenessFilter {

  /**
   * 检查值是否可能已存在。
   *
   * <p>对于HashSet实现，返回确定性结果。 对于BloomFilter实现，可能存在误判（false positive），即返回true但实际不存在。
   *
   * @param value 要检查的值
   * @return 如果值可能已存在返回true，否则返回false
   */
  boolean mightContain(String value);

  /**
   * 添加值到过滤器。
   *
   * @param value 要添加的值
   * @return 如果值成功添加返回true，如果值已存在返回false
   */
  boolean put(String value);

  /** 清空过滤器中的所有数据。 */
  void clear();

  /**
   * 获取过滤器中已添加的元素数量（估计值）。
   *
   * <p>对于HashSet实现，返回精确值。 对于BloomFilter实现，返回估计值。
   *
   * @return 元素数量
   */
  long size();

  /**
   * 获取过滤器的预期容量。
   *
   * @return 预期容量
   */
  long expectedCapacity();

  /**
   * 获取过滤器的误判率（仅适用于概率性过滤器）。
   *
   * <p>对于HashSet实现，返回0.0（无误判）。 对于BloomFilter实现，返回配置的误判率。
   *
   * @return 误判率（0.0-1.0）
   */
  double falsePositiveProbability();

  /**
   * 获取过滤器的内存占用估计（字节）。
   *
   * @return 内存占用（字节）
   */
  long estimatedMemoryUsage();

  /**
   * 获取过滤器类型名称。
   *
   * @return 过滤器类型（如 "HashSet", "BloomFilter"）
   */
  String getFilterType();

  /**
   * 获取过滤器的统计信息。
   *
   * @return 统计信息字符串
   */
  default String getStatistics() {
    return String.format(
        "Filter{type=%s, size=%d, capacity=%d, fpp=%.4f, memory=%dMB}",
        getFilterType(),
        size(),
        expectedCapacity(),
        falsePositiveProbability(),
        estimatedMemoryUsage() / 1024 / 1024);
  }

  /**
   * 检查过滤器是否已满。
   *
   * @return 如果过滤器已满返回true
   */
  default boolean isFull() {
    return size() >= expectedCapacity();
  }

  /**
   * 获取过滤器的填充率。
   *
   * @return 填充率（0.0-1.0）
   */
  default double fillRate() {
    long capacity = expectedCapacity();
    if (capacity == 0) {
      return 0.0;
    }
    return (double) size() / capacity;
  }
}
