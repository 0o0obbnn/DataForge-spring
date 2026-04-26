package com.dataforge.facade;

import java.util.List;
import java.util.Map;

/**
 * 互联网数据生成器门面
 *
 * <p>提供互联网相关数据的生成方法
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class InternetGen {

  private final DataGen gen;

  public InternetGen(DataGen gen) {
    this.gen = gen;
  }

  /** 生成邮箱地址 */
  public String email() {
    return gen.generate("email");
  }

  /** 生成邮箱地址（指定域名） */
  public String email(String domain) {
    return gen.generate("email", Map.of("domain", domain));
  }

  /** 生成 URL */
  public String url() {
    return gen.generate("url");
  }

  /** 生成域名 */
  public String domain() {
    return gen.generate("domain");
  }

  /** 生成 IPv4 地址 */
  public String ipv4() {
    return gen.generate("ip", Map.of("version", "4"));
  }

  /** 生成 IPv6 地址 */
  public String ipv6() {
    return gen.generate("ip", Map.of("version", "6"));
  }

  /** 生成 MAC 地址 */
  public String macAddress() {
    return gen.generate("mac");
  }

  /** 生成用户名 */
  public String username() {
    return gen.generate("username");
  }

  /** 生成密码 */
  public String password() {
    return gen.generate("password");
  }

  /** 生成密码（指定长度） */
  public String password(int length) {
    return gen.generate("password", Map.of("length", length));
  }

  /** 生成 User-Agent */
  public String userAgent() {
    return gen.generate("template", Map.of("template", "Mozilla/5.0 ({{os}}) {{browser}}"));
  }

  /** 生成颜色十六进制值 */
  public String colorHex() {
    return gen.generate("color", Map.of("format", "HEX"));
  }

  /** 批量生成邮箱 */
  public List<String> emails(int count) {
    return gen.generateList("email", count);
  }

  /** 批量生成 URL */
  public List<String> urls(int count) {
    return gen.generateList("url", count);
  }

  /** 批量生成 IPv4 地址 */
  public List<String> ipv4s(int count) {
    return gen.generateList("ip", count, Map.of("version", "4"));
  }
}
