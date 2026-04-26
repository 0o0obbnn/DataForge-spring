package com.dataforge.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据加载工具类。
 *
 * <p>用于从资源文件或外部文件加载数据，支持缓存和权重解析。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class DataLoader {

  private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

  /** 数据缓存，避免重复加载。 */
  private static final Map<String, List<String>> DATA_CACHE = new ConcurrentHashMap<>();

  /** 原始数据缓存（不解析权重）。 */
  private static final Map<String, List<String>> RAW_DATA_CACHE = new ConcurrentHashMap<>();

  /** 权重数据缓存。 */
  private static final Map<String, Map<String, Integer>> WEIGHT_CACHE = new ConcurrentHashMap<>();

  /**
   * 从资源文件加载原始行数据（不解析权重）。
   *
   * @param resourcePath 资源文件路径
   * @return 数据列表，保持原始行格式
   */
  public static List<String> loadRawDataFromResource(String resourcePath) {
    return RAW_DATA_CACHE.computeIfAbsent(
        resourcePath,
        path -> {
          List<String> data = new ArrayList<>();

          try (InputStream inputStream =
                  DataLoader.class.getClassLoader().getResourceAsStream(path);
              BufferedReader reader =
                  new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            if (inputStream == null) {
              logger.warn("Resource not found: {}", path);
              return data;
            }

            String line;
            while ((line = reader.readLine()) != null) {
              line = line.trim();

              // 跳过空行和注释行
              if (line.isEmpty() || line.startsWith("#")) {
                continue;
              }

              // 保持原始行格式，不解析权重
              data.add(line);
            }

            logger.debug("Loaded {} items from resource: {}", data.size(), path);

          } catch (IOException e) {
            logger.error("Failed to load data from resource: {}", path, e);
          }

          return data;
        });
  }

  /**
   * 从资源文件加载数据列表。
   *
   * @param resourcePath 资源文件路径
   * @return 数据列表
   */
  public static List<String> loadDataFromResource(String resourcePath) {
    return DATA_CACHE.computeIfAbsent(
        resourcePath,
        path -> {
          List<String> data = new ArrayList<>();

          try (InputStream inputStream =
                  DataLoader.class.getClassLoader().getResourceAsStream(path);
              BufferedReader reader =
                  new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            if (inputStream == null) {
              logger.warn("Resource not found: {}", path);
              return data;
            }

            String line;
            while ((line = reader.readLine()) != null) {
              line = line.trim();

              // 跳过空行和注释行
              if (line.isEmpty() || line.startsWith("#")) {
                continue;
              }

              // 解析权重格式：item:weight 或 item
              String item = parseItem(line);
              if (!item.isEmpty()) {
                data.add(item);
              }
            }

            logger.debug("Loaded {} items from resource: {}", data.size(), path);

          } catch (IOException e) {
            logger.error("Failed to load data from resource: {}", path, e);
          }

          return data;
        });
  }

  /**
   * 从资源文件加载带权重的数据。
   *
   * @param resourcePath 资源文件路径
   * @return 数据权重映射
   */
  public static Map<String, Integer> loadWeightedDataFromResource(String resourcePath) {
    return WEIGHT_CACHE.computeIfAbsent(
        resourcePath,
        path -> {
          Map<String, Integer> weightedData = new HashMap<>();

          try (InputStream inputStream =
                  DataLoader.class.getClassLoader().getResourceAsStream(path);
              BufferedReader reader =
                  new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            if (inputStream == null) {
              logger.warn("Resource not found: {}", path);
              return weightedData;
            }

            String line;
            while ((line = reader.readLine()) != null) {
              line = line.trim();

              // 跳过空行和注释行
              if (line.isEmpty() || line.startsWith("#")) {
                continue;
              }

              // 解析权重格式：item:weight 或 item
              String[] parts = line.split(":");
              String item = parts[0].trim();
              int weight = 1; // 默认权重

              if (parts.length > 1) {
                try {
                  weight = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                  logger.warn("Invalid weight format in line: {}, using default weight 1", line);
                }
              }

              if (!item.isEmpty()) {
                weightedData.put(item, weight);
              }
            }

            logger.debug("Loaded {} weighted items from resource: {}", weightedData.size(), path);

          } catch (IOException e) {
            logger.error("Failed to load weighted data from resource: {}", path, e);
          }

          return weightedData;
        });
  }

  /**
   * 根据权重随机选择数据项。
   *
   * @param weightedData 权重数据映射
   * @param random 随机数生成器
   * @return 选中的数据项
   */
  public static String selectByWeight(Map<String, Integer> weightedData, java.util.Random random) {
    if (weightedData.isEmpty()) {
      return null;
    }

    // 计算总权重
    int totalWeight = weightedData.values().stream().mapToInt(Integer::intValue).sum();

    // 生成随机数
    int randomValue = random.nextInt(totalWeight);

    // 根据权重选择
    int currentWeight = 0;
    for (Map.Entry<String, Integer> entry : weightedData.entrySet()) {
      currentWeight += entry.getValue();
      if (randomValue < currentWeight) {
        return entry.getKey();
      }
    }

    // 理论上不应该到达这里，返回第一个元素作为fallback
    return weightedData.keySet().iterator().next();
  }

  /**
   * 从外部文件加载数据列表。
   *
   * @param filePath 文件路径
   * @return 数据列表
   */
  public static List<String> loadDataFromFile(String filePath) {
    List<String> data = new ArrayList<>();

    try (BufferedReader reader =
        java.nio.file.Files.newBufferedReader(
            java.nio.file.Paths.get(filePath), StandardCharsets.UTF_8)) {

      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();

        // 跳过空行和注释行
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }

        String item = parseItem(line);
        if (!item.isEmpty()) {
          data.add(item);
        }
      }

      logger.debug("Loaded {} items from file: {}", data.size(), filePath);

    } catch (IOException e) {
      logger.error("Failed to load data from file: {}", filePath, e);
    }

    return data;
  }

  /**
   * 解析数据项，去除权重部分。
   *
   * @param line 原始行
   * @return 数据项
   */
  private static String parseItem(String line) {
    String[] parts = line.split(":");
    return parts[0].trim();
  }

  /** 清除缓存。 */
  public static void clearCache() {
    DATA_CACHE.clear();
    RAW_DATA_CACHE.clear();
    WEIGHT_CACHE.clear();
    logger.debug("Data cache cleared");
  }

  /**
   * 获取缓存统计信息。
   *
   * @return 缓存统计信息
   */
  public static String getCacheStats() {
    return String.format(
        "Data cache: %d entries, Weight cache: %d entries", DATA_CACHE.size(), WEIGHT_CACHE.size());
  }
}
