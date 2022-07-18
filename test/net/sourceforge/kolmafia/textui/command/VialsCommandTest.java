package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VialsCommandTest extends AbstractCommandTestBase {
  public VialsCommandTest() {
    this.command = "vials";
  }

  @BeforeEach
  public void setup() {
    KoLCharacter.reset("vials");
    Preferences.reset("vials");
  }

  @Test
  public void identifiesKnownPotions() {
    Cleanups cleanups = setProperty("lastSlimeVial3885", "strong");
    try (cleanups) {
      String output = execute("");
      assertThat(output, containsString("red: strong"));
    }
  }

  @Test
  public void countsPotionsInInventory() {
    Cleanups cleanups = addItem("vial of red slime");
    try (cleanups) {
      String output = execute("");
      assertThat(output, containsString("red:  (have 1)"));
    }
  }

  @Test
  public void countsCreatablePotions() {
    Cleanups cleanups =
        new Cleanups(
            addItem("vial of red slime"),
            addItem("vial of yellow slime"),
            addSkill("Advanced Saucecrafting"),
            hasRange());
    try (cleanups) {
      String output = execute("");
      assertThat(output, containsString("orange:  (have 0, can make 1)"));
    }
  }
}
