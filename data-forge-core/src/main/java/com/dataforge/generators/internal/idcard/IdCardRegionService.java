package com.dataforge.generators.internal.idcard;

import com.dataforge.service.DataLoadingService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 身份证地区代码服务。
 *
 * <p>负责管理行政区划代码数据，支持地区代码查询、随机选择和权重选择。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Service
public class IdCardRegionService {

  private static final Logger logger = LoggerFactory.getLogger(IdCardRegionService.class);

  private static final String ADMINISTRATIVE_DIVISIONS_PATH = "data/administrative-divisions.txt";

  @Autowired private DataLoadingService dataLoadingService;

  private volatile Map<String, RegionInfo> regionCodes;
  private volatile List<String> allRegionCodes;
  private volatile Map<String, List<String>> regionsByProvince;
  private volatile Map<String, List<String>> regionsByCity;
  private volatile boolean dataLoaded = false;
  private final Object dataLoadLock = new Object();

  /** Fallback地区代码映射。 */
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
  public static class RegionInfo {
    public final String code;
    public final String province;
    public final String city;
    public final String district;
    public final int weight;

    public RegionInfo(String code, String province, String city, String district, int weight) {
      this.code = code;
      this.province = province;
      this.city = city;
      this.district = district;
      this.weight = weight;
    }

    @Override
    public String toString() {
      return String.format("%s (%s %s %s)", code, province, city, district);
    }
  }

  /** 确保数据已加载。 */
  public void ensureDataLoaded() {
    if (!dataLoaded) {
      synchronized (dataLoadLock) {
        if (!dataLoaded) {
          loadData();
          dataLoaded = true;
        }
      }
    }
  }

  /** 加载行政区划数据。 */
  private void loadData() {
    List<String> lines =
        dataLoadingService.loadLinesWithFallback(
            ADMINISTRATIVE_DIVISIONS_PATH,
            () -> {
              logger.warn("Failed to load region data, using fallback");
              return new ArrayList<>();
            });

    if (lines.isEmpty()) {
      initializeFallbackData();
      return;
    }

    parseRegionData(lines);
    logger.info("Region data loaded - Total regions: {}", regionCodes.size());
  }

  /** 解析地区数据。 */
  private void parseRegionData(List<String> lines) {
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
  }

  /** 初始化 fallback 数据。 */
  private void initializeFallbackData() {
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
    logger.info("Region fallback data initialized - Total regions: {}", regionCodes.size());
  }

  private int parseWeight(String weightStr) {
    try {
      return Integer.parseInt(weightStr);
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  /**
   * 选择地区代码。
   *
   * @param region 地区名称或代码
   * @return 地区代码
   */
  public String selectRegionCode(String region) {
    ensureDataLoaded();

    if (region == null || region.trim().isEmpty()) {
      return getRandomRegionCode();
    }

    String cleanRegion = region.trim();

    // 直接匹配地区代码
    if (regionCodes.containsKey(cleanRegion)) {
      return cleanRegion;
    }

    // 按省份匹配
    List<String> candidates = regionsByProvince.get(cleanRegion);
    if (candidates != null && !candidates.isEmpty()) {
      return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    // 按城市匹配
    candidates = regionsByCity.get(cleanRegion);
    if (candidates != null && !candidates.isEmpty()) {
      return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    return getRandomRegionCode();
  }

  /** 获取随机地区代码。 */
  public String getRandomRegionCode() {
    ensureDataLoaded();
    return allRegionCodes.get(ThreadLocalRandom.current().nextInt(allRegionCodes.size()));
  }

  /** 获取地区信息。 */
  public RegionInfo getRegionInfo(String code) {
    ensureDataLoaded();
    return regionCodes.get(code);
  }

  /** 获取所有地区代码。 */
  public List<String> getAllRegionCodes() {
    ensureDataLoaded();
    return new ArrayList<>(allRegionCodes);
  }

  /** 检查是否包含指定地区代码。 */
  public boolean containsRegion(String code) {
    ensureDataLoaded();
    return regionCodes.containsKey(code);
  }
}
