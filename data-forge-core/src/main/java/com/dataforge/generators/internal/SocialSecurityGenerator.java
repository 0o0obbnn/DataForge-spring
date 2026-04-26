package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 社保/医保号生成器
 *
 * <p>支持生成社保号和医保号，用于人力资源系统、医疗系统等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>type: 类型 (SOCIAL|MEDICAL|BOTH) 默认: SOCIAL
 *   <li>link_idcard: 是否关联身份证号 默认: true
 *   <li>region: 地区代码
 *   <li>length: 号码长度 默认: 18
 *   <li>prefix: 自定义前缀
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class SocialSecurityGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(SocialSecurityGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  @Override
  public String getType() {
    return "social_security";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String type = getStringParam(config, "type", "SOCIAL");
      boolean linkIdCard = getBooleanParam(config, "link_idcard", true);

      // 尝试从上下文获取身份证号
      if (linkIdCard) {
        Optional<String> idCard = context.get("idcard", String.class);
        if (idCard.isPresent()) {
          return generateFromIdCard(idCard.get(), type);
        }
      }

      // 独立生成
      return generateIndependent(config, type);

    } catch (Exception e) {
      logger.error("Failed to generate social security number", e);
      return "110000" + System.currentTimeMillis() % 1000000000000L;
    }
  }

  private String generateFromIdCard(String idCard, String type) {
    if (idCard.length() >= 18) {
      // 中国社保/医保号通常就是身份证号
      return idCard;
    }

    // 如果身份证号不完整，补充生成
    StringBuilder sb = new StringBuilder(idCard);
    while (sb.length() < 18) {
      sb.append(random.nextInt(10));
    }
    return sb.toString();
  }

  private String generateIndependent(FieldConfig config, String type) {
    StringBuilder number = new StringBuilder();

    // 前缀处理
    String prefix = getStringParam(config, "prefix", null);
    if (prefix != null) {
      number.append(prefix);
    } else {
      // 地区代码（6位）
      String region = getStringParam(config, "region", null);
      if (region != null) {
        number.append(region);
      } else {
        // 默认地区代码
        number.append(String.format("%06d", 110000 + random.nextInt(900000)));
      }
    }

    // 生成剩余部分
    int length = getIntParam(config, "length", 18);
    int remainingLength = length - number.length();

    for (int i = 0; i < remainingLength; i++) {
      number.append(random.nextInt(10));
    }

    return number.toString();
  }
}
