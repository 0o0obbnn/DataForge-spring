package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.GeneratorTestBase;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * EmailGenerator 完整测试示例
 *
 * <p>基于优化计划中的关键Generator测试模板，提供全面的业务逻辑覆盖
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("EmailGenerator 测试")
class EmailGeneratorComprehensiveTest extends GeneratorTestBase<EmailGenerator> {

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

  @Override
  protected EmailGenerator createGenerator() {
    return new EmailGenerator();
  }

  @Override
  protected String getGeneratorType() {
    return "email";
  }

  @Override
  protected String getBusinessLogicPattern() {
    return "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
  }

  @Nested
  @DisplayName("格式验证测试")
  class FormatValidationTests {

    @Test
    @DisplayName("应生成符合RFC5322标准的邮箱地址")
    void shouldGenerateValidEmailFormat() {
      String email = generator.generate(config, context);

      assertThat(email).isNotNull();
      assertThat(email).matches(EMAIL_PATTERN);
      assertThat(email).contains("@");
      assertThat(email).contains(".");
    }

    @Test
    @DisplayName("应生成唯一的邮箱地址")
    void shouldGenerateUniqueEmails() {
      Set<String> emails = new HashSet<>();
      for (int i = 0; i < 100; i++) {
        String email = generator.generate(config, context);
        assertThat(emails.add(email)).isTrue();
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"gmail.com", "yahoo.com", "hotmail.com", "outlook.com"})
    @DisplayName("应支持自定义域名")
    void shouldSupportCustomDomains(String domain) {
      config.setParam("domain", domain);

      String email = generator.generate(config, context);

      assertThat(email).endsWith("@" + domain);
      assertThat(email).matches(EMAIL_PATTERN);
    }
  }

  @Nested
  @DisplayName("业务逻辑测试")
  class BusinessLogicTests {

    @Test
    @DisplayName("应生成不同长度的用户名部分")
    void shouldGenerateDifferentUsernameLengths() {
      config.setParam("usernameLength", "10");

      String email = generator.generate(config, context);
      String username = email.split("@")[0];

      assertThat(username).hasSize(10);
      assertThat(username).matches("^[a-zA-Z0-9]+$");
    }

    @Test
    @DisplayName("应处理特殊字符用户名")
    void shouldHandleSpecialCharacters() {
      config.setParam("allowSpecialChars", "true");

      String email = generator.generate(config, context);

      assertThat(email).matches(EMAIL_PATTERN);
    }

    @Test
    @DisplayName("应生成企业邮箱格式")
    void shouldGenerateCorporateEmailFormat() {
      config.setParam("format", "corporate");
      config.setParam("domain", "company.com");

      String email = generator.generate(config, context);

      assertThat(email).endsWith("@company.com");
      assertThat(email).matches("^[a-z]+\\.[a-z]+@company\\.com$");
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryConditionTests {

    @Test
    @DisplayName("应处理极短用户名")
    void shouldHandleVeryShortUsername() {
      config.setParam("usernameLength", "1");

      String email = generator.generate(config, context);

      assertThat(email).matches(EMAIL_PATTERN);
      String username = email.split("@")[0];
      assertThat(username).hasSize(1);
    }

    @Test
    @DisplayName("应处理超长用户名")
    void shouldHandleVeryLongUsername() {
      config.setParam("usernameLength", "64");

      String email = generator.generate(config, context);

      assertThat(email).matches(EMAIL_PATTERN);
      String username = email.split("@")[0];
      assertThat(username).hasSize(64);
    }

    @Test
    @DisplayName("应处理无效域名参数")
    void shouldHandleInvalidDomainParameter() {
      config.setParam("domain", "");

      String email = generator.generate(config, context);

      assertThat(email).matches(EMAIL_PATTERN);
    }
  }

  @Nested
  @DisplayName("性能测试")
  class PerformanceTests {

    @Test
    @DisplayName("应快速生成大量邮箱地址")
    void shouldGenerateEmailsQuickly() {
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < 1000; i++) {
        String email = generator.generate(config, context);
        assertThat(email).isNotNull();
      }

      long duration = System.currentTimeMillis() - startTime;
      assertThat(duration).isLessThan(1000); // 应在1秒内完成
    }

    @Test
    @DisplayName("应内存高效")
    void shouldBeMemoryEfficient() {
      Runtime runtime = Runtime.getRuntime();
      long initialMemory = runtime.totalMemory() - runtime.freeMemory();

      List<String> emails = new ArrayList<>();
      for (int i = 0; i < 10000; i++) {
        emails.add(generator.generate(config, context));
      }

      long finalMemory = runtime.totalMemory() - runtime.freeMemory();
      long memoryIncrease = finalMemory - initialMemory;

      // 10000个邮箱地址应占用合理内存
      assertThat(memoryIncrease).isLessThan(10 * 1024 * 1024); // 小于10MB
    }
  }
}
