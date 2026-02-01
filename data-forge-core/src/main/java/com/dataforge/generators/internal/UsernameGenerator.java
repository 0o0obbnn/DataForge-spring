package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.core.uniqueness.UniquenessFilter;
import com.dataforge.core.uniqueness.UniquenessFilterFactory;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 账号名/用户名生成器
 *
 * <p>支持功能： 1. 指定长度和字符集的账号名生成 2. 与姓名关联的智能用户名生成 3. 支持前缀和后缀 4. 支持唯一性保证 5. 支持黑名单过滤
 *
 * <p>参数配置： - length: 账号名长度范围 "min,max"（默认"6,16"） - chars: 字符集类型
 * ALPHANUMERIC|ALPHANUMERIC_SPECIAL|NUMERIC|ALPHA|CUSTOM（默认ALPHANUMERIC） - custom_chars:
 * 自定义字符集（当chars=CUSTOM时使用） - prefix: 可选的账号名前缀 - suffix: 可选的账号名后缀 - unique: 是否在生成批次中保证唯一性（默认true） -
 * link_name: 是否关联姓名（默认true） - name_style: 姓名关联风格 PINYIN|INITIALS|MIXED（默认MIXED） - blacklist:
 * 黑名单词汇，逗号分隔
 *
 * <p>关联字段： - name: 从上下文中获取姓名，生成基于姓名的用户名
 *
 * @author DataForge
 * @since 1.0.0
 */
