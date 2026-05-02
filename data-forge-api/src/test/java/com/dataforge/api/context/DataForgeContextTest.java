package com.dataforge.api.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * DataForgeContext 接口完整测试类
 * 
 * <p>测试上下文接口的所有功能，包括类型安全获取、父子上下文、并发安全等</p>
 * 
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("DataForgeContext 接口测试")
class DataForgeContextTest {

    private DataForgeContext context;

    @BeforeEach
    void setUp() {
        context = new SimpleDataForgeContext();
    }

    @Nested
    @DisplayName("基础操作测试")
    class BasicOperationTests {

        @Test
        @DisplayName("应能存储和获取字符串值")
        void shouldStoreAndRetrieveStringValue() {
            context.put("test_key", "test_value");

            Optional<String> result = context.getString("test_key");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("test_value");
        }

        @Test
        @DisplayName("应能存储和获取整数值")
        void shouldStoreAndRetrieveIntegerValue() {
            context.put("int_key", 42);

            Optional<Integer> result = context.get("int_key", Integer.class);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(42);
        }

        @Test
        @DisplayName("应能存储和获取自定义对象")
        void shouldStoreAndRetrieveCustomObject() {
            TestObject testObject = new TestObject("test", 123);
            context.put("object_key", testObject);

            Optional<TestObject> result = context.get("object_key", TestObject.class);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(testObject);
        }

