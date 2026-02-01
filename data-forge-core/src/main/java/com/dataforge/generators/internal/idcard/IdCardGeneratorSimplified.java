package com.dataforge.generators.internal.idcard;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.internal.BaseGenerator;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 身份证号码生成器（简化版）。
 *
 * <p>使用拆分后的组件，职责更加单一： - {@link IdCardRegionService} 负责地区代码管理 - {@link IdCardValidationHelper}
 * 负责校验逻辑
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class IdCardGeneratorSimplified extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(IdCardGeneratorSimplified.class);

  @Autowired private IdCardRegionService regionService;

  @Override
  public String getType() {
    return "idcard";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String region = getStringParam(config, "region", null);
      String birthDateRange = getStringParam(config, "birth_date_range", "1980-01-01,2000-12-31");
      String gender = getStringParam(config, "gender", "ANY");
      boolean valid = getBooleanParam(config, "valid", true);

      if (!valid) {
        return generateInvalidIdCard();
      }

      String idCard = generateValidIdCard(region, birthDateRange, gender);
      putIdCardInfoToContext(context, idCard);

      return idCard;

    } catch (Exception e) {
      logger.error("Failed to generate ID card number", e);
      return "11010119800101001X";
    }
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  /** 生成有效的身份证号码。 */
  private String generateValidIdCard(String region, String birthDateRange, String gender) {
    // 1. 生成地区代码（前6位）
    String regionCode = regionService.selectRegionCode(region);

    // 2. 生成出生日期（第7-14位）
    String birthDate = IdCardValidationHelper.generateBirthDate(birthDateRange);

    // 3. 生成顺序码（第15-17位）
    String sequenceCode = IdCardValidationHelper.generateSequenceCode(gender);

    // 4. 计算校验码（第18位）
    String prefix = regionCode + birthDate + sequenceCode;
    String checkDigit = IdCardValidationHelper.calculateCheckDigit(prefix);

    return prefix + checkDigit;
  }

  /** 生成无效的身份证号码。 */
  private String generateInvalidIdCard() {
    // 生成一个明显无效的号码（校验码错误）
    String prefix = "11010119800101001";
    String wrongCheckDigit = "0"; // 故意使用错误的校验码
    return prefix + wrongCheckDigit;
  }

  /** 将身份证信息放入上下文。 */
  private void putIdCardInfoToContext(DataForgeContext context, String idCard) {
    context.put("idCard", idCard);

    // 提取并放入出生日期
    String birthDate = IdCardValidationHelper.extractBirthDate(idCard);
    if (birthDate != null) {
      context.put("birthDate", birthDate);
    }

    // 计算并放入年龄
    int age = IdCardValidationHelper.calculateAge(birthDate);
    context.put("age", age);

    // 提取并放入性别
    String gender = IdCardValidationHelper.extractGender(idCard);
    if (gender != null) {
      context.put("gender", gender);
    }

    // 提取并放入地区代码
    String regionCode = IdCardValidationHelper.extractRegionCode(idCard);
    if (regionCode != null) {
      context.put("regionCode", regionCode);

      // 获取并放入地区详细信息
      IdCardRegionService.RegionInfo regionInfo = regionService.getRegionInfo(regionCode);
      if (regionInfo != null) {
        context.put("province", regionInfo.province);
        context.put("city", regionInfo.city);
        context.put("district", regionInfo.district);
      }
    }
  }
}
