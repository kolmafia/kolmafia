package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.equipment.Slot;
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
    try (var _ =
        new Cleanups(withItem(ItemPool.HEWN_MOON_RUNE_SPOON), withProperty("moonTuned", true))) {
      String output = execute("marmot");

      assertErrorState();
      assertThat(output, containsString("already tuned the moon"));
    }
  }

  @Test
  void mustSpecifySign() {
    try (var _ = withItem(ItemPool.HEWN_MOON_RUNE_SPOON)) {
      String output = execute("");

      assertErrorState();
      assertThat(output, containsString("Which sign do you want to change to"));
    }
  }

  @Test
  void mustSpecifyValidSign() {
    try (var _ = withItem(ItemPool.HEWN_MOON_RUNE_SPOON)) {
      String output = execute("dog");

      assertErrorState();
      assertThat(output, containsString("I don't understand what sign"));
    }
  }

  @Test
  void mustNotSetToBadMoon() {
    try (var _ = withItem(ItemPool.HEWN_MOON_RUNE_SPOON)) {
      String output = execute("bad moon");

      assertErrorState();
      assertThat(output, containsString("choose to be born under a Bad Moon"));
    }
  }

  @Test
  void mustNotBeInBadMoon() {
    try (var _ =
        new Cleanups(withItem(ItemPool.HEWN_MOON_RUNE_SPOON), withSign(ZodiacSign.BAD_MOON))) {
      String output = execute("marmot");

      assertErrorState();
      assertThat(output, containsString("escape the Bad Moon"));
    }
  }

  @Test
  void mustChooseDifferentSign() {
    try (var _ =
        new Cleanups(withItem(ItemPool.HEWN_MOON_RUNE_SPOON), withSign(ZodiacSign.MARMOT))) {
      String output = execute("marmot");

      assertErrorState();
      assertThat(output, containsString("No need to change"));
    }
  }

  @Test
  void canChooseSign() {
    try (var _ =
        new Cleanups(withItem(ItemPool.HEWN_MOON_RUNE_SPOON), withSign(ZodiacSign.WALLABY))) {
      String output = execute("marmot");

      assertContinueState();
      assertThat(output, containsString("Tuning moon to Marmot"));
    }
  }

  @Test
  void worksWithEquippedSpoon() {
    try (var _ = withEquipped(Slot.ACCESSORY1, ItemPool.HEWN_MOON_RUNE_SPOON)) {
      String output = execute("marmot");

      assertContinueState();
      assertThat(output, containsString("Tuning moon to Marmot"));
    }
  }

  @Test
  void worksWithReplicaSpoon() {
    try (var _ =
        new Cleanups(
            withPath(Path.LEGACY_OF_LOATHING),
            withEquipped(Slot.ACCESSORY1, ItemPool.REPLICA_HEWN_MOON_RUNE_SPOON))) {
      String output = execute("marmot");

      assertContinueState();
      assertThat(output, containsString("Tuning moon to Marmot"));
    }
  }
}
