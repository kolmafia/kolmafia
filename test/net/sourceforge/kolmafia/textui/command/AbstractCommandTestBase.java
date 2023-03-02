package net.sourceforge.kolmafia.textui.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.CliCaller;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;

public abstract class AbstractCommandTestBase {
  protected String command = "abort";

  public String execute(final String params) {
    return execute(params, false);
  }

  public String execute(final String params, final boolean check) {
    return CliCaller.callCli(this.command, params, check);
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
