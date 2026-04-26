package com.dataforge.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataRelationEngine.DataRelation;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DataRelationEngine 测试类
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("DataRelationEngine 测试")
class DataRelationEngineTest {

  private DataRelationEngine engine;

  @BeforeEach
  void setUp() {
    engine = new DataRelationEngine();
  }

  @Nested
  @DisplayName("基本功能测试")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("空关联规则列表应返回原始记录")
    void emptyRelationsShouldReturnOriginalRecord() {
      // Given
      Map<String, Object> record = Map.of("name", "张三", "age", 25);
      List<DataRelation> relations = new ArrayList<>();

      // When
      Map<String, Object> result = engine.applyRelations(record, relations);

      // Then
      assertThat(result).containsEntry("name", "张三").containsEntry("age", 25);
    }

    @Test
    @DisplayName("null 关联规则列表应返回原始记录")
    void nullRelationsShouldReturnOriginalRecord() {
      // Given
      Map<String, Object> record = Map.of("name", "张三", "age", 25);

      // When
      Map<String, Object> result = engine.applyRelations(record, null);

      // Then
      assertThat(result).containsEntry("name", "张三").containsEntry("age", 25);
    }

    @Test
    @DisplayName("未知关联类型应记录警告并返回原始记录")
    void unknownRelationTypeShouldReturnOriginalRecord() {
      // Given
      Map<String, Object> record = new HashMap<>();
      record.put("name", "张三");
      DataRelation relation = new DataRelation("UNKNOWN_TYPE", "name", "output", null);
      List<DataRelation> relations = List.of(relation);

      // When
      Map<String, Object> result = engine.applyRelations(record, relations);

      // Then
      assertThat(result).containsEntry("name", "张三");
    }
  }

  @Nested
  @DisplayName("身份证关联测试")
  class IdCardRelationTests {

    @Test
    @DisplayName("有效身份证号应解析出年龄、性别和地区信息")
    void validIdCardShouldParseAgeGenderAndRegion() {
      // Given
      Map<String, Object> record = new HashMap<>();
      record.put("id_card", "110101199001011234"); // 北京，1990-01-01，男性
      DataRelation relation = new DataRelation("ID_CARD_RELATION", "id_card", null, null);
      List<DataRelation> relations = List.of(relation);

      // When
      Map<String, Object> result = engine.applyRelations(record, relations);

      // Then
      assertThat(result).containsEntry("province", "北京市").containsEntry("city", "北京市");

      // 验证出生日期
      assertThat(result).containsEntry("birth_date", "1990-01-01");

      // 验证性别（奇数为男性）
      assertThat(result).containsEntry("gender", "MALE");

      // 验证年龄
      int expectedAge = (int) ChronoUnit.YEARS.between(LocalDate.of(1990, 1, 1), LocalDate.now());
      assertThat(result).containsEntry("age", expectedAge);
    }

    @Test
    @DisplayName("女性身份证号应解析出女性性别")
    void femaleIdCardShouldParseFemaleGender() {
      // Given - 使用偶数序列码表示女性
      Map<String, Object> record = new HashMap<>();
      record.put("id_card", "310101199502022420"); // 上海，1995-02-02，女性
      DataRelation relation = new DataRelation("ID_CARD_RELATION", "id_card", null, null);

      // When
      Map<String, Object> result = engine.applyRelations(record, List.of(relation));

      // Then
      assertThat(result).containsEntry("gender", "FEMALE");
    }

    @Test
    @DisplayName("不同地区代码应解析出正确地区")
    void differentRegionCodesShouldParseCorrectRegion() {
      // Given - 广州
      Map<String, Object> record = new HashMap<>();
      record.put("id_card", "440100198505051234");
      DataRelation relation = new DataRelation("ID_CARD_RELATION", "id_card", null, null);

      // When
      Map<String, Object> result = engine.applyRelations(record, List.of(relation));

      // Then
      assertThat(result).containsEntry("province", "广东省").containsEntry("city", "广州市");
    }

    @Test
    @DisplayName("无效身份证号格式应返回原始记录")
    void invalidIdCardFormatShouldReturnOriginalRecord() {
      // Given
      Map<String, Object> record = new HashMap<>();
      record.put("id_card", "invalid_id_card");
      DataRelation relation = new DataRelation("ID_CARD_RELATION", "id_card", null, null);

      // When
      Map<String, Object> result = engine.applyRelations(record, List.of(relation));

      // Then
      assertThat(result).containsEntry("id_card", "invalid_id_card");
      assertThat(result).doesNotContainKey("age");
      assertThat(result).doesNotContainKey("gender");
    }

    @Test
    @DisplayName("null 身份证号应返回原始记录")
    void nullIdCardShouldReturnOriginalRecord() {
      // Given
      Map<String, Object> record = new HashMap<>();
      record.put("id_card", null);
      DataRelation relation = new DataRelation("ID_CARD_RELATION", "id_card", null, null);

      // When
      Map<String, Object> result = engine.applyRelations(record, List.of(relation));

      // Then
      assertThat(result).containsEntry("id_card", null);
    }

    @Test
    @DisplayName("未知地区代码应只解析年龄和性别")
    void unknownRegionCodeShouldParseAgeAndGenderOnly() {
      // Given - 使用未知的地区代码
      Map<String, Object> record = new HashMap<>();
      record.put("id_card", "999999199001011234"); // 未知地区
      DataRelation relation = new DataRelation("ID_CARD_RELATION", "id_card", null, null);

      // When
      Map<String, Object> result = engine.applyRelations(record, List.of(relation));

      // Then
      assertThat(result).containsKey("age").containsKey("gender");
      assertThat(result).doesNotContainKey("province");
    }

    @Test
    @DisplayName("带X校验位的身份证号应正确解析")
    void idCardWithXCheckDigitShouldParseCorrectly() {
      // Given
      Map<String, Object> record = new HashMap<>();
      record.put("id_card", "11010119900101121X"); // 以X结尾
      DataRelation relation = new DataRelation("ID_CARD_RELATION", "id_card", null, null);

      // When
      Map<String, Object> result = engine.applyRelations(record, List.of(relation));

      // Then
      assertThat(result).containsEntry("gender", "MALE");
      assertThat(result).containsKey("age");
    }
  }

  @Nested
  @DisplayName("姓名邮箱关联测试")
  class NameEmailRelationTests {

    @Test
    @DisplayName("英文姓名应生成邮箱用户名")
    void englishNameShouldGenerateEmailUsername() {
      // Given
      Map<String, Object> record = new HashMap<>();
      record.put("name", "JohnDoe");
      DataRelation relation = new DataRelation("NAME_EMAIL_RELATION", "name", null, null);

      // When
      Map<String, Object> result = engine.applyRelations(record, List.of(relation));

      // Then
      assertThat(result).containsKey("email_username");
      assertThat(result.get("email_username")).isEqualTo("johndoe");
    }

    @Test
    @DisplayName("中文姓名应转换为拼音邮箱用户名")
    void chineseNameShouldGeneratePinyinEmailUsername() {
      // Given
      Map<String, Object> record = new HashMap<>();
      record.put("name", "张三");
      DataRelation relation = new DataRelation("NAME_EMAIL_RELATION", "name", null, null);

      // When
      Map<String, Object> result = engine.applyRelations(record, List.of(relation));

      // Then
      assertThat(result).containsKey("email_username");
      assertThat(result.get("email_username").toString().toLowerCase()).contains("zhang");
    }

    @Test
    @DisplayName("null 姓名应返回原始记录")
    void nullNameShouldReturnOriginalRecord() {
      // Given
      Map<String, Object> record = new HashMap<>();
      record.put("name", null);
      DataRelation relation = new DataRelation("NAME_EMAIL_RELATION", "name", null, null);

      // When
      Map<String, Object> result = engine.applyRelations(record, List.of(relation));

      // Then
      assertThat(result).doesNotContainKey("email_username");
    }

    @Test
    @DisplayName("非字符串姓名应返回原始记录")
    void nonStringNameShouldReturnOriginalRecord() {
      // Given
      Map<String, Object> record = new HashMap<>();
      record.put("name", 12345);
      DataRelation relation = new DataRelation("NAME_EMAIL_RELATION", "name", null, null);

      // When
      Map<String, Object> result = engine.applyRelations(record, List.of(relation));

      // Then
      assertThat(result).doesNotContainKey("email_username");
    }
  }

  @Nested
  @DisplayName("银行卡关联测试")
  class BankCardRelationTests {

    @Test
    @DisplayName("工商银行卡应匹配工商银行")
    void icbcCardShouldMatchICBC() {
      // Given - 以4开头的BIN码
      Map<String, Object> record = new HashMap<>();
      record.put("bank_card", "4123456789012345");
      DataRelation relation = new DataRelation("BANK_CARD_RELATION", "bank_card", null, null);

      // When
      Map<String, Object> result = engine.applyRelations(record, List.of(relation));

      // Then
      assertThat(result).containsEntry("bank_name", "工商银行");
    }

    @Test
    @DisplayName("建设银行卡应匹配建设银行")
    void ccbCardShouldMatchCCB() {
      // Given - 以5开头的BIN码
      Map<String, Object> record = new HashMap<>();
      record.put("bank_card", "5123456789012345");
      DataRelation relation = new DataRelation("BANK_CARD_RELATION", "bank_card", null, null);

      // When
      Map<String, Object> result = engine.applyRelations(record, List.of(relation));

      // Then
      assertThat(result).containsEntry("bank_name", "建设银行");
    }

    @Test
    @DisplayName("农业银行卡应匹配农业银行")
    void abcCardShouldMatchABC() {
      // Given - 以6开头的BIN码
      Map<String, Object> record = new HashMap<>();
      record.put("bank_card", "6123456789012345");
      DataRelation relation = new DataRelation("BANK_CARD_RELATION", "bank_card", null, null);

      // When
      Map<String, Object> result = engine.applyRelations(record, List.of(relation));

      // Then
      assertThat(result).containsEntry("bank_name", "农业银行");
    }

    @Test
    @DisplayName("银联卡应匹配中国银联")
    void unionPayCardShouldMatchUnionPay() {
      // Given - 以62开头的BIN码
      Map<String, Object> record = new HashMap<>();
      record.put("bank_card", "6212345678901234");
      DataRelation relation = new DataRelation("BANK_CARD_RELATION", "bank_card", null, null);

      // When
      Map<String, Object> result = engine.applyRelations(record, List.of(relation));

      // Then
      assertThat(result).containsEntry("bank_name", "中国银联");
    }

    @Test
    @DisplayName("未知BIN码应返回未知银行")
    void unknownBinShouldReturnUnknownBank() {
      // Given - 以9开头的未知BIN码
      Map<String, Object> record = new HashMap<>();
      record.put("bank_card", "9123456789012345");
      DataRelation relation = new DataRelation("BANK_CARD_RELATION", "bank_card", null, null);

      // When
      Map<String, Object> result = engine.applyRelations(record, List.of(relation));

      // Then
      assertThat(result).containsEntry("bank_name", "未知银行");
    }

    @Test
    @DisplayName("null 银行卡号应返回原始记录")
    void nullCardNumberShouldReturnOriginalRecord() {
      // Given
      Map<String, Object> record = new HashMap<>();
      record.put("bank_card", null);
      DataRelation relation = new DataRelation("BANK_CARD_RELATION", "bank_card", null, null);

      // When
      Map<String, Object> result = engine.applyRelations(record, List.of(relation));

      // Then
      assertThat(result).doesNotContainKey("bank_name");
    }
  }

  @Nested
  @DisplayName("多关联规则测试")
  class MultipleRelationsTests {

    @Test
    @DisplayName("多个关联规则应依次应用")
    void multipleRelationsShouldBeAppliedSequentially() {
      // Given
      Map<String, Object> record = new HashMap<>();
      record.put("id_card", "110101199001011234");
      record.put("name", "zhangsan");

      DataRelation idCardRelation = new DataRelation("ID_CARD_RELATION", "id_card", null, null);
      DataRelation nameEmailRelation = new DataRelation("NAME_EMAIL_RELATION", "name", null, null);
      List<DataRelation> relations = List.of(idCardRelation, nameEmailRelation);

      // When
      Map<String, Object> result = engine.applyRelations(record, relations);

      // Then
      assertThat(result).containsKey("age").containsKey("gender").containsKey("email_username");
    }

    @Test
    @DisplayName("后续规则不应覆盖先前规则的数据")
    void subsequentRulesShouldNotOverwritePreviousData() {
      // Given
      Map<String, Object> record = new HashMap<>();
      record.put("id_card", "110101199001011234");
      record.put("name", "test");

      DataRelation idCardRelation = new DataRelation("ID_CARD_RELATION", "id_card", null, null);
      DataRelation nameEmailRelation = new DataRelation("NAME_EMAIL_RELATION", "name", null, null);

      // When
      Map<String, Object> result =
          engine.applyRelations(record, List.of(idCardRelation, nameEmailRelation));

      // Then
      assertThat(result).containsEntry("gender", "MALE");
      assertThat(result).containsEntry("email_username", "test");
    }
  }

  @Nested
  @DisplayName("数据关联定义类测试")
  class DataRelationClassTests {

    @Test
    @DisplayName("DataRelation 应正确存储所有参数")
    void dataRelationShouldStoreAllParameters() {
      // Given
      Map<String, Object> params = Map.of("key1", "value1", "key2", 123);

      // When
      DataRelation relation = new DataRelation("CUSTOM", "source", "target", params);

      // Then
      assertThat(relation.getType()).isEqualTo("CUSTOM");
      assertThat(relation.getSourceField()).isEqualTo("source");
      assertThat(relation.getTargetField()).isEqualTo("target");
      assertThat(relation.getParameters())
          .containsEntry("key1", "value1")
          .containsEntry("key2", 123);
    }

    @Test
    @DisplayName("DataRelation 应处理 null 参数")
    void dataRelationShouldHandleNullParameters() {
      // When
      DataRelation relation = new DataRelation("TYPE", "source", "target", null);

      // Then
      assertThat(relation.getParameters()).isEmpty();
    }
  }

  @Nested
  @DisplayName("异常处理测试")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("处理器异常不应中断其他规则应用")
    void processorExceptionShouldNotInterruptOtherRules() {
      // Given
      Map<String, Object> record = new HashMap<>();
      record.put("id_card", "invalid"); // 会导致解析异常
      record.put("name", "test");

      DataRelation idCardRelation = new DataRelation("ID_CARD_RELATION", "id_card", null, null);
      DataRelation nameEmailRelation = new DataRelation("NAME_EMAIL_RELATION", "name", null, null);

      // When
      Map<String, Object> result =
          engine.applyRelations(record, List.of(idCardRelation, nameEmailRelation));

      // Then - 第二个规则应该仍然执行
      assertThat(result).containsEntry("email_username", "test");
    }
  }
}
