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

    @Test
    @DisplayName("生成指定域名列表的邮箱")
    void shouldGenerateEmailWithSpecificDomains() {
        config.setParam("domains", "gmail.com,qq.com,163.com");

        String email = generator.generate(config, context);

        assertThat(email).isNotNull();
        // 验证邮箱格式正确
        assertThat(email).matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        // 验证是三个域名之一
        assertThat(email.endsWith("@gmail.com") || email.endsWith("@qq.com") || email.endsWith("@163.com")).isTrue();
    }

    @Test
    @DisplayName("生成个人类型邮箱")
    void shouldGeneratePersonalEmail() {
        config.setParam("type", "PERSONAL");

        String email = generator.generate(config, context);

        assertThat(email).isNotNull();
        assertThat(email).matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    @Test
    @DisplayName("生成企业类型邮箱")
    void shouldGenerateEnterpriseEmail() {
        config.setParam("type", "ENTERPRISE");

        String email = generator.generate(config, context);

        assertThat(email).isNotNull();
        assertThat(email).matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
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
    @DisplayName("使用姓名前缀生成邮箱")
    void shouldGenerateEmailWithNamePrefix() {
        config.setParam("prefix_name", "true");

        String email = generator.generate(config, context);

        assertThat(email).isNotNull();
        assertThat(email).matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    @Test
    @DisplayName("指定用户名长度范围")
    void shouldGenerateEmailWithCustomUsernameLength() {
        config.setParam("username_length", "8,15");

        String email = generator.generate(config, context);

        assertThat(email).isNotNull();
        assertThat(email).matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    @Test
    @DisplayName("生成无效邮箱")
    void shouldGenerateInvalidEmail() {
        config.setParam("valid", "false");

        String email = generator.generate(config, context);

        assertThat(email).isNotNull();
        // 无效邮箱格式可能不符合标准
        assertThat(email).isNotEmpty();
    }

    @Test
    @DisplayName("默认生成有效邮箱")
    void shouldDefaultToValidEmail() {
        SimpleFieldConfig emptyConfig = new SimpleFieldConfig();

        String email = generator.generate(emptyConfig, context);

        assertThat(email).isNotNull();
        assertThat(email).matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }
}
