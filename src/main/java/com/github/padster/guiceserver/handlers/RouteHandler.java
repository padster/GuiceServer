package com.github.padster.guiceserver.handlers;

import com.github.padster.guiceserver.BindingModule.Bindings;
import com.github.padster.guiceserver.handlers.RouteHandlerResponses.CSVResponse;
import com.github.padster.guiceserver.handlers.RouteHandlerResponses.FileDownloadResponse;
import com.github.padster.guiceserver.handlers.RouteHandlerResponses.JsonResponse;
import com.github.padster.guiceserver.handlers.RouteHandlerResponses.MustacheResponse;
import com.github.padster.guiceserver.handlers.RouteHandlerResponses.StreamResponse;
import com.github.padster.guiceserver.handlers.RouteHandlerResponses.TextResponse;

import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Preconditions;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Logic for finding which handler to route to, and converting its results to an HttpResponse.
 * Safe to ignore this file.
 */
public class RouteHandler implements HttpHandler {
  public static class UnauthorizedException extends RuntimeException {}

  public static class ParsedHandler {
    public final Handler handler;
    public final Map<String, String> pathParams;
    public ParsedHandler(Handler handler, Map<String, String> pathParams) {
      this.handler = handler;
      this.pathParams = pathParams;
    }
  }

  private final List<String[]> pathRules = new ArrayList<>();
  private final List<Provider<? extends Handler>> pathHandlers = new ArrayList<>();
  private final MustacheFactory mustacheFactory;

  @Inject public RouteHandler(MustacheFactory mustacheFactory,
      @Bindings Map<String, Provider<? extends Handler>> handlerMap) {
    handlerMap.forEach((path, handler) -> {
      Preconditions.checkArgument(path.startsWith("/"), "Handler paths must start with /");
      pathRules.add(path.substring(1).split("/"));
      pathHandlers.add(handler);
    });
    this.mustacheFactory = mustacheFactory;
  }

  @Override public void handle(HttpExchange exchange) throws IOException {
    System.out.println("Handle: " + exchange.getRequestURI().getPath());

    Preconditions.checkArgument(
        exchange.getRequestURI().getPath().startsWith("/"), "Path should start with /");
    String[] pathParts = exchange.getRequestURI().getPath().substring(1).split("/");

    try {
      ParsedHandler parsedHandler = parseHandler(pathParts);
      Object result = parsedHandler.handler.handle(parsedHandler.pathParams, exchange);
      Preconditions.checkState(result != null, "Can't have a null response.");

      if (result instanceof TextResponse) {
        this.handleTextResponse(exchange, (TextResponse) result);
      } else if (result instanceof JsonResponse) {
        this.handleJsonResponse(exchange, (JsonResponse) result);
      } else if (result instanceof StreamResponse) {
        this.handleStreamResponse(exchange, (StreamResponse) result);
      } else if (result instanceof MustacheResponse) {
        this.handleMustacheResponse(exchange, (MustacheResponse) result);
      } else if (result instanceof CSVResponse) {
        this.handleCsvResponse(exchange, (CSVResponse) result);
      } else if (result instanceof FileDownloadResponse) {
        this.handleFileDownloadResponse(exchange, (FileDownloadResponse) result);
      } else {
        throw new UnsupportedOperationException("Cannot handle responses of type ");
      }
    } catch (FileNotFoundException e) {
      handleNotFoundException(exchange);
    } catch (UnsupportedOperationException e) {
      handleBadMethodException(exchange);
    } catch (UnauthorizedException e) {
      handleUnauthorized(exchange);
    } catch (IllegalArgumentException e) {
      handleBadArguments(exchange);
    } catch (Exception e) {
      handleServerException(exchange, e);
    }
    exchange.close();
  }

  public ParsedHandler parseHandler(String[] parts) throws FileNotFoundException {
    for (int i = 0; i < pathRules.size(); i++) {
      Map<String, String> paramsMatched = match(parts, pathRules.get(i));
      if (paramsMatched != null) {
        return new ParsedHandler(pathHandlers.get(i).get(), paramsMatched);
      }
    }
    throw new FileNotFoundException("No matching handler for /" + String.join("/", parts));
  }

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

  void handleTextResponse(HttpExchange exchange, TextResponse response) throws IOException {
    exchange.sendResponseHeaders(200, response.text.length());
    exchange.getResponseBody().write(response.text.getBytes());
  }

  void handleJsonResponse(HttpExchange exchange, JsonResponse response) throws IOException {
    // HACK - automate this properly.
    exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "content-type");
    exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "http://localhost:3000");
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, response.json.length());
    exchange.getResponseBody().write(response.json.getBytes());
  }

  void handleStreamResponse(HttpExchange exchange, StreamResponse response) throws IOException {
    exchange.sendResponseHeaders(200, response.length);
    IOUtils.copy(response.stream, exchange.getResponseBody());
  }

  void handleMustacheResponse(HttpExchange exchange, MustacheResponse response) throws IOException {
    // TODO - stream response?
    StringWriter writer = new StringWriter();
    mustacheFactory.compile(response.templateName).execute(writer, response.input);

    exchange.getResponseHeaders().set("Content-Type", "text/html");
    exchange.sendResponseHeaders(200, writer.getBuffer().length());
    exchange.getResponseBody().write(writer.getBuffer().toString().getBytes());
    writer.close();
  }

  void handleCsvResponse(HttpExchange exchange, CSVResponse response) throws IOException {
    String disposition = String.format("attachment; filename=\"%s\"", response.fileName);
    exchange.getResponseHeaders().set("Content-Type", "text/csv");
    exchange.getResponseHeaders().set("Content-Disposition", disposition);

    exchange.sendResponseHeaders(200, response.csvContent.length());
    exchange.getResponseBody().write(response.csvContent.getBytes());
  }

  void handleFileDownloadResponse(HttpExchange exchange, FileDownloadResponse response) throws IOException {
    String disposition = String.format("attachment; filename=\"%s\"", response.fileName);
    exchange.getResponseHeaders().set("Content-Type", response.mimeType);
    exchange.getResponseHeaders().set("Content-Disposition", disposition);

    exchange.sendResponseHeaders(200, response.length);
    IOUtils.copy(response.stream, exchange.getResponseBody());
  }

  void handleBadArguments(HttpExchange exchange) throws IOException {
    exchange.sendResponseHeaders(400, 0);
  }

  void handleUnauthorized(HttpExchange exchange) throws IOException {
    exchange.sendResponseHeaders(401, 0);
  }

  void handleNotFoundException(HttpExchange exchange) throws IOException {
    exchange.sendResponseHeaders(404, 0);
  }

  void handleBadMethodException(HttpExchange exchange) throws IOException {
    exchange.sendResponseHeaders(405, 0);
  }

  void handleServerException(HttpExchange exchange, Exception e) throws IOException {
    System.err.println("500! Error = " + e.getMessage());
    e.printStackTrace(System.err);
    exchange.sendResponseHeaders(500, 0);
  }
}
