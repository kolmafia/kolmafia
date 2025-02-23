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
