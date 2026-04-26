package com.dataforge.web.performance;

import com.dataforge.config.SimpleFieldConfig;
import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.internal.EmailGenerator;
import com.dataforge.generators.internal.NameGenerator;
import com.dataforge.generators.internal.PhoneGenerator;
import com.dataforge.generators.internal.UuidGenerator;
import com.dataforge.generators.internal.YamlGenerator;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Generator性能基准测试。
 *
 * <p>使用JMH对常用Generator进行性能基准测试，评估吞吐量和延迟。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class GeneratorBenchmarkTest {

  private UuidGenerator uuidGenerator;
  private NameGenerator nameGenerator;
  private EmailGenerator emailGenerator;
  private PhoneGenerator phoneGenerator;
  private YamlGenerator yamlGenerator;
  private DataForgeContext context;
  private SimpleFieldConfig config;

  @Setup
  public void setup() {
    uuidGenerator = new UuidGenerator();
    nameGenerator = new NameGenerator();
    emailGenerator = new EmailGenerator();
    phoneGenerator = new PhoneGenerator();
    yamlGenerator = new YamlGenerator();
    context = new DataForgeContext();
    config = new SimpleFieldConfig();
  }

  @Benchmark
  public String benchmarkUuidGeneration() {
    config.setType("uuid");
    return uuidGenerator.generate(config, context);
  }

  @Benchmark
  public String benchmarkNameGeneration() {
    config.setType("name");
    return nameGenerator.generate(config, context);
  }

  @Benchmark
  public String benchmarkEmailGeneration() {
    config.setType("email");
    return emailGenerator.generate(config, context);
  }

  @Benchmark
  public String benchmarkPhoneGeneration() {
    config.setType("phone");
    return phoneGenerator.generate(config, context);
  }

  @Benchmark
  public String benchmarkYamlGeneration() {
    config.setType("yaml");
    config.setParam("structure", "SIMPLE");
    config.setParam("key_count", "5");
    return yamlGenerator.generate(config, context);
  }

  @Benchmark
  public String benchmarkComplexYamlGeneration() {
    config.setType("yaml");
    config.setParam("structure", "COMPLEX");
    config.setParam("depth", "3");
    config.setParam("key_count", "10");
    return yamlGenerator.generate(config, context);
  }

  @Benchmark
  public String benchmarkNestedYamlGeneration() {
    config.setType("yaml");
    config.setParam("structure", "NESTED");
    config.setParam("depth", "5");
    config.setParam("key_count", "8");
    return yamlGenerator.generate(config, context);
  }
}
