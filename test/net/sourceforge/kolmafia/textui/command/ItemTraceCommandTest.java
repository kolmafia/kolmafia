package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import internal.helpers.Cleanups;
import internal.helpers.Player;
import internal.helpers.RequestLoggerOutput;
import org.junit.jupiter.api.Test;

public class ItemTraceCommandTest extends AbstractCommandTestBase {

  public ItemTraceCommandTest() {
    this.command = "itrace";
  }

  @Test
  public void tracesItemsAddedToInventory() {
    execute("hair spray");

    RequestLoggerOutput.startStream();
    Cleanups cleanups = Player.addItem("hair spray");
    try (cleanups) {
      var text = RequestLoggerOutput.stopStream();
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
