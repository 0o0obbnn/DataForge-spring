package com.dataforge.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SimpleFieldConfig 测试")
class SimpleFieldConfigTest {

  @Nested
  @DisplayName("构造函数测试")
  class ConstructorTests {
    @Test
    @DisplayName("无参构造函数应创建实例")
    void shouldCreateInstanceWithNoArgConstructor() {
      SimpleFieldConfig config = new SimpleFieldConfig();

      assertThat(config).isNotNull();
      assertThat(config.getName()).isNull();
      assertThat(config.getType()).isNull();
      assertThat(config.isRequired()).isEqualTo(true);
      assertThat(config.getParams()).isNotNull();
      assertThat(config.getParams()).isEmpty();
    }

    @Test
    @DisplayName("带参数构造函数应设置name和type")
    void shouldSetNameAndTypeWithArgConstructor() {
      SimpleFieldConfig config = new SimpleFieldConfig("username", "string");

      assertThat(config).isNotNull();
      assertThat(config.getName()).isEqualTo("username");
      assertThat(config.getType()).isEqualTo("string");
      assertThat(config.isRequired()).isEqualTo(true);
    }

    @Test
    @DisplayName("带参数构造函数应处理null值")
    void shouldHandleNullValuesInConstructor() {
      SimpleFieldConfig config = new SimpleFieldConfig(null, null);

      assertThat(config).isNotNull();
      assertThat(config.getName()).isNull();
      assertThat(config.getType()).isNull();
    }

    @Test
    @DisplayName("带参数构造函数应初始化默认值")
    void shouldInitializeDefaultValuesWithArgConstructor() {
      SimpleFieldConfig config = new SimpleFieldConfig("age", "integer");

      assertThat(config.getDescription()).isNull();
      assertThat(config.isRequired()).isEqualTo(true);
      assertThat(config.getParams()).isNotNull();
      assertThat(config.getParams()).isEmpty();
    }
  }

  @Nested
  @DisplayName("继承功能测试")
  class InheritedFunctionalityTests {
    @Test
    @DisplayName("应支持FieldConfig的所有功能")
    void shouldSupportAllFieldConfigFeatures() {
      SimpleFieldConfig config = new SimpleFieldConfig("username", "string");

      // 测试基本属性
      config.setDescription("用户名");
      config.setRequired(false);

      // 测试参数管理
      config.setParam("minLength", 6);
      config.setParam("maxLength", 20);

      assertThat(config.getName()).isEqualTo("username");
      assertThat(config.getType()).isEqualTo("string");
      assertThat(config.getDescription()).isEqualTo("用户名");
      assertThat(config.isRequired()).isEqualTo(false);
      assertThat(config.getParam("minLength")).isEqualTo(6);
      assertThat(config.getParam("maxLength")).isEqualTo(20);
    }

    @Test
    @DisplayName("应支持参数的类型化获取")
    void shouldSupportTypedParameterRetrieval() {
      SimpleFieldConfig config = new SimpleFieldConfig("age", "integer");

      config.setParam("min", 18);
      config.setParam("max", 120);

      Integer min = config.getParam("min", Integer.class, 0);
      Integer max = config.getParam("max", Integer.class, 999);

      assertThat(min).isEqualTo(18);
      assertThat(max).isEqualTo(120);
    }

    @Test
    @DisplayName("应支持toString方法")
    void shouldSupportToStringMethod() {
      SimpleFieldConfig config = new SimpleFieldConfig("email", "string");

      String str = config.toString();

      assertThat(str).contains("SimpleFieldConfig");
      assertThat(str).contains("name='email'");
      assertThat(str).contains("type='string'");
    }

