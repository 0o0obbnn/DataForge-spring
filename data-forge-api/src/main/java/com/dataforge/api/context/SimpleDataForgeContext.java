package com.dataforge.api.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 简单的DataForge上下文实现。
 *
 * @author DataForge
 * @since 1.0.0
 */
public class SimpleDataForgeContext implements DataForgeContext {

  private final Map<String, Object> data;
  private final DataForgeContext parent;

  public SimpleDataForgeContext() {
    this.data = new HashMap<>();
    this.parent = null;
  }

  private SimpleDataForgeContext(DataForgeContext parent) {
    this.data = new HashMap<>();
    this.parent = parent;
  }

  @Override
  public <T> Optional<T> get(String key, Class<T> type) {
    Object value = data.get(key);
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
    data.put(key, value);
  }

  @Override
  public boolean containsKey(String key) {
    return data.containsKey(key) || (parent != null && parent.containsKey(key));
  }

  @Override
  public Set<String> keySet() {
    return Collections.unmodifiableSet(data.keySet());
  }

  @Override
  public Map<String, Object> getAll() {
    return Collections.unmodifiableMap(new HashMap<>(data));
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
    return Optional.ofNullable(data.remove(key));
  }
}
