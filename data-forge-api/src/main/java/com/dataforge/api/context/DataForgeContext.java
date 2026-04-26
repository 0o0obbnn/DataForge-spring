package com.dataforge.api.context;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 数据生成上下文接口。
 *
 * <p>用于在生成过程中传递和共享数据。
 *
 * @author DataForge
 * @since 1.0.0
 */
public interface DataForgeContext {

  /**
   * 获取上下文中的值。
   *
   * @param key 键
   * @param type 值类型
   * @param <T> 值类型
   * @return 值的Optional
   */
  <T> Optional<T> get(String key, Class<T> type);

  /**
   * 获取字符串值。
   *
   * @param key 键
   * @return 字符串值的Optional
   */
  default Optional<String> getString(String key) {
    return get(key, String.class);
  }

  /**
   * 放入值到上下文。
   *
   * @param key 键
   * @param value 值
   */
  void put(String key, Object value);

  /**
   * 检查是否包含指定键。
   *
   * @param key 键
   * @return 如果存在返回true
   */
  boolean containsKey(String key);

  /**
   * 获取所有键。
   *
   * @return 键的集合
   */
  Set<String> keySet();

  /**
   * 获取所有条目。
   *
   * @return 条目映射
   */
  Map<String, Object> getAll();

  /**
   * 清空上下文。
   */
  void clear();

  /**
   * 创建子上下文。
   *
   * @return 新的子上下文
   */
  DataForgeContext createChildContext();

  /**
   * 获取父上下文。
   *
   * @return 父上下文的Optional
   */
  Optional<DataForgeContext> getParent();

  /**
   * 从上下文中移除指定的键值对。
   *
   * @param key 要移除的键
   * @return 被移除的值的Optional，如果不存在返回空Optional
   */
  Optional<Object> remove(String key);
}
