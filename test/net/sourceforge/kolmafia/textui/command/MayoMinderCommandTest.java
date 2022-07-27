package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withWorkshedItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MayoMinderCommandTest extends AbstractCommandTestBase {

  public MayoMinderCommandTest() {
    this.command = "mayominder";
  }

  @BeforeAll
  public static void setup() {
    KoLCharacter.reset("mayominder");
    Preferences.reset("mayominder");
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  @Test
  void mustMakeCommand() {
    String output = execute("");

    assertContinueState();
    assertThat(output, containsString("Usage: mayominder"));
  }

  @Test
  void mustMakeValidCommand() {
    String output = execute("foobar");

    assertErrorState();
    assertThat(output, containsString("I don't understand what 'foobar' mayo is."));
  }

  @Test
  void mustHaveClinic() {
    String output = execute("stat");

    assertErrorState();
    assertThat(output, containsString("Mayo clinic not installed"));
  }

  @Test
  void mustHaveMayoMinder() {
    var cleanups = withWorkshedItem(ItemPool.MAYO_CLINIC);

    try (cleanups) {
      String output = execute("mayodiol");

      assertErrorState();
      assertThat(output, containsString("You cannot obtain a Mayo Minder"));
    }
  }

  @Test
  void canSetMinder() {
    var cleanups =
        new Cleanups(withWorkshedItem(ItemPool.MAYO_CLINIC), withItem(ItemPool.MAYO_MINDER));

    try (cleanups) {
      execute("mayostat");

      assertContinueState();
      var requests = getRequests();
      assertThat(requests, not(empty()));
      assertPostRequest(requests.get(0), "/inv_use.php", "which=3&whichitem=8285");
      assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1076&option=3");
    }
  }
}