public class UsernameGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger log = LoggerFactory.getLogger(UsernameGenerator.class);

  private static final String TYPE = "username";
  private static final String DEFAULT_LENGTH = "6,16";
  private static final String DEFAULT_CHARS = "ALPHANUMERIC";
  private static final boolean DEFAULT_UNIQUE = true;
  private static final boolean DEFAULT_LINK_NAME = true;
  private static final String DEFAULT_NAME_STYLE = "MIXED";

  // 字符集定义
  private static final String NUMERIC_CHARS = "0123456789";
  private static final String ALPHA_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String ALPHANUMERIC_CHARS = ALPHA_CHARS + NUMERIC_CHARS;
  private static final String SPECIAL_CHARS = "_-";
  private static final String ALPHANUMERIC_SPECIAL_CHARS = ALPHANUMERIC_CHARS + SPECIAL_CHARS;

  // 上下文键名
  private static final String CONTEXT_NAME = "name";

  // 唯一性过滤器（用于唯一性检查）
  // 使用ThreadLocal确保每个线程有独立的过滤器实例
  private static final ThreadLocal<UniquenessFilter> uniquenessFilter =
      ThreadLocal.withInitial(() -> UniquenessFilterFactory.create(1_000_000));

  // 默认黑名单
  private static final Set<String> DEFAULT_BLACKLIST =
      Set.of(
          "admin",
          "root",
          "administrator",
          "system",
          "test",
          "guest",
          "user",
          "null",
          "undefined",
          "password",
          "login",
          "register",
          "signin",
          "signup",
          "logout",
          "delete",
          "remove",
          "fuck",
          "shit",
          "damn",
          "hell",
          "sex",
          "porn",
          "xxx",
          "666",
          "888",
          "999");

  // 中文姓名拼音映射（简化版）
  private static final Map<String, String> PINYIN_MAP = new HashMap<>();

  static {
    // 常见姓氏拼音
    PINYIN_MAP.put("王", "wang");
    PINYIN_MAP.put("李", "li");
    PINYIN_MAP.put("张", "zhang");
    PINYIN_MAP.put("刘", "liu");
    PINYIN_MAP.put("陈", "chen");
    PINYIN_MAP.put("杨", "yang");
    PINYIN_MAP.put("赵", "zhao");
    PINYIN_MAP.put("黄", "huang");
    PINYIN_MAP.put("周", "zhou");
    PINYIN_MAP.put("吴", "wu");
    PINYIN_MAP.put("徐", "xu");
    PINYIN_MAP.put("孙", "sun");
    PINYIN_MAP.put("胡", "hu");
    PINYIN_MAP.put("朱", "zhu");
    PINYIN_MAP.put("高", "gao");
    PINYIN_MAP.put("林", "lin");
    PINYIN_MAP.put("何", "he");
    PINYIN_MAP.put("郭", "guo");
    PINYIN_MAP.put("马", "ma");
    PINYIN_MAP.put("罗", "luo");
    PINYIN_MAP.put("梁", "liang");

    // 常见名字拼音
    PINYIN_MAP.put("伟", "wei");
    PINYIN_MAP.put("芳", "fang");
    PINYIN_MAP.put("娜", "na");
    PINYIN_MAP.put("秀", "xiu");
    PINYIN_MAP.put("敏", "min");
    PINYIN_MAP.put("静", "jing");
    PINYIN_MAP.put("丽", "li");
    PINYIN_MAP.put("强", "qiang");
    PINYIN_MAP.put("磊", "lei");
    PINYIN_MAP.put("军", "jun");
    PINYIN_MAP.put("洋", "yang");
    PINYIN_MAP.put("勇", "yong");
    PINYIN_MAP.put("艳", "yan");
    PINYIN_MAP.put("杰", "jie");
    PINYIN_MAP.put("娟", "juan");
    PINYIN_MAP.put("涛", "tao");
    PINYIN_MAP.put("明", "ming");
    PINYIN_MAP.put("超", "chao");
    PINYIN_MAP.put("秀英", "xiuying");
    PINYIN_MAP.put("桂英", "guiying");
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    // 解析配置参数
    String lengthStr = getStringParam(config, "length", DEFAULT_LENGTH);
    String charsType = getStringParam(config, "chars", DEFAULT_CHARS);
    String customChars = getStringParam(config, "custom_chars", "");
    String prefix = getStringParam(config, "prefix", "");
    String suffix = getStringParam(config, "suffix", "");
    boolean unique = getBooleanParam(config, "unique", DEFAULT_UNIQUE);
    boolean linkName = getBooleanParam(config, "link_name", DEFAULT_LINK_NAME);
    String nameStyle = getStringParam(config, "name_style", DEFAULT_NAME_STYLE);
    String blacklistStr = getStringParam(config, "blacklist", "");

    // 解析长度范围
    int[] lengthRange = parseLengthRange(lengthStr);
    int minLength = lengthRange[0];
    int maxLength = lengthRange[1];

    // 构建字符集
    String charSet = buildCharSet(charsType, customChars);

    // 构建黑名单
    Set<String> blacklist = buildBlacklist(blacklistStr);

    // 生成用户名
    String username =
        generateUsername(
            minLength, maxLength, charSet, prefix, suffix, unique, linkName, nameStyle, blacklist,
            context);

    return username;
  }

  /** 解析长度范围 */
  private int[] parseLengthRange(String lengthStr) {
    try {
      if (lengthStr.contains(",")) {
        String[] parts = lengthStr.split(",");
        int min = Integer.parseInt(parts[0].trim());
        int max = Integer.parseInt(parts[1].trim());
        return new int[] {Math.max(1, min), Math.max(min, max)};
      } else {
        int length = Integer.parseInt(lengthStr.trim());
        return new int[] {Math.max(1, length), Math.max(1, length)};
      }
    } catch (Exception e) {
      log.warn("Invalid length parameter: {}. Using default.", lengthStr);
      return new int[] {6, 16};
    }
  }

  /** 构建字符集 */
  private String buildCharSet(String charsType, String customChars) {
    switch (charsType.toUpperCase()) {
      case "NUMERIC":
        return NUMERIC_CHARS;
      case "ALPHA":
        return ALPHA_CHARS;
      case "ALPHANUMERIC":
        return ALPHANUMERIC_CHARS;
      case "ALPHANUMERIC_SPECIAL":
        return ALPHANUMERIC_SPECIAL_CHARS;
      case "CUSTOM":
        return customChars.isEmpty() ? ALPHANUMERIC_CHARS : customChars;
      default:
        log.warn("Unknown chars type: {}. Using ALPHANUMERIC.", charsType);
        return ALPHANUMERIC_CHARS;
    }
  }

  /** 构建黑名单 */
  private Set<String> buildBlacklist(String blacklistStr) {
    Set<String> blacklist = new HashSet<>(DEFAULT_BLACKLIST);

    if (!blacklistStr.isEmpty()) {
      String[] words = blacklistStr.split(",");
      for (String word : words) {
        String trimmed = word.trim().toLowerCase();
        if (!trimmed.isEmpty()) {
          blacklist.add(trimmed);
        }
      }
    }

    return blacklist;
  }

  /** 生成用户名 */
  private String generateUsername(
      int minLength,
      int maxLength,
      String charSet,
      String prefix,
      String suffix,
      boolean unique,
      boolean linkName,
      String nameStyle,
      Set<String> blacklist,
      DataForgeContext context) {

    ThreadLocalRandom random = ThreadLocalRandom.current();
    UniquenessFilter filter = uniquenessFilter.get();
    String username;
    int attempts = 0;
    int maxAttempts = unique ? 100 : 1;

    do {
      attempts++;

      // 尝试基于姓名生成
      if (linkName && attempts <= 10) {
        username =
            generateNameBasedUsername(
                minLength, maxLength, charSet, prefix, suffix, nameStyle, context);
        if (username != null) {
          if (!isBlacklisted(username, blacklist) && (!unique || !filter.mightContain(username))) {
            break;
          }
        }
      }

      // 随机生成
      username = generateRandomUsername(minLength, maxLength, charSet, prefix, suffix, random);

    } while ((isBlacklisted(username, blacklist) || (unique && filter.mightContain(username)))
        && attempts < maxAttempts);

    // 记录生成的用户名（用于唯一性检查）
    if (unique) {
      boolean added = filter.put(username);
      if (!added && log.isDebugEnabled()) {
        log.debug("Username '{}' might be duplicate (filter collision)", username);
      }
    }

    // 定期输出统计信息
    if (unique && filter.size() % 100000 == 0) {
      log.info("Username generation statistics: {}", filter.getStatistics());
    }

    return username;
  }

  /** 基于姓名生成用户名 */
  private String generateNameBasedUsername(
      int minLength,
      int maxLength,
      String charSet,
      String prefix,
      String suffix,
      String nameStyle,
      DataForgeContext context) {

    String name = context.get(CONTEXT_NAME, String.class).orElse(null);
    if (name == null || name.trim().isEmpty()) {
      return null;
    }

    ThreadLocalRandom random = ThreadLocalRandom.current();
    StringBuilder username = new StringBuilder();

    // 添加前缀
    if (!prefix.isEmpty()) {
      username.append(prefix);
    }

    // 处理姓名
    String nameBase = processName(name, nameStyle);
    if (nameBase.isEmpty()) {
      return null;
    }

    username.append(nameBase);

    // 添加随机数字或字符
    int remainingLength = maxLength - username.length() - suffix.length();
    if (remainingLength > 0) {
      int addLength = random.nextInt(1, Math.min(remainingLength + 1, 5));
      for (int i = 0; i < addLength; i++) {
        if (random.nextBoolean() && charSet.contains("0")) {
          // 添加数字
          username.append(random.nextInt(10));
        } else {
          // 添加字符
          char c = charSet.charAt(random.nextInt(charSet.length()));
          username.append(c);
        }
      }
    }

    // 添加后缀
    if (!suffix.isEmpty()) {
      username.append(suffix);
    }

    // 长度检查
    String result = username.toString();
    if (result.length() < minLength || result.length() > maxLength) {
      return null;
    }

    return result;
  }

  /** 处理姓名 */
  private String processName(String name, String nameStyle) {
    if (name == null || name.trim().isEmpty()) {
      return "";
    }

    name = name.trim();

    // 如果是英文名，直接处理
    if (name.matches("[a-zA-Z\\s]+")) {
      return processEnglishName(name, nameStyle);
    }

    // 如果是中文名，转换为拼音
    return processChineseName(name, nameStyle);
  }

  /** 处理英文姓名 */
  private String processEnglishName(String name, String nameStyle) {
    String[] parts = name.split("\\s+");
    ThreadLocalRandom random = ThreadLocalRandom.current();

    switch (nameStyle.toUpperCase()) {
      case "INITIALS":
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
          if (!part.isEmpty()) {
            initials.append(Character.toLowerCase(part.charAt(0)));
          }
        }
        return initials.toString();

      case "PINYIN":
        // 对于英文名，PINYIN等同于全名
        return name.toLowerCase().replaceAll("\\s+", "");

      case "MIXED":
      default:
        if (parts.length >= 2) {
          // 随机选择：首字母+姓氏 或 名字+姓氏首字母
          if (random.nextBoolean()) {
            return Character.toLowerCase(parts[0].charAt(0))
                + parts[parts.length - 1].toLowerCase();
          } else {
            return parts[0].toLowerCase()
                + Character.toLowerCase(parts[parts.length - 1].charAt(0));
          }
        } else {
          return parts[0].toLowerCase();
        }
    }
  }

  /** 处理中文姓名 */
  private String processChineseName(String name, String nameStyle) {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    switch (nameStyle.toUpperCase()) {
      case "INITIALS":
        StringBuilder initials = new StringBuilder();
        for (char c : name.toCharArray()) {
          String pinyin = PINYIN_MAP.get(String.valueOf(c));
          if (pinyin != null && !pinyin.isEmpty()) {
            initials.append(pinyin.charAt(0));
          }
        }
        return initials.toString();

      case "PINYIN":
        StringBuilder fullPinyin = new StringBuilder();
        for (char c : name.toCharArray()) {
          String pinyin = PINYIN_MAP.get(String.valueOf(c));
          if (pinyin != null) {
            fullPinyin.append(pinyin);
          }
        }
        return fullPinyin.toString();

      case "MIXED":
      default:
        if (name.length() >= 2) {
          // 随机选择：姓氏拼音+名字首字母 或 姓氏首字母+名字拼音
          String surname = String.valueOf(name.charAt(0));
          String givenName = name.substring(1);

          String surnamePinyin = PINYIN_MAP.get(surname);
          if (surnamePinyin == null) surnamePinyin = surname;

          if (random.nextBoolean()) {
            // 姓氏拼音+名字首字母
            StringBuilder result = new StringBuilder(surnamePinyin);
            for (char c : givenName.toCharArray()) {
              String pinyin = PINYIN_MAP.get(String.valueOf(c));
              if (pinyin != null && !pinyin.isEmpty()) {
                result.append(pinyin.charAt(0));
              }
            }
            return result.toString();
          } else {
            // 姓氏首字母+名字拼音
            StringBuilder result = new StringBuilder();
            result.append(surnamePinyin.charAt(0));
            for (char c : givenName.toCharArray()) {
              String pinyin = PINYIN_MAP.get(String.valueOf(c));
              if (pinyin != null) {
                result.append(pinyin);
              }
            }
            return result.toString();
          }
        } else {
          String pinyin = PINYIN_MAP.get(name);
          return pinyin != null ? pinyin : name;
        }
    }
  }

  /** 生成随机用户名 */
  private String generateRandomUsername(
      int minLength,
      int maxLength,
      String charSet,
      String prefix,
      String suffix,
      ThreadLocalRandom random) {

    int baseLength = random.nextInt(minLength, maxLength + 1) - prefix.length() - suffix.length();
    baseLength = Math.max(1, baseLength);

    StringBuilder username = new StringBuilder();
    username.append(prefix);

    // 确保第一个字符是字母（如果字符集包含字母）
    String alphaChars = charSet.replaceAll("[^a-zA-Z]", "");
    if (!alphaChars.isEmpty()) {
      username.append(alphaChars.charAt(random.nextInt(alphaChars.length())));
      baseLength--;
    }

    // 生成剩余字符
    for (int i = 0; i < baseLength; i++) {
      username.append(charSet.charAt(random.nextInt(charSet.length())));
    }

    username.append(suffix);

    return username.toString();
  }

  /** 检查是否在黑名单中 */
  private boolean isBlacklisted(String username, Set<String> blacklist) {
    String lowerUsername = username.toLowerCase();

    for (String blackWord : blacklist) {
      if (lowerUsername.contains(blackWord)) {
        return true;
      }
    }

    return false;
  }
}
