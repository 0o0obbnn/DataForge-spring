package com.dataforge.web.repository;

import com.dataforge.web.entity.DataTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 数据模板仓储接口。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Repository
public interface DataTemplateRepository extends JpaRepository<DataTemplate, Long> {

  /**
   * 根据名称查询模板。
   *
   * @param name 模板名称
   * @return 匹配的模板
   */
  DataTemplate findByName(String name);

  /**
   * 根据名称删除模板。
   *
   * @param name 模板名称
   */
  void deleteByName(String name);

  /**
   * 分页查询激活状态的模板。
   *
   * @param isActive 激活状态
   * @param pageable 分页参数
   * @return 分页结果
   */
  Page<DataTemplate> findByIsActive(boolean isActive, Pageable pageable);
}
