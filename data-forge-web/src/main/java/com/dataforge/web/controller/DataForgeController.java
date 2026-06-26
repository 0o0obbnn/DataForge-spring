package com.dataforge.web.controller;

import com.dataforge.config.ForgeConfig;
import com.dataforge.service.DataForgeService;
import com.dataforge.web.config.DataForgeTasksProperties;
import com.dataforge.web.entity.GenerationHistory;
import com.dataforge.web.model.ApiResponse;
import com.dataforge.web.model.GeneratePreviewRequest;
import com.dataforge.web.model.GenerateRequest;
import com.dataforge.web.service.AsyncDataGenerationService;
import com.dataforge.web.service.GenerationHistoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  private final DataForgeService dataForgeService;
  private final AsyncDataGenerationService asyncDataGenerationService;
  private final GenerationHistoryService generationHistoryService;
  private final com.dataforge.web.service.MetricsService metricsService;
  private final DataForgeTasksProperties tasksProperties;
  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  public DataForgeController(
      DataForgeService dataForgeService,
      AsyncDataGenerationService asyncDataGenerationService,
      GenerationHistoryService generationHistoryService,
      com.dataforge.web.service.MetricsService metricsService,
      DataForgeTasksProperties tasksProperties) {
    this.dataForgeService = dataForgeService;
    this.asyncDataGenerationService = asyncDataGenerationService;
    this.generationHistoryService = generationHistoryService;
    this.metricsService = metricsService;
    this.tasksProperties = tasksProperties;
  }

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
   * 将GenerateRequest转换为YAML配置字符串
   *
   * @param request 生成请求
   * @return YAML格式的配置字符串
   */
  private String convertRequestToConfigString(GenerateRequest request) {
    try {
      Map<String, Object> config = new LinkedHashMap<>();
      config.put("count", request.getCount());
      config.put("validate", request.isValidate());
      config.put("threads", request.getThreads());
      config.put("seed", request.getSeed());

      // 输出配置
      if (request.getOutput() != null) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("format", request.getOutput().getFormat());
        output.put("file", request.getOutput().getFile());
        output.put("encoding", request.getOutput().getEncoding());
        config.put("output", output);
      }

      // 字段配置
      config.put("fields", request.getFields());

      // 转换为YAML
      return yamlMapper.writeValueAsString(config);

    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("无法转换配置: " + e.getMessage(), e);
    }
  }

  /**
   * 处理异步任务执行。异常由 GlobalExceptionHandler 统一映射：ResourceNotFoundException -> 404，其他 -> 500。
   *
   * @param future 异步任务
   * @return ResponseEntity
   */
  private ResponseEntity<ApiResponse<Long>> handleAsyncTask(
      CompletableFuture<GenerationHistory> future) {
    try {
      GenerationHistory history = future.join();
      return buildSuccessResponse(history.getId(), "Task submitted successfully");
    } catch (CompletionException e) {
      // Unwrap the CompletionException to get the actual cause
      if (e.getCause() instanceof RuntimeException) {
        throw (RuntimeException) e.getCause();
      }
      throw new RuntimeException(e.getCause());
    }
  }

  /**
   * 生成测试数据（同步模式）。
   *
   * <p>根据提供的配置同步生成指定数量的测试数据。该方法会阻塞直到数据生成完成。 适用于小批量数据生成场景。对于大批量数据，建议使用异步接口。
   *
   * <p>本接口不返回生成的数据内容，仅确认生成成功。数据写入请求中 output 指定的文件或流；当 output 为文件时，响应 data 中可包含 outputPath。
   *
   * @param request 数据生成请求，包含字段配置、输出格式、记录数量等参数
   * @return ResponseEntity 生成结果，data 为 Map：含 message；若输出为文件则含 outputPath
   */
  @PostMapping("/generate")
  @Operation(
      summary = "生成测试数据（同步）",
      description =
          "根据配置同步生成指定数量的测试数据。该方法会阻塞直到数据生成完成。"
              + "支持多种数据生成器（UUID、姓名、邮箱、电话等），支持多种输出格式（CSV、JSON、SQL等）。"
              + "适用于小批量数据生成场景（建议 < 10万条）。"
              + "本接口不返回生成的数据内容，仅确认成功；数据写入请求中 output 指定的文件或流。"
              + "成功时 data 包含 message；当 output 指定文件路径时，data 还包含 outputPath。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description =
            "成功生成数据。不返回生成内容；数据已写入请求 output 指定的文件或流。"
                + "data.message 为说明信息；当 output 为文件时 data.outputPath 为写入路径。",
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
  public ResponseEntity<ApiResponse<Map<String, Object>>> generateData(
      @Parameter(description = "数据生成请求，包含字段配置、输出格式、记录数量等", required = true) @RequestBody @Validated
          GenerateRequest request) {

    long startTime = System.currentTimeMillis();
    metricsService.recordGenerationRequest();

    // 创建初始历史记录
    GenerationHistory history = new GenerationHistory();
    history.setRecordCount(request.getCount());
    history.setStatus("IN_PROGRESS");

    try {
      ForgeConfig config = convertToForgeConfig(request);
      dataForgeService.generateData(config);

      long duration = System.currentTimeMillis() - startTime;
      metricsService.recordGenerationSuccess(request.getCount(), duration);

      // 更新历史记录为完成状态
      history.setStatus("COMPLETED");
      history.setDurationMs(duration);
      history.setCompletedAt(java.time.LocalDateTime.now());
      generationHistoryService.createHistory(history);

      Map<String, Object> data = new LinkedHashMap<>();
      data.put(
          "message",
          "Data generated successfully. Data written to the specified output (file or stream).");
      if (request.getOutput() != null
          && request.getOutput().getFile() != null
          && !request.getOutput().getFile().trim().isEmpty()) {
        data.put("outputPath", request.getOutput().getFile().trim());
      }
      return buildSuccessResponse(data, "Data generated successfully");
    } catch (Exception e) {
      metricsService.recordGenerationFailure();

      // 更新历史记录为失败状态
      history.setStatus("FAILED");
      history.setErrorMessage(e.getMessage());
      history.setCompletedAt(java.time.LocalDateTime.now());
      generationHistoryService.createHistory(history);

      throw e;
    }
  }

  /**
   * 生成预览数据（Web端直接展示）。
   *
   * <p>根据指定的生成器类型和数量生成测试数据，直接以JSON数组形式返回生成的数据记录。
   * 适用于Web端快速预览和测试数据生成功能，不写入文件或数据库。
   *
   * <p>预览模式下最多支持1000条记录，超出时校验失败返回400。
   *
   * @param request 预览请求，包含生成器类型、记录数量、可选参数
   * @return ResponseEntity 生成的数据记录列表，每条记录包含 "value" 字段
   */
  @PostMapping("/generate/preview")
  @Operation(
      summary = "生成预览数据（Web端展示）",
      description =
          "根据生成器类型和数量生成测试数据，直接返回JSON数组。"
              + "适用于Web端快速预览，不写入文件。最多支持1000条记录。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "成功生成预览数据，返回数据记录列表",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "参数验证失败，如生成器类型为空、记录数量超出范围等",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误，如生成器执行失败等",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> generatePreview(
      @Parameter(description = "预览请求，包含生成器类型和记录数量", required = true)
          @RequestBody @Validated GeneratePreviewRequest request) {

    List<Map<String, Object>> data =
        dataForgeService.generatePreviewData(
            request.getGeneratorType(), request.getCount(), request.getParams());

    return buildSuccessResponse(data, "Preview data generated successfully");
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

    // 将请求转换为配置字符串
    String configString = convertRequestToConfigString(request);

    CompletableFuture<GenerationHistory> future =
        asyncDataGenerationService.generateDataAsync(
            "custom-template", request.getCount(), configString);

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
      @Parameter(description = "模板ID，必须是已存在的模板", required = true, example = "1")
          @PathVariable("templateId")
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
      @Parameter(description = "任务ID，由异步生成接口返回", required = true, example = "1")
          @PathVariable("taskId")
          Long taskId) {
    GenerationHistory history = generationHistoryService.getHistoryById(taskId);
    return buildSuccessResponse(history, "Task status retrieved successfully");
  }

  /**
   * 获取所有可用的数据生成器列表。
   *
   * <p>返回系统中已注册的所有数据生成器的基本信息，包括类型标识和描述。 前端可通过此接口动态获取生成器目录，确保前后端生成器数量一致。
   *
   * @return ResponseEntity 生成器列表
   */
  @GetMapping("/generators")
  @Operation(
      summary = "获取可用生成器列表",
      description =
          "获取系统中所有已注册的数据生成器列表。"
              + "返回每个生成器的类型标识（id）和描述信息，用于前端生成器目录展示。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "成功获取生成器列表",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<List<Map<String, String>>>> getAvailableGenerators() {
    Map<String, String> generatorInfo = dataForgeService.getGeneratorInfo();
    List<Map<String, String>> generators = new ArrayList<>();

    for (Map.Entry<String, String> entry : generatorInfo.entrySet()) {
      Map<String, String> generator = new LinkedHashMap<>();
      generator.put("id", entry.getKey());
      generator.put("name", entry.getKey());
      // Extract description from the formatted info string
      String info = entry.getValue();
      String description = info;
      int descIndex = info.indexOf(" - ");
      if (descIndex >= 0) {
        int priorityIndex = info.indexOf(" (priority=");
        if (priorityIndex > descIndex) {
          description = info.substring(descIndex + 3, priorityIndex);
        } else {
          description = info.substring(descIndex + 3);
        }
      }
      generator.put("description", description);
      generators.add(generator);
    }

    // Sort by id for consistent ordering
    generators.sort((a, b) -> a.get("id").compareTo(b.get("id")));

    return buildSuccessResponse(generators, "Available generators retrieved successfully");
  }

  /**
   * 获取最近的生成任务（分页）。
   *
   * <p>按创建时间倒序分页返回数据生成任务历史记录，包括已完成、进行中和失败的任务。 可用于监控数据生成任务的执行情况。默认每页条数由配置
   * dataforge.tasks.default-page-size 指定。
   *
   * @param page 页码，从 0 开始，默认 0
   * @param size 每页条数，默认取配置 dataforge.tasks.default-page-size（默认 10）
   * @return ResponseEntity 最近的生成任务列表
   */
  @GetMapping("/tasks")
  @Operation(
      summary = "获取最近的生成任务（分页）",
      description =
          "按创建时间倒序分页获取数据生成任务的历史记录。"
              + "支持 page、size 参数；size 未指定时使用配置 dataforge.tasks.default-page-size（默认 10）。"
              + "包括已完成、进行中和失败的任务，可用于监控数据生成任务的执行情况。")
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
  public ResponseEntity<ApiResponse<List<GenerationHistory>>> getRecentTasks(
      @Parameter(description = "页码，从 0 开始", example = "0") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "每页条数，未指定时使用配置默认值") @RequestParam(required = false) Integer size) {
    int pageSize = size != null ? size : tasksProperties.getDefaultPageSize();
    Pageable pageable = PageRequest.of(page, pageSize);
    List<GenerationHistory> histories = generationHistoryService.getRecentHistories(pageable);
    return buildSuccessResponse(histories, "Recent tasks retrieved successfully");
  }
}
