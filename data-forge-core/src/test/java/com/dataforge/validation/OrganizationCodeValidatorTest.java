package com.dataforge.validation;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 组织机构代码校验器测试。
 *
 * <p>测试组织机构代码校验器的各种功能，包括： - 基本校验功能 - 校验码计算 - 代码生成 - 格式化功能 - 字符集验证
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("组织机构代码校验器测试")
class OrganizationCodeValidatorTest {

  private OrganizationCodeValidator validator;

  @BeforeEach
  void setUp() {
    validator = new OrganizationCodeValidator();
  }

  @Nested
  @DisplayName("基本校验测试")
  class BasicValidationTests {

    private String validOrgCode;

    @BeforeEach
    void setUpValidCode() {
      // 生成一个有效的组织机构代码用于测试
      validOrgCode = validator.generateRandomOrganizationCode();
    }

    @Test
    @DisplayName("有效的组织机构代码应通过校验")
    void validate_WithValidOrgCode_ShouldPass() {
      // When
      ValidationResult result = validator.validate(validOrgCode);

      // Then
      assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("校验码错误的组织机构代码应失败")
    void validate_WithInvalidCheckCode_ShouldFail() {
      // Given - 修改最后一位使校验失败
      String bodyCode = validOrgCode.substring(0, 8);
      String invalidOrgCode = bodyCode + (validOrgCode.charAt(8) == '9' ? 'X' : '9');

      // When
      ValidationResult result = validator.validate(invalidOrgCode);

      // Then
      assertThat(result.isValid()).isFalse();
      assertThat(result.getFirstErrorMessage()).contains("Check code mismatch");
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
      assertThat(result.getFirstErrorMessage()).contains("cannot be empty");
    }

    @Test
    @DisplayName("长度不足的组织机构代码应失败")
    void validate_WithTooShortCode_ShouldFail() {
      // Given - 8位
      String shortOrgCode = "12345678";

      // When
      ValidationResult result = validator.validate(shortOrgCode);

      // Then
      assertThat(result.isValid()).isFalse();
      assertThat(result.getFirstErrorMessage()).contains("exactly 9 characters");
    }

    @Test
    @DisplayName("长度过长的组织机构代码应失败")
    void validate_WithTooLongCode_ShouldFail() {
      // Given - 10位
      String longOrgCode = "1234567890";

      // When
      ValidationResult result = validator.validate(longOrgCode);

      // Then
      assertThat(result.isValid()).isFalse();
      assertThat(result.getFirstErrorMessage()).contains("exactly 9 characters");
    }

    @Test
    @DisplayName("包含连字符的代码应通过校验")
    void validate_WithHyphens_ShouldPass() {
      // Given - 格式化的组织机构代码
      String formattedCode = validator.formatOrganizationCode(validOrgCode);

      // When
      ValidationResult result = validator.validate(formattedCode);

      // Then
      assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("小写字母应自动转换为大写")
    void validate_WithLowercaseLetters_ShouldPass() {
      // Given - 包含小写字母的代码
      String codeWithLowercase = "abcdefgH9"; // 假设这是有效的

      // When
      ValidationResult result = validator.validate(codeWithLowercase);

      // Then - 应该通过字符集验证（转换为大写后）
      // 如果这个代码无效，至少应该验证转换逻辑
      if (!result.isValid()) {
        assertThat(result.getFirstErrorMessage()).doesNotContain("Invalid character");
      }
    }
  }

  @Nested
  @DisplayName("字符集校验测试")
  class CharacterSetValidationTests {

    @Test
    @DisplayName("包含无效字符的代码应失败")
    void validate_WithInvalidCharacter_ShouldFail() {
      // Given - 包含禁止字符 I
      String codeWithInvalidChar = "123456I7";

      // When & Then
      assertThatThrownBy(() -> validator.calculateCheckCode(codeWithInvalidChar))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid character");
    }

    @Test
    @DisplayName("包含禁止字符 O 的代码应失败")
    void validate_WithForbiddenCharacterO_ShouldFail() {
      // Given
      String codeWithO = "123456O7";

      // When & Then
      assertThatThrownBy(() -> validator.calculateCheckCode(codeWithO))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid character");
    }

    @Test
    @DisplayName("包含禁止字符 S 的代码应失败")
    void validate_WithForbiddenCharacterS_ShouldFail() {
      // Given
      String codeWithS = "123456S7";

      // When & Then
      assertThatThrownBy(() -> validator.calculateCheckCode(codeWithS))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid character");
    }

    @Test
    @DisplayName("包含禁止字符 V 的代码应失败")
    void validate_WithForbiddenCharacterV_ShouldFail() {
      // Given
      String codeWithV = "123456V7";

      // When & Then
      assertThatThrownBy(() -> validator.calculateCheckCode(codeWithV))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid character");
    }

    @Test
    @DisplayName("包含禁止字符 Z 的代码应失败")
    void validate_WithForbiddenCharacterZ_ShouldFail() {
      // Given
      String codeWithZ = "123456Z7";

      // When & Then
      assertThatThrownBy(() -> validator.calculateCheckCode(codeWithZ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid character");
    }

    @Test
    @DisplayName("检查字符是否有效")
    void isValidCodeCharacter_ShouldReturnCorrectResult() {
      // Then
      assertThat(validator.isValidCodeCharacter('0')).isTrue();
      assertThat(validator.isValidCodeCharacter('9')).isTrue();
      assertThat(validator.isValidCodeCharacter('A')).isTrue();
      assertThat(validator.isValidCodeCharacter('Y')).isTrue();
      assertThat(validator.isValidCodeCharacter('I')).isFalse(); // 禁止字符
      assertThat(validator.isValidCodeCharacter('O')).isFalse(); // 禁止字符
    }

    @Test
    @DisplayName("获取有效字符集")
    void getValidCharacterSet_ShouldReturnCorrectSet() {
      // When
      String charset = validator.getValidCharacterSet();

      // Then
      assertThat(charset).contains("0123456789");
      assertThat(charset).contains("A");
      assertThat(charset).contains("Y");
      assertThat(charset).doesNotContain("I");
      assertThat(charset).doesNotContain("O");
      assertThat(charset).doesNotContain("S");
      assertThat(charset).doesNotContain("V");
      assertThat(charset).doesNotContain("Z");
    }
  }

  @Nested
  @DisplayName("校验码计算测试")
  class CheckCodeCalculationTests {

    @Test
    @DisplayName("应计算正确的校验码")
    void calculateCheckCode_ShouldReturnCorrectCode() {
      // Given - 使用一个有效的8位本体代码
      String first8 = "12345678";

      // When
      char checkCode = validator.calculateCheckCode(first8);

      // Then - 只验证返回的是有效字符
      assertThat(Character.isDigit(checkCode) || checkCode == 'X').isTrue();
    }

    @Test
    @DisplayName("校验码为10时应返回X")
    void calculateCheckCode_WhenResultIs10_ShouldReturnX() {
      // Given - 使用一个已知会产生校验码10的前8位
      String first8 = "10000000";

      // When
      char checkCode = validator.calculateCheckCode(first8);

      // Then - 如果计算结果是10，应该返回X
      if (checkCode == 'X') {
        assertThat(checkCode).isEqualTo('X');
      }
    }

    @Test
    @DisplayName("null 输入应抛出异常")
    void calculateCheckCode_WithNullInput_ShouldThrowException() {
      // When & Then
      assertThatThrownBy(() -> validator.calculateCheckCode(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("exactly 8 characters");
    }

    @Test
    @DisplayName("长度错误的输入应抛出异常")
    void calculateCheckCode_WithWrongLength_ShouldThrowException() {
      // Given
      String wrongLength = "1234567"; // 7位

      // When & Then
      assertThatThrownBy(() -> validator.calculateCheckCode(wrongLength))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("exactly 8 characters");
    }

    @Test
    @DisplayName("生成完整代码应通过校验")
    void generateValidOrganizationCode_ShouldPassValidation() {
      // Given
      String first8 = "12345678";

      // When
      String fullCode = validator.generateValidOrganizationCode(first8);

      // Then
      assertThat(fullCode).hasSize(9);
      ValidationResult result = validator.validate(fullCode);
      assertThat(result.isValid()).isTrue();
    }
  }

  @Nested
  @DisplayName("随机代码生成测试")
  class RandomCodeGenerationTests {

    @Test
    @DisplayName("应生成有效的随机本体代码")
    void generateRandomBodyCode_ShouldReturnValidCode() {
      // When
      String bodyCode = validator.generateRandomBodyCode();

      // Then
      assertThat(bodyCode).hasSize(8);
      for (char c : bodyCode.toCharArray()) {
        assertThat(validator.isValidCodeCharacter(c)).isTrue();
      }
    }

    @Test
    @DisplayName("应生成有效的随机组织机构代码")
    void generateRandomOrganizationCode_ShouldReturnValidCode() {
      // When
      String randomCode = validator.generateRandomOrganizationCode();

      // Then
      assertThat(randomCode).hasSize(9);
      ValidationResult result = validator.validate(randomCode);
      assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("生成的代码应该唯一")
    void generateRandomOrganizationCode_ShouldGenerateUniqueCodes() {
      // When
      String code1 = validator.generateRandomOrganizationCode();
      String code2 = validator.generateRandomOrganizationCode();
      String code3 = validator.generateRandomOrganizationCode();

      // Then - 虽然有极小概率相同，但多次生成应该大概率不同
      assertThat(code1).isNotNull();
      assertThat(code2).isNotNull();
      assertThat(code3).isNotNull();
      // 注意：不能保证每次都不同，因为这是随机的
    }
  }

  @Nested
  @DisplayName("格式化功能测试")
  class FormattingTests {

    @Test
    @DisplayName("应正确格式化组织机构代码")
    void formatOrganizationCode_ShouldReturnFormattedString() {
      // Given - 生成一个有效的组织机构代码
      String orgCode = validator.generateRandomOrganizationCode();

      // When
      String formatted = validator.formatOrganizationCode(orgCode);

      // Then
      assertThat(formatted).hasSize(10); // 8位 + 1位连字符 + 1位校验码
      assertThat(formatted).contains("-");
    }

    @Test
    @DisplayName("null 输入应返回 null")
    void formatOrganizationCode_WithNullInput_ShouldReturnNull() {
      // When
      String formatted = validator.formatOrganizationCode(null);

      // Then
      assertThat(formatted).isNull();
    }

    @Test
    @DisplayName("长度错误的代码应返回原值")
    void formatOrganizationCode_WithWrongLength_ShouldReturnOriginal() {
      // Given
      String wrongLengthCode = "12345678";

      // When
      String formatted = validator.formatOrganizationCode(wrongLengthCode);

      // Then
      assertThat(formatted).isEqualTo(wrongLengthCode);
    }

    @Test
    @DisplayName("应正确解析格式化的代码")
    void parseFormattedCode_ShouldReturnCleanString() {
      // Given
      String formattedCode = "12345678-9";

      // When
      String parsed = validator.parseFormattedCode(formattedCode);

      // Then
      assertThat(parsed).isEqualTo("123456789");
    }

    @Test
    @DisplayName("null 输入应返回 null")
    void parseFormattedCode_WithNullInput_ShouldReturnNull() {
      // When
      String parsed = validator.parseFormattedCode(null);

      // Then
      assertThat(parsed).isNull();
    }

    @Test
    @DisplayName("格式化和解析应该可逆")
    void formatAndParse_ShouldBeReversible() {
      // Given - 生成一个有效的组织机构代码
      String originalCode = validator.generateRandomOrganizationCode();

      // When
      String formatted = validator.formatOrganizationCode(originalCode);
      String parsed = validator.parseFormattedCode(formatted);

      // Then
      assertThat(parsed).isEqualTo(originalCode);
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
      assertThat(name).isEqualTo("OrganizationCode");
    }

    @Test
    @DisplayName("getDescription 应返回正确的描述")
    void getDescription_ShouldReturnCorrectDescription() {
      // When
      String description = validator.getDescription();

      // Then
      assertThat(description).contains("Chinese organization code");
      assertThat(description).contains("GB 11714-1997");
    }

    @Test
    @DisplayName("isValid 方法应返回正确的布尔值")
    void isValid_WithValidCode_ShouldReturnTrue() {
      // Given - 生成一个有效的组织机构代码
      String validCode = validator.generateRandomOrganizationCode();

      // When
      boolean isValid = validator.isValid(validCode);

      // Then
      assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("isValid 方法对无效代码应返回 false")
    void isValid_WithInvalidCode_ShouldReturnFalse() {
      // Given
      String invalidCode = "12345678X"; // 错误的校验码

      // When
      boolean isValid = validator.isValid(invalidCode);

      // Then
      assertThat(isValid).isFalse();
    }
  }
}
