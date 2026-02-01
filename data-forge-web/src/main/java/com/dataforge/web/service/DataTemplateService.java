package com.dataforge.web.service;

import com.dataforge.web.entity.DataTemplate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 数据模板服务接口。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public interface DataTemplateService {

  DataTemplate createTemplate(DataTemplate template);

  DataTemplate updateTemplate(Long id, DataTemplate template);

  DataTemplate getTemplateById(Long id);

  DataTemplate getTemplateByName(String name);

  List<DataTemplate> getAllTemplates();

  List<DataTemplate> getActiveTemplates();

  /**
   * 分页查询所有模板。
   *
   * @param pageable 分页参数
   * @return 分页结果
   */
  Page<DataTemplate> getTemplates(Pageable pageable);

  /**
   * 分页查询激活的模板。
   *
   * @param pageable 分页参数
   * @return 分页结果
   */
  Page<DataTemplate> getActiveTemplates(Pageable pageable);

  void deleteTemplate(Long id);

  void deleteTemplateByName(String name);
}
