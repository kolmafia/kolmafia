package net.sourceforge.kolmafia.textui.function;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public abstract class LibraryFunction {
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  public @interface LibraryFunctionOverload {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER})
  public @interface LibraryFunctionParameter {
    String value();
  }

  static final String name = null;
}
