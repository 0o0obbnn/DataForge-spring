package com.dataforge.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dataforge.config.OutputConfig;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
 * ConsoleOutputStrategy 单元测试。
 *
 * <p>测试控制台输出策略的核心功能：表头输出、数据行输出、格式化等。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("ConsoleOutputStrategy 单元测试")
class ConsoleOutputStrategyTest {

  private ConsoleOutputStrategy strategy;
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;

  @BeforeEach
  void setUp() {
    strategy = new ConsoleOutputStrategy();
    // 重定向 System.out 以捕获输出
    System.setOut(new PrintStream(outputStream));
  }

  @AfterEach
  void tearDown() {
    // 恢复原始 System.out
    System.setOut(originalOut);
    outputStream.reset();
  }

  @Nested
  @DisplayName("初始化测试")
  class InitializationTests {

    @Test
    @DisplayName("成功初始化")
    void initialize_ShouldSucceed() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CONSOLE);
      List<String> fieldNames = List.of("id", "name", "age");

      // When
      strategy.initialize(config, fieldNames);

      // Then
      assertThat(strategy.getSupportedFormat()).isEqualTo(OutputConfig.Format.CONSOLE);
    }

    @Test
    @DisplayName("空字段列表应抛出异常")
    void initialize_WithEmptyFieldNames_ShouldThrowException() {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CONSOLE);
      List<String> fieldNames = new ArrayList<>();

      // When & Then
      assertThatThrownBy(() -> strategy.initialize(config, fieldNames))
          .isInstanceOf(OutputException.class)
          .hasMessageContaining("Field names cannot be null or empty");
    }

    @Test
    @DisplayName("null字段列表应抛出异常")
    void initialize_WithNullFieldNames_ShouldThrowException() {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CONSOLE);

      // When & Then
      assertThatThrownBy(() -> strategy.initialize(config, null))
          .isInstanceOf(OutputException.class)
          .hasMessageContaining("Field names cannot be null or empty");
    }
  }

  @Nested
  @DisplayName("数据输出测试")
  class DataOutputTests {

    @Test
    @DisplayName("输出单条记录应包含表头")
    void writeRecord_ShouldIncludeHeader() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CONSOLE);
      List<String> fieldNames = List.of("id", "name");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("id", 1);
      record.put("name", "Alice");

      // When
      strategy.writeRecord(record);

      // Then
      String output = outputStream.toString();
      assertThat(output).contains("id");
      assertThat(output).contains("name");
      assertThat(output).contains("1");
      assertThat(output).contains("Alice");
    }

    @Test
    @DisplayName("输出多条记录应只显示一次表头")
    void writeRecords_ShouldShowHeaderOnce() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CONSOLE);
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

      // Then
      String output = outputStream.toString();
      // 计算表头出现的次数（应该只有一次）
      long headerCount = output.lines().filter(line -> line.contains("id")).count();
      assertThat(headerCount).isEqualTo(1);
    }

    @Test
    @DisplayName("null值应显示为null")
    void writeRecord_WithNullValue_ShouldDisplayNull() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CONSOLE);
      List<String> fieldNames = List.of("id", "name");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("id", 1);
      record.put("name", null);

      // When
      strategy.writeRecord(record);

      // Then
      String output = outputStream.toString();
      assertThat(output).contains("null");
    }

    @Test
    @DisplayName("特殊字符应正确转义")
    void writeRecord_WithSpecialCharacters_ShouldEscapeCorrectly() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CONSOLE);
      List<String> fieldNames = List.of("text");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("text", "Hi\nBye"); // 使用更短的文本避免截断

      // When
      strategy.writeRecord(record);

      // Then
      String output = outputStream.toString();
      // 换行符应被替换为空格
      assertThat(output).contains("Hi Bye");
      // 输出中不应有原始的换行符（列内不应有换行）
      assertThat(output.lines().count()).isGreaterThan(1); // 表头、分隔线、数据行、分隔线
    }

    @Test
    @DisplayName("超长内容应被截断")
    void writeRecord_WithLongContent_ShouldTruncate() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CONSOLE);
      List<String> fieldNames = List.of("text");
      strategy.initialize(config, fieldNames);

      String longText =
          "This is a very long text that exceeds the default column width of 15 characters";
      Map<String, Object> record = new HashMap<>();
      record.put("text", longText);

      // When
      strategy.writeRecord(record);

      // Then
      String output = outputStream.toString();
      // 内容应该被截断并添加省略号
      assertThat(output).contains("...");
    }

    @Test
    @DisplayName("null记录应抛出异常")
    void writeRecord_WithNullRecord_ShouldThrowException() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CONSOLE);
      List<String> fieldNames = List.of("id", "name");
      strategy.initialize(config, fieldNames);

      // When & Then
      assertThatThrownBy(() -> strategy.writeRecord(null))
          .isInstanceOf(OutputException.class)
          .hasMessageContaining("Record cannot be null");
    }
  }

  @Nested
  @DisplayName("完成输出测试")
  class FinishTests {

    @Test
    @DisplayName("finish应显示记录总数")
    void finish_ShouldShowTotalRecords() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CONSOLE);
      List<String> fieldNames = List.of("id");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("id", 1);
      strategy.writeRecord(record);

      // When
      strategy.finish();

      // Then
      String output = outputStream.toString();
      assertThat(output).contains("Total records: 1");
    }

    @Test
    @DisplayName("无记录时finish应显示无记录消息")
    void finish_WithNoRecords_ShouldShowNoRecordsMessage() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CONSOLE);
      List<String> fieldNames = List.of("id");
      strategy.initialize(config, fieldNames);

      // When
      strategy.finish();

      // Then
      String output = outputStream.toString();
      assertThat(output).contains("No records to display");
    }

    @Test
    @DisplayName("finish应显示底部分隔线")
    void finish_ShouldShowBottomSeparator() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CONSOLE);
      List<String> fieldNames = List.of("id");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("id", 1);
      strategy.writeRecord(record);

      // When
      strategy.finish();

      // Then
      String output = outputStream.toString();
      // 应该有分隔线（包含"-"字符）
      assertThat(output).contains("-");
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
      config.setFormat(OutputConfig.Format.CONSOLE);
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
      config.setFormat(OutputConfig.Format.CONSOLE);
      List<String> fieldNames = List.of("id");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("id", 1);
      strategy.writeRecord(record);

      // When & Then - 不应抛出异常
      strategy.flush();
    }

    @Test
    @DisplayName("getDescription应返回描述")
    void getDescription_ShouldReturnDescription() {
      // When
      String description = strategy.getDescription();

      // Then
      assertThat(description).isNotNull();
      assertThat(description).contains("Console output strategy");
    }
  }

  @Nested
  @DisplayName("格式化测试")
  class FormattingTests {

    @Test
    @DisplayName("布尔值应正确显示")
    void writeRecord_WithBooleanValue_ShouldDisplayCorrectly() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CONSOLE);
      List<String> fieldNames = List.of("active");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("active", true);

      // When
      strategy.writeRecord(record);

      // Then
      String output = outputStream.toString();
      assertThat(output).contains("true");
    }

    @Test
    @DisplayName("数字应正确显示")
    void writeRecord_WithNumericValue_ShouldDisplayCorrectly() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CONSOLE);
      List<String> fieldNames = List.of("price", "quantity");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("price", 19.99);
      record.put("quantity", 42);

      // When
      strategy.writeRecord(record);

      // Then
      String output = outputStream.toString();
      assertThat(output).contains("19.99");
      assertThat(output).contains("42");
    }

    @Test
    @DisplayName("日期应正确显示")
    void writeRecord_WithDateValue_ShouldDisplayCorrectly() throws Exception {
      // Given
      OutputConfig config = new OutputConfig();
      config.setFormat(OutputConfig.Format.CONSOLE);
      List<String> fieldNames = List.of("createdDate");
      strategy.initialize(config, fieldNames);

      Map<String, Object> record = new HashMap<>();
      record.put("createdDate", java.time.LocalDate.of(2024, 1, 1));

      // When
      strategy.writeRecord(record);

      // Then
      String output = outputStream.toString();
      assertThat(output).contains("2024");
    }
  }
}
