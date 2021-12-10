package net.sourceforge.kolmafia.textui.function;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;

public abstract class LibraryClassFunction {
  String functionName;

  LibraryClassFunction(final String functionName) {
    this.functionName = functionName;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  public @interface LibraryFunctionOverload {
    String returns() default "void";
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER})
  public @interface LibraryFunctionParameter {
    String value();
  }

  public final List<Function> getFunctions() {
    List<Function> functions = new ArrayList<>();

    Class<? extends LibraryClassFunction> clazz = this.getClass();
    for (Method method : clazz.getDeclaredMethods()) {
      if (!method.isAnnotationPresent(LibraryFunctionOverload.class)) {
        continue;
      }

      Type returnType;

      Class<? extends Value> rt = (Class<? extends Value>) method.getReturnType();
      try {
        Field classType = rt.getField("classType");
        returnType = (Type) classType.get(null);
      } catch (NoSuchFieldException | IllegalAccessException e) {
        continue;
      }

      Parameter[] params = method.getParameters();
      Type[] paramTypes = new Type[params.length - 1];

      // Skip the first parameter as it is always the runtime.
      for (int i = 1; i < params.length; i++) {
        LibraryFunctionParameter annotation =
            params[i].getAnnotation(LibraryFunctionParameter.class);
        paramTypes[i - 1] = DataTypes.simpleTypes.find(annotation.value());
      }

      functions.add(new LibraryFunction(this.functionName, returnType, paramTypes, clazz, method));
    }

    return functions;
  }
}
