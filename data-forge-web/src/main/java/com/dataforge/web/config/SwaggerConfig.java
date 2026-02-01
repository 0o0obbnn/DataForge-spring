package com.dataforge.web.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger文档配置类。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Configuration
public class SwaggerConfig {

  /**
   * 配置OpenAPI文档信息。
   *
   * @return OpenAPI 配置后的OpenAPI对象
   */
  @Bean
  public OpenAPI dataForgeOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("DataForge API")
                .description("高性能、灵活且高度可配置的测试数据生成工具")
                .version("1.0.0")
                .license(new License().name("Apache 2.0").url("http://springdoc.org")))
        .externalDocs(
            new ExternalDocumentation()
                .description("DataForge Documentation")
                .url("https://dataforge.example.com/docs"));
  }
}
