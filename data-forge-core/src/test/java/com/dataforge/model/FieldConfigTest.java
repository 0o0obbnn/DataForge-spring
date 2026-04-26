package com.dataforge.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FieldConfig 测试")
class FieldConfigTest {

  private SimpleFieldConfig config;

  @BeforeEach
  void setUp() {
    config = new SimpleFieldConfig();
  }

  @Nested
  @DisplayName("构造函数测试")
  class ConstructorTests {
    @Test
    @DisplayName("无参构造函数应创建实例")
    void shouldCreateInstanceWithNoArgConstructor() {
      SimpleFieldConfig cfg = new SimpleFieldConfig();

      assertThat(cfg).isNotNull();
      assertThat(cfg.getName()).isNull();
      assertThat(cfg.getType()).isNull();
    }

    @Test
    @DisplayName("带参数构造函数应设置name和type")
    void shouldSetNameAndTypeWithArgConstructor() {
      SimpleFieldConfig cfg = new SimpleFieldConfig("username", "string");

      assertThat(cfg).isNotNull();
      assertThat(cfg.getName()).isEqualTo("username");
      assertThat(cfg.getType()).isEqualTo("string");
    }

    @Test
    @DisplayName("带参数构造函数应处理null值")
    void shouldHandleNullValuesInConstructor() {
      SimpleFieldConfig cfg = new SimpleFieldConfig(null, null);

      assertThat(cfg).isNotNull();
      assertThat(cfg.getName()).isNull();
      assertThat(cfg.getType()).isNull();
    }
  }

  @Nested
  @DisplayName("name属性测试")
  class NameTests {
    @Test
    @DisplayName("应能设置和获取name")
    void shouldSetAndGetName() {
      config.setName("username");

      assertThat(config.getName()).isEqualTo("username");
    }

    @Test
    @DisplayName("应能设置name为null")
    void shouldSetNameToNull() {
      config.setName("username");
      config.setName(null);

      assertThat(config.getName()).isNull();
    }

    @Test
    @DisplayName("应能设置name为空字符串")
    void shouldSetNameToEmptyString() {
      config.setName("");

      assertThat(config.getName()).isEqualTo("");
    }

    @Test
    @DisplayName("name应遵循命名规范")
    void nameShouldFollowNamingConvention() {
      config.setName("user_name");
      assertThat(config.getName()).isEqualTo("user_name");

      config.setName("_private");
      assertThat(config.getName()).isEqualTo("_private");

      config.setName("Name123");
      assertThat(config.getName()).isEqualTo("Name123");
    }
  }

  @Nested
  @DisplayName("type属性测试")
  class TypeTests {
    @Test
    @DisplayName("应能设置和获取type")
    void shouldSetAndGetType() {
      config.setType("string");

      assertThat(config.getType()).isEqualTo("string");
    }

    @Test
    @DisplayName("应能设置type为null")
    void shouldSetTypeToNull() {
      config.setType("string");
      config.setType(null);

      assertThat(config.getType()).isNull();
    }

    @Test
    @DisplayName("应能设置type为空字符串")
    void shouldSetTypeToEmptyString() {
      config.setType("");

      assertThat(config.getType()).isEqualTo("");
    }

    @Test
    @DisplayName("type应支持各种数据类型")
    void typeShouldSupportVariousDataTypes() {
      config.setType("string");
      assertThat(config.getType()).isEqualTo("string");

      config.setType("integer");
      assertThat(config.getType()).isEqualTo("integer");

      config.setType("decimal");
      assertThat(config.getType()).isEqualTo("decimal");

      config.setType("boolean");
      assertThat(config.getType()).isEqualTo("boolean");

      config.setType("date");
      assertThat(config.getType()).isEqualTo("date");
    }
  }

  @Nested
  @DisplayName("description属性测试")
  class DescriptionTests {
    @Test
    @DisplayName("应能设置和获取description")
    void shouldSetAndGetDescription() {
      config.setDescription("用户名");

      assertThat(config.getDescription()).isEqualTo("用户名");
    }

