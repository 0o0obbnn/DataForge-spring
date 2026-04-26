package com.dataforge.web.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.dataforge.web.config.TestRedisConfiguration;
import com.dataforge.web.entity.DataTemplate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(TestRedisConfiguration.class)
@DisplayName("DataTemplateRepository单元测试")
class DataTemplateRepositoryTest {

  @Autowired private DataTemplateRepository dataTemplateRepository;

  @Test
  @DisplayName("测试保存和查询数据模板")
  void should_save_and_find_data_template() {
    DataTemplate template = new DataTemplate();
    template.setName("测试模板");
    template.setDescription("测试描述");
    template.setConfig("{}");

    DataTemplate saved = dataTemplateRepository.save(template);
    Optional<DataTemplate> found = dataTemplateRepository.findById(saved.getId());

    assertTrue(found.isPresent());
    assertEquals("测试模板", found.get().getName());
  }

  @Test
  @DisplayName("测试根据名称查询模板")
  void should_find_template_by_name() {
    DataTemplate template = new DataTemplate();
    template.setName("唯一名称");
    template.setDescription("描述");
    template.setConfig("{}");
    dataTemplateRepository.save(template);

    DataTemplate found = dataTemplateRepository.findByName("唯一名称");

    assertNotNull(found);
    assertEquals("唯一名称", found.getName());
  }

  @Test
  @DisplayName("测试删除模板")
  void should_delete_template() {
    DataTemplate template = new DataTemplate();
    template.setName("待删除");
    template.setConfig("{}");
    DataTemplate saved = dataTemplateRepository.save(template);

    dataTemplateRepository.deleteById(saved.getId());

    Optional<DataTemplate> found = dataTemplateRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
