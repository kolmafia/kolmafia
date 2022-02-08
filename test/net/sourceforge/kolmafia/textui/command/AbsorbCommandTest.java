package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbsorbCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  public AbsorbCommandTest() {
    this.command = "absorb";
  }

  @Test
  void mustBeInNoob() {
    var cleanups = inPath(Path.NONE);
    try (cleanups) {
      String output = execute("1 helmet turtle");
      assertThat(output, containsString("not in a Gelatinous Noob"));
      assertErrorState();
    }
  }

  @Test
  void mustSpecifyItem() {
    var cleanups = inPath(Path.GELATINOUS_NOOB);
    try (cleanups) {
      String output = execute("");

      assertThat(output, containsString("No items specified"));
      assertErrorState();
    }
  }

  @Test
  void mustHaveAbsorbs() {
    var cleanups = new Cleanups(inPath(Path.GELATINOUS_NOOB), usedAbsorbs(100));
    try (cleanups) {
      String output = execute("1 helmet turtle");

      assertThat(output, containsString("Cannot absorb items"));
      assertErrorState();
    }
  }

  @Test
  void mustSpecifyValidItem() {
    var cleanups = new Cleanups(inPath(Path.GELATINOUS_NOOB));
    try (cleanups) {
      String output = execute("invalid item");

      assertThat(output, containsString("What item"));
      assertContinueState();
    }
  }

  @Test
  void mustHaveItem() {
    var cleanups = new Cleanups(inPath(Path.GELATINOUS_NOOB));
    try (cleanups) {
      String output = execute("1 dirty bottlecap");

      assertThat(output, containsString("Item not accessible"));
      assertErrorState();
    }
  }

  @Test
  void canAbsorbItems() {
    var cleanups = new Cleanups(inPath(Path.GELATINOUS_NOOB), addItem("Half a Purse", 15));
    try (cleanups) {
      String output = execute("15 Half a Purse");

      assertThat(output, containsString("Absorbed 15 Half Purses"));
      assertContinueState();
    }
  }
}
