package com.github.padster.guiceserver.handlers;

import com.github.mustachejava.MustacheFactory;
import com.github.padster.guiceserver.handlers.RouteHandlerResponses.*;
import com.github.padster.guiceserver.handlers.RouteParser.ParsedHandler;
import com.google.common.base.Preconditions;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;

import jakarta.inject.Inject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;

/**
 * Logic for finding which handler to route to, and converting its results to an HttpResponse.
 * Safe to ignore this file.
 */
public class RouteHandler implements HttpHandler {
  public static class UnauthorizedException extends RuntimeException {}

  private final RouteParser routeParser;
  private final MustacheFactory mustacheFactory;

  @Inject public RouteHandler(
      RouteParser routeParser, MustacheFactory mustacheFactory
  ) {
    this.routeParser = routeParser;
    this.mustacheFactory = mustacheFactory;
  }

  @Override public void handle(HttpExchange exchange) throws IOException {
    System.out.println("Handle: " + exchange.getRequestURI().getPath());

    try {
      ParsedHandler parsedHandler = this.routeParser.parseHandler(exchange);
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
      } else if (result instanceof RedirectResponse) {
        this.handleRedirectResponse(exchange, (RedirectResponse) result);
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

  void handleRedirectResponse(HttpExchange exchange, RedirectResponse response) throws IOException {
    exchange.getResponseHeaders().set("Location", response.location.toString());
    int code = response.isTemporary ? 302 : 303;
    exchange.sendResponseHeaders(code, -1);
  }

  void handleCsvResponse(HttpExchange exchange, CSVResponse response) throws IOException {
    String disposition = String.format("attachment; filename=\"%s\"", response.fileName);
    exchange.getResponseHeaders().set("Content-Type", "text/csv");
    exchange.getResponseHeaders().set("Content-Disposition", disposition);

    byte[] bytes = response.csvContent.getBytes("UTF-8");
    exchange.sendResponseHeaders(200, bytes.length);
    exchange.getResponseBody().write(bytes);
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
