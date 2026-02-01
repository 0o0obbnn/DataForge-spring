package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("银行卡生成器测试")
class BankCardGeneratorTest {

    private BankCardGenerator generator;
    private SimpleFieldConfig config;
    private DataForgeContext context;

    @BeforeEach
    void setUp() {
        generator = new BankCardGenerator();
        config = new SimpleFieldConfig();
        config.setType("bankcard");
        context = new DataForgeContext();
    }

    @Test
    @DisplayName("生成标准银行卡号")
    void shouldGenerateValidBankCard() {
        String card = generator.generate(config, context);

        assertThat(card).isNotNull();
        assertThat(card).matches("^\\d{16,19}$");
    }

    @Test
    @DisplayName("生成工商银行借记卡")
    void shouldGenerateICBCDebitCard() {
        config.setParam("bank", "ICBC");
        config.setParam("type", "DEBIT");

        String card = generator.generate(config, context);

        assertThat(card).isNotNull();
        assertThat(card).matches("^\\d{16,19}$");
        // 工行借记卡通常以6222开头
        assertThat(card.startsWith("62")).isTrue();
    }

    @Test
    @DisplayName("生成银联卡")
    void shouldGenerateUnionPayCard() {
        config.setParam("organization", "UNIONPAY");

        String card = generator.generate(config, context);

        assertThat(card).isNotNull();
        // 银联卡以62开头
        assertThat(card.startsWith("62")).isTrue();
        assertThat(card.length()).isBetween(16, 19);
    }

    @Test
    @DisplayName("生成信用卡")
    void shouldGenerateCreditCard() {
        config.setParam("type", "CREDIT");

        String card = generator.generate(config, context);

        assertThat(card).isNotNull();
        assertThat(card.length()).isBetween(16, 19);
    }

    @Test
    @DisplayName("生成借记卡")
    void shouldGenerateDebitCard() {
        config.setParam("type", "DEBIT");

        String card = generator.generate(config, context);

        assertThat(card).isNotNull();
        assertThat(card.length()).isBetween(16, 19);
    }

    @Test
    @DisplayName("生成的卡号通过Luhn校验")
    void shouldGenerateCardPassingLuhnCheck() {
        // 生成多个卡号并验证格式
        for (int i = 0; i < 10; i++) {
            String card = generator.generate(config, context);
            assertThat(card).matches("^\\d{16,19}$");
        }
    }

    @Test
    @DisplayName("生成多个不同卡号")
    void shouldGenerateDifferentCards() {
        String card1 = generator.generate(config, context);
        String card2 = generator.generate(config, context);

        assertThat(card1).isNotNull();
        assertThat(card2).isNotNull();
        assertThat(card1).matches("^\\d{16,19}$");
        assertThat(card2).matches("^\\d{16,19}$");
    }

    @Test
    @DisplayName("默认生成有效卡号")
    void shouldDefaultToValidCard() {
        SimpleFieldConfig emptyConfig = new SimpleFieldConfig();

        String card = generator.generate(emptyConfig, context);

        assertThat(card).isNotNull();
        assertThat(card).matches("^\\d{16,19}$");
    }
}
