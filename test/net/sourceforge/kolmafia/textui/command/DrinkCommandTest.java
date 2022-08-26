package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.HttpClientWrapper.setupFakeClient;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DrinkCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset(true);
    KoLCharacter.reset("DrinkCommandTest");
    Preferences.reset("DrinkCommandTest");
  }

  public DrinkCommandTest() {
    this.command = "drink";
  }

  @Nested
  class StillsuitDistillate {
    @Test
    public void canDrinkDistillate() {
      setupFakeClient();

      var cleanups = new Cleanups(withItem(ItemPool.STILLSUIT), withProperty("familiarSweat", 20));

      try (cleanups) {
        String output = execute("stillsuit distillate");
        var requests = getRequests();
        assertThat(output, containsString("Creating 1 stillsuit distillate"));
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/inventory.php", "action=distill");
        assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1476&option=1");
      }
    }

    @Test
    public void cannotDrinkDistillateWithoutStillSuiit() {
      setupFakeClient();

      var cleanups = new Cleanups(withProperty("familiarSweat"), withContinuationState());

      try (cleanups) {
        String output = execute("stillsuit distillate");
        var requests = getRequests();
        assertThat(output, containsString("You don't have a tiny stillsuit"));
        assertErrorState();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void cannotDrinkDistillateWithout10Drams() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem(ItemPool.STILLSUIT),
              withProperty("familiarSweat", 8),
              withContinuationState());

      try (cleanups) {
        String output = execute("stillsuit distillate");
        var requests = getRequests();
        assertThat(output, containsString("You need at least 10 drams of familiar sweat"));
        assertErrorState();
        assertThat(requests, hasSize(0));
      }
    }
  }
}
