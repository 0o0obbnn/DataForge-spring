package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.util.DataLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 域名生成器
 *
 * <p>支持的参数： - tld: 顶级域名 (com|org|net|cn|ANY) - include_subdomain: 是否包含子域名 (true|false) - length:
 * 域名长度范围 (如 "5,15") - type: 域名类型 (GENERIC|BRAND|DICTIONARY|RANDOM) - file: 自定义域名词典文件路径 -
 * international: 是否支持国际化域名 (true|false)
 *
 * @author DataForge
 */
public class DomainGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(DomainGenerator.class);
  private static final Random random = new Random();

  // 顶级域名分类
  private static final Map<String, List<String>> TLD_CATEGORIES = new HashMap<>();

  // 通用域名词汇
  private static final List<String> GENERIC_WORDS =
      Arrays.asList(
          "tech",
          "web",
          "app",
          "digital",
          "online",
          "net",
          "data",
          "cloud",
          "smart",
          "global",
          "world",
          "international",
          "universal",
          "mega",
          "super",
          "ultra",
          "pro",
          "expert",
          "master",
          "premium",
          "elite",
          "advanced",
          "modern",
          "future",
          "next",
          "new",
          "best",
          "top");

  // 品牌相关词汇
  private static final List<String> BRAND_WORDS =
      Arrays.asList(
          "corp",
          "inc",
          "ltd",
          "group",
          "company",
          "enterprise",
          "solutions",
          "systems",
          "services",
          "consulting",
          "partners",
          "associates",
          "ventures",
          "capital",
          "holdings",
          "industries",
          "technologies",
          "innovations",
          "dynamics",
          "synergy",
          "nexus",
          "matrix",
          "vertex");

  // 字典词汇
  private static final List<String> DICTIONARY_WORDS =
      Arrays.asList(
          "apple",
          "google",
          "amazon",
          "microsoft",
          "facebook",
          "twitter",
          "linkedin",
          "youtube",
          "instagram",
          "pinterest",
          "reddit",
          "github",
          "stackoverflow",
          "wikipedia",
          "mozilla",
          "adobe",
          "oracle",
          "ibm",
          "intel",
          "nvidia",
          "samsung",
          "sony",
          "canon",
          "nikon",
          "tesla");

  // 子域名前缀
  private static final List<String> SUBDOMAIN_PREFIXES =
      Arrays.asList(
          "www", "api", "app", "mobile", "m", "admin", "blog", "shop", "store", "mail", "email",
          "ftp", "cdn", "static", "img", "images", "media", "assets", "files", "docs", "help",
          "support", "dev", "test", "staging", "beta", "alpha", "demo", "preview", "secure", "ssl",
          "vpn");

  static {
    initializeTldCategories();
  }

  private static void initializeTldCategories() {
    // 通用顶级域名
    TLD_CATEGORIES.put(
        "GENERIC",
        Arrays.asList(
            "com", "org", "net", "edu", "gov", "mil", "int", "info", "biz", "name", "pro", "museum",
            "travel", "jobs"));

    // 国家顶级域名
    TLD_CATEGORIES.put(
        "COUNTRY",
        Arrays.asList(
            "cn", "us", "uk", "de", "fr", "jp", "au", "ca", "ru", "br", "in", "kr", "it", "es",
            "nl", "se", "no", "dk", "fi", "pl"));

    // 新通用顶级域名
    TLD_CATEGORIES.put(
        "NEW_GTLD",
        Arrays.asList(
            "tech", "app", "web", "online", "site", "store", "shop", "blog", "news", "media",
            "photo", "video", "music", "game", "sport", "health", "food", "travel", "hotel", "car",
            "auto", "finance"));

    // 中文顶级域名
    TLD_CATEGORIES.put("CHINESE", Arrays.asList("中国", "公司", "网络", "组织", "政府", "教育", "商业", "信息"));
  }

  @Override
  public String getType() {
    return "domain";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String tld = config.getParam("tld", String.class, "ANY");
      boolean includeSubdomain =
          Boolean.parseBoolean(config.getParam("include_subdomain", String.class, "false"));
      String lengthRange = config.getParam("length", String.class, "5,15");
      String type = config.getParam("type", String.class, "GENERIC");
      boolean international =
          Boolean.parseBoolean(config.getParam("international", String.class, "false"));

      // 解析长度范围
      String[] lengthParts = lengthRange.split(",");
      int minLength = Integer.parseInt(lengthParts[0].trim());
      int maxLength = lengthParts.length > 1 ? Integer.parseInt(lengthParts[1].trim()) : minLength;

      // 生成域名
      String domain =
          generateDomain(tld, includeSubdomain, minLength, maxLength, type, international, config);

      // 将域名信息存入上下文
      context.put("domain", domain);
      context.put("domain_tld", extractTld(domain));
      context.put("domain_sld", extractSld(domain));

      logger.debug("Generated domain: {}", domain);
      return domain;

    } catch (Exception e) {
      logger.error("Error generating domain", e);
      return "example.com";
    }
  }

  private String generateDomain(
      String tld,
      boolean includeSubdomain,
      int minLength,
      int maxLength,
      String type,
      boolean international,
      FieldConfig config) {

    StringBuilder domain = new StringBuilder();

    // 1. 生成子域名（如果需要）
    if (includeSubdomain && random.nextDouble() < 0.3) {
      String subdomain = generateSubdomain();
      domain.append(subdomain).append(".");
    }

    // 2. 生成二级域名
    String sld = generateSecondLevelDomain(type, minLength, maxLength, config);
    domain.append(sld);

    // 3. 生成顶级域名
    String topLevelDomain = generateTopLevelDomain(tld, international);
    domain.append(".").append(topLevelDomain);

    return domain.toString();
  }

  private String generateSubdomain() {
    return SUBDOMAIN_PREFIXES.get(random.nextInt(SUBDOMAIN_PREFIXES.size()));
  }

  private String generateSecondLevelDomain(
      String type, int minLength, int maxLength, FieldConfig config) {
    // 加载自定义词典
    List<String> customWords = loadCustomWords(config);
    if (!customWords.isEmpty()) {
      return selectFromWords(customWords, minLength, maxLength);
    }

    // 根据类型生成
    switch (type.toUpperCase()) {
      case "GENERIC":
        return generateGenericDomain(minLength, maxLength);

      case "BRAND":
        return generateBrandDomain(minLength, maxLength);

      case "DICTIONARY":
        return generateDictionaryDomain(minLength, maxLength);

      case "RANDOM":
      default:
        return generateRandomDomain(minLength, maxLength);
    }
  }

  private List<String> loadCustomWords(FieldConfig config) {
    String customFile = config.getParam("file", String.class, null);
    if (customFile != null) {
      try {
        return DataLoader.loadDataFromFile(customFile);
      } catch (Exception e) {
        logger.warn("Failed to load custom domain file: {}", customFile, e);
      }
    }
    return new ArrayList<>();
  }

  private String selectFromWords(List<String> words, int minLength, int maxLength) {
    // 过滤符合长度要求的词汇
    List<String> validWords = new ArrayList<>();
    for (String word : words) {
      if (word.length() >= minLength && word.length() <= maxLength) {
        validWords.add(word);
      }
    }

    if (validWords.isEmpty()) {
      return generateRandomDomain(minLength, maxLength);
    }

    return validWords.get(random.nextInt(validWords.size()));
  }

  private String generateGenericDomain(int minLength, int maxLength) {
    // 组合通用词汇
    if (random.nextBoolean() && GENERIC_WORDS.size() > 1) {
      String word1 = GENERIC_WORDS.get(random.nextInt(GENERIC_WORDS.size()));
      String word2 = GENERIC_WORDS.get(random.nextInt(GENERIC_WORDS.size()));
      String combined = word1 + word2;

      if (combined.length() >= minLength && combined.length() <= maxLength) {
        return combined;
      }
    }

    // 单个通用词汇
    List<String> validWords = new ArrayList<>();
    for (String word : GENERIC_WORDS) {
      if (word.length() >= minLength && word.length() <= maxLength) {
        validWords.add(word);
      }
    }

    if (!validWords.isEmpty()) {
      return validWords.get(random.nextInt(validWords.size()));
    }

    return generateRandomDomain(minLength, maxLength);
  }

  private String generateBrandDomain(int minLength, int maxLength) {
    // 组合品牌词汇
    if (random.nextBoolean() && BRAND_WORDS.size() > 1) {
      String word1 = BRAND_WORDS.get(random.nextInt(BRAND_WORDS.size()));
      String word2 = BRAND_WORDS.get(random.nextInt(BRAND_WORDS.size()));
      String combined = word1 + word2;

      if (combined.length() >= minLength && combined.length() <= maxLength) {
        return combined;
      }
    }

    // 单个品牌词汇
    List<String> validWords = new ArrayList<>();
    for (String word : BRAND_WORDS) {
      if (word.length() >= minLength && word.length() <= maxLength) {
        validWords.add(word);
      }
    }

    if (!validWords.isEmpty()) {
      return validWords.get(random.nextInt(validWords.size()));
    }

    return generateRandomDomain(minLength, maxLength);
  }

  private String generateDictionaryDomain(int minLength, int maxLength) {
    // 使用字典词汇
    List<String> validWords = new ArrayList<>();
    for (String word : DICTIONARY_WORDS) {
      if (word.length() >= minLength && word.length() <= maxLength) {
        validWords.add(word);
      }
    }

    if (!validWords.isEmpty()) {
      return validWords.get(random.nextInt(validWords.size()));
    }

    return generateRandomDomain(minLength, maxLength);
  }

  private String generateRandomDomain(int minLength, int maxLength) {
    int length = minLength + random.nextInt(maxLength - minLength + 1);
    StringBuilder domain = new StringBuilder();

    // 确保第一个字符是字母
    domain.append((char) ('a' + random.nextInt(26)));

    // 生成剩余字符
    for (int i = 1; i < length; i++) {
      if (random.nextDouble() < 0.1 && i > 1 && i < length - 1) {
        // 10%概率添加连字符（不在开头和结尾）
        domain.append('-');
      } else if (random.nextDouble() < 0.2) {
        // 20%概率添加数字
        domain.append(random.nextInt(10));
      } else {
        // 添加字母
        domain.append((char) ('a' + random.nextInt(26)));
      }
    }

    // 确保不以连字符结尾
    String result = domain.toString();
    if (result.endsWith("-")) {
      result = result.substring(0, result.length() - 1) + (char) ('a' + random.nextInt(26));
    }

    return result;
  }

  private String generateTopLevelDomain(String tld, boolean international) {
    if (!"ANY".equalsIgnoreCase(tld)) {
      return tld.toLowerCase();
    }

    // 选择TLD类别
    List<String> categories = new ArrayList<>(TLD_CATEGORIES.keySet());
    if (!international) {
      categories.remove("CHINESE"); // 如果不支持国际化，移除中文TLD
    }

    String category = categories.get(random.nextInt(categories.size()));
    List<String> tlds = TLD_CATEGORIES.get(category);

    return tlds.get(random.nextInt(tlds.size()));
  }

  private String extractTld(String domain) {
    int lastDot = domain.lastIndexOf('.');
    if (lastDot > 0 && lastDot < domain.length() - 1) {
      return domain.substring(lastDot + 1);
    }
    return "unknown";
  }

  private String extractSld(String domain) {
    // 移除子域名
    String withoutSubdomain = domain;
    String[] parts = domain.split("\\.");
    if (parts.length > 2) {
      // 假设最后两部分是SLD.TLD
      withoutSubdomain = parts[parts.length - 2] + "." + parts[parts.length - 1];
    }

    int lastDot = withoutSubdomain.lastIndexOf('.');
    if (lastDot > 0) {
      return withoutSubdomain.substring(0, lastDot);
    }
    return withoutSubdomain;
  }
}
