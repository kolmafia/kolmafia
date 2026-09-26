package net.sourceforge.kolmafia.swingui.panel;

import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSkill;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.Arrays;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ConcoctionType;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class UseItemEnqueuePanelTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("useItemEnqueue");
    Preferences.reset("useItemEnqueue");

    // Now that we have a username set, we can edit preferences and have per-user defaults.
    // But first, make sure we don't persist anything.
    Preferences.setBoolean("saveSettingsOnSet", false);
  }

  @Test
  public void universalSeasoningEnabledWhenAvailable() {
    var cleanups =
        new Cleanups(
            withProperty("universalSeasoningCost", 1000),
            withProperty("_universalSeasoningsUsed", 0),
            withItem(ItemPool.UNIVERSAL_SEASONING));

    try (cleanups) {
      var panel = new UseItemEnqueuePanel(ConcoctionType.FOOD, null);
      var buttonSearch =
          Arrays.stream(panel.buttons)
              .filter(b -> b.getText().equals("universal seasoning"))
              .findFirst();
      assertTrue(buttonSearch.isPresent());
      var button = buttonSearch.get();
      assertTrue(button.isEnabled());
    }
  }

  @Test
  public void universalSeasoningDisabledWhenUnavailable() {
    var cleanups =
        new Cleanups(
            withProperty("universalSeasoningCost", 1000),
            withProperty("_universalSeasoningsUsed", 0));

    try (cleanups) {
      var panel = new UseItemEnqueuePanel(ConcoctionType.FOOD, null);
      var buttonSearch =
          Arrays.stream(panel.buttons)
              .filter(b -> b.getText().equals("universal seasoning"))
              .findFirst();
      assertTrue(buttonSearch.isPresent());
      var button = buttonSearch.get();
      assertFalse(button.isEnabled());
    }
  }

  @Test
  public void universalSeasoningDisabledWhenUsed() {
    var cleanups =
        new Cleanups(
            withProperty("universalSeasoningCost", 1000),
            withProperty("_universalSeasoningsUsed", 1),
            withItem(ItemPool.UNIVERSAL_SEASONING));

    try (cleanups) {
      var panel = new UseItemEnqueuePanel(ConcoctionType.FOOD, null);
      var buttonSearch =
          Arrays.stream(panel.buttons)
              .filter(b -> b.getText().equals("universal seasoning"))
              .findFirst();
      assertTrue(buttonSearch.isPresent());
      var button = buttonSearch.get();
      assertFalse(button.isEnabled());
    }
  }

  @Test
  public void odeToBoozeDisabledWithNoSkill() {
    var panel = new UseItemEnqueuePanel(ConcoctionType.BOOZE, null);
    var buttonSearch =
        Arrays.stream(panel.buttons).filter(b -> b.getText().equals("cast ode")).findFirst();
    assertTrue(buttonSearch.isPresent());
    var button = buttonSearch.get();
    var enabled = button.isEnabled();
    assertFalse(enabled);
    assertThat(button.getToolTipText(), containsString("You do not know The Ode to Booze"));
  }

  @Test
  public void odeToBoozeEnabledWithSkill() {
    var cleanups = withSkill(SkillPool.ODE_TO_BOOZE);

    try (cleanups) {
      var panel = new UseItemEnqueuePanel(ConcoctionType.BOOZE, null);
      var buttonSearch =
          Arrays.stream(panel.buttons).filter(b -> b.getText().equals("cast ode")).findFirst();
      assertTrue(buttonSearch.isPresent());
      var button = buttonSearch.get();
      assertTrue(button.isEnabled());
    }
  }

  @Test
  public void odeToBoozeDisabledWithNoRoom() {
    var cleanups =
        new Cleanups(
            withSkill(SkillPool.ODE_TO_BOOZE),
            withEffect(EffectPool.THE_BALLAD_OF_RICHIE_THINGFINDER),
            withEffect(EffectPool.BENETTONS_MEDLEY_OF_DIVERSITY),
            withEffect(EffectPool.ELRONS_EXPLOSIVE_ETUDE));

    try (cleanups) {
      var panel = new UseItemEnqueuePanel(ConcoctionType.BOOZE, null);
      var buttonSearch =
          Arrays.stream(panel.buttons).filter(b -> b.getText().equals("cast ode")).findFirst();
      assertTrue(buttonSearch.isPresent());
      var button = buttonSearch.get();
      assertFalse(button.isEnabled());
      assertThat(button.getToolTipText(), containsString("can't remember any more songs"));
    }
  }

  @Test
  public void odeToBoozeEnabledWithRoom() {
    var cleanups =
        new Cleanups(
            withSkill(SkillPool.ODE_TO_BOOZE),
            withEffect(EffectPool.BENETTONS_MEDLEY_OF_DIVERSITY),
            withEffect(EffectPool.ELRONS_EXPLOSIVE_ETUDE));

    try (cleanups) {
      var panel = new UseItemEnqueuePanel(ConcoctionType.BOOZE, null);
      var buttonSearch =
          Arrays.stream(panel.buttons).filter(b -> b.getText().equals("cast ode")).findFirst();
      assertTrue(buttonSearch.isPresent());
      var button = buttonSearch.get();
      assertTrue(button.isEnabled());
    }
  }

  @Test
  public void odeToBoozeEnabledWithNoRoomButEffect() {
    var cleanups =
        new Cleanups(
            withSkill(SkillPool.ODE_TO_BOOZE),
            withEffect(EffectPool.BENETTONS_MEDLEY_OF_DIVERSITY),
            withEffect(EffectPool.ELRONS_EXPLOSIVE_ETUDE),
            withEffect(EffectPool.ODE));

    try (cleanups) {
      var panel = new UseItemEnqueuePanel(ConcoctionType.BOOZE, null);
      var buttonSearch =
          Arrays.stream(panel.buttons).filter(b -> b.getText().equals("cast ode")).findFirst();
      assertTrue(buttonSearch.isPresent());
      var button = buttonSearch.get();
      assertTrue(button.isEnabled());
    }
  }
}
