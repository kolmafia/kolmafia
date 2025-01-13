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

    public int levelToZone(int level) {
      return switch (level) {
        case 1 -> CYBER_ZONE1_HALFWAY;
        case 2 -> CYBER_ZONE2_HALFWAY;
        case 3 -> CYBER_ZONE3_HALFWAY;
        default -> 0;
      };
    }

    public void checkChoiceSpoilers(int level, String property, String value, String element) {
      int bits = 8 << (level - 1);
      int choice = levelToZone(level);
      var cleanups = new Cleanups(withProperty(property, value));
      try (cleanups) {
        var options = ChoiceAdventures.dynamicChoiceOptions(choice);
        assertNotNull(options);
        assertEquals(options.length, 2);
        String expected = "Get 0 (" + bits + ") and suffer " + element + " damage";
        assertThat(options[0].getName(), is(expected));
        assertThat(options[1].getName(), is("no reward, no damage"));
      }
    }

    public void checkDefenseChoiceSpoilers(int level, String defense, String element) {
      String property = "_cyberZone" + level + "Defense";
      checkChoiceSpoilers(level, property, defense, element);
    }

    public void checkEncounterChoiceSpoilers(int level, String encounter, String element) {
      String property = "lastEncounter";
      checkChoiceSpoilers(level, property, encounter, element);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    public void canDynamicallyGenerateChoiceSpoilersFromDefense(int level) {
      checkDefenseChoiceSpoilers(level, "firewall", "hot");
      checkDefenseChoiceSpoilers(level, "ICE barrier", "cold");
      checkDefenseChoiceSpoilers(level, "corruption quarantine", "stench");
      checkDefenseChoiceSpoilers(level, "parental controls", "sleaze");
      checkDefenseChoiceSpoilers(level, "null container", "spooky");
      checkDefenseChoiceSpoilers(level, "", "elemental");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    public void canDynamicallyGenerateChoiceSpoilersFromEncounter(int level) {
      checkEncounterChoiceSpoilers(level, "A Breezy System", "cold");
      checkEncounterChoiceSpoilers(level, "Arsenic & Old Spice", "stench");
      checkEncounterChoiceSpoilers(level, "I Live, You Live...", "sleaze");
      checkEncounterChoiceSpoilers(level, "Grave Secrets", "spooky");
      checkEncounterChoiceSpoilers(level, "Unknown Encounter", "elemental");
    }
  }
}
