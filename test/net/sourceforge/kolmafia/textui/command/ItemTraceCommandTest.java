package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import internal.helpers.Cleanups;
import internal.helpers.Player;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.RequestLogger;
import org.junit.jupiter.api.Test;

public class ItemTraceCommandTest extends AbstractCommandTestBase {

  public ItemTraceCommandTest() {
    this.command = "itrace";
  }

  @Test
  public void tracesItemsAddedToInventory() {
    execute("hair spray");

    var outputStream = new ByteArrayOutputStream();
    RequestLogger.openCustom(new PrintStream(outputStream));
    Cleanups cleanups = Player.addItem("hair spray");
    try (cleanups) {
      RequestLogger.closeCustom();
      var text = outputStream.toString();
      assertThat(text, containsString("itrace: hair spray = 1"));
    }
  }

  @Test
  public void clearsPreviousItems() {
    execute("hair spray");
    String output = execute("");

    assertThat(output, containsString("Previously watched items have been cleared"));
  }
}
