package com.dataforge.core.uniqueness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("HashSetUniquenessFilter 测试")
class HashSetUniquenessFilterTest {

  private HashSetUniquenessFilter filter;

  @BeforeEach
  void setUp() {
    filter = new HashSetUniquenessFilter(10000);
  }

  @Nested
  @DisplayName("构造函数测试")
  class ConstructorTests {

    @Test
    @DisplayName("应创建默认配置的HashSet过滤器")
    void shouldCreateDefaultFilter() {
      HashSetUniquenessFilter defaultFilter = HashSetUniquenessFilter.createDefault();

      assertThat(defaultFilter).isNotNull();
      assertThat(defaultFilter.expectedCapacity()).isEqualTo(1_000_000);
    }

    @Test
    @DisplayName("应创建小容量HashSet过滤器")
    void shouldCreateSmallFilter() {
      HashSetUniquenessFilter smallFilter = HashSetUniquenessFilter.createSmall();

      assertThat(smallFilter).isNotNull();
      assertThat(smallFilter.expectedCapacity()).isEqualTo(100_000);
    }

    @Test
    @DisplayName("无效容量应抛出异常")
    void shouldThrowExceptionForInvalidCapacity() {
      assertThatThrownBy(() -> new HashSetUniquenessFilter(0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Expected capacity must be positive");

      assertThatThrownBy(() -> new HashSetUniquenessFilter(-100))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Expected capacity must be positive");
    }

    @Test
    @DisplayName("应正确估算内存使用")
    void shouldEstimateMemoryUsage() {
      long memoryUsage = filter.estimatedMemoryUsage();

      assertThat(memoryUsage).isEqualTo(0);

      filter.put("value1");
      memoryUsage = filter.estimatedMemoryUsage();

      assertThat(memoryUsage).isGreaterThan(0);
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
      assertThat(filter.size()).isEqualTo(1);
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
  @DisplayName("HashSet特性测试")
  class HashSetCharacteristicsTests {

    @Test
    @DisplayName("应100%准确无误判")
    void shouldHaveNoFalsePositives() {
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

      for (int i = count; i < count + 1000; i++) {
        assertThat(filter.mightContain("value" + i))
            .as("Value %s should not be detected as present", "value" + i)
            .isFalse();
      }
    }

    @Test
    @DisplayName("应支持中等规模数据")
    void shouldSupportMediumScaleData() {
      int count = 100000;

      for (int i = 0; i < count; i++) {
        filter.put("value" + i);
      }

      assertThat(filter.size()).isEqualTo(count);
    }

    @Test
    @DisplayName("误判率应为0")
    void shouldHaveZeroFalsePositiveRate() {
      int count = 10000;

      for (int i = 0; i < count; i++) {
        filter.put("added" + i);
      }

      int falsePositives = 0;
      for (int i = 0; i < count; i++) {
        if (filter.mightContain("notadded" + i)) {
          falsePositives++;
        }
      }

      assertThat(falsePositives).isEqualTo(0);
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
      assertThat(filter.falsePositiveProbability()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("应正确报告过滤器类型")
    void shouldReportCorrectFilterType() {
      assertThat(filter.getFilterType()).isEqualTo("HashSet");
    }

    @Test
    @DisplayName("应正确报告统计信息")
    void shouldReportCorrectStatistics() {
      for (int i = 0; i < 100; i++) {
        filter.put("value" + i);
      }

      String stats = filter.getStatistics();

      assertThat(stats)
          .contains("HashSet")
          .contains("size=100")
          .contains("capacity=10000")
          .contains("fpp=0.00");
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

      assertThat(filter.size()).isEqualTo(1);
      assertThat(filter.mightContain("duplicate")).isTrue();
    }
  }

  @Nested
  @DisplayName("清空操作测试")
  class ClearOperationTests {

    @Test
    @DisplayName("清空应移除所有值")
    void clearShouldRemoveAllValues() {
      filter.put("value1");
      filter.put("value2");
      filter.put("value3");

      assertThat(filter.size()).isEqualTo(3);

      filter.clear();

      assertThat(filter.size()).isEqualTo(0);
      assertThat(filter.mightContain("value1")).isFalse();
      assertThat(filter.mightContain("value2")).isFalse();
      assertThat(filter.mightContain("value3")).isFalse();
    }

    @Test
    @DisplayName("清空后应能重新添加值")
    void shouldAllowReAddingAfterClear() {
      filter.put("value1");
      filter.clear();

      boolean result = filter.put("value1");

      assertThat(result).isTrue();
      assertThat(filter.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("清空空过滤器应无异常")
    void clearingEmptyFilterShouldNotThrowException() {
      assertThatCode(() -> filter.clear()).doesNotThrowAnyException();
      assertThat(filter.size()).isEqualTo(0);
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

    @Test
    @DisplayName("批量删除应高效")
    void shouldClearEfficiently() {
      int count = 10000;
      for (int i = 0; i < count; i++) {
        filter.put("value" + i);
      }

      long startTime = System.currentTimeMillis();
      filter.clear();
      long duration = System.currentTimeMillis() - startTime;

      assertThat(duration).isLessThan(100);
      assertThat(filter.size()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("不同配置测试")
  class DifferentConfigurationTests {

    @Test
    @DisplayName("大容量配置应使用更多内存")
    void largeCapacityShouldUseMoreMemory() {
      HashSetUniquenessFilter smallFilter = new HashSetUniquenessFilter(1000);
      HashSetUniquenessFilter largeFilter = new HashSetUniquenessFilter(10000);

      for (int i = 0; i < 500; i++) {
        smallFilter.put("value" + i);
        largeFilter.put("value" + i);
      }

      assertThat(largeFilter.estimatedMemoryUsage())
          .isGreaterThanOrEqualTo(smallFilter.estimatedMemoryUsage());
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
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);

      for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        executor.execute(
            () -> {
              try {
                for (int j = 0; j < operationsPerThread; j++) {
                  filter.put("thread" + threadId + "_value" + j);
                }
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(10, TimeUnit.SECONDS);
      executor.shutdown();

      assertThat(filter.size()).isEqualTo(threadCount * operationsPerThread);
    }

    @Test
    @DisplayName("应支持并发查询")
    void shouldSupportConcurrentQueries() throws InterruptedException {
      int threadCount = 10;
      int operationsPerThread = 1000;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);

      for (int i = 0; i < operationsPerThread; i++) {
        filter.put("value" + i);
      }

      for (int i = 0; i < threadCount; i++) {
        executor.execute(
            () -> {
              try {
                for (int j = 0; j < operationsPerThread; j++) {
                  filter.mightContain("value" + j);
                }
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(10, TimeUnit.SECONDS);
      executor.shutdown();

      assertThat(filter.size()).isEqualTo(operationsPerThread);
    }

    @Test
    @DisplayName("应支持并发清空")
    void shouldSupportConcurrentClears() throws InterruptedException {
      int threadCount = 5;
      int operationsPerThread = 1000;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);

      for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        executor.execute(
            () -> {
              try {
                for (int j = 0; j < operationsPerThread; j++) {
                  filter.put("thread" + threadId + "_value" + j);
                }
                filter.clear();
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(10, TimeUnit.SECONDS);
      executor.shutdown();

      assertThat(filter.size()).isLessThan(threadCount * operationsPerThread);
    }
  }

  @Nested
  @DisplayName("内存使用测试")
  class MemoryUsageTests {

    @Test
    @DisplayName("内存使用应随元素数量线性增长")
    void memoryUsageShouldGrowLinearly() {
      long initialMemory = filter.estimatedMemoryUsage();

      for (int i = 0; i < 1000; i++) {
        filter.put("value" + i);
      }

      long memoryAfter1000 = filter.estimatedMemoryUsage();

      for (int i = 1000; i < 2000; i++) {
        filter.put("value" + i);
      }

      long memoryAfter2000 = filter.estimatedMemoryUsage();

      assertThat(memoryAfter1000).isGreaterThan(initialMemory);
      assertThat(memoryAfter2000).isGreaterThan(memoryAfter1000);
      long expectedIncrease = (memoryAfter1000 - initialMemory);
      assertThat(memoryAfter2000 - memoryAfter1000)
          .isGreaterThanOrEqualTo((long) (expectedIncrease * 0.8));
    }
  }
}
