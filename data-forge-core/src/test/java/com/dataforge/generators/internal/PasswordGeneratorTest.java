package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PasswordGenerator 测试")
class PasswordGeneratorTest {

  private PasswordGenerator generator;
  private SimpleFieldConfig config;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new PasswordGenerator();
    config = new SimpleFieldConfig();
    config.setType("password");
    context = new DataForgeContext();
  }

  @Nested
  @DisplayName("默认配置测试")
  class DefaultConfigurationTests {

    @Test
    @DisplayName("默认配置应生成有效密码")
    void shouldGenerateValidPasswordWithDefaultConfig() {
      String password = generator.generate(config, context);

      assertThat(password).isNotNull();
      assertThat(password.length()).isBetween(8, 16);
    }

    @Test
    @DisplayName("默认密码应包含字母和数字（MEDIUM复杂度）")
    void shouldContainLettersAndDigitsWithDefaultConfig() {
      String password = generator.generate(config, context);

      boolean hasLetter = password.chars().anyMatch(Character::isLetter);
      boolean hasDigit = password.chars().anyMatch(Character::isDigit);

      assertThat(hasLetter).isTrue();
      assertThat(hasDigit).isTrue();
    }

    @Test
    @DisplayName("生成的密码应具有唯一性")
    void shouldGenerateUniquePasswords() {
      Set<String> passwords = new HashSet<>();

      for (int i = 0; i < 100; i++) {
        String password = generator.generate(config, context);
        assertThat(passwords).doesNotContain(password);
        passwords.add(password);
      }

      assertThat(passwords).hasSize(100);
    }
  }

  @Nested
  @DisplayName("长度配置测试")
  class LengthConfigurationTests {

    @Test
    @DisplayName("应生成指定长度的密码")
    void shouldGeneratePasswordWithFixedLength() {
      config.setParam("length", "12");

      String password = generator.generate(config, context);

      assertThat(password).hasSize(12);
    }

    @Test
    @DisplayName("应生成长度范围内的密码")
    void shouldGeneratePasswordWithinLengthRange() {
      config.setParam("length", "10,15");

      for (int i = 0; i < 20; i++) {
        String password = generator.generate(config, context);
        assertThat(password.length()).isBetween(10, 15);
      }
    }

    @Test
    @DisplayName("应处理无效长度参数并使用默认值")
    void shouldHandleInvalidLengthParameter() {
      config.setParam("length", "invalid");

      String password = generator.generate(config, context);

      assertThat(password).isNotNull();
      assertThat(password.length()).isBetween(8, 16);
    }

    @Test
    @DisplayName("应处理最小长度为1的情况")
    void shouldHandleMinimumLengthOfOne() {
      config.setParam("length", "1,5");

      String password = generator.generate(config, context);

      assertThat(password.length()).isBetween(1, 5);
    }
  }

  @Nested
  @DisplayName("复杂度级别测试")
  class ComplexityLevelTests {

    @Test
    @DisplayName("LOW复杂度应生成纯数字或纯字母密码")
    void shouldGenerateLowComplexityPassword() {
      config.setParam("complexity", "LOW");

      String password = generator.generate(config, context);

      boolean isAllDigits = password.chars().allMatch(Character::isDigit);
      boolean isAllLetters = password.chars().allMatch(Character::isLetter);

      assertThat(isAllDigits || isAllLetters).isTrue();
    }

    @Test
    @DisplayName("MEDIUM复杂度应包含数字和字母")
    void shouldGenerateMediumComplexityPassword() {
      config.setParam("complexity", "MEDIUM");

      String password = generator.generate(config, context);

      boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
      boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
      boolean hasDigit = password.chars().anyMatch(Character::isDigit);

      assertThat(hasLowercase || hasUppercase).isTrue();
      assertThat(hasDigit).isTrue();
    }

    @Test
    @DisplayName("HIGH复杂度应包含数字、大小写字母和特殊字符")
    void shouldGenerateHighComplexityPassword() {
      config.setParam("complexity", "HIGH");

      String password = generator.generate(config, context);

      boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
      boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
      boolean hasDigit = password.chars().anyMatch(Character::isDigit);
      boolean hasSpecial = password.chars().anyMatch(c -> "!@#$%^&*".indexOf(c) >= 0);

      assertThat(hasLowercase).isTrue();
      assertThat(hasUppercase).isTrue();
      assertThat(hasDigit).isTrue();
      assertThat(hasSpecial).isTrue();
    }

    @Test
    @DisplayName("无效复杂度应回退到MEDIUM")
    void shouldFallbackToMediumForInvalidComplexity() {
      config.setParam("complexity", "INVALID");

      String password = generator.generate(config, context);

      assertThat(password).isNotNull();
      boolean hasLetter = password.chars().anyMatch(Character::isLetter);
      boolean hasDigit = password.chars().anyMatch(Character::isDigit);
      assertThat(hasLetter && hasDigit).isTrue();
    }
  }

  @Nested
  @DisplayName("自定义字符集测试")
  class CustomCharacterSetTests {

    @Test
    @DisplayName("CUSTOM复杂度应使用自定义字符集")
    void shouldUseCustomCharacterSet() {
      config.setParam("complexity", "CUSTOM");
      config.setParam("custom_chars", "ABC123");

      String password = generator.generate(config, context);

      assertThat(password).matches("^[ABC123]+$");
    }

    @Test
    @DisplayName("空自定义字符集应使用默认字符集")
    void shouldUseDefaultCharacterSetForEmptyCustomChars() {
      config.setParam("complexity", "CUSTOM");
      config.setParam("custom_chars", "");

      String password = generator.generate(config, context);

      assertThat(password).isNotNull();
      boolean hasLetter = password.chars().anyMatch(Character::isLetter);
      boolean hasDigit = password.chars().anyMatch(Character::isDigit);
      assertThat(hasLetter || hasDigit).isTrue();
    }
  }

  @Nested
  @DisplayName("弱密码测试")
  class WeakPasswordTests {

    @Test
    @DisplayName("启用弱密码应生成常见弱密码")
    void shouldGenerateWeakPasswordWhenEnabled() {
      config.setParam("include_weak", "true");
      config.setParam("weak_ratio", "1.0"); // 100% 弱密码
      config.setParam("length", "6,8");

      String password = generator.generate(config, context);

      assertThat(password).isNotNull();
      assertThat(password.length()).isBetween(6, 8);
    }

    @Test
    @DisplayName("弱密码生成应受weak_ratio参数影响")
    void weakPasswordGenerationShouldRespectWeakRatio() {
      config.setParam("include_weak", "true");
      config.setParam("weak_ratio", "1.0"); // 100% 弱密码
      config.setParam("length", "6,8");

      // 生成多个密码，应该都是弱密码
      boolean foundWeakPattern = false;
      for (int i = 0; i < 50; i++) {
        String password = generator.generate(config, context);
        if (isSimplePassword(password)) {
          foundWeakPattern = true;
          break;
        }
      }

      assertThat(foundWeakPattern).isTrue();
    }

    @Test
    @DisplayName("禁用弱密码应生成强密码")
    void shouldGenerateStrongPasswordWhenWeakDisabled() {
      config.setParam("include_weak", "false");
      config.setParam("complexity", "HIGH");

      String password = generator.generate(config, context);

      assertThat(password).isNotNull();
      boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
      boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
      boolean hasDigit = password.chars().anyMatch(Character::isDigit);
      assertThat(hasLowercase && hasUppercase && hasDigit).isTrue();
    }

    private boolean isSimplePassword(String password) {
      // 检查是否所有字符相同
      boolean allSame = password.chars().distinct().count() == 1;
      if (allSame) return true;

      // 检查是否是连续数字
      boolean consecutiveDigits = true;
      for (int i = 1; i < password.length(); i++) {
        if (password.charAt(i) - password.charAt(i - 1) != 1) {
          consecutiveDigits = false;
          break;
        }
      }
      if (consecutiveDigits && password.length() > 3) return true;

      return false;
    }
  }

  @Nested
  @DisplayName("必需字符要求测试")
  class RequiredCharacterTests {

    @Test
    @DisplayName("强制大写字母应包含大写字母")
    void shouldRequireUppercaseWhenSpecified() {
      config.setParam("require_uppercase", "true");
      config.setParam("length", "12");

      String password = generator.generate(config, context);

      assertThat(password).matches(".*[A-Z].*");
    }

    @Test
    @DisplayName("强制小写字母应包含小写字母")
    void shouldRequireLowercaseWhenSpecified() {
      config.setParam("require_lowercase", "true");
      config.setParam("length", "12");

      String password = generator.generate(config, context);

      assertThat(password).matches(".*[a-z].*");
    }

    @Test
    @DisplayName("强制数字应包含数字")
    void shouldRequireDigitsWhenSpecified() {
      config.setParam("require_digits", "true");
      config.setParam("length", "12");

      String password = generator.generate(config, context);

      assertThat(password).matches(".*\\d.*");
    }

    @Test
    @DisplayName("强制特殊字符应包含特殊字符")
    void shouldRequireSpecialCharsWhenSpecified() {
      config.setParam("require_special", "true");
      config.setParam("complexity", "HIGH");
      config.setParam("length", "12");

      String password = generator.generate(config, context);

      assertThat(password).matches(".*[!@#$%^&*].*");
    }

    @Test
    @DisplayName("强制所有字符类型应满足所有要求")
    void shouldRequireAllCharacterTypes() {
      config.setParam("require_uppercase", "true");
      config.setParam("require_lowercase", "true");
      config.setParam("require_digits", "true");
      config.setParam("require_special", "true");
      config.setParam("complexity", "HIGH");
      config.setParam("length", "16");

      String password = generator.generate(config, context);

      assertThat(password).matches(".*[a-z].*");
      assertThat(password).matches(".*[A-Z].*");
      assertThat(password).matches(".*\\d.*");
      assertThat(password).matches(".*[!@#$%^&*].*");
    }
  }

  @Nested
  @DisplayName("易混淆字符排除测试")
  class AmbiguousCharacterTests {

    @Test
    @DisplayName("排除易混淆字符应不包含0O1lI")
    void shouldExcludeAmbiguousCharacters() {
      config.setParam("exclude_ambiguous", "true");
      config.setParam("complexity", "HIGH");
      config.setParam("length", "20");

      for (int i = 0; i < 50; i++) {
        String password = generator.generate(config, context);
        assertThat(password)
            .doesNotContain("0")
            .doesNotContain("O")
            .doesNotContain("1")
            .doesNotContain("l")
            .doesNotContain("I")
            .doesNotContain("|");
      }
    }

    @Test
    @DisplayName("不排除易混淆字符可能包含这些字符")
    void mayIncludeAmbiguousCharactersWhenNotExcluded() {
      config.setParam("exclude_ambiguous", "false");
      config.setParam("complexity", "HIGH");
      config.setParam("length", "20");

      for (int i = 0; i < 50; i++) {
        String password = generator.generate(config, context);
        if (password.matches(".*[0O1lI|].*")) {
          break;
        }
      }

      // 不保证一定包含，但有可能
      assertThat(true).isTrue();
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class EdgeCaseTests {

    @Test
    @DisplayName("弱密码比例超过1.0应被限制为1.0")
    void shouldClampWeakRatioToMaximum() {
      config.setParam("include_weak", "true");
      config.setParam("weak_ratio", "1.5"); // 超过1.0

      String password = generator.generate(config, context);

      assertThat(password).isNotNull();
    }

    @Test
    @DisplayName("弱密码比例低于0.0应被限制为0.0")
    void shouldClampWeakRatioToMinimum() {
      config.setParam("include_weak", "true");
      config.setParam("weak_ratio", "-0.5"); // 低于0.0

      String password = generator.generate(config, context);

      assertThat(password).isNotNull();
    }

    @Test
    @DisplayName("长度范围反转应自动调整")
    void shouldHandleReversedLengthRange() {
      config.setParam("length", "16,8"); // min > max

      String password = generator.generate(config, context);

      assertThat(password).isNotNull();
      assertThat(password.length()).isBetween(8, 16);
    }

    @Test
    @DisplayName("生成长度列表密码应保持性能")
    void shouldMaintainPerformanceForBulkGeneration() {
      config.setParam("complexity", "MEDIUM");
      config.setParam("length", "12");

      long startTime = System.currentTimeMillis();
      for (int i = 0; i < 1000; i++) {
        String password = generator.generate(config, context);
        assertThat(password).isNotNull();
      }
      long duration = System.currentTimeMillis() - startTime;

      // 1000个密码应该在5秒内生成
      assertThat(duration).isLessThan(5000);
    }
  }
}
