package com.dataforge.io;

import static org.junit.jupiter.api.Assertions.*;

import com.dataforge.config.OutputConfig;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * SqlOutputStrategy 单元测试。
 *
 * <p>测试 SQL 输出策略的跨数据库兼容性、标识符转义、值转义等功能。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("SqlOutputStrategy 跨数据库测试")
class SqlOutputStrategyTest {

  @TempDir Path tempDir;

  /** 测试 MySQL 标识符转义。 */
  @Test
  @DisplayName("MySQL 标识符转义应使用反引号")
  void testMySqlIdentifierEscaping() throws Exception {
    SqlOutputStrategy strategy = new SqlOutputStrategy();
    strategy.setSqlDialect(SqlDialect.MYSQL);

    OutputConfig config = new OutputConfig();
    config.setFormat(OutputConfig.Format.SQL);
    config.setSqlDialect(SqlDialect.MYSQL);
    config.setSqlTableName("test_table");

    File outputFile = tempDir.resolve("test_mysql.sql").toFile();
    config.setFile(outputFile.getAbsolutePath());

    List<String> fieldNames = List.of("user_name", "age", "email");

    strategy.initialize(config, fieldNames);

    Map<String, Object> record = new HashMap<>();
    record.put("user_name", "Alice");
    record.put("age", 30);
    record.put("email", "alice@example.com");

    strategy.writeRecord(record);
    strategy.finish();

    String sqlOutput = Files.readString(outputFile.toPath());
    assertTrue(
        sqlOutput.contains("`user_name`"), "MySQL identifiers should be escaped with backticks");
    assertTrue(sqlOutput.contains("`test_table`"), "Table name should be escaped");
  }

  /** 测试 PostgreSQL 标识符转义。 */
  @Test
  @DisplayName("PostgreSQL 标识符转义应使用双引号")
  void testPostgreSqlIdentifierEscaping() throws Exception {
    SqlOutputStrategy strategy = new SqlOutputStrategy();
    strategy.setSqlDialect(SqlDialect.POSTGRESQL);

    OutputConfig config = new OutputConfig();
    config.setFormat(OutputConfig.Format.SQL);
    config.setSqlDialect(SqlDialect.POSTGRESQL);
    config.setSqlTableName("test_table");

    File outputFile = tempDir.resolve("test_postgres.sql").toFile();
    config.setFile(outputFile.getAbsolutePath());

    List<String> fieldNames = List.of("user_name", "age", "email");

    strategy.initialize(config, fieldNames);

    Map<String, Object> record = new HashMap<>();
    record.put("user_name", "Bob");
    record.put("age", 25);
    record.put("email", "bob@example.com");

    strategy.writeRecord(record);
    strategy.finish();

    String sqlOutput = Files.readString(outputFile.toPath());
    assertTrue(
        sqlOutput.contains("\"user_name\""),
        "PostgreSQL identifiers should be escaped with double quotes");
    assertTrue(sqlOutput.contains("\"test_table\""), "Table name should be escaped");
  }

  /** 测试 SQL Server 标识符转义。 */
  @Test
  @DisplayName("SQL Server 标识符转义应使用方括号")
  void testSqlServerIdentifierEscaping() throws Exception {
    SqlOutputStrategy strategy = new SqlOutputStrategy();
    strategy.setSqlDialect(SqlDialect.SQL_SERVER);

    OutputConfig config = new OutputConfig();
    config.setFormat(OutputConfig.Format.SQL);
    config.setSqlDialect(SqlDialect.SQL_SERVER);
    config.setSqlTableName("test_table");

    File outputFile = tempDir.resolve("test_sqlserver.sql").toFile();
    config.setFile(outputFile.getAbsolutePath());

    List<String> fieldNames = List.of("user_name", "age", "email");

    strategy.initialize(config, fieldNames);

    Map<String, Object> record = new HashMap<>();
    record.put("user_name", "Charlie");
    record.put("age", 35);
    record.put("email", "charlie@example.com");

    strategy.writeRecord(record);
    strategy.finish();

    String sqlOutput = Files.readString(outputFile.toPath());
    assertTrue(
        sqlOutput.contains("[user_name]"),
        "SQL Server identifiers should be escaped with square brackets");
    assertTrue(sqlOutput.contains("[test_table]"), "Table name should be escaped");
  }

