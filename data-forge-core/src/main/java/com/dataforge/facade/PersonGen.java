package com.dataforge.facade;

import java.util.List;
import java.util.Map;

/**
 * 个人信息生成器门面
 *
 * <p>提供个人相关数据的生成方法
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class PersonGen {

  private final DataGen gen;

  public PersonGen(DataGen gen) {
    this.gen = gen;
  }

  /** 生成完整姓名 */
  public String fullName() {
    return gen.generate("name");
  }

  /** 生成中文姓名 */
  public String chineseName() {
    return gen.generate("name", Map.of("type", "CN"));
  }

  /** 生成中文姓名（指定性别） */
  public String chineseName(String gender) {
    return gen.generate("name", Map.of("type", "CN", "gender", gender));
  }

  /** 生成英文姓名 */
  public String englishName() {
    return gen.generate("name", Map.of("type", "EN"));
  }

  /** 生成英文姓名（指定性别） */
  public String englishName(String gender) {
    return gen.generate("name", Map.of("type", "EN", "gender", gender));
  }

  /** 生成姓氏 */
  public String lastName() {
    return gen.generate("name", Map.of("part", "LAST"));
  }

  /** 生成名字 */
  public String firstName() {
    return gen.generate("name", Map.of("part", "FIRST"));
  }

  /** 生成身份证号 */
  public String idCard() {
    return gen.generate("idcard");
  }

  /** 生成身份证号（指定地区） */
  public String idCard(String region) {
    return gen.generate("idcard", Map.of("region", region));
  }

  /** 生成电话号码 */
  public String phone() {
    return gen.generate("phone");
  }

  /** 生成电话号码（指定国家） */
  public String phone(String country) {
    return gen.generate("phone", Map.of("country", country));
  }

  /** 生成手机号码 */
  public String mobile() {
    return gen.generate("phone", Map.of("type", "MOBILE"));
  }

  /** 生成年龄 */
  public Integer age() {
    return Integer.parseInt(gen.generate("random_number", Map.of("min", 1, "max", 100)));
  }

  /** 生成年龄（指定范围） */
  public Integer age(int min, int max) {
    if (min < 0 || max < 0) {
      throw new IllegalArgumentException(
          "Age range must be non-negative: min=" + min + ", max=" + max);
    }
    if (min > max) {
      throw new IllegalArgumentException(
          "Min age must not exceed max: min=" + min + ", max=" + max);
    }
    return Integer.parseInt(gen.generate("random_number", Map.of("min", min, "max", max)));
  }

  /** 生成性别 */
  public String gender() {
    return gen.generate("enum", Map.of("values", "男,女"));
  }

  /** 批量生成姓名 */
  public List<String> fullNames(int count) {
    return gen.generateList("name", count);
  }

  /** 批量生成身份证号 */
  public List<String> idCards(int count) {
    return gen.generateList("idcard", count);
  }

  /** 批量生成电话号码 */
  public List<String> phones(int count) {
    return gen.generateList("phone", count);
  }
}
