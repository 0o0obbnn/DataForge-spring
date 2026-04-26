package com.dataforge.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dataforge.config.OutputConfig;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CsvOutputStrategy 单元测试。
 *
 * <p>测试 CSV 输出策略的核心功能：文件写入、分隔符处理、标题行、编码等。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("CsvOutputStrategy 单元测试")
class CsvOutputStrategyTest {

  private CsvOutputStrategy strategy;
  private Path tempDir;

  @BeforeEach
  void setUp() throws IOException {
    strategy = new CsvOutputStrategy();
    tempDir = Files.createTempDirectory("csv-test");
  }

  @AfterEach
  void tearDown() throws IOException {
    // 清理临时文件
    if (tempDir != null && Files.exists(tempDir)) {
      Files.walk(tempDir)
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

  @Nested
  @DisplayName("初始化测试")
  class InitializationTests {

    @Test
    @DisplayName("成功初始化")
    void initialize_ShouldSucceed() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      List<String> fieldNames = List.of("id", "name", "age");

      // When
      strategy.initialize(config, fieldNames);

      // Then
      assertThat(strategy.getSupportedFormat()).isEqualTo(OutputConfig.Format.CSV);
    }

    @Test
    @DisplayName("空字段列表应抛出异常")
    void initialize_WithEmptyFieldNames_ShouldThrowException() {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      List<String> fieldNames = new ArrayList<>();

      // When & Then
      assertThatThrownBy(() -> strategy.initialize(config, fieldNames))
          .isInstanceOf(OutputException.class);
    }

    @Test
    @DisplayName("自定义分隔符应生效")
    void initialize_WithCustomDelimiter_ShouldApplyDelimiter() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      config.setCsvDelimiter(";");
      List<String> fieldNames = List.of("id", "name");

      // When
      strategy.initialize(config, fieldNames);

      // Then
      // 分隔符应该被正确设置（通过写入验证）
      Map<String, Object> record = new HashMap<>();
      record.put("id", 1);
      record.put("name", "Alice");
      strategy.writeRecord(record);
      strategy.finish();

      String content = Files.readString(tempDir.resolve("test.csv"));
      assertThat(content).contains(";");
    }
  }

  @Nested
  @DisplayName("数据输出测试")
  class DataOutputTests {

    @Test
    @DisplayName("输出单条记录应成功")
    void writeRecord_ShouldSucceed() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      config.setCsvIncludeHeader(false);
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
      String content = Files.readString(tempDir.resolve("test.csv"));
      assertThat(content).contains("1,Alice,30");
    }