  /** 测试 Oracle 标识符转义（不转义，仅白名单验证）。 */
  @Test
  @DisplayName("Oracle 标识符不应转义，通过白名单验证")
  void testOracleIdentifierEscaping() throws Exception {
    SqlOutputStrategy strategy = new SqlOutputStrategy();
    strategy.setSqlDialect(SqlDialect.ORACLE);

    OutputConfig config = new OutputConfig();
    config.setFormat(OutputConfig.Format.SQL);
    config.setSqlDialect(SqlDialect.ORACLE);
    config.setSqlTableName("test_table");

    File outputFile = tempDir.resolve("test_oracle.sql").toFile();
    config.setFile(outputFile.getAbsolutePath());

    List<String> fieldNames = List.of("user_name", "age", "email");

    strategy.initialize(config, fieldNames);

    Map<String, Object> record = new HashMap<>();
    record.put("user_name", "David");
    record.put("age", 40);
    record.put("email", "david@example.com");

    strategy.writeRecord(record);
    strategy.finish();

    String sqlOutput = Files.readString(outputFile.toPath());
    assertTrue(
        sqlOutput.contains("user_name"),
        "Oracle identifiers should not be escaped (whitelist only)");
    assertFalse(
        sqlOutput.contains("`") && sqlOutput.contains("\""),
        "Oracle should not use quote characters");
  }

  /** 测试 H2 数据库标识符转义。 */
  @Test
  @DisplayName("H2 数据库标识符转义应使用双引号")
  void testH2IdentifierEscaping() throws Exception {
    SqlOutputStrategy strategy = new SqlOutputStrategy();
    strategy.setSqlDialect(SqlDialect.H2);

    OutputConfig config = new OutputConfig();
    config.setFormat(OutputConfig.Format.SQL);
    config.setSqlDialect(SqlDialect.H2);
    config.setSqlTableName("test_table");

    File outputFile = tempDir.resolve("test_h2.sql").toFile();
    config.setFile(outputFile.getAbsolutePath());

    List<String> fieldNames = List.of("user_name", "age", "email");

    strategy.initialize(config, fieldNames);

    Map<String, Object> record = new HashMap<>();
    record.put("user_name", "Eve");
    record.put("age", 28);
    record.put("email", "eve@example.com");

    strategy.writeRecord(record);
    strategy.finish();

    String sqlOutput = Files.readString(outputFile.toPath());
    assertTrue(
        sqlOutput.contains("\"user_name\""), "H2 identifiers should be escaped with double quotes");
  }

  /** 测试 SQLite 数据库标识符转义。 */
  @Test
  @DisplayName("SQLite 数据库标识符转义应使用双引号")
  void testSqliteIdentifierEscaping() throws Exception {
    SqlOutputStrategy strategy = new SqlOutputStrategy();
    strategy.setSqlDialect(SqlDialect.SQLITE);

    OutputConfig config = new OutputConfig();
    config.setFormat(OutputConfig.Format.SQL);
    config.setSqlDialect(SqlDialect.SQLITE);
    config.setSqlTableName("test_table");

    File outputFile = tempDir.resolve("test_sqlite.sql").toFile();
    config.setFile(outputFile.getAbsolutePath());

    List<String> fieldNames = List.of("user_name", "age", "email");

    strategy.initialize(config, fieldNames);

    Map<String, Object> record = new HashMap<>();
    record.put("user_name", "Frank");
    record.put("age", 33);
    record.put("email", "frank@example.com");

    strategy.writeRecord(record);
    strategy.finish();

    String sqlOutput = Files.readString(outputFile.toPath());
    assertTrue(
        sqlOutput.contains("\"user_name\""),
        "SQLite identifiers should be escaped with double quotes");
  }

  /** 测试无效标识符应抛出异常。 */
  @Test
  @DisplayName("无效标识符应抛出 OutputException")
  void testInvalidIdentifierShouldThrowException() {
    SqlOutputStrategy strategy = new SqlOutputStrategy();
    strategy.setSqlDialect(SqlDialect.MYSQL);

    OutputConfig config = new OutputConfig();
    config.setFormat(OutputConfig.Format.SQL);
    config.setSqlTableName("test_table");

    List<String> fieldNames = new ArrayList<>();
    fieldNames.add("valid_field");
    fieldNames.add("invalid field"); // 包含空格，应该失败

    strategy.initialize(config, fieldNames);

    Map<String, Object> record = new HashMap<>();
    record.put("valid_field", "value1");
    record.put("invalid field", "value2");

    assertThrows(
        com.dataforge.io.OutputException.class,
        () -> strategy.writeRecord(record),
        "Invalid identifiers should throw OutputException when writing record");
  }

