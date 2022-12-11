package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.HttpClientWrapper.setupFakeClient;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EatCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("EatCommandTest");
    Preferences.reset("EatCommandTest");
  }

  public EatCommandTest() {
    this.command = "eat";
  }

  @Nested
  class GlitchSeasonReward {
    @Test
    public void canEatGlitchSeasonReward() {
      setupFakeClient();

      var cleanups = new Cleanups(withItem(ItemPool.GLITCH_ITEM));

      try (cleanups) {
        String output = execute("glitch season");
        var requests = getRequests();
        assertThat(output, containsString("Eating 1 [glitch season reward name]..."));
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/inv_eat.php", "whichitem=10207&ajax=1&quantity=1");
      }
    }

    @Test
    public void canEatGlitchSeasonRewardAfterImplementing() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem(ItemPool.GLITCH_ITEM), withProperty("_glitchItemImplemented", true));

      try (cleanups) {
        String output = execute("glitch season");
        var requests = getRequests();
        assertThat(output, containsString("Eating 1 [glitch season reward name]..."));
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/inv_eat.php", "whichitem=10207&ajax=1&quantity=1");
      }
    }
  }

  @Test
  public void canEatInGreyYou() {
    var cleanups =
        new Cleanups(
            withPath(Path.GREY_YOU), withClass(AscensionClass.GREY_GOO), withItem(ItemPool.TOMATO));

    try (cleanups) {
      String output = execute("tomato");
      assertContinueState();
      assertThat(output, containsString("Eating 1 tomato"));
    }
  }
}
