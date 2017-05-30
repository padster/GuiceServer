package com.github.padster.guiceserver;

import com.github.padster.guiceserver.handlers.Handler;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import javax.inject.Provider;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

/**
 * Override to provide all the path -> handler bindings.
 */
public abstract class BindingModule extends AbstractModule {
  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  public @interface Bindings {}

  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  public @interface ServerPort {}

  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  public @interface SequebotPort {}

  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  public @interface BackupFilePath {}

  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  public @interface CurrentUser {}

  final String DATA_PATH = "/_";


  // Override these two!
  abstract void bindPageHandlers(); // handlers for HTML requests..
  abstract void bindDataHandlers(); // handlers for JSON requests.


  public final Map<String, Provider<? extends Handler>> bindings = new HashMap<>();

  @Override protected void configure() {
    bind(new TypeLiteral<Map<String, Provider<? extends Handler>>>(){})
        .annotatedWith(Bindings.class)
        .toInstance(bindings);

    bindPageHandlers();
    bindDataHandlers();

    bind(MustacheFactory.class).toInstance(new DefaultMustacheFactory());
  }

  public <T extends Handler> void bindPageHandler(String path, Class<T> handlerClass) {
    bind(handlerClass).in(Singleton.class);
    bindings.put(path, this.getProvider(handlerClass));
  }

  public <T extends Handler> void bindDataHandler(String path, Class<T> handlerClass) {
    bind(handlerClass).in(Singleton.class);
    bindings.put(DATA_PATH + path, this.getProvider(handlerClass));
  }
}
