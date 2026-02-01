package com.dataforge.io;

import static org.junit.jupiter.api.Assertions.*;

import com.dataforge.config.OutputConfig;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SqlOutputStrategy 单元测试。
 *
 * <p>测试SQL标识符验证、转义和数据库兼容性。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("SqlOutputStrategy 测试")
class SqlOutputStrategyTest {

  private SqlOutputStrategy strategy;
  private Method escapeIdentifierMethod;
  private List<String> fieldNames;
  private OutputConfig config;

  @BeforeEach
  void setUp() throws Exception {
    strategy = new SqlOutputStrategy();
    fieldNames = List.of("id", "name", "email");
    config = new OutputConfig();
    config.setFormat(OutputConfig.Format.SQL);

    // 获取私有方法用于测试
    escapeIdentifierMethod =
        SqlOutputStrategy.class.getDeclaredMethod("escapeIdentifier", String.class);
    escapeIdentifierMethod.setAccessible(true);
  }

  /**
   * 解包反射调用的异常，获取实际的cause。
   */
  private Throwable getCause(Throwable ex) {
    if (ex instanceof InvocationTargetException) {
      return ((InvocationTargetException) ex).getCause();
    }
    return ex;
  }

  @Test
  @DisplayName("有效标识符应通过验证")
  void testValidIdentifiers() throws Exception {
    String[] validIds = {
      "table", "table_name", "_table", "Table123", "a_b_c", "uppercase", "lowercase", "MixEdCase"
    };

    for (String id : validIds) {
      String result = (String) escapeIdentifierMethod.invoke(strategy, id);
      assertNotNull(result, "Result should not be null for: " + id);
      assertTrue(result.startsWith("`"), "Result should start with backtick: " + id);
      assertTrue(result.endsWith("`"), "Result should end with backtick: " + id);
    }
  }

  @Test
  @DisplayName("包含空格的标识符应抛出异常")
  void testInvalidIdentifierWithSpace() {
    String invalidId = "table name";

    Exception ex =
        assertThrows(
            Exception.class,
            () -> escapeIdentifierMethod.invoke(strategy, invalidId));

    Throwable cause = getCause(ex);
    assertTrue(
        cause instanceof OutputException,
        "Should throw OutputException");
    assertTrue(
        cause.getMessage().contains("Invalid SQL identifier"),
        "Error message should mention invalid identifier");
  }

  @Test
  @DisplayName("包含连字符的标识符应抛出异常")
  void testInvalidIdentifierWithHyphen() {
    String[] invalidIds = {"table-name", "my-table", "test-data"};

    for (String invalidId : invalidIds) {
      Exception ex =
          assertThrows(
              Exception.class,
              () -> escapeIdentifierMethod.invoke(strategy, invalidId));

      assertTrue(
          getCause(ex) instanceof OutputException,
          "Should reject identifier with hyphen: " + invalidId);
    }
  }

  @Test
  @DisplayName("包含点的标识符应抛出异常")
  void testInvalidIdentifierWithDot() {
    String[] invalidIds = {"table.name", "schema.table", "db.schema.table"};

    for (String invalidId : invalidIds) {
      Exception ex =
          assertThrows(
              Exception.class,
              () -> escapeIdentifierMethod.invoke(strategy, invalidId));

      assertTrue(
          getCause(ex) instanceof OutputException,
          "Should reject identifier with dot: " + invalidId);
    }
  }

  @Test
  @DisplayName("包含分号的标识符应抛出异常（SQL注入防护）")
  void testInvalidIdentifierWithSemicolon() {
    String[] invalidIds = {
      "table;", "table;drop", "table; DROP TABLE", "users;drop table users"
    };

    for (String invalidId : invalidIds) {
      Exception ex =
          assertThrows(
              Exception.class,
              () -> escapeIdentifierMethod.invoke(strategy, invalidId));

      Throwable cause = getCause(ex);
      assertTrue(
          cause instanceof OutputException,
          "Should reject SQL injection attempt: " + invalidId);
      assertTrue(
          cause.getMessage().contains("Invalid SQL identifier"),
          "Should indicate invalid identifier for: " + invalidId);
    }
  }

