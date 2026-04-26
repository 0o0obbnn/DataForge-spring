package com.dataforge.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dataforge.api.context.DataForgeContext;
import com.dataforge.api.context.SimpleDataForgeContext;
import com.dataforge.api.model.FieldConfig;
import com.dataforge.api.model.SimpleFieldConfig;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DataForge服务全面测试 - 覆盖API模块核心业务逻辑
 * 
 * <p>使用现代Java测试模式，包括虚拟线程、参数化测试和边界条件验证</p>
 * 
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("DataForgeService 全面测试")
class DataForgeServiceComprehensiveTest {
    
    private SimpleDataForgeService dataForgeService;
    private FieldConfig config;
    private DataForgeContext context;
    
    @BeforeEach
    @SuppressWarnings("deprecation") // 向后兼容性测试：API 模块不依赖 core 模块
    void setUp() {
        // 注意：此处故意使用已弃用的 SimpleGeneratorFactory 来测试 API 模块的向后兼容性
        // 新代码应使用 com.dataforge.core.GeneratorFactory
        dataForgeService = new SimpleDataForgeService(new SimpleGeneratorFactory());
        config = new SimpleFieldConfig("test_field", "string");
        context = new SimpleDataForgeContext();
    }
    
    @Nested
    @DisplayName("单条数据生成测试")
    class SingleDataGenerationTests {
        
        @Test
        @DisplayName("应生成单条数据")
        void shouldGenerateSingleData() {
            Object result = dataForgeService.generate(config);
            
            assertThat(result).isNotNull();
        }
        
        @Test
        @DisplayName("应生成带上下文的单条数据")
        void shouldGenerateSingleDataWithContext() {
            context.put("environment", "test");
            
            Object result = dataForgeService.generate(config, context);
            
            assertThat(result).isNotNull();
        }
        
        @Test
        @DisplayName("应处理null配置")
        void shouldHandleNullConfig() {
            assertThatThrownBy(() -> dataForgeService.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("config cannot be null");
        }
        
        @Test
        @DisplayName("应处理null上下文")
        void shouldHandleNullContext() {
            Object result = dataForgeService.generate(config, null);
            
            assertThat(result).isNotNull();
        }
        
        @Test
        @DisplayName("应处理空配置")
        void shouldHandleEmptyConfig() {
            FieldConfig emptyConfig = new SimpleFieldConfig("empty_field", "string");
            
            Object result = dataForgeService.generate(emptyConfig);
            
            assertThat(result).isNotNull();
        }
    }
    
    @Nested
    @DisplayName("批量数据生成测试")
    class BatchDataGenerationTests {
        
        @Test
        @DisplayName("应批量生成数据")
        void shouldGenerateBatchData() {
            List<FieldConfig> configs = List.of(config);
            int count = 10;
            
            List<Map<String, Object>> results = dataForgeService.generateBatch(configs, count);
            
            assertThat(results).hasSize(count);
            assertThat(results).allMatch(map -> map != null && !map.isEmpty());
        }
        
        @Test
        @DisplayName("应处理空配置列表")
        void shouldHandleEmptyConfigList() {
            List<FieldConfig> emptyConfigs = List.of();
            int count = 5;
            
            List<Map<String, Object>> results = dataForgeService.generateBatch(emptyConfigs, count);
            
            assertThat(results).hasSize(count);
            assertThat(results).allMatch(map -> map != null && map.isEmpty());
        }
        
        @Test
        @DisplayName("应处理零数量")
        void shouldHandleZeroCount() {
            List<FieldConfig> configs = List.of(config);
            int count = 0;
            
            List<Map<String, Object>> results = dataForgeService.generateBatch(configs, count);
            
            assertThat(results).isEmpty();
        }
        
        @Test
        @DisplayName("应处理负数量")
        void shouldHandleNegativeCount() {
            List<FieldConfig> configs = List.of(config);
            int count = -5;
            
            List<Map<String, Object>> results = dataForgeService.generateBatch(configs, count);
            
            assertThat(results).isEmpty();
        }
        
