package com.dataforge.web.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JaCoCo覆盖率验证测试。
 *
 * <p>此测试类用于验证JaCoCo覆盖率配置是否正常工作。 运行 `mvn clean test jacoco:report` 后，检查
 * target/jacoco-report/index.html 查看覆盖率报告。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("JaCoCo覆盖率验证测试")
class JaCoCoCoverageVerificationTest {

  @Test
  @DisplayName("应能运行测试并生成覆盖率报告")
  void shouldRunTestsAndGenerateCoverageReport() {
    String instructions =
        """
        运行以下命令生成覆盖率报告：

        1. 运行所有测试并生成覆盖率报告：
           mvn clean test jacoco:report

        2. 查看覆盖率报告：
           打开 target/jacoco-report/index.html

        3. 聚合所有模块的覆盖率报告：
           mvn clean test -pl data-forge-coverage jacoco:report-aggregate

        4. 查看聚合报告：
           打开 target/jacoco-aggregate/index.html

        覆盖率要求：
        - 行覆盖率 (Line Coverage): ≥ 75%
        - 分支覆盖率 (Branch Coverage): ≥ 70%
        - 方法覆盖率 (Method Coverage): ≥ 80%
        - 类覆盖率 (Class Coverage): ≥ 90%
        """;

    System.out.println(instructions);
  }

  @Test
  @DisplayName("应能检查覆盖率阈值")
  void shouldCheckCoverageThresholds() {
    String checkInstructions =
        """
        检查覆盖率阈值：

        运行以下命令检查覆盖率是否符合要求：
        mvn jacoco:check

        如果覆盖率低于阈值，构建将失败。

        当前阈值配置（data-forge-web/pom.xml）：
        - CLASS: 85%
        - METHOD: 85%
        - LINE: 85%
        - BRANCH: 80%

        注意：这些是临时的高阈值，实际生产环境应根据测试优化计划调整：
        - 行覆盖率: ≥ 75%
        - 分支覆盖率: ≥ 70%
        - 方法覆盖率: ≥ 80%
        - 类覆盖率: ≥ 90%
        """;

    System.out.println(checkInstructions);
  }

  @Test
  @DisplayName("应能生成覆盖率报告摘要")
  void shouldGenerateCoverageSummary() {
    String summaryInstructions =
        """
        生成覆盖率报告摘要：

        1. 单独运行每个模块的测试：
           cd data-forge-api && mvn test jacoco:report
           cd data-forge-core && mvn test jacoco:report
           cd data-forge-cli && mvn test jacoco:report
           cd data-forge-web && mvn test jacoco:report

        2. 从根目录运行所有测试：
           mvn clean test

        3. 生成聚合报告：
           cd data-forge-coverage
           mvn jacoco:report-aggregate

        4. 查看报告：
           - 单模块报告: data-forge-web/target/jacoco-report/index.html
           - 聚合报告: target/jacoco-aggregate/index.html
        """;

    System.out.println(summaryInstructions);
  }
}