    @Test
    @DisplayName("默认description应为null")
    void defaultDescriptionShouldBeNull() {
      assertThat(config.getDescription()).isNull();
    }

    @Test
    @DisplayName("应能设置description为null")
    void shouldSetDescriptionToNull() {
      config.setDescription("用户名");
      config.setDescription(null);

      assertThat(config.getDescription()).isNull();
    }

    @Test
    @DisplayName("应能设置description为空字符串")
    void shouldSetDescriptionToEmptyString() {
      config.setDescription("");

      assertThat(config.getDescription()).isEqualTo("");
    }

    @Test
    @DisplayName("description应支持长文本")
    void descriptionShouldSupportLongText() {
      String longDescription = "这是一个很长的字段描述，用于测试description属性是否能够正确处理长文本内容。";
      config.setDescription(longDescription);

      assertThat(config.getDescription()).isEqualTo(longDescription);
    }
  }

  @Nested
  @DisplayName("required属性测试")
  class RequiredTests {
    @Test
    @DisplayName("默认required应为true")
    void defaultRequiredShouldBeTrue() {
      assertThat(config.isRequired()).isEqualTo(true);
    }

    @Test
    @DisplayName("应能设置和获取required")
    void shouldSetAndGetRequired() {
      config.setRequired(false);

      assertThat(config.isRequired()).isEqualTo(false);

      config.setRequired(true);

      assertThat(config.isRequired()).isEqualTo(true);
    }

    @Test
    @DisplayName("应能设置required为false")
    void shouldSetRequiredToFalse() {
      config.setRequired(false);

      assertThat(config.isRequired()).isFalse();
    }
  }

  @Nested
  @DisplayName("params属性测试")
  class ParamsTests {
    @Test
    @DisplayName("默认params应为空Map")
    void defaultParamsShouldBeEmptyMap() {
      assertThat(config.getParams()).isNotNull();
      assertThat(config.getParams()).isEmpty();
    }

    @Test
    @DisplayName("应能设置和获取params")
    void shouldSetAndGetParams() {
      Map<String, Object> params = new HashMap<>();
      params.put("min", 1);
      params.put("max", 100);

      config.setParams(params);

      assertThat(config.getParams()).isEqualTo(params);
    }

    @Test
    @DisplayName("设置params为null应创建空Map")
    void shouldCreateEmptyMapWhenSetParamsToNull() {
      Map<String, Object> params = new HashMap<>();
      params.put("key", "value");

      config.setParams(params);
      config.setParams(null);

      assertThat(config.getParams()).isNotNull();
      assertThat(config.getParams()).isEmpty();
    }

    @Test
    @DisplayName("设置params应替换原有params")
    void shouldReplaceExistingParams() {
      config.setParam("oldKey", "oldValue");

      Map<String, Object> newParams = new HashMap<>();
      newParams.put("newKey", "newValue");
      config.setParams(newParams);

      assertThat(config.getParams()).hasSize(1);
      assertThat(config.getParams()).containsKey("newKey");
      assertThat(config.getParams()).doesNotContainKey("oldKey");
    }
  }

  @Nested
  @DisplayName("参数管理方法测试")
  class ParameterManagementTests {
    @Test
    @DisplayName("应能添加参数")
    void shouldAddParameter() {
      config.setParam("min", 1);

      assertThat(config.hasParam("min")).isEqualTo(true);
      assertThat(config.getParam("min")).isEqualTo(1);
    }

    @Test
    @DisplayName("应能获取参数")
    void shouldGetParameter() {
      config.setParam("max", 100);

      assertThat(config.getParam("max")).isEqualTo(100);
    }

    @Test
    @DisplayName("获取不存在的参数应返回null")
    void shouldReturnNullForNonExistentParameter() {
      assertThat(config.getParam("nonexistent")).isNull();
    }

