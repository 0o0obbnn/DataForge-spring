package com.dataforge.web.controller;

import com.dataforge.config.ForgeConfig;
import com.dataforge.service.DataForgeService;
import com.dataforge.web.entity.GenerationHistory;
import com.dataforge.web.model.ApiResponse;
import com.dataforge.web.model.GenerateRequest;
import com.dataforge.web.service.AsyncDataGenerationService;
import com.dataforge.web.service.GenerationHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DataForge数据生成控制器。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/dataforge")
@Tag(name = "DataForge", description = "数据生成相关API")
@Validated
public class DataForgeController extends BaseController {

  @Autowired private DataForgeService dataForgeService;

  @Autowired private AsyncDataGenerationService asyncDataGenerationService;

  @Autowired private GenerationHistoryService generationHistoryService;

  @Autowired private com.dataforge.web.service.MetricsService metricsService;

  /**
   * 将GenerateRequest转换为ForgeConfig
   *
   * @param request 生成请求
   * @return ForgeConfig配置对象
   */
  private ForgeConfig convertToForgeConfig(GenerateRequest request) {
    ForgeConfig config = new ForgeConfig();
    config.setCount(request.getCount());
    config.setOutput(request.getOutput());
    config.setFields(request.getFields());
    config.setValidate(request.isValidate());
    config.setThreads(request.getThreads());
    config.setSeed(request.getSeed());
    return config;
  }

  /**
   * 处理异步任务执行
   *
   * @param future 异步任务
   * @return ResponseEntity
   */
  private ResponseEntity<ApiResponse<Long>> handleAsyncTask(
      CompletableFuture<GenerationHistory> future) {
    try {
      GenerationHistory history = future.join();
      return buildSuccessResponse(history.getId(), "Task submitted successfully");
    } catch (Exception e) {
      return buildInternalErrorResponse("Failed to submit task: " + e.getMessage());
    }
  }

