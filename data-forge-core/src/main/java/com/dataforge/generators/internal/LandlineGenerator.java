package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 固话号码生成器
 *
 * <p>支持的参数： - region: 地区 (CN|US|UK|JP|ANY) - area_code: 区号 (指定区号或随机) - format: 输出格式
 * (STANDARD|INTERNATIONAL|COMPACT|PARENTHESES) - include_extension: 是否包含分机号 (true|false) -
 * extension_length: 分机号长度 (2-6) - number_length: 号码长度 (7-8)
 *
 * @author DataForge
 */
public class LandlineGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(LandlineGenerator.class);
  private static final Random random = new Random();

  // 输出格式枚举
  private enum LandlineFormat {
    STANDARD, // 标准格式：区号-号码
    INTERNATIONAL, // 国际格式：+国家码-区号-号码
    COMPACT, // 紧凑格式：区号号码（无分隔符）
    PARENTHESES // 括号格式：(区号) 号码
  }

  // 国家信息
  private static final Map<String, CountryInfo> COUNTRY_INFO = new HashMap<>();

  // 中国区号和对应城市
  private static final Map<String, String> CHINA_AREA_CODES = new HashMap<>();

  // 国家信息类
  private static class CountryInfo {
    final String countryCode;
    final Map<String, String> areaCodes; // 区号 -> 城市名
    final int[] numberLengths; // 支持的号码长度
    final String separator;

    CountryInfo(
        String countryCode, Map<String, String> areaCodes, int[] numberLengths, String separator) {
      this.countryCode = countryCode;
      this.areaCodes = areaCodes;
      this.numberLengths = numberLengths;
      this.separator = separator;
    }
  }

  static {
    initializeChinaAreaCodes();
    initializeCountryInfo();
  }

  private static void initializeChinaAreaCodes() {
    // 主要城市区号
    CHINA_AREA_CODES.put("010", "北京");
    CHINA_AREA_CODES.put("021", "上海");
    CHINA_AREA_CODES.put("022", "天津");
    CHINA_AREA_CODES.put("023", "重庆");
    CHINA_AREA_CODES.put("024", "沈阳");
    CHINA_AREA_CODES.put("025", "南京");
    CHINA_AREA_CODES.put("027", "武汉");
    CHINA_AREA_CODES.put("028", "成都");
    CHINA_AREA_CODES.put("029", "西安");

    // 省会城市区号
    CHINA_AREA_CODES.put("0311", "石家庄");
    CHINA_AREA_CODES.put("0351", "太原");
    CHINA_AREA_CODES.put("0371", "郑州");
    CHINA_AREA_CODES.put("0431", "长春");
    CHINA_AREA_CODES.put("0451", "哈尔滨");
    CHINA_AREA_CODES.put("0471", "呼和浩特");
    CHINA_AREA_CODES.put("0531", "济南");
    CHINA_AREA_CODES.put("0551", "合肥");
    CHINA_AREA_CODES.put("0571", "杭州");
    CHINA_AREA_CODES.put("0591", "福州");
    CHINA_AREA_CODES.put("0731", "长沙");
    CHINA_AREA_CODES.put("0771", "南宁");
    CHINA_AREA_CODES.put("0791", "南昌");
    CHINA_AREA_CODES.put("0851", "贵阳");
    CHINA_AREA_CODES.put("0871", "昆明");
    CHINA_AREA_CODES.put("0891", "拉萨");
    CHINA_AREA_CODES.put("0931", "兰州");
    CHINA_AREA_CODES.put("0951", "银川");
    CHINA_AREA_CODES.put("0971", "西宁");
    CHINA_AREA_CODES.put("0991", "乌鲁木齐");

    // 重要地级市区号
    CHINA_AREA_CODES.put("0411", "大连");
    CHINA_AREA_CODES.put("0512", "苏州");
    CHINA_AREA_CODES.put("0532", "青岛");
    CHINA_AREA_CODES.put("0574", "宁波");
    CHINA_AREA_CODES.put("0592", "厦门");
    CHINA_AREA_CODES.put("0755", "深圳");
    CHINA_AREA_CODES.put("0756", "珠海");
    CHINA_AREA_CODES.put("0760", "中山");
    CHINA_AREA_CODES.put("0769", "东莞");
    CHINA_AREA_CODES.put("020", "广州");
  }

  private static void initializeCountryInfo() {
    // 中国
    Map<String, String> usAreaCodes = new HashMap<>();
    usAreaCodes.put("212", "New York");
    usAreaCodes.put("213", "Los Angeles");
    usAreaCodes.put("312", "Chicago");
    usAreaCodes.put("415", "San Francisco");
    usAreaCodes.put("617", "Boston");
    usAreaCodes.put("202", "Washington DC");
    usAreaCodes.put("305", "Miami");
    usAreaCodes.put("713", "Houston");
    usAreaCodes.put("214", "Dallas");
    usAreaCodes.put("404", "Atlanta");

    COUNTRY_INFO.put("CN", new CountryInfo("+86", CHINA_AREA_CODES, new int[] {7, 8}, "-"));
    COUNTRY_INFO.put("US", new CountryInfo("+1", usAreaCodes, new int[] {7}, "-"));

    // 英国
    Map<String, String> ukAreaCodes = new HashMap<>();
    ukAreaCodes.put("20", "London");
    ukAreaCodes.put("121", "Birmingham");
    ukAreaCodes.put("131", "Edinburgh");
    ukAreaCodes.put("141", "Glasgow");
    ukAreaCodes.put("151", "Liverpool");
    ukAreaCodes.put("161", "Manchester");
    ukAreaCodes.put("113", "Leeds");
    ukAreaCodes.put("114", "Sheffield");
    ukAreaCodes.put("115", "Nottingham");
    ukAreaCodes.put("117", "Bristol");

    COUNTRY_INFO.put("UK", new CountryInfo("+44", ukAreaCodes, new int[] {6, 7}, " "));

    // 日本
    Map<String, String> jpAreaCodes = new HashMap<>();
    jpAreaCodes.put("3", "Tokyo");
    jpAreaCodes.put("6", "Osaka");
    jpAreaCodes.put("52", "Nagoya");
    jpAreaCodes.put("92", "Fukuoka");
    jpAreaCodes.put("11", "Sapporo");
    jpAreaCodes.put("22", "Sendai");
    jpAreaCodes.put("75", "Kyoto");
    jpAreaCodes.put("82", "Hiroshima");
    jpAreaCodes.put("95", "Niigata");
    jpAreaCodes.put("96", "Kumamoto");

    COUNTRY_INFO.put("JP", new CountryInfo("+81", jpAreaCodes, new int[] {8}, "-"));
  }

  @Override
  public String getType() {
    return "landline";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String region = config.getParam("region", String.class, "CN");
      String areaCode = config.getParam("area_code", String.class, null);
      String format = config.getParam("format", String.class, "STANDARD");
      boolean includeExtension =
          Boolean.parseBoolean(config.getParam("include_extension", String.class, "false"));
      int extensionLength =
          Integer.parseInt(config.getParam("extension_length", String.class, "3"));
      int numberLength = Integer.parseInt(config.getParam("number_length", String.class, "0"));

      // 生成固话号码
      String landlineNumber =
          generateLandlineNumber(
              region, areaCode, format, includeExtension, extensionLength, numberLength);

      // 将固话号码信息存入上下文
      context.put("landline_number", landlineNumber);
      context.put("landline_region", region);
      context.put("landline_area_code", extractAreaCode(landlineNumber, region));
      context.put("landline_city", getCityName(extractAreaCode(landlineNumber, region), region));

      logger.debug("Generated landline number: {}", landlineNumber);
      return landlineNumber;

    } catch (Exception e) {
      logger.error("Error generating landline number", e);
      return "010-12345678";
    }
  }

  private String generateLandlineNumber(
      String region,
      String areaCode,
      String format,
      boolean includeExtension,
      int extensionLength,
      int numberLength) {

    // 获取国家信息
    CountryInfo countryInfo = COUNTRY_INFO.get(region.toUpperCase());
    if (countryInfo == null) {
      countryInfo = COUNTRY_INFO.get("CN"); // 默认使用中国
    }

    // 确定区号
    String finalAreaCode = areaCode != null ? areaCode : selectRandomAreaCode(countryInfo);

    // 确定号码长度
    int finalNumberLength = numberLength > 0 ? numberLength : selectRandomNumberLength(countryInfo);

    // 生成号码主体
    String mainNumber = generateMainNumber(finalNumberLength);

    // 生成分机号
    String extension = includeExtension ? generateExtension(extensionLength) : null;

    // 应用格式
    return formatLandlineNumber(countryInfo, finalAreaCode, mainNumber, extension, format);
  }

  private String selectRandomAreaCode(CountryInfo countryInfo) {
    List<String> areaCodes = new ArrayList<>(countryInfo.areaCodes.keySet());
    return areaCodes.get(random.nextInt(areaCodes.size()));
  }

  private int selectRandomNumberLength(CountryInfo countryInfo) {
    int[] lengths = countryInfo.numberLengths;
    return lengths[random.nextInt(lengths.length)];
  }

  private String generateMainNumber(int length) {
    StringBuilder number = new StringBuilder();

    // 第一位不能是0
    number.append(1 + random.nextInt(9));

    // 生成剩余位数
    for (int i = 1; i < length; i++) {
      number.append(random.nextInt(10));
    }

    return number.toString();
  }

  private String generateExtension(int length) {
    StringBuilder extension = new StringBuilder();

    for (int i = 0; i < length; i++) {
      extension.append(random.nextInt(10));
    }

    return extension.toString();
  }

  private String formatLandlineNumber(
      CountryInfo countryInfo,
      String areaCode,
      String mainNumber,
      String extension,
      String format) {

    StringBuilder landlineNumber = new StringBuilder();
    LandlineFormat landlineFormat = parseLandlineFormat(format);

    switch (landlineFormat) {
      case INTERNATIONAL:
        landlineNumber.append(countryInfo.countryCode);
        landlineNumber.append(countryInfo.separator);
        landlineNumber.append(areaCode);
        landlineNumber.append(countryInfo.separator);
        landlineNumber.append(mainNumber);
        break;

      case COMPACT:
        landlineNumber.append(areaCode).append(mainNumber);
        break;

      case PARENTHESES:
        landlineNumber.append("(").append(areaCode).append(") ");
        landlineNumber.append(mainNumber);
        break;

      case STANDARD:
      default:
        landlineNumber.append(areaCode);
        landlineNumber.append(countryInfo.separator);
        landlineNumber.append(mainNumber);
        break;
    }

    // 添加分机号
    if (extension != null) {
      landlineNumber.append(" ext.").append(extension);
    }

    return landlineNumber.toString();
  }

  private LandlineFormat parseLandlineFormat(String format) {
    try {
      return LandlineFormat.valueOf(format.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Unknown landline format: {}. Using STANDARD.", format);
      return LandlineFormat.STANDARD;
    }
  }

  private String extractAreaCode(String landlineNumber, String region) {
    try {
      CountryInfo countryInfo = COUNTRY_INFO.get(region.toUpperCase());
      if (countryInfo == null) {
        return "unknown";
      }

      // 移除国家代码和格式字符
      String cleanNumber = landlineNumber;
      if (cleanNumber.startsWith(countryInfo.countryCode)) {
        cleanNumber = cleanNumber.substring(countryInfo.countryCode.length());
      }

      // 移除格式字符
      cleanNumber = cleanNumber.replaceAll("[^0-9]", "");

      // 根据区号长度提取
      for (String code : countryInfo.areaCodes.keySet()) {
        if (cleanNumber.startsWith(code)) {
          return code;
        }
      }

      return "unknown";
    } catch (Exception e) {
      return "unknown";
    }
  }

  private String getCityName(String areaCode, String region) {
    CountryInfo countryInfo = COUNTRY_INFO.get(region.toUpperCase());
    if (countryInfo != null && countryInfo.areaCodes.containsKey(areaCode)) {
      return countryInfo.areaCodes.get(areaCode);
    }
    return "unknown";
  }

  /** 验证固话号码格式 */
  public boolean validateLandlineNumber(String landlineNumber, String region) {
    if (landlineNumber == null || landlineNumber.isEmpty()) {
      return false;
    }

    CountryInfo countryInfo = COUNTRY_INFO.get(region.toUpperCase());
    if (countryInfo == null) {
      return false;
    }

    // 移除所有非数字字符（除了+号）
    String cleanNumber = landlineNumber.replaceAll("[^0-9+]", "");

    // 检查是否包含有效的区号
    for (String areaCode : countryInfo.areaCodes.keySet()) {
      if (cleanNumber.contains(areaCode)) {
        return true;
      }
    }

    return false;
  }

  /** 生成特定城市的固话号码 */
  public String generateCityLandline(String city, String region) {
    CountryInfo countryInfo = COUNTRY_INFO.get(region.toUpperCase());
    if (countryInfo == null) {
      return generateLandlineNumber(region, null, "STANDARD", false, 0, 0);
    }

    // 查找城市对应的区号
    String areaCode = null;
    for (Map.Entry<String, String> entry : countryInfo.areaCodes.entrySet()) {
      if (entry.getValue().equalsIgnoreCase(city)
          || entry.getValue().contains(city)
          || city.contains(entry.getValue())) {
        areaCode = entry.getKey();
        break;
      }
    }

    if (areaCode == null) {
      areaCode = selectRandomAreaCode(countryInfo);
    }

    int numberLength = selectRandomNumberLength(countryInfo);
    String mainNumber = generateMainNumber(numberLength);

    return formatLandlineNumber(countryInfo, areaCode, mainNumber, null, "STANDARD");
  }

  /** 生成企业固话号码（可能包含多个分机号） */
  public String generateCorporateLandline(String region, int extensionCount) {
    String baseLandline = generateLandlineNumber(region, null, "STANDARD", false, 0, 0);

    if (extensionCount <= 1) {
      return baseLandline + " ext." + generateExtension(3);
    }

    StringBuilder result = new StringBuilder(baseLandline);
    result.append(" (ext.");

    for (int i = 0; i < extensionCount; i++) {
      if (i > 0) {
        result.append(",");
      }
      result.append(generateExtension(3));
    }

    result.append(")");
    return result.toString();
  }
}
