package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 颜色值生成器
 *
 * <p>根据DataForge设计文档要求，生成各种格式的颜色值用于UI测试和图形处理系统测试。 支持HEX、RGB、HSL、HSV、CMYK等多种颜色格式。
 *
 * @author DataForge
 * @version 1.0.0
 * @since 2024-01-15
 */
@Component
public class ColorValueGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(ColorValueGenerator.class);

  // 常用颜色名称
  private static final List<String> COLOR_NAMES =
      Arrays.asList(
          "red",
          "green",
          "blue",
          "yellow",
          "cyan",
          "magenta",
          "black",
          "white",
          "gray",
          "orange",
          "purple",
          "pink",
          "brown",
          "lime",
          "navy",
          "maroon",
          "olive",
          "teal",
          "silver",
          "gold",
          "indigo",
          "violet",
          "turquoise");

  @Override
  public String getType() {
    return "color";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String format = config.getParam("format", String.class, "HEX");
      boolean uppercase = Boolean.parseBoolean(config.getParam("uppercase", String.class, "false"));
      boolean includeAlpha =
          Boolean.parseBoolean(config.getParam("includeAlpha", String.class, "false"));
      String prefix = config.getParam("prefix", String.class, "");

      return generateColor(format, uppercase, includeAlpha, prefix);

    } catch (Exception e) {
      logger.warn("Error generating color value: {}", e.getMessage());
      // 生成默认HEX颜色
      return generateDefaultColor();
    }
  }

  /** 生成指定格式的颜色值 */
  private String generateColor(
      String format, boolean uppercase, boolean includeAlpha, String prefix) {
    switch (format.toUpperCase()) {
      case "HEX":
        return generateHexColor(uppercase, includeAlpha, prefix);

      case "RGB":
        return generateRgbColor(includeAlpha);

      case "HSL":
        return generateHslColor(includeAlpha);

      case "HSV":
        return generateHsvColor();

      case "CMYK":
        return generateCmykColor();

      case "NAME":
        return generateColorName(uppercase);

      default:
        return generateHexColor(uppercase, includeAlpha, prefix);
    }
  }

  /** 生成HEX格式颜色值 */
  private String generateHexColor(boolean uppercase, boolean includeAlpha, String prefix) {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    String hex;

    if (includeAlpha) {
      // 包含Alpha通道的8位HEX颜色
      hex =
          String.format(
              "#%02x%02x%02x%02x",
              random.nextInt(256), random.nextInt(256), random.nextInt(256), random.nextInt(256));
    } else {
      // 6位HEX颜色
      hex =
          String.format(
              "#%02x%02x%02x", random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    if (uppercase) {
      hex = hex.toUpperCase();
    }

    return prefix + hex;
  }

  /** 生成RGB格式颜色值 */
  private String generateRgbColor(boolean includeAlpha) {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    if (includeAlpha) {
      // RGBA格式
      return String.format(
          "rgba(%d, %d, %d, %.2f)",
          random.nextInt(256), random.nextInt(256), random.nextInt(256), random.nextDouble());
    } else {
      // RGB格式
      return String.format(
          "rgb(%d, %d, %d)", random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }
  }

  /** 生成HSL格式颜色值 */
  private String generateHslColor(boolean includeAlpha) {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    int hue = random.nextInt(360); // 色相 (0-359)
    int saturation = random.nextInt(101); // 饱和度 (0-100)
    int lightness = random.nextInt(101); // 亮度 (0-100)

    if (includeAlpha) {
      // HSLA格式
      return String.format(
          "hsla(%d, %d%%, %d%%, %.2f)", hue, saturation, lightness, random.nextDouble());
    } else {
      // HSL格式
      return String.format("hsl(%d, %d%%, %d%%)", hue, saturation, lightness);
    }
  }

  /** 生成HSV格式颜色值 */
  private String generateHsvColor() {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    int hue = random.nextInt(360); // 色相 (0-359)
    int saturation = random.nextInt(101); // 饱和度 (0-100)
    int value = random.nextInt(101); // 明度 (0-100)

    return String.format("hsv(%d, %d%%, %d%%)", hue, saturation, value);
  }

  /** 生成CMYK格式颜色值 */
  private String generateCmykColor() {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    int cyan = random.nextInt(101); // 青色 (0-100)
    int magenta = random.nextInt(101); // 品红色 (0-100)
    int yellow = random.nextInt(101); // 黄色 (0-100)
    int black = random.nextInt(101); // 黑色 (0-100)

    return String.format("cmyk(%d%%, %d%%, %d%%, %d%%)", cyan, magenta, yellow, black);
  }

  /** 生成颜色名称 */
  private String generateColorName(boolean uppercase) {
    String name = COLOR_NAMES.get(ThreadLocalRandom.current().nextInt(COLOR_NAMES.size()));
    return uppercase ? name.toUpperCase() : name.toLowerCase();
  }

  /** 生成默认颜色值 */
  private String generateDefaultColor() {
    return generateHexColor(false, false, "");
  }

  @Override
  public boolean isValidConfig(FieldConfig config) {
    if (config == null) {
      return false;
    }

    String format = config.getParam("format", String.class, "HEX");

    // 验证格式
    String[] validFormats = {"HEX", "RGB", "HSL", "HSV", "CMYK", "NAME"};
    for (String validFormat : validFormats) {
      if (validFormat.equalsIgnoreCase(format)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public String getDescription() {
    return "生成颜色值，支持HEX、RGB、HSL、HSV、CMYK、颜色名称等格式，" + "可配置大小写、透明度和前缀，适用于UI测试和图形处理系统测试";
  }
}
