package com.dataforge.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DataGen 测试")
class DataGenTest {

  private DataGen dataGen;

  @BeforeEach
  void setUp() {
    dataGen = new DataGen();
  }

  @Nested
  @DisplayName("构造函数测试")
  class ConstructorTests {
    @Test
    @DisplayName("无参构造函数应创建实例")
    void shouldCreateInstanceWithNoArgConstructor() {
      DataGen gen = new DataGen();

      assertThat(gen).isNotNull();
    }

    @Test
    @DisplayName("实例应能调用所有基础方法")
    void instanceShouldCallAllBasicMethods() {
      DataGen gen = new DataGen();

      assertThat(gen.uuid()).isNotNull();
      assertThat(gen.name()).isNotNull();
      assertThat(gen.phone()).isNotNull();
      assertThat(gen.email()).isNotNull();
      assertThat(gen.bankCard()).isNotNull();
    }
  }

  @Nested
  @DisplayName("基础生成方法测试")
  class BasicGenerationTests {
    @Test
    @DisplayName("应生成UUID")
    void shouldGenerateUuid() {
      String uuid = dataGen.uuid();

      assertThat(uuid).isNotNull();
      assertThat(uuid).isNotEmpty();
      assertThat(uuid).matches("[0-9a-fA-F-]{36}");
    }

    @Test
    @DisplayName("应生成姓名")
    void shouldGenerateName() {
      String name = dataGen.name();

      assertThat(name).isNotNull();
      assertThat(name).isNotEmpty();
      assertThat(name.length()).isBetween(2, 50);
    }

    @Test
    @DisplayName("应生成电话号码")
    void shouldGeneratePhone() {
      String phone = dataGen.phone();

      assertThat(phone).isNotNull();
      assertThat(phone).isNotEmpty();
      assertThat(phone).matches("^[0-9+\\-\\s]+$");
    }

    @Test
    @DisplayName("应生成邮箱")
    void shouldGenerateEmail() {
      String email = dataGen.email();

      assertThat(email).isNotNull();
      assertThat(email).isNotEmpty();
      assertThat(email).matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    @Test
    @DisplayName("应生成银行卡号")
    void shouldGenerateBankCard() {
      String bankCard = dataGen.bankCard();

      assertThat(bankCard).isNotNull();
      assertThat(bankCard).isNotEmpty();
      assertThat(bankCard).matches("\\d{16,19}");
    }

    @Test
    @DisplayName("应生成整数")
    void shouldGenerateInteger() {
      Integer integer = dataGen.integer();

      assertThat(integer).isNotNull();
    }

    @Test
    @DisplayName("应生成小数")
    void shouldGenerateDecimal() {
      Double decimal = dataGen.decimal();

      assertThat(decimal).isNotNull();
    }
  }

  @Nested
  @DisplayName("参数化生成方法测试")
  class ParameterizedGenerationTests {
    @Test
    @DisplayName("应生成指定类型的姓名")
    void shouldGenerateNameWithTypeAndGender() {
      String name = dataGen.name("CN", "MALE");

      assertThat(name).isNotNull();
      assertThat(name).isNotEmpty();
    }

    @Test
    @DisplayName("应生成指定国家的电话")
    void shouldGeneratePhoneWithCountry() {
      String phone = dataGen.phone("CN");

      assertThat(phone).isNotNull();
      assertThat(phone).isNotEmpty();
    }

    @Test
    @DisplayName("应生成指定域名的邮箱")
    void shouldGenerateEmailWithDomain() {
      String email = dataGen.email("example.com");

      assertThat(email).isNotNull();
      assertThat(email).isNotEmpty();
      assertThat(email).contains("@example.com");
    }

    @Test
    @DisplayName("应生成指定范围的整数")
    void shouldGenerateIntegerInRange() {
      Integer integer = dataGen.integer(1, 100);

      assertThat(integer).isNotNull();
      assertThat(integer).isBetween(1, 100);
    }

    @Test
    @DisplayName("应生成指定范围和精度的小数")
    void shouldGenerateDecimalInRangeWithScale() {
      Double decimal = dataGen.decimal(0.0, 100.0, 2);

      assertThat(decimal).isNotNull();
      assertThat(decimal).isBetween(0.0, 100.0);
    }
  }

  @Nested
  @DisplayName("分类门面访问器测试")
  class FacadeAccessorTests {
    @Test
    @DisplayName("获取PersonGen应返回实例")
    void shouldReturnPersonGenInstance() {
      PersonGen personGen = dataGen.person();

      assertThat(personGen).isNotNull();
    }

    @Test
    @DisplayName("获取InternetGen应返回实例")
    void shouldReturnInternetGenInstance() {
      InternetGen internetGen = dataGen.internet();

      assertThat(internetGen).isNotNull();
    }

    @Test
    @DisplayName("获取AddressGen应返回实例")
    void shouldReturnAddressGenInstance() {
      AddressGen addressGen = dataGen.address();

      assertThat(addressGen).isNotNull();
    }

    @Test
    @DisplayName("获取FinanceGen应返回实例")
    void shouldReturnFinanceGenInstance() {
      FinanceGen financeGen = dataGen.finance();

      assertThat(financeGen).isNotNull();
    }

