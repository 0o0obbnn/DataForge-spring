package com.dataforge.web.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dataforge.web.cache.MultiLevelCacheManager;
import com.dataforge.web.entity.DataTemplate;
import com.dataforge.web.repository.DataTemplateRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * DataTemplateService 单元测试。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DataTemplateServiceImplTest {

  @Mock private DataTemplateRepository dataTemplateRepository;

  @Mock private MultiLevelCacheManager cacheManager;

  @InjectMocks private DataTemplateServiceImpl dataTemplateService;

  @Test
  @DisplayName("创建模板 - 成功")
  void createTemplate_Success() {
    // Given
    DataTemplate template = new DataTemplate();
    template.setName("Test Template");

    when(dataTemplateRepository.findByName("Test Template")).thenReturn(null);
    when(dataTemplateRepository.save(template)).thenReturn(template);

    // When
    DataTemplate result = dataTemplateService.createTemplate(template);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Test Template");
    verify(dataTemplateRepository).save(template);
    verify(cacheManager, times(2)).evict(anyString()); // Verify cache eviction
  }

  @Test
  @DisplayName("创建模板 - 名称已存在抛出异常")
  void createTemplate_NameExists_ThrowsException() {
    // Given
    DataTemplate template = new DataTemplate();
    template.setName("Existing Template");

    when(dataTemplateRepository.findByName("Existing Template")).thenReturn(new DataTemplate());

    // When & Then
    assertThatThrownBy(() -> dataTemplateService.createTemplate(template))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");

    verify(dataTemplateRepository, never()).save(any());
  }

  @Test
  @DisplayName("更新模板 - 成功")
  void updateTemplate_Success() {
    // Given
    Long id = 1L;
    DataTemplate existingTemplate = new DataTemplate();
    existingTemplate.setId(id);
    existingTemplate.setName("Old Name");

    DataTemplate newTemplate = new DataTemplate();
    newTemplate.setName("New Name");
    newTemplate.setDescription("New Desc");
    newTemplate.setActive(true);

    when(dataTemplateRepository.findById(id)).thenReturn(Optional.of(existingTemplate));
    when(dataTemplateRepository.save(existingTemplate)).thenReturn(existingTemplate);

    // When
    DataTemplate result = dataTemplateService.updateTemplate(id, newTemplate);

    // Then
    assertThat(result.getName()).isEqualTo("New Name");
    assertThat(result.getDescription()).isEqualTo("New Desc");
    assertThat(result.isActive()).isTrue();
    verify(cacheManager, times(2)).evict(anyString());
  }

  @Test
  @DisplayName("更新模板 - ID不存在抛出异常")
  void updateTemplate_NotFound_ThrowsException() {
    // Given
    Long id = 999L;
    when(dataTemplateRepository.findById(id)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> dataTemplateService.updateTemplate(id, new DataTemplate()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }

  @Test
  @DisplayName("根据ID获取模板 - 缓存命中")
  void getTemplateById_CacheHit() {
    // Given
    Long id = 1L;
    DataTemplate cachedTemplate = new DataTemplate();
    cachedTemplate.setId(id);

    when(cacheManager.get("template:" + id)).thenReturn(cachedTemplate);

    // When
    DataTemplate result = dataTemplateService.getTemplateById(id);

    // Then
    assertThat(result).isEqualTo(cachedTemplate);
    verify(dataTemplateRepository, never()).findById(any());
    verify(cacheManager, never()).put(anyString(), any(), anyLong());
  }

  @Test
  @DisplayName("根据ID获取模板 - 缓存未命中")
  void getTemplateById_CacheMiss() {
    // Given
    Long id = 1L;
    DataTemplate template = new DataTemplate();
    template.setId(id);

    when(cacheManager.get("template:" + id)).thenReturn(null);
    when(dataTemplateRepository.findById(id)).thenReturn(Optional.of(template));

    // When
    DataTemplate result = dataTemplateService.getTemplateById(id);

    // Then
    assertThat(result).isEqualTo(template);
    verify(cacheManager).put(eq("template:" + id), eq(template), anyLong());
  }

  @Test
  @DisplayName("根据名称获取模板 - 成功")
  void getTemplateByName_Success() {
    // Given
    String name = "Test";
    DataTemplate template = new DataTemplate();
    template.setName(name);

    when(cacheManager.get("template:name:" + name)).thenReturn(null);
    when(dataTemplateRepository.findByName(name)).thenReturn(template);

    // When
    DataTemplate result = dataTemplateService.getTemplateByName(name);

    // Then
    assertThat(result).isEqualTo(template);
    verify(cacheManager).put(eq("template:name:" + name), eq(template), anyLong());
  }

  @Test
  @DisplayName("获取所有模板 - 成功")
  void getAllTemplates_Success() {
    // Given
    List<DataTemplate> templates = Collections.singletonList(new DataTemplate());
    when(cacheManager.get("template:all")).thenReturn(null);
    when(dataTemplateRepository.findAll()).thenReturn(templates);

    // When
    List<DataTemplate> result = dataTemplateService.getAllTemplates();

    // Then
    assertThat(result).hasSize(1);
    verify(cacheManager).put(eq("template:all"), eq(templates), anyLong());
  }

  @Test
  @DisplayName("分页获取所有模板 - 成功")
  void getTemplates_Pageable_Success() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    List<DataTemplate> templates = Collections.singletonList(new DataTemplate());
    Page<DataTemplate> page = new PageImpl<>(templates, pageable, 1);

    when(dataTemplateRepository.findAll(pageable)).thenReturn(page);

    // When
    Page<DataTemplate> result = dataTemplateService.getTemplates(pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @Test
  @DisplayName("删除模板 - 成功")
  void deleteTemplate_Success() {
    // Given
    Long id = 1L;
    when(dataTemplateRepository.existsById(id)).thenReturn(true);
    doNothing().when(dataTemplateRepository).deleteById(id);

    // When
    dataTemplateService.deleteTemplate(id);

    // Then
    verify(dataTemplateRepository).deleteById(id);
    verify(cacheManager, times(2)).evict(anyString());
  }

  @Test
  @DisplayName("根据名称删除模板 - 成功")
  void deleteTemplateByName_Success() {
    // Given
    String name = "Test";
    DataTemplate template = new DataTemplate();
    when(dataTemplateRepository.findByName(name)).thenReturn(template);
    doNothing().when(dataTemplateRepository).deleteByName(name);

    // When
    dataTemplateService.deleteTemplateByName(name);

    // Then
    verify(dataTemplateRepository).deleteByName(name);
    verify(cacheManager, times(2)).evict(anyString());
  }
}