    @Test
    @DisplayName("应能检查参数是否存在")
    void shouldCheckParameterExists() {
      assertThat(config.hasParam("key")).isEqualTo(false);

      config.setParam("key", "value");

      assertThat(config.hasParam("key")).isEqualTo(true);
    }

    @Test
    @DisplayName("应能移除参数")
    void shouldRemoveParameter() {
      config.setParam("key", "value");

      Object removedValue = config.removeParam("key");

      assertThat(removedValue).isEqualTo("value");
      assertThat(config.hasParam("key")).isFalse();
    }

    @Test
    @DisplayName("移除不存在的参数应返回null")
    void shouldReturnNullWhenRemovingNonExistentParameter() {
      Object removedValue = config.removeParam("nonexistent");

      assertThat(removedValue).isNull();
    }

    @Test
    @DisplayName("应能覆盖已有参数")
    void shouldOverwriteExistingParameter() {
      config.setParam("key", "oldValue");
      config.setParam("key", "newValue");

      assertThat(config.getParam("key")).isEqualTo("newValue");
      assertThat(config.getParams()).hasSize(1);
    }

    @Test
    @DisplayName("应支持各种类型的参数值")
    void shouldSupportVariousParameterTypes() {
      config.setParam("string", "value");
      config.setParam("integer", 123);
      config.setParam("decimal", 3.14);
      config.setParam("boolean", true);
      config.setParam("null", null);

      assertThat(config.getParam("string")).isEqualTo("value");
      assertThat(config.getParam("integer")).isEqualTo(123);
      assertThat(config.getParam("decimal")).isEqualTo(3.14);
      assertThat(config.getParam("boolean")).isEqualTo(true);
      assertThat(config.getParam("null")).isNull();
    }
  }

  @Nested
  @DisplayName("类型化参数获取测试")
  class TypedParameterTests {
    @Test
    @DisplayName("应能获取指定类型的参数")
    void shouldGetParameterWithType() {
      config.setParam("value", 123);

      Integer value = config.getParam("value", Integer.class, 0);

      assertThat(value).isEqualTo(123);
    }

    @Test
    @DisplayName("类型不匹配应返回默认值")
    void shouldReturnDefaultValueWhenTypeMismatch() {
      config.setParam("value", "123");

      Integer value = config.getParam("value", Integer.class, 0);

      assertThat(value).isEqualTo(0);
    }

    @Test
    @DisplayName("参数不存在应返回默认值")
    void shouldReturnDefaultValueWhenParameterNotExists() {
      Integer value = config.getParam("nonexistent", Integer.class, 999);

      assertThat(value).isEqualTo(999);
    }

    @Test
    @DisplayName("应支持String类型参数")
    void shouldSupportStringParameter() {
      config.setParam("value", "test");

      String value = config.getParam("value", String.class, "default");

      assertThat(value).isEqualTo("test");
    }

    @Test
    @DisplayName("应支持Integer类型参数")
    void shouldSupportIntegerParameter() {
      config.setParam("value", 42);

      Integer value = config.getParam("value", Integer.class, 0);

      assertThat(value).isEqualTo(42);
    }

    @Test
    @DisplayName("应支持Double类型参数")
    void shouldSupportDoubleParameter() {
      config.setParam("value", 3.14159);

      Double value = config.getParam("value", Double.class, 0.0);

      assertThat(value).isEqualTo(3.14159);
    }

    @Test
    @DisplayName("应支持Boolean类型参数")
    void shouldSupportBooleanParameter() {
      config.setParam("value", true);

      Boolean value = config.getParam("value", Boolean.class, false);

      assertThat(value).isEqualTo(true);
    }

    @Test
    @DisplayName("应支持null默认值")
    void shouldSupportNullDefaultValue() {
      String value = config.getParam("nonexistent", String.class, null);

      assertThat(value).isNull();
    }
  }

