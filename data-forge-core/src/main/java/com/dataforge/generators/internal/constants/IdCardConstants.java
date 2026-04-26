package com.dataforge.generators.internal.constants;

/**
 * 身份证号生成器常量类。
 *
 * <p>包含身份证号生成过程中使用的各种常量值。
 *
 * @author DataForge
 * @since 1.0.0
 */
public final class IdCardConstants {

  private IdCardConstants() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  // 身份证号长度
  public static final int IDCARD_LENGTH = 18;

  // 地区代码位置
  public static final int REGION_CODE_START = 0;
  public static final int REGION_CODE_END = 6;
  public static final int REGION_CODE_LENGTH = 6;

  // 出生日期位置
  public static final int BIRTH_DATE_START = 6;
  public static final int BIRTH_DATE_END = 14;
  public static final int BIRTH_DATE_LENGTH = 8;

  // 顺序码位置
  public static final int SEQUENCE_START = 14;
  public static final int SEQUENCE_END = 17;
  public static final int SEQUENCE_LENGTH = 3;

  // 校验位位置
  public static final int CHECK_DIGIT_INDEX = 17;

  // 年份、月份、日期在出生日期中的位置
  public static final int YEAR_START = 0;
  public static final int YEAR_END = 4;
  public static final int MONTH_START = 4;
  public static final int MONTH_END = 6;
  public static final int DAY_START = 6;
  public static final int DAY_END = 8;

  // 校验位权重系数
  public static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

  // 校验位映射
  public static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

  // 默认日期范围
  public static final String DEFAULT_START_DATE = "1980-01-01";
  public static final String DEFAULT_END_DATE = "2000-12-31";

  // 性别判断
  public static final int MALE_SEQUENCE_START = 1;
  public static final int MALE_SEQUENCE_END = 499;
  public static final int FEMALE_SEQUENCE_START = 500;
  public static final int FEMALE_SEQUENCE_END = 999;

  // 无效身份证类型
  public static final String INVALID_TYPE_FORMAT = "FORMAT";
  public static final String INVALID_TYPE_CHECKSUM = "CHECKSUM";
  public static final String INVALID_TYPE_DATE = "DATE";
  public static final String INVALID_TYPE_REGION = "REGION";
}
