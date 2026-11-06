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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class LegendaryAcorndionManagerTest {
  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset("LegendaryAcorndionManagerTest");
    Preferences.reset("LegendaryAcorndionManagerTest");
  }

  @Test
  void canHandleCombatSkill() {}

  @ParameterizedTest
  @CsvSource(
      delimiter = ';',
      value = {
        "desc_acorndion_1;8;Guano Junction:STAT_GAIN,SNIFF|The Bat Hole Entrance:FRUIT,RESTORE,STUN|The Briny Deeps:SEA_COWS,NONCOMBAT_10,BETTER_DIVER",
        "desc_acorndion_2;12;Guano Junction:STAT_GAIN,SNIFF,STAT_BUFF|The Bat Hole Entrance:FRUIT,RESTORE,STUN|The Briny Deeps:SEA_COWS,NONCOMBAT_10,BETTER_DIVER|The Overgrown Lot:ENTANGLE|The Skeleton Store:DAMAGE_REDUCTION,INITIATIVE",
        "desc_acorndion_3;12;The Briny Deeps:PASSIVE_DMG_PERCENT,SEA_FRUIT|The Dungeons of Doom:SNIFF|The Fungal Nethers:SNIFF|The Neverending Party:BANISH|The Overgrown Lot:PASSIVE_DMG_SLEAZE,WEAKEN,NONCOMBAT_5|The Skeleton Store:PASSIVE_DMG_SPOOKY,PASSIVE_DMG,BRIDGE_PARTS|The Sleazy Back Alley:MHP_DEBUFF,ITEM_DROPS",
      })
  void canParseAcorndionDesc(String testFile, int acornsUsed, String acornsPlanted) {
    var cleanups =
        new Cleanups(
            withProperty("_legendaryAcornsUsed"),
            withProperty("legendaryAcornsPlanted"),
            withNextResponse(200, html("request/test_request_" + testFile + ".html")));

    try (cleanups) {
      DebugDatabase.itemDescriptionText(ItemPool.LEGENDARY_ACORNDION, true);
      assertThat("_legendaryAcornsUsed", isSetTo(acornsUsed));
      assertThat("legendaryAcornsPlanted", isSetTo(acornsPlanted));
    }
  }
}
