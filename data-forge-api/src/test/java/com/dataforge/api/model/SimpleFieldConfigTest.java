package com.dataforge.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SimpleFieldConfig 测试")
class SimpleFieldConfigTest {

  private SimpleFieldConfig config;

  @BeforeEach
  void setUp() {
    config = new SimpleFieldConfig("testField", "string");
  }

  @Nested
  @DisplayName("基本功能测试")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("应创建配置")
    void shouldCreateConfig() {
      assertThat(config.getName()).isEqualTo("testField");
      assertThat(config.getType()).isEqualTo("string");
      assertThat(config.getParams()).isNotNull();
    }

    @Test
    @DisplayName("应创建带参数的配置")
    void shouldCreateConfigWithParams() {
      Map<String, Object> params = new HashMap<>();
      params.put("length", 10);
      params.put("nullable", false);

      SimpleFieldConfig configWithParams =
          new SimpleFieldConfig("testField", "string", params);

      assertThat(configWithParams.getName()).isEqualTo("testField");
      assertThat(configWithParams.getType()).isEqualTo("string");
      assertThat(configWithParams.getParams()).hasSize(2);
      assertThat(configWithParams.getParams().get("length")).isEqualTo(10);
      assertThat(configWithParams.getParams().get("nullable")).isEqualTo(false);
    }

    @Test
    @DisplayName("应设置参数")
    void shouldSetParameter() {
      config.setParam("length", 20);

      assertThat(config.getParams().get("length")).isEqualTo(20);
    }

    @Test
    @DisplayName("应获取参数")
    void shouldGetParameter() {
      config.setParam("length", 30);

      Integer length = config.getParam("length", Integer.class, 0);

      assertThat(length).isEqualTo(30);
    }

    @Test
    @DisplayName("应获取参数Optional")
    void shouldGetParameterOptional() {
      config.setParam("length", 40);

      Optional<Integer> length = config.getParamOptional("length", Integer.class);

      assertThat(length).isPresent();
      assertThat(length.get()).isEqualTo(40);
    }

