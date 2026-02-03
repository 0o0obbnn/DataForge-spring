package com.dataforge.web.performance;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * 性能基准测试运行器。
 *
 * <p>提供便捷的方法来运行JMH性能基准测试。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public final class BenchmarkRunner {

  private BenchmarkRunner() {}

  /**
   * 运行Generator性能基准测试。
   *
   * @throws RunnerException 如果运行失败
   */
  public static void runGeneratorBenchmarks() throws RunnerException {
    ChainedOptionsBuilder options =
        new OptionsBuilder()
            .include(GeneratorBenchmarkTest.class.getSimpleName())
            .shouldDoGC(true)
            .resultFormat(ResultFormatType.JSON)
            .result("target/jmh-results/generator-benchmark.json")
            .forks(1)
            .warmupIterations(3)
            .warmupTime(TimeValue.seconds(1))
            .measurementIterations(5)
            .measurementTime(TimeValue.seconds(1))
            .threads(1);

    new Runner(options.build()).run();
  }

  /**
   * 运行所有性能基准测试。
   *
   * @throws RunnerException 如果运行失败
   */
  public static void runAllBenchmarks() throws RunnerException {
    ChainedOptionsBuilder options =
        new OptionsBuilder()
            .include("com.dataforge.web.performance.*")
            .shouldDoGC(true)
            .resultFormat(ResultFormatType.JSON)
            .result("target/jmh-results/all-benchmarks.json")
            .forks(1)
            .warmupIterations(3)
            .warmupTime(TimeValue.seconds(1))
            .measurementIterations(5)
            .measurementTime(TimeValue.seconds(1))
            .threads(1);

    new Runner(options.build()).run();
  }

  /**
   * 运行自定义基准测试。
   *
   * @param benchmarkClass 基准测试类
   * @throws RunnerException 如果运行失败
   */
  public static void runCustomBenchmark(Class<?> benchmarkClass) throws RunnerException {
    ChainedOptionsBuilder options =
        new OptionsBuilder()
            .include(benchmarkClass.getSimpleName())
            .shouldDoGC(true)
            .resultFormat(ResultFormatType.JSON)
            .result("target/jmh-results/" + benchmarkClass.getSimpleName() + ".json")
            .forks(1)
            .warmupIterations(3)
            .warmupTime(TimeValue.seconds(1))
            .measurementIterations(5)
            .measurementTime(TimeValue.seconds(1))
            .threads(1);

    new Runner(options.build()).run();
  }

  /**
   * 主方法，用于直接运行基准测试。
   *
   * @param args 命令行参数
   * @throws RunnerException 如果运行失败
   */
  public static void main(String[] args) throws RunnerException {
    System.out.println("Starting Generator Performance Benchmarks...");
    runGeneratorBenchmarks();
    System.out.println("Benchmarks completed. Results saved to target/jmh-results/");
  }
}
