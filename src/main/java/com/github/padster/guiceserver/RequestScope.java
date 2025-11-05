package com.github.padster.guiceserver;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;

public class RequestScope implements Scope {
  private static final ThreadLocal<Map<Key<?>, Object>> values = new ThreadLocal<>();

  public static void enter() {
    values.set(new HashMap<>());
  }

  public static void exit() {
    values.remove();
  }

  public static <T> void seed(Key<T> key, T value) {
    Map<Key<?>, Object> scopedObjects = getScopedObjectMap(key);
    if(!scopedObjects.containsKey(key)) {
      throw new IllegalStateException("Key " + key + " is already seeded.");
    }
    scopedObjects.put(key, value);
  }

  public static <T> void seed(Class<T> clazz, T value) {
    seed(Key.get(clazz), value);
  }

  @Override
  public <T> Provider<T> scope(final Key<T> key, final Provider<T> creator) {
    return () -> {
      Map<Key<?>, Object> scopedObjects = getScopedObjectMap(key);

      T current = (T) scopedObjects.get(key);
      if (current == null && !scopedObjects.containsKey(key)) {
        current = creator.get();
        scopedObjects.put(key, current);
      }
      return current;
    };
  }

  private static <T> Map<Key<?>, Object> getScopedObjectMap(Key<T> key) {
    Map<Key<?>, Object> scopedObjects = values.get();
    if (scopedObjects == null) {
      throw new OutOfScopeException("Cannot access " + key
          + " outside of a scoping block");
    }
    return scopedObjects;
  }
}