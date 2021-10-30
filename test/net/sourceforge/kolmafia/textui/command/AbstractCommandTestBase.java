package net.sourceforge.kolmafia.textui.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

public abstract class AbstractCommandTestBase {
  protected String command = "abort";

  public String execute(final String params) {
    var outputStream = new ByteArrayOutputStream();
    RequestLogger.openCustom(new PrintStream(outputStream));
    var cli = new KoLmafiaCLI(System.in);
    cli.executeCommand(this.command, params);
    RequestLogger.closeCustom();
    return outputStream.toString();
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
