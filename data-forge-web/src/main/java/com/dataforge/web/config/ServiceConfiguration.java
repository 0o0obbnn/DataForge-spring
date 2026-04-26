package com.dataforge.web.config;

import com.dataforge.core.GeneratorFactory;
import com.dataforge.io.OutputStrategy;
import com.dataforge.service.DataForgeService;
import com.dataforge.validation.SecurityValidator;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 服务配置类，用于配置核心服务Bean
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Configuration
public class ServiceConfiguration {

  /**
   * 创建DataForgeService Bean
   *
   * @param generatorFactory 生成器工厂
   * @param outputStrategies 输出策略列表
   * @param securityValidator 安全验证器
   * @return DataForgeService实例
   */
  @Bean
  public DataForgeService dataForgeService(
      GeneratorFactory generatorFactory,
      List<OutputStrategy> outputStrategies,
      SecurityValidator securityValidator) {
    return new DataForgeService(generatorFactory, outputStrategies, securityValidator);
  }
}
