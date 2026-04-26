package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.validation.LuhnValidator;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 设备ID生成器
 *
 * <p>支持生成多种类型的设备标识符：
 *
 * <ul>
 *   <li>UUID - 通用唯一标识符
 *   <li>IMEI - 国际移动设备识别码（15位）
 *   <li>IMSI - 国际移动用户识别码（15位）
 *   <li>CUSTOM - 自定义格式设备ID
 * </ul>
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>type: 设备ID类型 (UUID|IMEI|IMSI|CUSTOM) 默认: UUID
 *   <li>length: 自定义长度（仅对CUSTOM类型有效）默认: 16
 *   <li>tac: IMEI的TAC码前缀（8位）
 *   <li>mcc: IMSI的移动国家码（3位）
 *   <li>mnc: IMSI的移动网络码（2-3位）
 *   <li>valid: 是否生成有效的设备ID 默认: true
 *   <li>format: 输出格式 (PLAIN|HYPHEN|COLON) 默认: PLAIN
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class DeviceIdGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(DeviceIdGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 设备ID类型枚举
  public enum DeviceIdType {
    UUID("UUID格式设备ID"),
    IMEI("国际移动设备识别码"),
    IMSI("国际移动用户识别码"),
    CUSTOM("自定义格式设备ID");

    private final String description;

    DeviceIdType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 输出格式枚举
  public enum OutputFormat {
    PLAIN("无分隔符"),
    HYPHEN("连字符分隔"),
    COLON("冒号分隔");

    private final String description;

    OutputFormat(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 常见TAC码（设备类型分配码）
  private static final List<String> COMMON_TAC_CODES =
      Arrays.asList(
          "35328506", // Apple iPhone
          "35209006", // Samsung Galaxy
          "35875505", // Huawei
          "35404906", // Xiaomi
          "35891706", // OPPO
          "35917406", // Vivo
          "35824005", // OnePlus
          "35679804", // Google Pixel
          "35434104", // Sony Xperia
          "35251203" // LG
          );

  // 移动国家码和网络码映射
  private static final Map<String, List<String>> MCC_MNC_MAP = new HashMap<>();

  static {
    // 中国
    MCC_MNC_MAP.put("460", Arrays.asList("00", "01", "02", "03", "07", "08", "11", "20"));
    // 美国
    MCC_MNC_MAP.put("310", Arrays.asList("030", "070", "150", "260", "410", "560", "680", "890"));
    // 英国
    MCC_MNC_MAP.put("234", Arrays.asList("10", "15", "20", "30", "33", "50", "55", "58"));
    // 日本
    MCC_MNC_MAP.put("440", Arrays.asList("00", "01", "02", "03", "04", "05", "06", "07"));
    // 韩国
    MCC_MNC_MAP.put("450", Arrays.asList("02", "03", "04", "05", "06", "07", "08", "11"));
  }

  @Override
  public String getType() {
    return "device_id";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取设备ID类型
      String typeStr = getStringParam(config, "type", "UUID");
      DeviceIdType type = parseDeviceIdType(typeStr);

      // 获取是否生成有效设备ID
      boolean valid = getBooleanParam(config, "valid", true);

      // 获取输出格式
      String formatStr = getStringParam(config, "format", "PLAIN");
      OutputFormat format = parseOutputFormat(formatStr);

      String deviceId;

      switch (type) {
        case UUID:
          deviceId = generateUuidDeviceId();
          break;
        case IMEI:
          deviceId = generateImei(config, valid);
          break;
        case IMSI:
          deviceId = generateImsi(config, valid);
          break;
        case CUSTOM:
          deviceId = generateCustomDeviceId(config);
          break;
        default:
          deviceId = generateUuidDeviceId();
          break;
      }

      // 应用输出格式
      return applyOutputFormat(deviceId, format, type);

    } catch (Exception e) {
      logger.error("Failed to generate device ID", e);
      // 返回一个默认的UUID作为fallback
      return UUID.randomUUID().toString().replace("-", "");
    }
  }

  /** 解析设备ID类型 */
  private DeviceIdType parseDeviceIdType(String typeStr) {
    try {
      return DeviceIdType.valueOf(typeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid device ID type: {}, using UUID as default", typeStr);
      return DeviceIdType.UUID;
    }
  }

  /** 解析输出格式 */
  private OutputFormat parseOutputFormat(String formatStr) {
    try {
      return OutputFormat.valueOf(formatStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid output format: {}, using PLAIN as default", formatStr);
      return OutputFormat.PLAIN;
    }
  }

  /** 生成UUID格式的设备ID */
  private String generateUuidDeviceId() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  /** 生成IMEI（国际移动设备识别码） */
  private String generateImei(FieldConfig config, boolean valid) {
    // 获取TAC码
    String tac = getStringParam(config, "tac", null);
    if (tac == null || tac.length() != 8) {
      // 随机选择一个常见的TAC码
      tac = COMMON_TAC_CODES.get(random.nextInt(COMMON_TAC_CODES.size()));
    }

    // 生成序列号（6位）
    String serialNumber = String.format("%06d", random.nextInt(1000000));

    // 组合前14位
    String imeiWithoutChecksum = tac + serialNumber;

    if (!valid) {
      // 生成无效的IMEI（错误的校验位）
      return imeiWithoutChecksum + String.valueOf(random.nextInt(10));
    }

    // 计算校验位（Luhn算法）
    LuhnValidator luhnValidator = new LuhnValidator();
    int checksum = luhnValidator.generateCheckDigit(imeiWithoutChecksum);

    return imeiWithoutChecksum + checksum;
  }

  /** 生成IMSI（国际移动用户识别码） */
  private String generateImsi(FieldConfig config, boolean valid) {
    // 获取MCC（移动国家码）
    String mcc = getStringParam(config, "mcc", "460"); // 默认中国
    if (mcc.length() != 3) {
      mcc = "460";
    }

    // 获取MNC（移动网络码）
    String mnc = getStringParam(config, "mnc", null);
    if (mnc == null) {
      List<String> mncList = MCC_MNC_MAP.get(mcc);
      if (mncList != null && !mncList.isEmpty()) {
        mnc = mncList.get(random.nextInt(mncList.size()));
      } else {
        mnc = String.format("%02d", random.nextInt(100));
      }
    }

    // 确保MNC长度为2-3位
    if (mnc.length() < 2) {
      mnc = String.format("%02d", Integer.parseInt(mnc));
    } else if (mnc.length() > 3) {
      mnc = mnc.substring(0, 3);
    }

    // 计算MSIN长度（总长度15位 - MCC 3位 - MNC长度）
    int msinLength = 15 - 3 - mnc.length();

    // 生成MSIN（移动用户识别号）
    StringBuilder msin = new StringBuilder();
    for (int i = 0; i < msinLength; i++) {
      msin.append(random.nextInt(10));
    }

    String imsi = mcc + mnc + msin.toString();

    if (!valid) {
      // 生成无效的IMSI（长度不正确）
      return imsi + String.valueOf(random.nextInt(10));
    }

    return imsi;
  }

  /** 生成自定义格式设备ID */
  private String generateCustomDeviceId(FieldConfig config) {
    int length = getIntParam(config, "length", 16);

    StringBuilder deviceId = new StringBuilder();
    String chars = "0123456789ABCDEF";

    for (int i = 0; i < length; i++) {
      deviceId.append(chars.charAt(random.nextInt(chars.length())));
    }

    return deviceId.toString();
  }

  /** 应用输出格式 */
  private String applyOutputFormat(String deviceId, OutputFormat format, DeviceIdType type) {
    if (format == OutputFormat.PLAIN || type == DeviceIdType.IMEI || type == DeviceIdType.IMSI) {
      return deviceId;
    }

    StringBuilder formatted = new StringBuilder();
    String separator = (format == OutputFormat.HYPHEN) ? "-" : ":";

    // 根据设备ID类型决定分组方式
    int groupSize = (type == DeviceIdType.UUID) ? 8 : 4;

    for (int i = 0; i < deviceId.length(); i++) {
      if (i > 0 && i % groupSize == 0) {
        formatted.append(separator);
      }
      formatted.append(deviceId.charAt(i));
    }

    return formatted.toString();
  }
}
