
package com.github.padster.guiceserver;

import com.github.padster.guiceserver.Annotations.RequestScoped;
import com.google.inject.Provider;
import com.sun.net.httpserver.HttpExchange;

@RequestScoped
public class HttpExchangeProvider implements Provider<HttpExchange> {
  private HttpExchange httpExchange;

  public void set(HttpExchange httpExchange) {
    this.httpExchange = httpExchange;
  }

  @Override
  public HttpExchange get() {
    if (httpExchange == null) {
      throw new IllegalStateException("Not inside a request scope.");
    }
    return httpExchange;
  }
}