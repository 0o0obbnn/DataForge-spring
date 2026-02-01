package com.dataforge.io;

/**
 * SQL 方言枚举，定义不同数据库的标识符转义规则。
 *
 * <p>支持的数据库及其标识符转义规则：
 * <ul>
 *   <li>MySQL / MariaDB: 使用反引号 ` 标识符</li>
 *   <li>PostgreSQL: 使用双引号 " 标识符</li>
 *   <li>SQL Server: 使用方括号 [ ] 标识符</li>
 *   <li>Oracle: 不支持标识符转义，依赖白名单验证</li>
 *   <li>H2: 使用双引号 " 标识符</li>
 *   <li>SQLite: 使用双引号 " 标识符</li>
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public enum SqlDialect {

  /** MySQL 数据库，使用反引号转义标识符 */
  MYSQL("MySQL", "`", "`"),

  /** MariaDB 数据库，使用反引号转义标识符 */
  MARIADB("MariaDB", "`", "`"),

  /** PostgreSQL 数据库，使用双引号转义标识符 */
  POSTGRESQL("PostgreSQL", "\"", "\""),

  /** SQL Server 数据库，使用方括号转义标识符 */
  SQL_SERVER("SQL Server", "[", "]"),

  /** Oracle 数据库，不支持标识符转义 */
  ORACLE("Oracle", "", ""),

  /** H2 数据库，使用双引号转义标识符 */
  H2("H2", "\"", "\""),

  /** SQLite 数据库，使用双引号转义标识符 */
  SQLITE("SQLite", "\"", "\"");

  private final String displayName;
  private final String quoteStart;
  private final String quoteEnd;

  /**
   * 构造函数。
   *
   * @param displayName 显示名称
   * @param quoteStart 起始引号
   * @param quoteEnd 结束引号
   */
  SqlDialect(String displayName, String quoteStart, String quoteEnd) {
    this.displayName = displayName;
    this.quoteStart = quoteStart;
    this.quoteEnd = quoteEnd;
  }

  /**
   * 获取显示名称。
   *
   * @return 显示名称
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * 获取起始引号。
   *
   * @return 起始引号
   */
  public String getQuoteStart() {
    return quoteStart;
  }

  /**
   * 获取结束引号。
   *
   * @return 结束引号
   */
  public String getQuoteEnd() {
    return quoteEnd;
  }

  /**
   * 转义标识符。
   *
   * @param identifier 标识符
   * @return 转义后的标识符
   */
  public String escapeIdentifier(String identifier) {
    if (quoteStart.isEmpty()) {
      // Oracle 不支持标识符转义，直接返回
      return identifier;
    }

    // 转义标识符内部的引号
    String escapedIdentifier = identifier.replace(quoteStart, quoteStart + quoteStart);
    return quoteStart + escapedIdentifier + quoteEnd;
  }

  /**
   * 是否支持标识符转义。
   *
   * @return true 如果支持标识符转义
   */
  public boolean supportsIdentifierQuoting() {
    return !quoteStart.isEmpty();
  }
}
