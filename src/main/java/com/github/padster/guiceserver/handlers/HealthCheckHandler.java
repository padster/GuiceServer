package com.github.padster.guiceserver.handlers;

import java.util.Map;

import com.github.padster.guiceserver.handlers.RouteHandlerResponses.TextResponse;
import com.sun.net.httpserver.HttpExchange;

/** Useful default handler for checking health of the server. */
public class HealthCheckHandler implements Handler {
  @Override public Object handle(
    Map<String, String> pathDetails, 
    HttpExchange exchange
  ) throws Exception {
    if ("GET".equals(exchange.getRequestMethod())) {
      return new TextResponse("OK");
    } else {
      throw new UnsupportedOperationException();
    }
  }
}
