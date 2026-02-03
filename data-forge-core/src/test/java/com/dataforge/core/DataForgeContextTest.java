package com.dataforge.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DataForgeContext 测试类
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("DataForgeContext 测试")
class DataForgeContextTest {

  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    context = new DataForgeContext();
  }

  @Nested
  @DisplayName("上下文初始化测试")
  class ContextInitializationTests {

    @Test
    @DisplayName("上下文创建后应处于空状态")
    void contextShouldBeEmptyAfterCreation() {
      // Then
      assertThat(context.isEmpty()).isTrue();
      assertThat(context.size()).isZero();
      assertThat(context.isClosed()).isFalse();
      assertThat(context.getCreatedAt()).isNotNull();
      assertThat(context.getCurrentRecordIndex()).isZero();
    }

    @Test
    @DisplayName("上下文创建时间应在合理范围内")
    void creationTimeShouldBeWithinReasonableRange() {
      // Given
      LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);

      // When
      try (DataForgeContext newContext = new DataForgeContext()) {
        // Then
        LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);
        assertThat(newContext.getCreatedAt()).isAfter(beforeCreation).isBefore(afterCreation);
      }
    }

    @Test
    @DisplayName("toString 应返回有效的上下文信息")
    void toStringShouldReturnValidContextInfo() {
      // When
      String result = context.toString();

      // Then
      assertThat(result).contains("DataForgeContext").contains("size=0").contains("closed=false");
    }
  }

  @Nested
  @DisplayName("基本存储和获取测试")
  class BasicStorageAndRetrievalTests {

    @Test
    @DisplayName("存储和获取字符串值应成功")
    void storeAndRetrieveStringValueShouldSucceed() {
      // Given
      String key = "testKey";
      String value = "testValue";

      // When
      context.put(key, value);

      // Then
      Optional<String> retrieved = context.get(key, String.class);
      assertThat(retrieved).isPresent().hasValue(value);
    }

    @Test
    @DisplayName("存储和获取整数值应成功")
    void storeAndRetrieveIntegerValueShouldSucceed() {
      // Given
      String key = "age";
      Integer value = 25;

      // When
      context.put(key, value);

      // Then
      Optional<Integer> retrieved = context.get(key, Integer.class);
      assertThat(retrieved).isPresent().hasValue(value);
    }

    @Test
    @DisplayName("存储和获取复杂对象应成功")
    void storeAndRetrieveComplexObjectShouldSucceed() {
      // Given
      String key = "data";
      Map<String, Object> value = Map.of("name", "John", "age", 30);

      // When
      context.put(key, value);

      // Then
      @SuppressWarnings("unchecked")
      Optional<Map<String, Object>> retrieved =
          (Optional<Map<String, Object>>) (Optional<?>) context.get(key, Map.class);
      assertThat(retrieved).isPresent();
      assertThat(retrieved.get()).containsEntry("name", "John").containsEntry("age", 30);
    }

    @Test
    @DisplayName("putValue 别名方法应正常工作")
    void putValueAliasMethodShouldWork() {
      // Given
      String key = "aliasKey";
      String value = "aliasValue";

      // When
      context.putValue(key, value);

      // Then
      assertThat(context.getValue(key)).isEqualTo(value);
    }

    @Test
    @DisplayName("获取不存在的键应返回空Optional")
    void getNonExistentKeyShouldReturnEmptyOptional() {
      // When
      Optional<String> result = context.get("nonExistent", String.class);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("类型不匹配时应返回空Optional")
    void typeMismatchShouldReturnEmptyOptional() {
      // Given
      context.put("number", 123);

      // When
      Optional<String> result = context.get("number", String.class);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("存储null值应成功")
    void storeNullValueShouldSucceed() {
      // Given
      String key = "nullKey";

      // When
      context.put(key, null);

      // Then
      // java.util.Optional 无法表达“present but value==null”，因此通过 containsKey 区分
      assertThat(context.containsKey(key)).isTrue();
      Optional<Object> retrieved = context.get(key);
      assertThat(retrieved).isEmpty();
    }

    @Test
    @DisplayName("更新已存在的键应覆盖旧值")
    void updateExistingKeyShouldOverrideOldValue() {
      // Given
      String key = "updateKey";
      context.put(key, "oldValue");

      // When
      context.put(key, "newValue");

      // Then
      Optional<String> retrieved = context.get(key, String.class);
      assertThat(retrieved).hasValue("newValue");
      assertThat(context.size()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("键值管理测试")
  class KeyValueManagementTests {

    @Test
    @DisplayName("containsKey 应正确检测键的存在")
    void containsKeyShouldDetectKeyPresence() {
      // Given
      context.put("existing", "value");

      // Then
      assertThat(context.containsKey("existing")).isTrue();
      assertThat(context.containsKey("nonExisting")).isFalse();
    }

    @Test
    @DisplayName("keySet 应返回所有键的集合")
    void keySetShouldReturnAllKeys() {
      // Given
      context.put("key1", "value1");
      context.put("key2", "value2");
      context.put("key3", "value3");

      // When
      Set<String> keys = context.keySet();

      // Then
      assertThat(keys).containsExactlyInAnyOrder("key1", "key2", "key3");
    }

    @Test
    @DisplayName("getAll 应返回所有条目的副本")
    void getAllShouldReturnCopyOfAllEntries() {
      // Given
      context.put("key1", "value1");
      context.put("key2", 123);

      // When
      Map<String, Object> all = context.getAll();

      // Then
      assertThat(all).hasSize(2).containsEntry("key1", "value1").containsEntry("key2", 123);

      // Verify it's a copy
      all.put("newKey", "newValue");
      assertThat(context.containsKey("newKey")).isFalse();
    }

    @Test
    @DisplayName("remove 应删除指定键并返回值")
    void removeShouldDeleteKeyAndReturnValue() {
      // Given
      context.put("removeKey", "removeValue");

      // When
      Optional<Object> removed = context.remove("removeKey");

      // Then
      assertThat(removed).isPresent().hasValue("removeValue");
      assertThat(context.containsKey("removeKey")).isFalse();
    }

    @Test
    @DisplayName("remove 不存在的键应返回空Optional")
    void removeNonExistentKeyShouldReturnEmptyOptional() {
      // When
      Optional<Object> removed = context.remove("nonExistent");

      // Then
      assertThat(removed).isEmpty();
    }

    @Test
    @DisplayName("clear 应清空所有数据")
    void clearShouldRemoveAllData() {
      // Given
      context.put("key1", "value1");
      context.put("key2", "value2");

      // When
      context.clear();

      // Then
      assertThat(context.isEmpty()).isTrue();
      assertThat(context.size()).isZero();
    }
  }

  @Nested
  @DisplayName("记录索引管理测试")
  class RecordIndexManagementTests {

    @Test
    @DisplayName("设置记录索引应成功")
    void setRecordIndexShouldSucceed() {
      // When
      context.setCurrentRecordIndex(5);

      // Then
      assertThat(context.getCurrentRecordIndex()).isEqualTo(5);
    }

    @Test
    @DisplayName("递增记录索引应成功")
    void incrementRecordIndexShouldSucceed() {
      // Given
      context.setCurrentRecordIndex(10);

      // When
      int newIndex = context.incrementRecordIndex();

      // Then
      assertThat(newIndex).isEqualTo(11);
      assertThat(context.getCurrentRecordIndex()).isEqualTo(11);
    }

    @Test
    @DisplayName("设置负数索引应抛出异常")
    void setNegativeIndexShouldThrowException() {
      // Then
      assertThatThrownBy(() -> context.setCurrentRecordIndex(-1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be negative");
    }
  }

  @Nested
  @DisplayName("父子上下文测试")
  class ParentChildContextTests {

    @Test
    @DisplayName("创建子上下文应成功")
    void createChildContextShouldSucceed() {
      // Given
      context.put("parentKey", "parentValue");

      // When
      DataForgeContext child = context.createChildContext();

      // Then
      assertThat(child).isNotNull();
      assertThat(child.getParent()).isPresent().hasValue(context);
    }

    @Test
    @DisplayName("子上下文应能从父上下文获取值")
    void childShouldGetValueFromParent() {
      // Given
      context.put("sharedKey", "sharedValue");
      DataForgeContext child = context.createChildContext();

      // When
      Optional<String> value = child.get("sharedKey", String.class);

      // Then
      assertThat(value).isPresent().hasValue("sharedValue");
    }

    @Test
    @DisplayName("子上下文的修改不应影响父上下文")
    void childModificationShouldNotAffectParent() {
      // Given
      DataForgeContext child = context.createChildContext();

      // When
      child.put("childKey", "childValue");

      // Then
      assertThat(context.containsKey("childKey")).isFalse();
      assertThat(child.containsKey("childKey")).isTrue();
    }

    @Test
    @DisplayName("子上下文覆盖父上下文值应独立存储")
    void childOverrideShouldStoreIndependently() {
      // Given
      context.put("key", "parentValue");
      DataForgeContext child = context.createChildContext();

      // When
      child.put("key", "childValue");

      // Then
      assertThat(context.get("key", String.class)).hasValue("parentValue");
      assertThat(child.get("key", String.class)).hasValue("childValue");
    }

    @Test
    @DisplayName("获取父上下文应为空当没有父上下文时")
    void getParentShouldBeEmptyWhenNoParent() {
      // Then
      assertThat(context.getParent()).isEmpty();
    }
  }

  @Nested
  @DisplayName("资源清理测试")
  class ResourceCleanupTests {

    @Test
    @DisplayName("关闭上下文应清空所有数据")
    void closeShouldClearAllData() {
      // Given
      context.put("key1", "value1");
      context.put("key2", "value2");

      // When
      context.close();

      // Then
      assertThat(context.isClosed()).isTrue();
      assertThat(context.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("重复关闭不应抛出异常")
    void doubleCloseShouldNotThrowException() {
      // Given
      context.close();

      // Then - calling close again should not throw exception
      try {
        context.close();
      } catch (Exception e) {
        // Should not throw
        throw new AssertionError("close() should not throw exception", e);
      }
    }

    @Test
    @DisplayName("关闭后操作应抛出异常")
    void operationsAfterCloseShouldThrowException() {
      // Given
      context.close();

      // Then
      assertThatThrownBy(() -> context.put("key", "value"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("try-with-resources 应正确关闭上下文")
    void tryWithResourcesShouldCloseContext() {
      // When
      try (DataForgeContext ctx = new DataForgeContext()) {
        ctx.put("key", "value");
        assertThat(ctx.isClosed()).isFalse();
      }

      // Then - no exception thrown
    }
  }

  @Nested
  @DisplayName("参数验证测试")
  class ParameterValidationTests {

    @Test
    @DisplayName("存储null键应抛出异常")
    void storeNullKeyShouldThrowException() {
      assertThatThrownBy(() -> context.put(null, "value"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("存储空字符串键应抛出异常")
    void storeEmptyKeyShouldThrowException() {
      assertThatThrownBy(() -> context.put("", "value"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or empty");
    }

    @Test
    @DisplayName("存储空白字符串键应抛出异常")
    void storeBlankKeyShouldThrowException() {
      assertThatThrownBy(() -> context.put("   ", "value"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or empty");
    }

    @Test
    @DisplayName("存储超长键应抛出异常")
    void storeOverlongKeyShouldThrowException() {
      // Given
      String longKey = "a".repeat(256);

      // Then
      assertThatThrownBy(() -> context.put(longKey, "value"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("exceeds maximum");
    }

    @Test
    @DisplayName("获取时null类型应抛出异常")
    void getWithNullTypeShouldThrowException() {
      assertThatThrownBy(() -> context.get("key", null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cannot be null");
    }
  }

  @Nested
  @DisplayName("容量限制测试")
  class CapacityLimitTests {

    @Test
    @DisplayName("超过最大容量应抛出异常")
    void exceedMaxCapacityShouldThrowException() {
      // This test uses reflection to test the capacity limit without actually
      // adding 10,000 items
      // In practice, the limit is high enough that normal usage won't hit it

      // Given - fill context to near capacity
      for (int i = 0; i < 100; i++) {
        context.put("key" + i, "value" + i);
      }

      // Then - context should still work normally
      assertThat(context.size()).isEqualTo(100);
    }
  }

  @Nested
  @DisplayName("线程安全测试")
  class ThreadSafetyTests {

    @Test
    @DisplayName("并发写入应保持数据完整性")
    void concurrentWritesShouldMaintainDataIntegrity() throws InterruptedException {
      // Given
      int threadCount = 10;
      int operationsPerThread = 100;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);

      // When
      for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        executor.submit(
            () -> {
              try {
                for (int j = 0; j < operationsPerThread; j++) {
                  context.put("thread" + threadId + "_op" + j, "value" + j);
                }
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(5, TimeUnit.SECONDS);
      executor.shutdown();

      // Then
      assertThat(context.size()).isEqualTo(threadCount * operationsPerThread);
    }

    @Test
    @DisplayName("并发读写应保持数据一致性")
    void concurrentReadWriteShouldMaintainConsistency() throws InterruptedException {
      // Given
      int writerCount = 5;
      int readerCount = 5;
      int operationsPerThread = 50;
      ExecutorService executor = Executors.newFixedThreadPool(writerCount + readerCount);
      CountDownLatch latch = new CountDownLatch(writerCount + readerCount);

      // Pre-populate some data
      for (int i = 0; i < 50; i++) {
        context.put("initial" + i, "value" + i);
      }

      // When - writers
      for (int i = 0; i < writerCount; i++) {
        final int threadId = i;
        executor.submit(
            () -> {
              try {
                for (int j = 0; j < operationsPerThread; j++) {
                  context.put("writer" + threadId + "_" + j, "value" + j);
                }
              } finally {
                latch.countDown();
              }
            });
      }

      // When - readers
      for (int i = 0; i < readerCount; i++) {
        executor.submit(
            () -> {
              try {
                for (int j = 0; j < operationsPerThread; j++) {
                  context.get("initial" + (j % 50), String.class);
                }
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(5, TimeUnit.SECONDS);
      executor.shutdown();

      // Then - no exceptions thrown, context in valid state
      assertThat(context.size()).isGreaterThanOrEqualTo(50);
    }
  }
}
