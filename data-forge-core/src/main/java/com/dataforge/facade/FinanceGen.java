package com.dataforge.facade;

import java.util.List;
import java.util.Map;

/**
 * 金融数据生成器门面
 *
 * <p>提供金融相关数据的生成方法
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class FinanceGen {

  private final DataGen gen;

  public FinanceGen(DataGen gen) {
    this.gen = gen;
  }

  /** 生成银行卡号 */
  public String bankCard() {
    return gen.generate("bankcard");
  }

  /** 生成银行卡号（指定银行） */
  public String bankCard(String bank) {
    return gen.generate("bankcard", Map.of("bank", bank));
  }

  /** 生成信用卡号 */
  public String creditCard() {
    return gen.generate("bankcard", Map.of("type", "CREDIT"));
  }

  /** 生成金额 */
  public Double amount() {
    return Double.parseDouble(
        gen.generate("decimal", Map.of("min", 0.0, "max", 10000.0, "scale", 2)));
  }

  /** 生成金额（指定范围） */
  public Double amount(double min, double max) {
    return Double.parseDouble(gen.generate("decimal", Map.of("min", min, "max", max, "scale", 2)));
  }

  /** 生成货币代码 */
  public String currencyCode() {
    return gen.generate("currency", Map.of("part", "CODE"));
  }

  /** 生成货币名称 */
  public String currencyName() {
    return gen.generate("currency", Map.of("part", "NAME"));
  }

  /** 生成货币符号 */
  public String currencySymbol() {
    return gen.generate("currency", Map.of("part", "SYMBOL"));
  }

  /** 生成 IBAN（国际银行账号） */
  public String iban() {
    return gen.generate("template", Map.of("template", "{{country:2}}{{random:20}}"));
  }

  /** 生成 BIC/SWIFT 代码 */
  public String bic() {
    return gen.generate(
        "template", Map.of("template", "{{alpha:4}}{{country:2}}{{alphanum:2}}{{alphanum:3}}"));
  }

  /** 批量生成银行卡号 */
  public List<String> bankCards(int count) {
    return gen.generateList("bankcard", count);
  }

  /** 批量生成信用卡号 */
  public List<String> creditCards(int count) {
    return gen.generateList("bankcard", count, Map.of("type", "CREDIT"));
  }
}
