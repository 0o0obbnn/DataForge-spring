package com.dataforge.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * 校验结果类。
 *
 * <p>封装了数据校验的结果信息，包括是否有效、错误消息等。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class ValidationResult {

  /** 校验是否通过。 */
  private final boolean valid;

  /** 错误消息列表。 */
  private final List<String> errorMessages;

  /** 警告消息列表。 */
  private final List<String> warningMessages;

  /** 构造函数 - 创建有效的校验结果。 */
  public ValidationResult() {
    this.valid = true;
    this.errorMessages = new ArrayList<>();
    this.warningMessages = new ArrayList<>();
  }

  /**
   * 构造函数 - 创建指定有效性的校验结果。
   *
   * @param valid 是否有效
   */
  public ValidationResult(boolean valid) {
    this.valid = valid;
    this.errorMessages = new ArrayList<>();
    this.warningMessages = new ArrayList<>();
  }

  /**
   * 构造函数 - 创建无效的校验结果并指定错误消息。
   *
   * @param errorMessage 错误消息
   */
  public ValidationResult(String errorMessage) {
    this.valid = false;
    this.errorMessages = new ArrayList<>();
    this.warningMessages = new ArrayList<>();
    this.errorMessages.add(errorMessage);
  }

  /**
   * 构造函数 - 创建校验结果并指定所有属性。
   *
   * @param valid 是否有效
   * @param errorMessages 错误消息列表
   * @param warningMessages 警告消息列表
   */
  public ValidationResult(boolean valid, List<String> errorMessages, List<String> warningMessages) {
    this.valid = valid;
    this.errorMessages = errorMessages != null ? new ArrayList<>(errorMessages) : new ArrayList<>();
    this.warningMessages =
        warningMessages != null ? new ArrayList<>(warningMessages) : new ArrayList<>();
  }

  /**
   * 检查校验是否通过。
   *
   * @return 如果校验通过返回true，否则返回false
   */
  public boolean isValid() {
    return valid;
  }

  /**
   * 获取错误消息列表。
   *
   * @return 错误消息列表的副本
   */
  public List<String> getErrorMessages() {
    return new ArrayList<>(errorMessages);
  }

  /**
   * 获取警告消息列表。
   *
   * @return 警告消息列表的副本
   */
  public List<String> getWarningMessages() {
    return new ArrayList<>(warningMessages);
  }

  /**
   * 检查是否有错误消息。
   *
   * @return 如果有错误消息返回true，否则返回false
   */
  public boolean hasErrors() {
    return !errorMessages.isEmpty();
  }

  /**
   * 检查是否有警告消息。
   *
   * @return 如果有警告消息返回true，否则返回false
   */
  public boolean hasWarnings() {
    return !warningMessages.isEmpty();
  }

  /**
   * 获取第一个错误消息。
   *
   * @return 第一个错误消息，如果没有错误消息则返回null
   */
  public String getFirstErrorMessage() {
    return errorMessages.isEmpty() ? null : errorMessages.get(0);
  }

  /**
   * 获取第一个警告消息。
   *
   * @return 第一个警告消息，如果没有警告消息则返回null
   */
  public String getFirstWarningMessage() {
    return warningMessages.isEmpty() ? null : warningMessages.get(0);
  }

  /**
   * 创建成功的校验结果。
   *
   * @return 成功的校验结果
   */
  public static ValidationResult success() {
    return new ValidationResult(true);
  }

  /**
   * 创建失败的校验结果。
   *
   * @param errorMessage 错误消息
   * @return 失败的校验结果
   */
  public static ValidationResult failure(String errorMessage) {
    return new ValidationResult(errorMessage);
  }

  /**
   * 创建带有多个错误消息的失败校验结果。
   *
   * @param errorMessages 错误消息列表
   * @return 失败的校验结果
   */
  public static ValidationResult failure(List<String> errorMessages) {
    return new ValidationResult(false, errorMessages, null);
  }

  /**
   * 创建带有警告的成功校验结果。
   *
   * @param warningMessage 警告消息
   * @return 带有警告的成功校验结果
   */
  public static ValidationResult successWithWarning(String warningMessage) {
    List<String> warnings = new ArrayList<>();
    warnings.add(warningMessage);
    return new ValidationResult(true, null, warnings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ValidationResult{");
    sb.append("valid=").append(valid);

    if (!errorMessages.isEmpty()) {
      sb.append(", errors=").append(errorMessages);
    }

    if (!warningMessages.isEmpty()) {
      sb.append(", warnings=").append(warningMessages);
    }

    sb.append('}');
    return sb.toString();
  }
}