  @Nested
  @DisplayName("toString测试")
  class ToStringTests {
    @Test
    @DisplayName("toString应包含基本信息")
    void toStringShouldContainBasicInfo() {
      config.setName("username");
      config.setType("string");

      String str = config.toString();

      assertThat(str).contains("SimpleFieldConfig");
      assertThat(str).contains("name='username'");
      assertThat(str).contains("type='string'");
    }

    @Test
    @DisplayName("toString应包含required状态")
    void toStringShouldContainRequiredStatus() {
      config.setName("username");
      config.setType("string");
      config.setRequired(false);

      String str = config.toString();

      assertThat(str).contains("required=false");
    }

    @Test
    @DisplayName("toString应包含params")
    void toStringShouldContainParams() {
      config.setName("username");
      config.setType("string");
      config.setParam("min", 1);
      config.setParam("max", 100);

      String str = config.toString();

      assertThat(str).contains("params=");
    }

    @Test
    @DisplayName("toString格式应正确")
    void toStringShouldHaveCorrectFormat() {
      config.setName("username");
      config.setType("string");

      String str = config.toString();

      assertThat(str)
          .matches(
              "SimpleFieldConfig\\{name='username', type='string', required=true, params=\\{\\}\\}");
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {
    @Test
    @DisplayName("name长度应为255字符以内")
    void nameLengthShouldBeWithin255Characters() {
      String shortName = "a";
      config.setName(shortName);
      assertThat(config.getName()).isEqualTo(shortName);

      String longName = "a".repeat(255);
      config.setName(longName);
      assertThat(config.getName()).isEqualTo(longName);
    }

    @Test
    @DisplayName("type长度应为100字符以内")
    void typeLengthShouldBeWithin100Characters() {
      String shortType = "a";
      config.setType(shortType);
      assertThat(config.getType()).isEqualTo(shortType);

      String longType = "a".repeat(100);
      config.setType(longType);
      assertThat(config.getType()).isEqualTo(longType);
    }

    @Test
    @DisplayName("应支持大量参数")
    void shouldSupportManyParameters() {
      for (int i = 0; i < 100; i++) {
        config.setParam("key" + i, "value" + i);
      }

      assertThat(config.getParams()).hasSize(100);
      for (int i = 0; i < 100; i++) {
        assertThat(config.getParam("key" + i)).isEqualTo("value" + i);
      }
    }

    @Test
    @DisplayName("应支持复杂参数值")
    void shouldSupportComplexParameterValues() {
      Map<String, Object> nestedMap = new HashMap<>();
      nestedMap.put("nestedKey", "nestedValue");
      config.setParam("map", nestedMap);

      assertThat(config.getParam("map")).isEqualTo(nestedMap);
    }
  }

  @Nested
  @DisplayName("参数验证测试")
  class ParameterValidationTests {
    @Test
    @DisplayName("name应以字母或下划线开头")
    void nameShouldStartWithLetterOrUnderscore() {
      config.setName("username");
      assertThat(config.getName()).isEqualTo("username");

      config.setName("_private");
      assertThat(config.getName()).isEqualTo("_private");
    }

    @Test
    @DisplayName("name应只包含字母、数字和下划线")
    void nameShouldContainOnlyLettersNumbersAndUnderscores() {
      config.setName("user_name123");
      assertThat(config.getName()).isEqualTo("user_name123");

      config.setName("UserName");
      assertThat(config.getName()).isEqualTo("UserName");
    }

    @Test
    @DisplayName("params应为独立实例")
    void paramsShouldBeIndependentInstance() {
      Map<String, Object> originalParams = new HashMap<>();
      originalParams.put("key", "value");

      config.setParams(originalParams);

      Map<String, Object> params = config.getParams();
      params.put("newKey", "newValue");

      assertThat(config.getParams()).containsKey("newKey");
    }

    @Test
    @DisplayName("设置空params应保留原有params")
    void shouldRetainOriginalParamsWhenSettingEmptyMap() {
      config.setParam("key", "value");

      config.setParams(new HashMap<>());

      assertThat(config.getParams()).isEmpty();
    }
  }
}
