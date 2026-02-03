package com.dataforge.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ConfigLoader 单元测试。
 *
 * <p>测试命令行参数解析、异常处理和范围验证。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("ConfigLoader 参数解析测试")
class ConfigLoaderTest {

  @Test
  @DisplayName("正常参数解析应成功")
  void testValidArguments() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();

    String[] args = {"--count", "1000", "--threads", "4", "--validate", "true"};
    ForgeConfig result = loader.mergeWithCliArgs(config, args);

    assertEquals(1000, result.getCount(), "Count should be 1000");
    assertEquals(4, result.getThreads(), "Threads should be 4");
    assertTrue(result.isValidate(), "Validate should be true");
  }

  @Test
  @DisplayName("空参数数组应返回原配置")
  void testEmptyArguments() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();
    config.setCount(500);

    String[] args = {};
    ForgeConfig result = loader.mergeWithCliArgs(config, args);

    assertEquals(500, result.getCount(), "Count should remain unchanged");
  }

  @Test
  @DisplayName("null参数数组应返回原配置")
  void testNullArguments() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();
    config.setCount(500);

    ForgeConfig result = loader.mergeWithCliArgs(config, null);

    assertEquals(500, result.getCount(), "Count should remain unchanged");
  }

  @Test
  @DisplayName("无效count值应抛出IllegalArgumentException")
  void testInvalidCountValue() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();

    String[] args = {"--count", "abc"};

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> loader.mergeWithCliArgs(config, args),
            "Should throw IllegalArgumentException for invalid count");

    assertTrue(
        ex.getMessage().contains("Invalid count value"),
        "Error message should mention 'Invalid count value'");
    assertTrue(ex.getMessage().contains("abc"), "Error message should include the invalid value");
    assertTrue(
        ex.getCause() instanceof NumberFormatException, "Cause should be NumberFormatException");
  }

  @Test
  @DisplayName("count超出范围(0)应抛出异常")
  void testCountBelowMinimum() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();

    String[] args = {"--count", "0"};

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> loader.mergeWithCliArgs(config, args),
            "Should throw IllegalArgumentException for count < 1");

    assertTrue(
        ex.getMessage().contains("count must be between"),
        "Error message should mention range validation");
    assertTrue(ex.getMessage().contains("1"), "Error message should show minimum value");
  }

  @Test
  @DisplayName("count超出范围(>1B)应抛出异常")
  void testCountAboveMaximum() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();

    String[] args = {"--count", "1000000001"};

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> loader.mergeWithCliArgs(config, args),
            "Should throw IllegalArgumentException for count > 1B");

    assertTrue(
        ex.getMessage().contains("count must be between"),
        "Error message should mention range validation");
  }

  @Test
  @DisplayName("无效threads值应抛出异常")
  void testInvalidThreadsValue() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();

    String[] args = {"--threads", "xyz"};

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> loader.mergeWithCliArgs(config, args),
            "Should throw IllegalArgumentException for invalid threads");

    assertTrue(
        ex.getMessage().contains("Invalid threads value"),
        "Error message should mention 'Invalid threads value'");
  }

  @Test
  @DisplayName("threads超出范围应抛出异常")
  void testThreadsOutOfRange() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();

    String[] args = {"--threads", "0"};

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> loader.mergeWithCliArgs(config, args),
            "Should throw IllegalArgumentException for threads < 1");

    assertTrue(
        ex.getMessage().contains("threads must be between"),
        "Error message should mention range validation");
  }

  @Test
  @DisplayName("threads最大值65应被拒绝")
  void testThreadsAboveMaximum() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();

    String[] args = {"--threads", "65"};

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> loader.mergeWithCliArgs(config, args),
            "Should throw IllegalArgumentException for threads > 64");

    assertTrue(
        ex.getMessage().contains("threads must be between"),
        "Error message should mention range validation");
  }

  @Test
  @DisplayName("无效seed值应抛出异常")
  void testInvalidSeedValue() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();

    String[] args = {"--seed", "invalid"};

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> loader.mergeWithCliArgs(config, args),
            "Should throw IllegalArgumentException for invalid seed");

    assertTrue(
        ex.getMessage().contains("Invalid seed value"),
        "Error message should mention 'Invalid seed value'");
  }

  @Test
  @DisplayName("正常seed值解析应成功")
  void testValidSeedValue() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();

    String[] args = {"--seed", "12345"};
    ForgeConfig result = loader.mergeWithCliArgs(config, args);

    assertEquals(12345L, result.getSeed(), "Seed should be 12345");
  }

  @Test
  @DisplayName("边界值测试: count=1应通过")
  void testCountBoundaryMinimum() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();

    String[] args = {"--count", "1"};
    ForgeConfig result = loader.mergeWithCliArgs(config, args);

    assertEquals(1, result.getCount(), "Count should be 1");
  }

  @Test
  @DisplayName("边界值测试: count=1000000000应通过")
  void testCountBoundaryMaximum() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();

    String[] args = {"--count", "1000000000"};
    ForgeConfig result = loader.mergeWithCliArgs(config, args);

    assertEquals(1000000000, result.getCount(), "Count should be 1000000000");
  }

  @Test
  @DisplayName("边界值测试: threads=1应通过")
  void testThreadsBoundaryMinimum() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();

    String[] args = {"--threads", "1"};
    ForgeConfig result = loader.mergeWithCliArgs(config, args);

    assertEquals(1, result.getThreads(), "Threads should be 1");
  }

  @Test
  @DisplayName("边界值测试: threads=64应通过")
  void testThreadsBoundaryMaximum() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();

    String[] args = {"--threads", "64"};
    ForgeConfig result = loader.mergeWithCliArgs(config, args);

    assertEquals(64, result.getThreads(), "Threads should be 64");
  }

  @Test
  @DisplayName("短参数名(-c)应正常工作")
  void testShortParameterName() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();

    String[] args = {"-c", "500"};
    ForgeConfig result = loader.mergeWithCliArgs(config, args);

    assertEquals(500, result.getCount(), "Short parameter -c should work");
  }

  @Test
  @DisplayName("多个参数组合应正确解析")
  void testMultipleParameters() {
    ConfigLoader loader = new ConfigLoader();
    ForgeConfig config = new ForgeConfig();

    String[] args = {
      "--count", "10000",
      "--threads", "8",
      "--seed", "999",
      "--validate", "false"
    };
    ForgeConfig result = loader.mergeWithCliArgs(config, args);

    assertEquals(10000, result.getCount(), "Count should be 10000");
    assertEquals(8, result.getThreads(), "Threads should be 8");
    assertEquals(999L, result.getSeed(), "Seed should be 999");
    assertFalse(result.isValidate(), "Validate should be false");
  }
}
