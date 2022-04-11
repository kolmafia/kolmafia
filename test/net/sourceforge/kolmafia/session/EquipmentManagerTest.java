package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.*;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Coverage driven collection of tests for FightRequest. */
public class EquipmentManagerTest {

  @BeforeAll
  private static void beforeAll() {
    // Simulate logging out and back in again.
    GenericRequest.passwordHash = "";
    KoLCharacter.reset("");
    KoLCharacter.reset("equipment manager test");
    Preferences.saveSettingsToFile = false;
  }

  @AfterAll
  private static void afterAll() {
    Preferences.saveSettingsToFile = true;
  }

  @Test
  public void thatUnbreakableUmbrellaIsRecognized() {
    AdventureResult unbrella = ItemPool.get(ItemPool.UNBREAKABLE_UMBRELLA, 1);
    assertEquals(EquipmentRequest.UNEQUIP, EquipmentManager.getEquipment(EquipmentManager.OFFHAND));
    EquipmentManager.setEquipment(EquipmentManager.OFFHAND, unbrella);
    assertEquals(unbrella, EquipmentManager.getEquipment(EquipmentManager.OFFHAND));

    Preferences.setString("umbrellaState", "broken");
    assertEquals("unbreakable umbrella (broken)", unbrella.getName());
    assertEquals(unbrella.getItemId(), ItemDatabase.getItemId(unbrella.getName()));

    Preferences.setString("umbrellaState", "forward-facing");
    assertEquals("unbreakable umbrella (forward-facing)", unbrella.getName());
    assertEquals(unbrella.getItemId(), ItemDatabase.getItemId(unbrella.getName()));

    Preferences.setString("umbrellaState", "bucket style");
    assertEquals("unbreakable umbrella (bucket style)", unbrella.getName());
    assertEquals(unbrella.getItemId(), ItemDatabase.getItemId(unbrella.getName()));

    Preferences.setString("umbrellaState", "pitchfork style");
    assertEquals("unbreakable umbrella (pitchfork style)", unbrella.getName());
    assertEquals(unbrella.getItemId(), ItemDatabase.getItemId(unbrella.getName()));

    Preferences.setString("umbrellaState", "constantly twirling");
    assertEquals("unbreakable umbrella (constantly twirling)", unbrella.getName());
    assertEquals(unbrella.getItemId(), ItemDatabase.getItemId(unbrella.getName()));

    Preferences.setString("umbrellaState", "cocoon");
    assertEquals("unbreakable umbrella (cocoon)", unbrella.getName());
    assertEquals(unbrella.getItemId(), ItemDatabase.getItemId(unbrella.getName()));
  }
}
