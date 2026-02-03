package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.util.DataLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 邮箱地址生成器。
 *
 * <p>生成符合邮箱格式的电子邮件地址，支持大规模邮箱生成。 支持自定义域名、用户名长度、与姓名关联、权重选择等功能。 通过配置文件管理域名数据，支持生成数十亿唯一邮箱地址。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class EmailGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(EmailGenerator.class);

  /** 邮箱域名数据文件路径。 */
  private static final String EMAIL_DOMAINS_PATH = "data/email-domains.txt";

  /** 缓存的域名数据。 */
  private volatile Map<String, DomainInfo> emailDomains;

  private volatile List<String> allDomains;
  private volatile Map<String, List<String>> domainsByType;

  /** Fallback域名数据（当文件加载失败时使用）。 */
  private static final List<String> FALLBACK_COMMON_DOMAINS =
      Arrays.asList(
          "qq.com",
          "163.com",
          "126.com",
          "gmail.com",
          "hotmail.com",
          "yahoo.com",
          "sina.com",
          "sohu.com",
          "139.com",
          "189.cn",
          "outlook.com",
          "foxmail.com",
          "aliyun.com",
          "yeah.net");

  /** 域名信息类。 */
  private static class DomainInfo {
    // 简化的域名信息类，目前只用作占位符
    // 实际的域名信息通过 Map 的键和分类映射来管理

    DomainInfo(String domain, String type, int weight) {
      // 构造函数保留以维持兼容性
    }
  }

  /** 中文姓名拼音映射（简化版）。 */
  private static final java.util.Map<String, String> PINYIN_MAP = new java.util.HashMap<>();

  static {
    // 常见姓氏拼音
    PINYIN_MAP.put("王", "wang");
    PINYIN_MAP.put("李", "li");
    PINYIN_MAP.put("张", "zhang");
    PINYIN_MAP.put("刘", "liu");
    PINYIN_MAP.put("陈", "chen");
    PINYIN_MAP.put("杨", "yang");
    PINYIN_MAP.put("黄", "huang");
    PINYIN_MAP.put("赵", "zhao");
    PINYIN_MAP.put("周", "zhou");
    PINYIN_MAP.put("吴", "wu");
    PINYIN_MAP.put("徐", "xu");
    PINYIN_MAP.put("孙", "sun");
    PINYIN_MAP.put("朱", "zhu");
    PINYIN_MAP.put("马", "ma");
    PINYIN_MAP.put("胡", "hu");
    PINYIN_MAP.put("郭", "guo");
    PINYIN_MAP.put("林", "lin");
    PINYIN_MAP.put("何", "he");
    PINYIN_MAP.put("高", "gao");
    PINYIN_MAP.put("梁", "liang");

    // 常见名字拼音
    PINYIN_MAP.put("伟", "wei");
    PINYIN_MAP.put("强", "qiang");
    PINYIN_MAP.put("磊", "lei");
    PINYIN_MAP.put("军", "jun");
    PINYIN_MAP.put("勇", "yong");
    PINYIN_MAP.put("涛", "tao");
    PINYIN_MAP.put("明", "ming");
    PINYIN_MAP.put("超", "chao");
    PINYIN_MAP.put("辉", "hui");
    PINYIN_MAP.put("华", "hua");
    PINYIN_MAP.put("丽", "li");
    PINYIN_MAP.put("娟", "juan");
    PINYIN_MAP.put("敏", "min");
    PINYIN_MAP.put("静", "jing");
    PINYIN_MAP.put("洁", "jie");
    PINYIN_MAP.put("秀", "xiu");
    PINYIN_MAP.put("兰", "lan");
    PINYIN_MAP.put("红", "hong");
    PINYIN_MAP.put("霞", "xia");
  }

  @Override
  public String getType() {
    return "email";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 延迟加载数据
      ensureDataLoaded(config);

      // 从参数中获取域名列表（支持 domain 单值或 domains 多值）
      String domainsParam = getStringParam(config, "domains", null);
      if (domainsParam == null || domainsParam.trim().isEmpty()) {
        String singleDomain = getStringParam(config, "domain", null);
        domainsParam = singleDomain != null && !singleDomain.trim().isEmpty() ? singleDomain : null;
      }
      List<String> domains = parseDomains(domainsParam);

      // 从参数中获取用户名长度范围（支持 username_length 或 usernameLength，支持单数字固定长度）
      String usernameLengthParam =
          getStringParam(config, "username_length", getStringParam(config, "usernameLength", "6,12"));
      int[] lengthRange = parseLengthRange(usernameLengthParam);

      // 从参数中获取是否使用姓名前缀
      boolean prefixName = getBooleanParam(config, "prefix_name", false);

      // 从参数中获取是否生成有效邮箱
      boolean valid = getBooleanParam(config, "valid", true);

      // 从参数中获取邮箱类型或格式（format=corporate 时按企业邮箱生成）
      String emailType = getStringParam(config, "type", "PERSONAL");
      String formatParam = getStringParam(config, "format", null);
      boolean corporateFormat =
          "corporate".equalsIgnoreCase(formatParam) || "corporate".equalsIgnoreCase(emailType);

      if (!valid) {
        return generateInvalidEmail();
      }

      return generateValidEmail(domains, lengthRange, prefixName, emailType, corporateFormat, context);

    } catch (Exception e) {
      logger.error("Failed to generate email (domains/params may be invalid): {}", e.getMessage(), e);
      // 返回一个默认邮箱作为fallback
      return "user@example.com";
    }
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  /**
   * 确保数据已加载。
   *
   * @param config 配置
   */
  private void ensureDataLoaded(FieldConfig config) {
    if (emailDomains == null) {
      synchronized (this) {
        if (emailDomains == null) {
          loadData(config);
        }
      }
    }
  }

  /**
   * 加载邮箱域名数据。
   *
   * @param config 配置
   */
  private void loadData(FieldConfig config) {
    try {
      // 检查是否有自定义数据文件路径
      String customDomainsPath = getStringParam(config, "email_domains_file", null);

      List<String> lines;
      if (customDomainsPath != null) {
        lines = DataLoader.loadDataFromFile(customDomainsPath);
      } else {
        lines = DataLoader.loadDataFromResource(EMAIL_DOMAINS_PATH);
      }

      emailDomains = new java.util.HashMap<>();
      domainsByType = new java.util.HashMap<>();

      for (String line : lines) {
        String[] parts = line.split(":");
        if (parts.length >= 2) {
          String domain = parts[0].trim();
          String type = parts[1].trim();
          int weight = parts.length > 2 ? parseWeight(parts[2].trim()) : 1;

          DomainInfo info = new DomainInfo(domain, type, weight);
          emailDomains.put(domain, info);

          domainsByType.computeIfAbsent(type, k -> new java.util.ArrayList<>()).add(domain);
        }
      }

      allDomains = new java.util.ArrayList<>(emailDomains.keySet());

      // 如果加载失败，使用fallback数据
      if (emailDomains.isEmpty()) {
        initializeFallbackData();
      }

      logger.info(
          "Email domain data loaded - Total domains: {}, Types: {}",
          emailDomains.size(),
          domainsByType.keySet().size());

    } catch (Exception e) {
      logger.warn("Failed to load email domain data, using fallback: {}", e.getMessage());
      initializeFallbackData();
    }
  }

  /**
   * 解析权重值。
   *
   * @param weightStr 权重字符串
   * @return 权重值
   */
  private int parseWeight(String weightStr) {
    try {
      return Integer.parseInt(weightStr);
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  /** 初始化fallback数据。 */
  private void initializeFallbackData() {
    emailDomains = new java.util.HashMap<>();
    domainsByType = new java.util.HashMap<>();

    // 添加fallback数据
    for (String domain : FALLBACK_COMMON_DOMAINS) {
      DomainInfo info = new DomainInfo(domain, "PERSONAL", 1);
      emailDomains.put(domain, info);
      domainsByType.computeIfAbsent("PERSONAL", k -> new java.util.ArrayList<>()).add(domain);
    }

    allDomains = new java.util.ArrayList<>(emailDomains.keySet());
  }

  /**
   * 生成有效的邮箱地址。
   *
   * @param domains 域名列表
   * @param lengthRange 用户名长度范围
   * @param prefixName 是否使用姓名前缀
   * @param emailType 邮箱类型
   * @param context 上下文
   * @return 有效的邮箱地址
   */
  private String generateValidEmail(
      List<String> domains,
      int[] lengthRange,
      boolean prefixName,
      String emailType,
      boolean corporateFormat,
      DataForgeContext context) {
    // 生成用户名（企业格式为 firstname.lastname）
    String username =
        corporateFormat
            ? generateCorporateUsername()
            : generateUsername(lengthRange, prefixName, context);

    // 选择域名
    String domain = selectDomain(domains, corporateFormat ? "CORPORATE" : emailType);

    return username + "@" + domain;
  }

  /** 生成企业邮箱用户名（小写 firstname.lastname 格式） */
  private String generateCorporateUsername() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    int len1 = random.nextInt(4, 10);
    int len2 = random.nextInt(4, 10);
    StringBuilder sb = new StringBuilder(len1 + len2 + 1);
    for (int i = 0; i < len1; i++) {
      sb.append((char) ('a' + random.nextInt(26)));
    }
    sb.append('.');
    for (int i = 0; i < len2; i++) {
      sb.append((char) ('a' + random.nextInt(26)));
    }
    return sb.toString();
  }

  /**
   * 生成用户名。
   *
   * @param lengthRange 长度范围
   * @param prefixName 是否使用姓名前缀
   * @param context 上下文
   * @return 用户名
   */
  private String generateUsername(int[] lengthRange, boolean prefixName, DataForgeContext context) {
    StringBuilder username = new StringBuilder();

    // 如果启用姓名前缀，尝试从上下文获取姓名
    if (prefixName) {
      String namePrefix = extractNamePrefix(context);
      if (namePrefix != null && !namePrefix.isEmpty()) {
        username.append(namePrefix);
      }
    }

    // 如果用户名还不够长，添加随机部分
    int minLength = lengthRange[0];
    int maxLength = lengthRange[1];
    ThreadLocalRandom random = ThreadLocalRandom.current();
    int targetLength = random.nextInt(maxLength - minLength + 1) + minLength;

    // 如果已有前缀，调整目标长度
    if (username.length() > 0) {
      targetLength = Math.max(targetLength, username.length() + 2);
    }

    // 填充到目标长度
    while (username.length() < targetLength) {
      if (username.length() == 0 || random.nextBoolean()) {
        // 添加字母
        username.append((char) ('a' + random.nextInt(26)));
      } else {
        // 添加数字
        username.append(random.nextInt(10));
      }
    }

    // 确保用户名不以数字开头
    if (Character.isDigit(username.charAt(0))) {
      username.setCharAt(0, (char) ('a' + random.nextInt(26)));
    }

    return username.toString();
  }

  /**
   * 从上下文中提取姓名前缀。
   *
   * @param context 上下文
   * @return 姓名前缀
   */
  private String extractNamePrefix(DataForgeContext context) {
    // 尝试从上下文获取姓名
    String name = context.get("name", String.class).orElse(null);
    if (name == null || name.trim().isEmpty()) {
      return null;
    }

    name = name.trim();

    // 如果是英文姓名
    if (name.matches("[a-zA-Z\\s]+")) {
      return extractEnglishNamePrefix(name);
    }

    // 如果是中文姓名
    if (name.matches("[\\u4e00-\\u9fa5]+")) {
      return extractChineseNamePrefix(name);
    }

    // 其他情况，取前几个字符
    return name.substring(0, Math.min(name.length(), 6)).toLowerCase();
  }

  /**
   * 提取英文姓名前缀。
   *
   * @param name 英文姓名
   * @return 前缀
   */
  private String extractEnglishNamePrefix(String name) {
    String[] parts = name.toLowerCase().split("\\s+");
    if (parts.length >= 2) {
      // 名.姓 或 名姓
      String firstName = parts[0];
      String lastName = parts[parts.length - 1];

      if (ThreadLocalRandom.current().nextBoolean()) {
        return firstName + "." + lastName;
      } else {
        return firstName + lastName;
      }
    } else {
      return parts[0];
    }
  }

  /**
   * 提取中文姓名前缀。
   *
   * @param name 中文姓名
   * @return 前缀
   */
  private String extractChineseNamePrefix(String name) {
    StringBuilder pinyin = new StringBuilder();

    for (char c : name.toCharArray()) {
      String py = PINYIN_MAP.get(String.valueOf(c));
      if (py != null) {
        pinyin.append(py);
      } else {
        // 如果没有找到拼音，使用字符的Unicode值生成
        pinyin.append("u").append(Integer.toHexString(c));
      }
    }

    return pinyin.toString();
  }

  /**
   * 选择域名。
   *
   * @param domains 指定的域名列表
   * @param emailType 邮箱类型
   * @return 域名
   */
  private String selectDomain(List<String> domains, String emailType) {
    // 确保已加载数据
    if (allDomains == null || allDomains.isEmpty()) {
      initializeFallbackData();
    }

    if (domains != null && !domains.isEmpty()) {
      return domains.get(ThreadLocalRandom.current().nextInt(domains.size()));
    }

    // 根据类型选择域名
    List<String> candidateDomains = domainsByType.get(emailType.toUpperCase());
    if (candidateDomains == null || candidateDomains.isEmpty()) {
      candidateDomains = allDomains;
    }

    // 再次检查候选域名列表是否为空
    if (candidateDomains == null || candidateDomains.isEmpty()) {
      logger.warn("No domains available, using fallback domain");
      return "example.com";
    }

    return candidateDomains.get(ThreadLocalRandom.current().nextInt(candidateDomains.size()));
  }

  /**
   * 生成无效的邮箱地址。
   *
   * @return 无效的邮箱地址
   */
  private String generateInvalidEmail() {
    int type = ThreadLocalRandom.current().nextInt(5);

    return switch (type) {
      case 0 -> generateEmailWithoutAt();
      case 1 -> generateEmailWithoutDomain();
      case 2 -> generateEmailWithoutUsername();
      case 3 -> generateEmailWithInvalidChars();
      default -> generateEmailWithMultipleAt();
    };
  }

  /**
   * 生成不包含@符号的邮箱。
   *
   * @return 无效邮箱
   */
  private String generateEmailWithoutAt() {
    return "usernameexample.com";
  }

  /**
   * 生成没有域名的邮箱。
   *
   * @return 无效邮箱
   */
  private String generateEmailWithoutDomain() {
    return "username@";
  }

  /**
   * 生成没有用户名的邮箱。
   *
   * @return 无效邮箱
   */
  private String generateEmailWithoutUsername() {
    return "@example.com";
  }

  /**
   * 生成包含非法字符的邮箱。
   *
   * @return 无效邮箱
   */
  private String generateEmailWithInvalidChars() {
    String[] invalidChars = {" ", "<", ">", "[", "]", "\\", ",", ";", ":"};
    String invalidChar = invalidChars[ThreadLocalRandom.current().nextInt(invalidChars.length)];
    return "user" + invalidChar + "name@example.com";
  }

  /**
   * 生成包含多个@符号的邮箱。
   *
   * @return 无效邮箱
   */
  private String generateEmailWithMultipleAt() {
    return "user@name@example.com";
  }

  /**
   * 解析域名参数。
   *
   * @param domainsParam 域名参数字符串
   * @return 域名列表
   */
  private List<String> parseDomains(String domainsParam) {
    if (domainsParam == null || domainsParam.trim().isEmpty()) {
      return null;
    }

    return Arrays.stream(domainsParam.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(java.util.stream.Collectors.toList());
  }

  /**
   * 解析长度范围参数。
   *
   * @param lengthParam 长度参数字符串
   * @return 长度范围数组 [min, max]
   */
  private int[] parseLengthRange(String lengthParam) {
    if (lengthParam == null || lengthParam.trim().isEmpty()) {
      return new int[] {6, 12};
    }
    String trimmed = lengthParam.trim();
    try {
      // 支持单数字固定长度，如 "10"、"1"、"64"
      if (!trimmed.contains(",")) {
        int fixed = Integer.parseInt(trimmed);
        fixed = Math.max(1, Math.min(64, fixed));
        return new int[] {fixed, fixed};
      }
      String[] parts = trimmed.split(",");
      if (parts.length == 2) {
        int min = Integer.parseInt(parts[0].trim());
        int max = Integer.parseInt(parts[1].trim());
        return new int[] {Math.max(1, min), Math.max(min, max)};
      }
    } catch (NumberFormatException e) {
      logger.warn("Invalid length range: {}, using default", lengthParam);
    }

    return new int[] {6, 12}; // 默认范围
  }

  /**
   * 获取邮箱域名统计信息。
   *
   * @return 统计信息
   */
  public String getDomainStats() {
    ensureDataLoaded(null);

    StringBuilder stats = new StringBuilder();
    stats.append("Total domains: ").append(emailDomains.size()).append("\n");

    for (Map.Entry<String, List<String>> entry : domainsByType.entrySet()) {
      stats
          .append(entry.getKey())
          .append(": ")
          .append(entry.getValue().size())
          .append(" domains\n");
    }

    // 计算理论组合数（用户名长度6-12位，字母数字组合）
    // 每个域名 × 用户名组合数 = 总组合数
    long usernameVariations = 0;
    for (int len = 6; len <= 12; len++) {
      // 每个位置可以是26个字母或10个数字，但第一位必须是字母
      usernameVariations += 26 * (long) Math.pow(36, len - 1);
    }

    long totalCombinations = (long) emailDomains.size() * usernameVariations;
    stats.append("\nTotal possible combinations: ").append(String.format("%,d", totalCombinations));

    return stats.toString();
  }

  @Override
  public String getDescription() {
    return "Email address generator - generates email addresses with comprehensive domain and"
        + " username customization";
  }
}