    @Test
    @DisplayName("应检查参数存在")
    void shouldCheckParameterExists() {
      config.setParam("length", 50);

      assertThat(config.hasParam("length")).isTrue();
      assertThat(config.hasParam("nonexistent")).isFalse();
    }
  }

  @Nested
  @DisplayName("参数默认值测试")
  class ParameterDefaultValueTests {

    @Test
    @DisplayName("应返回默认值当参数不存在")
    void shouldReturnDefaultValueWhenParameterNotExists() {
      Integer length = config.getParam("length", Integer.class, 100);

      assertThat(length).isEqualTo(100);
    }

    @Test
    @DisplayName("应返回默认值当参数类型不匹配")
    void shouldReturnDefaultValueWhenTypeMismatch() {
      config.setParam("length", "string_value");

      Integer length = config.getParam("length", Integer.class, 200);

      assertThat(length).isEqualTo(200);
    }

    @Test
    @DisplayName("应返回空Optional当参数不存在")
    void shouldReturnEmptyOptionalWhenParameterNotExists() {
      Optional<String> value = config.getParamOptional("nonexistent", String.class);

      assertThat(value).isEmpty();
    }

    @Test
    @DisplayName("应返回空Optional当参数类型不匹配")
    void shouldReturnEmptyOptionalWhenTypeMismatch() {
      config.setParam("value", "string_value");

      Optional<Integer> value = config.getParamOptional("value", Integer.class);

      assertThat(value).isEmpty();
    }

    @Test
    @DisplayName("应处理null参数值")
    void shouldHandleNullParameterValue() {
      config.setParam("nullValue", null);

      String value = config.getParam("nullValue", String.class, "default");

      assertThat(value).isEqualTo("default");
    }
  }

  @Nested
  @DisplayName("参数类型测试")
  class ParameterTypeTests {

    @Test
    @DisplayName("应获取字符串参数")
    void shouldGetStringParameter() {
      config.setParam("name", "John");

      String name = config.getParam("name", String.class, "");

      assertThat(name).isEqualTo("John");
    }

    @Test
    @DisplayName("应获取整数参数")
    void shouldGetIntegerParameter() {
      config.setParam("age", 30);

      Integer age = config.getParam("age", Integer.class, 0);

      assertThat(age).isEqualTo(30);
    }

    @Test
    @DisplayName("应获取布尔参数")
    void shouldGetBooleanParameter() {
      config.setParam("active", true);

      Boolean active = config.getParam("active", Boolean.class, false);

      assertThat(active).isTrue();
    }

    @Test
    @DisplayName("应获取长整型参数")
    void shouldGetLongParameter() {
      config.setParam("timestamp", 1234567890L);

      Long timestamp = config.getParam("timestamp", Long.class, 0L);

      assertThat(timestamp).isEqualTo(1234567890L);
    }

    @Test
    @DisplayName("应获取双精度参数")
    void shouldGetDoubleParameter() {
      config.setParam("price", 19.99);

      Double price = config.getParam("price", Double.class, 0.0);

      assertThat(price).isEqualTo(19.99);
    }

    @Test
    @DisplayName("应获取对象参数")
    void shouldGetObjectParameter() {
      Map<String, Object> nested = new HashMap<>();
      nested.put("key", "value");
      config.setParam("nested", nested);

      @SuppressWarnings("unchecked")
      Map<String, Object> result =
          config.getParam("nested", Map.class, new HashMap<>());

      assertThat(result).containsEntry("key", "value");
    }
  }

  @Nested
  @DisplayName("参数映射测试")
  class ParameterMapTests {

    @Test
    @DisplayName("应返回参数映射的副本")
    void shouldReturnCopyOfParamsMap() {
      config.setParam("key1", "value1");
      Map<String, Object> params1 = config.getParams();

      assertThatThrownBy(() -> params1.put("key2", "value2"))
          .isInstanceOf(UnsupportedOperationException.class);
      
      Map<String, Object> params2 = config.getParams();

      assertThat(params2).hasSize(1);
      assertThat(params2).containsEntry("key1", "value1");
    }

    @Test
    @DisplayName("应支持多个参数")
    void shouldSupportMultipleParameters() {
      config.setParam("param1", "value1");
      config.setParam("param2", 123);
      config.setParam("param3", true);

      Map<String, Object> params = config.getParams();

      assertThat(params).hasSize(3);
      assertThat(params).containsEntry("param1", "value1");
      assertThat(params).containsEntry("param2", 123);
      assertThat(params).containsEntry("param3", true);
    }

    @Test
    @DisplayName("应覆盖已存在的参数")
    void shouldOverrideExistingParameter() {
      config.setParam("key", "value1");
      config.setParam("key", "value2");

      String value = config.getParam("key", String.class, "");

      assertThat(value).isEqualTo("value2");
    }
  }

  @Nested
  @DisplayName("Builder测试")
  class BuilderTests {

    @Test
    @DisplayName("应使用Builder创建配置")
    void shouldCreateConfigUsingBuilder() {
      SimpleFieldConfig builtConfig =
          SimpleFieldConfig.builder("builtField", "integer")
              .param("min", 1)
              .param("max", 100)
              .build();

      assertThat(builtConfig.getName()).isEqualTo("builtField");
      assertThat(builtConfig.getType()).isEqualTo("integer");
      assertThat(builtConfig.getParams()).hasSize(2);
      assertThat(builtConfig.getParam("min", Integer.class, 0)).isEqualTo(1);
      assertThat(builtConfig.getParam("max", Integer.class, 0)).isEqualTo(100);
    }

    @Test
    @DisplayName("应支持Builder链式调用")
    void shouldSupportBuilderChaining() {
      SimpleFieldConfig builtConfig =
          SimpleFieldConfig.builder("field", "string")
              .param("length", 10)
              .param("nullable", true)
              .param("pattern", "[a-z]+")
              .build();

      assertThat(builtConfig.getParams()).hasSize(3);
    }

    @Test
    @DisplayName("应创建无参数的Builder配置")
    void shouldCreateBuilderConfigWithoutParams() {
      SimpleFieldConfig builtConfig =
          SimpleFieldConfig.builder("field", "string").build();

      assertThat(builtConfig.getParams()).isEmpty();
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {

    @Test
    @DisplayName("应处理空名称")
    void shouldHandleEmptyName() {
      SimpleFieldConfig emptyNameConfig = new SimpleFieldConfig("", "string");

      assertThat(emptyNameConfig.getName()).isEmpty();
    }

    @Test
    @DisplayName("应处理空类型")
    void shouldHandleEmptyType() {
      SimpleFieldConfig emptyTypeConfig = new SimpleFieldConfig("field", "");

      assertThat(emptyTypeConfig.getType()).isEmpty();
    }

    @Test
    @DisplayName("应处理null参数值")
    void shouldHandleNullParameterValue() {
      config.setParam("nullKey", null);

      assertThat(config.hasParam("nullKey")).isTrue();
      assertThat(config.getParam("nullKey", String.class, "default")).isEqualTo("default");
    }

    @Test
    @DisplayName("应处理空参数映射")
    void shouldHandleEmptyParamsMap() {
      SimpleFieldConfig configWithEmptyParams =
          new SimpleFieldConfig("field", "string", new HashMap<>());

      assertThat(configWithEmptyParams.getParams()).isEmpty();
    }

    @Test
    @DisplayName("应处理特殊字符参数键")
    void shouldHandleSpecialCharacterParameterKeys() {
      config.setParam("key-with-dash", "value1");
      config.setParam("key_with_underscore", "value2");
      config.setParam("key.with.dot", "value3");

      assertThat(config.getParam("key-with-dash", String.class, "")).isEqualTo("value1");
      assertThat(config.getParam("key_with_underscore", String.class, "")).isEqualTo("value2");
      assertThat(config.getParam("key.with.dot", String.class, "")).isEqualTo("value3");
    }

    @Test
    @DisplayName("应处理大量参数")
    void shouldHandleLargeNumberOfParameters() {
      for (int i = 0; i < 1000; i++) {
        config.setParam("param" + i, "value" + i);
      }

      assertThat(config.getParams()).hasSize(1000);
      assertThat(config.getParam("param500", String.class, "")).isEqualTo("value500");
    }
  }

  @Nested
  @DisplayName("不可变性测试")
  class ImmutabilityTests {

    @Test
    @DisplayName("getParams应返回不可变映射")
    void shouldReturnImmutableMapFromGetParams() {
      config.setParam("key", "value");
      Map<String, Object> params = config.getParams();

      assertThatThrownBy(() -> params.put("newKey", "newValue"))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }
}