    @Test
    @DisplayName("获取DateTimeGen应返回实例")
    void shouldReturnDateTimeGenInstance() {
      DateTimeGen dateTimeGen = dataGen.dateTime();

      assertThat(dateTimeGen).isNotNull();
    }

    @Test
    @DisplayName("多次获取应返回同一实例")
    void shouldReturnSameInstanceOnMultipleAccess() {
      PersonGen gen1 = dataGen.person();
      PersonGen gen2 = dataGen.person();

      assertThat(gen1).isSameAs(gen2);
    }
  }

  @Nested
  @DisplayName("批量生成方法测试")
  class BatchGenerationTests {
    @Test
    @DisplayName("批量生成UUID应返回正确数量")
    void shouldGenerateCorrectNumberOfUuids() {
      int count = 100;
      List<String> uuids = dataGen.uuids(count);

      assertThat(uuids).hasSize(count);
      assertThat(uuids).allSatisfy(uuid -> {
        assertThat(uuid).isNotNull();
        assertThat(uuid).matches("[0-9a-fA-F-]{36}");
      });
    }

    @Test
    @DisplayName("批量生成姓名应返回正确数量")
    void shouldGenerateCorrectNumberOfNames() {
      int count = 100;
      List<String> names = dataGen.names(count);

      assertThat(names).hasSize(count);
      assertThat(names).allSatisfy(name -> {
        assertThat(name).isNotNull();
        assertThat(name).isNotEmpty();
      });
    }

    @Test
    @DisplayName("批量生成电话应返回正确数量")
    void shouldGenerateCorrectNumberOfPhones() {
      int count = 100;
      List<String> phones = dataGen.phones(count);

      assertThat(phones).hasSize(count);
      assertThat(phones).allSatisfy(phone -> {
        assertThat(phone).isNotNull();
        assertThat(phone).matches("^[0-9+\\-\\s]+$");
      });
    }

    @Test
    @DisplayName("批量生成邮箱应返回正确数量")
    void shouldGenerateCorrectNumberOfEmails() {
      int count = 100;
      List<String> emails = dataGen.emails(count);

      assertThat(emails).hasSize(count);
      assertThat(emails).allSatisfy(email -> {
        assertThat(email).isNotNull();
        assertThat(email).matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
      });
    }

    @Test
    @DisplayName("批量生成银行卡应返回正确数量")
    void shouldGenerateCorrectNumberOfBankCards() {
      int count = 100;
      List<String> bankCards = dataGen.bankCards(count);

      assertThat(bankCards).hasSize(count);
      assertThat(bankCards).allSatisfy(card -> {
        assertThat(card).isNotNull();
        // 银行卡号可以是15-19位数字
        assertThat(card).matches("\\d{15,19}");
      });
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {
    @Test
    @DisplayName("批量生成0个应返回空列表")
    void shouldReturnEmptyListForZeroCount() {
      List<String> uuids = dataGen.uuids(0);

      assertThat(uuids).isNotNull();
      assertThat(uuids).isEmpty();
    }

    @Test
    @DisplayName("批量生成大量数据应成功")
    void shouldGenerateLargeBatchOfData() {
      int count = 10000;
      List<String> uuids = dataGen.uuids(count);

      assertThat(uuids).hasSize(count);
      assertThat(uuids).allSatisfy(uuid -> {
        assertThat(uuid).isNotNull();
        assertThat(uuid).isNotEmpty();
      });
    }

    @Test
    @DisplayName("整数范围最小值等于最大值应生成固定值")
    void shouldGenerateFixedValueWhenMinEqualsMax() {
      Integer integer = dataGen.integer(100, 100);

      assertThat(integer).isEqualTo(100);
    }

    @Test
    @DisplayName("小数范围最小值等于最大值应生成固定值")
    void shouldGenerateFixedValueWhenDecimalMinEqualsMax() {
      Double decimal = dataGen.decimal(100.0, 100.0, 2);

      assertThat(decimal).isEqualTo(100.0);
    }
  }

  @Nested
  @DisplayName("数据格式验证测试")
  class FormatValidationTests {
    @Test
    @DisplayName("UUID格式应为有效格式")
    void shouldHaveValidUuidFormat() {
      String uuid = dataGen.uuid();

      assertThat(uuid).matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    @Test
    @DisplayName("邮箱格式应为有效格式")
    void shouldHaveValidEmailFormat() {
      String email = dataGen.email();

      assertThat(email).matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    @Test
    @DisplayName("银行卡号格式应为有效格式")
    void shouldHaveValidBankCardFormat() {
      String bankCard = dataGen.bankCard();

      // 银行卡号可以是15-19位数字
      assertThat(bankCard).matches("\\d{15,19}");
    }

    @Test
    @DisplayName("整数应在合理范围内")
    void integerShouldBeInReasonableRange() {
      Integer integer = dataGen.integer();

      assertThat(integer).isNotNull();
    }

    @Test
    @DisplayName("小数应在合理范围内")
    void decimalShouldBeInReasonableRange() {
      Double decimal = dataGen.decimal();

      assertThat(decimal).isNotNull();
    }
  }
}