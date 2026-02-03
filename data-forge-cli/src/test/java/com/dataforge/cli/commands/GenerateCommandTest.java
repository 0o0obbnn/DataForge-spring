package com.dataforge.cli.commands;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.PrintStream;
import java.nio.file.Path;

@DisplayName("GenerateCommand 测试")
class GenerateCommandTest {

  private GenerateCommand command;
  private CommandLine cmd;
  private StringWriter stringWriter;

  @BeforeEach
  void setUp() {
    command = new GenerateCommand();
    cmd = new CommandLine(command);
    stringWriter = new StringWriter();
    cmd.setOut(new PrintWriter(stringWriter));
  }

  @Nested
  @DisplayName("基本功能测试")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("应成功执行默认命令")
    void shouldExecuteDefaultCommandSuccessfully() throws Exception {
      int exitCode = cmd.execute();

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("DataForge CLI - Generate Command");
    }

    @Test
    @DisplayName("应显示帮助信息")
    void shouldDisplayHelpInformation() {
      int exitCode = cmd.execute("--help");

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Generate test data based on configuration");
      assertThat(output).contains("--config");
      assertThat(output).contains("--count");
      assertThat(output).contains("--output");
    }

    @Test
    @DisplayName("应显示版本信息")
    void shouldDisplayVersionInformation() {
      int exitCode = cmd.execute("--version");

      assertThat(exitCode).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("参数解析测试")
  class ParameterParsingTests {

    @Test
    @DisplayName("应解析配置文件参数")
    void shouldParseConfigFileParameter() throws Exception {
      int exitCode = cmd.execute("-c", "config.yaml");

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Config: config.yaml");
    }

    @Test
    @DisplayName("应解析长格式配置文件参数")
    void shouldParseLongFormatConfigFileParameter() throws Exception {
      int exitCode = cmd.execute("--config", "config.json");

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Config: config.json");
    }

    @Test
    @DisplayName("应解析数量参数")
    void shouldParseCountParameter() throws Exception {
      int exitCode = cmd.execute("-n", "100");

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Count: 100");
    }

    @Test
    @DisplayName("应解析长格式数量参数")
    void shouldParseLongFormatCountParameter() throws Exception {
      int exitCode = cmd.execute("--count", "50");

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Count: 50");
    }

    @Test
    @DisplayName("应解析输出文件参数")
    void shouldParseOutputFileParameter() throws Exception {
      int exitCode = cmd.execute("-o", "output.json");

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Output: output.json");
    }

    @Test
    @DisplayName("应解析长格式输出文件参数")
    void shouldParseLongFormatOutputFileParameter() throws Exception {
      int exitCode = cmd.execute("--output", "output.csv");

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Output: output.csv");
    }

    @Test
    @DisplayName("应解析所有参数组合")
    void shouldParseAllParametersCombination() throws Exception {
      int exitCode =
          cmd.execute("-c", "config.yaml", "-n", "200", "-o", "result.json");

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Config: config.yaml");
      assertThat(output).contains("Count: 200");
      assertThat(output).contains("Output: result.json");
    }
  }

  @Nested
  @DisplayName("默认值测试")
  class DefaultValueTests {

    @Test
    @DisplayName("应使用默认数量值")
    void shouldUseDefaultCountValue() throws Exception {
      int exitCode = cmd.execute();

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Count: 10");
    }

    @Test
    @DisplayName("配置文件和输出文件应默认为null")
    void shouldDefaultConfigAndOutputToNull() throws Exception {
      int exitCode = cmd.execute();

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Config: null");
      assertThat(output).contains("Output: null");
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {

    @Test
    @DisplayName("应处理零数量")
    void shouldHandleZeroCount() throws Exception {
      int exitCode = cmd.execute("-n", "0");

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Count: 0");
    }

    @Test
    @DisplayName("应处理大数量")
    void shouldHandleLargeCount() throws Exception {
      int exitCode = cmd.execute("-n", "1000000");

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Count: 1000000");
    }

    @Test
    @DisplayName("应处理负数量")
    void shouldHandleNegativeCount() throws Exception {
      int exitCode = cmd.execute("-n", "-1");

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Count: -1");
    }

    @Test
    @DisplayName("应处理空配置文件路径")
    void shouldHandleEmptyConfigFilePath() throws Exception {
      int exitCode = cmd.execute("-c", "");

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Config: ");
    }

    @Test
    @DisplayName("应处理空输出文件路径")
    void shouldHandleEmptyOutputFilePath() throws Exception {
      int exitCode = cmd.execute("-o", "");

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Output: ");
    }

    @Test
    @DisplayName("应处理带空格的文件路径")
    void shouldHandleFilePathsWithSpaces() throws Exception {
      int exitCode = cmd.execute("-o", "my output file.json");

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Output: my output file.json");
    }
  }

  @Nested
  @DisplayName("错误处理测试")
  class ErrorHandlingTests {

    @Test
    @DisplayName("应处理无效参数")
    void shouldHandleInvalidParameters() {
      int exitCode = cmd.execute("--invalid-parameter");

      assertThat(exitCode).isNotEqualTo(0);
    }

    @Test
    @DisplayName("应处理非数字数量")
    void shouldHandleNonNumericCount() {
      int exitCode = cmd.execute("-n", "abc");

      assertThat(exitCode).isNotEqualTo(0);
    }

    @Test
    @DisplayName("应处理缺失参数值")
    void shouldHandleMissingParameterValue() {
      int exitCode = cmd.execute("-n");

      assertThat(exitCode).isNotEqualTo(0);
    }
  }

  @Nested
  @DisplayName("命令描述测试")
  class CommandDescriptionTests {

    @Test
    @DisplayName("应有正确的命令名称")
    void shouldHaveCorrectCommandName() {
      assertThat(cmd.getCommandName()).isEqualTo("generate");
    }

    @Test
    @DisplayName("应有正确的命令描述")
    void shouldHaveCorrectCommandDescription() {
      assertThat(cmd.getCommandSpec().usageMessage().description())
          .contains("Generate test data based on configuration.");
    }

    @Test
    @DisplayName("应显示stub实现提示")
    void shouldDisplayStubImplementationNotice() throws Exception {
      int exitCode = cmd.execute();

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Note: This is a stub implementation.");
    }
  }

  @Nested
  @DisplayName("输出格式测试")
  class OutputFormatTests {

    @Test
    @DisplayName("应输出所有参数信息")
    void shouldOutputAllParameterInformation() throws Exception {
      int exitCode =
          cmd.execute("-c", "test.yaml", "-n", "50", "-o", "test.json");

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      assertThat(output).contains("Config: test.yaml");
      assertThat(output).contains("Count: 50");
      assertThat(output).contains("Output: test.json");
    }

    @Test
    @DisplayName("应输出每行独立信息")
    void shouldOutputInformationOnSeparateLines() throws Exception {
      int exitCode = cmd.execute();

      assertThat(exitCode).isEqualTo(0);
      String output = stringWriter.toString();
      String[] lines = output.split("\r?\n");
      assertThat(lines).hasSizeGreaterThan(0);
    }
  }

  @Nested
  @DisplayName("多次执行测试")
  class MultipleExecutionTests {

    @Test
    @DisplayName("应支持多次执行相同命令")
    void shouldSupportMultipleExecutionsOfSameCommand() throws Exception {
      int exitCode1 = cmd.execute("-n", "10");
      int exitCode2 = cmd.execute("-n", "20");

      assertThat(exitCode1).isEqualTo(0);
      assertThat(exitCode2).isEqualTo(0);
    }

    @Test
    @DisplayName("应支持多次执行不同命令")
    void shouldSupportMultipleExecutionsOfDifferentCommands() throws Exception {
      int exitCode1 = cmd.execute("-c", "config1.yaml");
      int exitCode2 = cmd.execute("-c", "config2.yaml", "-n", "50");

      assertThat(exitCode1).isEqualTo(0);
      assertThat(exitCode2).isEqualTo(0);
    }
  }
}
