package com.dataforge.core.uniqueness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UniquenessFilterFactory 测试")
class UniquenessFilterFactoryTest {

  @Nested
  @DisplayName("自动选择类型测试")
  class AutoSelectionTests {

    @Test
    @DisplayName("小数据量应选择HashSet")
    void shouldSelectHashSetForSmallData() {
      UniquenessFilter filter = UniquenessFilterFactory.create(100_000);

      assertThat(filter).isNotNull();
      assertThat(filter.getFilterType()).isEqualTo("HashSet");
      assertThat(filter.expectedCapacity()).isEqualTo(100_000);
    }

    @Test
    @DisplayName("中等数据量应选择BloomFilter")
    void shouldSelectBloomFilterForMediumData() {
      UniquenessFilter filter = UniquenessFilterFactory.create(5_000_000);

      assertThat(filter).isNotNull();
      assertThat(filter.getFilterType()).isEqualTo("BloomFilter");
      assertThat(filter.expectedCapacity()).isEqualTo(5_000_000);
    }

    @Test
    @DisplayName("大数据量应选择BloomFilter")
    void shouldSelectBloomFilterForLargeData() {
      UniquenessFilter filter = UniquenessFilterFactory.create(50_000_000);

      assertThat(filter).isNotNull();
      assertThat(filter.getFilterType()).isEqualTo("BloomFilter");
      assertThat(filter.expectedCapacity()).isEqualTo(50_000_000);
    }

    @Test
    @DisplayName("边界值100万应选择BloomFilter")
    void shouldSelectBloomFilterForBoundaryValue() {
      UniquenessFilter filter = UniquenessFilterFactory.create(1_000_000);

      assertThat(filter).isNotNull();
      assertThat(filter.getFilterType()).isEqualTo("BloomFilter");
    }

    @Test
    @DisplayName("边界值99万应选择HashSet")
    void shouldSelectHashSetForJustBelowBoundary() {
      UniquenessFilter filter = UniquenessFilterFactory.create(999_999);

      assertThat(filter).isNotNull();
      assertThat(filter.getFilterType()).isEqualTo("HashSet");
    }
  }

  @Nested
  @DisplayName("指定类型创建测试")
  class SpecifiedTypeCreationTests {

    @Test
    @DisplayName("应创建HashSet类型过滤器")
    void shouldCreateHashSetFilter() {
      UniquenessFilter filter =
          UniquenessFilterFactory.create(UniquenessFilterFactory.FilterType.HASHSET, 100_000);

      assertThat(filter).isNotNull();
      assertThat(filter.getFilterType()).isEqualTo("HashSet");
      assertThat(filter.expectedCapacity()).isEqualTo(100_000);
    }

    @Test
    @DisplayName("应创建BloomFilter类型过滤器")
    void shouldCreateBloomFilter() {
      UniquenessFilter filter =
          UniquenessFilterFactory.create(
              UniquenessFilterFactory.FilterType.BLOOM_FILTER, 10_000_000);

      assertThat(filter).isNotNull();
      assertThat(filter.getFilterType()).isEqualTo("BloomFilter");
      assertThat(filter.expectedCapacity()).isEqualTo(10_000_000);
    }

    @Test
    @DisplayName("应创建AUTO类型过滤器")
    void shouldCreateAutoFilter() {
      UniquenessFilter filter =
          UniquenessFilterFactory.create(UniquenessFilterFactory.FilterType.AUTO, 100_000);

      assertThat(filter).isNotNull();
      assertThat(filter.getFilterType()).isEqualTo("HashSet");
    }