  /**
   * 生成测试数据（同步模式）。
   *
   * <p>根据提供的配置同步生成指定数量的测试数据。该方法会阻塞直到数据生成完成。 适用于小批量数据生成场景。对于大批量数据，建议使用异步接口。
   *
   * @param request 数据生成请求，包含字段配置、输出格式、记录数量等参数
   * @return ResponseEntity<ApiResponse<String>> 生成结果
   */
  @PostMapping("/generate")
  @Operation(
      summary = "生成测试数据（同步）",
      description =
          "根据配置同步生成指定数量的测试数据。该方法会阻塞直到数据生成完成。"
              + "支持多种数据生成器（UUID、姓名、邮箱、电话等），支持多种输出格式（CSV、JSON、SQL等）。"
              + "适用于小批量数据生成场景（建议 < 10万条）。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "成功生成数据",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "参数验证失败，如记录数量小于1、字段配置为空、输出配置无效等",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误，如数据生成失败、输出策略异常等",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<String>> generateData(
      @Parameter(description = "数据生成请求，包含字段配置、输出格式、记录数量等", required = true) @RequestBody @Validated
          GenerateRequest request) {

    long startTime = System.currentTimeMillis();
    metricsService.recordGenerationRequest();

    try {
      ForgeConfig config = convertToForgeConfig(request);
      dataForgeService.generateData(config);

      long duration = System.currentTimeMillis() - startTime;
      metricsService.recordGenerationSuccess(request.getCount(), duration);

      return buildSuccessResponse(null, "Data generated successfully");
    } catch (Exception e) {
      metricsService.recordGenerationFailure();
      throw e;
    }
  }

  /**
   * 异步生成测试数据。
   *
   * <p>根据提供的配置异步生成指定数量的测试数据。该方法立即返回任务ID，数据生成在后台进行。 适用于大批量数据生成场景。可通过任务ID查询生成进度和状态。
   *
   * @param request 数据生成请求，包含字段配置、输出格式、记录数量等参数
   * @return ResponseEntity<ApiResponse<Long>> 任务ID，可用于查询任务状态
   */
  @PostMapping("/generate/async")
  @Operation(
      summary = "异步生成测试数据",
      description =
          "根据配置异步生成指定数量的测试数据。该方法立即返回任务ID，数据生成在后台进行。"
              + "适用于大批量数据生成场景（建议 > 10万条）。"
              + "可通过返回的任务ID调用 /api/v1/dataforge/tasks/{taskId} 接口查询任务状态。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "任务已提交，返回任务ID",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "参数验证失败，如记录数量小于1、字段配置为空、输出配置无效等",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误，如任务提交失败等",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<Long>> generateDataAsync(
      @Parameter(description = "数据生成请求，包含字段配置、输出格式、记录数量等", required = true) @RequestBody @Validated
          GenerateRequest request) {

    CompletableFuture<GenerationHistory> future =
        asyncDataGenerationService.generateDataAsync(
            "custom-template", request.getCount(), request.toString());

    return handleAsyncTask(future);
  }

  /**
   * 按模板ID异步生成测试数据。
   *
   * <p>根据已保存的数据模板异步生成指定数量的测试数据。模板包含预定义的字段配置和输出格式。 该方法立即返回任务ID，数据生成在后台进行。记录数量可通过请求参数覆盖模板中的默认值。
   *
   * @param templateId 模板ID，必须是已存在的模板
   * @param request 数据生成请求，主要使用其中的记录数量参数
   * @return ResponseEntity<ApiResponse<Long>> 任务ID，可用于查询任务状态
   */
  @PostMapping("/generate/template/{templateId}")
  @Operation(
      summary = "按模板ID异步生成测试数据",
      description =
          "根据已保存的数据模板异步生成指定数量的测试数据。" + "模板包含预定义的字段配置和输出格式，记录数量可通过请求参数指定。" + "适用于需要重复使用相同配置的场景。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "任务已提交，返回任务ID",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "参数验证失败，如记录数量小于1、模板ID无效等",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "模板不存在",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误，如任务提交失败、模板配置解析失败等",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<Long>> generateDataByTemplateIdAsync(
      @Parameter(description = "模板ID，必须是已存在的模板", required = true, example = "1") @PathVariable
          Long templateId,
      @Parameter(description = "数据生成请求，主要使用其中的记录数量参数", required = true) @RequestBody @Validated
          GenerateRequest request) {

    CompletableFuture<GenerationHistory> future =
        asyncDataGenerationService.generateDataByTemplateIdAsync(templateId, request.getCount());

    return handleAsyncTask(future);
  }

  /**
   * 获取生成任务状态。
   *
   * <p>根据任务ID查询异步数据生成任务的状态信息，包括任务状态（IN_PROGRESS、COMPLETED、FAILED）、 生成记录数、耗时、错误信息等。
   *
   * @param taskId 任务ID，由异步生成接口返回
   * @return ResponseEntity<ApiResponse<GenerationHistory>> 任务状态信息
   */
  @GetMapping("/tasks/{taskId}")
  @Operation(
      summary = "获取生成任务状态",
      description =
          "根据任务ID获取异步数据生成任务的状态信息。"
              + "返回信息包括：任务状态（IN_PROGRESS、COMPLETED、FAILED）、生成记录数、耗时、完成时间、错误信息等。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "成功获取任务状态",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "任务不存在，任务ID无效",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<GenerationHistory>> getTaskStatus(
      @Parameter(description = "任务ID，由异步生成接口返回", required = true, example = "1") @PathVariable
          Long taskId) {
    try {
      GenerationHistory history = generationHistoryService.getHistoryById(taskId);
      return buildSuccessResponse(history, "Task status retrieved successfully");
    } catch (IllegalArgumentException e) {
      return buildNotFoundResponse("Task not found with id: " + taskId);
    } catch (Exception e) {
      return buildInternalErrorResponse("Failed to retrieve task status: " + e.getMessage());
    }
  }

  /**
   * 获取最近的生成任务。
   *
   * <p>获取最近10条数据生成任务的历史记录，包括已完成、进行中和失败的任务。 可用于监控数据生成任务的执行情况。
   *
   * @return ResponseEntity<ApiResponse<List<GenerationHistory>>> 最近的生成任务列表（最多10条）
   */
  @GetMapping("/tasks")
  @Operation(
      summary = "获取最近的生成任务",
      description = "获取最近10条数据生成任务的历史记录，按创建时间倒序排列。" + "包括已完成、进行中和失败的任务，可用于监控数据生成任务的执行情况。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "成功获取任务列表",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<List<GenerationHistory>>> getRecentTasks() {
    try {
      List<GenerationHistory> histories = generationHistoryService.getRecentHistories(10);
      return buildSuccessResponse(histories, "Recent tasks retrieved successfully");
    } catch (Exception e) {
      return buildInternalErrorResponse("Failed to retrieve recent tasks: " + e.getMessage());
    }
  }
}
