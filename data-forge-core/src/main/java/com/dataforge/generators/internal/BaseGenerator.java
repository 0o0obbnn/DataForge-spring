package com.dataforge.generators.internal;

import com.dataforge.model.FieldConfig;

/**
 * 生成器基类，提供通用的参数获取方法
 *
 * @author DataForge
 */
public abstract class BaseGenerator {

  /** 从配置中获取字符串参数。 */
  protected String getStringParam(FieldConfig config, String key, String defaultValue) {
    if (config == null || config.getParams() == null) {
      return defaultValue;
    }
    Object value = config.getParams().get(key);
    return value != null ? value.toString() : defaultValue;
  }

  /** 从配置中获取布尔参数。 */
  protected boolean getBooleanParam(FieldConfig config, String key, boolean defaultValue) {
    if (config == null || config.getParams() == null) {
      return defaultValue;
    }
    Object value = config.getParams().get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    return Boolean.parseBoolean(value.toString());
  }

  /** 从配置中获取整数参数。 */
  protected int getIntParam(FieldConfig config, String key, int defaultValue) {
    if (config == null || config.getParams() == null) {
      return defaultValue;
    }
    Object value = config.getParams().get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    try {
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /** 从配置中获取双精度浮点数参数。 */
  protected double getDoubleParam(FieldConfig config, String key, double defaultValue) {
    if (config == null || config.getParams() == null) {
      return defaultValue;
    }
    Object value = config.getParams().get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    try {
      return Double.parseDouble(value.toString());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /** 从配置中获取长整数参数。 */
  protected long getLongParam(FieldConfig config, String key, long defaultValue) {
    if (config == null || config.getParams() == null) {
      return defaultValue;
    }
    Object value = config.getParams().get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    try {
      return Long.parseLong(value.toString());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
