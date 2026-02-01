package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("手机号生成器测试")
class PhoneGeneratorTest {

    private PhoneGenerator generator;
    private SimpleFieldConfig config;
    private DataForgeContext context;

    @BeforeEach
    void setUp() {
        generator = new PhoneGenerator();
        config = new SimpleFieldConfig();
        config.setType("phone");
        context = new DataForgeContext();
    }

    @Test
    @DisplayName("生成标准手机号格式")
    void shouldGenerateValidPhoneNumber() {
        String phone = generator.generate(config, context);

        assertThat(phone).isNotNull();
        assertThat(phone).matches("^1[3-9]\\d{9}$");
        assertThat(phone.length()).isEqualTo(11);
    }

    @Test
    @DisplayName("生成中国移动手机号")
    void shouldGenerateChinaMobileNumber() {
        config.setParam("carrier", "MOBILE");

        String phone = generator.generate(config, context);

        assertThat(phone).isNotNull();
        assertThat(phone).matches("^1(3[4-9]|4[7-8]|5[0-27-9]|7[8]|8[2-478]|9[578])\\d{8}$");
    }

    @Test
    @DisplayName("生成中国联通手机号")
    void shouldGenerateChinaUnicomNumber() {
        config.setParam("carrier", "UNICOM");

        String phone = generator.generate(config, context);

        assertThat(phone).isNotNull();
        assertThat(phone).matches("^1(3[0-2]|4[56]|5[56]|6[67]|7[156]|8[56]|9[6])\\d{8}$");
    }

    @Test
    @DisplayName("生成中国电信手机号")
    void shouldGenerateChinaTelecomNumber() {
        config.setParam("carrier", "TELECOM");

        String phone = generator.generate(config, context);

        assertThat(phone).isNotNull();
        assertThat(phone).matches("^1(33|49|53|7[347]|8[019]|9[139])\\d{8}$");
    }

    @ParameterizedTest
    @ValueSource(strings = {"MOBILE", "UNICOM", "TELECOM", "ANY"})
    @DisplayName("支持不同运营商参数")
    void shouldSupportCarrierParam(String carrier) {
        config.setParam("carrier", carrier);

        String phone = generator.generate(config, context);

        assertThat(phone).isNotNull();
        assertThat(phone).matches("^1[3-9]\\d{9}$");
    }

    @Test
    @DisplayName("生成带格式的手机号")
    void shouldGenerateFormattedPhoneNumber() {
        config.setParam("formatted", "true");

        String phone = generator.generate(config, context);

        assertThat(phone).isNotNull();
        assertThat(phone).matches("^1[3-9]\\d{2}-\\d{4}-\\d{4}$");
    }

    @Test
    @DisplayName("生成多个手机号")
    void shouldGenerateMultiplePhoneNumbers() {
        for (int i = 0; i < 10; i++) {
            String phone = generator.generate(config, context);
            assertThat(phone).matches("^1[3-9]\\d{9}$");
        }
    }

    @Test
    @DisplayName("默认生成任意运营商手机号")
    void shouldDefaultToAnyCarrier() {
        String phone = generator.generate(config, context);

        assertThat(phone).isNotNull();
        assertThat(phone).matches("^1[3-9]\\d{9}$");
    }
}
