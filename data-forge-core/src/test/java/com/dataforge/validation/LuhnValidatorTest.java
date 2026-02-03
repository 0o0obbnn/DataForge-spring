package com.dataforge.validation;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Luhn算法校验器测试。
 *
 * <p>测试Luhn算法在各种场景下的正确性，包括： - 基本校验功能 - 校验位生成 - 银行卡号校验 - IMEI号校验 - 边界情况处理
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("Luhn算法校验器测试")
class LuhnValidatorTest {

  private LuhnValidator validator;

  @BeforeEach
  void setUp() {
    validator = new LuhnValidator();
  }

  @Nested
  @DisplayName("基本校验测试")
  class BasicValidationTests {

    @Test
    @DisplayName("有效的银行卡号应通过校验")
    void validate_WithValidBankCard_ShouldPass() {
      // Given - 使用真实的银行卡号（测试用）
      String validCardNumber = "4111111111111111"; // Visa 测试卡号

      // When
      ValidationResult result = validator.validate(validCardNumber);

      // Then
      assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("无效的银行卡号应失败")
    void validate_WithInvalidBankCard_ShouldFail() {
      // Given - 修改最后一位使校验失败
      String invalidCardNumber = "4111111111111112";

      // When
      ValidationResult result = validator.validate(invalidCardNumber);

      // Then
      assertThat(result.isValid()).isFalse();
      assertThat(result.getFirstErrorMessage()).contains("Luhn checksum validation failed");
    }

    @Test
    @DisplayName("null 输入应失败")
    void validate_WithNullInput_ShouldFail() {
      // When
      ValidationResult result = validator.validate(null);

      // Then
      assertThat(result.isValid()).isFalse();
      assertThat(result.getFirstErrorMessage()).contains("cannot be null");
    }

    @Test
    @DisplayName("空字符串应失败")
    void validate_WithEmptyString_ShouldFail() {
      // When
      ValidationResult result = validator.validate("");

      // Then
      assertThat(result.isValid()).isFalse();
      assertThat(result.getFirstErrorMessage()).contains("at least one digit");
    }

    @Test
    @DisplayName("包含空格的银行卡号应通过校验")
    void validate_WithSpaces_ShouldPass() {
      // Given - 包含空格的银行卡号
      String cardNumberWithSpaces = "4111 1111 1111 1111";

      // When
      ValidationResult result = validator.validate(cardNumberWithSpaces);

      // Then
      assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("包含连字符的银行卡号应通过校验")
    void validate_WithHyphens_ShouldPass() {
      // Given - 包含连字符的银行卡号
      String cardNumberWithHyphens = "4111-1111-1111-1111";

      // When
      ValidationResult result = validator.validate(cardNumberWithHyphens);

      // Then
      assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("单个数字应失败")
    void validate_WithSingleDigit_ShouldFail() {
      // Given
      String singleDigit = "5";

      // When
      ValidationResult result = validator.validate(singleDigit);

      // Then
      assertThat(result.isValid()).isFalse();
      assertThat(result.getFirstErrorMessage()).contains("at least 2 digits");
    }

    @Test
    @DisplayName("只有非数字字符应失败")
    void validate_WithOnlyNonDigits_ShouldFail() {
      // Given
      String onlyLetters = "abcd-efgh-ijkl-mnop";

      // When
      ValidationResult result = validator.validate(onlyLetters);

      // Then
      assertThat(result.isValid()).isFalse();
      assertThat(result.getFirstErrorMessage()).contains("at least one digit");
    }
  }

  @Nested
  @DisplayName("校验位生成测试")
  class CheckDigitGenerationTests {

    @Test
    @DisplayName("应生成正确的校验位")
    void generateCheckDigit_ShouldReturnCorrectDigit() {
      // Given
      String partialNumber = "411111111111111"; // 15位，缺少校验位

      // When
      int checkDigit = validator.generateCheckDigit(partialNumber);

      // Then - 校验位应该是1，组成完整的16位卡号
      assertThat(checkDigit).isEqualTo(1);
    }

    @Test
    @DisplayName("null 输入应抛出异常")
    void generateCheckDigit_WithNullInput_ShouldThrowException() {
      // When & Then
      assertThatThrownBy(() -> validator.generateCheckDigit(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or empty");
    }

    @Test
    @DisplayName("空字符串应抛出异常")
    void generateCheckDigit_WithEmptyString_ShouldThrowException() {
      // When & Then
      assertThatThrownBy(() -> validator.generateCheckDigit(""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or empty");
    }

    @Test
    @DisplayName("生成完整号码应通过校验")
    void generateValidNumber_ShouldPassValidation() {
      // Given
      String partialNumber = "510510510510510"; // 15位

      // When
      String fullNumber = validator.generateValidNumber(partialNumber);

      // Then
      assertThat(fullNumber).hasSize(16);
      ValidationResult result = validator.validate(fullNumber);
      assertThat(result.isValid()).isTrue();
    }
  }

  @Nested
  @DisplayName("银行卡号校验测试")
  class BankCardValidationTests {

    @Test
    @DisplayName("有效的银行卡号应通过校验")
    void validateBankCard_WithValidCard_ShouldPass() {
      // Given - Visa 测试卡号
      String validCardNumber = "4111111111111111";

      // When
      ValidationResult result = validator.validateBankCard(validCardNumber);

      // Then
      assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("MasterCard 测试卡号应通过校验")
    void validateBankCard_WithValidMasterCard_ShouldPass() {
      // Given - MasterCard 测试卡号
      String masterCardNumber = "5555555555554444";

      // When
      ValidationResult result = validator.validateBankCard(masterCardNumber);

      // Then
      assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("长度不足的银行卡号应失败")
    void validateBankCard_WithTooShortCard_ShouldFail() {
      // Given - 12位，少于13位
      String shortCardNumber = "411111111111";

      // When
      ValidationResult result = validator.validateBankCard(shortCardNumber);

      // Then
      assertThat(result.isValid()).isFalse();
      assertThat(result.getFirstErrorMessage()).contains("13-19 digits");
    }

    @Test
    @DisplayName("长度过长的银行卡号应失败")
    void validateBankCard_WithTooLongCard_ShouldFail() {
      // Given - 20位，超过19位
      String longCardNumber = "41111111111111111111";

      // When
      ValidationResult result = validator.validateBankCard(longCardNumber);

      // Then
      assertThat(result.isValid()).isFalse();
      assertThat(result.getFirstErrorMessage()).contains("13-19 digits");
    }

    @Test
    @DisplayName("null 银行卡号应失败")
    void validateBankCard_WithNullCard_ShouldFail() {
      // When
      ValidationResult result = validator.validateBankCard(null);

      // Then
      assertThat(result.isValid()).isFalse();
      assertThat(result.getFirstErrorMessage()).contains("cannot be null");
    }
  }

  @Nested
  @DisplayName("IMEI号校验测试")
  class ImeiValidationTests {

    @Test
    @DisplayName("有效的IMEI应通过校验")
    void validateIMEI_WithValidImei_ShouldPass() {
      // Given - 使用有效的IMEI号
      String validImei = "490154203237518";

      // When
      ValidationResult result = validator.validateIMEI(validImei);

      // Then
      assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("无效的IMEI应失败")
    void validateIMEI_WithInvalidImei_ShouldFail() {
      // Given - 修改最后一位使校验失败
      String invalidImei = "490154203237519";

      // When
      ValidationResult result = validator.validateIMEI(invalidImei);

      // Then
      assertThat(result.isValid()).isFalse();
      assertThat(result.getFirstErrorMessage()).contains("Luhn checksum validation failed");
    }

    @Test
    @DisplayName("长度不足的IMEI应失败")
    void validateIMEI_WithTooShortImei_ShouldFail() {
      // Given - 14位
      String shortImei = "49015420323751";

      // When
      ValidationResult result = validator.validateIMEI(shortImei);

      // Then
      assertThat(result.isValid()).isFalse();
      assertThat(result.getFirstErrorMessage()).contains("exactly 15 digits");
    }

    @Test
    @DisplayName("长度过长的IMEI应失败")
    void validateIMEI_WithTooLongImei_ShouldFail() {
      // Given - 16位
      String longImei = "4901542032375189";

      // When
      ValidationResult result = validator.validateIMEI(longImei);

      // Then
      assertThat(result.isValid()).isFalse();
      assertThat(result.getFirstErrorMessage()).contains("exactly 15 digits");
    }

    @Test
    @DisplayName("null IMEI应失败")
    void validateIMEI_WithNullImei_ShouldFail() {
      // When
      ValidationResult result = validator.validateIMEI(null);

      // Then
      assertThat(result.isValid()).isFalse();
      assertThat(result.getFirstErrorMessage()).contains("cannot be null");
    }
  }

  @Nested
  @DisplayName("Validator 接口方法测试")
  class ValidatorInterfaceTests {

    @Test
    @DisplayName("getName 应返回正确的名称")
    void getName_ShouldReturnCorrectName() {
      // When
      String name = validator.getName();

      // Then
      assertThat(name).isEqualTo("Luhn");
    }

    @Test
    @DisplayName("getDescription 应返回正确的描述")
    void getDescription_ShouldReturnCorrectDescription() {
      // When
      String description = validator.getDescription();

      // Then
      assertThat(description).contains("Luhn algorithm");
      assertThat(description).contains("bank cards");
      assertThat(description).contains("IMEI");
    }

    @Test
    @DisplayName("isValid 方法应返回正确的布尔值")
    void isValid_WithValidNumber_ShouldReturnTrue() {
      // Given
      String validNumber = "4111111111111111";

      // When
      boolean isValid = validator.isValid(validNumber);

      // Then
      assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("isValid 方法对无效号码应返回 false")
    void isValid_WithInvalidNumber_ShouldReturnFalse() {
      // Given
      String invalidNumber = "4111111111111112";

      // When
      boolean isValid = validator.isValid(invalidNumber);

      // Then
      assertThat(isValid).isFalse();
    }
  }
}
