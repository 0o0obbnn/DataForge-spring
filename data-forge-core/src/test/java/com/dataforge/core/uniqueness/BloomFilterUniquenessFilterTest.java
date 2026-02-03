package com.dataforge.core.uniqueness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BloomFilterUniquenessFilter 测试")
class BloomFilterUniquenessFilterTest {

  private BloomFilterUniquenessFilter filter;

  @BeforeEach
  void setUp() {
    filter = new BloomFilterUniquenessFilter(10000, 0.01);
  }

  @Nested
  @DisplayName("构造函数测试")
  class ConstructorTests {

    @Test
    @DisplayName("应创建默认配置的布隆过滤器")
    void shouldCreateDefaultFilter() {
      BloomFilterUniquenessFilter defaultFilter = BloomFilterUniquenessFilter.createDefault();

      assertThat(defaultFilter).isNotNull();
      assertThat(defaultFilter.expectedCapacity()).isEqualTo(10_000_000);
      assertThat(defaultFilter.falsePositiveProbability()).isEqualTo(0.01);
    }

    @Test
    @DisplayName("应创建大容量布隆过滤器")
    void shouldCreateLargeFilter() {
      BloomFilterUniquenessFilter largeFilter = BloomFilterUniquenessFilter.createLarge();

      assertThat(largeFilter).isNotNull();
      assertThat(largeFilter.expectedCapacity()).isEqualTo(100_000_000);
      assertThat(largeFilter.falsePositiveProbability()).isEqualTo(0.001);
    }

    @Test
    @DisplayName("无效容量应抛出异常")
    void shouldThrowExceptionForInvalidCapacity() {
      assertThatThrownBy(() -> new BloomFilterUniquenessFilter(0, 0.01))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Expected capacity must be positive");

      assertThatThrownBy(() -> new BloomFilterUniquenessFilter(-100, 0.01))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Expected capacity must be positive");
    }

