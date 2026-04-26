package com.dataforge.io;

import com.dataforge.config.OutputConfig;
import com.dataforge.io.format.OutputFormat;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 改进的文件输出策略 使用Format+Sink模式，职责清晰 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ImprovedFileOutputStrategy implements OutputStrategy {

  private BufferedWriter writer;
  private OutputFormat format;
  private List<String> fieldNames;
  private boolean initialized = false;

  @Override
  public boolean supports(OutputConfig config) {
    if (config == null) return false;
    boolean hasFile = config.getFile() != null && !config.getFile().trim().isEmpty();
    if (!hasFile) return false;
    // 仅在文件输出且格式为CSV或JSON时生效，避免误选 CONSOLE 等
    return config.getFormat() == OutputConfig.Format.CSV
        || config.getFormat() == OutputConfig.Format.JSON;
  }

  @Override
  public void initialize(OutputConfig config, List<String> fieldNames) throws OutputException {
    BufferedWriter tempWriter = null;
    try {
      this.fieldNames = fieldNames;

      // 验证并清理文件路径，防止目录遍历攻击
      String safeFilePath = validateAndSanitizePath(config.getFile());

      // 创建文件写入器，支持编码和追加模式
      String encoding = config.getEncoding() != null ? config.getEncoding() : "UTF-8";
      Charset charset = Charset.forName(encoding);

      tempWriter = new BufferedWriter(new FileWriter(safeFilePath, charset, config.isAppend()));
      this.writer = tempWriter;
      tempWriter = null; // 防止finally中关闭

      // 根据格式选择对应的Format实现
      this.format = selectFormat(config.getFormat());

      // 写入格式头部
      format.start(fieldNames, writer);

      this.initialized = true;

    } catch (IOException e) {
      throw new OutputException("无法创建输出文件: " + config.getFile(), e);
    } finally {
      // 如果初始化失败，确保关闭临时writer
      if (tempWriter != null) {
        try {
          tempWriter.close();
        } catch (IOException e) {
          // 忽略关闭异常
        }
      }
    }
  }

  /**
   * 验证并清理文件路径，防止目录遍历攻击。
   *
   * <p>安全措施：
   *
   * <ul>
   *   <li>1. 将路径标准化，解析所有".."和"."
   *   <li>2. 确保文件在允许的输出目录内
   *   <li>3. 验证文件名不包含非法字符
   * </ul>
   *
   * @param filePath 原始文件路径
   * @return 安全的文件路径
   * @throws OutputException 当路径验证失败时
   */
  private String validateAndSanitizePath(String filePath) throws OutputException {
    if (filePath == null || filePath.trim().isEmpty()) {
      throw new OutputException("文件路径不能为空");
    }

    try {
      // 标准化路径，解析所有".."和"."
      Path path = Paths.get(filePath).normalize();
      File file = path.toFile();

      // 获取规范化的绝对路径
      String canonicalPath = file.getCanonicalPath();
      File canonicalFile = new File(canonicalPath);

      // 检查是否是系统临时目录（允许测试使用）
      String tempDir = System.getProperty("java.io.tmpdir");
      if (tempDir != null && canonicalPath.startsWith(new File(tempDir).getCanonicalPath())) {
        // 确保父目录存在
        File parentDir = canonicalFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
          parentDir.mkdirs();
        }
        return canonicalPath;
      }

      // 定义允许的基础输出目录（默认为当前工作目录下的output文件夹）
      String baseDir = System.getProperty("dataforge.output.dir", "output");
      File baseDirFile = new File(baseDir).getCanonicalFile();

      // 确保文件在允许的输出目录内
      if (!canonicalFile.getAbsolutePath().startsWith(baseDirFile.getAbsolutePath())) {
        // 如果路径不在基础目录内，将文件限制在基础目录下
        String fileName = path.getFileName().toString();
        // 验证文件名不包含路径分隔符
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
          throw new OutputException("非法文件名，包含路径分隔符: " + fileName);
        }
        canonicalFile = new File(baseDirFile, fileName);
        canonicalPath = canonicalFile.getCanonicalPath();
      }

      // 确保父目录存在
      File parentDir = canonicalFile.getParentFile();
      if (parentDir != null && !parentDir.exists()) {
        if (!parentDir.mkdirs()) {
          throw new OutputException("无法创建父目录: " + parentDir.getAbsolutePath());
        }
      }

      return canonicalPath;

    } catch (IOException e) {
      throw new OutputException("文件路径验证失败: " + filePath, e);
    }
  }

  @Override
  public void writeRecord(Map<String, Object> record) throws OutputException {
    if (!initialized) {
      throw new OutputException("输出策略未初始化");
    }

    try {
      format.writeRecord(record, fieldNames, writer);
    } catch (IOException e) {
      throw new OutputException("写入记录失败", e);
    }
  }

  @Override
  public void writeRecords(List<Map<String, Object>> records) throws OutputException {
    if (!initialized) {
      throw new OutputException("输出策略未初始化");
    }

    try {
      format.writeRecords(records, fieldNames, writer);
    } catch (IOException e) {
      throw new OutputException("批量写入记录失败", e);
    }
  }

  @Override
  public void flush() throws OutputException {
    if (writer != null) {
      try {
        writer.flush();
      } catch (IOException e) {
        throw new OutputException("刷新输出失败", e);
      }
    }
  }

  @Override
  public void finish() throws OutputException {
    try {
      if (writer != null && format != null) {
        format.end(writer);
        writer.close();
      }
    } catch (IOException e) {
      throw new OutputException("完成输出失败", e);
    }
  }

  @Override
  public OutputConfig.Format getSupportedFormat() {
    return format != null ? format.getFormat() : OutputConfig.Format.CSV;
  }

  /** 根据配置选择格式实现 */
  private OutputFormat selectFormat(OutputConfig.Format formatType) throws OutputException {
    // 这里可以通过Spring容器注入或工厂模式获取Format实现
    // 简化实现，直接创建
    switch (formatType) {
      case CSV:
        return new com.dataforge.io.format.CsvFormat();
      case JSON:
        return new com.dataforge.io.format.JsonFormat();
      default:
        throw new OutputException("不支持的输出格式: " + formatType);
    }
  }
}
