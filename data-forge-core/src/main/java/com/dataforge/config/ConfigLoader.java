package com.dataforge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 配置文件加载器。 */
@Component
public class ConfigLoader {

  private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

  private final ObjectMapper jsonMapper = new ObjectMapper();
  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  /** 从文件加载配置。 */
  public ForgeConfig loadFromFile(String filePath) throws IOException {
    File file = new File(filePath);
    if (!file.exists()) {
      throw new IOException("配置文件不存在: " + filePath);
    }

    if (filePath.toLowerCase().endsWith(".yml") || filePath.toLowerCase().endsWith(".yaml")) {
      return yamlMapper.readValue(file, ForgeConfig.class);
    } else if (filePath.toLowerCase().endsWith(".json")) {
      return jsonMapper.readValue(file, ForgeConfig.class);
    } else {
      throw new IOException("不支持的配置文件格式: " + filePath);
    }
  }

  /** 从类路径加载配置。 */
  public ForgeConfig loadFromClasspath(String resourcePath) throws IOException {
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
    if (inputStream == null) {
      throw new IOException("类路径中找不到配置文件: " + resourcePath);
    }

    if (resourcePath.toLowerCase().endsWith(".yml")
        || resourcePath.toLowerCase().endsWith(".yaml")) {
      return yamlMapper.readValue(inputStream, ForgeConfig.class);
    } else if (resourcePath.toLowerCase().endsWith(".json")) {
      return jsonMapper.readValue(inputStream, ForgeConfig.class);
    } else {
      throw new IOException("不支持的配置文件格式: " + resourcePath);
    }
  }

  /** 合并命令行参数到配置。 */
  public ForgeConfig mergeWithCliArgs(ForgeConfig config, String[] args) {
    if (args == null || args.length == 0) {
      return config;
    }

    for (int i = 0; i < args.length - 1; i++) {
      String arg = args[i];
      String value = args[i + 1];

      try {
        switch (arg) {
          case "-c", "--count" -> {
            int count = CliArgumentParser.parseIntInRange("count", value, 1, 1_000_000_000);
            config.setCount(count);
          }
          case "-t", "--threads" -> {
            int threads = CliArgumentParser.parseIntInRange("threads", value, 1, 64);
            config.setThreads(threads);
          }
          case "--validate" -> config.setValidate(
              CliArgumentParser.parseBoolean("validate", value));
          case "--seed" -> config.setSeed(CliArgumentParser.parseLong("seed", value));
          default -> {
            // 忽略未知参数，记录警告
            logger.warn("Unknown CLI argument: {}, skipping", arg);
          }
        }
      } catch (IllegalArgumentException e) {
        logger.warn("Failed to parse CLI argument: {} {} - {}", arg, value, e.getMessage());
        throw e; // 重新抛出以终止程序
      }
    }

    return config;
  }
}
