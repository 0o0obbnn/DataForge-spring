package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 地理坐标生成器
 *
 * <p>支持生成地理坐标信息，包括经度、纬度、高度等，用于位置服务测试、 地图应用开发、LBS功能测试等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>format: 输出格式 (DECIMAL|DMS|JSON|WKT) 默认: DECIMAL
 *   <li>latitude_min: 最小纬度 默认: -90.0
 *   <li>latitude_max: 最大纬度 默认: 90.0
 *   <li>longitude_min: 最小经度 默认: -180.0
 *   <li>longitude_max: 最大经度 默认: 180.0
 *   <li>altitude_min: 最小高度（米）默认: -500.0
 *   <li>altitude_max: 最大高度（米）默认: 10000.0
 *   <li>precision: 小数点后位数 默认: 6
 *   <li>include_altitude: 是否包含高度 默认: false
 *   <li>center_lat: 中心纬度（区域生成）
 *   <li>center_lon: 中心经度（区域生成）
 *   <li>radius_km: 半径（公里，区域生成）
 *   <li>region: 预设区域 (CHINA|USA|EUROPE|WORLD) 默认: WORLD
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class GeolocationGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(GeolocationGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 输出格式枚举
  public enum OutputFormat {
    DECIMAL("小数格式"),
    DMS("度分秒格式"),
    JSON("JSON格式"),
    WKT("WKT格式");

    private final String description;

    OutputFormat(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 预设区域枚举
  public enum Region {
    CHINA("中国", 18.0, 54.0, 73.0, 135.0),
    USA("美国", 24.0, 49.0, -125.0, -66.0),
    EUROPE("欧洲", 35.0, 71.0, -10.0, 40.0),
    WORLD("全球", -90.0, 90.0, -180.0, 180.0);

    private final String description;
    private final double minLat;
    private final double maxLat;
    private final double minLon;
    private final double maxLon;

    Region(String description, double minLat, double maxLat, double minLon, double maxLon) {
      this.description = description;
      this.minLat = minLat;
      this.maxLat = maxLat;
      this.minLon = minLon;
      this.maxLon = maxLon;
    }

    public String getDescription() {
      return description;
    }

    public double getMinLat() {
      return minLat;
    }

    public double getMaxLat() {
      return maxLat;
    }

    public double getMinLon() {
      return minLon;
    }

    public double getMaxLon() {
      return maxLon;
    }
  }

  // 地理坐标信息类
  public static class GeoLocation {
    private final double latitude;
    private final double longitude;
    private final Double altitude;

    public GeoLocation(double latitude, double longitude, Double altitude) {
      this.latitude = latitude;
      this.longitude = longitude;
      this.altitude = altitude;
    }

    public double getLatitude() {
      return latitude;
    }

    public double getLongitude() {
      return longitude;
    }

    public Double getAltitude() {
      return altitude;
    }
  }

  @Override
  public String getType() {
    return "geolocation";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取输出格式
      String formatStr = getStringParam(config, "format", "DECIMAL");
      OutputFormat format = parseOutputFormat(formatStr);

      // 获取精度
      int precision = getIntParam(config, "precision", 6);

      // 获取是否包含高度
      boolean includeAltitude = getBooleanParam(config, "include_altitude", false);

      // 生成地理坐标
      GeoLocation location = generateGeoLocation(config, includeAltitude);

      // 格式化输出
      return formatGeoLocation(location, format, precision);

    } catch (Exception e) {
      logger.error("Failed to generate geolocation", e);
      // 返回一个默认坐标作为fallback（北京天安门）
      return "39.908722,116.397499";
    }
  }

  /** 解析输出格式 */
  private OutputFormat parseOutputFormat(String formatStr) {
    try {
      return OutputFormat.valueOf(formatStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid output format: {}, using DECIMAL as default", formatStr);
      return OutputFormat.DECIMAL;
    }
  }

  /** 生成地理坐标 */
  private GeoLocation generateGeoLocation(FieldConfig config, boolean includeAltitude) {
    double latitude, longitude;
    Double altitude = null;

    // 检查是否指定了中心点和半径（区域生成）
    String centerLatStr = getStringParam(config, "center_lat", null);
    String centerLonStr = getStringParam(config, "center_lon", null);
    String radiusStr = getStringParam(config, "radius_km", null);

    if (centerLatStr != null && centerLonStr != null && radiusStr != null) {
      try {
        // 区域生成模式
        double centerLat = Double.parseDouble(centerLatStr);
        double centerLon = Double.parseDouble(centerLonStr);
        double radiusKm = Double.parseDouble(radiusStr);

        double[] coords = generateWithinRadius(centerLat, centerLon, radiusKm);
        latitude = coords[0];
        longitude = coords[1];
      } catch (NumberFormatException e) {
        logger.warn(
            "Invalid center coordinates or radius: lat={}, lon={}, radius={}, using bounds"
                + " generation",
            centerLatStr,
            centerLonStr,
            radiusStr);
        // 如果解析失败，回退到范围生成模式
        double[] bounds = getBounds(config);
        latitude = bounds[0] + random.nextDouble() * (bounds[1] - bounds[0]);
        longitude = bounds[2] + random.nextDouble() * (bounds[3] - bounds[2]);
      }
    } else {
      // 范围生成模式
      double[] bounds = getBounds(config);
      latitude = bounds[0] + random.nextDouble() * (bounds[1] - bounds[0]);
      longitude = bounds[2] + random.nextDouble() * (bounds[3] - bounds[2]);
    }

    // 生成高度
    if (includeAltitude) {
      double altitudeMin = getDoubleParam(config, "altitude_min", -500.0);
      double altitudeMax = getDoubleParam(config, "altitude_max", 10000.0);
      altitude = altitudeMin + random.nextDouble() * (altitudeMax - altitudeMin);
    }

    return new GeoLocation(latitude, longitude, altitude);
  }

  /** 获取坐标边界 */
  private double[] getBounds(FieldConfig config) {
    // 优先使用自定义边界，如果没有指定则使用预设区域
    double latMin = getDoubleParam(config, "latitude_min", Double.NaN);
    double latMax = getDoubleParam(config, "latitude_max", Double.NaN);
    double lonMin = getDoubleParam(config, "longitude_min", Double.NaN);
    double lonMax = getDoubleParam(config, "longitude_max", Double.NaN);

    // 如果指定了自定义边界，使用自定义边界
    if (!Double.isNaN(latMin)
        && !Double.isNaN(latMax)
        && !Double.isNaN(lonMin)
        && !Double.isNaN(lonMax)) {
      return new double[] {latMin, latMax, lonMin, lonMax};
    }

    // 否则使用预设区域
    String regionStr = getStringParam(config, "region", "WORLD");
    try {
      Region region = Region.valueOf(regionStr.toUpperCase());
      return new double[] {
        region.getMinLat(), region.getMaxLat(), region.getMinLon(), region.getMaxLon()
      };
    } catch (IllegalArgumentException e) {
      // 如果区域无效，使用全球范围
      return new double[] {-90.0, 90.0, -180.0, 180.0};
    }
  }

  /** 在指定半径内生成坐标 */
  private double[] generateWithinRadius(double centerLat, double centerLon, double radiusKm) {
    // 将半径转换为度数（近似）
    double radiusDegrees = radiusKm / 111.0; // 1度约等于111公里

    // 生成随机角度和距离
    double angle = random.nextDouble() * 2 * Math.PI;
    double distance = random.nextDouble() * radiusDegrees;

    // 计算新坐标
    double deltaLat = distance * Math.cos(angle);
    double deltaLon = distance * Math.sin(angle) / Math.cos(Math.toRadians(centerLat));

    double newLat = centerLat + deltaLat;
    double newLon = centerLon + deltaLon;

    // 确保坐标在有效范围内
    newLat = Math.max(-90.0, Math.min(90.0, newLat));
    newLon = Math.max(-180.0, Math.min(180.0, newLon));

    return new double[] {newLat, newLon};
  }

  /** 格式化地理坐标 */
  private String formatGeoLocation(GeoLocation location, OutputFormat format, int precision) {
    switch (format) {
      case DECIMAL:
        return formatDecimal(location, precision);
      case DMS:
        return formatDMS(location, precision);
      case JSON:
        return formatJSON(location, precision);
      case WKT:
        return formatWKT(location, precision);
      default:
        return formatDecimal(location, precision);
    }
  }

  /** 格式化为小数格式 */
  private String formatDecimal(GeoLocation location, int precision) {
    DecimalFormat df = new DecimalFormat("#." + "0".repeat(precision));

    String result = df.format(location.getLatitude()) + "," + df.format(location.getLongitude());

    if (location.getAltitude() != null) {
      result += "," + df.format(location.getAltitude());
    }

    return result;
  }

  /** 格式化为度分秒格式 */
  private String formatDMS(GeoLocation location, int precision) {
    String latDMS = convertToDMS(location.getLatitude(), true);
    String lonDMS = convertToDMS(location.getLongitude(), false);

    String result = latDMS + " " + lonDMS;

    if (location.getAltitude() != null) {
      DecimalFormat df = new DecimalFormat("#." + "0".repeat(precision));
      result += " " + df.format(location.getAltitude()) + "m";
    }

    return result;
  }

  /** 格式化为JSON格式 */
  private String formatJSON(GeoLocation location, int precision) {
    DecimalFormat df = new DecimalFormat("#." + "0".repeat(precision));

    StringBuilder json = new StringBuilder();
    json.append("{\"latitude\":")
        .append(df.format(location.getLatitude()))
        .append(",\"longitude\":")
        .append(df.format(location.getLongitude()));

    if (location.getAltitude() != null) {
      json.append(",\"altitude\":").append(df.format(location.getAltitude()));
    }

    json.append("}");
    return json.toString();
  }

  /** 格式化为WKT格式 */
  private String formatWKT(GeoLocation location, int precision) {
    DecimalFormat df = new DecimalFormat("#." + "0".repeat(precision));

    if (location.getAltitude() != null) {
      return String.format(
          "POINT Z (%s %s %s)",
          df.format(location.getLongitude()),
          df.format(location.getLatitude()),
          df.format(location.getAltitude()));
    } else {
      return String.format(
          "POINT (%s %s)", df.format(location.getLongitude()), df.format(location.getLatitude()));
    }
  }

  /** 将十进制度数转换为度分秒格式 */
  private String convertToDMS(double decimal, boolean isLatitude) {
    boolean isNegative = decimal < 0;
    decimal = Math.abs(decimal);

    int degrees = (int) decimal;
    double minutesDecimal = (decimal - degrees) * 60;
    int minutes = (int) minutesDecimal;
    double seconds = (minutesDecimal - minutes) * 60;

    String direction;
    if (isLatitude) {
      direction = isNegative ? "S" : "N";
    } else {
      direction = isNegative ? "W" : "E";
    }

    return String.format("%d°%d'%.2f\"%s", degrees, minutes, seconds, direction);
  }
}
