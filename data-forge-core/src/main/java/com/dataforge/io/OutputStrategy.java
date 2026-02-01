package com.dataforge.io;

import com.dataforge.config.OutputConfig;
import java.util.List;
import java.util.Map;

/**
 * 数据输出策略接口。
 *
 * <p>定义了数据输出的统一契约，支持多种输出格式和目标。 所有具体的输出实现都必须实现此接口。
 *
 * <p>该接口采用策略模式设计，使得输出格式的切换变得简单灵活。 支持流式输出，适合处理大量数据的场景。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public interface OutputStrategy {

  /**
   * 获取输出策略支持的格式类型。
   *
   * @return 支持的输出格式
   */
  OutputConfig.Format getSupportedFormat();

  /**
   * 初始化输出策略。
   *
   * <p>在开始输出数据前调用，用于准备输出环境， 如创建文件、写入头部信息等。
   *
   * @param config 输出配置
   * @param fieldNames 字段名称列表，用于生成表头或结构定义
   * @throws OutputException 当初始化失败时
   */
  void initialize(OutputConfig config, List<String> fieldNames) throws OutputException;

  /**
   * 输出单条记录。
   *
   * <p>将一条记录的数据输出到目标位置。 该方法会被多次调用，每次处理一条记录。
   *
   * @param record 记录数据，键为字段名，值为字段值
   * @throws OutputException 当输出失败时
   */
  void writeRecord(Map<String, Object> record) throws OutputException;

  /**
   * 批量输出多条记录。
   *
   * <p>默认实现是循环调用writeRecord方法。 子类可以重写此方法以提供更高效的批量输出实现。
   *
   * @param records 记录列表
   * @throws OutputException 当输出失败时
   */
  default void writeRecords(List<Map<String, Object>> records) throws OutputException {
    for (Map<String, Object> record : records) {
      writeRecord(record);
    }
  }

  /**
   * 完成输出并清理资源。
   *
   * <p>在所有数据输出完成后调用，用于写入尾部信息、 关闭文件流、清理临时资源等。
   *
   * @throws OutputException 当完成操作失败时
   */
  void finish() throws OutputException;

  /**
   * 检查输出策略是否支持指定的配置。
   *
   * <p>默认实现检查格式是否匹配。 子类可以重写此方法以提供更详细的配置验证。
   *
   * @param config 输出配置
   * @return 如果支持返回true，否则返回false
   */
  default boolean supports(OutputConfig config) {
    return config != null
        && getSupportedFormat() == config.getFormat()
        && supportsMimeType(config.getMimeType());
  }

  /**
   * 检查输出策略是否支持指定的MIME类型。
   *
   * <p>默认实现检查MIME类型是否匹配格式。 子类可以重写此方法以提供更灵活的MIME类型支持。
   *
   * @param mimeType MIME类型
   * @return 如果支持返回true，否则返回false
   */
  default boolean supportsMimeType(String mimeType) {
    if (mimeType == null) {
      return true; // 无MIME类型要求
    }

    switch (getSupportedFormat()) {
      case CSV:
        return mimeType.equalsIgnoreCase("text/csv")
            || mimeType.equalsIgnoreCase("application/csv")
            || mimeType.equalsIgnoreCase("text/comma-separated-values");
      case JSON:
        return mimeType.equalsIgnoreCase("application/json")
            || mimeType.equalsIgnoreCase("text/json");
      case XML:
        return mimeType.equalsIgnoreCase("application/xml")
            || mimeType.equalsIgnoreCase("text/xml");
      case TSV:
        return mimeType.equalsIgnoreCase("text/tab-separated-values")
            || mimeType.equalsIgnoreCase("text/tsv");
      default:
        return false;
    }
  }

  /**
   * 获取输出策略的描述信息。
   *
   * <p>用于日志记录和调试信息。
   *
   * @return 策略描述
   */
  default String getDescription() {
    return "Output strategy for " + getSupportedFormat() + " format";
  }

  /**
   * 刷新输出缓冲区。
   *
   * <p>强制将缓冲区中的数据写入到目标位置。 默认实现为空，子类可以根据需要重写。
   *
   * @throws OutputException 当刷新失败时
   */
  default void flush() throws OutputException {
    // 默认实现为空
  }

  /**
   * 获取当前已输出的记录数量。
   *
   * <p>用于进度跟踪和统计信息。 默认返回-1表示不支持计数。
   *
   * @return 已输出的记录数量，-1表示不支持计数
   */
  default long getWrittenRecordCount() {
    return -1;
  }
}
