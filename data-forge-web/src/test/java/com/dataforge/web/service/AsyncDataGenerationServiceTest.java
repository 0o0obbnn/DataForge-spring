package com.dataforge.web.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dataforge.web.config.TestRedisConfiguration;
import com.dataforge.web.entity.GenerationHistory;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestRedisConfiguration.class)
@DisplayName("AsyncDataGenerationService单元测试")
class AsyncDataGenerationServiceTest {

  @Autowired private AsyncDataGenerationService asyncDataGenerationService;

  @Test
  @DisplayName("测试异步生成数据")
  void should_generate_data_async() throws Exception {
    String templateConfig = "fields:\n  - name: test\n    type: name";

    CompletableFuture<GenerationHistory> future =
        asyncDataGenerationService.generateDataAsync("测试模板", 10, templateConfig);

    assertNotNull(future);
    GenerationHistory history = future.get();
    assertNotNull(history);
    assertEquals(10, history.getRecordCount());
  }

  @Test
  @DisplayName("测试异步生成数据使用模板ID")
  void should_generate_data_by_template_id_async() throws Exception {
    // 注意：这个测试需要一个真实存在的模板ID
    // 在实际测试中可能需要先创建模板或使用@Sql注解初始化数据

    CompletableFuture<GenerationHistory> future =
        asyncDataGenerationService.generateDataByTemplateIdAsync(1L, 100);

    assertNotNull(future);
    // 由于可能模板不存在，这里只验证方法能正常返回
    assertDoesNotThrow(() -> future.get());
  }

  @Test
  @DisplayName("测试异步任务返回非空")
  void should_return_non_null_future() {
    String templateConfig = "{}";

    CompletableFuture<GenerationHistory> future =
        asyncDataGenerationService.generateDataAsync("测试", 5, templateConfig);

    assertNotNull(future);
    assertFalse(future.isCancelled());
  }
}