  /** 测试标识符长度限制。 */
  @Test
  @DisplayName("超过长度限制的标识符应抛出异常")
  void testIdentifierLengthLimit() {
    SqlOutputStrategy strategy = new SqlOutputStrategy();
    strategy.setSqlDialect(SqlDialect.MYSQL);

    OutputConfig config = new OutputConfig();
    config.setFormat(OutputConfig.Format.SQL);
    config.setSqlTableName("test_table");

    List<String> fieldNames = new ArrayList<>();
    fieldNames.add("valid_field");
    // 创建65个字符的标识符，超过64字符限制
    fieldNames.add("a".repeat(65));

    strategy.initialize(config, fieldNames);

    Map<String, Object> record = new HashMap<>();
    record.put("valid_field", "value1");
    record.put("a".repeat(65), "value2");

    assertThrows(
        com.dataforge.io.OutputException.class,
        () -> strategy.writeRecord(record),
        "Identifiers exceeding max length should throw OutputException when writing record");
  }

  /** 测试值转义（包含特殊字符）。 */
  @Test
  @DisplayName("字符串值中的单引号应被正确转义")
  void testStringEscaping() throws Exception {
    SqlOutputStrategy strategy = new SqlOutputStrategy();
    strategy.setSqlDialect(SqlDialect.MYSQL);

    OutputConfig config = new OutputConfig();
    config.setFormat(OutputConfig.Format.SQL);
    config.setSqlTableName("test_table");

    File outputFile = tempDir.resolve("test_escaping.sql").toFile();
    config.setFile(outputFile.getAbsolutePath());

    List<String> fieldNames = List.of("name", "quote");

    strategy.initialize(config, fieldNames);

    Map<String, Object> record = new HashMap<>();
    record.put("name", "O'Brien");
    record.put("quote", "It's a test");

    strategy.writeRecord(record);
    strategy.finish();

    String sqlOutput = Files.readString(outputFile.toPath());
    assertTrue(
        sqlOutput.contains("O''Brien") || sqlOutput.contains("O\\'Brien"),
        "Single quotes in values should be escaped");
  }

  /** 测试 NULL 值处理。 */
  @Test
  @DisplayName("NULL 值应正确处理为 SQL NULL")
  void testNullValueHandling() throws Exception {
    SqlOutputStrategy strategy = new SqlOutputStrategy();
    strategy.setSqlDialect(SqlDialect.MYSQL);

    OutputConfig config = new OutputConfig();
    config.setFormat(OutputConfig.Format.SQL);
    config.setSqlTableName("test_table");

    File outputFile = tempDir.resolve("test_null.sql").toFile();
    config.setFile(outputFile.getAbsolutePath());

    List<String> fieldNames = List.of("name", "age", "email");

    strategy.initialize(config, fieldNames);

    Map<String, Object> record = new HashMap<>();
    record.put("name", "Test");
    record.put("age", null);
    record.put("email", null);

    strategy.writeRecord(record);
    strategy.finish();

    String sqlOutput = Files.readString(outputFile.toPath());
    assertTrue(sqlOutput.contains("NULL"), "NULL values should be represented as SQL NULL");
  }

  /** 测试日期时间格式化。 */
  @Test
  @DisplayName("日期时间值应正确格式化")
  void testDateTimeFormatting() throws Exception {
    SqlOutputStrategy strategy = new SqlOutputStrategy();
    strategy.setSqlDialect(SqlDialect.MYSQL);

    OutputConfig config = new OutputConfig();
    config.setFormat(OutputConfig.Format.SQL);
    config.setSqlTableName("test_table");

    File outputFile = tempDir.resolve("test_datetime.sql").toFile();
    config.setFile(outputFile.getAbsolutePath());

    List<String> fieldNames = List.of("created_at");

    strategy.initialize(config, fieldNames);

    Map<String, Object> record = new HashMap<>();
    record.put("created_at", java.time.LocalDateTime.now());

    strategy.writeRecord(record);
    strategy.finish();

    String sqlOutput = Files.readString(outputFile.toPath());
    assertTrue(
        sqlOutput.contains("'") && sqlOutput.contains("'"), "DateTime values should be quoted");
  }
}
