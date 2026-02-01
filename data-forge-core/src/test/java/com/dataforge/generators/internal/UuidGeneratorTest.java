package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("UUID生成器测试")
class UuidGeneratorTest {

    private UuidGenerator generator;
    private SimpleFieldConfig config;
    private DataForgeContext context;

    @BeforeEach
    void setUp() {
        generator = new UuidGenerator();
        config = new SimpleFieldConfig();
        config.setType("uuid");
        context = new DataForgeContext();
    }

    @Test
    @DisplayName("生成标准UUID格式")
    void shouldGenerateValidUuidFormat() {
        String uuid = generator.generate(config, context);

        assertThat(uuid).isNotNull();
        assertThat(uuid).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    @Test
    @DisplayName("生成的UUID唯一性")
    void shouldGenerateUniqueUuids() {
        Set<String> uuids = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            String uuid = generator.generate(config, context);
            assertThat(uuids).doesNotContain(uuid);
            uuids.add(uuid);
        }

        assertThat(uuids).hasSize(100);
    }

    @ParameterizedTest
    @ValueSource(strings = {"standard", "random"})
    @DisplayName("支持不同UUID版本参数")
    void shouldSupportVersionParam(String version) {
        config.setParam("version", version);

        String uuid = generator.generate(config, context);

        assertThat(uuid).isNotNull();
        assertThat(uuid).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    @Test
    @DisplayName("生成大写格式UUID")
    void shouldGenerateUppercaseUuid() {
        config.setParam("uppercase", "true");

        String uuid = generator.generate(config, context);

        assertThat(uuid).matches("^[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}$");
    }

    @Test
    @DisplayName("生成无连字符UUID")
    void shouldGenerateUuidWithoutHyphens() {
        config.setParam("hyphens", "false");

        String uuid = generator.generate(config, context);

        assertThat(uuid).matches("^[0-9a-f]{32}$");
    }

    @Test
    @DisplayName("空配置使用默认值")
    void shouldUseDefaultsWithEmptyConfig() {
        SimpleFieldConfig emptyConfig = new SimpleFieldConfig();
        String uuid = generator.generate(emptyConfig, context);

        assertThat(uuid).isNotNull();
        assertThat(uuid).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }
}
