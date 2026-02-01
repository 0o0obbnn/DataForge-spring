package com.dataforge.core;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据生成上下文 - 增强版本。
 *
 * <p>在一次生成请求的生命周期内共享数据，用于解决字段间的关联性问题。 该类是线程安全的，支持并发访问，实现了AutoCloseable接口以确保资源正确释放。
 *
 * <p>典型使用场景：
 *
 * <ul>
 *   <li>身份证号生成器将出生日期、性别、地区信息存入上下文
 *   <li>年龄生成器从上下文获取出生日期，计算对应年龄
 *   <li>地址生成器从上下文获取地区信息，生成对应地区的详细地址
 * </ul>
 *
 * <p><strong>安全性增强：</strong>
 *
 * <ul>
 *   <li>增加了上下文大小限制，防止内存溢出
 *   <li>增强了参数验证和状态检查
 *   <li>实现了资源自动清理机制
 *   <li>移除了不必要的读写锁，提高性能
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class DataForgeContext implements com.dataforge.api.context.DataForgeContext, AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataForgeContext.class);

  /** 上下文最大容量限制，防止内存溢出攻击。 */
  private static final int MAX_CONTEXT_SIZE = 10_000;

  /** 键名最大长度限制。 */
  private static final int MAX_KEY_LENGTH = 255;

  /** 使用 ConcurrentHashMap 保证多线程生成时的线程安全。 移除了读写锁，因为ConcurrentHashMap已经提供了足够的并发保护。 */
  private final Map<String, Object> contextMap = new ConcurrentHashMap<>();

  /** 上下文创建时间，用于调试和日志记录。 */
  private final LocalDateTime createdAt;

  /** 当前记录的索引，用于批量生成时的记录标识。 */
  private volatile int currentRecordIndex = 0;

  /** 标识上下文是否已关闭。 */
  private volatile boolean closed = false;

  /** 父上下文，用于支持上下文嵌套。 */
  private final DataForgeContext parent;

  /** 构造函数，初始化上下文。 */
  public DataForgeContext() {
    this.createdAt = LocalDateTime.now();
    this.parent = null;
    LOGGER.debug("DataForgeContext created at {}", createdAt);
  }

  /** 构造函数，初始化带父上下文的子上下文。 */
  private DataForgeContext(DataForgeContext parent) {
    this.createdAt = LocalDateTime.now();
    this.parent = parent;
    LOGGER.debug("DataForgeContext created with parent at {}", createdAt);
  }

  /**
   * 向上下文中存储键值对 - 增强版本。
   *
   * <p>如果键已存在，将覆盖原有值。存储操作是线程安全的。
   *
   * @param key 键，不能为null或空字符串
   * @param value 值，可以为null
   * @throws IllegalArgumentException 当key为null或空字符串时
   * @throws IllegalStateException 当上下文已关闭或已达到最大容量时
   */
  @Override
  public void put(String key, Object value) {
    validateNotClosed();
    validateKey(key);

    // 检查容量限制（仅在添加新键时检查）
    if (contextMap.size() >= MAX_CONTEXT_SIZE && !contextMap.containsKey(key)) {
      throw new IllegalStateException(
          String.format("Context has reached maximum size limit: %d", MAX_CONTEXT_SIZE));
    }

    Object oldValue = contextMap.put(key, value);
    LOGGER.trace("Context put: key={}, value={}, oldValue={}", key, value, oldValue);
  }

  /**
   * 向上下文中存储键值对（别名方法）。
   *
   * @param key 键，不能为null或空字符串
   * @param value 值，可以为null
   * @throws IllegalArgumentException 当key为null或空字符串时
   * @throws IllegalStateException 当上下文已关闭时
   */
  public void putValue(String key, String value) {
    put(key, value);
  }

  /**
   * 从上下文中获取字符串值（别名方法）。
   *
   * @param key 键
   * @return 字符串值，如果键不存在或值不是字符串类型则返回null
   */
  public String getValue(String key) {
    return get(key, String.class).orElse(null);
  }

  /**
   * 从上下文中获取指定类型的值 - 增强版本。
   *
   * <p>该方法是类型安全的，只有当存储的值确实是指定类型的实例时才会返回。
   *
   * @param <V> 期望的值类型
   * @param key 键，不能为null或空字符串
   * @param type 期望的值类型的Class对象
   * @return 包含值的Optional，如果键不存在或类型不匹配则返回空Optional
   * @throws IllegalArgumentException 当参数无效时
   * @throws IllegalStateException 当上下文已关闭时
   */
  @Override
  @SuppressWarnings("unchecked")
  public <V> Optional<V> get(String key, Class<V> type) {
    validateNotClosed();
    validateKey(key);
    Objects.requireNonNull(type, "Type cannot be null");

    Object value = contextMap.get(key);
    if (value != null && type.isInstance(value)) {
      LOGGER.trace("Context get: key={}, type={}, value={}", key, type.getSimpleName(), value);
      return Optional.of((V) value);
    }

    // 如果本地没有，尝试从父上下文获取
    if (value == null && parent != null) {
      return parent.get(key, type);
    }

    LOGGER.trace(
        "Context get: key={}, type={}, value not found or type mismatch",
        key,
        type.getSimpleName());
    return Optional.empty();
  }

  /**
   * 从上下文中获取值，不进行类型检查。
   *
   * @param key 键
   * @return 值的Optional，如果键不存在则返回空Optional
   * @throws IllegalStateException 当上下文已关闭时
   */
  public Optional<Object> get(String key) {
    validateNotClosed();
    if (key == null || key.trim().isEmpty()) {
      return Optional.empty();
    }
    Object value = contextMap.get(key);
    if (value == null && parent != null) {
      return Optional.ofNullable(parent.getAll().get(key));
    }
    return Optional.ofNullable(value);
  }

  /**
   * 检查上下文中是否包含指定的键。
   *
   * @param key 键
   * @return 如果包含该键返回true，否则返回false
   * @throws IllegalStateException 当上下文已关闭时
   */
  @Override
  public boolean containsKey(String key) {
    validateNotClosed();
    if (key == null || key.trim().isEmpty()) {
      return false;
    }
    return contextMap.containsKey(key) || (parent != null && parent.containsKey(key));
  }

  /**
   * 获取所有键。
   *
   * @return 键的集合
   */
  @Override
  public Set<String> keySet() {
    return Collections.unmodifiableSet(contextMap.keySet());
  }

  /**
   * 获取所有条目。
   *
   * @return 条目映射
   */
  @Override
  public Map<String, Object> getAll() {
    return Collections.unmodifiableMap(new HashMap<>(contextMap));
  }

  /**
   * 从上下文中移除指定的键值对。
   *
   * @param key 要移除的键
   * @return 被移除的值的Optional，如果键不存在则返回空Optional
   * @throws IllegalStateException 当上下文已关闭时
   */
  @Override
  public Optional<Object> remove(String key) {
    validateNotClosed();
    if (key == null || key.trim().isEmpty()) {
      return Optional.empty();
    }

    Object removedValue = contextMap.remove(key);
    LOGGER.trace("Context remove: key={}, removedValue={}", key, removedValue);
    return Optional.ofNullable(removedValue);
  }

  /**
   * 清空上下文中的所有数据。
   *
   * <p>通常在开始生成新记录时调用，以避免数据污染。
   *
   * @throws IllegalStateException 当上下文已关闭时
   */
  @Override
  public void clear() {
    validateNotClosed();
    int size = contextMap.size();
    contextMap.clear();
    LOGGER.debug("Context cleared, removed {} entries", size);
  }

  /**
   * 创建子上下文。
   *
   * @return 新的子上下文
   */
  @Override
  public DataForgeContext createChildContext() {
    return new DataForgeContext(this);
  }

  /**
   * 获取父上下文。
   *
   * @return 父上下文的Optional
   */
  @Override
  public Optional<com.dataforge.api.context.DataForgeContext> getParent() {
    return Optional.ofNullable(parent);
  }

  /**
   * 获取上下文中存储的键值对数量。
   *
   * @return 键值对数量
   */
  public int size() {
    return contextMap.size();
  }

  /**
   * 检查上下文是否为空。
   *
   * @return 如果上下文为空返回true，否则返回false
   */
  public boolean isEmpty() {
    return contextMap.isEmpty();
  }

  /**
   * 获取当前记录的索引。
   *
   * @return 当前记录索引，从0开始
   */
  public int getCurrentRecordIndex() {
    return currentRecordIndex;
  }

  /**
   * 设置当前记录的索引 - 增强版本。
   *
   * <p>通常由框架在批量生成时自动调用。
   *
   * @param index 记录索引，应该大于等于0
   * @throws IllegalArgumentException 当index小于0时
   * @throws IllegalStateException 当上下文已关闭时
   */
  public void setCurrentRecordIndex(int index) {
    validateNotClosed();
    if (index < 0) {
      throw new IllegalArgumentException("Record index cannot be negative");
    }
    this.currentRecordIndex = index;
    LOGGER.trace("Current record index set to {}", index);
  }

  /**
   * 递增当前记录索引。
   *
   * @return 递增后的索引值
   * @throws IllegalStateException 当上下文已关闭时
   */
  public int incrementRecordIndex() {
    validateNotClosed();
    int newIndex = ++currentRecordIndex;
    LOGGER.trace("Record index incremented to {}", newIndex);
    return newIndex;
  }

  /**
   * 获取上下文创建时间。
   *
   * @return 创建时间
   */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * 检查上下文是否已关闭。
   *
   * @return 如果已关闭返回true，否则返回false
   */
  public boolean isClosed() {
    return closed;
  }

  /** 实现AutoCloseable接口，确保资源正确释放。 */
  @Override
  public void close() {
    if (!closed) {
      closed = true;
      int size = contextMap.size();
      contextMap.clear();
      LOGGER.debug("DataForgeContext closed and cleaned up, removed {} entries", size);
    }
  }

  /**
   * 验证上下文未关闭。
   *
   * @throws IllegalStateException 当上下文已关闭时
   */
  private void validateNotClosed() {
    if (closed) {
      throw new IllegalStateException("DataForgeContext has been closed");
    }
  }

  /**
   * 验证键的有效性。
   *
   * @param key 要验证的键
   * @throws IllegalArgumentException 当键无效时
   */
  private void validateKey(String key) {
    if (key == null || key.trim().isEmpty()) {
      throw new IllegalArgumentException("Key cannot be null or empty");
    }

    if (key.length() > MAX_KEY_LENGTH) {
      throw new IllegalArgumentException(
          String.format(
              "Key length exceeds maximum allowed: %d > %d", key.length(), MAX_KEY_LENGTH));
    }
  }

  /**
   * 获取上下文的字符串表示，用于调试。
   *
   * @return 上下文的字符串表示
   */
  @Override
  public String toString() {
    return String.format(
        "DataForgeContext{size=%d, recordIndex=%d, createdAt=%s, closed=%s}",
        size(), currentRecordIndex, createdAt, closed);
  }
}
