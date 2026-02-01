package com.dataforge.generators.internal;

import com.dataforge.service.DataLoadingService;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 支持数据加载的生成器基类。
 *
 * <p>提供统一的数据加载机制，支持延迟加载、fallback 数据和线程安全。 子类只需实现 {@link #getDataFilePath()} 和 {@link
 * #parseData(List)} 方法。
 *
 * @author DataForge Team
 * @since 1.0.0
 * @param <T> 生成器返回的数据类型
 */
public abstract class BaseDataLoadingGenerator<T> extends BaseGenerator {

  private static final Logger logger = LoggerFactory.getLogger(BaseDataLoadingGenerator.class);

  @Autowired protected DataLoadingService dataLoadingService;

  /** 数据加载状态。 */
  protected volatile boolean dataLoaded = false;

  /** 数据加载锁。 */
  protected final Object dataLoadLock = new Object();

  /**
   * 获取数据文件路径。
   *
   * @return 数据文件路径（相对于类路径）
   */
  protected abstract String getDataFilePath();

  /**
   * 解析加载的数据。
   *
   * @param lines 数据行列表
   */
  protected abstract void parseData(List<String> lines);

  /** 初始化 fallback 数据（当文件加载失败时调用）。 */
  protected abstract void initializeFallbackData();

  /** 确保数据已加载（线程安全的延迟加载）。 */
  protected void ensureDataLoaded() {
    if (!dataLoaded) {
      synchronized (dataLoadLock) {
        if (!dataLoaded) {
          loadDataInternal();
          dataLoaded = true;
        }
      }
    }
  }

  /** 内部数据加载方法。 */
  protected void loadDataInternal() {
    String filePath = getDataFilePath();
    List<String> lines =
        dataLoadingService.loadLinesWithFallback(
            filePath,
            () -> {
              logger.warn("Failed to load data from: {}, using fallback", filePath);
              initializeFallbackData();
              return List.of();
            });

    if (!lines.isEmpty()) {
      parseData(lines);
      logger.info("Successfully loaded {} lines from: {}", lines.size(), filePath);
    }
  }

  /**
   * 使用自定义解析器加载数据。
   *
   * @param <R> 解析后的数据类型
   * @param parser 数据行解析器
   * @return 解析后的数据列表
   */
  protected <R> List<R> loadDataWithParser(Function<String, R> parser) {
    return dataLoadingService.loadData(getDataFilePath(), parser);
  }

  /**
   * 使用自定义解析器和 fallback 加载数据。
   *
   * @param <R> 解析后的数据类型
   * @param parser 数据行解析器
   * @param fallback fallback 数据提供者
   * @return 解析后的数据列表
   */
  protected <R> List<R> loadDataWithParserAndFallback(
      Function<String, R> parser, Supplier<List<R>> fallback) {
    return dataLoadingService.loadDataWithFallback(getDataFilePath(), parser, fallback);
  }

  /** 重新加载数据（用于动态刷新）。 */
  protected void reloadData() {
    synchronized (dataLoadLock) {
      dataLoaded = false;
      ensureDataLoaded();
    }
  }

  /**
   * 检查数据是否已加载。
   *
   * @return 如果数据已加载返回 true
   */
  protected boolean isDataLoaded() {
    return dataLoaded;
  }
}
