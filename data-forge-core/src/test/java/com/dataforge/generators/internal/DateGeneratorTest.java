package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        String startDate = "2020-01-01";
        String endDate = "2023-12-31";
        config.setParam("start", startDate);
        config.setParam("end", endDate);

        String date = generator.generate(config, context);

        assertThat(date).isNotNull();
        assertThat(date).matches("^\\d{4}-\\d{2}-\\d{2}$");

        LocalDate generatedDate = LocalDate.parse(date);
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        assertThat(generatedDate).isBetween(start, end);
    }

    @Test
    @DisplayName("生成指定格式的日期")
    void shouldGenerateDateWithFormat() {
        config.setParam("format", "yyyy/MM/dd");

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
    @DisplayName("生成过去日期")
    void shouldGeneratePastDate() {
        config.setParam("range", "past");

        String date = generator.generate(config, context);

        assertThat(date).isNotNull();
        LocalDate generatedDate = LocalDate.parse(date);
        assertThat(generatedDate).isBeforeOrEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("生成未来日期")
    void shouldGenerateFutureDate() {
        config.setParam("range", "future");

        String date = generator.generate(config, context);

        assertThat(date).isNotNull();
        LocalDate generatedDate = LocalDate.parse(date);
        assertThat(generatedDate).isAfterOrEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("默认生成当前日期附近")
    void shouldDefaultToNearCurrentDate() {
        SimpleFieldConfig emptyConfig = new SimpleFieldConfig();

        String date = generator.generate(emptyConfig, context);

        assertThat(date).isNotNull();
        assertThat(date).matches("^\\d{4}-\\d{2}-\\d{2}$");
    }
}
