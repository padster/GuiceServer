package com.github.padster.guiceserver.handlers;

import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Utility for dealing with getting data from POST bodies. */
public class PostParser {
  // TODO: Support more encoding types?
  public static final String ENCODING = "utf-8";

  /** @return Post body from an exchange, as a String. */
  public static String bodyAsString(HttpExchange exchange) throws IOException {
    return new BufferedReader(
        new InputStreamReader(exchange.getRequestBody(), ENCODING)
    ).readLine();
  }

  /** @return Mapping of post parameter name to values attached to it. */
  public static Map<String, List<String>> bodyAsParams(HttpExchange exchange) throws IOException {
    Map<String, List<String>> params = new HashMap<>();

    // For each value pair... (tokenized by '&')
    for (String def : bodyAsString(exchange).split("[&]")) {
      int ix = def.indexOf('='); // split between 'a=b' and just boolean 'a'
      final String name, value;
      if (ix < 0) {
        name = URLDecoder.decode(def, ENCODING);
        value = "";
      } else {
        name = URLDecoder.decode(def.substring(0, ix), ENCODING);
        value = URLDecoder.decode(def.substring(ix + 1), ENCODING);
      }
      // And add to map
      params.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }
    return params;
  }

  /** Utility to make sure that an 'a=b' mapping was provided once, and return b */
  public static String forceSingle(Map<String, List<String>> params, String key) {
    if (!params.containsKey(key) || params.get(key).size() != 1) {
      throw new IllegalArgumentException();
    }
    return params.get(key).get(0);
  }

  /** Utility to that returns true if a checkbox value is included. */
  public static boolean parseCheckbox(Map<String, List<String>> params, String key) {
    return params.containsKey(key) && params.get(key).size() == 1 && "on".equals(params.get(key).get(0));
  }
}