    @Test
    @DisplayName("应支持参数删除")
    void shouldSupportParameterRemoval() {
      SimpleFieldConfig config = new SimpleFieldConfig("field", "type");

      config.setParam("temp", "value");
      Object removed = config.removeParam("temp");

      assertThat(removed).isEqualTo("value");
      assertThat(config.hasParam("temp")).isEqualTo(false);
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {
    @Test
    @DisplayName("应能处理空字符串name和type")
    void shouldHandleEmptyStringNameAndType() {
      SimpleFieldConfig config = new SimpleFieldConfig("", "");

      assertThat(config.getName()).isEqualTo("");
      assertThat(config.getType()).isEqualTo("");
    }

    @Test
    @DisplayName("应能处理特殊字符name")
    void shouldHandleSpecialCharacterName() {
      SimpleFieldConfig config = new SimpleFieldConfig("_user_name_123", "string");

      assertThat(config.getName()).isEqualTo("_user_name_123");
    }

    @Test
    @DisplayName("应能处理大量参数")
    void shouldHandleManyParameters() {
      SimpleFieldConfig config = new SimpleFieldConfig("data", "json");

      for (int i = 0; i < 50; i++) {
        config.setParam("key" + i, "value" + i);
      }

      assertThat(config.getParams()).hasSize(50);
    }

    @Test
    @DisplayName("应能处理null参数值")
    void shouldHandleNullParameterValue() {
      SimpleFieldConfig config = new SimpleFieldConfig("field", "type");

      config.setParam("nullable", null);

      assertThat(config.hasParam("nullable")).isEqualTo(true);
      assertThat(config.getParam("nullable")).isNull();
    }
  }

  @Nested
  @DisplayName("类型验证测试")
  class TypeValidationTests {
    @Test
    @DisplayName("实例应为SimpleFieldConfig类型")
    void instanceShouldBeSimpleFieldConfigType() {
      SimpleFieldConfig config = new SimpleFieldConfig();

      assertThat(config).isInstanceOf(SimpleFieldConfig.class);
    }

    @Test
    @DisplayName("实例应为FieldConfig类型")
    void instanceShouldBeFieldConfigType() {
      SimpleFieldConfig config = new SimpleFieldConfig();

      assertThat(config).isInstanceOf(FieldConfig.class);
    }

    @Test
    @DisplayName("实例应能向上转型为FieldConfig")
    void instanceShouldBeUpcastableToFieldConfig() {
      SimpleFieldConfig simpleConfig = new SimpleFieldConfig();
      FieldConfig fieldConfig = simpleConfig;

      assertThat(fieldConfig).isNotNull();
      assertThat(fieldConfig.getName()).isNull();
    }
  }

  @Nested
  @DisplayName("多实例测试")
  class MultipleInstancesTests {
    @Test
    @DisplayName("多个实例应相互独立")
    void multipleInstancesShouldBeIndependent() {
      SimpleFieldConfig config1 = new SimpleFieldConfig("field1", "type1");
      SimpleFieldConfig config2 = new SimpleFieldConfig("field2", "type2");

      config1.setDescription("Description 1");
      config2.setDescription("Description 2");

      assertThat(config1.getName()).isEqualTo("field1");
      assertThat(config2.getName()).isEqualTo("field2");
      assertThat(config1.getDescription()).isEqualTo("Description 1");
      assertThat(config2.getDescription()).isEqualTo("Description 2");
    }

    @Test
    @DisplayName("参数Map应相互独立")
    void parameterMapsShouldBeIndependent() {
      SimpleFieldConfig config1 = new SimpleFieldConfig();
      SimpleFieldConfig config2 = new SimpleFieldConfig();

      config1.setParam("key", "value1");
      config2.setParam("key", "value2");

      assertThat(config1.getParam("key")).isEqualTo("value1");
      assertThat(config2.getParam("key")).isEqualTo("value2");
    }
  }

  @Nested
  @DisplayName("默认值测试")
  class DefaultValueTests {
    @Test
    @DisplayName("required默认值应为true")
    void requiredDefaultValueShouldBeTrue() {
      SimpleFieldConfig config = new SimpleFieldConfig();

      assertThat(config.isRequired()).isEqualTo(true);
    }

    @Test
    @DisplayName("params默认值应为空Map")
    void paramsDefaultValueShouldBeEmptyMap() {
      SimpleFieldConfig config = new SimpleFieldConfig();

      assertThat(config.getParams()).isNotNull();
      assertThat(config.getParams()).isEmpty();
    }

    @Test
    @DisplayName("description默认值应为null")
    void descriptionDefaultValueShouldBeNull() {
      SimpleFieldConfig config = new SimpleFieldConfig();

      assertThat(config.getDescription()).isNull();
    }
  }
}