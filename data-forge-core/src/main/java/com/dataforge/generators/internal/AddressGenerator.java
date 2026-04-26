package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.util.DataLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 地址生成器
 *
 * <p>支持功能： 1. 基于行政区划数据的层级地址生成 2. 与身份证号的地区信息关联 3. 支持不同详细程度的地址 4. 支持自定义街道和小区名称
 *
 * <p>参数配置： - country: 国家代码（默认CN） - province: 指定省份名称或代码 - city: 指定城市名称或代码 - district: 指定区县名称或代码 -
 * detail_level: 详细程度 PROVINCE|CITY|DISTRICT|STREET|COMMUNITY|FULL（默认FULL） - include_zipcode:
 * 是否包含邮编（默认true） - link_idcard: 是否关联身份证号（默认true）
 *
 * <p>关联字段： - idcard: 从身份证号中提取地区代码 - region_code: 从上下文中获取地区代码
 *
 * @author DataForge
 * @since 1.0.0
 */
public class AddressGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger log = LoggerFactory.getLogger(AddressGenerator.class);

  private static final String TYPE = "address";
  private static final String DEFAULT_DETAIL_LEVEL = "FULL";
  private static final boolean DEFAULT_INCLUDE_ZIPCODE = true;
  private static final boolean DEFAULT_LINK_IDCARD = true;

  // 详细程度枚举
  public enum DetailLevel {
    PROVINCE,
    CITY,
    DISTRICT,
    STREET,
    COMMUNITY,
    FULL
  }

  // 上下文键名
  private static final String CONTEXT_ID_CARD = "idcard";
  private static final String CONTEXT_REGION_CODE = "region_code";

  // 数据缓存
  private static volatile Map<String, AdministrativeDivision> divisionsCache;
  private static volatile List<String> streetNames;
  private static volatile List<String> communityNames;
  private static volatile List<String> buildingNames;

  // 行政区划数据结构
  public static class AdministrativeDivision {
    private final String code;
    private final String name;
    private final String level;
    private final String parentCode;
    private final String zipCode;

    public AdministrativeDivision(
        String code, String name, String level, String parentCode, String zipCode) {
      this.code = code;
      this.name = name;
      this.level = level;
      this.parentCode = parentCode;
      this.zipCode = zipCode;
    }

    // Getters
    public String getCode() {
      return code;
    }

    public String getName() {
      return name;
    }

    public String getLevel() {
      return level;
    }

    public String getParentCode() {
      return parentCode;
    }

    public String getZipCode() {
      return zipCode;
    }
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
    String province = getStringParam(config, "province", null);
    String city = getStringParam(config, "city", null);
    String district = getStringParam(config, "district", null);
    String detailLevelStr = getStringParam(config, "detail_level", DEFAULT_DETAIL_LEVEL);
    boolean includeZipcode = getBooleanParam(config, "include_zipcode", DEFAULT_INCLUDE_ZIPCODE);
    boolean linkIdCard = getBooleanParam(config, "link_idcard", DEFAULT_LINK_IDCARD);

    // 解析详细程度
    DetailLevel detailLevel;
    try {
      detailLevel = DetailLevel.valueOf(detailLevelStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("Invalid detail level: {}. Using FULL.", detailLevelStr);
      detailLevel = DetailLevel.FULL;
    }

    // 确保数据已加载
    ensureDataLoaded();

    // 确定地区代码
    String regionCode =
        determineRegionCode(province, city, district, linkIdCard, context, detailLevel);

    // 生成地址
    return generateAddress(regionCode, detailLevel, includeZipcode);
  }

  /** 确保数据已加载 */
  private void ensureDataLoaded() {
    if (divisionsCache == null) {
      synchronized (AddressGenerator.class) {
        if (divisionsCache == null) {
          loadAdministrativeDivisions();
          loadAddressComponents();
        }
      }
    }
  }

  /** 加载行政区划数据 */
  private void loadAdministrativeDivisions() {
    try {
      List<String> lines = DataLoader.loadRawDataFromResource("data/administrative-divisions.txt");
      Map<String, AdministrativeDivision> divisions = new HashMap<>();

      log.debug("Processing {} lines from administrative-divisions.txt", lines.size());
      int processedLines = 0;

      for (String line : lines) {
        if (line.trim().isEmpty() || line.startsWith("#")) {
          continue;
        }

        String[] parts = line.split(":");
        if (parts.length >= 4) {
          try {
            String code = parts[0].trim();
            String province = parts[1].trim();
            String city = parts[2].trim();
            String district = parts[3].trim();
            processedLines++;

            if (processedLines <= 5) {
              log.debug(
                  "Processing line {}: code={}, province={}, city={}, district={}",
                  processedLines,
                  code,
                  province,
                  city,
                  district);
            }

            // 创建省级记录
            if (!province.isEmpty()) {
              String provinceCode = code.substring(0, 2) + "0000";
              if (!divisions.containsKey(provinceCode)) {
                divisions.put(
                    provinceCode,
                    new AdministrativeDivision(provinceCode, province, "PROVINCE", "", ""));
                if (processedLines <= 5) {
                  log.debug("Added province: {} -> {}", provinceCode, province);
                }
              }
            }

            // 创建市级记录
            if (!city.isEmpty() && !city.equals(province)) {
              String cityCode = code.substring(0, 4) + "00";
              if (!divisions.containsKey(cityCode)) {
                String parentCode = code.substring(0, 2) + "0000";
                divisions.put(
                    cityCode, new AdministrativeDivision(cityCode, city, "CITY", parentCode, ""));
              }
            }

            // 创建区县级记录
            if (!district.isEmpty()) {
              String parentCode;
              if (city.equals(province)) {
                // 直辖市情况：区直接属于市（如北京市朝阳区的parent是北京市）
                parentCode = code.substring(0, 2) + "0000";
              } else {
                // 普通省市情况：区属于市
                parentCode = code.substring(0, 4) + "00";
              }
              divisions.put(
                  code, new AdministrativeDivision(code, district, "DISTRICT", parentCode, ""));
            }
          } catch (Exception e) {
            log.error("Error processing line: {}", line, e);
          }
        } else {
          log.warn("Invalid line format (expected >= 4 parts, got {}): {}", parts.length, line);
        }
      }

      log.info("Processed {} lines, created {} divisions", processedLines, divisions.size());
      divisionsCache = divisions;
      log.info("Administrative division data loaded - Total regions: {}", divisions.size());

    } catch (Exception e) {
      log.error("Failed to load administrative division data", e);
      divisionsCache = new HashMap<>();
    }
  }

  /** 加载地址组件数据 */
  private void loadAddressComponents() {
    // 加载街道名称
    streetNames =
        Arrays.asList(
            "人民路", "解放路", "中山路", "建设路", "胜利路", "和平路", "友谊路", "光明路", "新华路", "文化路", "学府路", "科技路",
            "创业路", "发展路", "繁荣路", "幸福路", "安康路", "健康路", "长寿路", "吉祥路", "如意路", "顺心路", "美好路", "希望路",
            "未来路", "梦想路", "青春路", "活力路", "朝阳路", "向阳路", "东风路", "春风路", "南风路", "西风路", "北风路", "海风路",
            "山风路", "清风路", "和风路", "暖风路");

    // 加载小区名称
    communityNames =
        Arrays.asList(
            "阳光花园", "绿色家园", "幸福家园", "温馨家园", "和谐家园", "美好家园", "舒适家园", "宁静家园", "春天花园", "夏日花园", "秋韵花园",
            "冬雪花园", "四季花园", "百花园", "玫瑰园", "牡丹园", "桂花园", "梅花园", "兰花园", "菊花园", "荷花园", "樱花园", "桃花园",
            "杏花园", "金桂小区", "银桂小区", "丹桂小区", "月桂小区", "桂花小区", "梧桐小区", "银杏小区", "柳树小区", "松柏小区", "竹林小区",
            "梅园小区", "兰园小区", "菊园小区", "荷园小区", "莲花小区", "水仙小区", "紫薇小区", "海棠小区", "茉莉小区", "玉兰小区", "丁香小区",
            "薰衣草小区", "向日葵小区", "康乃馨小区");

    // 加载建筑名称
    buildingNames =
        Arrays.asList(
            "栋", "号楼", "座", "幢", "单元", "区", "院", "苑", "轩", "阁", "居", "庭", "府", "邸", "宅", "舍");

    log.info(
        "Address component data loaded - Streets: {}, Communities: {}, Buildings: {}",
        streetNames.size(),
        communityNames.size(),
        buildingNames.size());
  }

  /** 确定地区代码 */
  private String determineRegionCode(
      String province,
      String city,
      String district,
      boolean linkIdCard,
      DataForgeContext context,
      DetailLevel detailLevel) {

    // 1. 如果指定了具体地区参数
    if (district != null) {
      String code = findRegionCode(district);
      if (code != null) return code;
    }
    if (city != null) {
      String code = findRegionCode(city);
      if (code != null) return code;
    }
    if (province != null) {
      String code = findRegionCode(province);
      if (code != null) return code;
    }

    // 2. 尝试从身份证号中提取地区代码
    if (linkIdCard) {
      String regionCodeFromIdCard = getRegionCodeFromIdCard(context);
      if (regionCodeFromIdCard != null) {
        log.debug("Using region code from ID card: {}", regionCodeFromIdCard);
        return regionCodeFromIdCard;
      }
    }

    // 3. 从上下文中获取地区代码
    String regionCode = context.get(CONTEXT_REGION_CODE, String.class).orElse(null);
    if (regionCode != null) {
      return regionCode;
    }

    // 4. 根据详细程度随机选择适当级别的地区
    return getRandomRegionCodeByLevel(detailLevel);
  }

  /** 查找地区代码 */
  private String findRegionCode(String nameOrCode) {
    if (nameOrCode == null || nameOrCode.trim().isEmpty()) {
      return null;
    }

    // 如果是6位数字，直接作为代码使用
    if (nameOrCode.matches("\\d{6}")) {
      return nameOrCode;
    }

    // 按名称查找
    for (AdministrativeDivision division : divisionsCache.values()) {
      if (division.getName().equals(nameOrCode) || division.getName().contains(nameOrCode)) {
        return division.getCode();
      }
    }

    return null;
  }

  /** 从身份证号中提取地区代码 */
  private String getRegionCodeFromIdCard(DataForgeContext context) {
    String idCard = context.get(CONTEXT_ID_CARD, String.class).orElse(null);
    if (idCard != null && idCard.length() >= 6) {
      return idCard.substring(0, 6);
    }
    return null;
  }

  /** 随机选择地区代码 */
  private String getRandomRegionCode() {
    if (divisionsCache.isEmpty()) {
      return "110101"; // 默认北京东城区
    }

    List<String> codes = new ArrayList<>(divisionsCache.keySet());
    ThreadLocalRandom random = ThreadLocalRandom.current();
    return codes.get(random.nextInt(codes.size()));
  }

  /** 根据详细程度随机选择适当级别的地区代码 */
  private String getRandomRegionCodeByLevel(DetailLevel detailLevel) {
    if (divisionsCache.isEmpty()) {
      return "110101"; // 默认北京东城区
    }

    List<String> codes = new ArrayList<>();
    ThreadLocalRandom random = ThreadLocalRandom.current();

    switch (detailLevel) {
      case PROVINCE:
        // 选择省级代码 (后4位为0000)
        for (String code : divisionsCache.keySet()) {
          if (code.endsWith("0000")) {
            codes.add(code);
          }
        }
        break;
      case CITY:
        // 选择市级代码 (后2位为00，但不是0000)
        for (String code : divisionsCache.keySet()) {
          if (code.endsWith("00") && !code.endsWith("0000")) {
            codes.add(code);
          }
        }
        break;
      default:
        // 其他级别选择区县级代码 (不以00结尾)
        for (String code : divisionsCache.keySet()) {
          if (!code.endsWith("00")) {
            codes.add(code);
          }
        }
        break;
    }

    if (codes.isEmpty()) {
      // 如果没有找到对应级别的代码，回退到随机选择
      return getRandomRegionCode();
    }

    return codes.get(random.nextInt(codes.size()));
  }

  /** 生成地址 */
  private String generateAddress(
      String regionCode, DetailLevel detailLevel, boolean includeZipcode) {
    StringBuilder address = new StringBuilder();
    ThreadLocalRandom random = ThreadLocalRandom.current();

    // 构建层级地址
    List<String> addressParts = buildAddressParts(regionCode);

    // 根据详细程度添加地址组件
    int maxLevel = Math.min(detailLevel.ordinal() + 1, addressParts.size());
    for (int i = 0; i < maxLevel; i++) {
      if (i > 0) address.append("");
      address.append(addressParts.get(i));
    }

    // 添加详细地址组件
    if (detailLevel.ordinal() >= DetailLevel.STREET.ordinal()) {
      String street = streetNames.get(random.nextInt(streetNames.size()));
      int streetNumber = random.nextInt(1, 1000);
      address.append(street).append(streetNumber).append("号");
    }

    if (detailLevel.ordinal() >= DetailLevel.COMMUNITY.ordinal()) {
      String community = communityNames.get(random.nextInt(communityNames.size()));
      address.append(community);
    }

    if (detailLevel == DetailLevel.FULL) {
      int building = random.nextInt(1, 50);
      String buildingName = buildingNames.get(random.nextInt(buildingNames.size()));
      int unit = random.nextInt(1, 6);
      int room = random.nextInt(101, 3999);

      address
          .append(building)
          .append(buildingName)
          .append(unit)
          .append("单元")
          .append(room)
          .append("室");
    }

    // 添加邮编
    if (includeZipcode) {
      String zipCode = getZipCode(regionCode);
      if (zipCode != null && !zipCode.isEmpty()) {
        address.append(" (").append(zipCode).append(")");
      }
    }

    return address.toString();
  }

  /** 构建地址层级部分 */
  private List<String> buildAddressParts(String regionCode) {
    List<String> parts = new ArrayList<>();

    AdministrativeDivision current = divisionsCache.get(regionCode);
    if (current == null) {
      parts.add("未知地区");
      return parts;
    }

    // 从当前地区向上追溯到省级
    List<AdministrativeDivision> hierarchy = new ArrayList<>();
    int maxDepth = 5; // 防止无限循环
    while (current != null && maxDepth-- > 0) {
      hierarchy.add(current);
      if (current.getParentCode().isEmpty()) {
        break; // 到达顶级
      }
      AdministrativeDivision parent = divisionsCache.get(current.getParentCode());
      if (parent == null) {
        log.debug(
            "Parent not found for {}: parentCode={}", current.getName(), current.getParentCode());
      }
      current = parent;
    }

    // 反转顺序，从省到区县
    Collections.reverse(hierarchy);

    for (AdministrativeDivision division : hierarchy) {
      parts.add(division.getName());
    }

    log.debug("Built address parts for {}: {}", regionCode, parts);
    return parts;
  }

  /** 获取邮编 */
  private String getZipCode(String regionCode) {
    AdministrativeDivision division = divisionsCache.get(regionCode);
    if (division != null && !division.getZipCode().isEmpty()) {
      return division.getZipCode();
    }

    // 如果没有具体邮编，生成一个合理的邮编
    ThreadLocalRandom random = ThreadLocalRandom.current();
    int baseCode = Integer.parseInt(regionCode.substring(0, 2)) * 10000;
    int offset = random.nextInt(1000, 9999);
    return String.valueOf(baseCode + offset);
  }
}
