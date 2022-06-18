package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BangCommandTest extends AbstractCommandTestBase {
  public BangCommandTest() {
    this.command = "bang";
  }

  @BeforeEach
  public void setup() {
    KoLCharacter.reset("bang");
    Preferences.reset("bang");
  }

  @Test
  public void identifiesKnownPotions() {
    Cleanups cleanups = setProperty("lastBangPotion821", "confusion");
    try (cleanups) {
      String output = execute("");
      assertThat(output, containsString("bubbly: confusion"));
    }
  }

  @Test
  public void countsPotionsInInventory() {
    Cleanups cleanups = addItem("bubbly potion");
    try (cleanups) {
      String output = execute("");
      assertThat(output, containsString("bubbly:  (have 1)"));
    }
  }

  @Test
  public void countsPotionsInCloset() {
    Cleanups cleanups = addItemToCloset("bubbly potion");
    try (cleanups) {
      String output = execute("");
      assertThat(output, containsString("bubbly:  (have 0, 1 in closet)"));
    }
  }
}
