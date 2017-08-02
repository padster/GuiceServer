package com.github.padster.guiceserver.auth;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Annotations for use with GuiceServer's authentication system. */
public class AuthAnnotations {
  // Handlers marked with this will require authentication.
  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  public @interface LoginRequired {}
}
