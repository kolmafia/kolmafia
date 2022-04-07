package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UmbrellaCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    KoLCharacter.reset("testUser");
    KoLCharacter.reset(true);
    Preferences.setToDefault("umbrellaState");

    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  public UmbrellaCommandTest() {
    this.command = "umbrella";
  }

  private static void hasUmbrella() {
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.UNBREAKABLE_UMBRELLA));
  }

  @Test
  void mustHaveUmbrella() {
    String output = execute("ml");

    assertErrorState();
    assertThat(output, containsString("You need an Unbreakable Umbrella first."));
  }

  @Test
  void mustSpecifyState() {
    hasUmbrella();
    String output = execute("");

    assertErrorState();
    assertThat(output, containsString("What state do you want to fold your umbrella to?"));
  }

  @Test
  void mustSpecifyValidState() {
    hasUmbrella();
    String output = execute("the bourgeoisie");

    assertErrorState();
    assertThat(output, containsString("I don't understand what Umbrella form"));
  }

  @Test
  void canChooseBrokenState() {
    hasUmbrella();
    String output = execute("ml");

    assertContinueState();
    assertThat(output, containsString("Folding umbrella"));
  }

  @Test
  void canChoosForwardState() {
    hasUmbrella();
    String output = execute("dr");

    assertContinueState();
    assertThat(output, containsString("Folding umbrella"));
  }

  @Test
  void canChooseBucketState() {
    hasUmbrella();
    String output = execute("item");

    assertContinueState();
    assertThat(output, containsString("Folding umbrella"));
  }

  @Test
  void canChoosePitchforkState() {
    hasUmbrella();
    String output = execute("weapon");

    assertContinueState();
    assertThat(output, containsString("Folding umbrella"));
  }

  @Test
  void canChooseTwirlState() {
    hasUmbrella();
    String output = execute("spell");

    assertContinueState();
    assertThat(output, containsString("Folding umbrella"));
  }

  @Test
  void canChooseCocoonState() {
    hasUmbrella();
    String output = execute("nc");

    assertContinueState();
    assertThat(output, containsString("Folding umbrella"));
  }
}
