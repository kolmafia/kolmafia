package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

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
    try (var _ = withProperty("lastBangPotion821", "confusion")) {
      String output = execute("");
      assertThat(output, containsString("bubbly: confusion"));
    }
  }

  @Test
  public void countsPotionsInInventory() {
    try (var _ = withItem("bubbly potion")) {
      String output = execute("");
      assertThat(output, containsString("bubbly:  (have 1)"));
    }
  }

  @Test
  public void countsPotionsInCloset() {
    try (var _ = withItemInCloset("bubbly potion")) {
      String output = execute("");
      assertThat(output, containsString("bubbly:  (have 0, 1 in closet)"));
    }
  }
}
