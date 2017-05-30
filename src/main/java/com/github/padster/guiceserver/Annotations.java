package com.github.padster.guiceserver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import com.google.inject.BindingAnnotation;

public final class Annotations {
  private Annotations() {}

  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  public @interface Bindings {}

  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  public @interface ServerPort {}

  // TODO - pull out into auth module.
  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  public @interface CurrentUser {}
}
