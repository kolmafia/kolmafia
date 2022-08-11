package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Coverage driven collection of tests for FightRequest. */
public class EquipmentManagerTest {

  @BeforeAll
  public static void beforeAll() {
    // Simulate logging out and back in again.
    GenericRequest.passwordHash = "";
    KoLCharacter.reset("");
    KoLCharacter.reset("equipment manager test");
    Preferences.saveSettingsToFile = false;
  }

  @AfterAll
  public static void afterAll() {
    Preferences.saveSettingsToFile = true;
  }

  private static final AdventureResult UNBREAKABLE_UMBRELLA =
      ItemPool.get(ItemPool.UNBREAKABLE_UMBRELLA);

  @ParameterizedTest
  @ValueSource(
      strings = {
        "broken",
        "forward-facing",
        "bucket style",
        "pitchfork style",
        "constantly twirling",
        "cocoon"
      })
  public void thatUnbreakableUmbrellaIsRecognized(String style) {
    var cleanups = new Cleanups(withEquipped(EquipmentManager.OFFHAND, "unbreakable umbrella"));

    try (cleanups) {
      Preferences.setString("umbrellaState", style);
      assertEquals("unbreakable umbrella (" + style + ")", UNBREAKABLE_UMBRELLA.getName());
      assertEquals(
          UNBREAKABLE_UMBRELLA.getItemId(), ItemDatabase.getItemId(UNBREAKABLE_UMBRELLA.getName()));
    }
  }

  @Test
  public void equippingDesignerSweatpantsGivesCombatSkills() {
    assertThat(KoLCharacter.hasSkill("Sweat Flick"), equalTo(false));

    var cleanup =
        new Cleanups(
            withEquipped(EquipmentManager.PANTS, "designer sweatpants"),
            withProperty("sweat", 100));

    try (cleanup) {
      assertThat(KoLCharacter.hasSkill("Sweat Flick"), equalTo(true));
    }
  }

  @Test
  public void unequippingDesignerSweatpantsRemovesCombatSkills() {
    var cleanup =
        new Cleanups(
            withEquipped(EquipmentManager.PANTS, "designer sweatpants"),
            withProperty("sweat", 100));

    try (cleanup) {
      assertThat(KoLCharacter.hasSkill("Sweat Flick"), equalTo(true));
      EquipmentManager.setEquipment(EquipmentManager.PANTS, EquipmentRequest.UNEQUIP);
      assertThat(KoLCharacter.hasSkill("Sweat Flick"), equalTo(false));
    }
  }
}
