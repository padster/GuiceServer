package com.github.padster.guiceserver.auth;

import com.github.padster.guiceserver.handlers.Handler;
import com.github.padster.guiceserver.handlers.RouteParser;
import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

import jakarta.inject.Inject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;

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

  /** App says user logged in, so give the user an authenticated token. */
  public void handleLogin(HttpExchange exchange, String token) {
    HttpCookie cookie = new HttpCookie(COOKIE_NAME, token);
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    exchange.getResponseHeaders().set("Set-Cookie", cookie.toString());
  }

  /** App says user logged out, so clear their token. */
  public void handleLogout(HttpExchange exchange) {
    HttpCookie cookie = new HttpCookie(COOKIE_NAME, "");
    // For some reason, HttpCookie doesn't support this!?
    String expired = cookie.toString() + "; Max-Age=0";
    exchange.getResponseHeaders().set("Set-Cookie", expired);
  }

  @Override public Authenticator.Result authenticate(HttpExchange exchange) {
    Handler handler = null;
    System.out.println("AUTHENTICATE");
    try {
      handler = this.routeParser.parseHandler(exchange).handler;
    } catch (FileNotFoundException e) {
      // Fall through to no-handler logic below.
    }
    if (handler == null) {
      // No handler, so safe to skip
      System.out.println(">> No handler found, skipping auth");
      return new Authenticator.Success(null);
    }

    boolean loginRequired =
        (handler.getClass().getAnnotation(AuthAnnotations.LoginRequired.class) != null);
    System.out.println(">> Handler requires login: " + loginRequired);
    return handleAuth(exchange, loginRequired);
  }

  // Handles Authentication verification for this request.
  private Authenticator.Result handleAuth(HttpExchange exchange, boolean loginRequired) {
    // Handle preflight request - can't redirect, and doesn't need auth.
    if ("OPTIONS".equals(exchange.getRequestMethod())) {
      return new Authenticator.Success(null);
    }

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
        System.out.println("Principal = " + principal.getUsername());
        return new Authenticator.Success(principal);
      }
    }
  }

  /** @return the first Cookie associated to GuiceServer login, if found. */
  private HttpCookie getLoginCookie(String header) {
    // Remove "g_state={...};" from within cookie header if it's there:
    System.out.println("Cookie before g_state removal: " + header);
    if (header != null) {
      // Use regex to match and remove the entire g_state block
      header = header.replaceAll("g_state=\\{[^}]*\\};?\\s*", "");
      // Example: 'g_state={"i_l":0,"i_ll":1762714440988}; _gsID=abcde' -> ' _gsID=abcde'
    }
    System.out.println("Cookie after g_state removal: " + header);

    if (header == null) {
      return null;
    }

    try {
      List<HttpCookie> cookies = HttpCookie.parse(header);
      for (HttpCookie cookie : cookies) {
        if (COOKIE_NAME.equals(cookie.getName())) {
          return cookie;
        }
      }
    } catch (Exception e) {
      System.out.println("Error parsing cookies: " + e.getMessage());
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
