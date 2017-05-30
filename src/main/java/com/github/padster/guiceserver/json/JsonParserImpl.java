package com.github.padster.guiceserver.json;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import javax.inject.Provider;

/**
 * Provide mappings to and from JSON for a particular class, using Gson.
 */
public class JsonParserImpl<T> implements JsonParser<T> {
  public final Provider<Gson> gsonProvider;
  public final Type type;

  public JsonParserImpl(Provider<Gson> gsonProvider, Type type) {
    this.gsonProvider = gsonProvider;
    this.type = type;
  }

  @Override public T fromJson(String json) {
    return gson().fromJson(json, type);
  }

  @Override public String toJson(T value) {
    return gson().toJson(value);
  }

  private Gson gson() {
    return gsonProvider.get();
  }
}
