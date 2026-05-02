package com.dataforge.api.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SimpleDataForgeContext 测试")
class SimpleDataForgeContextTest {

  private SimpleDataForgeContext context;

  @BeforeEach
  void setUp() {
    context = new SimpleDataForgeContext();
  }

  @Nested
  @DisplayName("基本功能测试")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("应创建上下文")
    void shouldCreateContext() {
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("应存储和获取值")
    void shouldStoreAndRetrieveValue() {
      context.put("key", "value");

      Optional<String> result = context.getString("key");

      assertThat(result).isPresent();
      assertThat(result.get()).isEqualTo("value");
    }

    @Test
    @DisplayName("应检查键是否存在")
    void shouldCheckKeyExists() {
      context.put("key", "value");

      assertThat(context.containsKey("key")).isTrue();
      assertThat(context.containsKey("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("应获取所有键")
    void shouldGetAllKeys() {
      context.put("key1", "value1");
      context.put("key2", "value2");
      context.put("key3", "value3");

      Set<String> keys = context.keySet();

      assertThat(keys).hasSize(3);
      assertThat(keys).containsExactlyInAnyOrder("key1", "key2", "key3");
    }

    @Test
    @DisplayName("应获取所有条目")
    void shouldGetAllEntries() {
      context.put("key1", "value1");
      context.put("key2", 123);

      Map<String, Object> entries = context.getAll();

      assertThat(entries).hasSize(2);
      assertThat(entries).containsEntry("key1", "value1");
      assertThat(entries).containsEntry("key2", 123);
    }

    @Test
    @DisplayName("应清空上下文")
    void shouldClearContext() {
      context.put("key1", "value1");
      context.put("key2", "value2");

      context.clear();

      assertThat(context.keySet()).isEmpty();
      assertThat(context.getAll()).isEmpty();
    }

    @Test
    @DisplayName("应移除键")
    void shouldRemoveKey() {
      context.put("key", "value");

      Optional<Object> removed = context.remove("key");

      assertThat(removed).isPresent();
      assertThat(removed.get()).isEqualTo("value");
      assertThat(context.containsKey("key")).isFalse();
    }
  }

  @Nested
  @DisplayName("类型安全测试")
  class TypeSafetyTests {

    @Test
    @DisplayName("应获取字符串值")
    void shouldGetStringValue() {
      context.put("name", "John");

      Optional<String> name = context.get("name", String.class);

      assertThat(name).isPresent();
      assertThat(name.get()).isEqualTo("John");
    }

    @Test
    @DisplayName("应获取整数值")
    void shouldGetIntegerValue() {
      context.put("age", 30);

      Optional<Integer> age = context.get("age", Integer.class);

      assertThat(age).isPresent();
      assertThat(age.get()).isEqualTo(30);
    }

    @Test
    @DisplayName("应获取布尔值")
    void shouldGetBooleanValue() {
      context.put("active", true);

      Optional<Boolean> active = context.get("active", Boolean.class);

      assertThat(active).isPresent();
      assertThat(active.get()).isTrue();
    }

    @Test
    @DisplayName("应获取长整型值")
    void shouldGetLongValue() {
      context.put("timestamp", 1234567890L);

      Optional<Long> timestamp = context.get("timestamp", Long.class);

      assertThat(timestamp).isPresent();
      assertThat(timestamp.get()).isEqualTo(1234567890L);
    }

    @Test
    @DisplayName("应获取双精度值")
    void shouldGetDoubleValue() {
      context.put("price", 19.99);

      Optional<Double> price = context.get("price", Double.class);

      assertThat(price).isPresent();
      assertThat(price.get()).isEqualTo(19.99);
    }

    @Test
    @DisplayName("应返回空Optional当类型不匹配")
    void shouldReturnEmptyOptionalWhenTypeMismatch() {
      context.put("value", "string_value");

      Optional<Integer> intValue = context.get("value", Integer.class);

      assertThat(intValue).isEmpty();
    }

    @Test
    @DisplayName("应返回空Optional当键不存在")
    void shouldReturnEmptyOptionalWhenKeyNotExists() {
      Optional<String> value = context.get("nonexistent", String.class);

      assertThat(value).isEmpty();
    }
  }

  @Nested
  @DisplayName("null值处理测试")
  class NullValueHandlingTests {

    @Test
    @DisplayName("应存储null值")
    void shouldStoreNullValue() {
      context.put("key", null);

      Optional<String> result = context.getString("key");

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("应检查null值键存在")
    void shouldCheckNullValueKeyExists() {
      context.put("key", null);

      assertThat(context.containsKey("key")).isTrue();
    }

    @Test
    @DisplayName("应移除null值")
    void shouldRemoveNullValue() {
      context.put("key", null);

      Optional<Object> removed = context.remove("key");

      assertThat(removed).isEmpty();
      assertThat(context.containsKey("key")).isFalse();
    }

    @Test
    @DisplayName("getAll应返回null值")
    void shouldGetAllReturnNullValue() {
      context.put("key1", "value1");
      context.put("key2", null);

      Map<String, Object> entries = context.getAll();

      assertThat(entries).hasSize(2);
      assertThat(entries).containsKey("key1");
      assertThat(entries.get("key1")).isEqualTo("value1");
      assertThat(entries).containsKey("key2");
      assertThat(entries.get("key2")).isNull();
    }
  }

  @Nested
  @DisplayName("子上下文测试")
  class ChildContextTests {

    @Test
    @DisplayName("应创建子上下文")
    void shouldCreateChildContext() {
      DataForgeContext child = context.createChildContext();

      assertThat(child).isNotNull();
      assertThat(child.getParent()).isPresent();
      assertThat(child.getParent().get()).isSameAs(context);
    }

    @Test
    @DisplayName("子上下文应继承父上下文值")
    void shouldInheritParentValues() {
      context.put("parentKey", "parentValue");
      DataForgeContext child = context.createChildContext();

      Optional<String> value = child.getString("parentKey");

      assertThat(value).isPresent();
      assertThat(value.get()).isEqualTo("parentValue");
    }

    @Test
    @DisplayName("子上下文应覆盖父上下文值")
    void shouldOverrideParentValue() {
      context.put("key", "parentValue");
      DataForgeContext child = context.createChildContext();
      child.put("key", "childValue");

      Optional<String> childValue = child.getString("key");
      Optional<String> parentValue = context.getString("key");

      assertThat(childValue).isPresent();
      assertThat(childValue.get()).isEqualTo("childValue");
      assertThat(parentValue).isPresent();
      assertThat(parentValue.get()).isEqualTo("parentValue");
    }

    @Test
    @DisplayName("子上下文应独立于父上下文")
    void shouldBeIndependentFromParent() {
      DataForgeContext child = context.createChildContext();
      child.put("childKey", "childValue");

      assertThat(context.containsKey("childKey")).isFalse();
      assertThat(child.containsKey("childKey")).isTrue();
    }

    @Test
    @DisplayName("应支持多级子上下文")
    void shouldSupportMultipleChildLevels() {
      DataForgeContext child1 = context.createChildContext();
      DataForgeContext child2 = child1.createChildContext();
      DataForgeContext child3 = child2.createChildContext();

      context.put("root", "rootValue");
      child1.put("level1", "value1");
      child2.put("level2", "value2");
      child3.put("level3", "value3");

      assertThat(child3.getString("root")).isPresent();
      assertThat(child3.getString("root").get()).isEqualTo("rootValue");
      assertThat(child3.getString("level1")).isPresent();
      assertThat(child3.getString("level1").get()).isEqualTo("value1");
      assertThat(child3.getString("level2")).isPresent();
      assertThat(child3.getString("level2").get()).isEqualTo("value2");
      assertThat(child3.getString("level3")).isPresent();
      assertThat(child3.getString("level3").get()).isEqualTo("value3");
    }

    @Test
    @DisplayName("子上下文keySet应仅包含自有键")
    void shouldOnlyContainOwnKeysInKeySet() {
      context.put("parentKey", "parentValue");
      DataForgeContext child = context.createChildContext();
      child.put("childKey", "childValue");

      Set<String> childKeys = child.keySet();

      assertThat(childKeys).hasSize(1);
      assertThat(childKeys).containsExactly("childKey");
    }

    @Test
    @DisplayName("子上下文getAll应仅包含自有条目")
    void shouldOnlyContainOwnEntriesInGetAll() {
      context.put("parentKey", "parentValue");
      DataForgeContext child = context.createChildContext();
      child.put("childKey", "childValue");

      Map<String, Object> childEntries = child.getAll();

      assertThat(childEntries).hasSize(1);
      assertThat(childEntries).containsEntry("childKey", "childValue");
    }

    @Test
    @DisplayName("子上下文clear应仅清除自有值")
    void shouldOnlyClearOwnValues() {
      context.put("parentKey", "parentValue");
      DataForgeContext child = context.createChildContext();
      child.put("childKey", "childValue");

      child.clear();

      assertThat(context.containsKey("parentKey")).isTrue();
      assertThat(child.containsKey("childKey")).isFalse();
      assertThat(child.getString("parentKey")).isPresent();
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {

    @Test
    @DisplayName("应处理空键")
    void shouldHandleEmptyKey() {
      context.put("", "value");

      Optional<String> result = context.getString("");

      assertThat(result).isPresent();
      assertThat(result.get()).isEqualTo("value");
    }

    @Test
    @DisplayName("应处理特殊字符键")
    void shouldHandleSpecialCharacterKeys() {
      context.put("key-with-dash", "value1");
      context.put("key_with_underscore", "value2");
      context.put("key.with.dot", "value3");

      assertThat(context.getString("key-with-dash")).isPresent();
      assertThat(context.getString("key_with_underscore")).isPresent();
      assertThat(context.getString("key.with.dot")).isPresent();
    }

    @Test
    @DisplayName("应处理null键")
    void shouldHandleNullKey() {
      assertThatThrownBy(() -> context.put(null, "value"))
          .isInstanceOf(NullPointerException.class);

      assertThatThrownBy(() -> context.getString(null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("应处理大量键值对")
    void shouldHandleLargeNumberOfEntries() {
      for (int i = 0; i < 1000; i++) {
        context.put("key" + i, "value" + i);
      }

      assertThat(context.keySet()).hasSize(1000);
      assertThat(context.getString("key500")).isPresent();
      assertThat(context.getString("key500").get()).isEqualTo("value500");
    }

    @Test
    @DisplayName("应处理重复键")
    void shouldHandleDuplicateKeys() {
      context.put("key", "value1");
      context.put("key", "value2");

      Optional<String> result = context.getString("key");

      assertThat(result).isPresent();
      assertThat(result.get()).isEqualTo("value2");
    }

    @Test
    @DisplayName("应处理复杂对象值")
    void shouldHandleComplexObjectValues() {
      Map<String, Object> nested = Map.of("nestedKey", "nestedValue");
      context.put("complex", nested);

      @SuppressWarnings("unchecked")
      Map<String, Object> result = context.get("complex", Map.class).orElse(null);

      assertThat(result).isNotNull();
      assertThat(result).containsEntry("nestedKey", "nestedValue");
    }
  }

  @Nested
  @DisplayName("不可变性测试")
  class ImmutabilityTests {

    @Test
    @DisplayName("keySet应返回不可变集合")
    void shouldReturnImmutableSetFromKeySet() {
      context.put("key", "value");
      Set<String> keys = context.keySet();

      assertThatThrownBy(() -> keys.add("newKey"))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("getAll应返回不可变映射")
    void shouldReturnImmutableMapFromGetAll() {
      context.put("key", "value");
      Map<String, Object> entries = context.getAll();

      assertThatThrownBy(() -> entries.put("newKey", "newValue"))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Nested
  @DisplayName("父上下文测试")
  class ParentContextTests {

    @Test
    @DisplayName("根上下文应返回空父上下文")
    void shouldReturnEmptyParentForRootContext() {
      Optional<DataForgeContext> parent = context.getParent();

      assertThat(parent).isEmpty();
    }

    @Test
    @DisplayName("子上下文应返回父上下文")
    void shouldReturnParentForChildContext() {
      DataForgeContext child = context.createChildContext();

      Optional<DataForgeContext> parent = child.getParent();

      assertThat(parent).isPresent();
      assertThat(parent.get()).isSameAs(context);
    }
  }
}
