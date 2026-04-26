package com.dataforge.web.api;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import com.dataforge.web.entity.DataTemplate;
import com.dataforge.web.repository.DataTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

public class TemplateControllerTest extends BaseApiTest {

  @Autowired private DataTemplateRepository dataTemplateRepository;

  @Autowired private ObjectMapper objectMapper;

  @AfterEach
  public void cleanup() {
    dataTemplateRepository.deleteAll();
  }

  /**
   * 创建测试模板
   *
   * @param name 模板名称
   * @param description 模板描述
   * @param config 模板配置
   * @return DataTemplate对象
   */
  private DataTemplate createTestTemplate(String name, String description, String config) {
    DataTemplate template = new DataTemplate();
    template.setName(name);
    template.setDescription(description);
    template.setConfig(config);
    template.setActive(true);
    template.setCreatedAt(LocalDateTime.now());
    template.setUpdatedAt(LocalDateTime.now());
    template.setVersion(1);
    return dataTemplateRepository.save(template);
  }

  @Test
  public void testCreateTemplate() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/templates")
                .header("Authorization", getAuthToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new DataTemplate(
                            null,
                            "new-api-template",
                            "New API Test Template",
                            "{\"fields\": [{\"name\": \"name\", \"type\": \"string\"}]}",
                            null,
                            null,
                            true,
                            1))))
        .andDo(print())
        .andExpect(MockMvcResultMatchers.status().isCreated())
        .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(201))
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.message").value("Template created successfully"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.name").value("new-api-template"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.active").value(true));
  }

  @Test
  public void testGetAllTemplates() throws Exception {
    createTestTemplate(
        "template-api-1",
        "API Template 1",
        "{\"fields\": [{\"name\": \"field1\", \"type\": \"string\"}]}");
    createTestTemplate(
        "template-api-2",
        "API Template 2",
        "{\"fields\": [{\"name\": \"field2\", \"type\": \"number\"}]}");

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/v1/templates").header("Authorization", getAuthToken()))
        .andDo(print())
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.message").value("Templates retrieved successfully"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(2))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].name").isNotEmpty())
        .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].name").isNotEmpty());
  }

  @Test
  public void testGetTemplateById() throws Exception {
    DataTemplate template =
        createTestTemplate(
            "template-api-id",
            "API Template by ID",
            "{\"fields\": [{\"name\": \"id-field\", \"type\": \"string\"}]}");

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/v1/templates/" + template.getId())
                .header("Authorization", getAuthToken()))
        .andDo(print())
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.message").value("Template retrieved successfully"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(template.getId()))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.name").value(template.getName()))
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.data.description").value(template.getDescription()));
  }

  @Test
  public void testGetTemplateByNonExistentId() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/v1/templates/9999")
                .header("Authorization", getAuthToken()))
        .andDo(print())
        .andExpect(MockMvcResultMatchers.status().isNotFound())
        .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(404))
        .andExpect(MockMvcResultMatchers.jsonPath("$.message").isNotEmpty());
  }

  @Test
  public void testUpdateTemplate() throws Exception {
    DataTemplate template =
        createTestTemplate(
            "template-to-update",
            "Template to Update",
            "{\"fields\": [{\"name\": \"old-field\", \"type\": \"string\"}]}");
    template.setDescription("Updated Template");
    template.setConfig("{\"fields\": [{\"name\": \"new-field\", \"type\": \"number\"}]}");
    template.setActive(false);

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/api/v1/templates/" + template.getId())
                .header("Authorization", getAuthToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(template)))
        .andDo(print())
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.message").value("Template updated successfully"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(template.getId()))
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.data.description").value(template.getDescription()))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.config").value(template.getConfig()))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.active").value(template.isActive()));
  }

  @Test
  public void testDeleteTemplate() throws Exception {
    DataTemplate template =
        createTestTemplate(
            "template-to-delete",
            "Template to Delete",
            "{\"fields\": [{\"name\": \"delete-field\", \"type\": \"string\"}]}");

    mockMvc
        .perform(
            MockMvcRequestBuilders.delete("/api/v1/templates/" + template.getId())
                .header("Authorization", getAuthToken()))
        .andDo(print())
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.message").value("Template deleted successfully"));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/v1/templates/" + template.getId())
                .header("Authorization", getAuthToken()))
        .andDo(print())
        .andExpect(MockMvcResultMatchers.status().isNotFound());
  }

  @Test
  public void testCreateTemplateWithInvalidData() throws Exception {
    DataTemplate template = new DataTemplate();
    template.setName("");
    template.setDescription("Invalid Template");
    template.setConfig("invalid-config");
    template.setActive(true);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/templates")
                .header("Authorization", getAuthToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(template)))
        .andDo(print())
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  public void testCreateTemplateWithDuplicateName() throws Exception {
    createTestTemplate(
        "duplicate-template",
        "Template 1",
        "{\"fields\": [{\"name\": \"field1\", \"type\": \"string\"}]}");

    DataTemplate template2 = new DataTemplate();
    template2.setName("duplicate-template");
    template2.setDescription("Template 2");
    template2.setConfig("{\"fields\": [{\"name\": \"field2\", \"type\": \"number\"}]}");
    template2.setActive(true);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/templates")
                .header("Authorization", getAuthToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(template2)))
        .andDo(print())
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }
}
