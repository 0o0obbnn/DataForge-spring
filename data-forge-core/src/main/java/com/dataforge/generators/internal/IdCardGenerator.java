package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.util.DataLoader;
import com.dataforge.validation.IdCardValidator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 身份证号码生成器。
 *
 * <p>生成符合中国大陆18位身份证号码规则的身份证号，支持大规模身份证生成。 支持地区代码、出生日期范围、性别等参数配置和权重选择。 通过配置文件管理行政区划数据，支持生成数十亿唯一身份证号。
 * 生成的身份证号会自动关联到上下文中，供其他字段使用。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class IdCardGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(IdCardGenerator.class);

  @Autowired private IdCardValidator idCardValidator;

  /** 行政区划数据文件路径。 */
  private static final String ADMINISTRATIVE_DIVISIONS_PATH = "data/administrative-divisions.txt";

  /** 缓存的行政区划数据。 */
  private volatile Map<String, RegionInfo> regionCodes;

  private volatile List<String> allRegionCodes;
  private volatile Map<String, List<String>> regionsByProvince;
  private volatile Map<String, List<String>> regionsByCity;

  /** Fallback地区代码映射（当文件加载失败时使用）。 */
  private static final Map<String, String> FALLBACK_REGION_CODES = new HashMap<>();

  static {
    // 初始化fallback数据
    FALLBACK_REGION_CODES.put("110101", "北京市东城区");
    FALLBACK_REGION_CODES.put("110102", "北京市西城区");
    FALLBACK_REGION_CODES.put("110105", "北京市朝阳区");
    FALLBACK_REGION_CODES.put("110106", "北京市丰台区");
    FALLBACK_REGION_CODES.put("110108", "北京市海淀区");
    FALLBACK_REGION_CODES.put("120101", "天津市和平区");
    FALLBACK_REGION_CODES.put("120102", "天津市河东区");
    FALLBACK_REGION_CODES.put("310101", "上海市黄浦区");
    FALLBACK_REGION_CODES.put("310104", "上海市徐汇区");
    FALLBACK_REGION_CODES.put("310105", "上海市长宁区");
    FALLBACK_REGION_CODES.put("500101", "重庆市万州区");
    FALLBACK_REGION_CODES.put("500102", "重庆市涪陵区");
    FALLBACK_REGION_CODES.put("330106", "浙江省杭州市西湖区");
    FALLBACK_REGION_CODES.put("440100", "广东省广州市");
    FALLBACK_REGION_CODES.put("440300", "广东省深圳市");
    FALLBACK_REGION_CODES.put("510100", "四川省成都市");
  }

  /** 地区信息类。 */
  private static class RegionInfo {
    final String code;
    final String province;
    final String city;
    final String district;

    RegionInfo(String code, String province, String city, String district, int weight) {
      this.code = code;
      this.province = province;
      this.city = city;
      this.district = district;
    }
  }

  @Override
  public String getType() {
    return "idcard";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 延迟加载数据
      ensureDataLoaded(config);

      // 从参数中获取地区代码
      String region = getStringParam(config, "region", null);

      // 从参数中获取出生日期范围
      String birthDateRange = getStringParam(config, "birth_date_range", "1980-01-01,2000-12-31");

      // 从参数中获取性别
      String gender = getStringParam(config, "gender", "ANY");

      // 从上下文中获取性别（优先级更高）
      String contextGender = context.get("gender", String.class).orElse(null);
      if (contextGender != null) {
        gender = contextGender;
      }

      // 从参数中获取是否生成有效身份证号
      boolean valid = getBooleanParam(config, "valid", true);

      if (!valid) {
        return generateInvalidIdCard();
      }

      String idCard = generateValidIdCard(region, birthDateRange, gender);

      // 将生成的身份证信息放入上下文
      putIdCardInfoToContext(context, idCard);

      return idCard;

    } catch (Exception e) {
      logger.error("Failed to generate ID card number", e);
      // 返回一个默认身份证号作为fallback
      return "11010119800101001X";
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
    if (regionCodes == null) {
      synchronized (this) {
        if (regionCodes == null) {
          loadData(config);
        }
      }
    }
  }

  /**
   * 加载行政区划数据。
   *
   * @param config 配置
   */
  private void loadData(FieldConfig config) {
    try {
      // 检查是否有自定义数据文件路径
      String customRegionsPath = getStringParam(config, "regions_file", null);

      List<String> lines;
      if (customRegionsPath != null) {
        lines = DataLoader.loadDataFromFile(customRegionsPath);
      } else {
        lines = DataLoader.loadDataFromResource(ADMINISTRATIVE_DIVISIONS_PATH);
      }

      regionCodes = new HashMap<>();
      regionsByProvince = new HashMap<>();
      regionsByCity = new HashMap<>();

      for (String line : lines) {
        String[] parts = line.split(":");
        if (parts.length >= 4) {
          String code = parts[0].trim();
          String province = parts[1].trim();
          String city = parts[2].trim();
          String district = parts[3].trim();
          int weight = parts.length > 4 ? parseWeight(parts[4].trim()) : 1;

          RegionInfo info = new RegionInfo(code, province, city, district, weight);
          regionCodes.put(code, info);

          regionsByProvince.computeIfAbsent(province, k -> new java.util.ArrayList<>()).add(code);
          regionsByCity.computeIfAbsent(city, k -> new java.util.ArrayList<>()).add(code);
        }
      }

      allRegionCodes = new java.util.ArrayList<>(regionCodes.keySet());

      // 如果加载失败，使用fallback数据
      if (regionCodes.isEmpty()) {
        initializeFallbackData();
      }

      logger.info(
          "Administrative division data loaded - Total regions: {}, Provinces: {}",
          regionCodes.size(),
          regionsByProvince.keySet().size());

    } catch (Exception e) {
      logger.error("Failed to load administrative division data, using fallback", e);
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
    regionCodes = new HashMap<>();
    regionsByProvince = new HashMap<>();
    regionsByCity = new HashMap<>();

    // 添加fallback数据
    for (Map.Entry<String, String> entry : FALLBACK_REGION_CODES.entrySet()) {
      String code = entry.getKey();
      String fullName = entry.getValue();

      // 简单解析省市区信息
      String[] parts = fullName.split("省|市");
      String province =
          parts.length > 0 ? parts[0] + (fullName.contains("省") ? "省" : "市") : fullName;
      String city = parts.length > 1 ? parts[1] : "";
      String district = parts.length > 2 ? parts[2] : "";

      RegionInfo info = new RegionInfo(code, province, city, district, 1);
      regionCodes.put(code, info);

      regionsByProvince.computeIfAbsent(province, k -> new java.util.ArrayList<>()).add(code);
      if (!city.isEmpty()) {
        regionsByCity.computeIfAbsent(city, k -> new java.util.ArrayList<>()).add(code);
      }
    }

    allRegionCodes = new java.util.ArrayList<>(regionCodes.keySet());
  }

  /**
   * 生成有效的身份证号码。
   *
   * @param region 地区代码
   * @param birthDateRange 出生日期范围
   * @param gender 性别
   * @return 有效的身份证号码
   */
  private String generateValidIdCard(String region, String birthDateRange, String gender) {
    // 1. 生成地区代码（前6位）
    String regionCode = selectRegionCode(region);

    // 2. 生成出生日期（第7-14位）
    LocalDate birthDate = generateBirthDate(birthDateRange);
    String birthDateStr = birthDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    // 3. 生成顺序码（第15-17位）
    String sequenceCode = generateSequenceCode(gender);

    // 4. 组合前17位
    String first17 = regionCode + birthDateStr + sequenceCode;

    // 5. 计算校验位（第18位）
    char checkCode = idCardValidator.calculateCheckCode(first17);

    String idCard = first17 + checkCode;

    // 验证生成的身份证号
    if (!idCardValidator.isValid(idCard)) {
      logger.error("Generated invalid ID card: {}", maskIdCard(idCard));
      // 重新生成
      return generateValidIdCard(region, birthDateRange, gender);
    }

    logger.debug(
        "Generated valid ID card: {} (region: {}, birth: {}, gender: {})",
        maskIdCard(idCard),
        regionCode,
        birthDate,
        gender);

    return idCard;
  }

  /**
   * 生成无效的身份证号码。
   *
   * @return 无效的身份证号码
   */
  private String generateInvalidIdCard() {
    int type = ThreadLocalRandom.current().nextInt(4);

    return switch (type) {
      case 0 -> generateWrongLengthIdCard();
      case 1 -> generateWrongChecksumIdCard();
      case 2 -> generateInvalidDateIdCard();
      default -> generateOtherInvalidIdCard();
    };
  }

  /**
   * 生成长度错误的身份证号。
   *
   * @return 长度错误的身份证号
   */
  private String generateWrongLengthIdCard() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    int length =
        random.nextBoolean()
            ? random.nextInt(10) + 5
            : // 5-14位
            random.nextInt(5) + 19; // 19-23位

    StringBuilder idCard = new StringBuilder();
    for (int i = 0; i < length; i++) {
      if (i == length - 1 && random.nextBoolean()) {
        idCard.append('X');
      } else {
        idCard.append(random.nextInt(10));
      }
    }

    return idCard.toString();
  }

  /**
   * 生成校验位错误的身份证号。
   *
   * @return 校验位错误的身份证号
   */
  private String generateWrongChecksumIdCard() {
    // 先生成一个有效的身份证号
    String validIdCard = generateValidIdCard(null, "1980-01-01,2000-12-31", "ANY");

    // 修改最后一位校验位
    StringBuilder invalidIdCard = new StringBuilder(validIdCard.substring(0, 17));
    char lastChar = validIdCard.charAt(17);

    if (lastChar == 'X') {
      invalidIdCard.append('0');
    } else if (lastChar == '0') {
      invalidIdCard.append('X');
    } else {
      int digit = Character.getNumericValue(lastChar);
      int wrongDigit = (digit + 1) % 10;
      invalidIdCard.append(wrongDigit);
    }

    return invalidIdCard.toString();
  }

  /**
   * 生成日期无效的身份证号。
   *
   * @return 日期无效的身份证号
   */
  private String generateInvalidDateIdCard() {
    String regionCode = selectRegionCode(null);

    // 生成无效日期
    String[] invalidDates = {
      "19800230", // 2月30日
      "19801301", // 13月
      "19800001", // 0月
      "19800100", // 0日
      "19800132", // 32日
      "20250101" // 未来日期
    };

    ThreadLocalRandom random = ThreadLocalRandom.current();
    String invalidDate = invalidDates[random.nextInt(invalidDates.length)];
    String sequenceCode = generateSequenceCode("ANY");
    String first17 = regionCode + invalidDate + sequenceCode;

    // 计算校验位（即使日期无效，校验位仍然正确）
    char checkCode = idCardValidator.calculateCheckCode(first17);

    return first17 + checkCode;
  }

  /**
   * 生成其他类型的无效身份证号。
   *
   * @return 其他无效身份证号
   */
  private String generateOtherInvalidIdCard() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    // 生成全0或全相同数字的身份证号
    if (random.nextBoolean()) {
      return "000000000000000000";
    } else {
      int digit = random.nextInt(10);
      return String.valueOf(digit).repeat(18);
    }
  }

  /**
   * 选择地区代码。
   *
   * @param region 指定的地区代码
   * @return 地区代码
   */
  private String selectRegionCode(String region) {
    if (region != null && !region.trim().isEmpty()) {
      String cleanRegion = region.trim();

      // 如果是6位数字，直接使用
      if (cleanRegion.matches("\\d{6}")) {
        return cleanRegion;
      }

      // 如果是省市名称，查找对应代码
      for (RegionInfo info : regionCodes.values()) {
        if (info.province.contains(cleanRegion)
            || info.city.contains(cleanRegion)
            || info.district.contains(cleanRegion)) {
          return info.code;
        }
      }

      logger.warn("Unknown region: {}, using random region", region);
    }

    // 随机选择一个地区代码
    return allRegionCodes.get(ThreadLocalRandom.current().nextInt(allRegionCodes.size()));
  }

  /**
   * 生成出生日期。
   *
   * @param birthDateRange 出生日期范围
   * @return 出生日期
   */
  private LocalDate generateBirthDate(String birthDateRange) {
    LocalDate startDate = LocalDate.of(1980, 1, 1);
    LocalDate endDate = LocalDate.of(2000, 12, 31);

    if (birthDateRange != null && !birthDateRange.trim().isEmpty()) {
      try {
        String[] parts = birthDateRange.split(",");
        if (parts.length == 2) {
          startDate = LocalDate.parse(parts[0].trim());
          endDate = LocalDate.parse(parts[1].trim());
        }
      } catch (DateTimeParseException e) {
        logger.warn("Invalid birth date range: {}, using default", birthDateRange);
      }
    }

    // 确保日期范围合理
    LocalDate minDate = LocalDate.of(1900, 1, 1);
    LocalDate maxDate = LocalDate.now().minusYears(1); // 至少1岁

    if (startDate.isBefore(minDate)) {
      startDate = minDate;
    }
    if (endDate.isAfter(maxDate)) {
      endDate = maxDate;
    }
    if (startDate.isAfter(endDate)) {
      startDate = endDate.minusYears(20);
    }

    // 随机生成日期
    long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    long randomDays = ThreadLocalRandom.current().nextLong(daysBetween + 1);

    return startDate.plusDays(randomDays);
  }

  /**
   * 生成顺序码。
   *
   * @param gender 性别
   * @return 3位顺序码
   */
  private String generateSequenceCode(String gender) {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    // 前两位随机
    int first = random.nextInt(10);
    int second = random.nextInt(10);

    // 第三位决定性别（奇数男性，偶数女性）
    int third;
    if ("MALE".equalsIgnoreCase(gender) || "M".equalsIgnoreCase(gender)) {
      // 男性：奇数
      third = random.nextInt(5) * 2 + 1; // 1, 3, 5, 7, 9
    } else if ("FEMALE".equalsIgnoreCase(gender) || "F".equalsIgnoreCase(gender)) {
      // 女性：偶数
      third = random.nextInt(5) * 2; // 0, 2, 4, 6, 8
    } else {
      // 随机性别
      third = random.nextInt(10);
    }

    return String.format("%d%d%d", first, second, third);
  }

  /**
   * 将身份证信息放入上下文。
   *
   * @param context 上下文
   * @param idCard 身份证号
   */
  private void putIdCardInfoToContext(DataForgeContext context, String idCard) {
    try {
      // 提取并存储相关信息
      LocalDate birthDate = idCardValidator.extractBirthDate(idCard);
      if (birthDate != null) {
        context.put("birthDate", birthDate);
        context.put("birth_date", birthDate.toString());

        // 计算年龄
        int age = java.time.Period.between(birthDate, LocalDate.now()).getYears();
        context.put("age", age);
      }

      String gender = idCardValidator.extractGender(idCard);
      if (gender != null) {
        context.put("gender", gender);
        context.put("gender_cn", "M".equals(gender) ? "男" : "女");
      }

      String regionCode = idCardValidator.extractRegionCode(idCard);
      if (regionCode != null) {
        context.put("regionCode", regionCode);
        RegionInfo regionInfo = regionCodes.get(regionCode);
        if (regionInfo != null) {
          context.put("region_name", regionInfo.province + regionInfo.city + regionInfo.district);
          context.put("province", regionInfo.province);
          context.put("city", regionInfo.city);
          context.put("district", regionInfo.district);
        } else {
          context.put("region_name", "未知地区");
        }
      }

    } catch (Exception e) {
      logger.warn("Failed to extract ID card info for context", e);
    }
  }

  /**
   * 掩码身份证号用于日志记录。
   *
   * @param idCard 原始身份证号
   * @return 掩码后的身份证号
   */
  private String maskIdCard(String idCard) {
    if (idCard == null || idCard.length() < 8) {
      return "****";
    }

    // 显示前4位和后4位，中间用*代替
    String prefix = idCard.substring(0, 4);
    String suffix = idCard.substring(idCard.length() - 4);
    int maskLength = idCard.length() - 8;
    String mask = "*".repeat(Math.max(0, maskLength));

    return prefix + mask + suffix;
  }

  public void setIdCardValidator(IdCardValidator idCardValidator) {
    this.idCardValidator = idCardValidator;
  }

  /**
   * 获取行政区划统计信息。
   *
   * @return 统计信息
   */
  public String getRegionStats() {
    ensureDataLoaded(null);

    StringBuilder stats = new StringBuilder();
    stats.append("Total regions: ").append(regionCodes.size()).append("\n");

    stats.append("Provinces: ").append(regionsByProvince.keySet().size()).append("\n");
    for (Map.Entry<String, List<String>> entry : regionsByProvince.entrySet()) {
      stats
          .append("  ")
          .append(entry.getKey())
          .append(": ")
          .append(entry.getValue().size())
          .append(" regions\n");
    }

    // 计算理论组合数
    // 每个地区代码 × 日期范围 × 1000个顺序码 = 总组合数
    long dateRange =
        java.time.temporal.ChronoUnit.DAYS.between(
            LocalDate.of(1900, 1, 1), LocalDate.now().minusYears(1));
    long totalCombinations = (long) regionCodes.size() * dateRange * 1000;

    stats.append("\nTotal possible combinations: ").append(String.format("%,d", totalCombinations));

    return stats.toString();
  }

  @Override
  public String getDescription() {
    return "Chinese ID card number generator - generates 18-digit ID cards with comprehensive"
        + " region, birth date and gender support";
  }
}
