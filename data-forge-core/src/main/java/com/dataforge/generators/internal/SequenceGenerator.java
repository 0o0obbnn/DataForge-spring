package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数列生成器
 *
 * <p>支持生成各种数学数列，包括等差数列、等比数列、 斐波那契数列、质数数列等，用于数学测试、算法验证、序列分析等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>sequence_type: 数列类型 (ARITHMETIC|GEOMETRIC|FIBONACCI|PRIME|RANDOM|CUSTOM) 默认: ARITHMETIC
 *   <li>length: 数列长度 默认: 10
 *   <li>start_value: 起始值 默认: 1
 *   <li>step: 步长/公比 默认: 1
 *   <li>format: 输出格式 (ARRAY|COMMA|SPACE|JSON|VERBOSE) 默认: COMMA
 *   <li>precision: 小数精度 默认: 0
 *   <li>max_value: 最大值限制 默认: 1000000
 *   <li>include_zero: 是否包含零 默认: true
 *   <li>ascending: 是否升序 默认: true
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class SequenceGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(SequenceGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  @Override
  public String getType() {
    return "sequence";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String sequenceType = getStringParam(config, "sequence_type", "ARITHMETIC");
      int length = getIntParam(config, "length", 10);
      String format = getStringParam(config, "format", "COMMA");

      List<BigDecimal> sequence = generateSequence(sequenceType, length, config);
      String result = formatSequence(sequence, format, config);

      // 存储到上下文
      context.put("sequence_type", sequenceType);
      context.put("sequence_length", length);
      context.put("sequence_first", sequence.isEmpty() ? null : sequence.get(0));
      context.put("sequence_last", sequence.isEmpty() ? null : sequence.get(sequence.size() - 1));

      return result;

    } catch (Exception e) {
      logger.error("Failed to generate sequence", e);
      return "1,2,3,4,5";
    }
  }

  private List<BigDecimal> generateSequence(String sequenceType, int length, FieldConfig config) {
    switch (sequenceType.toUpperCase()) {
      case "ARITHMETIC":
        return generateArithmeticSequence(length, config);
      case "GEOMETRIC":
        return generateGeometricSequence(length, config);
      case "FIBONACCI":
        return generateFibonacciSequence(length, config);
      case "PRIME":
        return generatePrimeSequence(length, config);
      case "RANDOM":
        return generateRandomSequence(length, config);
      default:
        return generateArithmeticSequence(length, config);
    }
  }

  private List<BigDecimal> generateArithmeticSequence(int length, FieldConfig config) {
    BigDecimal start = BigDecimal.valueOf(getDoubleParam(config, "start_value", 1.0));
    BigDecimal step = BigDecimal.valueOf(getDoubleParam(config, "step", 1.0));

    List<BigDecimal> sequence = new ArrayList<>();
    BigDecimal current = start;

    for (int i = 0; i < length; i++) {
      sequence.add(current);
      current = current.add(step);
    }

    return sequence;
  }

  private List<BigDecimal> generateGeometricSequence(int length, FieldConfig config) {
    BigDecimal start = BigDecimal.valueOf(getDoubleParam(config, "start_value", 1.0));
    BigDecimal ratio = BigDecimal.valueOf(getDoubleParam(config, "step", 2.0));
    BigDecimal maxValue = BigDecimal.valueOf(getDoubleParam(config, "max_value", 1000000.0));

    List<BigDecimal> sequence = new ArrayList<>();
    BigDecimal current = start;

    for (int i = 0; i < length && current.compareTo(maxValue) <= 0; i++) {
      sequence.add(current);
      current = current.multiply(ratio);
    }

    return sequence;
  }

  private List<BigDecimal> generateFibonacciSequence(int length, FieldConfig config) {
    List<BigDecimal> sequence = new ArrayList<>();

    if (length >= 1) {
      sequence.add(BigDecimal.ZERO);
    }
    if (length >= 2) {
      sequence.add(BigDecimal.ONE);
    }

    for (int i = 2; i < length; i++) {
      BigDecimal next = sequence.get(i - 1).add(sequence.get(i - 2));
      sequence.add(next);
    }

    return sequence;
  }

  private List<BigDecimal> generatePrimeSequence(int length, FieldConfig config) {
    List<BigDecimal> sequence = new ArrayList<>();
    int current = 2;

    while (sequence.size() < length) {
      if (isPrime(current)) {
        sequence.add(BigDecimal.valueOf(current));
      }
      current++;
    }

    return sequence;
  }

  private List<BigDecimal> generateRandomSequence(int length, FieldConfig config) {
    double minValue = getDoubleParam(config, "min_value", 1.0);
    double maxValue = getDoubleParam(config, "max_value", 100.0);
    boolean ascending = getBooleanParam(config, "ascending", true);

    List<BigDecimal> sequence = new ArrayList<>();

    for (int i = 0; i < length; i++) {
      double value = minValue + random.nextDouble() * (maxValue - minValue);
      sequence.add(BigDecimal.valueOf(value));
    }

    if (ascending) {
      sequence.sort(BigDecimal::compareTo);
    }

    return sequence;
  }

  private boolean isPrime(int n) {
    if (n < 2) {
      return false;
    }
    if (n == 2) {
      return true;
    }
    if (n % 2 == 0) {
      return false;
    }

    for (int i = 3; i * i <= n; i += 2) {
      if (n % i == 0) {
        return false;
      }
    }
    return true;
  }

  private String formatSequence(List<BigDecimal> sequence, String format, FieldConfig config) {
    int precision = getIntParam(config, "precision", 0);

    // 应用精度
    List<String> formattedNumbers =
        sequence.stream()
            .map(bd -> bd.setScale(precision, RoundingMode.HALF_UP).toPlainString())
            .collect(Collectors.toList());

    switch (format.toUpperCase()) {
      case "ARRAY":
        return "[" + String.join(", ", formattedNumbers) + "]";
      case "COMMA":
        return String.join(",", formattedNumbers);
      case "SPACE":
        return String.join(" ", formattedNumbers);
      case "JSON":
        return "{\"sequence\":[" + String.join(",", formattedNumbers) + "]}";
      case "VERBOSE":
        return "Sequence: " + String.join(", ", formattedNumbers);
      default:
        return String.join(",", formattedNumbers);
    }
  }
}
