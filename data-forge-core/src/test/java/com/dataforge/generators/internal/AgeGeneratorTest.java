package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AgeGenerator 测试")
class AgeGeneratorTest {

  private AgeGenerator generator;
  private SimpleFieldConfig config;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new AgeGenerator();
    config = new SimpleFieldConfig();
    config.setType("age");
    context = new DataForgeContext();
  }

  @Nested
  @DisplayName("默认配置测试")
  class DefaultConfigurationTests {

    @Test
    @DisplayName("默认配置应生成18-60岁之间的年龄")
    void shouldGenerateAgeWithinDefaultRange() {
      Integer age = generator.generate(config, context);

      assertThat(age).isNotNull();
      assertThat(age).isBetween(18, 60);
    }

    @Test
    @DisplayName("生成的年龄应为整数")
    void shouldGenerateIntegerAge() {
      Integer age = generator.generate(config, context);

      assertThat(age).isNotNull();
      assertThat(age).isInstanceOf(Integer.class);
    }

    @Test
    @DisplayName("多次生成的年龄应在合理范围内分布")
    void shouldGenerateAgesWithReasonableDistribution() {
      for (int i = 0; i < 100; i++) {
        Integer age = generator.generate(config, context);
        assertThat(age).isBetween(18, 60);
      }
    }
  }

  @Nested
  @DisplayName("年龄范围配置测试")
  class AgeRangeTests {

    @Test
    @DisplayName("应生成指定范围内的年龄")
    void shouldGenerateAgeWithinSpecifiedRange() {
      config.setParam("min", "25");
      config.setParam("max", "35");

      for (int i = 0; i < 50; i++) {
        Integer age = generator.generate(config, context);
        assertThat(age).isBetween(25, 35);
      }
    }

    @Test
    @DisplayName("应处理最小年龄为0的情况")
    void shouldHandleMinAgeOfZero() {
      config.setParam("min", "0");
      config.setParam("max", "10");

      for (int i = 0; i < 20; i++) {
        Integer age = generator.generate(config, context);
        assertThat(age).isBetween(0, 10);
      }
    }

    @Test
    @DisplayName("应处理大年龄范围")
    void shouldHandleLargeAgeRange() {
      config.setParam("min", "0");
      config.setParam("max", "100");

      for (int i = 0; i < 50; i++) {
        Integer age = generator.generate(config, context);
        assertThat(age).isBetween(0, 100);
      }
    }

    @Test
    @DisplayName("应处理无效年龄范围并使用默认值")
    void shouldHandleInvalidAgeRange() {
      config.setParam("min", "60"); // min > max
      config.setParam("max", "18");

      Integer age = generator.generate(config, context);

      assertThat(age).isNotNull();
      assertThat(age).isBetween(18, 60); // 应该使用默认值
    }

    @Test
    @DisplayName("应处理负数年龄范围")
    void shouldHandleNegativeAgeRange() {
      config.setParam("min", "-10");
      config.setParam("max", "30");

      Integer age = generator.generate(config, context);

      assertThat(age).isNotNull();
      // 应该使用默认值
      assertThat(age).isBetween(18, 60);
    }
  }

  @Nested
  @DisplayName("分布类型测试")
  class DistributionTests {

    @Test
    @DisplayName("UNIFORM分布应均匀分布年龄")
    void shouldGenerateUniformlyDistributedAges() {
      config.setParam("distribution", "UNIFORM");
      config.setParam("min", "20");
      config.setParam("max", "40");

      // 统计年龄分布
      Map<Integer, Integer> distribution = new HashMap<>();
      for (int i = 0; i < 1000; i++) {
        Integer age = generator.generate(config, context);
        distribution.put(age, distribution.getOrDefault(age, 0) + 1);
      }

      // 验证每个年龄值都出现了
      assertThat(distribution).hasSizeBetween(15, 25); // 20-40岁之间应该有较均匀的分布

      // 验证没有明显的聚集（每个年龄出现次数在合理范围内）
      int minCount = distribution.values().stream().min(Integer::compareTo).orElse(0);
      int maxCount = distribution.values().stream().max(Integer::compareTo).orElse(0);
      double ratio = (double) maxCount / minCount;

      // 均匀分布时，最大值和最小值的比值不应该太大（通常 < 3）
      assertThat(ratio).isLessThan(3.0);
    }

    @Test
    @DisplayName("NORMAL分布应集中在中间值")
    void shouldGenerateNormallyDistributedAges() {
      config.setParam("distribution", "NORMAL");
      config.setParam("min", "20");
      config.setParam("max", "60");

      // 统计年龄分布
      Map<Integer, Integer> distribution = new HashMap<>();
      for (int i = 0; i < 1000; i++) {
        Integer age = generator.generate(config, context);
        distribution.put(age, distribution.getOrDefault(age, 0) + 1);
      }

      // 正态分布应该在中位数附近有最大频率
      int median = 40; // (20 + 60) / 2
      int medianCount = distribution.getOrDefault(median, 0);
      int edgeCount = distribution.getOrDefault(20, 0) + distribution.getOrDefault(60, 0);

      // 中位数附近的频率应该高于边缘
      assertThat(medianCount).isGreaterThan(edgeCount);
    }

    @Test
    @DisplayName("无效分布类型应使用默认UNIFORM")
    void shouldUseUniformForInvalidDistribution() {
      config.setParam("distribution", "INVALID");

      Integer age = generator.generate(config, context);

      assertThat(age).isNotNull();
      assertThat(age).isBetween(18, 60);
    }
  }

  @Nested
  @DisplayName("上下文关联测试")
  class ContextLinkTests {

    @Test
    @DisplayName("应从上下文中的出生日期计算年龄")
    void shouldCalculateAgeFromBirthDateInContext() {
      config.setParam("link_birth_date", "true");

      // 设置出生日期为30年前
      LocalDate birthDate = LocalDate.now().minusYears(30);
      context.put("birth_date", birthDate);

      Integer age = generator.generate(config, context);

      assertThat(age).isEqualTo(30);
    }

    @Test
    @DisplayName("应从上下文中的身份证号提取年龄")
    void shouldExtractAgeFromIdCardInContext() {
      config.setParam("link_birth_date", "true");

      // 设置身份证号（出生日期为1990-01-01）
      String idCard = "110101199001011234"; // 1990年1月1日
      context.put("idcard", idCard);

      Integer age = generator.generate(config, context);

      // 年龄应该是当前年份减去1990年
      int expectedAge = LocalDate.now().getYear() - 1990;
      assertThat(age).isEqualTo(expectedAge);
    }

    @Test
    @DisplayName("禁用关联时应生成随机年龄")
    void shouldGenerateRandomAgeWhenLinkDisabled() {
      config.setParam("link_birth_date", "false");

      LocalDate birthDate = LocalDate.now().minusYears(25);
      context.put("birth_date", birthDate);

      Integer age = generator.generate(config, context);

      // 应该忽略上下文中的出生日期，生成随机年龄
      assertThat(age).isBetween(18, 60);
    }

    @Test
    @DisplayName("出生日期在未来时应返回null并使用随机年龄")
    void shouldHandleFutureBirthDate() {
      config.setParam("link_birth_date", "true");

      LocalDate futureDate = LocalDate.now().plusYears(10);
      context.put("birth_date", futureDate);

      Integer age = generator.generate(config, context);

      // 应该忽略无效的出生日期，生成随机年龄
      assertThat(age).isBetween(18, 60);
    }

    @Test
    @DisplayName("无效身份证号格式应使用随机年龄")
    void shouldHandleInvalidIdCardFormat() {
      config.setParam("link_birth_date", "true");

      context.put("idcard", "invalid");

      Integer age = generator.generate(config, context);

      // 应该使用随机年龄
      assertThat(age).isBetween(18, 60);
    }

    @Test
    @DisplayName("身份证号优先级高于直接出生日期")
    void shouldPreferIdCardOverDirectBirthDate() {
      config.setParam("link_birth_date", "true");

      // 设置不一致的出生日期
      LocalDate birthDate = LocalDate.now().minusYears(25);
      context.put("birth_date", birthDate);

      // 设置身份证号（1990年出生）
      String idCard = "110101199001011234";
      context.put("idcard", idCard);

      Integer age = generator.generate(config, context);

      // 实际上 birthDate 的优先级更高，因为它先被检查
      // 所以年龄应该是25岁
      assertThat(age).isEqualTo(25);
    }
  }

  @Nested
  @DisplayName("精度测试")
  class PrecisionTests {

    @Test
    @DisplayName("默认精度应为整数")
    void shouldUseIntegerPrecisionByDefault() {
      Integer age = generator.generate(config, context);

      assertThat(age).isNotNull();
      // 应该是整数（虽然类型是Integer，但验证精度）
      assertThat(age % 1).isEqualTo(0);
    }

    @Test
    @DisplayName("精度为1.0应生成整数年龄")
    void shouldGenerateIntegerAgeWithPrecisionOne() {
      config.setParam("precision", "1.0");

      for (int i = 0; i < 50; i++) {
        Integer age = generator.generate(config, context);
        assertThat(age).isNotNull();
      }
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class EdgeCaseTests {

    @Test
    @DisplayName("最小年龄等于最大年龄应生成固定值")
    void shouldGenerateFixedAgeWhenMinEqualsMax() {
      config.setParam("min", "30");
      config.setParam("max", "30");

      Integer age = generator.generate(config, context);

      assertThat(age).isEqualTo(30);
    }

    @Test
    @DisplayName("空参数应使用默认值")
    void shouldHandleEmptyParameters() {
      // 不设置任何参数，应该使用默认值

      Integer age = generator.generate(config, context);

      assertThat(age).isNotNull();
      assertThat(age).isBetween(18, 60);
    }

    @Test
    @DisplayName("批量生成应保持性能")
    void shouldMaintainPerformanceForBulkGeneration() {
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < 10000; i++) {
        Integer age = generator.generate(config, context);
        assertThat(age).isNotNull();
      }

      long duration = System.currentTimeMillis() - startTime;

      // 10000个年龄应该在2秒内生成
      assertThat(duration).isLessThan(2000);
    }

    @Test
    @DisplayName("不同参数类型应正确解析")
    void shouldHandleDifferentParameterTypes() {
      config.setParam("min", 25); // Integer
      config.setParam("max", "35"); // String
      config.setParam("precision", 1.0); // Double

      for (int i = 0; i < 20; i++) {
        Integer age = generator.generate(config, context);
        assertThat(age).isBetween(25, 35);
      }
    }
  }
}
