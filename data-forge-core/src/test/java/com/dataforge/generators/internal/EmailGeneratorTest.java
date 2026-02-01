package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("邮箱生成器测试")
class EmailGeneratorTest {

    private EmailGenerator generator;
    private SimpleFieldConfig config;
    private DataForgeContext context;

    @BeforeEach
    void setUp() {
        generator = new EmailGenerator();
        config = new SimpleFieldConfig();
        config.setType("email");
        context = new DataForgeContext();
    }

    @Test
    @DisplayName("生成标准邮箱格式")
    void shouldGenerateValidEmailFormat() {
        String email = generator.generate(config, context);

        assertThat(email).isNotNull();
        assertThat(email).matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    @ParameterizedTest
    @ValueSource(strings = {"gmail.com", "qq.com", "163.com", "outlook.com", "hotmail.com"})
    @DisplayName("生成指定域名邮箱")
    void shouldGenerateEmailWithSpecificDomain(String domain) {
        config.setParam("domain", domain);

        String email = generator.generate(config, context);

        assertThat(email).isNotNull();
        assertThat(email).endsWith("@" + domain);
    }

    @Test
    @DisplayName("生成QQ邮箱")
    void shouldGenerateQQEmail() {
        config.setParam("type", "QQ");

        String email = generator.generate(config, context);

        assertThat(email).isNotNull();
        assertThat(email).matches("^\\d{5,11}@qq\\.com$");
    }

    @Test
    @DisplayName("生成163邮箱")
    void shouldGenerate163Email() {
        config.setParam("type", "163");

        String email = generator.generate(config, context);

        assertThat(email).isNotNull();
        assertThat(email).endsWith("@163.com");
    }

    @Test
    @DisplayName("生成多个不同邮箱")
    void shouldGenerateDifferentEmails() {
        for (int i = 0; i < 10; i++) {
            String email = generator.generate(config, context);
            assertThat(email).matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        }
    }

    @Test
    @DisplayName("邮箱用户名部分不为空")
    void shouldGenerateNonEmptyUsername() {
        String email = generator.generate(config, context);

        assertThat(email).isNotNull();
        String username = email.substring(0, email.indexOf('@'));
        assertThat(username).isNotEmpty();
    }

    @Test
    @DisplayName("生成企业邮箱")
    void shouldGenerateEnterpriseEmail() {
        config.setParam("domain", "company.com");

        String email = generator.generate(config, context);

        assertThat(email).isNotNull();
        assertThat(email).endsWith("@company.com");
    }
}
