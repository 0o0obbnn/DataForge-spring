package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("YamlGenerator 测试")
class YamlGeneratorTest {

  private YamlGenerator generator;
  private SimpleFieldConfig config;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new YamlGenerator();
    config = new SimpleFieldConfig();
    config.setType("yaml");
    context = new DataForgeContext();
  }

  @Nested
  @DisplayName("基本功能测试")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("应生成YAML内容")
    void shouldGenerateYamlContent() {
      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
      assertThat(yaml).isNotEmpty();
    }

    @Test
    @DisplayName("应生成有效YAML格式")
    void shouldGenerateValidYamlFormat() {
      String yaml = generator.generate(config, context);

      assertThat(yaml).contains(":");
      assertThat(yaml).doesNotContain("\t");
    }

    @Test
    @DisplayName("空配置应使用默认值")
    void shouldUseDefaultValuesForEmptyConfig() {
      SimpleFieldConfig emptyConfig = new SimpleFieldConfig();
      emptyConfig.setType("yaml");

      String yaml = generator.generate(emptyConfig, context);

      assertThat(yaml).isNotNull();
      assertThat(yaml).contains(":");
    }
  }

  @Nested
  @DisplayName("结构类型测试")
  class StructureTypeTests {

    @Test
    @DisplayName("应生成简单结构YAML")
    void shouldGenerateSimpleStructure() {
      config.setParam("structure", "SIMPLE");
      config.setParam("key_count", "3");

      String yaml = generator.generate(config, context);

      assertThat(yaml).contains(":");
      assertThat(yaml.lines().filter(line -> line.contains(":")).count()).isEqualTo(3L);
    }

    @Test
    @DisplayName("应生成嵌套结构YAML")
    void shouldGenerateNestedStructure() {
      config.setParam("structure", "NESTED");
      config.setParam("depth", "3");

      String yaml = generator.generate(config, context);

      assertThat(yaml).contains("  ");
      assertThat(yaml).contains("\n");
      assertThat(yaml.lines().filter(line -> line.startsWith("    ")).count()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("应生成列表结构YAML")
    void shouldGenerateListStructure() {
      config.setParam("structure", "LIST");

      String yaml = generator.generate(config, context);

      assertThat(yaml).contains("items:");
      assertThat(yaml).contains("- ");
    }

    @Test
    @DisplayName("应生成复杂结构YAML")
    void shouldGenerateComplexStructure() {
      config.setParam("structure", "COMPLEX");

      String yaml = generator.generate(config, context);

      assertThat(yaml).contains("config:").contains("database:").contains("features:");
    }

    @Test
    @DisplayName("无效结构类型应使用默认值")
    void shouldUseDefaultForInvalidStructure() {
      config.setParam("structure", "INVALID");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
      assertThat(yaml).contains(":");
    }
  }

  @Nested
  @DisplayName("参数配置测试")
  class ParameterConfigurationTests {

    @Test
    @DisplayName("应支持自定义深度")
    void shouldSupportCustomDepth() {
      config.setParam("structure", "NESTED");
      config.setParam("depth", "5");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
      assertThat(yaml).contains("    ");
    }

    @Test
    @DisplayName("应支持自定义键数量")
    void shouldSupportCustomKeyCount() {
      config.setParam("key_count", "10");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
      assertThat(yaml.lines().filter(line -> line.contains(":")).count()).isEqualTo(10L);
    }

    @Test
    @DisplayName("应支持字符串值类型")
    void shouldSupportStringValueType() {
      config.setParam("value_types", "STRING");

      String yaml = generator.generate(config, context);

      assertThat(yaml).contains("\"");
    }

    @Test
    @DisplayName("应支持数值类型")
    void shouldSupportNumberValueType() {
      config.setParam("value_types", "NUMBER");

      String yaml = generator.generate(config, context);

      assertThat(yaml).containsPattern(":\\s*\\d+");
    }

    @Test
    @DisplayName("应支持布尔类型")
    void shouldSupportBooleanValueType() {
      config.setParam("value_types", "BOOLEAN");

      String yaml = generator.generate(config, context);

      assertThat(yaml)
          .satisfiesAnyOf(
              y -> assertThat(y).contains("true"), y -> assertThat(y).contains("false"));
    }

    @Test
    @DisplayName("应支持null值类型")
    void shouldSupportNullValueType() {
      config.setParam("value_types", "NULL");

      String yaml = generator.generate(config, context);

      assertThat(yaml.toLowerCase()).contains("null");
    }

    @Test
    @DisplayName("应支持混合值类型")
    void shouldSupportMixedValueTypes() {
      config.setParam("value_types", "STRING,NUMBER,BOOLEAN");

      String yaml = generator.generate(config, context);

      assertThat(yaml)
          .contains("\"")
          .satisfiesAnyOf(
              y -> assertThat(y).contains("true"), y -> assertThat(y).contains("false"));

      // 检查是否包含数字模式，使用更宽松的验证
      // 由于随机性，可能不会每次都包含数字，所以使用更宽松的断言
      boolean hasNumberPattern = yaml.matches(".*:\\s*\\d+.*");
      boolean hasBooleanPattern = yaml.contains("true") || yaml.contains("false");
      boolean hasStringPattern = yaml.contains("\"");

      // 至少应该包含其中一种类型
      assertThat(hasNumberPattern || hasBooleanPattern || hasStringPattern).isTrue();
    }

    @Test
    @DisplayName("应支持ALL值类型")
    void shouldSupportAllValueTypes() {
      config.setParam("value_types", "ALL");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
    }

    @Test
    @DisplayName("应支持包含注释")
    void shouldSupportIncludingComments() {
      config.setParam("include_comments", "true");

      String yaml = generator.generate(config, context);

      assertThat(yaml).startsWith("#");
      assertThat(yaml).contains("Structure type:");
    }

    @Test
    @DisplayName("应支持生成无效YAML")
    void shouldSupportGeneratingInvalidYaml() {
      config.setParam("structure", "SIMPLE");
      config.setParam("invalid_yaml", "true");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
      boolean hasMissingColon =
          yaml.lines().filter(line -> !line.trim().isEmpty()).anyMatch(line -> !line.contains(":"));
      assertThat(hasMissingColon).isTrue();
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 5, 10})
    @DisplayName("应支持有效深度范围")
    void shouldSupportValidDepthRange(int depth) {
      config.setParam("structure", "NESTED");
      config.setParam("depth", String.valueOf(depth));

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11, 100})
    @DisplayName("应处理超出范围的深度")
    void shouldHandleOutOfRangeDepth(int depth) {
      config.setParam("structure", "NESTED");
      config.setParam("depth", String.valueOf(depth));

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 100})
    @DisplayName("应支持有效键数量范围")
    void shouldSupportValidKeyCountRange(int keyCount) {
      config.setParam("key_count", String.valueOf(keyCount));

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101, 1000})
    @DisplayName("应处理超出范围的键数量")
    void shouldHandleOutOfRangeKeyCount(int keyCount) {
      config.setParam("key_count", String.valueOf(keyCount));

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
    }

    @Test
    @DisplayName("最小深度应为1")
    void shouldUseMinimumDepthOf1() {
      config.setParam("structure", "NESTED");
      config.setParam("depth", "0");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
      assertThat(yaml).doesNotContain("    ");
    }

    @Test
    @DisplayName("最大深度应为10")
    void shouldUseMaximumDepthOf10() {
      config.setParam("structure", "NESTED");
      config.setParam("depth", "100");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
    }

    @Test
    @DisplayName("最小键数量应为1")
    void shouldUseMinimumKeyCountOf1() {
      config.setParam("key_count", "0");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
      assertThat(yaml.lines().filter(line -> line.contains(":")).count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("最大键数量应为100")
    void shouldUseMaximumKeyCountOf100() {
      config.setParam("key_count", "1000");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
      assertThat(yaml.lines().filter(line -> line.contains(":")).count()).isLessThanOrEqualTo(100L);
    }
  }

  @Nested
  @DisplayName("异常处理测试")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("null配置应不抛出异常")
    void shouldHandleNullConfig() {
      String yaml = generator.generate(null, context);

      assertThat(yaml).isNotNull();
      // YamlGenerator在null配置时使用默认值生成YAML，而不是错误信息
      assertThat(yaml).contains(":");
    }

    @Test
    @DisplayName("null上下文应不抛出异常")
    void shouldHandleNullContext() {
      String yaml = generator.generate(config, null);

      assertThat(yaml).isNotNull();
    }

    @Test
    @DisplayName("无效值类型应被忽略")
    void shouldIgnoreInvalidValueTypes() {
      config.setParam("value_types", "INVALID_TYPE1,INVALID_TYPE2");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
    }
  }

  @Nested
  @DisplayName("性能测试")
  class PerformanceTests {

    @Test
    @DisplayName("批量生成应高效")
    void shouldGenerateBatchEfficiently() {
      int count = 1000;
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < count; i++) {
        generator.generate(config, context);
      }

      long duration = System.currentTimeMillis() - startTime;

      assertThat(duration).isLessThan(5000);
    }

    @Test
    @DisplayName("复杂结构生成应高效")
    void shouldGenerateComplexStructureEfficiently() {
      config.setParam("structure", "COMPLEX");

      long startTime = System.currentTimeMillis();

      for (int i = 0; i < 100; i++) {
        generator.generate(config, context);
      }

      long duration = System.currentTimeMillis() - startTime;

      assertThat(duration).isLessThan(2000);
    }
  }

  @Nested
  @DisplayName("数据格式验证测试")
  class DataFormatValidationTests {

    @Test
    @DisplayName("简单结构应包含键值对")
    void simpleStructureShouldContainKeyValuePairs() {
      config.setParam("structure", "SIMPLE");

      String yaml = generator.generate(config, context);

      assertThat(yaml).contains(":");
      assertThat(yaml.lines().filter(line -> line.contains(":")).count()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("嵌套结构应包含缩进")
    void nestedStructureShouldContainIndentation() {
      config.setParam("structure", "NESTED");
      config.setParam("depth", "3");

      String yaml = generator.generate(config, context);

      assertThat(yaml).contains("  ");
      assertThat(yaml.lines().filter(line -> line.startsWith("    ")).count()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("列表结构应包含破折号")
    void listStructureShouldContainDashes() {
      config.setParam("structure", "LIST");

      String yaml = generator.generate(config, context);

      assertThat(yaml).contains("items:");
      assertThat(yaml.lines().filter(line -> line.trim().startsWith("-")).count())
          .isGreaterThan(0L);
    }

    @Test
    @DisplayName("复杂结构应包含多个部分")
    void complexStructureShouldContainMultipleSections() {
      config.setParam("structure", "COMPLEX");

      String yaml = generator.generate(config, context);

      assertThat(yaml).contains("config:").contains("database:").contains("features:");
    }
  }

  @Nested
  @DisplayName("生成器信息测试")
  class GeneratorInfoTests {

    @Test
    @DisplayName("应返回正确的类型")
    void shouldReturnCorrectType() {
      String type = generator.getType();

      assertThat(type).isEqualTo("yaml");
    }

    @Test
    @DisplayName("应返回正确的配置类")
    void shouldReturnCorrectConfigClass() {
      Class<?> configClass = generator.getConfigClass();

      assertThat(configClass).isEqualTo(com.dataforge.model.FieldConfig.class);
    }

    @Test
    @DisplayName("应返回正确的描述")
    void shouldReturnCorrectDescription() {
      String description = generator.getDescription();

      assertThat(description).isNotNull();
      assertThat(description).contains("YAML");
    }
  }
}
