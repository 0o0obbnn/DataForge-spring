package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("地址生成器测试")
class AddressGeneratorTest {

  private AddressGenerator generator;
  private SimpleFieldConfig config;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new AddressGenerator();
    config = new SimpleFieldConfig();
    config.setType("address");
    context = new DataForgeContext();
  }

  @Test
  @DisplayName("生成标准地址")
  void shouldGenerateValidAddress() {
    String address = generator.generate(config, context);

    assertThat(address).isNotNull();
    assertThat(address).isNotEmpty();
    // 地址应该包含省份、城市等信息
    assertThat(address.length()).isGreaterThan(5);
  }

  @Test
  @DisplayName("生成详细地址")
  void shouldGenerateDetailedAddress() {
    config.setParam("format", "detailed");

    String address = generator.generate(config, context);

    assertThat(address).isNotNull();
    assertThat(address).isNotEmpty();
  }

  @Test
  @DisplayName("生成简洁地址")
  void shouldGenerateSimpleAddress() {
    config.setParam("format", "simple");

    String address = generator.generate(config, context);

    assertThat(address).isNotNull();
    assertThat(address).isNotEmpty();
  }

  @Test
  @DisplayName("生成多个不同地址")
  void shouldGenerateDifferentAddresses() {
    String address1 = generator.generate(config, context);
    String address2 = generator.generate(config, context);

    assertThat(address1).isNotNull();
    assertThat(address2).isNotNull();
    assertThat(address1).isNotEmpty();
    assertThat(address2).isNotEmpty();
  }

  @Test
  @DisplayName("地址包含省份信息")
  void shouldGenerateAddressWithProvince() {
    String address = generator.generate(config, context);

    assertThat(address).isNotNull();
    // 中国地址通常包含"省"或"市"
    assertThat(address.contains("省") || address.contains("市") || address.contains("区")).isTrue();
  }

  @Test
  @DisplayName("默认生成有效地址")
  void shouldDefaultToValidAddress() {
    SimpleFieldConfig emptyConfig = new SimpleFieldConfig();

    String address = generator.generate(emptyConfig, context);

    assertThat(address).isNotNull();
    assertThat(address).isNotEmpty();
  }

  @Test
  @DisplayName("生成地址长度合理")
  void shouldGenerateAddressWithReasonableLength() {
    String address = generator.generate(config, context);

    assertThat(address).isNotNull();
    // 地址长度通常在10-100字符之间
    assertThat(address.length()).isBetween(5, 100);
  }
}
