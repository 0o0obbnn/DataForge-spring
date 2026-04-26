package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.TestFieldConfig;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CookieGenerator单元测试
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("CookieGenerator - Cookie生成器测试")
class CookieGeneratorTest {

  private CookieGenerator generator;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new CookieGenerator();
    context = new DataForgeContext();
  }

  @Test
  @DisplayName("生成默认Cookie")
  void shouldGenerateDefaultCookie() {
    TestFieldConfig config = new TestFieldConfig("field", "cookie", new HashMap<>());

    String result = generator.generate(config, context);

    assertThat(result).isNotNull();
    assertThat(result).isNotEmpty();
  }

  @Test
  @DisplayName("生成带名称的Cookie")
  void shouldGenerateCookieWithName() {
    TestFieldConfig config = new TestFieldConfig("field", "cookie", new HashMap<>());
    config.set("name", "session_id");

    String result = generator.generate(config, context);

    assertThat(result).isNotNull();
    assertThat(result).contains("session_id");
  }

  @Test
  @DisplayName("生成HttpOnly Cookie")
  void shouldGenerateHttpOnlyCookie() {
    TestFieldConfig config = new TestFieldConfig("field", "cookie", new HashMap<>());
    config.set("http_only", true);

    String result = generator.generate(config, context);

    assertThat(result).isNotNull();
    assertThat(result.toLowerCase()).contains("httponly");
  }

  @Test
  @DisplayName("生成Secure Cookie")
  void shouldGenerateSecureCookie() {
    TestFieldConfig config = new TestFieldConfig("field", "cookie", new HashMap<>());
    config.set("secure", true);

    String result = generator.generate(config, context);

    assertThat(result).isNotNull();
    assertThat(result.toLowerCase()).contains("secure");
  }

  @Test
  @DisplayName("验证生成器类型")
  void shouldReturnCorrectType() {
    assertThat(generator.getType()).isEqualTo("cookie");
  }
}
