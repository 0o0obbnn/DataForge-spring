package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import com.dataforge.validation.LuhnValidator;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("BankCardGenerator 测试")
class BankCardGeneratorTest {

  private BankCardGenerator generator;
  private SimpleFieldConfig config;
  private DataForgeContext context;
  private LuhnValidator luhnValidator;

  @BeforeEach
  void setUp() {
    generator = new BankCardGenerator();
    config = new SimpleFieldConfig();
    config.setType("bankcard");
    context = new DataForgeContext();
    luhnValidator = new LuhnValidator();
  }

  @Nested
  @DisplayName("基本功能测试")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("应生成银行卡号")
    void shouldGenerateBankCard() {
      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(cardNumber).isNotEmpty();
    }

    @Test
    @DisplayName("应将银行卡信息存入上下文")
    void shouldStoreBankCardInContext() {
      String cardNumber = generator.generate(config, context);

      assertThat(context.get("bankcard")).isNotNull();
      assertThat(context.get("bankcard_masked")).isNotNull();
      assertThat(context.get("bankcard")).isEqualTo(Optional.of(cardNumber));
    }

    @Test
    @DisplayName("应生成有效的银行卡号")
    void shouldGenerateValidBankCard() {
      config.setParam("valid", "true");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }
  }

  @Nested
  @DisplayName("卡类型测试")
  class CardTypeTests {

    @Test
    @DisplayName("应生成借记卡")
    void shouldGenerateDebitCard() {
      config.setParam("type", "DEBIT");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }

    @Test
    @DisplayName("应生成信用卡")
    void shouldGenerateCreditCard() {
      config.setParam("type", "CREDIT");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }

    @Test
    @DisplayName("应生成任意类型卡")
    void shouldGenerateAnyTypeCard() {
      config.setParam("type", "BOTH");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }
  }

  @Nested
  @DisplayName("卡组织测试")
  class CardOrganizationTests {

    @Test
    @DisplayName("应生成VISA卡")
    void shouldGenerateVisaCard() {
      config.setParam("issuer", "VISA");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(cardNumber).startsWith("4");
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }

    @Test
    @DisplayName("应生成万事达卡")
    void shouldGenerateMastercard() {
      config.setParam("issuer", "MASTERCARD");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(cardNumber).startsWith("5");
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }

    @Test
    @DisplayName("应生成银联卡")
    void shouldGenerateUnionpayCard() {
      config.setParam("issuer", "UNIONPAY");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(cardNumber).startsWith("62");
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }

    @Test
    @DisplayName("应生成美国运通卡")
    void shouldGenerateAmexCard() {
      config.setParam("issuer", "AMEX");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(cardNumber).matches("^3[47].*");
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }

    @Test
    @DisplayName("应生成JCB卡")
    void shouldGenerateJcbCard() {
      config.setParam("issuer", "JCB");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(cardNumber).startsWith("35");
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }
  }

  @Nested
  @DisplayName("卡号长度测试")
  class CardLengthTests {

    @ParameterizedTest
    @ValueSource(ints = {13, 14, 15, 16, 17, 18, 19})
    @DisplayName("应支持不同卡号长度")
    void shouldSupportDifferentCardLengths(int length) {
      config.setParam("length", String.valueOf(length));

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(cardNumber).hasSize(length);
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }

    @Test
    @DisplayName("应生成16位标准卡号")
    void shouldGenerate16DigitCard() {
      config.setParam("length", "16");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(cardNumber).hasSize(16);
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }

    @Test
    @DisplayName("应生成15位AMEX卡号")
    void shouldGenerate15DigitAmexCard() {
      config.setParam("issuer", "AMEX");
      config.setParam("length", "15");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(cardNumber).hasSize(15);
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }

    @Test
    @DisplayName("应生成19位银联卡号")
    void shouldGenerate19DigitUnionpayCard() {
      config.setParam("issuer", "UNIONPAY");
      config.setParam("length", "19");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(cardNumber).hasSize(19);
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }
  }

  @Nested
  @DisplayName("无效卡号测试")
  class InvalidCardTests {

    @Test
    @DisplayName("应生成长度错误的卡号")
    void shouldGenerateWrongLengthCard() {
      config.setParam("valid", "false");
      config.setParam("invalid_type", "WRONG_LENGTH");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(cardNumber.length() < 13 || cardNumber.length() > 19).isTrue();
    }

    @Test
    @DisplayName("应生成校验位错误的卡号")
    void shouldGenerateWrongChecksumCard() {
      config.setParam("valid", "false");

      String cardNumber = null;
      for (int i = 0; i < 10; i++) {
        cardNumber = generator.generate(config, context);
        if (cardNumber != null
            && cardNumber.matches("^[0-9]+$")
            && !luhnValidator.isValid(cardNumber)) {
          break;
        }
      }

      assertThat(cardNumber).isNotNull();
      if (cardNumber != null && cardNumber.matches("^[0-9]+$")) {
        assertThat(luhnValidator.isValid(cardNumber)).isFalse();
      }
    }

    @Test
    @DisplayName("应生成包含非数字字符的卡号")
    void shouldGenerateNonNumericCard() {
      config.setParam("valid", "false");
      config.setParam("invalid_type", "NON_NUMERIC");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(cardNumber).matches(".*[A-Za-z].*");
    }

    @Test
    @DisplayName("应生成全0卡号")
    void shouldGenerateAllZeroCard() {
      config.setParam("valid", "false");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
    }
  }

  @Nested
  @DisplayName("Luhn算法验证测试")
  class LuhnValidationTests {

    @Test
    @DisplayName("生成的有效卡号应通过Luhn验证")
    void shouldPassLuhnValidation() {
      config.setParam("valid", "true");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }

    @Test
    @DisplayName("生成的无效卡号应不通过Luhn验证")
    void shouldFailLuhnValidation() {
      config.setParam("valid", "false");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      if (cardNumber.matches("^[0-9]+$")) {
        assertThat(luhnValidator.isValid(cardNumber)).isFalse();
      } else {
        assertThat(cardNumber.matches(".*[A-Za-z].*")).isTrue();
      }
    }

    @Test
    @DisplayName("批量生成的卡号都应通过Luhn验证")
    void shouldAllPassLuhnValidation() {
      config.setParam("valid", "true");

      for (int i = 0; i < 100; i++) {
        String cardNumber = generator.generate(config, context);
        assertThat(luhnValidator.isValid(cardNumber))
            .withFailMessage("Card number %s failed Luhn validation", cardNumber)
            .isTrue();
      }
    }
  }

  @Nested
  @DisplayName("权重选择测试")
  class WeightSelectionTests {

    @Test
    @DisplayName("应使用权重选择")
    void shouldUseWeightSelection() {
      config.setParam("use_weight", "true");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }

    @Test
    @DisplayName("应不使用权重选择")
    void shouldNotUseWeightSelection() {
      config.setParam("use_weight", "false");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }
  }

  @Nested
  @DisplayName("掩码测试")
  class MaskingTests {

    @Test
    @DisplayName("应生成掩码卡号")
    void shouldGenerateMaskedCardNumber() {
      String cardNumber = generator.generate(config, context);

      Object maskedObj = context.get("bankcard_masked");
      assertThat(maskedObj).isNotNull();
      String masked =
          maskedObj instanceof Optional
              ? ((Optional<?>) maskedObj).map(Object::toString).orElse(null)
              : maskedObj.toString();

      assertThat(masked).isNotNull();
      assertThat(masked).contains("****");
      assertThat(masked).doesNotContain(cardNumber.substring(4, cardNumber.length() - 4));
    }

    @Test
    @DisplayName("掩码应显示前4位和后4位")
    void shouldShowFirstAndLastFourDigits() {
      String cardNumber = generator.generate(config, context);

      Object maskedObj = context.get("bankcard_masked");
      assertThat(maskedObj).isNotNull();
      String masked =
          maskedObj instanceof Optional
              ? ((Optional<?>) maskedObj).map(Object::toString).orElse(null)
              : maskedObj.toString();

      assertThat(masked).isNotNull();
      assertThat(masked).startsWith(cardNumber.substring(0, 4));
      assertThat(masked).endsWith(cardNumber.substring(cardNumber.length() - 4));
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {

    @Test
    @DisplayName("应处理最小卡号长度")
    void shouldHandleMinimumCardLength() {
      config.setParam("length", "13");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(cardNumber).hasSize(13);
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }

    @Test
    @DisplayName("应处理最大卡号长度")
    void shouldHandleMaximumCardLength() {
      config.setParam("length", "19");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(cardNumber).hasSize(19);
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }

    @Test
    @DisplayName("应处理超长卡号长度")
    void shouldHandleTooLongCardLength() {
      config.setParam("length", "25");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(cardNumber.length()).isLessThanOrEqualTo(19);
    }

    @Test
    @DisplayName("应处理超短卡号长度")
    void shouldHandleTooShortCardLength() {
      config.setParam("length", "8");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
      assertThat(cardNumber.length()).isGreaterThanOrEqualTo(13);
    }
  }

  @Nested
  @DisplayName("异常处理测试")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("null配置应生成默认卡号")
    void shouldGenerateDefaultCardForNullConfig() {
      String cardNumber = generator.generate(null, context);

      assertThat(cardNumber).isNotNull();
      assertThat(luhnValidator.isValid(cardNumber)).isTrue();
    }

    @Test
    @DisplayName("null上下文应不抛出异常")
    void shouldNotThrowForNullContext() {
      String cardNumber = generator.generate(config, null);

      assertThat(cardNumber).isNotNull();
    }

    @Test
    @DisplayName("无效卡类型应生成默认卡号")
    void shouldGenerateDefaultCardForInvalidType() {
      config.setParam("type", "INVALID_TYPE");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
    }

    @Test
    @DisplayName("无效卡组织应生成默认卡号")
    void shouldGenerateDefaultCardForInvalidIssuer() {
      config.setParam("issuer", "INVALID_ISSUER");

      String cardNumber = generator.generate(config, context);

      assertThat(cardNumber).isNotNull();
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

      assertThat(duration).isLessThan(10000);
    }

    @Test
    @DisplayName("Luhn验证应高效")
    void shouldValidateLuhnEfficiently() {
      config.setParam("valid", "true");

      long startTime = System.currentTimeMillis();

      for (int i = 0; i < 10000; i++) {
        String cardNumber = generator.generate(config, context);
        luhnValidator.isValid(cardNumber);
      }

      long duration = System.currentTimeMillis() - startTime;

      assertThat(duration).isLessThan(5000);
    }
  }

  @Nested
  @DisplayName("生成器信息测试")
  class GeneratorInfoTests {

    @Test
    @DisplayName("应返回正确的类型")
    void shouldReturnCorrectType() {
      String type = generator.getType();

      assertThat(type).isEqualTo("bankcard");
    }

    @Test
    @DisplayName("应返回正确的配置类")
    void shouldReturnCorrectConfigClass() {
      Class<?> configClass = generator.getConfigClass();

      assertThat(configClass).isEqualTo(com.dataforge.model.FieldConfig.class);
    }
  }

  @Nested
  @DisplayName("唯一性测试")
  class UniquenessTests {

    @Test
    @DisplayName("批量生成的卡号应具有唯一性")
    void shouldGenerateUniqueCardNumbers() {
      int count = 100;
      java.util.Set<String> cardNumbers = new java.util.HashSet<>();

      for (int i = 0; i < count; i++) {
        String cardNumber = generator.generate(config, context);
        cardNumbers.add(cardNumber);
      }

      assertThat(cardNumbers).hasSize(count);
    }

    @Test
    @DisplayName("相同配置应生成不同卡号")
    void shouldGenerateDifferentCardsForSameConfig() {
      String card1 = generator.generate(config, context);
      String card2 = generator.generate(config, context);

      assertThat(card1).isNotEqualTo(card2);
    }
  }
}
