package com.dataforge.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 数据加载服务，提供统一的数据加载功能。
 *
 * <p>支持从类路径加载数据文件，提供 fallback 机制，支持数据解析和转换。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Service
public class DataLoadingService {

  private static final Logger logger = LoggerFactory.getLogger(DataLoadingService.class);

  /**
   * 从类路径加载数据文件。
   *
   * @param filePath 文件路径
   * @param parser 数据行解析器
   * @param <T> 数据类型
   * @return 解析后的数据列表
   */
  public <T> List<T> loadData(String filePath, Function<String, T> parser) {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(filePath)) {
      if (is == null) {
        logger.warn("Data file not found: {}", filePath);
        return List.of();
      }

      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        return reader
            .lines()
            .filter(line -> !line.trim().isEmpty() && !line.startsWith("#"))
            .map(parser)
            .filter(item -> item != null)
            .collect(Collectors.toList());
      }
    } catch (IOException e) {
      logger.error("Failed to load data from: {}", filePath, e);
      return List.of();
    }
  }

  /**
   * 从类路径加载数据文件，带 fallback 机制。
   *
   * @param filePath 文件路径
   * @param parser 数据行解析器
   * @param fallback fallback 数据提供者
   * @param <T> 数据类型
   * @return 解析后的数据列表，加载失败时返回 fallback 数据
   */
  public <T> List<T> loadDataWithFallback(
      String filePath, Function<String, T> parser, Supplier<List<T>> fallback) {
    List<T> data = loadData(filePath, parser);
    if (data.isEmpty()) {
      logger.info("Using fallback data for: {}", filePath);
      return fallback.get();
    }
    return data;
  }

  /**
   * 从类路径加载原始文本行。
   *
   * @param filePath 文件路径
   * @return 文本行列表
   */
  public List<String> loadLines(String filePath) {
    return loadData(filePath, line -> line);
  }

  /**
   * 从类路径加载原始文本行，带 fallback 机制。
   *
   * @param filePath 文件路径
   * @param fallback fallback 数据提供者
   * @return 文本行列表
   */
  public List<String> loadLinesWithFallback(String filePath, Supplier<List<String>> fallback) {
    return loadDataWithFallback(filePath, line -> line, fallback);
  }
}
