package com.dataforge.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dataforge.config.OutputConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ImprovedFileOutputStrategy CSV 输出测试。
 *
 * <p>测试文件输出策略的 CSV 基本功能。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("ImprovedFileOutputStrategy CSV 输出测试")
class ImprovedFileOutputStrategyCsvTest {

  private ImprovedFileOutputStrategy strategy;
  private Path outputDir;
  private int testCounter = 0;

  @BeforeEach
  void setUp() throws IOException {
    strategy = new ImprovedFileOutputStrategy();
    // 创建 output 目录
    outputDir = Path.of("output");
    if (!Files.exists(outputDir)) {
      Files.createDirectories(outputDir);
    }
  }

  @AfterEach
  void tearDown() throws IOException {
    // 清理创建的测试文件
    if (outputDir != null && Files.exists(outputDir)) {
      Files.walk(outputDir)
          .filter(Files::isRegularFile)
          .sorted((a, b) -> -a.compareTo(b))
          .forEach(
              path -> {
                try {
                  Files.delete(path);
                } catch (IOException e) {
                  // 忽略删除错误
                }
              });
    }
  }

  private String getTestFileName() {
    return "test_" + (++testCounter) + ".csv";
  }

  @Nested
  @DisplayName("初始化测试")
  class InitializationTests {

    @Test
    @DisplayName("成功初始化")
    void initialize_ShouldSucceed() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile("output/" + getTestFileName());
      List<String> fieldNames = List.of("id", "name", "age");

      // When
      strategy.initialize(config, fieldNames);

      // Then
      assertThat(strategy.getSupportedFormat()).isEqualTo(OutputConfig.Format.CSV);
      strategy.finish();
    }

    @Test
    @DisplayName("空字段列表应成功创建仅包含换行符的文件")
    void initialize_WithEmptyFieldNames_ShouldCreateEmptyFile() throws Exception {
      // Given
      String fileName = getTestFileName();
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile("output/" + fileName);
      List<String> fieldNames = List.of();

      // When
      strategy.initialize(config, fieldNames);
      strategy.finish();

      // Then
      // 空字段列表应该创建一个仅包含换行符的文件
      assertThat(Files.exists(outputDir.resolve(fileName))).isTrue();
      String content = Files.readString(outputDir.resolve(fileName));
      assertThat(content).isEqualTo("\n");
    }
  }

  @Nested
  @DisplayName("数据输出测试")
  class DataOutputTests {

    @Test
    @DisplayName("输出单条记录应成功")
    void writeRecord_ShouldSucceed() throws Exception {
      // Given
      String fileName = getTestFileName();
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile("output/" + fileName);
      List<String> fieldNames = List.of("id", "name", "age");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("id", 1);
      record.put("name", "Alice");
      record.put("age", 30);

      // When
      strategy.writeRecord(record);
      strategy.finish();

      // Then
      String content = Files.readString(outputDir.resolve(fileName));
      assertThat(content).contains("id,name,age");
      assertThat(content).contains("1,Alice,30");
    }

    @Test
    @DisplayName("输出多条记录应成功")
    void writeRecords_ShouldSucceed() throws Exception {
      // Given
      String fileName = getTestFileName();
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile("output/" + fileName);
      List<String> fieldNames = List.of("id", "name");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record1 = new HashMap<>();
      record1.put("id", 1);
      record1.put("name", "Alice");

      Map<String, Object> record2 = new HashMap<>();
      record2.put("id", 2);
      record2.put("name", "Bob");

      // When
      strategy.writeRecord(record1);
      strategy.writeRecord(record2);
      strategy.finish();

      // Then
      String content = Files.readString(outputDir.resolve(fileName));
      assertThat(content).contains("1,Alice");
      assertThat(content).contains("2,Bob");
    }

    @Test
    @DisplayName("null值应显示为空")
    void writeRecord_WithNullValue_ShouldDisplayEmpty() throws Exception {
      // Given
      String fileName = getTestFileName();
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile("output/" + fileName);
      List<String> fieldNames = List.of("id", "name");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("id", 1);
      record.put("name", null);

      // When
      strategy.writeRecord(record);
      strategy.finish();

      // Then
      String content = Files.readString(outputDir.resolve(fileName));
      // null值应该显示为空
      assertThat(content).contains("1,");
    }

    @Test
    @DisplayName("包含逗号的值应被引号包裹")
    void writeRecord_WithCommaInValue_ShouldQuoteValue() throws Exception {
      // Given
      String fileName = getTestFileName();
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile("output/" + fileName);
      List<String> fieldNames = List.of("name");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("name", "Doe, John");

      // When
      strategy.writeRecord(record);
      strategy.finish();

      // Then
      String content = Files.readString(outputDir.resolve(fileName));
      // 包含逗号的值应该被引号包裹
      assertThat(content).contains("\"Doe, John\"");
    }

    @Test
    @DisplayName("包含换行符的值应被引号包裹")
    void writeRecord_WithNewlineInValue_ShouldQuoteValue() throws Exception {
      // Given
      String fileName = getTestFileName();
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile("output/" + fileName);
      List<String> fieldNames = List.of("address");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("address", "123 Main St\nNew York");

      // When
      strategy.writeRecord(record);
      strategy.finish();

      // Then
      String content = Files.readString(outputDir.resolve(fileName));
      // 包含换行符的值应该被引号包裹
      assertThat(content).contains("\"");
    }

    @Test
    @DisplayName("null记录应抛出异常")
    void writeRecord_WithNullRecord_ShouldThrowException() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile("output/" + getTestFileName());
      List<String> fieldNames = List.of("id", "name");
      strategy.initialize(config, fieldNames);

      // When & Then
      assertThatThrownBy(() -> strategy.writeRecord(null))
          .isInstanceOf(Exception.class); // NullPointerException 是 RuntimeException 的子类
    }
  }

  @Nested
  @DisplayName("支持验证测试")
  class SupportTests {

    @Test
    @DisplayName("CSV格式配置应被支持")
    void supports_WithCsvFormat_ShouldReturnTrue() {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile("output/" + getTestFileName());

      // When & Then
      assertThat(strategy.supports(config)).isTrue();
    }

    @Test
    @DisplayName("JSON格式配置应被支持")
    void supports_WithJsonFormat_ShouldReturnTrue() {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.JSON);
      config.setFile("output/test.json");

      // When & Then
      assertThat(strategy.supports(config)).isTrue();
    }

    @Test
    @DisplayName("CONSOLE格式配置不应被支持")
    void supports_WithConsoleFormat_ShouldReturnFalse() {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CONSOLE);
      config.setFile("output/test.csv");

      // When & Then
      assertThat(strategy.supports(config)).isFalse();
    }

    @Test
    @DisplayName("无文件路径不应被支持")
    void supports_WithoutFile_ShouldReturnFalse() {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      // 不设置文件路径

      // When & Then
      assertThat(strategy.supports(config)).isFalse();
    }
  }
}
