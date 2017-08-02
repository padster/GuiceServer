package com.github.padster.guiceserver.auth;

import com.github.padster.guiceserver.handlers.Handler;
import com.github.padster.guiceserver.handlers.RouteParser;
import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpExchange;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpCookie;

/**
 * GuiceServer authentication system.
 * This will intercept invocations of handlers marked with @LoginRequired,
 * and force authentication. If none is found, users will be redirected to the login page.
 */
public class RouteAuthenticator extends Authenticator {
  public static final String COOKIE_NAME = "_gsID";

  private final RouteParser routeParser;

  @Inject
  public RouteAuthenticator(RouteParser routeParser) {
    this.routeParser = routeParser;
  }

  @Override public Authenticator.Result authenticate(HttpExchange exchange) {
    System.out.println("Authenticating: " + exchange.getRequestURI().getPath());

    final Handler handler;
    try {
      handler = this.routeParser.parseHandler(exchange).handler;
    } catch (FileNotFoundException e) {
      return new Authenticator.Failure(404);
    }

    if (handler == null) {
      // No handler, so safe to skip
      return new Authenticator.Success(null);
    }

    if (handler.getClass().getAnnotation(AuthAnnotations.LoginRequired.class) != null) {
      // Authentication needed!
      return handleAuth(exchange);
    }

    // Otherwise, continue as usual with no user.
    // TODO: fill in user details anyway if cookie is present?
    return new Authenticator.Success(null);
  }

  // Handles Authentication verification for this request.
  private Authenticator.Result handleAuth(HttpExchange exchange) {
    // Read the Cookie header, looking for Guice Server authentication.
    String cookieStr = exchange.getRequestHeaders().getFirst("Cookie");
    HttpCookie cookie = getLoginCookie(cookieStr);

    if (cookie == null) {
      // Cookie not set - redirecting!
      // TODO: Set URL to head back to after.
      exchange.getResponseHeaders().set("Location", "/lab/login");
      try {
        exchange.sendResponseHeaders(302, -1L);
      } catch (IOException e) {} // Ignore, not much we can do...
      return new Authenticator.Failure(302);
    }

    // TODO: Validate the cookie value here.
    return new Authenticator.Success(null);
  }

  /** @return the first Cookie associated to GuiceServer login, if found. */
  private HttpCookie getLoginCookie(String header) {
    if (header != null) {
      for (HttpCookie cookie : HttpCookie.parse(header)) {
        if (COOKIE_NAME.equals(cookie.getName())) {
          return cookie;
        }
      }
    }
    return null;
  }
}
