package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.*;
import static internal.helpers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class GenericRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("GenericRequestTest");
    Preferences.reset("GenericRequestTest");
  }

  @Test
  public void hallowienerVolcoinoNotPickedUpByLuckyGoldRing() {
    assertEquals("", Preferences.getString("lastEncounter"));
    equip(EquipmentManager.ACCESSORY1, "lucky gold ring");
    assertEquals(false, Preferences.getBoolean("_luckyGoldRingVolcoino"));

    KoLAdventure.setLastAdventure("The Bubblin' Caldera");

    GenericRequest request = new GenericRequest("adventure.php?snarfblat=451");
    request.setHasResult(true);
    request.responseText = html("request/test_adventure_hallowiener_volcoino_lucky_gold_ring.html");

    request.processResponse();

    assertEquals("Lava Dogs", Preferences.getString("lastEncounter"));
    assertEquals(false, Preferences.getBoolean("_luckyGoldRingVolcoino"));
  }

  @Test
  public void seeingEmptySpookyPuttyMonsterSetsProperty() {
    Preferences.setString("spookyPuttyMonster", "zmobie");

    var req = new GenericRequest("desc_item.php?whichitem=324375100");
    req.responseText = html("request/test_desc_item_spooky_putty_monster_empty.html");
    req.processResponse();

    assertThat("spookyPuttyMonster", isSetTo(""));
  }

  @ParameterizedTest
  @ValueSource(strings = {"beast", "elf"})
  public void learnLocketPhylumFromLocketDescription(String phylum) {
    var req = new GenericRequest("desc_item.php?whichitem=634036450");
    req.responseText = html("request/test_desc_item_combat_lovers_locket_" + phylum + ".html");
    req.processResponse();

    assertThat("locketPhylum", isSetTo(phylum));
  }

  @ParameterizedTest
  @ValueSource(ints = {100, 0})
  public void parseDesignerSweatpants(int expectedSweat) {
    var req = new GenericRequest("desc_item.php?whichitem=800334855");
    req.responseText =
        html("request/test_desc_item_designer_sweatpants_" + expectedSweat + "_sweat.html");
    req.processResponse();

    assertThat("sweat", isSetTo(expectedSweat));
  }
}
