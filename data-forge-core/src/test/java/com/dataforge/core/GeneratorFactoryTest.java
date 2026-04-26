package com.dataforge.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * GeneratorFactory 单元测试。
 *
 * <p>测试工厂的核心功能：初始化、生成器获取、注册、统计等。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("GeneratorFactory 单元测试")
class GeneratorFactoryTest {

  private GeneratorFactory factory;

  @BeforeEach
  void setUp() {
    // 构造函数会自动调用 initialize()
    factory = new GeneratorFactory();
  }

  @Nested
  @DisplayName("初始化测试")
  class InitializationTests {

    @Test
    @DisplayName("构造后应自动初始化并加载生成器")
    void constructor_ShouldAutoInitializeAndLoadGenerators() {
      // Then - 构造函数已自动初始化
      assertThat(factory.isInitialized()).isTrue();
      assertThat(factory.getGeneratorCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("获取初始化统计信息")
    void getInitializationStats_ShouldReturnValidStats() {
      // When
      Map<String, Object> stats = factory.getInitializationStats();

      // Then
      assertThat(stats).isNotNull();
      assertThat(stats).containsKey("initialized");
      assertThat(stats.get("initialized")).isEqualTo(true);
    }

    @Test
    @DisplayName("重新初始化应清除并重新加载")
    void reinitialize_ShouldClearAndReload() {
      // Given
      int originalCount = factory.getGeneratorCount();

      // When
      factory.reinitialize();

      // Then
      assertThat(factory.isInitialized()).isTrue();
      assertThat(factory.getGeneratorCount()).isEqualTo(originalCount);
    }
  }

  @Nested
  @DisplayName("生成器获取测试")
  class GetGeneratorTests {

    @Test
    @DisplayName("根据有效类型获取生成器")
    void getGenerator_WithValidType_ShouldReturnGenerator() {
      // Given
      Set<String> types = factory.getAvailableTypes();
      assertThat(types).isNotEmpty();
      String anyType = types.iterator().next();

      // When
      DataGenerator<?, ?> generator = factory.getGenerator(anyType);

      // Then
      assertThat(generator).isNotNull();
      assertThat(generator.getType()).isEqualTo(anyType);
    }

    @Test
    @DisplayName("根据无效类型获取生成器应返回null")
    void getGenerator_WithInvalidType_ShouldReturnNull() {
      // When
      DataGenerator<?, ?> generator = factory.getGenerator("non-existent-type-xyz");

      // Then
      assertThat(generator).isNull();
    }

    @Test
    @DisplayName("使用null类型应抛出异常")
    void getGenerator_WithNullType_ShouldThrowException() {
      assertThatThrownBy(() -> factory.getGenerator(null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("使用空字符串类型应抛出异常")
    void getGenerator_WithEmptyType_ShouldThrowException() {
      assertThatThrownBy(() -> factory.getGenerator(""))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("hasGenerator 应正确判断生成器存在性")
    void hasGenerator_ShouldReturnCorrectResult() {
      // Given
      Set<String> types = factory.getAvailableTypes();
      String existingType = types.iterator().next();

      // When & Then
      assertThat(factory.hasGenerator(existingType)).isTrue();
      assertThat(factory.hasGenerator("non-existent-type")).isFalse();
    }
  }

  @Nested
  @DisplayName("生成器注册测试")
  class RegisterGeneratorTests {

    @Test
    @DisplayName("注册新生成器应成功")
    @SuppressWarnings("unchecked")
    void registerGenerator_NewGenerator_ShouldSucceed() {
      // Given
      String newType = "test-custom-generator-" + System.currentTimeMillis();
      DataGenerator<?, ? extends FieldConfig> mockGenerator = mock(DataGenerator.class);
      when(mockGenerator.getType()).thenReturn(newType);
      when(mockGenerator.isStateless()).thenReturn(true);

      // When
      assertThatCode(() -> factory.registerGenerator(mockGenerator)).doesNotThrowAnyException();

      // Then
      assertThat(factory.hasGenerator(newType)).isTrue();
      assertThat(factory.getGenerator(newType)).isEqualTo(mockGenerator);
    }

    @Test
    @DisplayName("注册null生成器应抛出异常")
    void registerGenerator_NullGenerator_ShouldThrowException() {
      assertThatThrownBy(() -> factory.registerGenerator(null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("注销生成器应成功")
    @SuppressWarnings("unchecked")
    void unregisterGenerator_ExistingGenerator_ShouldSucceed() {
      // Given
      String customType = "test-unregister-generator-" + System.currentTimeMillis();
      DataGenerator<?, ? extends FieldConfig> mockGenerator = mock(DataGenerator.class);
      when(mockGenerator.getType()).thenReturn(customType);
      when(mockGenerator.isStateless()).thenReturn(true);
      factory.registerGenerator(mockGenerator);
      assertThat(factory.hasGenerator(customType)).isTrue();

      // When
      DataGenerator<?, ?> removed = factory.unregisterGenerator(customType);

      // Then
      assertThat(removed).isEqualTo(mockGenerator);
      assertThat(factory.hasGenerator(customType)).isFalse();
    }

    @Test
    @DisplayName("注销不存在的生成器应返回null")
    void unregisterGenerator_NonExistingGenerator_ShouldReturnNull() {
      // When
      DataGenerator<?, ?> removed = factory.unregisterGenerator("non-existent-type");

      // Then
      assertThat(removed).isNull();
    }
  }

  @Nested
  @DisplayName("统计功能测试")
  class StatisticsTests {

    @Test
    @DisplayName("获取使用统计应返回有效数据")
    void getUsageStatistics_ShouldWork() {
      // Given - 调用几次生成器
      Set<String> types = factory.getAvailableTypes();
      String anyType = types.iterator().next();
      factory.getGenerator(anyType);
      factory.getGenerator(anyType);

      // When
      Map<String, Long> stats = factory.getUsageStatistics();

      // Then
      assertThat(stats).isNotNull();
      assertThat(stats.getOrDefault(anyType, 0L)).isGreaterThanOrEqualTo(2L);
    }

    @Test
    @DisplayName("重置使用统计应清空数据")
    void resetUsageStatistics_ShouldClearStats() {
      // Given
      Set<String> types = factory.getAvailableTypes();
      String anyType = types.iterator().next();
      factory.getGenerator(anyType);

      // When
      factory.resetUsageStatistics();

      // Then
      Map<String, Long> stats = factory.getUsageStatistics();
      assertThat(stats.getOrDefault(anyType, 0L)).isEqualTo(0L);
    }

    @Test
    @DisplayName("获取最受欢迎的生成器类型")
    void getMostUsedGeneratorTypes_ShouldReturnSortedList() {
      // Given
      Set<String> types = factory.getAvailableTypes();
      if (types.size() >= 2) {
        factory.resetUsageStatistics(); // 先重置
        String[] typeArray = types.toArray(new String[0]);
        String type1 = typeArray[0];
        String type2 = typeArray[1];

        // 使用 type1 三次，type2 一次
        factory.getGenerator(type1);
        factory.getGenerator(type1);
        factory.getGenerator(type1);
        factory.getGenerator(type2);

        // When
        var mostUsed = factory.getMostUsedGeneratorTypes(2);

        // Then
        assertThat(mostUsed).hasSize(2);
        assertThat(mostUsed.get(0)).isEqualTo(type1); // 使用最多的排在前面
      }
    }

    @Test
    @DisplayName("获取生成器详细信息")
    void getGeneratorInfo_ShouldReturnDetails() {
      // When
      Map<String, String> info = factory.getGeneratorInfo();

      // Then
      assertThat(info).isNotNull();
      assertThat(info).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("可用类型测试")
  class AvailableTypesTests {

    @Test
    @DisplayName("获取可用类型应返回非空集合")
    void getAvailableTypes_ShouldReturnNonEmptySet() {
      // When
      Set<String> types = factory.getAvailableTypes();

      // Then
      assertThat(types).isNotNull();
      assertThat(types).isNotEmpty();
    }

    @Test
    @DisplayName("获取生成器数量应与类型集合大小一致")
    void getGeneratorCount_ShouldMatchTypesSize() {
      // When
      int count = factory.getGeneratorCount();
      Set<String> types = factory.getAvailableTypes();

      // Then
      assertThat(count).isEqualTo(types.size());
    }
  }

  @Nested
  @DisplayName("健康检查测试")
  class HealthCheckTests {

    @Test
    @DisplayName("健康检查应返回UP状态")
    void healthCheck_ShouldReturnUpStatus() {
      // When
      Map<String, Object> health = factory.healthCheck();

      // Then
      assertThat(health).isNotNull();
      assertThat(health.get("status")).isIn("UP", "DEGRADED");
      assertThat(health).containsKey("generatorCount");
    }
  }
}
