package net.sourceforge.kolmafia.textui.parsetree;

import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withTurnsPlayed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import internal.helpers.Cleanups;
import java.util.List;
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
    var lf = new LibraryFunction("total_turns_played", DataTypes.INT_TYPE, List.of());
    assertThat(lf.getDescription(), nullValue());

    var result = lf.withDescription("test description");
    assertThat(result, is(lf)); // returns this for chaining
    assertThat(lf.getDescription(), is("test description"));
  }

  @Test
  void seededDescriptionsExist() {
    var printFn = allFunctions.findFunctions("print")[0];
    assertThat(
        ((LibraryFunction) printFn).getDescription(),
        is("Prints a blank line to the CLI and session log."));
  }

  @Test
  void seededParamDescriptionsExist() {
    var fns = allFunctions.findFunctions("visit_url");
    // Find the 3-param overload (url, usePostMethod, encoded)
    var found = false;
    for (var fn : fns) {
      if (fn.getVariableReferences().size() == 3) {
        var encodedParam = fn.getVariableReferences().get(2);
        assertThat(encodedParam.getDescription(), is("If true, the URL is already URL-encoded"));
        found = true;
        break;
      }
    }
    assertThat("Expected a visit_url overload with 3 params", found);
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
