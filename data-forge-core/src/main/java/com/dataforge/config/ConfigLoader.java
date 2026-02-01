package com.dataforge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.stereotype.Component;

/** 配置文件加载器。 */
@Component
public class ConfigLoader {

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
    // 简化实现，实际可以更复杂
    for (int i = 0; i < args.length - 1; i++) {
      String arg = args[i];
      String value = args[i + 1];

      switch (arg) {
        case "-c", "--count" -> config.setCount(Integer.parseInt(value));
        case "-t", "--threads" -> config.setThreads(Integer.parseInt(value));
        case "--validate" -> config.setValidate(Boolean.parseBoolean(value));
        case "--seed" -> config.setSeed(Long.parseLong(value));
      }
    }

    return config;
  }
}
