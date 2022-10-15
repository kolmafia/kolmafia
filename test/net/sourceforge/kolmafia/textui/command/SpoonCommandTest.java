package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SpoonCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("testUser");
    Preferences.reset("testUser");
  }

  public SpoonCommandTest() {
    this.command = "spoon";
  }

  @Test
  void mustHaveSpoon() {
    String output = execute("marmot");

    assertErrorState();
    assertThat(output, containsString("You need a hewn moon-rune spoon"));
  }

  @Test
  void mustNotHaveTuned() {
    var cleanups =
        new Cleanups(withItem(ItemPool.HEWN_MOON_RUNE_SPOON), withProperty("moonTuned", true));

    try (cleanups) {
      String output = execute("marmot");

      assertErrorState();
      assertThat(output, containsString("already tuned the moon"));
    }
  }

  @Test
  void mustSpecifySign() {
    var cleanups = withItem(ItemPool.HEWN_MOON_RUNE_SPOON);

    try (cleanups) {
      String output = execute("");

      assertErrorState();
      assertThat(output, containsString("Which sign do you want to change to"));
    }
  }

  @Test
  void mustSpecifyValidSign() {
    var cleanups = withItem(ItemPool.HEWN_MOON_RUNE_SPOON);

    try (cleanups) {
      String output = execute("dog");

      assertErrorState();
      assertThat(output, containsString("I don't understand what sign"));
    }
  }

  @Test
  void mustNotSetToBadMoon() {
    var cleanups = withItem(ItemPool.HEWN_MOON_RUNE_SPOON);

    try (cleanups) {
      String output = execute("bad moon");

      assertErrorState();
      assertThat(output, containsString("choose to be born under a Bad Moon"));
    }
  }

  @Test
  void mustNotBeInBadMoon() {
    var cleanups =
        new Cleanups(withItem(ItemPool.HEWN_MOON_RUNE_SPOON), withSign(ZodiacSign.BAD_MOON));

    try (cleanups) {
      String output = execute("marmot");

      assertErrorState();
      assertThat(output, containsString("escape the Bad Moon"));
    }
  }

  @Test
  void mustChooseDifferentSign() {
    var cleanups =
        new Cleanups(withItem(ItemPool.HEWN_MOON_RUNE_SPOON), withSign(ZodiacSign.MARMOT));

    try (cleanups) {
      String output = execute("marmot");

      assertErrorState();
      assertThat(output, containsString("No need to change"));
    }
  }

  @Test
  void canChooseSign() {
    var cleanups =
        new Cleanups(withItem(ItemPool.HEWN_MOON_RUNE_SPOON), withSign(ZodiacSign.WALLABY));

    try (cleanups) {
      String output = execute("marmot");

      assertContinueState();
      assertThat(output, containsString("Tuning moon to Marmot"));
    }
  }
}
