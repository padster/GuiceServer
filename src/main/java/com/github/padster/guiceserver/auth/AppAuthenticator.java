package com.github.padster.guiceserver.auth;

import com.sun.net.httpserver.HttpPrincipal;

import java.net.URI;

/** API GuiceServer authentication uses to talk back to the calling app. */
public interface AppAuthenticator {
  /** Callback triggered before handler is run, when no user was found. */
  void handleNoUser();

  /**
   * Callback triggered before handler when user token is found.
   * @return Username if token is valid, otherwise null.
   */
  HttpPrincipal handleAuthToken(String token);

  /** @return Login URL, given the path we were trying to access. */
  URI buildLoginURI(String pathFrom);
}
