package com.github.padster.guiceserver.handlers;

import com.github.padster.guiceserver.Annotations;
import com.google.common.base.Preconditions;
import com.sun.net.httpserver.HttpExchange;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Handles common logic for converting bound paths to handlers. */
public class RouteParser {
  private final List<String[]> pathRules = new ArrayList<>();
  private final List<Provider<? extends Handler>> pathHandlers = new ArrayList<>();

  public static class ParsedHandler {
    public final Handler handler;
    public final Map<String, String> pathParams;
    public ParsedHandler(Handler handler, Map<String, String> pathParams) {
      this.handler = handler;
      this.pathParams = pathParams;
    }
  }

  @Inject
  public RouteParser(@Annotations.Bindings Map<String, Provider<? extends Handler>> handlerMap) {
    handlerMap.forEach((path, handler) -> {
      Preconditions.checkArgument(path.startsWith("/"), "Handler paths must start with /");
      pathRules.add(path.substring(1).split("/"));
      pathHandlers.add(handler);
    });
  }

  /** @return Handler and params for a given exchange. */
  public ParsedHandler parseHandler(HttpExchange exchange) throws FileNotFoundException {
    Preconditions.checkArgument(
        exchange.getRequestURI().getPath().startsWith("/"), "Path should start with /");
    String[] parts = exchange.getRequestURI().getPath().substring(1).split("/");
    for (int i = 0; i < pathRules.size(); i++) {
      Map<String, String> paramsMatched = match(parts, pathRules.get(i));
      if (paramsMatched != null) {
        return new ParsedHandler(pathHandlers.get(i).get(), paramsMatched);
      }
    }
    throw new FileNotFoundException("No matching handler for /" + String.join("/", parts));
  }

  /**
   * Match path segments to a pattern block.
   *
   * @return The params extracted, or null for no match.
   */
  private static Map<String, String> match(String[] parts, String[] pattern) {
    Map<String, String> result = new HashMap<>();
    for (int i = 0; i < parts.length; i++) {
      if (pattern.length <= i) {
        return null;
      } else if (pattern[i].equals("*")) {
        continue;
      } else if (pattern[i].equals("**")) {
        Preconditions.checkArgument(i == pattern.length - 1, "** can only appear at the end.");
        break;
      } else if (pattern[i].startsWith(":")) {
        result.put(pattern[i].substring(1), parts[i]);
      } else if (pattern[i].equals(parts[i])) {
        continue;
      } else {
        return null;
      }
    }
    if (pattern.length > parts.length) {
      return null;
    }
    return result;
  }
}
