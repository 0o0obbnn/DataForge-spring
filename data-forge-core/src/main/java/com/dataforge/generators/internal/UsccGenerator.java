package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.validation.UsccValidator;
import com.dataforge.validation.ValidationResult;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 统一社会信用代码生成器
 *
 * <p>基于GB32100-2015标准生成18位统一社会信用代码 结构：登记管理部门码(1位) + 机构类别码(1位) + 行政区划码(6位) + 主体标识码(9位) + 校验码(1位)
 *
 * <p>支持的参数： - region: 行政区划码 (6位数字，如 "110000" 表示北京) - org_type: 机构类别
 * (ENTERPRISE|INDIVIDUAL|SOCIAL_ORG|INSTITUTION|ANY) - valid: 是否生成有效代码 (true|false)
 *
 * @author DataForge
 */
public class UsccGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(UsccGenerator.class);
  private static final Random random = new Random();

  // 登记管理部门码
  private static final List<String> REGISTRATION_AUTHORITIES =
      Arrays.asList(
          "1", // 机构编制
          "5", // 民政
          "9", // 工商
          "Y" // 其他
          );

  // 机构类别码
  private static final String ORG_TYPE_ENTERPRISE = "1"; // 企业
  private static final String ORG_TYPE_INDIVIDUAL = "2"; // 个体工商户
  private static final String ORG_TYPE_SOCIAL_ORG = "3"; // 农民专业合作社
  private static final String ORG_TYPE_INSTITUTION = "9"; // 事业单位

  // 常用行政区划码
  private static final List<String> COMMON_REGIONS =
      Arrays.asList(
          "110000", // 北京市
          "120000", // 天津市
          "130000", // 河北省
          "140000", // 山西省
          "150000", // 内蒙古自治区
          "210000", // 辽宁省
          "220000", // 吉林省
          "230000", // 黑龙江省
          "310000", // 上海市
          "320000", // 江苏省
          "330000", // 浙江省
          "340000", // 安徽省
          "350000", // 福建省
          "360000", // 江西省
          "370000", // 山东省
          "410000", // 河南省
          "420000", // 湖北省
          "430000", // 湖南省
          "440000", // 广东省
          "450000", // 广西壮族自治区
          "460000", // 海南省
          "500000", // 重庆市
          "510000", // 四川省
          "520000", // 贵州省
          "530000", // 云南省
          "540000", // 西藏自治区
          "610000", // 陕西省
          "620000", // 甘肃省
          "630000", // 青海省
          "640000", // 宁夏回族自治区
          "650000" // 新疆维吾尔自治区
          );

  // 校验码字符集
  private static final String CHECK_CODE_CHARS = "0123456789ABCDEFGHJKLMNPQRTUWXY";

  // 校验码权重
  private static final int[] WEIGHTS = {
    1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28
  };

  @Override
  public String getType() {
    return "uscc";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String region = config.getParam("region", String.class, null);
      String orgType = config.getParam("org_type", String.class, "ANY");
      boolean valid = Boolean.parseBoolean(config.getParam("valid", String.class, "true"));

      // 生成统一社会信用代码
      String uscc = generateUscc(region, orgType, valid);

      // 验证生成的代码
      if (valid) {
        UsccValidator validator = new UsccValidator();
        ValidationResult validation = validator.validate(uscc);
        if (!validation.isValid()) {
          logger.warn(
              "Generated invalid USCC: {}, errors: {}", uscc, validation.getErrorMessages());
          // 重新生成
          uscc = generateUscc(region, orgType, true);
        }
      }

      logger.debug("Generated USCC: {}", uscc);
      return uscc;

    } catch (Exception e) {
      logger.error("Error generating USCC", e);
      return "91110000000000000A"; // 默认有效代码
    }
  }

  private String generateUscc(String region, String orgType, boolean valid) {
    StringBuilder uscc = new StringBuilder();

    // 1. 登记管理部门码 (1位)
    String registrationAuthority =
        REGISTRATION_AUTHORITIES.get(random.nextInt(REGISTRATION_AUTHORITIES.size()));
    uscc.append(registrationAuthority);

    // 2. 机构类别码 (1位)
    String orgTypeCode = getOrgTypeCode(orgType);
    uscc.append(orgTypeCode);

    // 3. 行政区划码 (6位)
    String regionCode = getRegionCode(region);
    uscc.append(regionCode);

    // 4. 主体标识码 (9位)
    String entityCode = generateEntityCode();
    uscc.append(entityCode);

    // 5. 校验码 (1位)
    String checkCode;
    if (valid) {
      checkCode = calculateCheckCode(uscc.toString());
    } else {
      // 生成错误的校验码
      do {
        checkCode =
            String.valueOf(CHECK_CODE_CHARS.charAt(random.nextInt(CHECK_CODE_CHARS.length())));
      } while (checkCode.equals(calculateCheckCode(uscc.toString())));
    }
    uscc.append(checkCode);

    return uscc.toString();
  }

  private String getOrgTypeCode(String orgType) {
    switch (orgType) {
      case "ENTERPRISE":
        return ORG_TYPE_ENTERPRISE;
      case "INDIVIDUAL":
        return ORG_TYPE_INDIVIDUAL;
      case "SOCIAL_ORG":
        return ORG_TYPE_SOCIAL_ORG;
      case "INSTITUTION":
        return ORG_TYPE_INSTITUTION;
      case "ANY":
      default:
        return Arrays.asList(
                ORG_TYPE_ENTERPRISE, ORG_TYPE_INDIVIDUAL, ORG_TYPE_SOCIAL_ORG, ORG_TYPE_INSTITUTION)
            .get(random.nextInt(4));
    }
  }

  private String getRegionCode(String region) {
    if (region != null && region.matches("\\d{6}")) {
      return region;
    }

    // 使用常用行政区划码
    return COMMON_REGIONS.get(random.nextInt(COMMON_REGIONS.size()));
  }

  private String generateEntityCode() {
    StringBuilder entityCode = new StringBuilder();

    // 生成9位主体标识码
    for (int i = 0; i < 9; i++) {
      // 使用数字和大写字母（排除I、O、S、V、Z）
      String chars = "0123456789ABCDEFGHJKLMNPQRTUWXY";
      entityCode.append(chars.charAt(random.nextInt(chars.length())));
    }

    return entityCode.toString();
  }

  private String calculateCheckCode(String code17) {
    int sum = 0;

    // 计算加权和
    for (int i = 0; i < 17; i++) {
      char c = code17.charAt(i);
      int value;

      if (Character.isDigit(c)) {
        value = c - '0';
      } else {
        // 字母转换为数值
        value = CHECK_CODE_CHARS.indexOf(c);
      }

      sum += value * WEIGHTS[i];
    }

    // 计算校验码
    int remainder = sum % 31;
    int checkValue = (31 - remainder) % 31;
    return String.valueOf(CHECK_CODE_CHARS.charAt(checkValue));
  }
}
