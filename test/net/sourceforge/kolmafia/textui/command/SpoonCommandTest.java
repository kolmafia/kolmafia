package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SpoonCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    KoLCharacter.reset("testUser");
    KoLCharacter.reset(true);
    Preferences.resetToDefault("moonTuned");

    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  public SpoonCommandTest() {
    this.command = "spoon";
  }

  static void hasSpoon() {
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.HEWN_MOON_RUNE_SPOON));
  }

  @Test
  void mustHaveSpoon() {
    String output = execute("marmot");

    assertErrorState();
    assertThat(output, containsString("You need a hewn moon-rune spoon"));
    assertEquals("None", KoLCharacter.getSign());
  }

  @Test
  void mustNotHaveTuned() {
    hasSpoon();
    Preferences.setBoolean("moonTuned", true);
    String output = execute("marmot");

    assertErrorState();
    assertThat(output, containsString("already tuned the moon"));
    assertEquals("None", KoLCharacter.getSign());
  }

  @Test
  void mustSpecifySign() {
    hasSpoon();
    String output = execute("");

    assertErrorState();
    assertThat(output, containsString("Which sign do you want to change to"));
    assertEquals("None", KoLCharacter.getSign());
  }

  @Test
  void mustSpecifyValidSign() {
    hasSpoon();
    String output = execute("dog");

    assertErrorState();
    assertThat(output, containsString("I don't understand what sign"));
    assertEquals("None", KoLCharacter.getSign());
  }

  @Test
  void mustNotSetToBadMoon() {
    hasSpoon();
    String output = execute("bad moon");

    assertErrorState();
    assertThat(output, containsString("choose to be born under a Bad Moon"));
    assertEquals("None", KoLCharacter.getSign());
  }

  @Test
  void mustNotBeInBadMoon() {
    hasSpoon();
    KoLCharacter.setSign("Bad Moon");
    String output = execute("marmot");

    assertErrorState();
    assertThat(output, containsString("escape the Bad Moon"));
    assertEquals("Bad Moon", KoLCharacter.getSign());
  }

  @Test
  void mustChooseDifferentSign() {
    hasSpoon();
    KoLCharacter.setSign("Marmot");
    String output = execute("marmot");

    assertErrorState();
    assertThat(output, containsString("No need to change"));
    assertEquals("Marmot", KoLCharacter.getSign());
  }

  @Test
  void canChooseSign() {
    hasSpoon();
    KoLCharacter.setSign("Wallaby");
    String output = execute("marmot");

    assertContinueState();
    assertThat(output, containsString("Tuning moon to Marmot"));
  }
}
