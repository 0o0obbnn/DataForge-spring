package com.dataforge.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * DataForge Web应用程序的主入口类。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.dataforge"})
@EnableConfigurationProperties
public class DataForgeWebApplication {

  /**
   * 应用程序主入口方法。
   *
   * @param args 命令行参数
   */
  public static void main(String[] args) {
    SpringApplication.run(DataForgeWebApplication.class, args);
  }
}