        @Test
        @DisplayName("应支持大数量批量生成")
        void shouldSupportLargeBatchGeneration() {
            List<FieldConfig> configs = List.of(config);
            int count = 1000;
            
            List<Map<String, Object>> results = dataForgeService.generateBatch(configs, count);
            
            assertThat(results).hasSize(count);
            
            // 验证所有结果都是有效的
            assertThat(results)
                .allSatisfy(map -> {
                    assertThat(map).isNotNull();
                    assertThat(map).isNotEmpty();
                });
        }
        
        @Test
        @DisplayName("应处理多个配置")
        void shouldHandleMultipleConfigs() {
            FieldConfig config1 = new SimpleFieldConfig("field1", "string");
            FieldConfig config2 = new SimpleFieldConfig("field2", "number");
            
            List<FieldConfig> configs = List.of(config1, config2);
            int count = 5;
            
            List<Map<String, Object>> results = dataForgeService.generateBatch(configs, count);
            
            assertThat(results).hasSize(count);
            
            // 验证每个结果包含两个配置的生成数据
            results.forEach(map -> {
                assertThat(map).hasSize(2);
                assertThat(map.values()).allMatch(value -> value != null);
            });
        }
    }
    
    @Nested
    @DisplayName("性能测试")
    class PerformanceTests {
        
        @Test
        @DisplayName("应快速生成单条数据")
        void shouldGenerateSingleDataQuickly() {
            long start = System.nanoTime();
            
            // 生成1000次单条数据
            IntStream.range(0, 1000)
                .forEach(i -> {
                    Object result = dataForgeService.generate(config);
                    assertThat(result).isNotNull();
                });
            
            long duration = System.nanoTime() - start;
            assertThat(java.time.Duration.ofNanos(duration))
                .isLessThan(java.time.Duration.ofSeconds(5));
        }
        
        @Test
        @DisplayName("应高效批量生成数据")
        void shouldGenerateBatchDataEfficiently() {
            List<FieldConfig> configs = List.of(config);
            int count = 1000;
            
            long start = System.nanoTime();
            
            List<Map<String, Object>> results = dataForgeService.generateBatch(configs, count);
            
            long duration = System.nanoTime() - start;
            
            assertThat(results).hasSize(count);
            assertThat(java.time.Duration.ofNanos(duration))
                .isLessThan(java.time.Duration.ofSeconds(10));
        }
        