  @Test
  @DisplayName("包含单引号的标识符应抛出异常")
  void testInvalidIdentifierWithQuote() {
    String[] invalidIds = {"table'", "table's", "users' passwords"};

    for (String invalidId : invalidIds) {
      Exception ex =
          assertThrows(
              Exception.class,
              () -> escapeIdentifierMethod.invoke(strategy, invalidId));

      assertTrue(
          getCause(ex) instanceof OutputException,
          "Should reject identifier with quote: " + invalidId);
    }
  }

  @Test
  @DisplayName("超长标识符应抛出异常")
  void testTooLongIdentifier() {
    String longId = "a".repeat(65);

    Exception ex =
        assertThrows(
            Exception.class,
            () -> escapeIdentifierMethod.invoke(strategy, longId));

    Throwable cause = getCause(ex);
    assertTrue(
        cause instanceof OutputException,
        "Should throw OutputException for too long identifier");
    assertTrue(
        cause.getMessage().contains("too long"),
        "Error message should mention 'too long'");
    assertTrue(
        cause.getMessage().contains("65"),
        "Error message should show actual length");
    assertTrue(
        cause.getMessage().contains("64"),
        "Error message should show max length");
  }

  @Test
  @DisplayName("最大长度标识符应通过验证")
  void testMaxLengthIdentifier() throws Exception {
    String maxLengthId = "a".repeat(64);

    String result = (String) escapeIdentifierMethod.invoke(strategy, maxLengthId);
    assertNotNull(result);
    assertTrue(result.startsWith("`"));
    assertTrue(result.endsWith("`"));
  }

  @Test
  @DisplayName("数字开头的标识符应抛出异常")
  void testIdentifierStartingWithNumber() {
    String[] invalidIds = {"123table", "9table", "0_table"};

    for (String invalidId : invalidIds) {
      Exception ex =
          assertThrows(
              Exception.class,
              () -> escapeIdentifierMethod.invoke(strategy, invalidId));

      assertTrue(
          getCause(ex) instanceof OutputException,
          "Should reject identifier starting with number: " + invalidId);
    }
  }

  @Test
  @DisplayName("特殊字符开头的标识符应抛出异常")
  void testIdentifierStartingWithSpecialChar() {
    String[] invalidIds = {"@table", "$table", "#table"};

    for (String invalidId : invalidIds) {
      Exception ex =
          assertThrows(
              Exception.class,
              () -> escapeIdentifierMethod.invoke(strategy, invalidId));

      assertTrue(
          getCause(ex) instanceof OutputException,
          "Should reject identifier starting with special char: " + invalidId);
    }
  }

  @Test
  @DisplayName("null标识符应返回null")
  void testNullIdentifier() throws Exception {
    String result = (String) escapeIdentifierMethod.invoke(strategy, (String) null);
    assertNull(result, "Null identifier should return null");
  }

  @Test
  @DisplayName("空标识符应返回空字符串")
  void testEmptyIdentifier() throws Exception {
    String result = (String) escapeIdentifierMethod.invoke(strategy, "");
    assertEquals("", result, "Empty identifier should return empty string");
  }

  @Test
  @DisplayName("包含反引号的标识符应被拒绝（保留字符）")
  void testBacktickInIdentifier() {
    String invalidId = "table`name";

    Exception ex =
        assertThrows(
            Exception.class,
            () -> escapeIdentifierMethod.invoke(strategy, invalidId));

    Throwable cause = getCause(ex);
    assertTrue(
        cause instanceof OutputException,
        "Should reject identifier containing backtick");
    assertTrue(
        cause.getMessage().contains("Invalid SQL identifier"),
        "Should indicate invalid identifier");
  }
}
