package net.sourceforge.kolmafia.swingui.panel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase.ConcoctionType;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UseItemEnqueuePanelTest {

  @BeforeEach
  private void beforeEach() {
    KoLCharacter.reset(true);
    KoLCharacter.reset("fakeUserName");

    // Now that we have a username set, we can edit preferences and have per-user defaults.
    // But first, make sure we don't persist anything.
    Preferences.setBoolean("saveSettingsOnSet", false);
  }

  private void loadInventory(String jsonInventory) {
    try {
      InventoryManager.parseInventory(new JSONObject(jsonInventory));
    } catch (JSONException e) {
      fail("Inventory parsing failed.");
    }
  }

  @Test
  public void universalSeasoningEnabledWhenAvailable() {
    Preferences.setInteger("universalSeasoningCost", 1000);
    Preferences.setInteger("_universalSeasoningsUsed", 0);
    loadInventory("{\"10640\": \"1\"}");

    var panel = new UseItemEnqueuePanel(ConcoctionType.FOOD, null);
    var buttonSearch =
        Arrays.stream(panel.buttons)
            .filter(b -> b.getText().equals("universal seasoning"))
            .findFirst();
    assertTrue(buttonSearch.isPresent());
    var button = buttonSearch.get();
    assertTrue(button.isEnabled());
  }

  @Test
  public void universalSeasoningDisabledWhenUnavailable() {
    Preferences.setInteger("universalSeasoningCost", 1000);
    Preferences.setInteger("_universalSeasoningsUsed", 0);
    loadInventory("{\"10640\": \"0\"}");

    var panel = new UseItemEnqueuePanel(ConcoctionType.FOOD, null);
    var buttonSearch =
        Arrays.stream(panel.buttons)
            .filter(b -> b.getText().equals("universal seasoning"))
            .findFirst();
    assertTrue(buttonSearch.isPresent());
    var button = buttonSearch.get();
    assertFalse(button.isEnabled());
  }

  @Test
  public void universalSeasoningDisabledWhenUsed() {
    Preferences.setInteger("universalSeasoningCost", 1000);
    Preferences.setInteger("_universalSeasoningsUsed", 1);
    loadInventory("{\"10640\": \"1\"}");

    var panel = new UseItemEnqueuePanel(ConcoctionType.FOOD, null);
    var buttonSearch =
        Arrays.stream(panel.buttons)
            .filter(b -> b.getText().equals("universal seasoning"))
            .findFirst();
    assertTrue(buttonSearch.isPresent());
    var button = buttonSearch.get();
    assertFalse(button.isEnabled());
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
  }

  @Test
  public void odeToBoozeEnabledWithSkill() {
    KoLCharacter.addAvailableSkill(SkillPool.ODE_TO_BOOZE);

    var panel = new UseItemEnqueuePanel(ConcoctionType.BOOZE, null);
    var buttonSearch =
        Arrays.stream(panel.buttons).filter(b -> b.getText().equals("cast ode")).findFirst();
    assertTrue(buttonSearch.isPresent());
    var button = buttonSearch.get();
    assertTrue(button.isEnabled());
  }

  @Test
  public void odeToBoozeDisabledWithNoRoom() {
    KoLCharacter.addAvailableSkill(SkillPool.ODE_TO_BOOZE);

    KoLConstants.activeEffects.add(EffectPool.get(530)); // The Ballad of Richie Thingfinder
    KoLConstants.activeEffects.add(EffectPool.get(531)); // Benetton's Medley of Diversity
    KoLConstants.activeEffects.add(EffectPool.get(532)); // Elron's Explosive Etude

    var panel = new UseItemEnqueuePanel(ConcoctionType.BOOZE, null);
    var buttonSearch =
        Arrays.stream(panel.buttons).filter(b -> b.getText().equals("cast ode")).findFirst();
    assertTrue(buttonSearch.isPresent());
    var button = buttonSearch.get();
    assertFalse(button.isEnabled());
  }

  @Test
  public void odeToBoozeEnabledWithRoom() {
    KoLCharacter.addAvailableSkill(SkillPool.ODE_TO_BOOZE);

    KoLConstants.activeEffects.add(EffectPool.get(531)); // Benetton's Medley of Diversity
    KoLConstants.activeEffects.add(EffectPool.get(532)); // Elron's Explosive Etude

    var panel = new UseItemEnqueuePanel(ConcoctionType.BOOZE, null);
    var buttonSearch =
        Arrays.stream(panel.buttons).filter(b -> b.getText().equals("cast ode")).findFirst();
    assertTrue(buttonSearch.isPresent());
    var button = buttonSearch.get();
    assertTrue(button.isEnabled());
  }
}
