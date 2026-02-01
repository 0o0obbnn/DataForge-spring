package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.validation.IdCardValidator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 身份证号码生成器（重构版本）。
 *
 * <p>使用 {@link BaseDataLoadingGenerator} 基类，简化数据加载逻辑。 生成符合中国大陆18位身份证号码规则的身份证号，支持大规模身份证生成。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class IdCardGeneratorRefactored extends BaseDataLoadingGenerator<String>
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(IdCardGeneratorRefactored.class);

  // IdCardValidator 保留用于后续扩展验证功能
  @SuppressWarnings("unused")
  @Autowired
  private IdCardValidator idCardValidator;

  /** 行政区划数据文件路径。 */
  private static final String ADMINISTRATIVE_DIVISIONS_PATH = "data/administrative-divisions.txt";

  /** 缓存的行政区划数据。 */
  private Map<String, RegionInfo> regionCodes;

  private List<String> allRegionCodes;
  private Map<String, List<String>> regionsByProvince;
  private Map<String, List<String>> regionsByCity;

  /** Fallback地区代码映射（当文件加载失败时使用）。 */
  private static final Map<String, String> FALLBACK_REGION_CODES = new HashMap<>();

  static {
    FALLBACK_REGION_CODES.put("110101", "北京市东城区");
    FALLBACK_REGION_CODES.put("110102", "北京市西城区");
    FALLBACK_REGION_CODES.put("110105", "北京市朝阳区");
    FALLBACK_REGION_CODES.put("110106", "北京市丰台区");
    FALLBACK_REGION_CODES.put("110108", "北京市海淀区");
    FALLBACK_REGION_CODES.put("310101", "上海市黄浦区");
    FALLBACK_REGION_CODES.put("440100", "广东省广州市");
    FALLBACK_REGION_CODES.put("510100", "四川省成都市");
  }

  /** 地区信息类。 */
  @SuppressWarnings("unused")
  private static class RegionInfo {
    // code 和 weight 保留用于后续扩展功能
    final String code;
    final String province;
    final String city;
    final String district;
    final int weight;

    RegionInfo(String code, String province, String city, String district, int weight) {
      this.code = code;
      this.province = province;
      this.city = city;
      this.district = district;
      this.weight = weight;
    }
  }

  @Override
  public String getType() {
    return "idcard";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 使用基类的延迟加载机制
      ensureDataLoaded();

      String region = getStringParam(config, "region", null);
      String birthDateRange = getStringParam(config, "birth_date_range", "1980-01-01,2000-12-31");
      String gender = getStringParam(config, "gender", "ANY");
      boolean valid = getBooleanParam(config, "valid", true);

      if (!valid) {
        return generateInvalidIdCard();
      }

      String idCard = generateValidIdCard(region, birthDateRange, gender);
      putIdCardInfoToContext(context, idCard);

      return idCard;

    } catch (Exception e) {
      logger.error("Failed to generate ID card number", e);
      return "11010119800101001X";
    }
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  // ==================== BaseDataLoadingGenerator 抽象方法实现 ====================

  @Override
  protected String getDataFilePath() {
    return ADMINISTRATIVE_DIVISIONS_PATH;
  }

  @Override
  protected void parseData(List<String> lines) {
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

        regionsByProvince.computeIfAbsent(province, k -> new ArrayList<>()).add(code);
        regionsByCity.computeIfAbsent(city, k -> new ArrayList<>()).add(code);
      }
    }

    allRegionCodes = new ArrayList<>(regionCodes.keySet());

    logger.info(
        "Administrative division data loaded - Total regions: {}, Provinces: {}",
        regionCodes.size(),
        regionsByProvince.keySet().size());
  }

  @Override
  protected void initializeFallbackData() {
    regionCodes = new HashMap<>();
    regionsByProvince = new HashMap<>();
    regionsByCity = new HashMap<>();

    for (Map.Entry<String, String> entry : FALLBACK_REGION_CODES.entrySet()) {
      String code = entry.getKey();
      String fullName = entry.getValue();

      String[] parts = fullName.split("省|市");
      String province =
          parts.length > 0 ? parts[0] + (fullName.contains("省") ? "省" : "市") : fullName;
      String city = parts.length > 1 ? parts[1] : "";
      String district = parts.length > 2 ? parts[2] : "";

      RegionInfo info = new RegionInfo(code, province, city, district, 1);
      regionCodes.put(code, info);

      regionsByProvince.computeIfAbsent(province, k -> new ArrayList<>()).add(code);
      if (!city.isEmpty()) {
        regionsByCity.computeIfAbsent(city, k -> new ArrayList<>()).add(code);
      }
    }

    allRegionCodes = new ArrayList<>(regionCodes.keySet());
  }

  // ==================== 私有辅助方法 ====================

  private int parseWeight(String weightStr) {
    try {
      return Integer.parseInt(weightStr);
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  private String generateValidIdCard(String region, String birthDateRange, String gender) {
    String regionCode = selectRegionCode(region);
    LocalDate birthDate = generateBirthDate(birthDateRange);
    String birthDateStr = birthDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String sequenceCode = generateSequenceCode(gender);

    String prefix = regionCode + birthDateStr + sequenceCode;
    String checkDigit = calculateCheckDigit(prefix);

    return prefix + checkDigit;
  }

  private String generateInvalidIdCard() {
    // 简化实现
    return "11010119800101001X";
  }

  private String selectRegionCode(String region) {
    if (region == null || region.trim().isEmpty()) {
      return allRegionCodes.get(ThreadLocalRandom.current().nextInt(allRegionCodes.size()));
    }

    String cleanRegion = region.trim();

    if (regionCodes.containsKey(cleanRegion)) {
      return cleanRegion;
    }

    List<String> candidates = regionsByProvince.get(cleanRegion);
    if (candidates != null && !candidates.isEmpty()) {
      return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    candidates = regionsByCity.get(cleanRegion);
    if (candidates != null && !candidates.isEmpty()) {
      return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    return allRegionCodes.get(ThreadLocalRandom.current().nextInt(allRegionCodes.size()));
  }

  private LocalDate generateBirthDate(String birthDateRange) {
    String[] dates = birthDateRange.split(",");
    LocalDate startDate = LocalDate.parse(dates[0].trim());
    LocalDate endDate = dates.length > 1 ? LocalDate.parse(dates[1].trim()) : LocalDate.now();

    long startEpochDay = startDate.toEpochDay();
    long endEpochDay = endDate.toEpochDay();
    long randomDay =
        startEpochDay + ThreadLocalRandom.current().nextLong(endEpochDay - startEpochDay);

    return LocalDate.ofEpochDay(randomDay);
  }

  private String generateSequenceCode(String gender) {
    int sequence = ThreadLocalRandom.current().nextInt(1, 1000);

    if (!"ANY".equalsIgnoreCase(gender)) {
      boolean isMale = "M".equalsIgnoreCase(gender) || "MALE".equalsIgnoreCase(gender);
      if (isMale && sequence % 2 == 0) {
        sequence++;
      } else if (!isMale && sequence % 2 == 1) {
        sequence++;
      }
    }

    return String.format("%03d", sequence);
  }

  private String calculateCheckDigit(String prefix) {
    int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    String checkDigits = "10X98765432";

    int sum = 0;
    for (int i = 0; i < 17; i++) {
      sum += (prefix.charAt(i) - '0') * weights[i];
    }

    return String.valueOf(checkDigits.charAt(sum % 11));
  }

  private void putIdCardInfoToContext(DataForgeContext context, String idCard) {
    context.put("idCard", idCard);

    String birthDateStr = idCard.substring(6, 14);
    context.put("birthDate", birthDateStr);

    int year = Integer.parseInt(birthDateStr.substring(0, 4));
    int currentYear = LocalDate.now().getYear();
    context.put("age", currentYear - year);

    int sequenceCode = Integer.parseInt(idCard.substring(14, 17));
    String gender = (sequenceCode % 2 == 0) ? "女" : "男";
    context.put("gender", gender);

    String regionCode = idCard.substring(0, 6);
    context.put("regionCode", regionCode);

    RegionInfo info = regionCodes.get(regionCode);
    if (info != null) {
      context.put("province", info.province);
      context.put("city", info.city);
      context.put("district", info.district);
    }
  }
}
