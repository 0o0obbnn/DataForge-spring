package com.dataforge.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dataforge.api.context.DataForgeContext;
import com.dataforge.api.generator.DataGenerator;
import com.dataforge.api.model.FieldConfig;
import com.dataforge.api.model.SimpleFieldConfig;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SimpleDataForgeService 全面测试
 *
 * <p>基于TEST_OPTIMIZATION_PLAN.md中的设计，提供完整的API服务测试覆盖</p>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SimpleDataForgeService 全面测试")
class SimpleDataForgeServiceComprehensiveTest {
    
    @Mock
    private GeneratorFactory generatorFactory;
    
    @Mock
    private DataGenerator<Object, FieldConfig> mockGenerator;
    
    @Mock
    private DataForgeContext mockContext;
    
    private SimpleDataForgeService service;
    
    @BeforeEach
    void setUp() {
        service = new SimpleDataForgeService(generatorFactory);
    }
    
    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {
        
        @Test
        @DisplayName("应使用工厂创建服务")
        void shouldCreateServiceWithFactory() {
            SimpleDataForgeService serviceWithFactory = new SimpleDataForgeService(generatorFactory);
            
            assertThat(serviceWithFactory).isNotNull();
            assertThat(serviceWithFactory.getGeneratorFactory()).isEqualTo(generatorFactory);
        }
        
        @Test
        @DisplayName("应使用默认工厂创建服务（向后兼容性测试）")
        @SuppressWarnings("deprecation") // 测试已弃用的 SimpleGeneratorFactory 向后兼容性
        void shouldCreateServiceWithDefaultFactory() {
            // 注意：此测试仅用于验证向后兼容性，新代码应使用 core.GeneratorFactory
            SimpleDataForgeService serviceWithDefault = new SimpleDataForgeService(new SimpleGeneratorFactory());
            
            assertThat(serviceWithDefault).isNotNull();
            assertThat(serviceWithDefault.getGeneratorFactory()).isNotNull();
        }
    }
    
    @Nested
    @DisplayName("单次生成测试")
    class SingleGenerationTests {
        
        @Test
        @DisplayName("应使用工厂生成数据")
        void shouldUseFactoryToGenerateData() {
            FieldConfig config = new SimpleFieldConfig("testField", "test");
            when(generatorFactory.getGenerator("test"))
                .thenReturn(Optional.of(mockGenerator));
            when(mockGenerator.generate(any(), any()))
                .thenReturn("generated");
            
            Object result = service.generate(config);
            
            assertThat(result).isEqualTo("generated");
            verify(mockGenerator).generate(any(), any());
        }
        
        @Test
        @DisplayName("应使用自定义上下文生成数据")
        void shouldUseCustomContextToGenerateData() {
            FieldConfig config = new SimpleFieldConfig("testField", "test");
            when(generatorFactory.getGenerator("test"))
                .thenReturn(Optional.of(mockGenerator));
            when(mockGenerator.generate(any(), any()))
                .thenReturn("generated");
            
            Object result = service.generate(config, mockContext);
            
            assertThat(result).isEqualTo("generated");
            verify(mockGenerator).generate(config, mockContext);
        }
        
