package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HatterCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("HatterCommand");
    HttpClientWrapper.setupFakeClient();
    ChoiceManager.handlingChoice = false;
  }

  public HatterCommandTest() {
    this.command = "hatter";
  }

  @Test
  void notAllowedInTCRS() {
    var cleanups =
        new Cleanups(withPath(AscensionPath.Path.CRAZY_RANDOM_SUMMER_TWO), withContinuationState());

    try (cleanups) {
      String output = execute("10");

      assertThat(output, containsString("You can't get Down the Rabbit Hole"));
      assertErrorState();
    }
  }
}
