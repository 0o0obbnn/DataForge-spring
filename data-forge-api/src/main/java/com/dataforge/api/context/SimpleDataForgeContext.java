package com.dataforge.api.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的DataForge上下文实现。
 *
 * @author DataForge
 * @since 1.0.0
 */
public class SimpleDataForgeContext implements DataForgeContext {

  // 用于在ConcurrentHashMap中表示null值的标记对象
  private static final Object NULL_MARKER = new Object();
  
  private final Map<String, Object> data;
  private final DataForgeContext parent;

  public SimpleDataForgeContext() {
    this.data = new ConcurrentHashMap<>();
    this.parent = null;
  }

  private SimpleDataForgeContext(DataForgeContext parent) {
    this.data = new ConcurrentHashMap<>();
    this.parent = parent;
  }

  @Override
  public <T> Optional<T> get(String key, Class<T> type) {
    Object value = data.get(key);
    // 检查是否是null标记
    if (value == NULL_MARKER) {
      return Optional.empty();
    }
    if (value == null && parent != null) {
      return parent.get(key, type);
    }
    if (value == null) {
      return Optional.empty();
    }
    if (type.isInstance(value)) {
      return Optional.of(type.cast(value));
    }
    return Optional.empty();
  }

  @Override
  public void put(String key, Object value) {
    // ConcurrentHashMap不支持null值，使用标记对象代替
    data.put(key, value == null ? NULL_MARKER : value);
  }

  @Override
  public boolean containsKey(String key) {
    if (key == null) {
      return false;
    }
    return data.containsKey(key) || (parent != null && parent.containsKey(key));
  }

  @Override
  public Set<String> keySet() {
    return Collections.unmodifiableSet(data.keySet());
  }

  @Override
  public Map<String, Object> getAll() {
    Map<String, Object> result = new HashMap<>();
    for (Map.Entry<String, Object> entry : data.entrySet()) {
      Object value = entry.getValue();
      if (value == NULL_MARKER) {
        result.put(entry.getKey(), null);
      } else {
        result.put(entry.getKey(), value);
      }
    }
    return Collections.unmodifiableMap(result);
  }

  @Override
  public void clear() {
    data.clear();
  }

  @Override
  public DataForgeContext createChildContext() {
    return new SimpleDataForgeContext(this);
  }

  @Override
  public Optional<DataForgeContext> getParent() {
    return Optional.ofNullable(parent);
  }

  @Override
  public Optional<Object> remove(String key) {
    Object removed = data.remove(key);
    if (removed == NULL_MARKER) {
      return Optional.empty();
    }
    return Optional.ofNullable(removed);
  }
}
