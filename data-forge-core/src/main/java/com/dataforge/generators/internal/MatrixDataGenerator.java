package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 矩阵数据生成器
 *
 * <p>根据DataForge设计文档要求，生成各种类型的矩阵数据用于数学计算、机器学习等场景测试。 支持整数、浮点数、布尔值等类型的矩阵，可配置矩阵维度和数值范围。
 *
 * @author DataForge
 * @version 1.0.0
 * @since 2024-01-15
 */
@Component
public class MatrixDataGenerator extends BaseGenerator
    implements DataGenerator<List<List<Object>>, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(MatrixDataGenerator.class);

  @Override
  public String getType() {
    return "matrix";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public List<List<Object>> generate(FieldConfig config, DataForgeContext context) {
    try {
      String dataType = config.getParam("dataType", String.class, "DOUBLE");
      int rows = Integer.parseInt(config.getParam("rows", String.class, "3"));
      int cols = Integer.parseInt(config.getParam("cols", String.class, "3"));
      String valueRange = config.getParam("valueRange", String.class, "-100,100");
      boolean sparse = Boolean.parseBoolean(config.getParam("sparse", String.class, "false"));
      double sparseRatio = Double.parseDouble(config.getParam("sparseRatio", String.class, "0.1"));

      return generateMatrix(dataType, rows, cols, valueRange, sparse, sparseRatio);

    } catch (Exception e) {
      logger.warn("Error generating matrix data: {}", e.getMessage());
      // 生成默认3x3浮点数矩阵
      return generateDefaultMatrix();
    }
  }

  /** 生成矩阵数据 */
  private List<List<Object>> generateMatrix(
      String dataType, int rows, int cols, String valueRange, boolean sparse, double sparseRatio) {
    List<List<Object>> matrix = new ArrayList<>(rows);

    // 解析数值范围
    double minValue = -100.0;
    double maxValue = 100.0;
    try {
      String[] rangeParts = valueRange.split(",");
      if (rangeParts.length == 2) {
        minValue = Double.parseDouble(rangeParts[0]);
        maxValue = Double.parseDouble(rangeParts[1]);
      }
    } catch (NumberFormatException e) {
      logger.warn("Invalid value range format, using default range -100 to 100");
    }

    ThreadLocalRandom random = ThreadLocalRandom.current();

    for (int i = 0; i < rows; i++) {
      List<Object> row = new ArrayList<>(cols);
      for (int j = 0; j < cols; j++) {
        // 处理稀疏矩阵
        if (sparse && random.nextDouble() < sparseRatio) {
          row.add(null); // 稀疏矩阵中的空值
          continue;
        }

        Object value;
        switch (dataType.toUpperCase()) {
          case "INTEGER":
            value = random.nextInt((int) minValue, (int) maxValue + 1);
            break;
          case "LONG":
            value = random.nextLong((long) minValue, (long) maxValue + 1);
            break;
          case "BOOLEAN":
            value = random.nextBoolean();
            break;
          case "DOUBLE":
          default:
            value = minValue + (maxValue - minValue) * random.nextDouble();
            break;
        }
        row.add(value);
      }
      matrix.add(row);
    }

    return matrix;
  }

  /** 生成默认矩阵 */
  private List<List<Object>> generateDefaultMatrix() {
    List<List<Object>> matrix = new ArrayList<>(3);
    ThreadLocalRandom random = ThreadLocalRandom.current();

    for (int i = 0; i < 3; i++) {
      List<Object> row = new ArrayList<>(3);
      for (int j = 0; j < 3; j++) {
        row.add(-100.0 + 200.0 * random.nextDouble());
      }
      matrix.add(row);
    }

    return matrix;
  }

  @Override
  public boolean isValidConfig(FieldConfig config) {
    if (config == null) {
      return false;
    }

    try {
      int rows = Integer.parseInt(config.getParam("rows", String.class, "3"));
      int cols = Integer.parseInt(config.getParam("cols", String.class, "3"));
      String valueRange = config.getParam("valueRange", String.class, "-100,100");
      double sparseRatio = Double.parseDouble(config.getParam("sparseRatio", String.class, "0.1"));

      // 验证矩阵维度（1x1到1000x1000）
      if (rows < 1 || rows > 1000 || cols < 1 || cols > 1000) {
        return false;
      }

      // 验证数值范围格式
      String[] rangeParts = valueRange.split(",");
      if (rangeParts.length != 2) {
        return false;
      }

      double minValue = Double.parseDouble(rangeParts[0]);
      double maxValue = Double.parseDouble(rangeParts[1]);
      if (minValue >= maxValue) {
        return false;
      }

      // 验证稀疏比例（0.0到1.0之间）
      return sparseRatio >= 0.0 && sparseRatio <= 1.0;

    } catch (NumberFormatException e) {
      return false;
    }
  }

  @Override
  public String getDescription() {
    return "生成矩阵数据，支持整数、长整数、浮点数、布尔值等类型，" + "可配置矩阵维度、数值范围和稀疏性，适用于数学计算和机器学习测试场景";
  }
}
