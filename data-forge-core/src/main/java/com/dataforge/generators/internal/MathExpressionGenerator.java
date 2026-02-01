package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数学表达式生成器
 *
 * <p>支持生成各种数学表达式，包括基本运算、函数调用、 方程式等，用于数学测试、表达式解析、计算验证等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>expression_type: 表达式类型 (ARITHMETIC|ALGEBRAIC|TRIGONOMETRIC|LOGARITHMIC|POLYNOMIAL|EQUATION)
 *       默认: ARITHMETIC
 *   <li>complexity: 复杂度 (SIMPLE|MEDIUM|COMPLEX) 默认: MEDIUM
 *   <li>variable_count: 变量数量 默认: 1
 *   <li>max_depth: 最大嵌套深度 默认: 3
 *   <li>include_parentheses: 是否包含括号 默认: true
 *   <li>operators: 允许的运算符 默认: +,-,*,/
 *   <li>functions: 允许的函数 默认: sin,cos,log
 *   <li>format: 输出格式 (INFIX|PREFIX|POSTFIX|LATEX) 默认: INFIX
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class MathExpressionGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(MathExpressionGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  private static final String[] BASIC_OPERATORS = {"+", "-", "*", "/"};
  private static final String[] ADVANCED_OPERATORS = {"+", "-", "*", "/", "^", "%"};
  private static final String[] VARIABLES = {"x", "y", "z", "a", "b", "c", "t", "n"};

  @Override
  public String getType() {
    return "math_expression";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String expressionType = getStringParam(config, "expression_type", "ARITHMETIC");
      String complexity = getStringParam(config, "complexity", "MEDIUM");
      String format = getStringParam(config, "format", "INFIX");

      String expression = generateExpression(expressionType, complexity, config);
      String result = formatExpression(expression, format);

      // 存储到上下文
      context.put("expression_type", expressionType);
      context.put("expression_complexity", complexity);
      context.put("expression_format", format);

      return result;

    } catch (Exception e) {
      logger.error("Failed to generate math expression", e);
      return "x + 1";
    }
  }

  private String generateExpression(String expressionType, String complexity, FieldConfig config) {
    switch (expressionType.toUpperCase()) {
      case "ARITHMETIC":
        return generateArithmeticExpression(complexity, config);
      case "ALGEBRAIC":
        return generateAlgebraicExpression(complexity, config);
      case "TRIGONOMETRIC":
        return generateTrigonometricExpression(complexity, config);
      case "LOGARITHMIC":
        return generateLogarithmicExpression(complexity, config);
      case "POLYNOMIAL":
        return generatePolynomialExpression(complexity, config);
      case "EQUATION":
        return generateEquation(complexity, config);
      default:
        return generateArithmeticExpression(complexity, config);
    }
  }

  private String generateArithmeticExpression(String complexity, FieldConfig config) {
    int depth = getComplexityDepth(complexity);
    String[] operators = getOperators(config, BASIC_OPERATORS);

    return buildExpression(depth, operators, false, config);
  }

  private String generateAlgebraicExpression(String complexity, FieldConfig config) {
    int depth = getComplexityDepth(complexity);
    String[] operators = getOperators(config, ADVANCED_OPERATORS);

    return buildExpression(depth, operators, true, config);
  }

  private String generateTrigonometricExpression(String complexity, FieldConfig config) {
    String[] trigFunctions = {"sin", "cos", "tan", "sec", "csc", "cot"};
    String function = trigFunctions[random.nextInt(trigFunctions.length)];
    String innerExpression = generateSimpleExpression(config);

    return function + "(" + innerExpression + ")";
  }

  private String generateLogarithmicExpression(String complexity, FieldConfig config) {
    String[] logFunctions = {"log", "ln", "log10", "log2"};
    String function = logFunctions[random.nextInt(logFunctions.length)];
    String innerExpression = generateSimpleExpression(config);

    return function + "(" + innerExpression + ")";
  }

  private String generatePolynomialExpression(String complexity, FieldConfig config) {
    int degree = getComplexityDepth(complexity) + 1;
    String variable = getRandomVariable(config);
    StringBuilder polynomial = new StringBuilder();

    for (int i = degree; i >= 0; i--) {
      if (polynomial.length() > 0) {
        polynomial.append(" + ");
      }

      int coefficient = 1 + random.nextInt(10);
      if (i == 0) {
        polynomial.append(coefficient);
      } else if (i == 1) {
        polynomial.append(coefficient).append(variable);
      } else {
        polynomial.append(coefficient).append(variable).append("^").append(i);
      }
    }

    return polynomial.toString();
  }

  private String generateEquation(String complexity, FieldConfig config) {
    String leftSide = generateAlgebraicExpression(complexity, config);
    String rightSide = generateSimpleExpression(config);

    return leftSide + " = " + rightSide;
  }

  private String buildExpression(
      int depth, String[] operators, boolean useVariables, FieldConfig config) {
    if (depth <= 0) {
      return generateTerm(useVariables, config);
    }

    String left = buildExpression(depth - 1, operators, useVariables, config);
    String operator = operators[random.nextInt(operators.length)];
    String right = buildExpression(depth - 1, operators, useVariables, config);

    boolean includeParentheses = getBooleanParam(config, "include_parentheses", true);
    if (includeParentheses && random.nextBoolean()) {
      return "(" + left + " " + operator + " " + right + ")";
    } else {
      return left + " " + operator + " " + right;
    }
  }

  private String generateTerm(boolean useVariables, FieldConfig config) {
    if (useVariables && random.nextBoolean()) {
      return getRandomVariable(config);
    } else {
      return String.valueOf(1 + random.nextInt(20));
    }
  }

  private String generateSimpleExpression(FieldConfig config) {
    String variable = getRandomVariable(config);
    int coefficient = 1 + random.nextInt(10);
    int constant = random.nextInt(20);

    return coefficient + variable + " + " + constant;
  }

  private String getRandomVariable(FieldConfig config) {
    int variableCount = getIntParam(config, "variable_count", 1);
    int maxIndex = Math.min(variableCount, VARIABLES.length);
    return VARIABLES[random.nextInt(maxIndex)];
  }

  private String[] getOperators(FieldConfig config, String[] defaultOperators) {
    String operatorsParam = getStringParam(config, "operators", null);
    if (operatorsParam != null) {
      return operatorsParam.split(",");
    }
    return defaultOperators;
  }

  private int getComplexityDepth(String complexity) {
    switch (complexity.toUpperCase()) {
      case "SIMPLE":
        return 1;
      case "MEDIUM":
        return 2;
      case "COMPLEX":
        return 3;
      default:
        return 2;
    }
  }

  private String formatExpression(String expression, String format) {
    switch (format.toUpperCase()) {
      case "INFIX":
        return expression;
      case "PREFIX":
        return convertToPrefix(expression);
      case "POSTFIX":
        return convertToPostfix(expression);
      case "LATEX":
        return convertToLatex(expression);
      default:
        return expression;
    }
  }

  private String convertToPrefix(String expression) {
    // 简化的前缀转换（实际应用中需要完整的解析器）
    return "prefix(" + expression + ")";
  }

  private String convertToPostfix(String expression) {
    // 简化的后缀转换（实际应用中需要完整的解析器）
    return "postfix(" + expression + ")";
  }

  private String convertToLatex(String expression) {
    // 简单的LaTeX转换
    return expression.replace("^", "^{")
        + "}"
            .replace("sqrt", "\\sqrt")
            .replace("sin", "\\sin")
            .replace("cos", "\\cos")
            .replace("log", "\\log");
  }
}
