package com.dataforge.core.uniqueness;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于布隆过滤器的唯一性过滤器实现。
 *
 * <p>使用Google Guava的BloomFilter实现，提供高效的内存使用和快速的查询性能。
 *
 * <p><strong>性能特点：</strong>
 *
 * <ul>
 *   <li>内存占用：1000万条约12MB（相比HashSet的200MB，节省94%）
 *   <li>查询时间：O(k)，k为哈希函数数量，通常为常数
 *   <li>误判率：可配置，默认1%
 *   <li>无假阴性：如果返回false，则值一定不存在
 * </ul>
 *
 * <p><strong>适用场景：</strong>
 *
 * <ul>
 *   <li>大数据量生成（百万级以上）
 *   <li>可接受小概率误判
 *   <li>内存受限环境
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class BloomFilterUniquenessFilter implements UniquenessFilter {

  private static final Logger logger = LoggerFactory.getLogger(BloomFilterUniquenessFilter.class);

  private final BloomFilter<String> bloomFilter;
  private final long expectedCapacity;
  private final double falsePositiveProbability;
  private final AtomicLong elementCount;

  /**
   * 创建布隆过滤器唯一性过滤器。
   *
   * @param expectedCapacity 预期容量
   * @param falsePositiveProbability 误判率（0.0-1.0）
   */
  @SuppressWarnings("null")
  public BloomFilterUniquenessFilter(long expectedCapacity, double falsePositiveProbability) {
    if (expectedCapacity <= 0) {
      throw new IllegalArgumentException("Expected capacity must be positive");
    }
    if (falsePositiveProbability <= 0.0 || falsePositiveProbability >= 1.0) {
      throw new IllegalArgumentException("False positive probability must be between 0.0 and 1.0");
    }

    this.expectedCapacity = expectedCapacity;
    this.falsePositiveProbability = falsePositiveProbability;
    this.elementCount = new AtomicLong(0);

    // 创建布隆过滤器
    // Charsets.UTF_8 和 Funnels.stringFunnel 都是非 null 的常量/方法
    com.google.common.hash.Funnel<? super String> funnel = Funnels.stringFunnel(Charsets.UTF_8);
    this.bloomFilter = BloomFilter.create(funnel, expectedCapacity, falsePositiveProbability);

    logger.info(
        "Created BloomFilter with capacity={}, fpp={}, estimated memory={}MB",
        expectedCapacity,
        falsePositiveProbability,
        estimatedMemoryUsage() / 1024 / 1024);
  }

  /**
   * 创建默认配置的布隆过滤器（容量1000万，误判率1%）。
   *
   * @return 布隆过滤器实例
   */
  public static BloomFilterUniquenessFilter createDefault() {
    return new BloomFilterUniquenessFilter(10_000_000, 0.01);
  }

  /**
   * 创建大容量布隆过滤器（容量1亿，误判率0.1%）。
   *
   * @return 布隆过滤器实例
   */
  public static BloomFilterUniquenessFilter createLarge() {
    return new BloomFilterUniquenessFilter(100_000_000, 0.001);
  }

  @Override
  public boolean mightContain(String value) {
    if (value == null) {
      return false;
    }
    return bloomFilter.mightContain(value);
  }

  @Override
  public boolean put(String value) {
    if (value == null) {
      return false;
    }

    // 检查是否已存在（注意：布隆过滤器可能误判）
    boolean existed = bloomFilter.mightContain(value);

    // 添加到过滤器
    bloomFilter.put(value);

    // 增加计数（即使可能已存在，我们也增加计数以保持一致性）
    // 这是因为布隆过滤器无法准确判断元素是否真的存在
    elementCount.incrementAndGet();

    return !existed;
  }

  @Override
  public void clear() {
    // 布隆过滤器不支持清空，需要重新创建
    logger.warn(
        "BloomFilter does not support clear operation. "
            + "Consider creating a new instance instead.");
    throw new UnsupportedOperationException(
        "BloomFilter does not support clear operation. Create a new instance instead.");
  }

  @Override
  public long size() {
    return elementCount.get();
  }

  @Override
  public long expectedCapacity() {
    return expectedCapacity;
  }

  @Override
  public double falsePositiveProbability() {
    return falsePositiveProbability;
  }

  @Override
  public long estimatedMemoryUsage() {
    // 布隆过滤器内存占用估算公式：
    // m = -n * ln(p) / (ln(2)^2)
    // 其中 n 是预期元素数量，p 是误判率，m 是位数组大小（bits）
    double m = -expectedCapacity * Math.log(falsePositiveProbability) / Math.pow(Math.log(2), 2);
    return (long) (m / 8); // 转换为字节
  }

  @Override
  public String getFilterType() {
    return "BloomFilter";
  }

  @Override
  public String getStatistics() {
    return String.format(
        "BloomFilter{size=%d, capacity=%d, fillRate=%.2f%%, fpp=%.4f, "
            + "actualFpp=%.4f, memory=%dMB}",
        size(),
        expectedCapacity,
        fillRate() * 100,
        falsePositiveProbability,
        calculateActualFpp(),
        estimatedMemoryUsage() / 1024 / 1024);
  }

  /**
   * 计算当前实际的误判率。
   *
   * <p>随着元素的增加，实际误判率会逐渐接近配置的误判率。
   *
   * @return 实际误判率
   */
  private double calculateActualFpp() {
    if (size() == 0) {
      return 0.0;
    }
    // 实际误判率公式：(1 - e^(-kn/m))^k
    // 简化计算：使用配置的误判率乘以填充率
    return falsePositiveProbability * Math.min(1.0, fillRate() * 1.5);
  }
}
