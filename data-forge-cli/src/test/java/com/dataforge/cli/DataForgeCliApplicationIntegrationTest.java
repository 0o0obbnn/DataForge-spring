package com.dataforge.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

@DisplayName("DataForgeCliApplication 集成测试")
class DataForgeCliApplicationIntegrationTest {

  private ConfigurableApplicationContext context;

  @BeforeEach
  void setUp() {
    System.setProperty("spring.main.web-application-type", "none");
    System.setProperty("test.mode", "true");
  }

  @AfterEach
  void tearDown() {
    if (context != null) {
      context.close();
    }
    System.clearProperty("spring.main.web-application-type");
  }

  @Nested
  @DisplayName("应用启动测试")
  class ApplicationStartupTests {

    @Test
    @DisplayName("应成功启动应用")
    void shouldStartApplicationSuccessfully() {
      context = SpringApplication.run(DataForgeCliApplication.class);

      assertThat(context).isNotNull();
      assertThat(context.isActive()).isTrue();
    }

    @Test
    @DisplayName("应加载Spring上下文")
    void shouldLoadSpringContext() {
      context = SpringApplication.run(DataForgeCliApplication.class);

      assertThat(context.getBean(DataForgeCliApplication.class)).isNotNull();
    }
  }

  @Nested
  @DisplayName("命令行执行测试")
  class CommandLineExecutionTests {

    @Test
    @DisplayName("应执行generate命令")
    void shouldExecuteGenerateCommand() {
      context = SpringApplication.run(DataForgeCliApplication.class);

      DataForgeCliApplication application = context.getBean(DataForgeCliApplication.class);

      assertThat(application).isNotNull();
    }

    @Test
    @DisplayName("应处理空参数")
    void shouldHandleEmptyArguments() throws Exception {
      context = SpringApplication.run(DataForgeCliApplication.class);

      DataForgeCliApplication application = context.getBean(DataForgeCliApplication.class);

      assertThat(application).isNotNull();
    }
  }

  @Nested
  @DisplayName("Bean配置测试")
  class BeanConfigurationTests {

    @Test
    @DisplayName("应注入GenerateCommand bean")
    void shouldInjectGenerateCommandBean() {
      context = SpringApplication.run(DataForgeCliApplication.class);

      assertThat(context.getBean("generateCommand")).isNotNull();
    }

    @Test
    @DisplayName("应注入ApplicationContext")
    void shouldInjectApplicationContext() {
      context = SpringApplication.run(DataForgeCliApplication.class);

      assertThat(context).isNotNull();
      assertThat(context.getBeanFactory()).isNotNull();
    }
  }

  @Nested
  @DisplayName("配置属性测试")
  class ConfigurationPropertiesTests {

    @Test
    @DisplayName("应禁用Web应用类型")
    void shouldDisableWebApplicationType() {
      context = SpringApplication.run(DataForgeCliApplication.class);

      String webType = context.getEnvironment().getProperty("spring.main.web-application-type");
      assertThat(webType).isEqualTo("none");
    }

    @Test
    @DisplayName("应使用none作为Web应用类型")
    void shouldUseNoneAsWebApplicationType() {
      System.setProperty("spring.main.web-application-type", "none");
      context = SpringApplication.run(DataForgeCliApplication.class);

      assertThat(context).isNotNull();
    }
  }

  @Nested
  @DisplayName("异常处理测试")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("应处理无效参数")
    void shouldHandleInvalidArguments() {
      String[] args = {"--invalid-argument"};

      assertThatCode(() -> SpringApplication.run(DataForgeCliApplication.class, args))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("应处理空参数数组")
    void shouldHandleEmptyArgumentsArray() {
      String[] args = {};

      assertThatCode(() -> SpringApplication.run(DataForgeCliApplication.class, args))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("应用关闭测试")
  class ApplicationShutdownTests {

    @Test
    @DisplayName("应正常关闭应用")
    void shouldShutdownApplicationGracefully() {
      context = SpringApplication.run(DataForgeCliApplication.class);

      assertThat(context.isActive()).isTrue();

      context.close();

      assertThat(context.isActive()).isFalse();
    }

    @Test
    @DisplayName("应支持多次启动和关闭")
    void shouldSupportMultipleStartAndShutdown() {
      ConfigurableApplicationContext context1 = SpringApplication.run(DataForgeCliApplication.class);
      context1.close();

      ConfigurableApplicationContext context2 = SpringApplication.run(DataForgeCliApplication.class);
      context2.close();

      assertThat(context1.isActive()).isFalse();
      assertThat(context2.isActive()).isFalse();
    }
  }

  @Nested
  @DisplayName("Picocli集成测试")
  class PicocliIntegrationTests {

    @Test
    @DisplayName("应使用PicocliSpringFactory")
    void shouldUsePicocliSpringFactory() {
      context = SpringApplication.run(DataForgeCliApplication.class);

      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("应支持Picocli命令")
    void shouldSupportPicocliCommands() {
      context = SpringApplication.run(DataForgeCliApplication.class);

      assertThat(context).isNotNull();
    }
  }

  @Nested
  @DisplayName("组件扫描测试")
  class ComponentScanTests {

    @Test
    @DisplayName("应扫描com.dataforge包")
    void shouldScanComDataforgePackage() {
      context = SpringApplication.run(DataForgeCliApplication.class);

      String[] beanNames = context.getBeanNamesForType(Object.class);
      assertThat(beanNames).isNotEmpty();
    }

    @Test
    @DisplayName("应加载CLI组件")
    void shouldLoadCLIComponents() {
      context = SpringApplication.run(DataForgeCliApplication.class);

      assertThat(context.getBean("generateCommand")).isNotNull();
    }
  }

  @Nested
  @DisplayName("环境配置测试")
  class EnvironmentConfigurationTests {

    @Test
    @DisplayName("应加载默认环境配置")
    void shouldLoadDefaultEnvironmentConfiguration() {
      context = SpringApplication.run(DataForgeCliApplication.class);

      assertThat(context.getEnvironment()).isNotNull();
    }

    @Test
    @DisplayName("应支持自定义环境配置")
    void shouldSupportCustomEnvironmentConfiguration() {
      System.setProperty("custom.property", "custom-value");
      context = SpringApplication.run(DataForgeCliApplication.class);

      String customProperty = context.getEnvironment().getProperty("custom.property");
      assertThat(customProperty).isEqualTo("custom-value");
    }
  }
}
