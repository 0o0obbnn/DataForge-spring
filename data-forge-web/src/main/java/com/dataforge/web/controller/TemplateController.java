package com.dataforge.web.controller;

import com.dataforge.web.entity.DataTemplate;
import com.dataforge.web.model.ApiResponse;
import com.dataforge.web.service.DataTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 数据模板管理控制器。
 *
 * <p>提供数据模板的CRUD操作，包括创建、查询、更新和删除模板。 模板用于保存和复用数据生成配置，提高数据生成的效率和一致性。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "Templates", description = "数据模板管理相关API，支持模板的创建、查询、更新和删除操作")
public class TemplateController extends BaseController {

  private final DataTemplateService dataTemplateService;

  public TemplateController(DataTemplateService dataTemplateService) {
    this.dataTemplateService = dataTemplateService;
  }

  /**
   * 创建数据模板。
   *
   * @param template 模板数据
   * @return ResponseEntity<ApiResponse<DataTemplate>> 创建结果
   */
  @PostMapping
  @Operation(summary = "创建数据模板", description = "创建一个新的数据生成模板。模板包含字段配置、输出格式等生成参数，可用于后续的数据生成任务。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "模板创建成功",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "请求参数无效，如模板名称已存在或配置格式错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<DataTemplate>> createTemplate(
      @Parameter(description = "模板数据，包含名称、配置等信息", required = true) @RequestBody @Valid
          DataTemplate template) {
    try {
      DataTemplate createdTemplate = dataTemplateService.createTemplate(template);
      return buildCreatedResponse(createdTemplate, "Template created successfully");
    } catch (IllegalArgumentException e) {
      return buildBadRequestResponse(e.getMessage());
    } catch (Exception e) {
      return buildInternalErrorResponse("Failed to create template");
    }
  }

  /**
   * 获取所有数据模板。
   *
   * @return ResponseEntity<ApiResponse<List<DataTemplate>>> 模板列表
   */
  @GetMapping
  @Operation(summary = "获取所有数据模板", description = "获取系统中所有已创建的数据模板列表，包括激活和未激活的模板。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "成功获取模板列表",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<List<DataTemplate>>> getAllTemplates() {
    try {
      List<DataTemplate> templates = dataTemplateService.getAllTemplates();
      return buildSuccessResponse(templates, "Templates retrieved successfully");
    } catch (Exception e) {
      return buildInternalErrorResponse("Failed to retrieve templates");
    }
  }

  /**
   * 获取激活的数据模板。
   *
   * @return ResponseEntity<ApiResponse<List<DataTemplate>>> 激活的模板列表
   */
  @GetMapping("/active")
  @Operation(summary = "获取激活的数据模板", description = "获取所有状态为激活的数据模板列表。激活的模板可以直接用于数据生成任务。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "成功获取激活模板列表",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<List<DataTemplate>>> getActiveTemplates() {
    try {
      List<DataTemplate> templates = dataTemplateService.getActiveTemplates();
      return buildSuccessResponse(templates, "Active templates retrieved successfully");
    } catch (Exception e) {
      return buildInternalErrorResponse("Failed to retrieve active templates");
    }
  }

  /**
   * 分页获取数据模板。
   *
   * @param page 页码（0开始）
   * @param size 每页大小
   * @param sort 排序字段
   * @return 分页结果
   */
  @GetMapping("/page")
  @Operation(summary = "分页获取数据模板", description = "支持分页和排序的模板查询接口")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "分页查询成功",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<DataTemplate>>>
      getTemplatesPage(
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "10") int size,
          @RequestParam(defaultValue = "id") String sort) {
    try {
      org.springframework.data.domain.Pageable pageable =
          org.springframework.data.domain.PageRequest.of(
              page, size, org.springframework.data.domain.Sort.by(sort));
      org.springframework.data.domain.Page<DataTemplate> templates =
          dataTemplateService.getTemplates(pageable);
      return buildSuccessResponse(templates, "Templates retrieved successfully");
    } catch (Exception e) {
      return buildInternalErrorResponse("Failed to retrieve templates");
    }
  }

  /**
   * 分页获取激活的数据模板。
   *
   * @param page 页码（0开始）
   * @param size 每页大小
   * @return 分页结果
   */
  @GetMapping("/active/page")
  @Operation(summary = "分页获取激活的数据模板", description = "支持分页查询激活状态的模板")
  public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<DataTemplate>>>
      getActiveTemplatesPage(
          @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    try {
      org.springframework.data.domain.Pageable pageable =
          org.springframework.data.domain.PageRequest.of(page, size);
      org.springframework.data.domain.Page<DataTemplate> templates =
          dataTemplateService.getActiveTemplates(pageable);
      return buildSuccessResponse(templates, "Active templates retrieved successfully");
    } catch (Exception e) {
      return buildInternalErrorResponse("Failed to retrieve active templates");
    }
  }

