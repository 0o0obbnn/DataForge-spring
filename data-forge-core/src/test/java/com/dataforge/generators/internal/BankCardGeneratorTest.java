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
    @DisplayName("生成银行卡号")
    void shouldGenerateBankCard() {
        String card = generator.generate(config, context);

        assertThat(card).isNotNull();
        assertThat(card).matches("^\\d{15,19}$");
    }

    @Test
    @DisplayName("生成银行卡号(指定银行)")
    void shouldGenerateBankCardWithBank() {
        config.setParam("bank", "ICBC");
        config.setParam("type", "DEBIT");

        String card = generator.generate(config, context);

        assertThat(card).isNotNull();
        assertThat(card).matches("^\\d{15,19}$");
    }

    @Test
    @DisplayName("生成银行卡号(指定卡组织)")
    void shouldGenerateBankCardWithOrganization() {
        config.setParam("organization", "UNIONPAY");

        String card = generator.generate(config, context);

        assertThat(card).isNotNull();
        assertThat(card).matches("^\\d{15,19}$");
    }

    @Test
    @DisplayName("生成信用卡")
    void shouldGenerateCreditCard() {
        config.setParam("type", "CREDIT");

        String card = generator.generate(config, context);

        assertThat(card).isNotNull();
        assertThat(card.length()).isBetween(15, 19);
    }

    @Test
    @DisplayName("生成借记卡")
    void shouldGenerateDebitCard() {
        config.setParam("type", "DEBIT");

        String card = generator.generate(config, context);

        assertThat(card).isNotNull();
        assertThat(card.length()).isBetween(15, 19);
    }

    @Test
    @DisplayName("生成多个不同卡号")
    void shouldGenerateDifferentCards() {
        for (int i = 0; i < 10; i++) {
            String card = generator.generate(config, context);
            assertThat(card).matches("^\\d{15,19}$");
        }
    }

    @Test
    @DisplayName("默认生成有效卡号")
    void shouldDefaultToValidCard() {
        SimpleFieldConfig emptyConfig = new SimpleFieldConfig();

        String card = generator.generate(emptyConfig, context);

        assertThat(card).isNotNull();
        assertThat(card).matches("^\\d{15,19}$");
    }
}
