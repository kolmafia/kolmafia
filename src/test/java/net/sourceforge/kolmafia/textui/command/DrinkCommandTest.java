package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.HttpClientWrapper.setupFakeClient;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath;
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

      try (var _ = new Cleanups(withItem(ItemPool.STILLSUIT), withProperty("familiarSweat", 20))) {
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

      try (var _ = new Cleanups(withProperty("familiarSweat"), withContinuationState())) {
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

      try (var _ =
          new Cleanups(
              withItem(ItemPool.STILLSUIT),
              withProperty("familiarSweat", 8),
              withContinuationState())) {
        String output = execute("stillsuit distillate");
        var requests = getRequests();
        assertThat(output, containsString("You need at least 10 drams of familiar sweat"));
        assertErrorState();
        assertThat(requests, hasSize(0));
      }
    }
  }

  @Test
  public void canDrinkInGreyYou() {
    try (var _ =
        new Cleanups(
            withPath(AscensionPath.Path.GREY_YOU),
            withClass(AscensionClass.GREY_GOO),
            withItem(ItemPool.BOTTLE_OF_GIN))) {
      String output = execute("bottle of gin");
      assertContinueState();
      assertThat(output, containsString("Drinking 1 bottle of gin"));
    }
  }
}
