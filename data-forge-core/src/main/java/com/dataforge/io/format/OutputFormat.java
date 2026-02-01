package com.dataforge.io.format;

import com.dataforge.config.OutputConfig;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** 输出格式接口 负责将数据序列化为特定格式（CSV/JSON/SQL等） */
public interface OutputFormat {

  /** 获取支持的格式 */
  OutputConfig.Format getFormat();

  /** 开始输出（写入头部信息） */
  void start(List<String> fieldNames, Appendable out) throws IOException;

  /** 写入单条记录 */
  void writeRecord(Map<String, Object> record, List<String> fieldNames, Appendable out)
      throws IOException;

  /** 写入多条记录 */
  default void writeRecords(
      List<Map<String, Object>> records, List<String> fieldNames, Appendable out)
      throws IOException {
    for (Map<String, Object> record : records) {
      writeRecord(record, fieldNames, out);
    }
  }

  /** 结束输出（写入尾部信息） */
  void end(Appendable out) throws IOException;
}
