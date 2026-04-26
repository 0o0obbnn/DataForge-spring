package com.dataforge.web.dto;

import com.dataforge.web.entity.DataTemplate;
import org.springframework.stereotype.Component;

/**
 * DataTemplate 实体与 DTO 之间的映射器。
 *
 * @author DataForge Team
 * @since 1.1.0
 */
@Component
public class DataTemplateMapper {

  /**
   * 将请求 DTO 转换为实体。
   *
   * @param dto 请求 DTO
   * @return DataTemplate 实体
   */
  public DataTemplate toEntity(DataTemplateRequestDTO dto) {
    DataTemplate entity = new DataTemplate();
    entity.setName(dto.getName());
    entity.setDescription(dto.getDescription());
    entity.setConfig(dto.getConfig());
    entity.setActive(dto.isActive());
    return entity;
  }

  /**
   * 使用 DTO 更新已有实体。
   *
   * @param entity 待更新的实体
   * @param dto 请求 DTO
   */
  public void updateEntity(DataTemplate entity, DataTemplateRequestDTO dto) {
    entity.setName(dto.getName());
    entity.setDescription(dto.getDescription());
    entity.setConfig(dto.getConfig());
    entity.setActive(dto.isActive());
  }

  /**
   * 将实体转换为响应 DTO。
   *
   * @param entity DataTemplate 实体
   * @return 响应 DTO
   */
  public DataTemplateResponseDTO toResponseDTO(DataTemplate entity) {
    return DataTemplateResponseDTO.builder()
        .id(entity.getId())
        .name(entity.getName())
        .description(entity.getDescription())
        .config(entity.getConfig())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .active(entity.isActive())
        .version(entity.getVersion())
        .build();
  }
}
