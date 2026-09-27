package net.sourceforge.kolmafia.combat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.javascript.JavascriptRuntime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class MacrofierJavascriptFilterTest {
  private static Context cx;
  private static Scriptable scope;

  @BeforeAll
  public static void beforeAll() {
    cx = Context.enter();
    cx.setLanguageVersion(Context.VERSION_ES6);
    scope = cx.initSafeStandardObjects();
  }

  @AfterAll
  public static void afterAll() {
    Context.exit();
  }

  @AfterEach
  public void cleanup() {
    Macrofier.resetMacroOverride();
    FightRequest.combatFilterThatDidNothing = null;
    KoLmafia.forceContinue();
  }

  private JavascriptRuntime withFilter(String javascript) {
    var controller = new JavascriptRuntime("");
    var baseFunction = (BaseFunction) cx.evaluateString(scope, javascript, "test filter", 1, null);
    Macrofier.setJavaScriptMacroOverride(baseFunction, scope, scope);
    Macrofier.setMacroOverride("filterPlaceholder", controller);
    controller.setState(ScriptRuntime.State.NORMAL);
    return controller;
  }

  @Test
  public void filterSuccessDuringAbortSetsStateExit() {
    var controller = withFilter("(round, monster, page) => 'attack'");
    KoLmafia.updateDisplay(KoLConstants.MafiaState.ABORT, "Aborted state");

    assertThat(Macrofier.macrofy(), is("abort"));
    assertThat(controller.getState(), is(ScriptRuntime.State.EXIT));
  }

  @Test
  public void filterSuccessSetsStateNormal() {
    var controller = withFilter("(round, monster, page) => 'attack'");

    assertThat(Macrofier.macrofy(), is("attack"));
    assertThat(controller.getState(), is(ScriptRuntime.State.NORMAL));
  }

  @Test
  public void filterThrowSetsStateExit() {
    var controller =
        withFilter("(function(round, monster, page) { throw new Error('This is an error'); })");

    Macrofier.macrofy();

    assertThat(controller.getState(), is(ScriptRuntime.State.EXIT));
  }
}
