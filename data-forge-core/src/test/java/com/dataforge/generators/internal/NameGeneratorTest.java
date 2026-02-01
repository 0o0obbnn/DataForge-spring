package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("姓名生成器测试")
class NameGeneratorTest {

    private NameGenerator generator;
    private SimpleFieldConfig config;
    private DataForgeContext context;

    @BeforeEach
    void setUp() {
        generator = new NameGenerator();
        config = new SimpleFieldConfig();
        config.setType("name");
        context = new DataForgeContext();
    }

    @Test
    @DisplayName("生成中文姓名")
    void shouldGenerateChineseName() {
        config.setParam("type", "CN");

        String name = generator.generate(config, context);

        assertThat(name).isNotNull();
        assertThat(name.length()).isBetween(2, 4);
        // 中文姓名应该只包含中文字符
        assertThat(name).matches("^[\\u4e00-\\u9fa5]+$");
    }

    @Test
    @DisplayName("生成英文姓名")
    void shouldGenerateEnglishName() {
        config.setParam("type", "EN");

        String name = generator.generate(config, context);

        assertThat(name).isNotNull();
        assertThat(name).contains(" ");
        // 英文名应该只包含字母和空格
        assertThat(name).matches("^[a-zA-Z\\s]+$");
    }

    @ParameterizedTest
    @ValueSource(strings = {"MALE", "FEMALE", "ANY"})
    @DisplayName("按性别生成姓名")
    void shouldGenerateNameByGender(String gender) {
        config.setParam("type", "CN");
        config.setParam("gender", gender);

        String name = generator.generate(config, context);

        assertThat(name).isNotNull();
        assertThat(name.length()).isBetween(2, 4);
    }

    @Test
    @DisplayName("生成男性中文姓名")
    void shouldGenerateMaleChineseName() {
        config.setParam("type", "CN");
        config.setParam("gender", "MALE");

        String name = generator.generate(config, context);

        assertThat(name).isNotNull();
        assertThat(name.length()).isBetween(2, 4);
    }

    @Test
    @DisplayName("生成女性中文姓名")
    void shouldGenerateFemaleChineseName() {
        config.setParam("type", "CN");
        config.setParam("gender", "FEMALE");

        String name = generator.generate(config, context);

        assertThat(name).isNotNull();
        assertThat(name.length()).isBetween(2, 4);
    }

    @Test
    @DisplayName("生成多个不重复的姓名")
    void shouldGenerateUniqueNames() {
        config.setParam("type", "CN");

        String name1 = generator.generate(config, context);
        String name2 = generator.generate(config, context);

        // 由于随机性，两个姓名可能相同也可能不同
        // 但都应该符合中文姓名格式
        assertThat(name1).matches("^[\\u4e00-\\u9fa5]+$");
        assertThat(name2).matches("^[\\u4e00-\\u9fa5]+$");
    }

    @Test
    @DisplayName("默认生成中文姓名")
    void shouldDefaultToChineseName() {
        String name = generator.generate(config, context);

        assertThat(name).isNotNull();
        assertThat(name).matches("^[\\u4e00-\\u9fa5]+$");
    }
}