    @Test
    @DisplayName("无效误判率应抛出异常")
    void shouldThrowExceptionForInvalidFpp() {
      assertThatThrownBy(() -> new BloomFilterUniquenessFilter(1000, 0.0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("False positive probability must be between 0.0 and 1.0");

      assertThatThrownBy(() -> new BloomFilterUniquenessFilter(1000, 1.0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("False positive probability must be between 0.0 and 1.0");

      assertThatThrownBy(() -> new BloomFilterUniquenessFilter(1000, -0.1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("False positive probability must be between 0.0 and 1.0");

      assertThatThrownBy(() -> new BloomFilterUniquenessFilter(1000, 1.5))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("False positive probability must be between 0.0 and 1.0");
    }

    @Test
    @DisplayName("应正确估算内存使用")
    void shouldEstimateMemoryUsage() {
      long memoryUsage = filter.estimatedMemoryUsage();

      assertThat(memoryUsage).isGreaterThan(0);
      assertThat(memoryUsage).isLessThan(10 * 1024 * 1024);
    }
  }

  @Nested
  @DisplayName("基本功能测试")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("新值应被接受")
    void shouldAcceptNewValue() {
      boolean result = filter.put("value1");

      assertThat(result).isTrue();
      assertThat(filter.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("重复值应被拒绝")
    void shouldRejectDuplicateValue() {
      filter.put("value1");
      boolean result = filter.put("value1");

      assertThat(result).isFalse();
      assertThat(filter.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("null值应被忽略")
    void shouldIgnoreNullValue() {
      boolean result = filter.put(null);

      assertThat(result).isFalse();
      assertThat(filter.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("mightContain应正确检查值")
    void shouldCheckValueWithMightContain() {
      filter.put("value1");

      assertThat(filter.mightContain("value1")).isTrue();
      assertThat(filter.mightContain("value2")).isFalse();
    }

    @Test
    @DisplayName("null值mightContain应返回false")
    void shouldReturnFalseForNullMightContain() {
      boolean result = filter.mightContain(null);

      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("布隆过滤器特性测试")
  class BloomFilterCharacteristicsTests {

    @Test
    @DisplayName("布隆过滤器可能有误判但无漏判")
    void shouldHaveNoFalseNegatives() {
      Set<String> added = new HashSet<>();
      int count = 1000;

      for (int i = 0; i < count; i++) {
        String value = "value" + i;
        filter.put(value);
        added.add(value);
      }

      for (String value : added) {
        assertThat(filter.mightContain(value))
            .as("Value %s should be detected as present", value)
            .isTrue();
      }
    }

    @Test
    @DisplayName("应支持大规模数据")
    void shouldSupportLargeScaleData() {
      int count = 5000;
      int testCount = 1000;
      int falsePositives = 0;

      for (int i = 0; i < count; i++) {
        filter.put("value" + i);
      }

      for (int i = count; i < count + testCount; i++) {
        if (filter.mightContain("value" + i)) {
          falsePositives++;
        }
      }

      double actualFpp = (double) falsePositives / testCount;
      assertThat(actualFpp).isLessThan(0.02);
    }

    @Test
    @DisplayName("误判率应在合理范围内")
    void shouldHaveAcceptableFalsePositiveRate() {
      int count = 10000;
      int testCount = 1000;

      for (int i = 0; i < count; i++) {
        filter.put("added" + i);
      }

      int falsePositives = 0;
      for (int i = 0; i < testCount; i++) {
        if (filter.mightContain("notadded" + i)) {
          falsePositives++;
        }
      }

      double actualFpp = (double) falsePositives / testCount;
      assertThat(actualFpp).isLessThan(0.02);
    }
  }

  @Nested
  @DisplayName("统计信息测试")
  class StatisticsTests {

    @Test
    @DisplayName("应正确报告大小")
    void shouldReportCorrectSize() {
      filter.put("value1");
      filter.put("value2");
      filter.put("value3");

      assertThat(filter.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("应正确报告预期容量")
    void shouldReportCorrectExpectedCapacity() {
      assertThat(filter.expectedCapacity()).isEqualTo(10000);
    }

    @Test
    @DisplayName("应正确报告误判率")
    void shouldReportCorrectFalsePositiveProbability() {
      assertThat(filter.falsePositiveProbability()).isEqualTo(0.01);
    }

    @Test
    @DisplayName("应正确报告过滤器类型")
    void shouldReportCorrectFilterType() {
      assertThat(filter.getFilterType()).isEqualTo("BloomFilter");
    }

    @Test
    @DisplayName("应正确报告统计信息")
    void shouldReportCorrectStatistics() {
      for (int i = 0; i < 100; i++) {
        filter.put("value" + i);
      }

      String stats = filter.getStatistics();

      assertThat(stats)
          .contains("BloomFilter")
          .contains("size=100")
          .contains("capacity=10000")
          .contains("fpp=0.0100");
    }

    @Test
    @DisplayName("应正确计算填充率")
    void shouldCalculateCorrectFillRate() {
      for (int i = 0; i < 5000; i++) {
        filter.put("value" + i);
      }

      assertThat(filter.fillRate()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("空过滤器填充率应为0")
    void emptyFilterShouldHaveZeroFillRate() {
      assertThat(filter.fillRate()).isEqualTo(0.0);
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {

    @Test
    @DisplayName("应处理空字符串")
    void shouldHandleEmptyString() {
      boolean result = filter.put("");

      assertThat(result).isTrue();
      assertThat(filter.mightContain("")).isTrue();
    }

    @Test
    @DisplayName("应处理长字符串")
    void shouldHandleLongString() {
      String longString = "a".repeat(10000);
      boolean result = filter.put(longString);

      assertThat(result).isTrue();
      assertThat(filter.mightContain(longString)).isTrue();
    }

    @Test
    @DisplayName("应处理特殊字符")
    void shouldHandleSpecialCharacters() {
      String specialString = "特殊字符!@#$%^&*()_+-=[]{}|;':\",./<>?";
      boolean result = filter.put(specialString);

      assertThat(result).isTrue();
      assertThat(filter.mightContain(specialString)).isTrue();
    }

    @Test
    @DisplayName("应处理Unicode字符")
    void shouldHandleUnicodeCharacters() {
      String unicodeString = "Hello世界🌍";
      boolean result = filter.put(unicodeString);

      assertThat(result).isTrue();
      assertThat(filter.mightContain(unicodeString)).isTrue();
    }

    @Test
    @DisplayName("应处理大量重复值")
    void shouldHandleManyDuplicateValues() {
      for (int i = 0; i < 1000; i++) {
        filter.put("duplicate");
      }

      assertThat(filter.size()).isEqualTo(1000);
      assertThat(filter.mightContain("duplicate")).isTrue();
    }
  }

  @Nested
  @DisplayName("性能测试")
  class PerformanceTests {

    @Test
    @DisplayName("批量添加应高效")
    void shouldAddBatchEfficiently() {
      int count = 10000;
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < count; i++) {
        filter.put("value" + i);
      }

      long duration = System.currentTimeMillis() - startTime;
      assertThat(duration).isLessThan(1000);
    }

    @Test
    @DisplayName("批量查询应高效")
    void shouldQueryBatchEfficiently() {
      int count = 10000;
      for (int i = 0; i < count; i++) {
        filter.put("value" + i);
      }

      long startTime = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
        filter.mightContain("value" + i);
      }
      long duration = System.currentTimeMillis() - startTime;

      assertThat(duration).isLessThan(1000);
    }
  }

  @Nested
  @DisplayName("不支持的操作测试")
  class UnsupportedOperationsTests {

    @Test
    @DisplayName("clear操作应抛出异常")
    void clearShouldThrowException() {
      assertThatThrownBy(() -> filter.clear())
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("does not support clear operation");
    }
  }

  @Nested
  @DisplayName("不同配置测试")
  class DifferentConfigurationTests {

    @Test
    @DisplayName("低误判率配置应使用更多内存")
    void lowFppShouldUseMoreMemory() {
      BloomFilterUniquenessFilter lowFppFilter = new BloomFilterUniquenessFilter(10000, 0.001);
      BloomFilterUniquenessFilter highFppFilter = new BloomFilterUniquenessFilter(10000, 0.1);

      assertThat(lowFppFilter.estimatedMemoryUsage())
          .isGreaterThan(highFppFilter.estimatedMemoryUsage());
    }

    @Test
    @DisplayName("大容量配置应使用更多内存")
    void largeCapacityShouldUseMoreMemory() {
      BloomFilterUniquenessFilter smallFilter = new BloomFilterUniquenessFilter(1000, 0.01);
      BloomFilterUniquenessFilter largeFilter = new BloomFilterUniquenessFilter(10000, 0.01);

      assertThat(largeFilter.estimatedMemoryUsage())
          .isGreaterThan(smallFilter.estimatedMemoryUsage());
    }
  }

  @Nested
  @DisplayName("并发安全测试")
  class ConcurrencyTests {

    @Test
    @DisplayName("应支持并发添加")
    void shouldSupportConcurrentAdds() throws InterruptedException {
      int threadCount = 10;
      int operationsPerThread = 1000;
      Thread[] threads = new Thread[threadCount];

      for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        threads[i] =
            new Thread(
                () -> {
                  for (int j = 0; j < operationsPerThread; j++) {
                    filter.put("thread" + threadId + "_value" + j);
                  }
                });
        threads[i].start();
      }

      for (Thread thread : threads) {
        thread.join();
      }

      assertThat(filter.size()).isEqualTo(threadCount * operationsPerThread);
    }
  }
}
