package com.dataforge.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataMaskingEngine.MaskingStats;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DataMaskingEngine 测试类
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("DataMaskingEngine 测试")
class DataMaskingEngineTest {

  private DataMaskingEngine engine;

  @BeforeEach
  void setUp() {
    engine = new DataMaskingEngine();
  }

  @Nested
  @DisplayName("身份证号遮蔽测试")
  class IdCardMaskingTests {

    @Test
    @DisplayName("有效18位身份证号应正确遮蔽")
    void valid18DigitIdCardShouldBeMasked() {
      // Given
      String idCard = "110101199001011234";

      // When
      String result = engine.maskValue(idCard, "ID_CARD");

      // Then
      assertThat(result).isEqualTo("110101********1234");
    }

    @Test
    @DisplayName("带X校验位的身份证号应正确遮蔽")
    void idCardWithXShouldBeMasked() {
      // Given
      String idCard = "11010119900101121X";

      // When
      String result = engine.maskValue(idCard, "ID_CARD");

      // Then
      assertThat(result).isEqualTo("110101********121X");
    }

    @Test
    @DisplayName("非18位身份证号应返回原值")
    void non18DigitIdCardShouldReturnOriginal() {
      // Given
      String shortIdCard = "123456";

      // When
      String result = engine.maskValue(shortIdCard, "ID_CARD");

      // Then
      assertThat(result).isEqualTo(shortIdCard);
    }

