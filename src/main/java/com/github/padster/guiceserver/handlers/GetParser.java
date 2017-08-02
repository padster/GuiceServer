package com.github.padster.guiceserver.handlers;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Utility for dealing with GET request query params. */
public class GetParser {
  // TODO: Support more encoding types?
  public static final String ENCODING = "utf-8";

  /** @return Mapping of query parameter name to values attached to it. */
  public static Map<String, List<String>> queryAsParams(HttpExchange exchange) throws IOException {
    Map<String, List<String>> params = new HashMap<>();

    // For each value pair... (tokenized by '&')
    for (String def : exchange.getRequestURI().getQuery().split("&")) {
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
}
