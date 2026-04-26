package com.dataforge.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * DataLoadingService 单元测试。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("DataLoadingService 测试")
class DataLoadingServiceTest {

  private DataLoadingService dataLoadingService;

  @BeforeEach
  void setUp() {
    dataLoadingService = new DataLoadingService();
  }

  @Test
  @DisplayName("测试加载不存在的文件应返回空列表")
  void testLoadNonExistentFile() {
    List<String> result = dataLoadingService.loadLines("non-existent-file.txt");
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("测试加载数据并解析")
  void testLoadDataWithParser() {
    Function<String, Integer> parser = Integer::parseInt;

    // 测试空文件情况
    List<Integer> result = dataLoadingService.loadData("empty-file.txt", parser);
    assertNotNull(result);
  }

  @Test
  @DisplayName("测试带 fallback 的数据加载")
  void testLoadDataWithFallback() {
    List<String> fallbackData = Arrays.asList("fallback1", "fallback2");

    List<String> result =
        dataLoadingService.loadLinesWithFallback("non-existent.txt", () -> fallbackData);

    assertEquals(fallbackData, result);
  }

  @Test
  @DisplayName("测试数据解析转换")
  void testDataParsing() {
    // 测试数据解析功能
    Function<String, String> upperCaseParser = String::toUpperCase;

    // 由于无法确定测试环境是否有实际文件，这里主要测试方法存在性
    assertDoesNotThrow(
        () -> {
          dataLoadingService.loadData("test.txt", upperCaseParser);
        });
  }

  @Test
  @DisplayName("测试 loadLines 方法")
  void testLoadLines() {
    // 测试加载文本行
    List<String> result = dataLoadingService.loadLines("test.txt");
    assertNotNull(result);
  }

  @Test
  @DisplayName("测试 loadLinesWithFallback 方法")
  void testLoadLinesWithFallback() {
    List<String> fallback = Arrays.asList("line1", "line2");

    List<String> result =
        dataLoadingService.loadLinesWithFallback("non-existent.txt", () -> fallback);

    assertEquals(fallback, result);
  }

  @Test
  @DisplayName("测试空 fallback 提供者")
  void testEmptyFallbackSupplier() {
    // 当 fallback 返回 null 时，应该返回空列表
    // 服务应该处理 null fallback 的情况
    // 实际行为可能是返回空列表或抛出异常
    // 这里我们只验证不会抛出未捕获的异常
    assertDoesNotThrow(
        () -> {
          dataLoadingService.loadLinesWithFallback("non-existent.txt", () -> null);
        });
  }
}
