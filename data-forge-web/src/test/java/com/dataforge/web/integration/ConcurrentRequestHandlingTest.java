package com.dataforge.web.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dataforge.web.api.BaseApiTest;
import com.dataforge.web.entity.GenerationHistory;
import com.dataforge.web.model.ApiResponse;
import com.dataforge.web.model.GenerateRequest;
import com.dataforge.web.repository.GenerationHistoryRepository;
import com.dataforge.web.util.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 并发请求处理集成测试。
 *
 * <p>测试系统在高并发场景下的稳定性和正确性。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("并发请求处理集成测试")
public class ConcurrentRequestHandlingTest extends BaseApiTest {

  @Autowired private ObjectMapper objectMapper;

  @Autowired private GenerationHistoryRepository historyRepository;

  @AfterEach
  void tearDown() {
    historyRepository.deleteAll();
  }

  @Nested
  @DisplayName("并发同步生成测试")
  class ConcurrentSyncGenerationTests {

    @Test
    @DisplayName("应处理并发生成请求")
    void shouldHandleConcurrentGenerationRequests() throws Exception {
      int threadCount = 10;
      int recordsPerRequest = 50;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);
      List<Future<Integer>> futures = new ArrayList<>();
      AtomicInteger successCount = new AtomicInteger(0);

      for (int i = 0; i < threadCount; i++) {
        futures.add(
            executor.submit(
                () -> {
                  try {
                    GenerateRequest request =
                        TestDataFactory.createSimpleGenerateRequest(recordsPerRequest);

                    MvcResult result =
                        mockMvc
                            .perform(
                                post("/api/v1/dataforge/generate")
                                    .header("Authorization", getAuthToken())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn();

                    int statusCode = result.getResponse().getStatus();
                    if (statusCode == 200) {
                      successCount.incrementAndGet();
                    }
                    return statusCode;
                  } catch (Exception e) {
                    return -1;
                  } finally {
                    latch.countDown();
                  }
                }));
      }

      latch.await(60, TimeUnit.SECONDS);

      for (Future<Integer> future : futures) {
        assertThat(future.get()).isEqualTo(200);
      }

      assertThat(successCount.get()).isEqualTo(threadCount);

      List<GenerationHistory> histories = historyRepository.findAll();
      assertThat(histories).hasSize(threadCount);

      executor.shutdown();
    }

    @Test
    @DisplayName("应处理大量并发请求")
    void shouldHandleLargeNumberOfConcurrentRequests() throws Exception {
      int threadCount = 20;
      int recordsPerRequest = 20;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);
      AtomicInteger successCount = new AtomicInteger(0);

      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                GenerateRequest request =
                    TestDataFactory.createSimpleGenerateRequest(recordsPerRequest);

