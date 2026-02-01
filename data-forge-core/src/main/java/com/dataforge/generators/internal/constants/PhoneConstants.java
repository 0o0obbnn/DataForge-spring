package com.dataforge.generators.internal.constants;

/**
 * 手机号生成器常量类。
 *
 * @author DataForge
 * @since 1.0.0
 */
public final class PhoneConstants {

  private PhoneConstants() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  // 手机号长度
  public static final int PHONE_LENGTH = 11;

  // 手机号前缀长度
  public static final int PREFIX_LENGTH = 3;

  // 手机号后缀长度
  public static final int SUFFIX_LENGTH = 8;

  // 手机号段前缀
  public static final String[] MOBILE_PREFIXES = {
    "130", "131", "132", "133", "134", "135", "136", "137", "138", "139", "150", "151", "152",
    "153", "155", "156", "157", "158", "159", "180", "181", "182", "183", "184", "185", "186",
    "187", "188", "189", "190", "191", "192", "193", "195", "196", "197", "198", "199"
  };

  // 虚拟运营商号段
  public static final String[] VIRTUAL_PREFIXES = {"170", "171", "165", "167"};

  // 物联网号段
  public static final String[] IOT_PREFIXES = {"140", "141", "142", "143", "144", "146", "148"};
}
