package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withoutItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ThrowItemCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ThrowItemCommandTest");
    Preferences.reset("ThrowItemCommandTest");
  }

  public ThrowItemCommandTest() {
    this.command = "throw";
  }

  @Test
  void mustSpecifyVictim() {
    String output = execute("brick");

    assertErrorState();
    assertThat(output, containsString("No <s>victim</s>recipient specified."));
  }

  @Test
  void cannotThrowAfterDailyLimit() {
    var cleanups = withProperty("_crimboTraining", true);

    try (cleanups) {
      String output = execute("crimbo training manual at gausie");

      assertErrorState();
      assertThat(
          output, containsString("You cannot throw any more Crimbo training manuals today."));
    }
  }

  @Test
  void cannotThrowUnthrowables() {
    String output = execute("toast at gausie");

    assertErrorState();
    assertThat(output, containsString("The toast is not properly balanced for throwing."));
  }

  @Test
  void mustHaveToThrow() {
    var cleanups = withoutItem(ItemPool.BRICK);

    try (cleanups) {
      String output = execute("brick at gausie");

      assertErrorState();
      assertThat(output, startsWith("You need 1 more brick to continue"));
    }
  }

  @Test
  void canSuccessfullyThrow() {
    HttpClientWrapper.setupFakeClient();

    var cleanups = withItem(ItemPool.BRICK);

    try (cleanups) {
      String output = execute("brick at gausie");

      var requests = getRequests();

      assertContinueState();
      assertThat(output, startsWith("Throwing brick at gausie..."));

      assertThat(requests, hasSize(1));
      assertPostRequest(
          requests.get(0),
          "/curse.php",
          "action=use&whichitem=" + ItemPool.BRICK + "&targetplayer=gausie");
    }
  }
}
