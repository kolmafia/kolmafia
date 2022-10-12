package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class SaberCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("testUser");
    Preferences.reset("testUser");
  }

  public SaberCommandTest() {
    this.command = "saber";
  }

  @Test
  void mustHaveSaber() {
    String output = execute("ml");

    assertErrorState();
    assertThat(output, containsString("You need a Fourth of May Cosplay Saber"));
  }

  @Test
  void mustNotHaveUpgraded() {
    var cleanups = new Cleanups(withItem(ItemPool.FOURTH_SABER), withProperty("_saberMod", 1));

    try (cleanups) {
      String output = execute("mp");

      assertErrorState();
      assertThat(output, containsString("already upgraded"));
    }
  }

  @Test
  void mustSpecifyUpgrade() {
    var cleanups = withItem(ItemPool.FOURTH_SABER);

    try (cleanups) {
      String output = execute("");

      assertErrorState();
      assertThat(output, containsString("Which upgrade"));
    }
  }

  @Test
  void mustSpecifyValidUpgrade() {
    var cleanups = withItem(ItemPool.FOURTH_SABER);

    try (cleanups) {
      String output = execute("dog");

      assertErrorState();
      assertThat(output, containsString("I don't understand what upgrade"));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"ml", "mp", "resistance", "familiar"})
  void canChooseUpgrades(String upgrade) {
    var cleanups = withItem(ItemPool.FOURTH_SABER);

    try (cleanups) {
      String output = execute(upgrade);

      assertContinueState();
      assertThat(output, containsString("Upgrading saber"));
    }
  }
}
