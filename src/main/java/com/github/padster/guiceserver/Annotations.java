package com.github.padster.guiceserver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;
import com.google.inject.ScopeAnnotation;

public final class Annotations {
  private Annotations() {}

  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  public @interface Bindings {}

  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  public @interface ServerPort {}

  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  public @interface ClientUri {}

  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  public @interface CurrentUser {}

  @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
  @Retention(RetentionPolicy.RUNTIME)
  @ScopeAnnotation
  public @interface RequestScoped {}
}
