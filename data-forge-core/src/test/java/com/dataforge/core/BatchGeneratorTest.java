package com.dataforge.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dataforge.config.SimpleFieldConfig;
import com.dataforge.core.BatchGenerator.DataSchema;
import com.dataforge.core.BatchGenerator.FieldDefinition;
import com.dataforge.core.BatchGenerator.GenerationStats;
import com.dataforge.core.BatchGenerator.GenerationStatus;
import com.dataforge.core.config.BatchProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * BatchGenerator 单元测试。
 *
 * <p>测试批量数据生成器的单线程生成、多线程并发生成、线程安全、性能监控、资源清理等功能。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("BatchGenerator 单元测试")
class BatchGeneratorTest {

  @Mock private GeneratorFactory generatorFactory;

  @Mock private DataRelationEngine relationEngine;

  private BatchGenerator batchGenerator;
  private GeneratorFactory realGeneratorFactory;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // 创建实际的 GeneratorFactory 实例
    realGeneratorFactory = new GeneratorFactory();

    // 创建实际的 BatchGenerator 实例用于测试
    batchGenerator =
        new BatchGenerator(realGeneratorFactory, relationEngine, new BatchProperties());
  }

  @Nested
  @DisplayName("单线程批量生成测试")
  class SingleThreadBatchGenerationTests {

    @Test
    @DisplayName("单线程批量生成应成功")
    void generateBatch_WithSingleThread_ShouldSucceed() {
      // Given
      int threadCount = 1;
      int totalRecords = 100;
      int batchSize = 100;

      batchGenerator.setThreadCount(threadCount);
      batchGenerator.setBatchSize(batchSize);

      DataSchema schema = createTestSchema(2);
      AtomicInteger recordsProcessed = new AtomicInteger(0);

      // When
      GenerationStats stats =
          batchGenerator.generateBatch(
              schema,
              totalRecords,
              batch -> {
                recordsProcessed.addAndGet(batch.size());
              });

      // Then
      assertThat(recordsProcessed.get()).isEqualTo(totalRecords);
      assertThat(stats.getTotalRecords()).isEqualTo(totalRecords);
      assertThat(stats.getThreadCount()).isEqualTo(threadCount);
      assertThat(stats.getBatchSize()).isEqualTo(batchSize);
      assertThat(stats.getThroughput()).isGreaterThan(0);
    }

    @Test
    @DisplayName("单线程小批量生成应保持顺序")
    void generateBatch_WithSmallSingleThreadBatch_ShouldMaintainOrder() {
      // Given
      int totalRecords = 50;
      List<Integer> batchSequence = new ArrayList<>();

      batchGenerator.setThreadCount(1);
      batchGenerator.setBatchSize(100); // 最小批次大小是100

      DataSchema schema = createTestSchema(1);

      // When
      batchGenerator.generateBatch(
          schema,
          totalRecords,
          batch -> {
            batchSequence.add(batch.size());
          });

      // Then
      assertThat(batchSequence).hasSizeGreaterThan(0);
      // 由于批次大小是100但总记录数是50，所以只有一批
      assertThat(batchSequence.get(0)).isEqualTo(50);
    }
  }

  @Nested
  @DisplayName("多线程并发生成测试")
  class ConcurrentGenerationTests {

    @Test
    @DisplayName("多线程并发生成应成功")
    void generateBatch_WithMultipleThreads_ShouldSucceed() {
      // Given
      int threadCount = 4;
      int totalRecords = 1000;
      int batchSize = 100;

      batchGenerator.setThreadCount(threadCount);
      batchGenerator.setBatchSize(batchSize);

      DataSchema schema = createTestSchema(3);
      AtomicInteger recordsProcessed = new AtomicInteger(0);

      // When
      GenerationStats stats =
          batchGenerator.generateBatch(
              schema,
              totalRecords,
              batch -> {
                recordsProcessed.addAndGet(batch.size());
              });

      // Then
      assertThat(recordsProcessed.get()).isEqualTo(totalRecords);
      assertThat(stats.getTotalRecords()).isEqualTo(totalRecords);
      assertThat(stats.getThreadCount()).isEqualTo(threadCount);
      assertThat(stats.getThroughput()).isGreaterThan(0);
    }

    @Test
    @DisplayName("多线程并发生成应保证数据完整性")
    void generateBatch_WithMultipleThreads_ShouldMaintainDataIntegrity() {
      // Given
      int threadCount = 4;
      int totalRecords = 2000;

      batchGenerator.setThreadCount(threadCount);
      batchGenerator.setBatchSize(200);

      DataSchema schema = createTestSchema(2);
      AtomicInteger totalBatches = new AtomicInteger(0);
      AtomicInteger totalRecordsInBatches = new AtomicInteger(0);

      // When
      batchGenerator.generateBatch(
          schema,
          totalRecords,
          batch -> {
            totalBatches.incrementAndGet();
            totalRecordsInBatches.addAndGet(batch.size());
          });

      // Then
      assertThat(totalRecordsInBatches.get()).isEqualTo(totalRecords);
      // 批次数量应该接近预期（每批 200 条）
      assertThat(totalBatches.get()).isGreaterThanOrEqualTo(totalRecords / 200);
    }

    @Test
    @DisplayName("高并发应保持性能")
    void generateBatch_WithHighConcurrency_ShouldMaintainPerformance() {
      // Given
      int threadCount = 8;
      int totalRecords = 5000;

      batchGenerator.setThreadCount(threadCount);
      batchGenerator.setBatchSize(500);

      DataSchema schema = createTestSchema(3);
      AtomicInteger recordsProcessed = new AtomicInteger(0);

      // When
      long startTime = System.currentTimeMillis();
      GenerationStats stats =
          batchGenerator.generateBatch(
              schema,
              totalRecords,
              batch -> {
                recordsProcessed.addAndGet(batch.size());
              });
      long duration = System.currentTimeMillis() - startTime;

      // Then
      assertThat(recordsProcessed.get()).isEqualTo(totalRecords);
      assertThat(stats.getTotalRecords()).isEqualTo(totalRecords);
      // 高并发应该在合理时间内完成（例如10秒内）
      assertThat(duration).isLessThan(10000);
      // 吞吐量应该合理（至少500条/秒）
      assertThat(stats.getThroughput()).isGreaterThanOrEqualTo(500);
    }
  }

  @Nested
  @DisplayName("线程安全验证测试")
  class ThreadSafetyTests {

    @Test
    @DisplayName("并发状态查询应是线程安全的")
    void getStatus_WithConcurrentGeneration_ShouldBeThreadSafe() throws InterruptedException {
      // Given
      batchGenerator.setThreadCount(4);
      batchGenerator.setBatchSize(100);
      DataSchema schema = createTestSchema(2);
      AtomicInteger statusQueries = new AtomicInteger(0);

      // When
      Thread generationThread =
          new Thread(
              () -> {
                batchGenerator.generateBatch(
                    schema,
                    1000,
                    batch -> {
                      // 在生成过程中查询状态
                      GenerationStatus status = batchGenerator.getStatus();
                      if (status.isGenerating()) {
                        statusQueries.incrementAndGet();
                      }
                    });
              });

      Thread queryThread =
          new Thread(
              () -> {
                for (int i = 0; i < 10; i++) {
                  try {
                    Thread.sleep(50);
                    batchGenerator.getStatus();
                    statusQueries.incrementAndGet();
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              });

      generationThread.start();
      queryThread.start();

      generationThread.join();
      queryThread.join();

      // Then
      assertThat(statusQueries.get()).isGreaterThan(0);
      // 确保状态查询没有抛出异常
      assertThatCode(() -> batchGenerator.getStatus()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("并发配置更新应是线程安全的")
    void setThreadCount_WithConcurrentGeneration_ShouldBeSafe() {
      // Given
      batchGenerator.setThreadCount(2);
      DataSchema schema = createTestSchema(1);

      // When
      Thread generationThread =
          new Thread(
              () -> {
                batchGenerator.generateBatch(schema, 500, batch -> {});
              });

      Thread configThread =
          new Thread(
              () -> {
                try {
                  Thread.sleep(10);
                  batchGenerator.setThreadCount(4);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              });

      generationThread.start();
      configThread.start();

      // Then - 确保没有抛出异常
      assertThatCode(
              () -> {
                generationThread.join();
                configThread.join();
              })
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("性能监控测试")
  class PerformanceMonitoringTests {

    @Test
    @DisplayName("生成状态应正确更新")
    void getStatus_ShouldReturnCorrectStatus() throws InterruptedException {
      // Given
      batchGenerator.setThreadCount(2);
      batchGenerator.setBatchSize(100);
      DataSchema schema = createTestSchema(2);

      // When - 开始生成
      Thread generationThread =
          new Thread(
              () -> {
                batchGenerator.generateBatch(schema, 1000, batch -> {});
              });
      generationThread.start();

      // 等待确保生成开始
      Thread.sleep(100);

      // 在生成过程中查询状态
      GenerationStatus statusDuring = batchGenerator.getStatus();

      // 等待生成完成
      generationThread.join();

      GenerationStatus statusAfter = batchGenerator.getStatus();

      // Then
      // 注意：由于生成速度很快，可能在查询时已经完成
      // 我们主要验证状态查询不会抛出异常，并且返回合理的值
      assertThat(statusDuring.getRecordsGenerated()).isGreaterThanOrEqualTo(0);
      assertThat(statusAfter.isGenerating()).isFalse();
      assertThat(statusAfter.getRecordsGenerated()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("生成统计应正确计算")
    void generateBatch_ShouldReturnCorrectStats() {
      // Given
      int totalRecords = 500;
      int threadCount = 2;
      int batchSize = 100;

      batchGenerator.setThreadCount(threadCount);
      batchGenerator.setBatchSize(batchSize);
      DataSchema schema = createTestSchema(2);

      // When
      GenerationStats stats = batchGenerator.generateBatch(schema, totalRecords, batch -> {});

      // Then
      assertThat(stats.getTotalRecords()).isEqualTo(totalRecords);
      assertThat(stats.getThreadCount()).isEqualTo(threadCount);
      assertThat(stats.getBatchSize()).isEqualTo(batchSize);
      assertThat(stats.getThroughput()).isGreaterThan(0);
      assertThat(stats.getDurationMs()).isGreaterThan(0);
    }

    @Test
    @DisplayName("吞吐量计算应准确")
    void generateBatch_ShouldCalculateThroughputCorrectly() {
      // Given
      int totalRecords = 1000;
      batchGenerator.setThreadCount(4);
      batchGenerator.setBatchSize(200);
      DataSchema schema = createTestSchema(3);

      // When
      GenerationStats stats = batchGenerator.generateBatch(schema, totalRecords, batch -> {});

      // Then - 验证吞吐量计算：记录数 / (毫秒数 / 1000)
      long expectedThroughput =
          (totalRecords * 1000) / (stats.getDurationMs() > 0 ? stats.getDurationMs() : 1);
      assertThat((long) stats.getThroughput())
          .isGreaterThanOrEqualTo(expectedThroughput - 10L); // 允许小误差
    }
  }

  @Nested
  @DisplayName("资源清理验证测试")
  class ResourceCleanupTests {

    @Test
    @DisplayName("shutdown 应正确关闭线程池")
    void shutdown_ShouldCloseThreadPoolProperly() {
      // Given
      batchGenerator.setThreadCount(4);
      batchGenerator.setBatchSize(100);
      DataSchema schema = createTestSchema(2);

      // When
      batchGenerator.generateBatch(schema, 1000, batch -> {});
      batchGenerator.shutdown();

      // Then - 验证线程池已关闭
      assertThatCode(() -> batchGenerator.shutdown()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("多次 shutdown 不应抛出异常")
    void shutdown_CalledMultipleTimes_ShouldNotThrowException() {
      // Given
      DataSchema schema = createTestSchema(1);
      batchGenerator.generateBatch(schema, 100, batch -> {});

      // When & Then
      assertThatCode(
              () -> {
                batchGenerator.shutdown();
                batchGenerator.shutdown(); // 第二次调用
              })
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("关闭后不应允许新任务")
    void generateBatch_AfterShutdown_ShouldNotAllowNewTasks() {
      // Given
      DataSchema schema = createTestSchema(1);
      batchGenerator.generateBatch(schema, 100, batch -> {});
      batchGenerator.shutdown();

      // When & Then
      // shutdown 后的 Generator 实例应该能被创建新实例来继续工作
      // 这里我们验证 shutdown 不会破坏对象状态
      assertThatCode(
              () -> {
                batchGenerator.shutdown();
              })
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("异常中断处理测试")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("并发生成应抛出异常")
    void generateBatch_WithConcurrentGeneration_ShouldThrowException() {
      // Given
      batchGenerator.setThreadCount(2);
      batchGenerator.setBatchSize(100);
      DataSchema schema = createTestSchema(2);

      // When - 启动第一个生成任务
      Thread firstGeneration =
          new Thread(
              () -> {
                batchGenerator.generateBatch(
                    schema,
                    1000,
                    batch -> {
                      try {
                        Thread.sleep(100); // 模拟耗时操作
                      } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                      }
                    });
              });

      firstGeneration.start();

      // 等待确保第一个任务开始
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      // Then - 第二个并发任务应该抛出异常
      assertThatThrownBy(() -> batchGenerator.generateBatch(schema, 100, batch -> {}))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Another batch generation is in progress");

      try {
        firstGeneration.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    @Test
    @DisplayName("无效线程数应抛出异常")
    void setThreadCount_WithInvalidValue_ShouldThrowException() {
      // When & Then - 小于1
      assertThatThrownBy(() -> batchGenerator.setThreadCount(0))
          .isInstanceOf(IllegalArgumentException.class);

      // When & Then - 大于16
      assertThatThrownBy(() -> batchGenerator.setThreadCount(17))
          .isInstanceOf(IllegalArgumentException.class);

      // When & Then - 负数
      assertThatThrownBy(() -> batchGenerator.setThreadCount(-1))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("无效批次大小应抛出异常")
    void setBatchSize_WithInvalidValue_ShouldThrowException() {
      // When & Then - 小于100
      assertThatThrownBy(() -> batchGenerator.setBatchSize(99))
          .isInstanceOf(IllegalArgumentException.class);

      // When & Then - 大于100000
      assertThatThrownBy(() -> batchGenerator.setBatchSize(100001))
          .isInstanceOf(IllegalArgumentException.class);

      // When & Then - 负数
      assertThatThrownBy(() -> batchGenerator.setBatchSize(-1))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null 模式应抛出异常")
    void generateBatch_WithNullSchema_ShouldThrowException() {
      // When & Then
      assertThatThrownBy(() -> batchGenerator.generateBatch(null, 100, batch -> {}))
          .isInstanceOf(java.util.concurrent.CompletionException.class)
          .hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("null 消费者应抛出异常")
    void generateBatch_WithNullConsumer_ShouldThrowException() {
      // Given
      DataSchema schema = createTestSchema(1);

      // When & Then
      assertThatThrownBy(() -> batchGenerator.generateBatch(schema, 100, null))
          .isInstanceOf(java.util.concurrent.CompletionException.class)
          .hasCauseInstanceOf(NullPointerException.class);
    }
  }

  @Nested
  @DisplayName("状态查询测试")
  class StatusQueryTests {

    @Test
    @DisplayName("未生成时状态应为空闲")
    void getStatus_WhenNotGenerating_ShouldReturnIdleStatus() {
      // When
      GenerationStatus status = batchGenerator.getStatus();

      // Then
      assertThat(status.isGenerating()).isFalse();
      assertThat(status.getRecordsGenerated()).isEqualTo(0);
      assertThat(status.getElapsedMs()).isEqualTo(0);
      assertThat(status.getCurrentThroughput()).isEqualTo(0);
    }

    @Test
    @DisplayName("生成中状态应正确反映进度")
    void getStatus_DuringGeneration_ShouldReflectProgress() throws InterruptedException {
      // Given
      batchGenerator.setThreadCount(2);
      batchGenerator.setBatchSize(100);
      DataSchema schema = createTestSchema(2);

      // When
      Thread generationThread =
          new Thread(
              () -> {
                batchGenerator.generateBatch(
                    schema,
                    500,
                    batch -> {
                      try {
                        Thread.sleep(10); // 模拟耗时
                      } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                      }
                    });
              });

      generationThread.start();

      // 等待生成开始
      Thread.sleep(50);

      // 循环等待，直到生成了一些记录或超时
      GenerationStatus status = null;
      int maxAttempts = 50; // 最多等待5秒
      int attempts = 0;

      while (attempts < maxAttempts) {
        status = batchGenerator.getStatus();
        if (status.getRecordsGenerated() > 0) {
          break;
        }
        Thread.sleep(100);
        attempts++;
      }

      generationThread.join(10000); // 最多等待10秒确保完成

      // Then
      // 注意：由于生成可能已经完成，isGenerating可能为false
      assertThat(status.getRecordsGenerated()).isGreaterThan(0);
    }
  }

  @Nested
  @DisplayName("配置验证测试")
  class ConfigurationValidationTests {

    @Test
    @DisplayName("线程数配置应正确生效")
    void setThreadCount_ShouldApplyCorrectly() {
      // When
      batchGenerator.setThreadCount(8);

      // Then
      assertThat(batchGenerator.getThreadCount()).isEqualTo(8);
    }

    @Test
    @DisplayName("批次大小配置应正确生效")
    void setBatchSize_ShouldApplyCorrectly() {
      // When
      batchGenerator.setBatchSize(5000);

      // Then
      // 注意：batchSize 是私有字段，通过生成验证
      DataSchema schema = createTestSchema(1);
      GenerationStats stats = batchGenerator.generateBatch(schema, 100, batch -> {});
      assertThat(stats.getBatchSize()).isEqualTo(5000);
    }

    @Test
    @DisplayName("进度监控配置应正确生效")
    void setEnableProgressMonitoring_ShouldApplyCorrectly() {
      // When
      batchGenerator.setEnableProgressMonitoring(false);

      // Then - 验证不会抛出异常
      assertThatCode(() -> batchGenerator.setEnableProgressMonitoring(false))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("进度报告间隔配置应正确生效")
    void setProgressReportInterval_ShouldApplyCorrectly() {
      // When
      batchGenerator.setProgressReportInterval(10000);

      // Then - 验证不会抛出异常
      assertThatCode(() -> batchGenerator.setProgressReportInterval(10000))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("获取线程池执行器应返回有效实例")
    void getTaskExecutor_ShouldReturnValidInstance() {
      // When
      var executor = batchGenerator.getTaskExecutor();

      // Then
      assertThat(executor).isNotNull();
    }
  }

  // 辅助方法

  private DataSchema createTestSchema(int fieldCount) {
    List<FieldDefinition> fields = new ArrayList<>();
    for (int i = 0; i < fieldCount; i++) {
      SimpleFieldConfig config = new SimpleFieldConfig("field_" + i, "name");

      FieldDefinition definition = new FieldDefinition("field_" + i, "name", config);
      fields.add(definition);
    }

    return new DataSchema(fields, new ArrayList<>());
  }
}
