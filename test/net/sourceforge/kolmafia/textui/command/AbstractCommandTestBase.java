package net.sourceforge.kolmafia.textui.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.RequestLoggerOutput;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.StaticEntity;

public abstract class AbstractCommandTestBase {
  protected String command = "abort";

  public String execute(final String params) {
    return execute(params, false);
  }

  public String execute(final String params, final boolean check) {
    RequestLoggerOutput.startStream();
    var cli = new KoLmafiaCLI(System.in);
    KoLmafiaCLI.isExecutingCheckOnlyCommand = check;
    cli.executeCommand(this.command, params);
    KoLmafiaCLI.isExecutingCheckOnlyCommand = false;
    return RequestLoggerOutput.stopStream();
  }

  public static void assertState(final MafiaState state) {
    assertEquals(state, StaticEntity.getContinuationState());
  }

  public static void assertContinueState() {
    assertState(MafiaState.CONTINUE);
  }

  public static void assertErrorState() {
    assertState(MafiaState.ERROR);
  }
}
