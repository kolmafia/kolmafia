package net.sourceforge.kolmafia.textui.javascript;

import java.lang.reflect.Method;
import java.util.Arrays;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class EnumeratedWrapperPrototype extends ScriptableObject {
  private static final long serialVersionUID = 1L;

  private final Class<?> recordValueClass;
  private final Type type;

  public EnumeratedWrapperPrototype(Class<?> recordValueClass, Type type) {
    this.recordValueClass = recordValueClass;
    this.type = type;
  }

  public void initToScope(Context cx, Scriptable scope, Scriptable runtimeLibrary) {
    setPrototype(ScriptableObject.getObjectPrototype(scope));

    if (recordValueClass != null) {
      for (Method method : recordValueClass.getDeclaredMethods()) {
        if (method.getName().startsWith("get_")) {
          ProxyRecordMethodWrapper methodWrapper =
              new ProxyRecordMethodWrapper(
                  scope, ScriptableObject.getFunctionPrototype(scope), method);
          String methodShortName =
              JavascriptRuntime.toCamelCase(method.getName().replace("get_", ""));
          setGetterOrSetter(methodShortName, 0, methodWrapper, false);
        }
      }
    }

    try {
      Method constructorMethod = EnumeratedWrapper.class.getDeclaredMethod("constructDefaultValue");
      FunctionObject constructor = new FunctionObject(getClassName(), constructorMethod, scope);
      constructor.addAsConstructor(scope, this);
      if (runtimeLibrary != null) {
        ScriptableObject.defineProperty(
            runtimeLibrary, getClassName(), constructor, DONTENUM | READONLY | PERMANENT);
      }

      Method getMethod =
          EnumeratedWrapper.class.getDeclaredMethod(
              "genericGet", Context.class, Scriptable.class, Object[].class, Function.class);
      Function getFunction = new FunctionObject("get", getMethod, scope);
      ScriptableObject.defineProperty(
          getFunction, "typeName", getClassName(), DONTENUM | READONLY | PERMANENT);
      constructor.defineProperty("get", getFunction, DONTENUM | READONLY | PERMANENT);

      Method allMethod =
          EnumeratedWrapper.class.getDeclaredMethod(
              "all", Context.class, Scriptable.class, Object[].class, Function.class);
      Function allFunction = new FunctionObject("all", allMethod, scope);
      ScriptableObject.defineProperty(
          allFunction, "typeName", getClassName(), DONTENUM | READONLY | PERMANENT);
      constructor.defineProperty("all", allFunction, DONTENUM | READONLY | PERMANENT);

      constructor.defineProperty(
          "none", EnumeratedWrapper.getNone(scope, type), DONTENUM | READONLY | PERMANENT);

      constructor.sealObject();

      for (String methodName : new String[] {"toString", "toJSON"}) {
        var method =
            Arrays.stream(EnumeratedWrapper.class.getMethods())
                .filter(m -> m.getName().equals(methodName))
                .findAny()
                .orElseThrow(NoSuchMethodException::new);
        FunctionObject functionObject = new FunctionObject(methodName, method, scope);
        defineProperty(methodName, functionObject, DONTENUM | READONLY | PERMANENT);
        functionObject.sealObject();
      }
    } catch (NoSuchMethodException e) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, "NoSuchMethodException: " + e.getMessage());
    }

    sealObject();
  }

  public static EnumeratedWrapperPrototype getPrototypeInstance(Scriptable scope, Type type) {
    Scriptable topScope = ScriptableObject.getTopLevelScope(scope);
    Object constructor = ScriptableObject.getProperty(topScope, getClassName(type));
    if (!(constructor instanceof Scriptable)) {
      return null;
    }
    Object result = ScriptableObject.getProperty((Scriptable) constructor, "prototype");
    return result instanceof EnumeratedWrapperPrototype
        ? (EnumeratedWrapperPrototype) result
        : null;
  }

  public static String getClassName(Type type) {
    return JavascriptRuntime.capitalize(type.getName());
  }

  @Override
  public String getClassName() {
    return EnumeratedWrapperPrototype.getClassName(type);
  }
}
