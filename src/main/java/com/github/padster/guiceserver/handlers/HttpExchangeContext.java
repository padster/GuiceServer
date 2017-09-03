package com.github.padster.guiceserver.handlers;

import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.fileupload.RequestContext;

import java.io.IOException;
import java.io.InputStream;

/** Wrap Sun's HttpExchange as Apache's file upload RequestContext */
public class HttpExchangeContext implements RequestContext {
  private final HttpExchange exchange;

  public HttpExchangeContext(HttpExchange exchange) {
    this.exchange = exchange;
  }

  @Override public String getCharacterEncoding() {
    return "UTF-8";
  }

  @Override public String getContentType() {
    return exchange.getRequestHeaders().getFirst("Content-type");
  }

  @Override public int getContentLength() {
    return 0; //tested to work with 0 as return
  }

  @Override public InputStream getInputStream() throws IOException {
    return exchange.getRequestBody();
  }
}
