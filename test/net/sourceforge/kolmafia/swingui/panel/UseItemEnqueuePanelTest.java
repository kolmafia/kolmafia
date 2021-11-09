package net.sourceforge.kolmafia.swingui.panel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class UseItemEnqueuePanelTest {
  @BeforeAll
  private static void injectPreferences() {
    KoLCharacter.reset("fakeUserName");

    // Now that we have a username set, we can edit preferences and have per-user defaults.
    // But first, make sure we don't persist anything.
    Preferences.setBoolean("saveSettingsOnSet", false);
  }

  @AfterAll
  private static void cleanupSession() {
    KoLCharacter.reset("");
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

    var panel = new UseItemEnqueuePanel(true, false, false, null);
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

    var panel = new UseItemEnqueuePanel(true, false, false, null);
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

    var panel = new UseItemEnqueuePanel(true, false, false, null);
    var buttonSearch =
        Arrays.stream(panel.buttons)
            .filter(b -> b.getText().equals("universal seasoning"))
            .findFirst();
    assertTrue(buttonSearch.isPresent());
    var button = buttonSearch.get();
    assertFalse(button.isEnabled());
  }
}