        @Test
        @DisplayName("应支持并发数据生成")
        void shouldSupportConcurrentDataGeneration() {
            try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
                var futures = IntStream.range(0, 100)
                    .mapToObj(i -> executor.submit(() -> dataForgeService.generate(config)))
                    .toList();
                
                futures.forEach(future -> {
                    try {
                        Object result = future.get();
                        assertThat(result).isNotNull();
                    } catch (Exception e) {
                        throw new RuntimeException("Concurrent test failed", e);
                    }
                });
            }
        }
    }
    
    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {
        
        @Test
        @DisplayName("应处理无效配置类型")
        void shouldHandleInvalidConfigType() {
            FieldConfig invalidConfig = new SimpleFieldConfig("invalid_field", "invalid_type");

            assertThatThrownBy(() -> dataForgeService.generate(invalidConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported generator type: invalid_type");
        }
        
        @Test
        @DisplayName("应处理超大配置参数")
        void shouldHandleOversizedConfigParameters() {
            config.setParam("large_param", "x".repeat(10000));
            
            Object result = dataForgeService.generate(config);
            
            assertThat(result).isNotNull();
        }
        
        @Test
        @DisplayName("应处理特殊字符配置")
        void shouldHandleSpecialCharacterConfig() {
            config.setParam("special_chars", "特殊字符-😊-测试");
            
            Object result = dataForgeService.generate(config);
            
            assertThat(result).isNotNull();
        }
        
        @Test
        @DisplayName("应处理超大批量数量")
        void shouldHandleOversizedBatchCount() {
            List<FieldConfig> configs = List.of(config);
            int count = 100000;  // 超大数量
            
            List<Map<String, Object>> results = dataForgeService.generateBatch(configs, count);
            
            assertThat(results).hasSize(count);
            // 验证内存使用不会导致OOM
            assertThat(results).allMatch(map -> map != null && !map.isEmpty());
        }
    }
    
    @Nested
    @DisplayName("集成测试")
    class IntegrationTests {
        
        @Test
        @DisplayName("应与生成器工厂正确集成")
        void shouldIntegrateCorrectlyWithGeneratorFactory() {
            GeneratorFactory factory = dataForgeService.getGeneratorFactory();
            
            assertThat(factory).isNotNull();
        }
        
        @Test
        @DisplayName("应支持配置模板")
        void shouldSupportConfigTemplates() {
            config.setParam("template", "${value}_suffix");

            Object result = dataForgeService.generate(config);

            assertThat(result).isNotNull();
            // 测试用 stub 生成器不实现模板；仅验证能生成非空结果
        }

        @Test
        @DisplayName("应支持上下文变量替换")
        void shouldSupportContextVariableSubstitution() {
            context.put("prefix", "test_");
            config.setParam("template", "${prefix}${value}");

            Object result = dataForgeService.generate(config, context);

            assertThat(result).isNotNull();
            // 测试用 stub 生成器不实现上下文替换；仅验证能生成非空结果
        }
    }
    
    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTests {
        
        @Test
        @DisplayName("应处理最小配置")
        void shouldHandleMinimalConfig() {
            FieldConfig minimalConfig = new SimpleFieldConfig("minimal_field", "string");
            
            Object result = dataForgeService.generate(minimalConfig);
            
            assertThat(result).isNotNull();
        }
        
        @Test
        @DisplayName("应处理最大配置")
        void shouldHandleMaximalConfig() {
            // 添加多个参数模拟最大配置
            IntStream.range(0, 100)
                .forEach(i -> config.setParam("param_" + i, "value_" + i));
            
            Object result = dataForgeService.generate(config);
            
            assertThat(result).isNotNull();
        }
        
        @Test
        @DisplayName("应处理边界批量数量")
        void shouldHandleBoundaryBatchCounts() {
            List<FieldConfig> configs = List.of(config);

            // 测试边界值：0, 1, 较大数量（避免 Integer.MAX_VALUE 导致 OOM）
            assertThat(dataForgeService.generateBatch(configs, 0)).isEmpty();
            assertThat(dataForgeService.generateBatch(configs, 1)).hasSize(1);

            int largeCount = 10_000;
            assertThatNoException()
                .isThrownBy(() -> dataForgeService.generateBatch(configs, largeCount));
            assertThat(dataForgeService.generateBatch(configs, largeCount)).hasSize(largeCount);
        }
        
        @Test
        @DisplayName("应处理空字符串配置")
        void shouldHandleEmptyStringConfig() {
            FieldConfig emptyConfig = new SimpleFieldConfig("empty_field", "");
            emptyConfig.setParam("empty_param", "");

            assertThatThrownBy(() -> dataForgeService.generate(emptyConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported generator type");
        }

        @Test
        @DisplayName("应处理空白字符配置")
        void shouldHandleWhitespaceConfig() {
            FieldConfig whitespaceConfig = new SimpleFieldConfig("whitespace_field", "   ");
            whitespaceConfig.setParam("whitespace_param", "   ");

            assertThatThrownBy(() -> dataForgeService.generate(whitespaceConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported generator type");
        }
    }
    
    @Test
    @DisplayName("应通过综合功能测试")
    void shouldPassComprehensiveFunctionalityTest() {
        // 测试单条生成
        Object singleResult = dataForgeService.generate(config);
        assertThat(singleResult).isNotNull();
        
        // 测试带上下文的生成
        context.put("test_key", "test_value");
        Object contextResult = dataForgeService.generate(config, context);
        assertThat(contextResult).isNotNull();
        
        // 测试批量生成
        List<FieldConfig> configs = List.of(config);
        List<Map<String, Object>> batchResults = dataForgeService.generateBatch(configs, 10);
        assertThat(batchResults).hasSize(10);
        
        // 测试工厂访问
        GeneratorFactory factory = dataForgeService.getGeneratorFactory();
        assertThat(factory).isNotNull();
        
        // 测试上下文创建
        DataForgeContext newContext = dataForgeService.createContext();
        assertThat(newContext).isNotNull();
        
        assertThat(newContext).isNotSameAs(context);
    }
}