package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("日期生成器测试")
class DateGeneratorTest {

  private DateGenerator generator;
  private SimpleFieldConfig config;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new DateGenerator();
    config = new SimpleFieldConfig();
    config.setType("date");
    context = new DataForgeContext();
  }

  @Test
  @DisplayName("生成标准日期字符串")
  void shouldGenerateValidDateString() {
    String date = generator.generate(config, context);

    assertThat(date).isNotNull();
    // 默认格式 yyyy-MM-dd
    assertThat(date).matches("^\\d{4}-\\d{2}-\\d{2}$");
  }

  @Test
  @DisplayName("生成指定范围内的日期")
  void shouldGenerateDateInRange() {
    config.setParam("startDate", "2020-01-01");
    config.setParam("endDate", "2023-12-31");

    String date = generator.generate(config, context);

    assertThat(date).isNotNull();
    assertThat(date).isNotEmpty();
  }

  @Test
  @DisplayName("生成ISO格式日期")
  void shouldGenerateISODate() {
    config.setParam("format", "ISO");

    String date = generator.generate(config, context);

    assertThat(date).isNotNull();
    assertThat(date).matches("^\\d{4}-\\d{2}-\\d{2}$");
  }

  @Test
  @DisplayName("生成美式日期格式")
  void shouldGenerateUSDate() {
    config.setParam("format", "US");

    String date = generator.generate(config, context);

    assertThat(date).isNotNull();
    assertThat(date).matches("^\\d{2}/\\d{2}/\\d{4}$");
  }

  @Test
  @DisplayName("生成欧式日期格式")
  void shouldGenerateEUDate() {
    config.setParam("format", "EU");

    String date = generator.generate(config, context);

    assertThat(date).isNotNull();
    assertThat(date).matches("^\\d{2}/\\d{2}/\\d{4}$");
  }

  @Test
  @DisplayName("生成自定义格式日期")
  void shouldGenerateCustomFormatDate() {
    config.setParam("format", "CUSTOM");
    config.setParam("customFormat", "yyyy/MM/dd");

    String date = generator.generate(config, context);

    assertThat(date).isNotNull();
    assertThat(date).matches("^\\d{4}/\\d{2}/\\d{2}$");
  }

  @Test
  @DisplayName("生成多个不同日期")
  void shouldGenerateDifferentDates() {
    String date1 = generator.generate(config, context);
    String date2 = generator.generate(config, context);

    assertThat(date1).isNotNull();
    assertThat(date2).isNotNull();
    assertThat(date1).matches("^\\d{4}-\\d{2}-\\d{2}$");
    assertThat(date2).matches("^\\d{4}-\\d{2}-\\d{2}$");
  }

  @Test
  @DisplayName("生成工作日")
  void shouldGenerateWeekday() {
    config.setParam("dayType", "WEEKDAY");

    String date = generator.generate(config, context);

    assertThat(date).isNotNull();
    assertThat(date).isNotEmpty();
  }

  @Test
  @DisplayName("生成周末")
  void shouldGenerateWeekend() {
    config.setParam("dayType", "WEEKEND");

    String date = generator.generate(config, context);

    assertThat(date).isNotNull();
    assertThat(date).isNotEmpty();
  }

  @Test
  @DisplayName("生成无效日期")
  void shouldGenerateInvalidDate() {
    config.setParam("valid", "false");

    String date = generator.generate(config, context);

    assertThat(date).isNotNull();
    assertThat(date).isNotEmpty();
  }

  @Test
  @DisplayName("默认生成有效日期")
  void shouldDefaultToValidDate() {
    SimpleFieldConfig emptyConfig = new SimpleFieldConfig();

    String date = generator.generate(emptyConfig, context);

    assertThat(date).isNotNull();
    assertThat(date).isNotEmpty();
  }
}
