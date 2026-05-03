package com.dataforge.facade;

import java.util.List;
import java.util.Map;

/**
 * 地址信息生成器门面
 *
 * <p>提供地址相关数据的生成方法
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class AddressGen {

  private final DataGen gen;

  public AddressGen(DataGen gen) {
    this.gen = gen;
  }

  /** 生成完整地址 */
  public String fullAddress() {
    return gen.generate("address");
  }

  /** 生成省份 */
  public String province() {
    return gen.generate("address", Map.of("detail_level", "PROVINCE", "include_zipcode", "false"));
  }

  /** 生成城市 */
  public String city() {
    return gen.generate("address", Map.of("detail_level", "CITY", "include_zipcode", "false"));
  }

  /** 生成区县 */
  public String district() {
    return gen.generate("address", Map.of("detail_level", "DISTRICT", "include_zipcode", "false"));
  }

  /** 生成街道 */
  public String street() {
    return gen.generate("address", Map.of("detail_level", "STREET", "include_zipcode", "false"));
  }

  /** 生成邮政编码 */
  public String zipCode() {
    // Generate a random 6-digit zip code
    return String.format("%06d", (int) (Math.random() * 1000000));
  }

  /** 生成国家 */
  public String country() {
    // Return a fixed country for Chinese addresses
    return "中国";
  }

  /** 生成经度 */
  public Double longitude() {
    return Double.parseDouble(
        gen.generate("decimal", Map.of("min", -180.0, "max", 180.0, "scale", 6)));
  }

  /** 生成纬度 */
  public Double latitude() {
    return Double.parseDouble(
        gen.generate("decimal", Map.of("min", -90.0, "max", 90.0, "scale", 6)));
  }

  /** 生成坐标（经度,纬度） */
  public String coordinates() {
    return longitude() + "," + latitude();
  }

  /** 批量生成完整地址 */
  public List<String> fullAddresses(int count) {
    return gen.generateList("address", count);
  }

  /** 批量生成省份 */
  public List<String> provinces(int count) {
    return gen.generateList(
        "address", count, Map.of("detail_level", "PROVINCE", "include_zipcode", "false"));
  }

  /** 批量生成城市 */
  public List<String> cities(int count) {
    return gen.generateList(
        "address", count, Map.of("detail_level", "CITY", "include_zipcode", "false"));
  }

  /** 批量生成区县 */
  public List<String> districts(int count) {
    return gen.generateList(
        "address", count, Map.of("detail_level", "DISTRICT", "include_zipcode", "false"));
  }

  /** 批量生成街道 */
  public List<String> streets(int count) {
    return gen.generateList(
        "address", count, Map.of("detail_level", "STREET", "include_zipcode", "false"));
  }

  /** 批量生成邮政编码 */
  public List<String> zipCodes(int count) {
    return gen.generateList("decimal", count, Map.of("min", 100000, "max", 999999, "scale", 0));
  }

  /** 批量生成国家 */
  public List<String> countries(int count) {
    // Return list of "中国" for each count
    return java.util.stream.IntStream.range(0, count)
        .mapToObj(i -> "中国")
        .collect(java.util.stream.Collectors.toList());
  }
}
