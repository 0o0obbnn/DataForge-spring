package com.dataforge.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PersonGen 测试")
class PersonGenTest {

  private PersonGen personGen;
  private DataGen dataGen;

  @BeforeEach
  void setUp() {
    dataGen = new DataGen();
    personGen = new PersonGen(dataGen);
  }

  @Nested
  @DisplayName("姓名生成测试")
  class NameGenerationTests {

    @Test
    @DisplayName("应生成完整姓名")
    void shouldGenerateFullName() {
      String name = personGen.fullName();

      assertThat(name).isNotNull();
      assertThat(name).isNotEmpty();
      assertThat(name.length()).isBetween(2, 50);
    }

    @Test
    @DisplayName("应生成中文姓名")
    void shouldGenerateChineseName() {
      String name = personGen.chineseName();

      assertThat(name).isNotNull();
      assertThat(name).isNotEmpty();
      assertThat(name).matches("^[\\u4e00-\\u9fa5]+$");
    }

    @Test
    @DisplayName("应生成男性中文姓名")
    void shouldGenerateMaleChineseName() {
      String name = personGen.chineseName("MALE");

      assertThat(name).isNotNull();
      assertThat(name).isNotEmpty();
      assertThat(name).matches("^[\\u4e00-\\u9fa5]+$");
    }

    @Test
    @DisplayName("应生成女性中文姓名")
    void shouldGenerateFemaleChineseName() {
      String name = personGen.chineseName("FEMALE");

      assertThat(name).isNotNull();
      assertThat(name).isNotEmpty();
      assertThat(name).matches("^[\\u4e00-\\u9fa5]+$");
    }

    @Test
    @DisplayName("应生成英文姓名")
    void shouldGenerateEnglishName() {
      String name = personGen.englishName();

      assertThat(name).isNotNull();
      assertThat(name).isNotEmpty();
      assertThat(name).matches("^[a-zA-Z\\s]+$");
    }

    @Test
    @DisplayName("应生成男性英文姓名")
    void shouldGenerateMaleEnglishName() {
      String name = personGen.englishName("MALE");

      assertThat(name).isNotNull();
      assertThat(name).isNotEmpty();
      assertThat(name).matches("^[a-zA-Z\\s]+$");
    }

    @Test
    @DisplayName("应生成女性英文姓名")
    void shouldGenerateFemaleEnglishName() {
      String name = personGen.englishName("FEMALE");

      assertThat(name).isNotNull();
      assertThat(name).isNotEmpty();
      assertThat(name).matches("^[a-zA-Z\\s]+$");
    }

    @Test
    @DisplayName("应生成姓氏")
    void shouldGenerateLastName() {
      String lastName = personGen.lastName();

      assertThat(lastName).isNotNull();
      assertThat(lastName).isNotEmpty();
    }

    @Test
    @DisplayName("应生成名字")
    void shouldGenerateFirstName() {
      String firstName = personGen.firstName();

      assertThat(firstName).isNotNull();
      assertThat(firstName).isNotEmpty();
    }

    @Test
    @DisplayName("批量生成姓名应返回正确数量")
    void shouldGenerateCorrectNumberOfNames() {
      List<String> names = personGen.fullNames(10);

      assertThat(names).hasSize(10);
      names.forEach(
          name -> {
            assertThat(name).isNotNull();
            assertThat(name).isNotEmpty();
          });
    }

    @Test
    @DisplayName("批量生成姓名应包含不同值")
    void shouldGenerateDifferentNamesInBatch() {
      List<String> names = personGen.fullNames(100);

      assertThat(names).hasSize(100);
      long uniqueCount = names.stream().distinct().count();
      assertThat(uniqueCount).isGreaterThan(50);
    }
  }

  @Nested
  @DisplayName("身份证号生成测试")
  class IdCardGenerationTests {

    @Test
    @DisplayName("应生成身份证号")
    void shouldGenerateIdCard() {
      String idCard = personGen.idCard();

      assertThat(idCard).isNotNull();
      assertThat(idCard).hasSize(18);
      assertThat(idCard).matches("^\\d{17}[0-9Xx]$");
    }

    @Test
    @DisplayName("应生成指定地区的身份证号")
    void shouldGenerateIdCardWithRegion() {
      String idCard = personGen.idCard("110101");

      assertThat(idCard).isNotNull();
      assertThat(idCard).hasSize(18);
      assertThat(idCard).startsWith("110101");
    }

    @Test
    @DisplayName("批量生成身份证号应返回正确数量")
    void shouldGenerateCorrectNumberOfIdCards() {
      List<String> idCards = personGen.idCards(10);

      assertThat(idCards).hasSize(10);
      idCards.forEach(
          idCard -> {
            assertThat(idCard).isNotNull();
            assertThat(idCard).hasSize(18);
            assertThat(idCard).matches("^\\d{17}[0-9Xx]$");
          });
    }

    @Test
    @DisplayName("批量生成身份证号应保证唯一性")
    void shouldGenerateUniqueIdCardsInBatch() {
      List<String> idCards = personGen.idCards(100);

      assertThat(idCards).hasSize(100);
      long uniqueCount = idCards.stream().distinct().count();
      assertThat(uniqueCount).isGreaterThan(90);
    }
  }