                MvcResult result =
                    mockMvc
                        .perform(
                            post("/api/v1/dataforge/generate")
                                .header("Authorization", getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andReturn();

                int statusCode = result.getResponse().getStatus();
                if (statusCode == 200) {
                  successCount.incrementAndGet();
                }
              } catch (Exception e) {
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(120, TimeUnit.SECONDS);

      assertThat(successCount.get()).isEqualTo(threadCount);

      executor.shutdown();
    }

    @Test
    @DisplayName("应处理不同类型的并发请求")
    void shouldHandleDifferentTypesOfConcurrentRequests() throws Exception {
      int threadCount = 5;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);
      AtomicInteger successCount = new AtomicInteger(0);

      for (int i = 0; i < threadCount; i++) {
        final int index = i;
        executor.submit(
            () -> {
              try {
                GenerateRequest request;
                switch (index % 3) {
                  case 0:
                    request = TestDataFactory.createSimpleGenerateRequest(30);
                    break;
                  case 1:
                    request = TestDataFactory.createComplexGenerateRequest(20);
                    break;
                  default:
                    request =
                        TestDataFactory.createGenerateRequest(
                            25, TestDataFactory.createAllTypeFields());
                    break;
                }

                MvcResult result =
                    mockMvc
                        .perform(
                            post("/api/v1/dataforge/generate")
                                .header("Authorization", getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andReturn();

                int statusCode = result.getResponse().getStatus();
                if (statusCode == 200) {
                  successCount.incrementAndGet();
                }
              } catch (Exception e) {
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(90, TimeUnit.SECONDS);

      assertThat(successCount.get()).isEqualTo(threadCount);

      executor.shutdown();
    }
  }

  @Nested
  @DisplayName("并发异步生成测试")
  class ConcurrentAsyncGenerationTests {

    @Test
    @DisplayName("应处理并发异步任务提交")
    void shouldHandleConcurrentAsyncTaskSubmission() throws Exception {
      int threadCount = 10;
      int recordsPerRequest = 100;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);
      AtomicInteger successCount = new AtomicInteger(0);

      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                GenerateRequest request =
                    TestDataFactory.createSimpleGenerateRequest(recordsPerRequest);

                MvcResult result =
                    mockMvc
                        .perform(
                            post("/api/v1/dataforge/generate/async")
                                .header("Authorization", getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andReturn();

                int statusCode = result.getResponse().getStatus();
                if (statusCode == 200) {
                  successCount.incrementAndGet();
                }
              } catch (Exception e) {
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(60, TimeUnit.SECONDS);

      assertThat(successCount.get()).isEqualTo(threadCount);

      executor.shutdown();
    }

    @Test
    @DisplayName("应能同时查询多个任务状态")
    void shouldQueryMultipleTaskStatusesConcurrently() throws Exception {
      int taskCount = 5;
      List<Long> taskIds = new ArrayList<>();

      // 首先提交异步任务并获取任务ID
      for (int i = 0; i < taskCount; i++) {
        GenerateRequest request = TestDataFactory.createSimpleGenerateRequest(50);

        MvcResult result =
            mockMvc
                .perform(
                    post("/api/v1/dataforge/generate/async")
                        .header("Authorization", getAuthToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        // 使用 ObjectMapper 解析响应获取任务ID
        // 注意: Jackson 默认将小整数反序列化为 Integer，需要用 Number 类型接收
        @SuppressWarnings("unchecked")
        ApiResponse<Number> response = objectMapper.readValue(content, ApiResponse.class);
        Number taskIdNumber = response.getData();
        Long taskId = taskIdNumber != null ? taskIdNumber.longValue() : null;
        taskIds.add(taskId);
      }

      // 验证所有任务ID都已成功提取
      assertThat(taskIds).doesNotContainNull();
      assertThat(taskIds).doesNotContain(0L);

      // 并发查询所有任务状态
      ExecutorService executor = Executors.newFixedThreadPool(taskCount);
      CountDownLatch latch = new CountDownLatch(taskCount);
      AtomicInteger successCount = new AtomicInteger(0);

      for (Long taskId : taskIds) {
        executor.submit(
            () -> {
              try {
                mockMvc
                    .perform(
                        get("/api/v1/dataforge/tasks/" + taskId)
                            .header("Authorization", getAuthToken()))
                    .andExpect(status().isOk());
                successCount.incrementAndGet();
              } catch (Exception e) {
                // 记录异常以便调试
                e.printStackTrace();
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(30, TimeUnit.SECONDS);

      assertThat(successCount.get()).isEqualTo(taskCount);

      executor.shutdown();
    }
  }

  @Nested
  @DisplayName("混合并发请求测试")
  class MixedConcurrentRequestsTests {

    @Test
    @DisplayName("应同时处理同步和异步请求")
    void shouldHandleSyncAndAsyncRequestsSimultaneously() throws Exception {
      int totalRequests = 10;
      ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
      CountDownLatch latch = new CountDownLatch(totalRequests);
      AtomicInteger successCount = new AtomicInteger(0);

      for (int i = 0; i < totalRequests; i++) {
        final boolean isAsync = i % 2 == 0;
        executor.submit(
            () -> {
              try {
                GenerateRequest request = TestDataFactory.createSimpleGenerateRequest(50);
                String endpoint =
                    isAsync ? "/api/v1/dataforge/generate/async" : "/api/v1/dataforge/generate";

                MvcResult result =
                    mockMvc
                        .perform(
                            post(endpoint)
                                .header("Authorization", getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andReturn();

                int statusCode = result.getResponse().getStatus();
                if (statusCode == 200) {
                  successCount.incrementAndGet();
                }
              } catch (Exception e) {
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(90, TimeUnit.SECONDS);

      assertThat(successCount.get()).isEqualTo(totalRequests);

      executor.shutdown();
    }

    @Test
    @DisplayName("应处理不同负载的并发请求")
    void shouldHandleConcurrentRequestsWithDifferentLoads() throws Exception {
      int threadCount = 6;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);
      AtomicInteger successCount = new AtomicInteger(0);

      int[] recordCounts = {10, 50, 100, 200, 500, 1000};

      for (int i = 0; i < threadCount; i++) {
        final int recordCount = recordCounts[i];
        executor.submit(
            () -> {
              try {
                GenerateRequest request = TestDataFactory.createSimpleGenerateRequest(recordCount);

                MvcResult result =
                    mockMvc
                        .perform(
                            post("/api/v1/dataforge/generate")
                                .header("Authorization", getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andReturn();

                int statusCode = result.getResponse().getStatus();
                if (statusCode == 200) {
                  successCount.incrementAndGet();
                }
              } catch (Exception e) {
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(120, TimeUnit.SECONDS);

      assertThat(successCount.get()).isEqualTo(threadCount);

      executor.shutdown();
    }
  }

  @Nested
  @DisplayName("并发错误处理测试")
  class ConcurrentErrorHandlingTests {

    @Test
    @DisplayName("应正确处理并发中的错误请求")
    void shouldHandleErrorRequestsInConcurrency() throws Exception {
      int totalRequests = 10;
      ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
      CountDownLatch latch = new CountDownLatch(totalRequests);
      AtomicInteger successCount = new AtomicInteger(0);
      AtomicInteger errorCount = new AtomicInteger(0);

      for (int i = 0; i < totalRequests; i++) {
        final boolean isInvalid = i % 3 == 0;
        executor.submit(
            () -> {
              try {
                GenerateRequest request;
                if (isInvalid) {
                  request = new GenerateRequest();
                  request.setCount(-1);
                } else {
                  request = TestDataFactory.createSimpleGenerateRequest(20);
                }

                MvcResult result =
                    mockMvc
                        .perform(
                            post("/api/v1/dataforge/generate")
                                .header("Authorization", getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andReturn();

                int statusCode = result.getResponse().getStatus();
                if (statusCode == 200) {
                  successCount.incrementAndGet();
                } else if (statusCode == 400) {
                  errorCount.incrementAndGet();
                }
              } catch (Exception e) {
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(60, TimeUnit.SECONDS);

      // i % 3 == 0 意味着索引 0, 3, 6, 9 会被标记为无效请求，共4个错误请求
      int invalidRequests = (totalRequests + 2) / 3;  // 向上取整计算
      int validRequests = totalRequests - invalidRequests;
      assertThat(successCount.get()).isEqualTo(validRequests);
      assertThat(errorCount.get()).isEqualTo(invalidRequests);

      executor.shutdown();
    }

    @Test
    @DisplayName("应保持系统稳定性在并发错误场景下")
    void shouldMaintainStabilityUnderConcurrentErrors() throws Exception {
      int totalRequests = 15;
      ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
      CountDownLatch latch = new CountDownLatch(totalRequests);
      AtomicInteger successCount = new AtomicInteger(0);

      for (int i = 0; i < totalRequests; i++) {
        final boolean isValid = i % 2 == 0;
        executor.submit(
            () -> {
              try {
                GenerateRequest request;
                if (isValid) {
                  request = TestDataFactory.createSimpleGenerateRequest(30);
                } else {
                  request = new GenerateRequest();
                  request.setCount(0);
                }

                MvcResult result =
                    mockMvc
                        .perform(
                            post("/api/v1/dataforge/generate")
                                .header("Authorization", getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andReturn();

                int statusCode = result.getResponse().getStatus();
                if (statusCode == 200) {
                  successCount.incrementAndGet();
                }
              } catch (Exception e) {
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(90, TimeUnit.SECONDS);

      int validRequests = (totalRequests + 1) / 2;
      assertThat(successCount.get()).isEqualTo(validRequests);

      executor.shutdown();
    }
  }

  @Nested
  @DisplayName("并发性能测试")
  class ConcurrentPerformanceTests {

    @Test
    @DisplayName("应在合理时间内完成并发请求")
    void shouldCompleteConcurrentRequestsInReasonableTime() throws Exception {
      int threadCount = 10;
      int recordsPerRequest = 50;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                GenerateRequest request =
                    TestDataFactory.createSimpleGenerateRequest(recordsPerRequest);

                mockMvc
                    .perform(
                        post("/api/v1/dataforge/generate")
                            .header("Authorization", getAuthToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn();
              } catch (Exception e) {
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(120, TimeUnit.SECONDS);
      long duration = System.currentTimeMillis() - startTime;

      assertThat(duration).isLessThan(60000);

      executor.shutdown();
    }

    @Test
    @DisplayName("应支持持续并发请求")
    void shouldSupportSustainedConcurrentRequests() throws Exception {
      int rounds = 3;
      int requestsPerRound = 5;

      for (int round = 0; round < rounds; round++) {
        ExecutorService executor = Executors.newFixedThreadPool(requestsPerRound);
        CountDownLatch latch = new CountDownLatch(requestsPerRound);

        for (int i = 0; i < requestsPerRound; i++) {
          executor.submit(
              () -> {
                try {
                  GenerateRequest request = TestDataFactory.createSimpleGenerateRequest(30);

                  mockMvc
                      .perform(
                          post("/api/v1/dataforge/generate")
                              .header("Authorization", getAuthToken())
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
                      .andReturn();
                } catch (Exception e) {
                } finally {
                  latch.countDown();
                }
              });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
      }

      List<GenerationHistory> histories = historyRepository.findAll();
      assertThat(histories).hasSize(rounds * requestsPerRound);
    }
  }
}
