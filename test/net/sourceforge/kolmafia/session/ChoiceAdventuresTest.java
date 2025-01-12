package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ChoiceAdventuresTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ChoiceAdventures");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("ChoiceAdventures");
  }

  @Nested
  class GreatOverlookLodge {
    private static final int GREAT_OVERLOOK_LODGE = 606;

    @Test
    void itemDropTestWorks() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, "Radio KoL Maracas"),
              withEffect(EffectPool.THERES_NO_N_IN_LOVE));

      try (cleanups) {
        var options = ChoiceAdventures.dynamicChoiceOptions(GREAT_OVERLOOK_LODGE);
        assert options != null;
        assertThat(options[1].getName(), is("need +50% item drop, have 115%"));
      }
    }

    @Test
    void itemDropTestDoesntConsiderItemFairy() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 400),
              withEquipped(Slot.ACCESSORY1, "Radio KoL Maracas"));

      try (cleanups) {
        var options = ChoiceAdventures.dynamicChoiceOptions(GREAT_OVERLOOK_LODGE);
        assert options != null;
        assertThat(options[1].getName(), is("need +50% item drop, have 15%"));
      }
    }

    @Test
    void itemDropTestDoesntConsiderFoodFairy() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.COOKBOOKBAT, 400),
              withEquipped(Slot.ACCESSORY1, "Radio KoL Maracas"));

      try (cleanups) {
        var options = ChoiceAdventures.dynamicChoiceOptions(GREAT_OVERLOOK_LODGE);
        assert options != null;
        assertThat(options[1].getName(), is("need +50% item drop, have 15%"));
      }
    }
  }

  @Nested
  class CyberRealm {
    private static final int CYBER_ZONE1_HALFWAY = 1545;
    private static final int CYBER_ZONE2_HALFWAY = 1547;
    private static final int CYBER_ZONE3_HALFWAY = 1549;

    public void checkChoiceSpoilers(int level, String defense, String element) {
      int bits = 8 << (level - 1);
      String property = "_cyberZone" + level + "Defense";
      int choice =
          switch (level) {
            case 1 -> CYBER_ZONE1_HALFWAY;
            case 2 -> CYBER_ZONE2_HALFWAY;
            case 3 -> CYBER_ZONE3_HALFWAY;
            default -> 0;
          };
      var cleanups = new Cleanups(withProperty(property, defense));
      try (cleanups) {
        var options = ChoiceAdventures.dynamicChoiceOptions(choice);
        assertNotNull(options);
        assertEquals(options.length, 2);
        String expected = "Get 0 (" + bits + ") and suffer " + element + " damage";
        assertThat(options[0].getName(), is(expected));
        assertThat(options[1].getName(), is("no reward, no damage"));
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    public void canDynamicallyGenerateChoiceSpoilers(int level) {
      checkChoiceSpoilers(level, "firewall", "hot");
      checkChoiceSpoilers(level, "ICE barrier", "cold");
      checkChoiceSpoilers(level, "corruption quarantine", "stench");
      checkChoiceSpoilers(level, "parental controls", "sleaze");
      checkChoiceSpoilers(level, "null container", "spooky");
      checkChoiceSpoilers(level, "", "elemental");
    }
  }
}
