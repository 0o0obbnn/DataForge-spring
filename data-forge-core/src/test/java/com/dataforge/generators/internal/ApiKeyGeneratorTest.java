package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ApiKeyGenerator 测试")
class ApiKeyGeneratorTest {

  private ApiKeyGenerator generator;
  private SimpleFieldConfig config;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new ApiKeyGenerator();
    config = new SimpleFieldConfig();
    config.setType("apikey");
    context = new DataForgeContext();
  }

  @Nested
  @DisplayName("基本功能测试")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("应生成非空API密钥")
    void shouldGenerateNonNullApiKey() {
      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey).isNotEmpty();
    }

    @Test
    @DisplayName("应生成符合长度的密钥")
    void shouldGenerateApiKeyWithCorrectLength() {
      config.setParam("length", "32");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey.length()).isGreaterThanOrEqualTo(16);
    }

    @Test
    @DisplayName("应将API密钥信息存入上下文")
    void shouldStoreApiKeyInfoInContext() {
      config.setParam("type", "JWT");

      generator.generate(config, context);

      assertThat(context.get("api_key")).isNotNull();
      assertThat(context.get("api_key_type")).isEqualTo(Optional.of("JWT"));
      assertThat(context.get("api_key_format")).isNotNull();
    }
  }

  @Nested
  @DisplayName("密钥类型测试")
  class KeyTypeTests {

    @Test
    @DisplayName("应生成JWT类型密钥")
    void shouldGenerateJwtTypeKey() {
      config.setParam("type", "JWT");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey.split("\\.").length).isEqualTo(3);
    }

    @Test
    @DisplayName("应生成Bearer类型密钥")
    void shouldGenerateBearerTypeKey() {
      config.setParam("type", "BEARER");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey).startsWith("Bearer ");
    }

    @Test
    @DisplayName("应生成Basic类型密钥")
    void shouldGenerateBasicTypeKey() {
      config.setParam("type", "BASIC");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey).startsWith("Basic ");
    }

    @Test
    @DisplayName("应生成Custom类型密钥")
    void shouldGenerateCustomTypeKey() {
      config.setParam("type", "CUSTOM");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey).matches("^[a-zA-Z0-9_-]{16,64}$");
    }

    @ParameterizedTest
    @ValueSource(strings = {"JWT", "BEARER", "BASIC", "CUSTOM"})
    @DisplayName("应支持所有密钥类型")
    void shouldSupportAllKeyTypes(String type) {
      config.setParam("type", type);

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
    }
  }

  @Nested
  @DisplayName("格式类型测试")
  class FormatTypeTests {

    @Test
    @DisplayName("应生成BASE64格式密钥")
    void shouldGenerateBase64FormatKey() {
      config.setParam("format", "BASE64");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey).matches("^[A-Za-z0-9+/=_-]+$");
    }

    @Test
    @DisplayName("应生成HEX格式密钥")
    void shouldGenerateHexFormatKey() {
      config.setParam("format", "HEX");
      config.setParam("prefix", "");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey).matches("^[0-9a-f]+$");
    }

    @Test
    @DisplayName("应生成ALPHANUMERIC格式密钥")
    void shouldGenerateAlphanumericFormatKey() {
      config.setParam("format", "ALPHANUMERIC");
      config.setParam("prefix", "");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey).matches("^[A-Za-z0-9]+$");
    }

    @ParameterizedTest
    @ValueSource(strings = {"BASE64", "HEX", "ALPHANUMERIC"})
    @DisplayName("应支持所有格式类型")
    void shouldSupportAllFormats(String format) {
      config.setParam("format", format);

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
    }
  }

  @Nested
  @DisplayName("前缀测试")
  class PrefixTests {

    @Test
    @DisplayName("应使用自定义前缀")
    void shouldUseCustomPrefix() {
      config.setParam("prefix", "sk_");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey).startsWith("sk_");
    }

    @Test
    @DisplayName("应使用Bearer前缀")
    void shouldUseBearerPrefix() {
      config.setParam("type", "BEARER");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).startsWith("Bearer ");
    }

    @Test
    @DisplayName("应使用Basic前缀")
    void shouldUseBasicPrefix() {
      config.setParam("type", "BASIC");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).startsWith("Basic ");
    }

    @Test
    @DisplayName("JWT类型应无前缀")
    void shouldHaveNoPrefixForJwt() {
      config.setParam("type", "JWT");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).doesNotStartWith("Bearer ");
      assertThat(apiKey).doesNotStartWith("Basic ");
    }

    @Test
    @DisplayName("应支持空前缀")
    void shouldSupportEmptyPrefix() {
      config.setParam("prefix", "");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
    }
  }

  @Nested
  @DisplayName("校验和测试")
  class ChecksumTests {

    @Test
    @DisplayName("应生成包含校验和的密钥")
    void shouldGenerateKeyWithChecksum() {
      config.setParam("include_checksum", "true");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey.length()).isGreaterThan(4);
    }

    @Test
    @DisplayName("应生成不包含校验和的密钥")
    void shouldGenerateKeyWithoutChecksum() {
      config.setParam("include_checksum", "false");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
    }

    @Test
    @DisplayName("校验和应为4位十六进制")
    void shouldHave4DigitHexChecksum() {
      config.setParam("include_checksum", "true");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
      String checksum = apiKey.substring(apiKey.length() - 4);
      assertThat(checksum).matches("^[0-9a-f]{4}$");
    }
  }

  @Nested
  @DisplayName("安全性测试")
  class SecurityTests {

    @Test
    @DisplayName("应使用安全随机数生成器")
    void shouldUseSecureRandom() {
      config.setParam("secure", "true");

      String apiKey1 = generator.generate(config, context);
      String apiKey2 = generator.generate(config, context);

      assertThat(apiKey1).isNotEqualTo(apiKey2);
    }

    @Test
    @DisplayName("应支持非安全随机数生成器")
    void shouldSupportNonSecureRandom() {
      config.setParam("secure", "false");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
    }

    @Test
    @DisplayName("多次生成应产生不同密钥")
    void shouldGenerateDifferentKeys() {
      String key1 = generator.generate(config, context);
      String key2 = generator.generate(config, context);
      String key3 = generator.generate(config, context);

      assertThat(key1).isNotEqualTo(key2);
      assertThat(key2).isNotEqualTo(key3);
      assertThat(key1).isNotEqualTo(key3);
    }
  }

  @Nested
  @DisplayName("特定服务风格测试")
  class ServiceStyleTests {

    @Test
    @DisplayName("应生成Stripe风格密钥")
    void shouldGenerateStripeStyleKey() {
      String apiKey = generator.generateServiceApiKey("STRIPE", 32);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey).matches("^(sk_|pk_|rk_)[A-Za-z0-9]+$");
    }

    @Test
    @DisplayName("应生成GitHub风格密钥")
    void shouldGenerateGithubStyleKey() {
      String apiKey = generator.generateServiceApiKey("GITHUB", 40);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey).matches("^(ghp_|gho_|ghu_|ghs_|ghr_)[A-Za-z0-9]+$");
    }

    @Test
    @DisplayName("应生成OpenAI风格密钥")
    void shouldGenerateOpenAIStyleKey() {
      String apiKey = generator.generateServiceApiKey("OPENAI", 48);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey).startsWith("sk-");
    }

    @Test
    @DisplayName("应生成AWS风格密钥")
    void shouldGenerateAwsStyleKey() {
      String apiKey = generator.generateServiceApiKey("AWS", 20);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey).matches("^(AKIA|ASIA)[A-Za-z0-9]+$");
    }

    @Test
    @DisplayName("应生成Google风格密钥")
    void shouldGenerateGoogleStyleKey() {
      String apiKey = generator.generateServiceApiKey("GOOGLE", 39);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey).startsWith("AIza");
    }

    @Test
    @DisplayName("应生成Slack风格密钥")
    void shouldGenerateSlackStyleKey() {
      String apiKey = generator.generateServiceApiKey("SLACK", 30);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey).matches("^(xoxb-|xoxp-|xoxa-)[A-Za-z0-9-]+$");
    }
  }

  @Nested
  @DisplayName("密钥对生成测试")
  class KeyPairTests {

    @Test
    @DisplayName("应生成公钥/私钥对")
    void shouldGenerateKeyPair() {
      Map<String, String> keyPair = generator.generateApiKeyPair();

      assertThat(keyPair).isNotNull();
      assertThat(keyPair).containsKeys("public_key", "secret_key");
      assertThat(keyPair.get("public_key")).startsWith("pk_");
      assertThat(keyPair.get("secret_key")).startsWith("sk_");
    }

    @Test
    @DisplayName("公钥和私钥应不同")
    void shouldHaveDifferentPublicAndSecretKeys() {
      Map<String, String> keyPair = generator.generateApiKeyPair();

      assertThat(keyPair.get("public_key")).isNotEqualTo(keyPair.get("secret_key"));
    }

    @Test
    @DisplayName("多次生成应产生不同密钥对")
    void shouldGenerateDifferentKeyPairs() {
      Map<String, String> pair1 = generator.generateApiKeyPair();
      Map<String, String> pair2 = generator.generateApiKeyPair();

      assertThat(pair1.get("public_key")).isNotEqualTo(pair2.get("public_key"));
      assertThat(pair1.get("secret_key")).isNotEqualTo(pair2.get("secret_key"));
    }
  }

  @Nested
  @DisplayName("临时Token测试")
  class TemporaryTokenTests {

    @Test
    @DisplayName("应生成临时token")
    void shouldGenerateTemporaryToken() {
      String token = generator.generateTemporaryToken(60);

      assertThat(token).isNotNull();
      assertThat(token).startsWith("tmp_");
    }

    @Test
    @DisplayName("临时token应包含过期时间")
    void shouldIncludeExpirationInTemporaryToken() {
      String token = generator.generateTemporaryToken(30);

      assertThat(token).isNotNull();
      assertThat(token).matches("^tmp_[A-Za-z0-9]+_[0-9a-f]+$");
    }

    @Test
    @DisplayName("不同过期时间应生成不同token")
    void shouldGenerateDifferentTokensForDifferentExpirations() {
      String token1 = generator.generateTemporaryToken(30);
      String token2 = generator.generateTemporaryToken(60);

      assertThat(token1).isNotEqualTo(token2);
    }
  }

  @Nested
  @DisplayName("格式验证测试")
  class FormatValidationTests {

    @Test
    @DisplayName("应验证JWT格式")
    void shouldValidateJwtFormat() {
      String jwt = "header.payload.signature";
      boolean isValid = generator.validateApiKeyFormat(jwt, "JWT");

      assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("应验证Bearer格式")
    void shouldValidateBearerFormat() {
      String bearer = "Bearer abc123";
      boolean isValid = generator.validateApiKeyFormat(bearer, "BEARER");

      assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("应验证Basic格式")
    void shouldValidateBasicFormat() {
      String basic = "Basic abc123";
      boolean isValid = generator.validateApiKeyFormat(basic, "BASIC");

      assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("应验证Custom格式")
    void shouldValidateCustomFormat() {
      String custom = "api_1234567890123456";
      boolean isValid = generator.validateApiKeyFormat(custom, "CUSTOM");

      assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("应拒绝无效JWT格式")
    void shouldRejectInvalidJwtFormat() {
      String invalidJwt = "invalid.jwt";
      boolean isValid = generator.validateApiKeyFormat(invalidJwt, "JWT");

      assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("应拒绝空密钥")
    void shouldRejectEmptyKey() {
      boolean isValid = generator.validateApiKeyFormat("", "CUSTOM");

      assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("应拒绝null密钥")
    void shouldRejectNullKey() {
      boolean isValid = generator.validateApiKeyFormat(null, "CUSTOM");

      assertThat(isValid).isFalse();
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {

    @ParameterizedTest
    @ValueSource(ints = {16, 32, 64, 128})
    @DisplayName("应支持不同密钥长度")
    void shouldSupportDifferentKeyLengths(int length) {
      config.setParam("length", String.valueOf(length));

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey.length()).isGreaterThanOrEqualTo(16);
    }

    @Test
    @DisplayName("应处理最小长度")
    void shouldHandleMinimumLength() {
      config.setParam("length", "1");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
    }

    @Test
    @DisplayName("应处理超大长度")
    void shouldHandleVeryLargeLength() {
      config.setParam("length", "1000");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
    }

    @Test
    @DisplayName("应处理无效长度")
    void shouldHandleInvalidLength() {
      config.setParam("length", "invalid");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
    }
  }

  @Nested
  @DisplayName("异常处理测试")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("null配置应返回默认密钥")
    void shouldReturnDefaultKeyForNullConfig() {
      String apiKey = generator.generate(null, context);

      assertThat(apiKey).isNotNull();
      assertThat(apiKey).startsWith("sk_test_");
    }

    @Test
    @DisplayName("null上下文应不抛出异常")
    void shouldNotThrowExceptionForNullContext() {
      String apiKey = generator.generate(config, null);

      assertThat(apiKey).isNotNull();
    }

    @Test
    @DisplayName("无效类型应使用默认值")
    void shouldUseDefaultForInvalidType() {
      config.setParam("type", "INVALID_TYPE");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
    }

    @Test
    @DisplayName("无效格式应使用默认值")
    void shouldUseDefaultForInvalidFormat() {
      config.setParam("format", "INVALID_FORMAT");

      String apiKey = generator.generate(config, context);

      assertThat(apiKey).isNotNull();
    }
  }

  @Nested
  @DisplayName("性能测试")
  class PerformanceTests {

    @Test
    @DisplayName("批量生成应高效")
    void shouldGenerateBatchEfficiently() {
      int count = 1000;
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < count; i++) {
        generator.generate(config, context);
      }

      long duration = System.currentTimeMillis() - startTime;

      assertThat(duration).isLessThan(5000);
    }

    @Test
    @DisplayName("JWT生成应高效")
    void shouldGenerateJwtEfficiently() {
      config.setParam("type", "JWT");

      long startTime = System.currentTimeMillis();

      for (int i = 0; i < 100; i++) {
        generator.generate(config, context);
      }

      long duration = System.currentTimeMillis() - startTime;

      assertThat(duration).isLessThan(2000);
    }

    @Test
    @DisplayName("密钥对生成应高效")
    void shouldGenerateKeyPairsEfficiently() {
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < 100; i++) {
        generator.generateApiKeyPair();
      }

      long duration = System.currentTimeMillis() - startTime;

      assertThat(duration).isLessThan(2000);
    }
  }

  @Nested
  @DisplayName("生成器信息测试")
  class GeneratorInfoTests {

    @Test
    @DisplayName("应返回正确的类型")
    void shouldReturnCorrectType() {
      String type = generator.getType();

      assertThat(type).isEqualTo("apikey");
    }

    @Test
    @DisplayName("应返回正确的配置类")
    void shouldReturnCorrectConfigClass() {
      Class<?> configClass = generator.getConfigClass();

      assertThat(configClass).isEqualTo(com.dataforge.model.FieldConfig.class);
    }
  }
}