  @Nested
  @DisplayName("电话号码生成测试")
  class PhoneGenerationTests {

    @Test
    @DisplayName("应生成电话号码")
    void shouldGeneratePhone() {
      String phone = personGen.phone();

      assertThat(phone).isNotNull();
      assertThat(phone).isNotEmpty();
      assertThat(phone).matches("^\\d{11}$");
    }

    @Test
    @DisplayName("应生成指定国家的电话号码")
    void shouldGeneratePhoneWithCountry() {
      String phone = personGen.phone("CN");

      assertThat(phone).isNotNull();
      assertThat(phone).isNotEmpty();
    }

    @Test
    @DisplayName("应生成手机号码")
    void shouldGenerateMobile() {
      String mobile = personGen.mobile();

      assertThat(mobile).isNotNull();
      assertThat(mobile).isNotEmpty();
      assertThat(mobile).matches("^1[3-9]\\d{9}$");
    }

    @Test
    @DisplayName("批量生成电话号码应返回正确数量")
    void shouldGenerateCorrectNumberOfPhones() {
      List<String> phones = personGen.phones(10);

      assertThat(phones).hasSize(10);
      phones.forEach(
          phone -> {
            assertThat(phone).isNotNull();
            assertThat(phone).isNotEmpty();
          });
    }

    @Test
    @DisplayName("批量生成电话号码应包含不同值")
    void shouldGenerateDifferentPhonesInBatch() {
      List<String> phones = personGen.phones(100);

      assertThat(phones).hasSize(100);
      long uniqueCount = phones.stream().distinct().count();
      assertThat(uniqueCount).isGreaterThan(90);
    }
  }

  @Nested
  @DisplayName("年龄生成测试")
  class AgeGenerationTests {

    @Test
    @DisplayName("应生成年龄")
    void shouldGenerateAge() {
      Integer age = personGen.age();

      assertThat(age).isNotNull();
      assertThat(age).isBetween(1, 100);
    }

    @Test
    @DisplayName("应生成指定范围的年龄")
    void shouldGenerateAgeInRange() {
      Integer age = personGen.age(18, 65);

      assertThat(age).isNotNull();
      assertThat(age).isBetween(18, 65);
    }

    @Test
    @DisplayName("应生成最小年龄")
    void shouldGenerateMinAge() {
      Integer age = personGen.age(1, 1);

      assertThat(age).isEqualTo(1);
    }

    @Test
    @DisplayName("应生成最大年龄")
    void shouldGenerateMaxAge() {
      Integer age = personGen.age(100, 100);

      assertThat(age).isEqualTo(100);
    }

    @Test
    @DisplayName("批量生成年龄应在范围内")
    void shouldGenerateAgesInRange() {
      for (int i = 0; i < 100; i++) {
        Integer age = personGen.age(18, 65);
        assertThat(age).isBetween(18, 65);
      }
    }
  }

  @Nested
  @DisplayName("性别生成测试")
  class GenderGenerationTests {

    @Test
    @DisplayName("应生成性别")
    void shouldGenerateGender() {
      String gender = personGen.gender();

      assertThat(gender).isNotNull();
      assertThat(gender).isIn("男", "女");
    }

    @Test
    @DisplayName("批量生成性别应包含两种性别")
    void shouldGenerateBothGenders() {
      List<String> genders = new java.util.ArrayList<>();
      for (int i = 0; i < 100; i++) {
        genders.add(personGen.gender());
      }

      assertThat(genders).contains("男", "女");
    }
  }

  @Nested
  @DisplayName("数据关联性测试")
  class DataCorrelationTests {

    @Test
    @DisplayName("中文姓名和性别应关联")
    void chineseNameShouldCorrelateWithGender() {
      String maleName = personGen.chineseName("MALE");
      String femaleName = personGen.chineseName("FEMALE");

      assertThat(maleName).isNotNull();
      assertThat(femaleName).isNotNull();
      assertThat(maleName).isNotEqualTo(femaleName);
    }

    @Test
    @DisplayName("英文姓名和性别应关联")
    void englishNameShouldCorrelateWithGender() {
      String maleName = personGen.englishName("MALE");
      String femaleName = personGen.englishName("FEMALE");

      assertThat(maleName).isNotNull();
      assertThat(femaleName).isNotNull();
      assertThat(maleName).isNotEqualTo(femaleName);
    }

