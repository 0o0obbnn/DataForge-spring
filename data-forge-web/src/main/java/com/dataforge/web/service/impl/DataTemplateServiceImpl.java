package com.dataforge.web.service.impl;

import com.dataforge.web.cache.MultiLevelCacheManager;
import com.dataforge.web.entity.DataTemplate;
import com.dataforge.web.repository.DataTemplateRepository;
import com.dataforge.web.service.DataTemplateService;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DataTemplateServiceImpl implements DataTemplateService {

  @Autowired private DataTemplateRepository dataTemplateRepository;

  @Autowired private MultiLevelCacheManager cacheManager;

  // 缓存键前缀
  private static final String TEMPLATE_CACHE_PREFIX = "template:";
  private static final String TEMPLATE_NAME_CACHE_PREFIX = "template:name:";
  private static final String ALL_TEMPLATES_CACHE_KEY = "template:all";
  private static final String ACTIVE_TEMPLATES_CACHE_KEY = "template:active";

  // 缓存过期时间（秒）
  private static final long TEMPLATE_CACHE_EXPIRY = 3600;

  @Override
  public DataTemplate createTemplate(DataTemplate template) {
    if (dataTemplateRepository.findByName(template.getName()) != null) {
      throw new IllegalArgumentException(
          "Template with name '" + template.getName() + "' already exists");
    }

    DataTemplate createdTemplate = dataTemplateRepository.save(template);

    // 清除相关缓存
    clearTemplateCaches();

    return createdTemplate;
  }

  @Override
  public DataTemplate updateTemplate(Long id, DataTemplate template) {
    Optional<DataTemplate> existingTemplateOpt = dataTemplateRepository.findById(id);
    if (existingTemplateOpt.isEmpty()) {
      throw new IllegalArgumentException("Template with id '" + id + "' not found");
    }

    // existingTemplateOpt.get() 在 isEmpty() 检查后调用，不会为 null
    DataTemplate existingTemplate = existingTemplateOpt.get();
    existingTemplate.setName(template.getName());
    existingTemplate.setDescription(template.getDescription());
    existingTemplate.setConfig(template.getConfig());
    existingTemplate.setActive(template.isActive());

    DataTemplate updatedTemplate = dataTemplateRepository.save(existingTemplate);

    // 清除相关缓存
    clearTemplateCaches();

    return updatedTemplate;
  }

  @Override
  @Transactional(readOnly = true)
  public DataTemplate getTemplateById(Long id) {
    String cacheKey = TEMPLATE_CACHE_PREFIX + id;

    // 从缓存获取
    DataTemplate template = cacheManager.get(cacheKey);
    if (template != null) {
      return template;
    }

    // 缓存未命中，从数据库获取
    template =
        dataTemplateRepository
            .findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("Template with id '" + id + "' not found"));

    // 更新缓存
    cacheManager.put(cacheKey, template, TEMPLATE_CACHE_EXPIRY);

    return template;
  }

  @Override
  @Transactional(readOnly = true)
  public DataTemplate getTemplateByName(String name) {
    String cacheKey = TEMPLATE_NAME_CACHE_PREFIX + name;

    // 从缓存获取
    DataTemplate template = cacheManager.get(cacheKey);
    if (template != null) {
      return template;
    }

    // 缓存未命中，从数据库获取
    template = dataTemplateRepository.findByName(name);
    if (template == null) {
      throw new IllegalArgumentException("Template with name '" + name + "' not found");
    }

    // 更新缓存
    cacheManager.put(cacheKey, template, TEMPLATE_CACHE_EXPIRY);

    return template;
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataTemplate> getAllTemplates() {
    // 从缓存获取
    @SuppressWarnings("unchecked")
    List<DataTemplate> templates = (List<DataTemplate>) cacheManager.get(ALL_TEMPLATES_CACHE_KEY);
    if (templates != null) {
      return templates;
    }

    // 缓存未命中，从数据库获取
    templates = dataTemplateRepository.findAll();

    // 更新缓存
    cacheManager.put(ALL_TEMPLATES_CACHE_KEY, templates, TEMPLATE_CACHE_EXPIRY);

    return templates;
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataTemplate> getActiveTemplates() {
    // 从缓存获取
    @SuppressWarnings("unchecked")
    List<DataTemplate> templates =
        (List<DataTemplate>) cacheManager.get(ACTIVE_TEMPLATES_CACHE_KEY);
    if (templates != null) {
      return templates;
    }

    // 缓存未命中，从数据库获取
    templates = dataTemplateRepository.findAll().stream().filter(DataTemplate::isActive).toList();

    // 更新缓存
    cacheManager.put(ACTIVE_TEMPLATES_CACHE_KEY, templates, TEMPLATE_CACHE_EXPIRY);

    return templates;
  }

  @Override
  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<DataTemplate> getTemplates(
      org.springframework.data.domain.Pageable pageable) {
    return dataTemplateRepository.findAll(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<DataTemplate> getActiveTemplates(
      org.springframework.data.domain.Pageable pageable) {
    return dataTemplateRepository.findByIsActive(true, pageable);
  }

  @Override
  public void deleteTemplate(Long id) {
    if (!dataTemplateRepository.existsById(id)) {
      throw new IllegalArgumentException("Template with id '" + id + "' not found");
    }

    dataTemplateRepository.deleteById(id);

    // 清除相关缓存
    clearTemplateCaches();
  }

  @Override
  public void deleteTemplateByName(String name) {
    DataTemplate template = dataTemplateRepository.findByName(name);
    if (template == null) {
      throw new IllegalArgumentException("Template with name '" + name + "' not found");
    }

    dataTemplateRepository.deleteByName(name);

    // 清除相关缓存
    clearTemplateCaches();
  }

  /** 清除模板相关的所有缓存。 */
  private void clearTemplateCaches() {
    // 清除所有模板相关缓存
    cacheManager.evict(ALL_TEMPLATES_CACHE_KEY);
    cacheManager.evict(ACTIVE_TEMPLATES_CACHE_KEY);
    // 注意：这里没有清除单个模板的缓存，因为我们不知道具体的键
    // 在实际应用中，可以考虑使用Redis的key pattern来清除所有相关缓存
  }
}
