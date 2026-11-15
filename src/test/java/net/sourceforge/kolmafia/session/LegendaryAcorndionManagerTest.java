package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class LegendaryAcorndionManagerTest {
  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset("LegendaryAcorndionManagerTest");
    Preferences.reset("LegendaryAcorndionManagerTest");
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = ';',
      value = {
        "acorndion_1;8;The Dark Elbow of the Woods:NONCOMBAT_5|The Haunted Bathroom:BRIDGE_PARTS|The Haunted Bedroom:BRIDGE_PARTS|The Haunted Billiards Room:BRIDGE_PARTS|The Haunted Gallery:BRIDGE_PARTS|The Haunted Library:BRIDGE_PARTS,BANISH|The Hippy Camp:NONCOMBAT_5",
        "acorndion_2;10;Domed City of Grimacia:PASSIVE_DMG_SPOOKY,DAMAGE_REDUCTION,INITIATIVE|Guano Junction:STUN,STAT_BUFF,SNIFF|Inside the Palindome:NONCOMBAT_5|The Copperhead Club:BRIDGE_PARTS|The Dark Elbow of the Woods:NONCOMBAT_5|The Haunted Ballroom:BRIDGE_PARTS|The Haunted Bathroom:BRIDGE_PARTS|The Haunted Bedroom:BRIDGE_PARTS|The Haunted Billiards Room:BRIDGE_PARTS|The Haunted Gallery:BRIDGE_PARTS|The Haunted Library:BRIDGE_PARTS,BANISH|The Hidden Bowling Alley:BANISH|The Hippy Camp:NONCOMBAT_5",
      })
  void canParseAcorndionDesc(String testFile, int acornsUsed, String acornsPlanted) {
    var cleanups =
        new Cleanups(
            withProperty("_legendaryAcornsUsed"),
            withProperty("legendaryAcornsPlanted"),
            withNextResponse(200, html("request/test_desc_item_" + testFile + ".html")));

    try (cleanups) {
      DebugDatabase.itemDescriptionText(ItemPool.LEGENDARY_ACORNDION, true);
      assertThat("_legendaryAcornsUsed", isSetTo(acornsUsed));
      assertThat("legendaryAcornsPlanted", isSetTo(acornsPlanted));
    }
  }
}
