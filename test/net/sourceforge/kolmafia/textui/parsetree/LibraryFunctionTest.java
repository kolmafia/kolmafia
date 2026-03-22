package net.sourceforge.kolmafia.textui.parsetree;

import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withTurnsPlayed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LibraryFunctionTest {
  private AshRuntime runtime;
  private static FunctionList allFunctions;

  @BeforeAll
  static void beforeAll() {
    allFunctions = RuntimeLibrary.getFunctions();
  }

  @BeforeEach
  void beforeEach() {
    this.runtime = new AshRuntime();
  }

  @Test
  void withDescriptionStoresAndReturnsDescription() {
    var fn = allFunctions.findFunctions("total_turns_played")[0];
    assertThat(fn, is(org.hamcrest.Matchers.instanceOf(LibraryFunction.class)));
    var lf = (LibraryFunction) fn;
    assertThat(lf.getDescription(), nullValue());

    var result = lf.withDescription("test description");
    assertThat(result, is(lf)); // returns this for chaining
    assertThat(lf.getDescription(), is("test description"));

    // clean up
    lf.withDescription(null);
  }

  @Test
  void seededDescriptionsExist() {
    var printFn = allFunctions.findFunctions("print")[0];
    assertThat(
        ((LibraryFunction) printFn).getDescription(),
        is("Prints a message to the CLI and session log."));

    var adventureFn = allFunctions.findFunctions("adventure")[0];
    assertThat(
        ((LibraryFunction) adventureFn).getDescription(),
        is(
            "Spends the specified number of adventures at a location. Returns true if all adventures were used."));

    var getPropertyFn = allFunctions.findFunctions("get_property")[0];
    assertThat(
        ((LibraryFunction) getPropertyFn).getDescription(),
        is("Returns the value of a KoLmafia property."));
  }

  @Test
  void seededParamDescriptionsExist() {
    var fns = allFunctions.findFunctions("visit_url");
    // Find the 4-param overload (string, usePostMethod, encoded)
    for (var fn : fns) {
      if (fn.getVariableReferences().size() == 3) {
        var encodedParam = fn.getVariableReferences().get(2);
        assertThat(encodedParam.getDescription(), is("If true, the URL is already URL-encoded"));
        break;
      }
    }
  }

  @Test
  void execute() {
    var cleanups = withTurnsPlayed(22);
    try (cleanups) {
      var totalTurnsPlayed = allFunctions.findFunctions("total_turns_played")[0];
      var result = totalTurnsPlayed.execute(runtime, new Object[] {runtime});

      assertThat(result.type, is(DataTypes.INT_TYPE));
      assertThat(result.intValue(), is(22L));
    }
  }

  @Test
  void executeRespectsContinuationState() {
    var cleanups =
        new Cleanups(withTurnsPlayed(22), withContinuationState(KoLConstants.MafiaState.ERROR));
    try (cleanups) {
      var totalTurnsPlayed = allFunctions.findFunctions("total_turns_played")[0];
      var result = totalTurnsPlayed.execute(runtime, new Object[] {runtime});

      assertThat(result, nullValue());
    }
  }
}
