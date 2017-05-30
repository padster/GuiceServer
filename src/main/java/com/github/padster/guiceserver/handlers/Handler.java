package com.github.padster.guiceserver.handlers;

import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/** Generic handler that handles an HTTP request at a given path. */
public interface Handler {
  Object handle(Map<String, String> pathDetails, HttpExchange exchange) throws Exception;
}
