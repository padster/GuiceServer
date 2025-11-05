package com.github.padster.guiceserver;

import com.github.padster.guiceserver.Annotations.Bindings;
import com.github.padster.guiceserver.Annotations.CurrentUser;
import com.github.padster.guiceserver.Annotations.RequestScoped;
import com.github.padster.guiceserver.handlers.Handler;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.sun.net.httpserver.HttpExchange;

import jakarta.inject.Provider;
import java.util.HashMap;
import java.util.Map;

/**
 * Override to provide all the path -> handler bindings.
 */
public abstract class BaseBindingModule extends AbstractModule {
  final String DATA_PATH = "/_";


  // Override these two!
  protected abstract void bindPageHandlers(); // handlers for HTML requests..
  protected abstract void bindDataHandlers(); // handlers for JSON requests.


  public final Map<String, Provider<? extends Handler>> bindings = new HashMap<>();

  @Override protected void configure() {
    bindScope(RequestScoped.class, new RequestScope());
    bind(HttpExchange.class).toProvider(HttpExchangeProvider.class).in(RequestScoped.class);

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

  @Provides @CurrentUser
  public String provideCurrentUser(@RequestScoped Provider<HttpExchange> exchangeProvider) {
    return exchangeProvider.get().getPrincipal().getUsername();
  }
}