        @Test
        @DisplayName("不支持的类型应抛出异常")
        void shouldThrowExceptionForUnsupportedType() {
            FieldConfig config = new SimpleFieldConfig("testField", "unsupported");
            when(generatorFactory.getGenerator("unsupported"))
                .thenReturn(Optional.empty());
            
            assertThatThrownBy(() -> service.generate(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported");
        }
        
        @Test
        @DisplayName("空配置应抛出异常")
        void shouldThrowExceptionForNullConfig() {
            assertThatThrownBy(() -> service.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("config cannot be null");
        }
        
        @Test
        @DisplayName("空类型配置应抛出异常")
        void shouldThrowExceptionForNullType() {
            FieldConfig config = new SimpleFieldConfig("testField", null);
            
            assertThatThrownBy(() -> service.generate(config))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
    
    @Nested
    @DisplayName("批量生成测试")
    class BatchGenerationTests {
        
        @Test
        @DisplayName("批量生成应返回正确数量")
        void shouldGenerateCorrectBatchSize() {
            List<FieldConfig> configs = Arrays.asList(
                createConfig("field1", "string"),
                createConfig("field2", "number")
            );
            
            when(generatorFactory.getGenerator("string"))
                .thenReturn(Optional.of(mockGenerator));
            when(generatorFactory.getGenerator("number"))
                .thenReturn(Optional.of(mockGenerator));
            when(mockGenerator.generate(any(), any()))
                .thenReturn("value");
            
            List<Map<String, Object>> results = service.generateBatch(configs, 100);
            
            assertThat(results).hasSize(100);
            results.forEach(row -> {
                assertThat(row).containsKeys("field1", "field2");
                assertThat(row.get("field1")).isEqualTo("value");
                assertThat(row.get("field2")).isEqualTo("value");
            });
        }
        
        @Test
        @DisplayName("应处理空配置列表")
        void shouldHandleEmptyConfigList() {
            List<FieldConfig> configs = List.of();
            
            List<Map<String, Object>> results = service.generateBatch(configs, 10);
            
            assertThat(results).hasSize(10);
            results.forEach(row -> {
                assertThat(row).isEmpty();
            });
        }
        
        @Test
        @DisplayName("应处理零批量大小")
        void shouldHandleZeroBatchSize() {
            List<FieldConfig> configs = Arrays.asList(
                createConfig("field1", "string")
            );
            
            List<Map<String, Object>> results = service.generateBatch(configs, 0);
            
            assertThat(results).isEmpty();
        }
        
        @Test
        @DisplayName("应处理部分不支持的类型")
        void shouldHandlePartialUnsupportedTypes() {
            List<FieldConfig> configs = Arrays.asList(
                createConfig("field1", "supported"),
                createConfig("field2", "unsupported")
            );
            
            when(generatorFactory.getGenerator("supported"))
                .thenReturn(Optional.of(mockGenerator));
            when(generatorFactory.getGenerator("unsupported"))
                .thenReturn(Optional.empty());
            when(mockGenerator.generate(any(), any()))
                .thenReturn("value");
            
            assertThatThrownBy(() -> service.generateBatch(configs, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported");
        }
        
        @Test
        @DisplayName("应为每行创建独立上下文")
        void shouldCreateIndependentContextForEachRow() {
            List<FieldConfig> configs = Arrays.asList(
                createConfig("field1", "string")
            );
            
            when(generatorFactory.getGenerator("string"))
                .thenReturn(Optional.of(mockGenerator));
            when(mockGenerator.generate(any(), any()))
                .thenReturn("value");
            
            List<Map<String, Object>> results = service.generateBatch(configs, 5);
            
            assertThat(results).hasSize(5);
            // 验证为每行调用了生成器
            verify(mockGenerator, times(5)).generate(any(), any());
        }
    }
    
    @Nested
    @DisplayName("生成器工厂测试")
    class GeneratorFactoryTests {
        
        @Test
        @DisplayName("应返回正确的生成器工厂")
        void shouldReturnCorrectGeneratorFactory() {
            GeneratorFactory factory = service.getGeneratorFactory();
            
            assertThat(factory).isEqualTo(generatorFactory);
        }
        
        @Test
        @DisplayName("应支持多个生成器类型")
        void shouldSupportMultipleGeneratorTypes() {
            FieldConfig config1 = createConfig("field1", "type1");
            FieldConfig config2 = createConfig("field2", "type2");
            
            when(generatorFactory.getGenerator("type1"))
                .thenReturn(Optional.of(mockGenerator));
            when(generatorFactory.getGenerator("type2"))
                .thenReturn(Optional.of(mockGenerator));
            when(mockGenerator.generate(any(), any()))
                .thenReturn("value1")
                .thenReturn("value2");
            
            Object result1 = service.generate(config1);
            Object result2 = service.generate(config2);
            
            assertThat(result1).isEqualTo("value1");
            assertThat(result2).isEqualTo("value2");
            verify(generatorFactory).getGenerator("type1");
            verify(generatorFactory).getGenerator("type2");
        }
    }
    
    @Nested
    @DisplayName("上下文管理测试")
    class ContextManagementTests {
        
        @Test
        @DisplayName("应创建新的上下文实例")
        void shouldCreateNewContextInstance() {
            DataForgeContext context1 = service.createContext();
            DataForgeContext context2 = service.createContext();
            
            assertThat(context1).isNotNull();
            assertThat(context2).isNotNull();
            assertThat(context1).isNotSameAs(context2);
        }
        
        @Test
        @DisplayName("应为每次生成创建新上下文")
        void shouldCreateNewContextForEachGeneration() {
            FieldConfig config = createConfig("field", "type");
            
            when(generatorFactory.getGenerator("type"))
                .thenReturn(Optional.of(mockGenerator));
            when(mockGenerator.generate(any(), any()))
                .thenReturn("value");
            
            Object result1 = service.generate(config);
            Object result2 = service.generate(config);
            
            assertThat(result1).isEqualTo("value");
            assertThat(result2).isEqualTo("value");
            // 验证为每次生成调用了生成器
            verify(mockGenerator, times(2)).generate(any(), any());
        }
    }
    
    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTests {
        
        @Test
        @DisplayName("应处理大量配置")
        void shouldHandleLargeNumberOfConfigs() {
            List<FieldConfig> configs = Arrays.asList(
                createConfig("field1", "type1"),
                createConfig("field2", "type2"),
                createConfig("field3", "type3")
            );
            
            when(generatorFactory.getGenerator(any()))
                .thenReturn(Optional.of(mockGenerator));
            when(mockGenerator.generate(any(), any()))
                .thenReturn("value");
            
            List<Map<String, Object>> results = service.generateBatch(configs, 1000);
            
            assertThat(results).hasSize(1000);
            assertThat(results).allSatisfy(row -> {
                assertThat(row).hasSize(3);
            });
        }
        
        @Test
        @DisplayName("应处理大量批量大小")
        void shouldHandleLargeBatchSize() {
            List<FieldConfig> configs = Arrays.asList(
                createConfig("field", "type")
            );
            
            when(generatorFactory.getGenerator("type"))
                .thenReturn(Optional.of(mockGenerator));
            when(mockGenerator.generate(any(), any()))
                .thenReturn("value");
            
            List<Map<String, Object>> results = service.generateBatch(configs, 10000);
            
            assertThat(results).hasSize(10000);
        }
        
        @Test
        @DisplayName("应处理重复字段名")
        void shouldHandleDuplicateFieldNames() {
            List<FieldConfig> configs = Arrays.asList(
                createConfig("field", "type1"),
                createConfig("field", "type2")  // 重复字段名
            );
            
            when(generatorFactory.getGenerator("type1"))
                .thenReturn(Optional.of(mockGenerator));
            when(generatorFactory.getGenerator("type2"))
                .thenReturn(Optional.of(mockGenerator));
            when(mockGenerator.generate(any(), any()))
                .thenReturn("value1")
                .thenReturn("value2");
            
            List<Map<String, Object>> results = service.generateBatch(configs, 1);
            
            assertThat(results).hasSize(1);
            Map<String, Object> row = results.get(0);
            // 后一个值会覆盖前一个值
            assertThat(row.get("field")).isEqualTo("value2");
        }
    }
    
    /**
     * 创建测试配置
     */
    private FieldConfig createConfig(String name, String type) {
        return new SimpleFieldConfig(name, type);
    }
}