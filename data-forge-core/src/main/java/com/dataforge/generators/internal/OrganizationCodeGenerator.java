package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.validation.OrganizationCodeValidator;
import com.dataforge.validation.ValidationResult;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 组织机构代码生成器
 *
 * <p>基于GB 11714-1997标准生成9位组织机构代码 结构：8位主体代码 + 1位校验码
 *
 * <p>支持的参数： - valid: 是否生成有效代码 (true|false) - prefix: 代码前缀 (8位以内的字符串)
 *
 * <p>注意：组织机构代码已被统一社会信用代码替代，主要用于历史数据兼容
 *
 * @author DataForge
 */
public class OrganizationCodeGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(OrganizationCodeGenerator.class);
  private static final Random random = new Random();

  // 组织机构代码字符集（数字和大写字母，排除I、O、S、V、Z）
  private static final String CODE_CHARS = "0123456789ABCDEFGHJKLMNPQRTUWXY";

  // 校验码权重
  private static final int[] WEIGHTS = {3, 7, 9, 10, 5, 8, 4, 2};

  @Override
  public String getType() {
    return "orgcode";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      boolean valid = Boolean.parseBoolean(config.getParam("valid", String.class, "true"));
      String prefix = config.getParam("prefix", String.class, null);

      // 生成组织机构代码
      String orgCode = generateOrgCode(prefix, valid);

      // 验证生成的代码
      if (valid) {
        OrganizationCodeValidator validator = new OrganizationCodeValidator();
        ValidationResult validation = validator.validate(orgCode);
        if (!validation.isValid()) {
          logger.warn(
              "Generated invalid organization code: {}, errors: {}",
              orgCode,
              validation.getErrorMessages());
          // 重新生成
          orgCode = generateOrgCode(prefix, true);
        }
      }

      logger.debug("Generated organization code: {}", orgCode);
      return orgCode;

    } catch (Exception e) {
      logger.error("Error generating organization code", e);
      return "12345678-9"; // 默认有效代码
    }
  }

  private String generateOrgCode(String prefix, boolean valid) {
    StringBuilder code = new StringBuilder();

    // 1. 生成8位主体代码
    String mainCode = generateMainCode(prefix);
    code.append(mainCode);

    // 2. 添加分隔符
    code.append("-");

    // 3. 生成校验码
    String checkCode;
    if (valid) {
      checkCode = calculateCheckCode(mainCode);
    } else {
      // 生成错误的校验码
      do {
        checkCode = String.valueOf(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
      } while (checkCode.equals(calculateCheckCode(mainCode)));
    }
    code.append(checkCode);

    return code.toString();
  }

  private String generateMainCode(String prefix) {
    StringBuilder mainCode = new StringBuilder();

    // 如果有前缀，使用前缀
    if (prefix != null && !prefix.isEmpty()) {
      // 确保前缀只包含有效字符
      String validPrefix = prefix.toUpperCase().replaceAll("[^0-9A-Z]", "");
      validPrefix = validPrefix.replaceAll("[IOSVZ]", ""); // 排除无效字符

      if (validPrefix.length() > 8) {
        validPrefix = validPrefix.substring(0, 8);
      }

      mainCode.append(validPrefix);
    }

    // 补充剩余位数
    while (mainCode.length() < 8) {
      char c = CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length()));
      mainCode.append(c);
    }

    return mainCode.toString();
  }

  private String calculateCheckCode(String mainCode) {
    int sum = 0;

    // 计算加权和
    for (int i = 0; i < 8; i++) {
      char c = mainCode.charAt(i);
      int value;

      if (Character.isDigit(c)) {
        value = c - '0';
      } else {
        // 字母转换为数值
        value = CODE_CHARS.indexOf(c);
      }

      sum += value * WEIGHTS[i];
    }

    // 计算校验码
    int remainder = 11 - (sum % 11);

    if (remainder == 10) {
      return "X";
    } else if (remainder == 11) {
      return "0";
    } else {
      return String.valueOf(remainder);
    }
  }
}