    @Test
    @DisplayName("输出多条记录应成功")
    void writeRecords_ShouldSucceed() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      config.setCsvIncludeHeader(false);
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
      String content = Files.readString(tempDir.resolve("test.csv"));
      assertThat(content).contains("1,Alice");
      assertThat(content).contains("2,Bob");
    }

    @Test
    @DisplayName("null值应显示为空")
    void writeRecord_WithNullValue_ShouldDisplayEmpty() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      config.setCsvIncludeHeader(false);
      List<String> fieldNames = List.of("id", "name");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("id", 1);
      record.put("name", null);

      // When
      strategy.writeRecord(record);
      strategy.finish();

      // Then
      String content = Files.readString(tempDir.resolve("test.csv"));
      // null值应该显示为空
      assertThat(content).contains("1,");
    }

    @Test
    @DisplayName("包含逗号的值应被引号包裹")
    void writeRecord_WithCommaInValue_ShouldQuoteValue() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      config.setCsvIncludeHeader(false);
      List<String> fieldNames = List.of("name");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("name", "Doe, John");

      // When
      strategy.writeRecord(record);
      strategy.finish();

      // Then
      String content = Files.readString(tempDir.resolve("test.csv"));
      // 包含逗号的值应该被引号包裹
      assertThat(content).contains("\"Doe, John\"");
    }

    @Test
    @DisplayName("包含换行符的值应被引号包裹")
    void writeRecord_WithNewlineInValue_ShouldQuoteValue() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      config.setCsvIncludeHeader(false);
      List<String> fieldNames = List.of("address");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("address", "123 Main St\nNew York");

      // When
      strategy.writeRecord(record);
      strategy.finish();

      // Then
      String content = Files.readString(tempDir.resolve("test.csv"));
      // 包含换行符的值应该被引号包裹
      assertThat(content).contains("\"");
    }

    @Test
    @DisplayName("null记录应抛出异常")
    void writeRecord_WithNullRecord_ShouldThrowException() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      List<String> fieldNames = List.of("id", "name");
      strategy.initialize(config, fieldNames);

      // When & Then
      assertThatThrownBy(() -> strategy.writeRecord(null)).isInstanceOf(OutputException.class);
    }
  }

  @Nested
  @DisplayName("标题行测试")
  class HeaderTests {

    @Test
    @DisplayName("启用标题行应输出字段名")
    void writeRecord_WithHeaderEnabled_ShouldIncludeFieldNames() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      config.setCsvIncludeHeader(true);
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
      String content = Files.readString(tempDir.resolve("test.csv"));
      // 第一行应该是标题行
      String[] lines = content.split("\n");
      assertThat(lines[0]).contains("id,name,age");
    }

    @Test
    @DisplayName("禁用标题行不应输出字段名")
    void writeRecord_WithHeaderDisabled_ShouldNotIncludeFieldNames() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      config.setCsvIncludeHeader(false);
      List<String> fieldNames = List.of("id", "name");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("id", 1);
      record.put("name", "Alice");

      // When
      strategy.writeRecord(record);
      strategy.finish();

      // Then
      String content = Files.readString(tempDir.resolve("test.csv"));
      // 不应包含字段名
      assertThat(content).doesNotContain("id,name");
      assertThat(content).contains("1,Alice");
    }
  }

  @Nested
  @DisplayName("安全模式测试")
  class SafeModeTests {

    @Test
    @DisplayName("启用安全模式应防护CSV注入")
    void writeRecord_WithSafeModeEnabled_ShouldPreventInjection() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      config.setCsvIncludeHeader(false);
      config.setCsvSafeMode(true);
      List<String> fieldNames = List.of("formula");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("formula", "=SUM(A1:B2)");

      // When
      strategy.writeRecord(record);
      strategy.finish();

      // Then
      String content = Files.readString(tempDir.resolve("test.csv"));
      // 在安全模式下，以 = 开头的值应该被转义
      assertThat(content).contains("'=SUM(A1:B2)");
    }

    @Test
    @DisplayName("禁用安全模式不应转义")
    void writeRecord_WithSafeModeDisabled_ShouldNotEscape() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      config.setCsvIncludeHeader(false);
      config.setCsvSafeMode(false);
      List<String> fieldNames = List.of("formula");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("formula", "=SUM(A1:B2)");

      // When
      strategy.writeRecord(record);
      strategy.finish();

      // Then
      String content = Files.readString(tempDir.resolve("test.csv"));
      // 在非安全模式下，值应该保持原样
      assertThat(content).contains("=SUM(A1:B2)");
    }
  }

  @Nested
  @DisplayName("编码测试")
  class EncodingTests {

    @Test
    @DisplayName("UTF-8编码应正确处理中文")
    void writeRecord_WithUtf8Encoding_ShouldHandleChinese() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      config.setEncoding("UTF-8");
      config.setCsvIncludeHeader(false);
      List<String> fieldNames = List.of("name");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("name", "张三");

      // When
      strategy.writeRecord(record);
      strategy.finish();

      // Then
      String content = Files.readString(tempDir.resolve("test.csv"));
      assertThat(content).contains("张三");
    }

    @Test
    @DisplayName("GBK编码应正确处理中文")
    void writeRecord_WithGbkEncoding_ShouldHandleChinese() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      config.setEncoding("GBK");
      config.setCsvIncludeHeader(false);
      List<String> fieldNames = List.of("name");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("name", "张三");

      // When
      strategy.writeRecord(record);
      strategy.finish();

      // Then
      String content = Files.readString(tempDir.resolve("test.csv"), Charset.forName("GBK"));
      assertThat(content).contains("张三");
    }
  }

  @Nested
  @DisplayName("追加模式测试")
  class AppendModeTests {

    @Test
    @DisplayName("启用追加模式应保留原有内容")
    void writeRecord_WithAppendMode_ShouldPreserveExistingContent() throws Exception {
      // Given
      Path filePath = tempDir.resolve("test.csv");

      // 写入初始内容
      Files.writeString(filePath, "id,name\n1,Alice\n");

      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(filePath.toString());
      config.setAppend(true);
      config.setCsvIncludeHeader(false);
      List<String> fieldNames = List.of("id", "name");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("id", 2);
      record.put("name", "Bob");

      // When
      strategy.writeRecord(record);
      strategy.finish();

      // Then
      String content = Files.readString(filePath);
      assertThat(content).contains("1,Alice");
      assertThat(content).contains("2,Bob");
    }
  }

  @Nested
  @DisplayName("辅助方法测试")
  class UtilityTests {

    @Test
    @DisplayName("getWrittenRecordCount应返回正确数量")
    void getWrittenRecordCount_ShouldReturnCorrectCount() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      config.setCsvIncludeHeader(false);
      List<String> fieldNames = List.of("id");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("id", 1);

      // When
      strategy.writeRecord(record);
      strategy.writeRecord(record);
      strategy.writeRecord(record);

      // Then
      assertThat(strategy.getWrittenRecordCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("flush应刷新输出流")
    void flush_ShouldFlushOutputStream() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      List<String> fieldNames = List.of("id");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("id", 1);
      strategy.writeRecord(record);

      // When & Then - 不应抛出异常
      strategy.flush();
    }
  }

  @Nested
  @DisplayName("错误处理测试")
  class ErrorHandlingTests {

    @Test
    @DisplayName("无效编码应抛出异常")
    void initialize_WithInvalidEncoding_ShouldThrowException() {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CSV);
      config.setFile(tempDir.resolve("test.csv").toString());
      config.setEncoding("INVALID_ENCODING");
      List<String> fieldNames = List.of("id");

      // When & Then
      // CsvOutputStrategy 可能抛出 OutputException 或 UnsupportedCharsetException
      // 关键是验证它会抛出异常
      assertThatThrownBy(() -> strategy.initialize(config, fieldNames))
          .isInstanceOfAny(
              OutputException.class, java.nio.charset.UnsupportedCharsetException.class);
    }
  }
}