    @Test
    @DisplayName("null 身份证号应返回 null")
    void nullIdCardShouldReturnNull() {
      // When
      String result = engine.maskValue(null, "ID_CARD");

      // Then
      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("手机号遮蔽测试")
  class PhoneMaskingTests {

    @Test
    @DisplayName("有效11位手机号应正确遮蔽")
    void valid11DigitPhoneShouldBeMasked() {
      // Given
      String phone = "13812345678";

      // When
      String result = engine.maskValue(phone, "PHONE");

      // Then
      assertThat(result).isEqualTo("138****5678");
    }

    @Test
    @DisplayName("非11位手机号应返回原值")
    void non11DigitPhoneShouldReturnOriginal() {
      // Given
      String shortPhone = "12345";

      // When
      String result = engine.maskValue(shortPhone, "PHONE");

      // Then
      assertThat(result).isEqualTo(shortPhone);
    }

    @Test
    @DisplayName("空字符串手机号应返回原值")
    void emptyPhoneShouldReturnOriginal() {
      // Given
      String emptyPhone = "";

      // When
      String result = engine.maskValue(emptyPhone, "PHONE");

      // Then
      assertThat(result).isEqualTo(emptyPhone);
    }
  }

  @Nested
  @DisplayName("邮箱遮蔽测试")
  class EmailMaskingTests {

    @Test
    @DisplayName("长用户名邮箱应正确遮蔽")
    void longUsernameEmailShouldBeMasked() {
      // Given
      String email = "zhangsan@example.com";

      // When
      String result = engine.maskValue(email, "EMAIL");

      // Then
      assertThat(result).isEqualTo("zh******n@example.com");
    }

    @Test
    @DisplayName("短用户名邮箱应正确遮蔽")
    void shortUsernameEmailShouldBeMasked() {
      // Given
      String email = "ab@example.com";

      // When
      String result = engine.maskValue(email, "EMAIL");

      // Then
      assertThat(result).isEqualTo("a*b@example.com");
    }

    @Test
    @DisplayName("无效邮箱格式应返回原值")
    void invalidEmailShouldReturnOriginal() {
      // Given
      String invalidEmail = "notanemail";

      // When
      String result = engine.maskValue(invalidEmail, "EMAIL");

      // Then
      assertThat(result).isEqualTo(invalidEmail);
    }

    @Test
    @DisplayName("null 邮箱应返回 null")
    void nullEmailShouldReturnNull() {
      // When
      String result = engine.maskValue(null, "EMAIL");

      // Then
      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("银行卡号遮蔽测试")
  class BankCardMaskingTests {

    @Test
    @DisplayName("16位银行卡号应正确遮蔽")
    void valid16DigitCardShouldBeMasked() {
      // Given
      String card = "6222021234567890123";

      // When
      String result = engine.maskValue(card, "BANK_CARD");

      // Then
      assertThat(result).startsWith("6222").endsWith("0123");
      assertThat(result).contains("***");
    }

    @Test
    @DisplayName("短于8位卡号应返回原值")
    void shortCardShouldReturnOriginal() {
      // Given
      String shortCard = "1234567";

      // When
      String result = engine.maskValue(shortCard, "BANK_CARD");

      // Then
      assertThat(result).isEqualTo(shortCard);
    }

    @Test
    @DisplayName("19位银行卡号应正确遮蔽")
    void valid19DigitCardShouldBeMasked() {
      // Given
      String card = "6222021234567890123";

      // When
      String result = engine.maskValue(card, "BANK_CARD");

      // Then
      assertThat(result).hasSize(card.length());
      assertThat(result.substring(0, 4)).isEqualTo("6222");
      assertThat(result.substring(result.length() - 4)).isEqualTo("0123");
    }
  }

  @Nested
  @DisplayName("中文姓名遮蔽测试")
  class ChineseNameMaskingTests {

    @Test
    @DisplayName("两字姓名应正确遮蔽")
    void twoCharacterNameShouldBeMasked() {
      // Given
      String name = "张三";

      // When
      String result = engine.maskValue(name, "CHINESE_NAME");

      // Then
      assertThat(result).isEqualTo("张*");
    }

    @Test
    @DisplayName("三字姓名应正确遮蔽")
    void threeCharacterNameShouldBeMasked() {
      // Given
      String name = "张三丰";

      // When
      String result = engine.maskValue(name, "CHINESE_NAME");

      // Then
      assertThat(result).isEqualTo("张**");
    }

    @Test
    @DisplayName("单字姓名应返回原值")
    void singleCharacterNameShouldReturnOriginal() {
      // Given
      String name = "张";

      // When
      String result = engine.maskValue(name, "CHINESE_NAME");

      // Then
      assertThat(result).isEqualTo(name);
    }

    @Test
    @DisplayName("null 姓名应返回 null")
    void nullNameShouldReturnNull() {
      // When
      String result = engine.maskValue(null, "CHINESE_NAME");

      // Then
      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("自动遮蔽测试")
  class AutoMaskingTests {

    @Test
    @DisplayName("自动检测身份证号并遮蔽")
    void autoDetectIdCardShouldMask() {
      // Given
      String idCard = "110101199001011234";

      // When
      String result = engine.autoMaskValue(idCard);

      // Then
      assertThat(result).isEqualTo("110101********1234");
    }

    @Test
    @DisplayName("自动检测手机号并遮蔽")
    void autoDetectPhoneShouldMask() {
      // Given
      String phone = "13812345678";

      // When
      String result = engine.autoMaskValue(phone);

      // Then
      assertThat(result).isEqualTo("138****5678");
    }

    @Test
    @DisplayName("自动检测邮箱并遮蔽")
    void autoDetectEmailShouldMask() {
      // Given
      String email = "test@example.com";

      // When
      String result = engine.autoMaskValue(email);

      // Then
      assertThat(result).contains("@example.com");
      assertThat(result).contains("*");
    }

    @Test
    @DisplayName("非敏感数据应返回原值")
    void nonSensitiveDataShouldReturnOriginal() {
      // Given
      String normalText = "这是一个普通文本";

      // When
      String result = engine.autoMaskValue(normalText);

      // Then
      assertThat(result).isEqualTo(normalText);
    }

    @Test
    @DisplayName("null 值自动遮蔽应返回 null")
    void nullValueAutoMaskShouldReturnNull() {
      // When
      String result = engine.autoMaskValue(null);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("空字符串自动遮蔽应返回原值")
    void emptyStringAutoMaskShouldReturnOriginal() {
      // Given
      String empty = "";

      // When
      String result = engine.autoMaskValue(empty);

      // Then
      assertThat(result).isEqualTo(empty);
    }
  }

  @Nested
  @DisplayName("记录遮蔽测试")
  class RecordMaskingTests {

    @Test
    @DisplayName("单条记录遮蔽应成功")
    void singleRecordMaskingShouldSucceed() {
      // Given
      Map<String, Object> record = new HashMap<>();
      record.put("name", "张三");
      record.put("phone", "13812345678");
      record.put("age", 25); // 非字符串字段

      Map<String, String> fieldTypeMapping = Map.of("name", "CHINESE_NAME", "phone", "PHONE");

      // When
      Map<String, Object> result = engine.maskRecord(record, fieldTypeMapping);

      // Then
      assertThat(result.get("name")).isEqualTo("张*");
      assertThat(result.get("phone")).isEqualTo("138****5678");
      assertThat(result.get("age")).isEqualTo(25); // 非字符串字段保持不变
    }

    @Test
    @DisplayName("无类型映射时应自动检测")
    void noTypeMappingShouldAutoDetect() {
      // Given
      Map<String, Object> record = new HashMap<>();
      record.put("id_card", "110101199001011234");
      record.put("email", "test@example.com");

      // When
      Map<String, Object> result = engine.maskRecord(record, new HashMap<>());

      // Then
      assertThat(result.get("id_card")).isEqualTo("110101********1234");
      assertThat(result.get("email").toString()).contains("*");
    }

    @Test
    @DisplayName("空记录应返回空记录")
    void emptyRecordShouldReturnEmptyRecord() {
      // Given
      Map<String, Object> record = new HashMap<>();

      // When
      Map<String, Object> result = engine.maskRecord(record, new HashMap<>());

      // Then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("自定义规则测试")
  class CustomRuleTests {

    @Test
    @DisplayName("添加自定义遮蔽规则应成功")
    void addCustomRuleShouldSucceed() {
      // Given
      Pattern pattern = Pattern.compile("\\d{4}");

      // When
      engine.addCustomRule("CUSTOM", "自定义规则", value -> "****", pattern);

      // Then
      Map<String, String> allRules = engine.getAllRules();
      assertThat(allRules).containsKey("CUSTOM");
    }

    @Test
    @DisplayName("移除遮蔽规则应成功")
    void removeRuleShouldSucceed() {
      // Given - 先添加自定义规则
      engine.addCustomRule("TEMP", "临时规则", value -> "***", Pattern.compile(".*"));

      // When
      engine.removeRule("TEMP");

      // Then
      Map<String, String> allRules = engine.getAllRules();
      assertThat(allRules).doesNotContainKey("TEMP");
    }

    @Test
    @DisplayName("移除不存在的规则不应抛出异常")
    void removeNonExistentRuleShouldNotThrow() {
      // Then
      org.assertj.core.api.Assertions.assertThatCode(() -> engine.removeRule("NON_EXISTENT"))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("规则管理测试")
  class RuleManagementTests {

    @Test
    @DisplayName("获取所有规则应返回默认规则")
    void getAllRulesShouldReturnDefaultRules() {
      // When
      Map<String, String> rules = engine.getAllRules();

      // Then
      assertThat(rules)
          .containsKey("ID_CARD")
          .containsKey("PHONE")
          .containsKey("EMAIL")
          .containsKey("BANK_CARD")
          .containsKey("CHINESE_NAME");
    }

    @Test
    @DisplayName("获取遮蔽统计应返回正确数量")
    void getMaskingStatsShouldReturnCorrectCount() {
      // When
      MaskingStats stats = engine.getMaskingStats();

      // Then
      assertThat(stats.getRuleCount()).isEqualTo(5); // 5个默认规则
      assertThat(stats.getPatternCount()).isEqualTo(5); // 5个检测模式
    }

    @Test
    @DisplayName("MaskingStats toString 应返回有效信息")
    void maskingStatsToStringShouldReturnValidInfo() {
      // Given
      MaskingStats stats = engine.getMaskingStats();

      // When
      String result = stats.toString();

      // Then
      assertThat(result).contains("MaskingStats").contains("rules=").contains("patterns=");
    }
  }

  @Nested
  @DisplayName("未知数据类型测试")
  class UnknownDataTypeTests {

    @Test
    @DisplayName("未知数据类型应返回原值")
    void unknownDataTypeShouldReturnOriginal() {
      // Given
      String value = "test value";

      // When
      String result = engine.maskValue(value, "UNKNOWN_TYPE");

      // Then
      assertThat(result).isEqualTo(value);
    }
  }
}
