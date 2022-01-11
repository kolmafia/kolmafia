package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GenericRequestTest {
  @BeforeEach
  public void beforeEach() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("GenericRequestTest");
  }

  @Test
  public void hallowienerVolcoinoNotPickedUpByLuckyGoldRing() throws IOException {
    assertEquals("", Preferences.getString("lastEncounter"));
    equip(EquipmentManager.ACCESSORY1, "lucky gold ring");
    assertEquals(false, Preferences.getBoolean("_luckyGoldRingVolcoino"));

    KoLAdventure.setLastAdventure("The Bubblin' Caldera");

    GenericRequest request = new GenericRequest("adventure.php?snarfblat=451");
    request.setHasResult(true);
    request.responseText =
        Files.readString(
            Paths.get("request/test_adventure_hallowiener_volcoino_lucky_gold_ring.html"));

    request.processResponse();

    assertEquals("Lava Dogs", Preferences.getString("lastEncounter"));
    assertEquals(false, Preferences.getBoolean("_luckyGoldRingVolcoino"));
  }
}
