package net.sourceforge.kolmafia.textui.javascript;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ScriptException;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class LibraryFunctionStubTest {
  private static Context cx;
  private static Scriptable scope;

  @BeforeAll
  public static void beforeAll() {
    LibraryFunctionStubTest.cx = Context.enter();

    cx.setLanguageVersion(Context.VERSION_ES6);
    cx.setOptimizationLevel(1);
    cx.setTrackUnhandledPromiseRejections(true);

    LibraryFunctionStubTest.scope = cx.initSafeStandardObjects();
  }

  @AfterAll
  public static void afterAll() {
    Context.exit();
  }

  private static List<Arguments> callFunctionProvider() {
    return List.of(
        Arguments.of(
            "truncate(1.0)", "truncate", new Object[] {DataTypes.makeFloatValue(1.0)}, null),
        Arguments.of(
            "max(1, 2, 3)",
            "max",
            new Object[] {
              DataTypes.makeIntValue(1), DataTypes.makeIntValue(2), DataTypes.makeIntValue(3)
            },
            null),
        Arguments.of(
            "monsterHp(1, {})",
            "monster_hp",
            new Object[] {DataTypes.makeIntValue(1), new Value(DataTypes.ANY_TYPE)},
            "Function 'monster_hp( int, null )' undefined.  This script may require a more recent version of KoLmafia and/or its supporting scripts."));
  }

  @ParameterizedTest
  @MethodSource("callFunctionProvider")
  public void callFunctionThrowsOnlyIfExpected(
      String script, String ashFunction, Object[] args, String expectedException) {
    try {
      var runtime = new JavascriptRuntime(script);
      runtime.initRuntimeLibrary(cx, scope, null);
      var stub =
          new LibraryFunctionStub(
              scope, ScriptableObject.getFunctionPrototype(scope), runtime, ashFunction);
      stub.call(cx, scope, cx.newObject(scope), args);
    } catch (ScriptException e) {
      assertThat(e.getMessage(), is(expectedException));
    }
  }
}
