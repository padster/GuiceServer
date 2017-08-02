package com.github.padster.guiceserver.auth;

import com.github.padster.guiceserver.handlers.Handler;
import com.github.padster.guiceserver.handlers.RouteParser;
import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;

/**
 * GuiceServer authentication system.
 * This will intercept invocations of handlers marked with @LoginRequired,
 * and force authentication. If none is found, users will be redirected to the login page.
 */
public class RouteAuthenticator extends Authenticator {
  public static final String COOKIE_NAME = "_gsID";

  private final AppAuthenticator authenticator;
  private final RouteParser routeParser;

  @Inject
  public RouteAuthenticator(RouteParser routeParser, AppAuthenticator authenticator) {
    this.authenticator = authenticator;
    this.routeParser = routeParser;
  }

  public void handleLogin(HttpExchange exchange, String token) {
    HttpCookie cookie = new HttpCookie(COOKIE_NAME, token);
    exchange.getResponseHeaders().set("Set-Cookie", cookie.toString());
  }

  @Override public Authenticator.Result authenticate(HttpExchange exchange) {
    System.out.println("Authenticating: " + exchange.getRequestURI().getPath());

    Handler handler = null;
    try {
      handler = this.routeParser.parseHandler(exchange).handler;
    } catch (FileNotFoundException e) {
      // Fall through to no-handler logic below.
    }
    if (handler == null) {
      // No handler, so safe to skip
      return new Authenticator.Success(null);
    }

    boolean loginRequired =
        (handler.getClass().getAnnotation(AuthAnnotations.LoginRequired.class) != null);
    return handleAuth(exchange, loginRequired);
  }

  // Handles Authentication verification for this request.
  private Authenticator.Result handleAuth(HttpExchange exchange, boolean loginRequired) {
    // Read the Cookie header, looking for Guice Server authentication.
    String cookieStr = exchange.getRequestHeaders().getFirst("Cookie");
    HttpCookie cookie = getLoginCookie(cookieStr);

    if (cookie == null) {
      if (loginRequired) {
        // Cookie not set - redirecting!
        URI redirect = authenticator.buildLoginURI(exchange.getRequestURI().getPath());
        exchange.getResponseHeaders().set("Location", redirect.toString());
        return sendFail(exchange, 302);
      } else {
        authenticator.handleNoUser();
        return new Authenticator.Success(null);
      }
    } else {
      HttpPrincipal principal = authenticator.handleAuthToken(cookie.getValue());
      if (principal == null) {
        // User permission not allowed.
        return sendFail(exchange, 404);
      } else {
        return new Authenticator.Success(principal);
      }
    }
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

  /** Send response code, and return failed login. */
  private Authenticator.Result sendFail(HttpExchange exchange, int code) {
    try {
      exchange.sendResponseHeaders(code, -1L);
    } catch (IOException e) {} // Ignore, not much we can do...
    return new Authenticator.Failure(code);
  }
}
