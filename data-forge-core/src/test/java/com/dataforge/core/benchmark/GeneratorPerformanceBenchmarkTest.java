package com.dataforge.core.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.BatchGenerator;
import com.dataforge.core.BatchGenerator.DataSchema;
import com.dataforge.core.BatchGenerator.FieldDefinition;
import com.dataforge.core.DataForgeContext;
import com.dataforge.core.GeneratorFactory;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.SimpleFieldConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 生成器性能基准测试。
 *
 * <p>测试核心生成器的性能表现，建立性能基线以便后续优化和回归测试。
 *
 * <p>测试指标：
 *
 * <ul>
 *   <li>单线程生成速率（records/sec）
 *   <li>多线程扩展性
 *   <li>内存使用效率
 *   <li>大批量数据生成性能
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("生成器性能基准测试")
public class GeneratorPerformanceBenchmarkTest {

  @Autowired private GeneratorFactory generatorFactory;

  @Autowired private BatchGenerator batchGenerator;

  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    context = new DataForgeContext();
  }

  /** 测试 UUID 生成器性能。 */
  @Test
  @DisplayName("UUID生成器性能基准 - 单线程 10000 条记录")
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  void uuidGeneratorPerformance() {
    // Arrange
    DataGenerator<?, ?> generator = generatorFactory.getGenerator("uuid");
    assertThat(generator).isNotNull();

    SimpleFieldConfig config = new SimpleFieldConfig();
    config.setType("uuid");

    @SuppressWarnings("unchecked")
    DataGenerator<String, SimpleFieldConfig> uuidGenerator =
        (DataGenerator<String, SimpleFieldConfig>) generator;

    // Act
    long startTime = System.nanoTime();
    int count = 10000;

    for (int i = 0; i < count; i++) {
      uuidGenerator.generate(config, context);
    }

    long endTime = System.nanoTime();
    long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    double recordsPerSecond = (count * 1000.0) / durationMs;

    // Assert & Report
    System.out.printf(
        "UUID Generator Performance: %d records in %d ms (%.2f records/sec)%n",
        count, durationMs, recordsPerSecond);

    // 性能基线：至少 10000 records/sec
    assertThat(recordsPerSecond).isGreaterThan(10000.0);
  }

  /** 测试姓名生成器性能。 */
  @Test
  @DisplayName("姓名生成器性能基准 - 单线程 5000 条记录")
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  void nameGeneratorPerformance() {
    // Arrange
    DataGenerator<?, ?> generator = generatorFactory.getGenerator("name");
    assertThat(generator).isNotNull();

    SimpleFieldConfig config = new SimpleFieldConfig();
    config.setType("name");
    config.setParam("type", "CN");

    @SuppressWarnings("unchecked")
    DataGenerator<String, SimpleFieldConfig> nameGenerator =
        (DataGenerator<String, SimpleFieldConfig>) generator;

    // Act
    long startTime = System.nanoTime();
    int count = 5000;

    for (int i = 0; i < count; i++) {
      nameGenerator.generate(config, context);
    }

    long endTime = System.nanoTime();
    long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    double recordsPerSecond = (count * 1000.0) / durationMs;

    // Assert & Report
    System.out.printf(
        "Name Generator Performance: %d records in %d ms (%.2f records/sec)%n",
        count, durationMs, recordsPerSecond);

    // 性能基线：至少 5000 records/sec
    assertThat(recordsPerSecond).isGreaterThan(5000.0);
  }

  /** 测试批量生成器性能。 */
  @Test
  @DisplayName("BatchGenerator 性能基准 - 10万条记录")
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void batchGeneratorPerformance() {
    // Arrange
    List<FieldDefinition> fields = new ArrayList<>();
    fields.add(new FieldDefinition("id", "uuid", new SimpleFieldConfig()));
    fields.add(new FieldDefinition("name", "name", new SimpleFieldConfig()));
    fields.add(new FieldDefinition("email", "email", new SimpleFieldConfig()));

    DataSchema schema = new DataSchema(fields, new ArrayList<>());
    long totalRecords = 100000;

    List<Map<String, Object>> generatedRecords = new ArrayList<>();

    // Act
    long startTime = System.nanoTime();

    BatchGenerator.GenerationStats stats =
        batchGenerator.generateBatch(
            schema, totalRecords, records -> generatedRecords.addAll(records));

    long endTime = System.nanoTime();
    long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    double recordsPerSecond = (totalRecords * 1000.0) / durationMs;

    // Assert & Report
    System.out.printf(
        "BatchGenerator Performance: %d records in %d ms (%.2f records/sec)%n",
        totalRecords, durationMs, recordsPerSecond);
    System.out.printf(
        "Stats: threadCount=%d, batchSize=%d, throughput=%d records/sec%n",
        stats.getThreadCount(), 10000, stats.getThroughput());

    // 性能基线：至少 10000 records/sec
    assertThat(recordsPerSecond).isGreaterThan(10000.0);
    assertThat(generatedRecords.size()).isEqualTo(totalRecords);
  }

  /** 测试多线程扩展性。 */
  @Test
  @DisplayName("多线程扩展性测试 - 1/2/4/8 线程对比")
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void multiThreadScalabilityTest() {
    // Arrange
    List<FieldDefinition> fields = new ArrayList<>();
    fields.add(new FieldDefinition("id", "uuid", new SimpleFieldConfig()));
    fields.add(new FieldDefinition("name", "name", new SimpleFieldConfig()));

    DataSchema schema = new DataSchema(fields, new ArrayList<>());
    long totalRecords = 50000;

    int[] threadCounts = {1, 2, 4, 8};

    System.out.println("Multi-thread Scalability Test:");
    System.out.println("==============================");

    for (int threadCount : threadCounts) {
      // 重置线程数
      batchGenerator.setThreadCount(threadCount);

      List<Map<String, Object>> generatedRecords = new ArrayList<>();

      // Act
      long startTime = System.nanoTime();

      batchGenerator.generateBatch(
          schema, totalRecords, records -> generatedRecords.addAll(records));

      long endTime = System.nanoTime();
      long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
      double recordsPerSecond = (totalRecords * 1000.0) / durationMs;

      // Report
      System.out.printf(
          "Threads=%d: %d records in %d ms (%.2f records/sec)%n",
          threadCount, totalRecords, durationMs, recordsPerSecond);

      // Assert
      assertThat(generatedRecords.size()).isEqualTo(totalRecords);
    }

    System.out.println("==============================");
  }

  /** 测试大批量数据生成性能。 */
  @Test
  @DisplayName("大批量数据生成测试 - 50万条记录")
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void largeBatchGenerationTest() {
    // Arrange
    List<FieldDefinition> fields = new ArrayList<>();
    fields.add(new FieldDefinition("id", "uuid", new SimpleFieldConfig()));

    DataSchema schema = new DataSchema(fields, new ArrayList<>());
    long totalRecords = 500000;

    // 使用流式输出避免内存溢出
    final long[] generatedCount = {0};

    // Act
    long startTime = System.nanoTime();

    batchGenerator.generateBatch(
        schema,
        totalRecords,
        records -> {
          generatedCount[0] += records.size();
          // 不存储记录，只计数，测试纯生成性能
        });

    long endTime = System.nanoTime();
    long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    double recordsPerSecond = (totalRecords * 1000.0) / durationMs;

    // Assert & Report
    System.out.printf(
        "Large Batch Generation: %d records in %d ms (%.2f records/sec)%n",
        totalRecords, durationMs, recordsPerSecond);

    assertThat(generatedCount[0]).isEqualTo(totalRecords);
    assertThat(recordsPerSecond).isGreaterThan(5000.0); // 大批量数据基线
  }
}