        @Test
        @DisplayName("应正确处理null值存储")
        void shouldHandleNullValueStorage() {
            context.put("null_key", null);

            Optional<String> result = context.getString("null_key");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("应返回空Optional当键不存在时")
        void shouldReturnEmptyOptionalWhenKeyNotExists() {
            Optional<String> result = context.getString("non_existent_key");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("应返回空Optional当类型不匹配时")
        void shouldReturnEmptyOptionalWhenTypeMismatch() {
            context.put("string_key", "test_value");

            Optional<Integer> result = context.get("string_key", Integer.class);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("存在性检查测试")
    class ExistenceCheckTests {

        @Test
        @DisplayName("应正确检查键是否存在")
        void shouldCorrectlyCheckKeyExistence() {
            context.put("existing_key", "value");

            boolean exists = context.containsKey("existing_key");
            boolean notExists = context.containsKey("non_existing_key");

            assertThat(exists).isTrue();
            assertThat(notExists).isFalse();
        }

        @Test
        @DisplayName("应正确处理null键检查")
        void shouldHandleNullKeyCheck() {
            assertThatNoException()
                .isThrownBy(() -> context.containsKey(null));
        }

        @Test
        @DisplayName("应返回正确的键集合")
        void shouldReturnCorrectKeySet() {
            context.put("key1", "value1");
            context.put("key2", "value2");
            context.put("key3", "value3");

            Set<String> keySet = context.keySet();

            assertThat(keySet).hasSize(3);
            assertThat(keySet).contains("key1", "key2", "key3");
        }

        @Test
        @DisplayName("应返回空键集合当上下文为空时")
        void shouldReturnEmptyKeySetWhenContextIsEmpty() {
            Set<String> keySet = context.keySet();

            assertThat(keySet).isEmpty();
        }
    }

    @Nested
    @DisplayName("批量操作测试")
    class BatchOperationTests {

        @Test
        @DisplayName("应能获取所有条目")
        void shouldGetAllEntries() {
            context.put("key1", "value1");
            context.put("key2", 42);
            context.put("key3", new TestObject("test", 123));

            Map<String, Object> allEntries = context.getAll();

            assertThat(allEntries).hasSize(3);
            assertThat(allEntries).containsKeys("key1", "key2", "key3");
            assertThat(allEntries.get("key1")).isEqualTo("value1");
            assertThat(allEntries.get("key2")).isEqualTo(42);
        }

        @Test
        @DisplayName("应返回空映射当上下文为空时")
        void shouldReturnEmptyMapWhenContextIsEmpty() {
            Map<String, Object> allEntries = context.getAll();

            assertThat(allEntries).isEmpty();
        }

        @Test
        @DisplayName("应能清空上下文")
        void shouldClearContext() {
            context.put("key1", "value1");
            context.put("key2", "value2");

            context.clear();

            assertThat(context.keySet()).isEmpty();
            assertThat(context.getString("key1")).isEmpty();
            assertThat(context.getString("key2")).isEmpty();
        }

        @Test
        @DisplayName("应能移除指定键")
        void shouldRemoveSpecificKey() {
            context.put("key1", "value1");
            context.put("key2", "value2");

            Optional<Object> removed = context.remove("key1");

            assertThat(removed).isPresent();
            assertThat(removed.get()).isEqualTo("value1");
            assertThat(context.containsKey("key1")).isFalse();
            assertThat(context.containsKey("key2")).isTrue();
        }

        @Test
        @DisplayName("应返回空Optional当移除不存在的键时")
        void shouldReturnEmptyOptionalWhenRemovingNonExistentKey() {
            Optional<Object> removed = context.remove("non_existent_key");

            assertThat(removed).isEmpty();
        }
    }

    @Nested
    @DisplayName("父子上下文测试")
    class ParentChildContextTests {

        @Test
        @DisplayName("应能创建子上下文")
        void shouldCreateChildContext() {
            DataForgeContext childContext = context.createChildContext();

            assertThat(childContext).isNotNull();
            assertThat(childContext.getParent()).isPresent();
            assertThat(childContext.getParent().get()).isEqualTo(context);
        }

        @Test
        @DisplayName("子上下文应能访问父上下文的值")
        void shouldAccessParentContextValuesFromChild() {
            context.put("parent_key", "parent_value");
            DataForgeContext childContext = context.createChildContext();

            Optional<String> result = childContext.getString("parent_key");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("parent_value");
        }

        @Test
        @DisplayName("子上下文的修改不应影响父上下文")
        void shouldNotAffectParentContextWhenChildModifies() {
            DataForgeContext childContext = context.createChildContext();
            childContext.put("child_key", "child_value");

            Optional<String> parentResult = context.getString("child_key");
            Optional<String> childResult = childContext.getString("child_key");

            assertThat(parentResult).isEmpty();
            assertThat(childResult).isPresent();
            assertThat(childResult.get()).isEqualTo("child_value");
        }

        @Test
        @DisplayName("子上下文应优先使用自己的值")
        void shouldPreferChildContextValueOverParent() {
            context.put("shared_key", "parent_value");
            DataForgeContext childContext = context.createChildContext();
            childContext.put("shared_key", "child_value");

            Optional<String> parentResult = context.getString("shared_key");
            Optional<String> childResult = childContext.getString("shared_key");

            assertThat(parentResult).isPresent();
            assertThat(parentResult.get()).isEqualTo("parent_value");
            assertThat(childResult).isPresent();
            assertThat(childResult.get()).isEqualTo("child_value");
        }

        @Test
        @DisplayName("应正确处理多层嵌套上下文")
        void shouldHandleMultiLevelNestedContexts() {
            DataForgeContext childContext = context.createChildContext();
            DataForgeContext grandChildContext = childContext.createChildContext();

            context.put("level1", "value1");
            childContext.put("level2", "value2");
            grandChildContext.put("level3", "value3");

            assertThat(grandChildContext.getString("level1")).contains("value1");
            assertThat(grandChildContext.getString("level2")).contains("value2");
            assertThat(grandChildContext.getString("level3")).contains("value3");
            assertThat(childContext.getString("level3")).isEmpty();
            assertThat(context.getString("level2")).isEmpty();
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTests {

        @ParameterizedTest
        @ValueSource(strings = {"", "  ", "null", "undefined"})
        @DisplayName("应处理特殊键名")
        void shouldHandleSpecialKeyNames(String specialKey) {
            context.put(specialKey, "special_value");

            Optional<String> result = context.getString(specialKey);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("special_value");
        }

        @Test
        @DisplayName("应处理大量键值对")
        void shouldHandleLargeNumberOfKeyValuePairs() {
            int numberOfPairs = 1000;

            for (int i = 0; i < numberOfPairs; i++) {
                context.put("key_" + i, "value_" + i);
            }

            assertThat(context.keySet()).hasSize(numberOfPairs);
            
            for (int i = 0; i < numberOfPairs; i++) {
                Optional<String> result = context.getString("key_" + i);
                assertThat(result).contains("value_" + i);
            }
        }

        @Test
        @DisplayName("应处理并发访问")
        void shouldHandleConcurrentAccess() throws InterruptedException {
            int threadCount = 10;
            int operationsPerThread = 100;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "thread_" + threadId + "_key_" + j;
                        String value = "thread_" + threadId + "_value_" + j;
                        context.put(key, value);
                        
                        Optional<String> retrieved = context.getString(key);
                        assertThat(retrieved).contains(value);
                        
                        context.remove(key);
                        assertThat(context.getString(key)).isEmpty();
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // 验证所有操作完成后上下文为空
            assertThat(context.keySet()).isEmpty();
        }
    }

    @Nested
    @DisplayName("类型安全测试")
    class TypeSafetyTests {

        @Test
        @DisplayName("应确保类型安全获取")
        void shouldEnsureTypeSafeRetrieval() {
            context.put("string_key", "string_value");
            context.put("int_key", 42);
            context.put("double_key", 3.14);

            // 正确的类型获取
            assertThat(context.getString("string_key")).contains("string_value");
            assertThat(context.get("int_key", Integer.class)).contains(42);
            assertThat(context.get("double_key", Double.class)).contains(3.14);

            // 错误的类型获取应返回空
            assertThat(context.get("string_key", Integer.class)).isEmpty();
            assertThat(context.get("int_key", String.class)).isEmpty();
            assertThat(context.get("double_key", Boolean.class)).isEmpty();
        }

        @Test
        @DisplayName("应处理继承类型的获取")
        void shouldHandleInheritedTypeRetrieval() {
            TestObject testObject = new TestObject("test", 123);
            context.put("object_key", testObject);

            // 可以获取为Object类型
            Optional<Object> asObject = context.get("object_key", Object.class);
            assertThat(asObject).contains(testObject);

            // 也可以获取为具体类型
            Optional<TestObject> asTestObject = context.get("object_key", TestObject.class);
            assertThat(asTestObject).contains(testObject);
        }
    }

    /**
     * 测试用自定义对象
     */
    private static class TestObject {
        private final String name;
        private final int value;

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestObject that = (TestObject) obj;
            return value == that.value && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + value;
        }
    }
}