    @Test
    @DisplayName("无效类型应抛出异常")
    void shouldThrowExceptionForInvalidType() {
      assertThatThrownBy(
              () -> UniquenessFilterFactory.create(UniquenessFilterFactory.FilterType.AUTO, 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Expected capacity must be positive");
    }
  }

  @Nested
  @DisplayName("完整配置创建测试")
  class FullConfigurationCreationTests {

    @Test
    @DisplayName("应创建带自定义误判率的BloomFilter")
    void shouldCreateBloomFilterWithCustomFpp() {
      UniquenessFilter filter =
          UniquenessFilterFactory.create(
              UniquenessFilterFactory.FilterType.BLOOM_FILTER, 10_000_000, 0.001);

      assertThat(filter).isNotNull();
      assertThat(filter.getFilterType()).isEqualTo("BloomFilter");
      assertThat(filter.expectedCapacity()).isEqualTo(10_000_000);
      assertThat(filter.falsePositiveProbability()).isEqualTo(0.001);
    }

    @Test
    @DisplayName("应创建带自定义误判率的HashSet")
    void shouldCreateHashSetWithCustomFpp() {
      UniquenessFilter filter =
          UniquenessFilterFactory.create(
              UniquenessFilterFactory.FilterType.HASHSET, 100_000, 0.001);

      assertThat(filter).isNotNull();
      assertThat(filter.getFilterType()).isEqualTo("HashSet");
      assertThat(filter.expectedCapacity()).isEqualTo(100_000);
      assertThat(filter.falsePositiveProbability()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("应创建带自定义误判率的AUTO过滤器")
    void shouldCreateAutoFilterWithCustomFpp() {
      UniquenessFilter filter =
          UniquenessFilterFactory.create(
              UniquenessFilterFactory.FilterType.AUTO, 10_000_000, 0.001);

      assertThat(filter).isNotNull();
      assertThat(filter.getFilterType()).isEqualTo("BloomFilter");
      assertThat(filter.expectedCapacity()).isEqualTo(10_000_000);
      assertThat(filter.falsePositiveProbability()).isEqualTo(0.001);
    }
  }

  @Nested
  @DisplayName("参数验证测试")
  class ParameterValidationTests {

    @Test
    @DisplayName("容量为0应抛出异常")
    void shouldThrowExceptionForZeroCapacity() {
      assertThatThrownBy(() -> UniquenessFilterFactory.create(0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Expected capacity must be positive");
    }

    @Test
    @DisplayName("负容量应抛出异常")
    void shouldThrowExceptionForNegativeCapacity() {
      assertThatThrownBy(() -> UniquenessFilterFactory.create(-100))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Expected capacity must be positive");
    }

    @Test
    @DisplayName("无效误判率应被忽略（HashSet）")
    void shouldIgnoreInvalidFppForHashSet() {
      UniquenessFilter filter =
          UniquenessFilterFactory.create(UniquenessFilterFactory.FilterType.HASHSET, 100_000, 0.5);

      assertThat(filter).isNotNull();
      assertThat(filter.falsePositiveProbability()).isEqualTo(0.0);
    }
  }

  @Nested
  @DisplayName("误判率推荐测试")
  class FppRecommendationTests {

    @Test
    @DisplayName("小数据量应推荐1%误判率")
    void shouldRecommendDefaultFppForSmallData() {
      double fpp = UniquenessFilterFactory.recommendFalsePositiveProbability(100_000);

      assertThat(fpp).isEqualTo(0.01);
    }

    @Test
    @DisplayName("中等数据量应推荐1%误判率")
    void shouldRecommendDefaultFppForMediumData() {
      double fpp = UniquenessFilterFactory.recommendFalsePositiveProbability(5_000_000);

      assertThat(fpp).isEqualTo(0.01);
    }

    @Test
    @DisplayName("大数据量应推荐0.1%误判率")
    void shouldRecommendLowFppForLargeData() {
      double fpp = UniquenessFilterFactory.recommendFalsePositiveProbability(50_000_000);

      assertThat(fpp).isEqualTo(0.001);
    }

    @Test
    @DisplayName("边界值1000万应推荐0.1%误判率")
    void shouldRecommendLowFppForBoundaryValue() {
      double fpp = UniquenessFilterFactory.recommendFalsePositiveProbability(10_000_000);

      assertThat(fpp).isEqualTo(0.001);
    }

    @Test
    @DisplayName("边界值999万应推荐1%误判率")
    void shouldRecommendDefaultFppForJustBelowBoundary() {
      double fpp = UniquenessFilterFactory.recommendFalsePositiveProbability(9_999_999);

      assertThat(fpp).isEqualTo(0.01);
    }
  }

  @Nested
  @DisplayName("内存估算测试")
  class MemoryEstimationTests {

    @Test
    @DisplayName("应正确估算HashSet内存使用")
    void shouldEstimateHashSetMemoryUsage() {
      long memory =
          UniquenessFilterFactory.estimateMemoryUsage(
              UniquenessFilterFactory.FilterType.HASHSET, 100_000, 0.0);

      assertThat(memory).isEqualTo(100_000 * 92);
    }

    @Test
    @DisplayName("应正确估算BloomFilter内存使用")
    void shouldEstimateBloomFilterMemoryUsage() {
      long memory =
          UniquenessFilterFactory.estimateMemoryUsage(
              UniquenessFilterFactory.FilterType.BLOOM_FILTER, 10_000_000, 0.01);

      assertThat(memory).isGreaterThan(0);
      assertThat(memory).isLessThan(20 * 1024 * 1024);
    }

    @Test
    @DisplayName("低误判率BloomFilter应使用更多内存")
    void lowFppShouldUseMoreMemory() {
      long highFppMemory =
          UniquenessFilterFactory.estimateMemoryUsage(
              UniquenessFilterFactory.FilterType.BLOOM_FILTER, 10_000_000, 0.01);
      long lowFppMemory =
          UniquenessFilterFactory.estimateMemoryUsage(
              UniquenessFilterFactory.FilterType.BLOOM_FILTER, 10_000_000, 0.001);

      assertThat(lowFppMemory).isGreaterThan(highFppMemory);
    }

    @Test
    @DisplayName("大容量应使用更多内存")
    void largeCapacityShouldUseMoreMemory() {
      long smallMemory =
          UniquenessFilterFactory.estimateMemoryUsage(
              UniquenessFilterFactory.FilterType.BLOOM_FILTER, 1_000_000, 0.01);
      long largeMemory =
          UniquenessFilterFactory.estimateMemoryUsage(
              UniquenessFilterFactory.FilterType.BLOOM_FILTER, 10_000_000, 0.01);

      assertThat(largeMemory).isGreaterThan(smallMemory);
    }

    @Test
    @DisplayName("BloomFilter应比HashSet节省内存")
    void bloomFilterShouldSaveMemoryComparedToHashSet() {
      long hashSetMemory =
          UniquenessFilterFactory.estimateMemoryUsage(
              UniquenessFilterFactory.FilterType.HASHSET, 10_000_000, 0.0);
      long bloomFilterMemory =
          UniquenessFilterFactory.estimateMemoryUsage(
              UniquenessFilterFactory.FilterType.BLOOM_FILTER, 10_000_000, 0.01);

      assertThat(bloomFilterMemory).isLessThan(hashSetMemory);
      double savingsPercentage = (1.0 - (double) bloomFilterMemory / hashSetMemory) * 100;
      assertThat(savingsPercentage).isGreaterThan(80.0);
    }
  }

  @Nested
  @DisplayName("功能验证测试")
  class FunctionalityVerificationTests {

    @Test
    @DisplayName("创建的HashSet应正常工作")
    void createdHashSetShouldWork() {
      UniquenessFilter filter =
          UniquenessFilterFactory.create(UniquenessFilterFactory.FilterType.HASHSET, 1000);

      filter.put("value1");
      filter.put("value2");

      assertThat(filter.mightContain("value1")).isTrue();
      assertThat(filter.mightContain("value2")).isTrue();
      assertThat(filter.mightContain("value3")).isFalse();
      assertThat(filter.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("创建的BloomFilter应正常工作")
    void createdBloomFilterShouldWork() {
      UniquenessFilter filter =
          UniquenessFilterFactory.create(
              UniquenessFilterFactory.FilterType.BLOOM_FILTER, 10000, 0.01);

      filter.put("value1");
      filter.put("value2");

      assertThat(filter.mightContain("value1")).isTrue();
      assertThat(filter.mightContain("value2")).isTrue();
      assertThat(filter.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("创建的AUTO过滤器应正常工作")
    void createdAutoFilterShouldWork() {
      UniquenessFilter filter = UniquenessFilterFactory.create(1000);

      filter.put("value1");
      filter.put("value2");

      assertThat(filter.mightContain("value1")).isTrue();
      assertThat(filter.mightContain("value2")).isTrue();
      assertThat(filter.mightContain("value3")).isFalse();
      assertThat(filter.size()).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("统计信息测试")
  class StatisticsTests {

    @Test
    @DisplayName("HashSet应返回正确统计信息")
    void hashSetShouldReturnCorrectStatistics() {
      UniquenessFilter filter =
          UniquenessFilterFactory.create(UniquenessFilterFactory.FilterType.HASHSET, 1000);

      filter.put("value1");
      filter.put("value2");

      String stats = filter.getStatistics();

      assertThat(stats)
          .contains("HashSet")
          .contains("size=2")
          .contains("capacity=1000")
          .contains("fpp=0.00");
    }

    @Test
    @DisplayName("BloomFilter应返回正确统计信息")
    void bloomFilterShouldReturnCorrectStatistics() {
      UniquenessFilter filter =
          UniquenessFilterFactory.create(
              UniquenessFilterFactory.FilterType.BLOOM_FILTER, 10000, 0.01);

      filter.put("value1");
      filter.put("value2");

      String stats = filter.getStatistics();

      assertThat(stats)
          .contains("BloomFilter")
          .contains("size=2")
          .contains("capacity=10000")
          .contains("fpp=0.0100");
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {

    @Test
    @DisplayName("最小容量1应创建成功")
    void shouldCreateWithMinimumCapacity() {
      UniquenessFilter filter = UniquenessFilterFactory.create(1);

      assertThat(filter).isNotNull();
      assertThat(filter.expectedCapacity()).isEqualTo(1);
    }

    @Test
    @DisplayName("极大容量应创建成功")
    void shouldCreateWithVeryLargeCapacity() {
      UniquenessFilter filter = UniquenessFilterFactory.create(1_000_000_000L);

      assertThat(filter).isNotNull();
      assertThat(filter.expectedCapacity()).isEqualTo(1_000_000_000L);
    }

    @Test
    @DisplayName("最小误判率应创建成功")
    void shouldCreateWithMinimumFpp() {
      UniquenessFilter filter =
          UniquenessFilterFactory.create(
              UniquenessFilterFactory.FilterType.BLOOM_FILTER, 10000, 0.0001);

      assertThat(filter).isNotNull();
      assertThat(filter.falsePositiveProbability()).isEqualTo(0.0001);
    }

    @Test
    @DisplayName("接近1的误判率应创建成功")
    void shouldCreateWithNearMaximumFpp() {
      UniquenessFilter filter =
          UniquenessFilterFactory.create(
              UniquenessFilterFactory.FilterType.BLOOM_FILTER, 10000, 0.99);

      assertThat(filter).isNotNull();
      assertThat(filter.falsePositiveProbability()).isEqualTo(0.99);
    }
  }
}