  /**
   * 根据ID获取数据模板。
   *
   * @param id 模板ID
   * @return ResponseEntity<ApiResponse<DataTemplate>> 模板信息
   */
  @GetMapping("/{id}")
  @Operation(summary = "根据ID获取数据模板", description = "根据模板ID获取指定的数据模板详细信息。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "成功获取模板信息",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "模板不存在",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<DataTemplate>> getTemplateById(
      @Parameter(description = "模板ID", required = true, example = "1") @PathVariable Long id) {
    try {
      DataTemplate template = dataTemplateService.getTemplateById(id);
      return buildSuccessResponse(template, "Template retrieved successfully");
    } catch (IllegalArgumentException e) {
      return buildNotFoundResponse(e.getMessage());
    } catch (Exception e) {
      return buildInternalErrorResponse("Failed to retrieve template");
    }
  }

  /**
   * 根据名称获取数据模板。
   *
   * @param name 模板名称
   * @return ResponseEntity<ApiResponse<DataTemplate>> 模板信息
   */
  @GetMapping("/name/{name}")
  @Operation(summary = "根据名称获取数据模板", description = "根据模板名称获取指定的数据模板详细信息。模板名称在系统中必须唯一。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "成功获取模板信息",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "模板不存在",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<DataTemplate>> getTemplateByName(
      @Parameter(description = "模板名称", required = true, example = "user-data-template")
          @PathVariable
          String name) {
    try {
      DataTemplate template = dataTemplateService.getTemplateByName(name);
      return buildSuccessResponse(template, "Template retrieved successfully");
    } catch (IllegalArgumentException e) {
      return buildNotFoundResponse(e.getMessage());
    } catch (Exception e) {
      return buildInternalErrorResponse("Failed to retrieve template");
    }
  }

  /**
   * 更新数据模板。
   *
   * @param id 模板ID
   * @param template 更新的模板数据
   * @return ResponseEntity<ApiResponse<DataTemplate>> 更新结果
   */
  @PutMapping("/{id}")
  @Operation(summary = "更新数据模板", description = "更新指定ID的数据模板。可以修改模板名称、配置等信息。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "模板更新成功",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "模板不存在",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<DataTemplate>> updateTemplate(
      @Parameter(description = "模板ID", required = true, example = "1") @PathVariable Long id,
      @Parameter(description = "更新的模板数据", required = true) @RequestBody @Valid
          DataTemplate template) {
    try {
      DataTemplate updatedTemplate = dataTemplateService.updateTemplate(id, template);
      return buildSuccessResponse(updatedTemplate, "Template updated successfully");
    } catch (IllegalArgumentException e) {
      return buildNotFoundResponse(e.getMessage());
    } catch (Exception e) {
      return buildInternalErrorResponse("Failed to update template");
    }
  }

  /**
   * 根据ID删除数据模板。
   *
   * @param id 模板ID
   * @return ResponseEntity<ApiResponse<Void>> 删除结果
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "根据ID删除数据模板", description = "删除指定ID的数据模板。删除操作不可恢复，请谨慎操作。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "模板删除成功",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "模板不存在",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<Void>> deleteTemplate(
      @Parameter(description = "模板ID", required = true, example = "1") @PathVariable Long id) {
    try {
      dataTemplateService.deleteTemplate(id);
      return buildSuccessResponse(null, "Template deleted successfully");
    } catch (IllegalArgumentException e) {
      return buildNotFoundResponse(e.getMessage());
    } catch (Exception e) {
      return buildInternalErrorResponse("Failed to delete template");
    }
  }

  /**
   * 根据名称删除数据模板。
   *
   * @param name 模板名称
   * @return ResponseEntity<ApiResponse<Void>> 删除结果
   */
  @DeleteMapping("/name/{name}")
  @Operation(summary = "根据名称删除数据模板", description = "删除指定名称的数据模板。删除操作不可恢复，请谨慎操作。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "模板删除成功",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "模板不存在",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<Void>> deleteTemplateByName(
      @Parameter(description = "模板名称", required = true, example = "user-data-template")
          @PathVariable
          String name) {
    try {
      dataTemplateService.deleteTemplateByName(name);
      return buildSuccessResponse(null, "Template deleted successfully");
    } catch (IllegalArgumentException e) {
      return buildNotFoundResponse(e.getMessage());
    } catch (Exception e) {
      return buildInternalErrorResponse("Failed to delete template");
    }
  }
}
