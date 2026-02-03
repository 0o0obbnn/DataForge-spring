package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
  @DisplayName("生成标准UUID4格式")
  void shouldGenerateValidUuid4Format() {
    String uuid = generator.generate(config, context);

    assertThat(uuid).isNotNull();
    assertThat(uuid)
        .matches("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
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

  @Test
  @DisplayName("生成UUID4(随机)")
  void shouldGenerateUuid4() {
    config.setParam("type", "UUID4");

    String uuid = generator.generate(config, context);

    assertThat(uuid).isNotNull();
    // UUID4格式: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
    assertThat(uuid)
        .matches("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
  }

  @Test
  @DisplayName("生成UUID并验证类型")
  void shouldGenerateUuidWithCorrectType() {
    config.setParam("type", "UUID4");

    String uuid = generator.generate(config, context);

    assertThat(uuid).isNotNull();
    // 验证是有效的UUID格式
    assertThat(UUID.fromString(uuid).toString()).isEqualTo(uuid);
    // 验证版本号为4
    char version = uuid.charAt(14);
    assertThat(version).isEqualTo('4');
  }

  @Test
  @DisplayName("空配置使用默认值(UUID4)")
  void shouldUseDefaultsWithEmptyConfig() {
    SimpleFieldConfig emptyConfig = new SimpleFieldConfig();

    String uuid = generator.generate(emptyConfig, context);

    assertThat(uuid).isNotNull();
    assertThat(uuid).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
  }

  @Test
  @DisplayName("生成多个UUID并验证版本")
  void shouldGenerateMultipleUuidsWithCorrectVersion() {
    for (int i = 0; i < 10; i++) {
      String uuid = generator.generate(config, context);
      // 验证是有效的UUID格式
      assertThat(UUID.fromString(uuid).toString()).isEqualTo(uuid);
    }
  }
}
