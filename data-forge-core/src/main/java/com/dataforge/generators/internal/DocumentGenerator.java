package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 文档生成器
 *
 * <p>根据DataForge设计文档第10.5节：PDF／Office 文档 (PDF / Office Document) 生成模拟的PDF或Office文档的二进制片段，
 * 用于测试文档处理和安全扫描。
 *
 * @author DataForge
 * @version 1.0.0
 * @since 2024-01-15
 */
@Component
public class DocumentGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(DocumentGenerator.class);

  // 文档类型模板映射
  private static final Map<String, DocumentTemplate> DOCUMENT_TEMPLATES = new ConcurrentHashMap<>();

  static {
    DOCUMENT_TEMPLATES.put("PDF", new PdfTemplate());
    DOCUMENT_TEMPLATES.put("DOCX", new DocxTemplate());
    DOCUMENT_TEMPLATES.put("XLSX", new XlsxTemplate());
    DOCUMENT_TEMPLATES.put("PPTX", new PptxTemplate());
    DOCUMENT_TEMPLATES.put("ODT", new OdtTemplate());
    DOCUMENT_TEMPLATES.put("ODS", new OdsTemplate());
    DOCUMENT_TEMPLATES.put("ODP", new OdpTemplate());
  }

  public DocumentGenerator() {}

  @Override
  public String getType() {
    return "DOCUMENT";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String documentType = getStringParam(config, "type", "PDF");
      int length = getIntParam(config, "length", 1024);
      boolean includeMacro = getBooleanParam(config, "includeMacroSignature", false);
      boolean includeScript = getBooleanParam(config, "includeScriptSignature", false);
      boolean includeXss = getBooleanParam(config, "includeXssSignature", false);
      String encoding = getStringParam(config, "encoding", "BASE64");

      return generateDocument(
          documentType, length, includeMacro, includeScript, includeXss, encoding);

    } catch (Exception e) {
      logger.warn("Error generating document: {}", e.getMessage());
      return generateDefaultPdfDocument();
    }
  }

  /** 生成文档 */
  private String generateDocument(
      String documentType,
      int length,
      boolean includeMacro,
      boolean includeScript,
      boolean includeXss,
      String encoding) {
    DocumentTemplate template = DOCUMENT_TEMPLATES.get(documentType.toUpperCase());
    if (template == null) {
      logger.warn("Unsupported document type: {}, using PDF", documentType);
      template = DOCUMENT_TEMPLATES.get("PDF");
    }

    byte[] documentData =
        template.generateDocument(length, includeMacro, includeScript, includeXss);
    return encodeDocument(documentData, encoding);
  }

  /** 根据编码格式编码文档数据 */
  private String encodeDocument(byte[] data, String encoding) {
    switch (encoding.toUpperCase()) {
      case "BASE64":
        return Base64.getEncoder().encodeToString(data);
      case "HEX":
        return encodeToHex(data);
      case "RAW":
        return new String(data, StandardCharsets.UTF_8);
      default:
        logger.warn("Unknown encoding: {}, using BASE64", encoding);
        return Base64.getEncoder().encodeToString(data);
    }
  }

  /** 编码为十六进制字符串 */
  private String encodeToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02X", b & 0xFF));
    }
    return sb.toString();
  }

  /** 生成默认PDF文档 */
  private String generateDefaultPdfDocument() {
    DocumentTemplate pdfTemplate = DOCUMENT_TEMPLATES.get("PDF");
    byte[] documentData = pdfTemplate.generateDocument(1024, false, false, false);
    return Base64.getEncoder().encodeToString(documentData);
  }

  @Override
  public boolean isValidConfig(FieldConfig config) {
    if (config == null) {
      return false;
    }

    String documentType = getStringParam(config, "type", "PDF");
    String encoding = getStringParam(config, "encoding", "BASE64");
    int length = getIntParam(config, "length", 1024);

    // 验证文档类型
    if (!DOCUMENT_TEMPLATES.containsKey(documentType.toUpperCase())) {
      return false;
    }

    // 验证编码格式
    if (!encoding.matches("(?i)BASE64|HEX|RAW")) {
      return false;
    }

    // 验证长度
    return length > 0 && length <= 10 * 1024 * 1024; // 最大10MB
  }

  @Override
  public String getDescription() {
    return "生成模拟的PDF或Office文档二进制片段，支持PDF、DOCX、XLSX、PPTX、ODT、ODS、ODP格式，" + "可包含宏脚本或XSS特征用于安全测试";
  }

  /** 文档模板接口 */
  interface DocumentTemplate {
    byte[] generateDocument(
        int length, boolean includeMacro, boolean includeScript, boolean includeXss);
  }

  /** PDF模板实现 */
  static class PdfTemplate implements DocumentTemplate {
    @Override
    public byte[] generateDocument(
        int length, boolean includeMacro, boolean includeScript, boolean includeXss) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      // PDF文件头
      String header = "%PDF-1.4\n";
      baos.writeBytes(header.getBytes(StandardCharsets.US_ASCII));

      // 添加恶意特征
      if (includeMacro) {
        String macroSignature = "/JavaScript ";
        baos.writeBytes(macroSignature.getBytes(StandardCharsets.US_ASCII));
      }

      if (includeScript) {
        String scriptSignature = "/Action /S /JavaScript /JS ";
        baos.writeBytes(scriptSignature.getBytes(StandardCharsets.US_ASCII));
      }

      if (includeXss) {
        String xssSignature = "<script>alert('XSS')</script>";
        baos.writeBytes(xssSignature.getBytes(StandardCharsets.US_ASCII));
      }

      // 填充剩余内容
      int currentSize = baos.size();
      if (currentSize < length) {
        byte[] padding = new byte[length - currentSize];
        ThreadLocalRandom.current().nextBytes(padding);
        baos.writeBytes(padding);
      }

      return baos.toByteArray();
    }
  }

  /** DOCX模板实现 */
  static class DocxTemplate implements DocumentTemplate {
    @Override
    public byte[] generateDocument(
        int length, boolean includeMacro, boolean includeScript, boolean includeXss) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      // DOCX文件头
      String header = "PK\u0003\u0004";
      baos.writeBytes(header.getBytes(StandardCharsets.US_ASCII));

      // 添加恶意特征
      if (includeMacro) {
        String macroSignature = "[VBA macros]";
        baos.writeBytes(macroSignature.getBytes(StandardCharsets.US_ASCII));
      }

      if (includeScript) {
        String scriptSignature = "<w:script>";
        baos.writeBytes(scriptSignature.getBytes(StandardCharsets.US_ASCII));
      }

      if (includeXss) {
        String xssSignature = "<script>alert('XSS')</script>";
        baos.writeBytes(xssSignature.getBytes(StandardCharsets.US_ASCII));
      }

      // 填充剩余内容
      int currentSize = baos.size();
      if (currentSize < length) {
        byte[] padding = new byte[length - currentSize];
        ThreadLocalRandom.current().nextBytes(padding);
        baos.writeBytes(padding);
      }

      return baos.toByteArray();
    }
  }

  /** XLSX模板实现 */
  static class XlsxTemplate implements DocumentTemplate {
    @Override
    public byte[] generateDocument(
        int length, boolean includeMacro, boolean includeScript, boolean includeXss) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      // XLSX文件头
      String header = "PK\u0003\u0004";
      baos.writeBytes(header.getBytes(StandardCharsets.US_ASCII));

      // 添加恶意特征
      if (includeMacro) {
        String macroSignature = "[VBA macros]";
        baos.writeBytes(macroSignature.getBytes(StandardCharsets.US_ASCII));
      }

      if (includeScript) {
        String scriptSignature = "<x:script>";
        baos.writeBytes(scriptSignature.getBytes(StandardCharsets.US_ASCII));
      }

      if (includeXss) {
        String xssSignature = "<script>alert('XSS')</script>";
        baos.writeBytes(xssSignature.getBytes(StandardCharsets.US_ASCII));
      }

      // 填充剩余内容
      int currentSize = baos.size();
      if (currentSize < length) {
        byte[] padding = new byte[length - currentSize];
        ThreadLocalRandom.current().nextBytes(padding);
        baos.writeBytes(padding);
      }

      return baos.toByteArray();
    }
  }

  /** PPTX模板实现 */
  static class PptxTemplate implements DocumentTemplate {
    @Override
    public byte[] generateDocument(
        int length, boolean includeMacro, boolean includeScript, boolean includeXss) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      // PPTX文件头
      String header = "PK\u0003\u0004";
      baos.writeBytes(header.getBytes(StandardCharsets.US_ASCII));

      // 添加恶意特征
      if (includeMacro) {
        String macroSignature = "[VBA macros]";
        baos.writeBytes(macroSignature.getBytes(StandardCharsets.US_ASCII));
      }

      if (includeScript) {
        String scriptSignature = "<p:script>";
        baos.writeBytes(scriptSignature.getBytes(StandardCharsets.US_ASCII));
      }

      if (includeXss) {
        String xssSignature = "<script>alert('XSS')</script>";
        baos.writeBytes(xssSignature.getBytes(StandardCharsets.US_ASCII));
      }

      // 填充剩余内容
      int currentSize = baos.size();
      if (currentSize < length) {
        byte[] padding = new byte[length - currentSize];
        ThreadLocalRandom.current().nextBytes(padding);
        baos.writeBytes(padding);
      }

      return baos.toByteArray();
    }
  }

  /** ODT模板实现 */
  static class OdtTemplate implements DocumentTemplate {
    @Override
    public byte[] generateDocument(
        int length, boolean includeMacro, boolean includeScript, boolean includeXss) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      // ODT文件头
      String header = "PK\u0003\u0004";
      baos.writeBytes(header.getBytes(StandardCharsets.US_ASCII));

      // 添加恶意特征
      if (includeMacro) {
        String macroSignature = "[macros]";
        baos.writeBytes(macroSignature.getBytes(StandardCharsets.US_ASCII));
      }

      if (includeScript) {
        String scriptSignature = "<office:script>";
        baos.writeBytes(scriptSignature.getBytes(StandardCharsets.US_ASCII));
      }

      if (includeXss) {
        String xssSignature = "<script>alert('XSS')</script>";
        baos.writeBytes(xssSignature.getBytes(StandardCharsets.US_ASCII));
      }

      // 填充剩余内容
      int currentSize = baos.size();
      if (currentSize < length) {
        byte[] padding = new byte[length - currentSize];
        ThreadLocalRandom.current().nextBytes(padding);
        baos.writeBytes(padding);
      }

      return baos.toByteArray();
    }
  }

  /** ODS模板实现 */
  static class OdsTemplate implements DocumentTemplate {
    @Override
    public byte[] generateDocument(
        int length, boolean includeMacro, boolean includeScript, boolean includeXss) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      // ODS文件头
      String header = "PK\u0003\u0004";
      baos.writeBytes(header.getBytes(StandardCharsets.US_ASCII));

      // 添加恶意特征
      if (includeMacro) {
        String macroSignature = "[macros]";
        baos.writeBytes(macroSignature.getBytes(StandardCharsets.US_ASCII));
      }

      if (includeScript) {
        String scriptSignature = "<office:script>";
        baos.writeBytes(scriptSignature.getBytes(StandardCharsets.US_ASCII));
      }

      if (includeXss) {
        String xssSignature = "<script>alert('XSS')</script>";
        baos.writeBytes(xssSignature.getBytes(StandardCharsets.US_ASCII));
      }

      // 填充剩余内容
      int currentSize = baos.size();
      if (currentSize < length) {
        byte[] padding = new byte[length - currentSize];
        ThreadLocalRandom.current().nextBytes(padding);
        baos.writeBytes(padding);
      }

      return baos.toByteArray();
    }
  }

  /** ODP模板实现 */
  static class OdpTemplate implements DocumentTemplate {
    @Override
    public byte[] generateDocument(
        int length, boolean includeMacro, boolean includeScript, boolean includeXss) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      // ODP文件头
      String header = "PK\u0003\u0004";
      baos.writeBytes(header.getBytes(StandardCharsets.US_ASCII));

      // 添加恶意特征
      if (includeMacro) {
        String macroSignature = "[macros]";
        baos.writeBytes(macroSignature.getBytes(StandardCharsets.US_ASCII));
      }

      if (includeScript) {
        String scriptSignature = "<office:script>";
        baos.writeBytes(scriptSignature.getBytes(StandardCharsets.US_ASCII));
      }

      if (includeXss) {
        String xssSignature = "<script>alert('XSS')</script>";
        baos.writeBytes(xssSignature.getBytes(StandardCharsets.US_ASCII));
      }

      // 填充剩余内容
      int currentSize = baos.size();
      if (currentSize < length) {
        byte[] padding = new byte[length - currentSize];
        ThreadLocalRandom.current().nextBytes(padding);
        baos.writeBytes(padding);
      }

      return baos.toByteArray();
    }
  }
}