    @Test
    @DisplayName("身份证号和地区应关联")
    void idCardShouldCorrelateWithRegion() {
      String idCard1 = personGen.idCard("110101");
      String idCard2 = personGen.idCard("310101");

      assertThat(idCard1).startsWith("110101");
      assertThat(idCard2).startsWith("310101");
      assertThat(idCard1).isNotEqualTo(idCard2);
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {

    @Test
    @DisplayName("批量生成0个姓名应返回空列表")
    void shouldReturnEmptyListForZeroCount() {
      List<String> names = personGen.fullNames(0);

      assertThat(names).isNotNull();
      assertThat(names).isEmpty();
    }

    @Test
    @DisplayName("批量生成1个姓名应返回列表")
    void shouldReturnListForOneCount() {
      List<String> names = personGen.fullNames(1);

      assertThat(names).isNotNull();
      assertThat(names).hasSize(1);
    }

    @Test
    @DisplayName("批量生成大量姓名应成功")
    void shouldGenerateLargeBatchOfNames() {
      List<String> names = personGen.fullNames(1000);

      assertThat(names).hasSize(1000);
      names.forEach(
          name -> {
            assertThat(name).isNotNull();
            assertThat(name).isNotEmpty();
          });
    }

    @Test
    @DisplayName("年龄范围最小值应正确")
    void shouldHandleMinAgeRange() {
      Integer age = personGen.age(0, 0);

      assertThat(age).isNotNull();
    }

    @Test
    @DisplayName("年龄范围最大值应正确")
    void shouldHandleMaxAgeRange() {
      Integer age = personGen.age(100, 100);

      assertThat(age).isEqualTo(100);
    }
  }

  @Nested
  @DisplayName("性能测试")
  class PerformanceTests {

    @Test
    @DisplayName("批量生成应高效")
    void shouldGenerateBatchEfficiently() {
      long startTime = System.currentTimeMillis();

      List<String> names = personGen.fullNames(10000);

      long duration = System.currentTimeMillis() - startTime;

      assertThat(names).hasSize(10000);
      assertThat(duration).isLessThan(5000);
    }

    @Test
    @DisplayName("连续生成应高效")
    void shouldGenerateContinuouslyEfficiently() {
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < 1000; i++) {
        personGen.fullName();
        personGen.idCard();
        personGen.phone();
      }

      long duration = System.currentTimeMillis() - startTime;

      assertThat(duration).isLessThan(5000);
    }
  }

  @Nested
  @DisplayName("数据格式验证测试")
  class DataFormatValidationTests {

    @Test
    @DisplayName("中文姓名应只包含中文字符")
    void chineseNameShouldContainOnlyChineseCharacters() {
      String name = personGen.chineseName();

      assertThat(name).matches("^[\\u4e00-\\u9fa5]+$");
    }

    @Test
    @DisplayName("英文姓名应只包含字母和空格")
    void englishNameShouldContainOnlyLettersAndSpaces() {
      String name = personGen.englishName();

      assertThat(name).matches("^[a-zA-Z\\s]+$");
    }

    @Test
    @DisplayName("身份证号应为18位")
    void idCardShouldBe18Digits() {
      String idCard = personGen.idCard();

      assertThat(idCard).hasSize(18);
    }

    @Test
    @DisplayName("身份证号最后一位应为数字或X")
    void idCardLastCharShouldBeDigitOrX() {
      String idCard = personGen.idCard();
      char lastChar = idCard.charAt(17);

      assertThat(Character.isDigit(lastChar) || lastChar == 'X' || lastChar == 'x').isTrue();
    }

    @Test
    @DisplayName("手机号应以1开头")
    void mobileShouldStartWith1() {
      String mobile = personGen.mobile();

      assertThat(mobile).startsWith("1");
    }

    @Test
    @DisplayName("手机号应为11位")
    void mobileShouldBe11Digits() {
      String mobile = personGen.mobile();

      assertThat(mobile).hasSize(11);
    }
  }

  @Nested
  @DisplayName("异常处理测试")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("无效年龄范围应抛出异常")
    void shouldThrowExceptionForInvalidAgeRange() {
      assertThatThrownBy(() -> personGen.age(100, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("负数年龄范围应抛出异常")
    void shouldThrowExceptionForNegativeAgeRange() {
      assertThatThrownBy(() -> personGen.age(-1, 10)).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("集成测试")
  class IntegrationTests {

    @Test
    @DisplayName("应生成完整人员信息")
    void shouldGenerateCompletePersonInfo() {
      String name = personGen.fullName();
      String idCard = personGen.idCard();
      String phone = personGen.phone();
      String gender = personGen.gender();
      Integer age = personGen.age();

      assertThat(name).isNotNull();
      assertThat(idCard).isNotNull();
      assertThat(phone).isNotNull();
      assertThat(gender).isNotNull();
      assertThat(age).isNotNull();
    }

    @Test
    @DisplayName("应生成批量完整人员信息")
    void shouldGenerateBatchCompletePersonInfo() {
      List<String> names = personGen.fullNames(10);
      List<String> idCards = personGen.idCards(10);
      List<String> phones = personGen.phones(10);

      assertThat(names).hasSize(10);
      assertThat(idCards).hasSize(10);
      assertThat(phones).hasSize(10);
    }

    @Test
    @DisplayName("应生成关联的完整人员信息")
    void shouldGenerateCorrelatedCompletePersonInfo() {
      String maleName = personGen.chineseName("MALE");
      String femaleName = personGen.chineseName("FEMALE");

      assertThat(maleName).isNotNull();
      assertThat(femaleName).isNotNull();
      assertThat(maleName).isNotEqualTo(femaleName);
    }
  }
}
