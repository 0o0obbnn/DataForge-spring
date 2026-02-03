package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 颜色值生成器
 *
 * <p>支持生成各种格式的颜色值，用于UI测试、设计系统、前端开发等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>format: 颜色格式 (HEX|RGB|HSL|HSV|CMYK|NAME) 默认: HEX
 *   <li>alpha: 是否包含透明度 默认: false
 *   <li>uppercase: HEX格式是否大写 默认: true
 *   <li>prefix: 是否包含前缀（如#） 默认: true
 *   <li>brightness: 亮度范围 (DARK|LIGHT|ANY) 默认: ANY
 *   <li>saturation: 饱和度范围 (LOW|HIGH|ANY) 默认: ANY
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class ColorGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(ColorGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 颜色格式枚举
  public enum ColorFormat {
    HEX("十六进制"),
    RGB("RGB格式"),
    HSL("HSL格式"),
    HSV("HSV格式"),
    CMYK("CMYK格式"),
    NAME("颜色名称");

    private final String description;

    ColorFormat(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 常见颜色名称
  private static final String[] COLOR_NAMES = {
    "red", "green", "blue", "yellow", "orange", "purple", "pink", "brown",
    "black", "white", "gray", "grey", "cyan", "magenta", "lime", "maroon",
    "navy", "olive", "teal", "silver", "gold", "indigo", "violet", "coral",
    "salmon", "khaki", "plum", "orchid", "tan", "beige", "mint", "lavender"
  };

  @Override
  public String getType() {
    return "color2";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String formatStr = getStringParam(config, "format", "HEX");
      ColorFormat format = parseColorFormat(formatStr);

      switch (format) {
        case HEX:
          return generateHex(config);
        case RGB:
          return generateRGB(config);
        case HSL:
          return generateHSL(config);
        case HSV:
          return generateHSV(config);
        case CMYK:
          return generateCMYK(config);
        case NAME:
          return generateColorName(config);
        default:
          return generateHex(config);
      }

    } catch (Exception e) {
      logger.error("Failed to generate color", e);
      return "#FF0000"; // 红色作为fallback
    }
  }

  private ColorFormat parseColorFormat(String formatStr) {
    try {
      return ColorFormat.valueOf(formatStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid color format: {}, using HEX as default", formatStr);
      return ColorFormat.HEX;
    }
  }

  private String generateHex(FieldConfig config) {
    boolean alpha = getBooleanParam(config, "alpha", false);
    boolean uppercase = getBooleanParam(config, "uppercase", true);
    boolean prefix = getBooleanParam(config, "prefix", true);

    int[] rgb = generateRGBValues(config);

    StringBuilder hex = new StringBuilder();
    if (prefix) {
      hex.append("#");
    }

    hex.append(String.format("%02x%02x%02x", rgb[0], rgb[1], rgb[2]));

    if (alpha) {
      int alphaValue = random.nextInt(256);
      hex.append(String.format("%02x", alphaValue));
    }

    return uppercase ? hex.toString().toUpperCase() : hex.toString();
  }

  private String generateRGB(FieldConfig config) {
    boolean alpha = getBooleanParam(config, "alpha", false);
    int[] rgb = generateRGBValues(config);

    if (alpha) {
      double alphaValue = random.nextDouble();
      return String.format("rgba(%d, %d, %d, %.2f)", rgb[0], rgb[1], rgb[2], alphaValue);
    } else {
      return String.format("rgb(%d, %d, %d)", rgb[0], rgb[1], rgb[2]);
    }
  }

  private String generateHSL(FieldConfig config) {
    boolean alpha = getBooleanParam(config, "alpha", false);

    int hue = random.nextInt(360);
    int saturation = generateSaturation(config);
    int lightness = generateLightness(config);

    if (alpha) {
      double alphaValue = random.nextDouble();
      return String.format("hsla(%d, %d%%, %d%%, %.2f)", hue, saturation, lightness, alphaValue);
    } else {
      return String.format("hsl(%d, %d%%, %d%%)", hue, saturation, lightness);
    }
  }

  private String generateHSV(FieldConfig config) {
    int hue = random.nextInt(360);
    int saturation = generateSaturation(config);
    int value = generateLightness(config); // 使用相同的逻辑

    return String.format("hsv(%d, %d%%, %d%%)", hue, saturation, value);
  }

  private String generateCMYK(FieldConfig config) {
    int cyan = random.nextInt(101);
    int magenta = random.nextInt(101);
    int yellow = random.nextInt(101);
    int black = random.nextInt(101);

    return String.format("cmyk(%d%%, %d%%, %d%%, %d%%)", cyan, magenta, yellow, black);
  }

  private String generateColorName(FieldConfig config) {
    return COLOR_NAMES[random.nextInt(COLOR_NAMES.length)];
  }

  private int[] generateRGBValues(FieldConfig config) {
    String brightness = getStringParam(config, "brightness", "ANY");

    int[] rgb = new int[3];

    switch (brightness.toUpperCase()) {
      case "DARK":
        // 暗色：RGB值在0-127之间
        for (int i = 0; i < 3; i++) {
          rgb[i] = random.nextInt(128);
        }
        break;
      case "LIGHT":
        // 亮色：RGB值在128-255之间
        for (int i = 0; i < 3; i++) {
          rgb[i] = 128 + random.nextInt(128);
        }
        break;
      case "ANY":
      default:
        // 任意：RGB值在0-255之间
        for (int i = 0; i < 3; i++) {
          rgb[i] = random.nextInt(256);
        }
        break;
    }

    return rgb;
  }

  private int generateSaturation(FieldConfig config) {
    String saturation = getStringParam(config, "saturation", "ANY");

    switch (saturation.toUpperCase()) {
      case "LOW":
        return random.nextInt(31); // 0-30%
      case "HIGH":
        return 70 + random.nextInt(31); // 70-100%
      case "ANY":
      default:
        return random.nextInt(101); // 0-100%
    }
  }

  private int generateLightness(FieldConfig config) {
    String brightness = getStringParam(config, "brightness", "ANY");

    switch (brightness.toUpperCase()) {
      case "DARK":
        return random.nextInt(31); // 0-30%
      case "LIGHT":
        return 70 + random.nextInt(31); // 70-100%
      case "ANY":
      default:
        return random.nextInt(101); // 0-100%
    }
  }
}
