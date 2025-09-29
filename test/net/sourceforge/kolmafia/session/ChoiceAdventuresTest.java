package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
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

    public void checkMaxResistanceChoiceSpoilers(
        int level, String property, String value, String element) {
      int bits = element.equals("elemental") ? 0 : (8 << (level - 1));
      int damage = element.equals("elemental") ? (50 * level) : (10 * level + (level - 1));
      int choice = levelToZone(level);
      var cleanups =
          new Cleanups(
              withProperty(property, value),
              // Prismatic Resistance: +9
              withEffect("Synthesis: Hot"),
              withEffect("Synthesis: Cold"),
              withEffect("Synthesis: Pungent"),
              withEffect("Synthesis: Greasy"),
              withEffect("Synthesis: Scary"),
              // Prismatic Resistance: +3
              withEquipped(Slot.OFFHAND, "six-rainbow shield"));
      try (cleanups) {
        var options = ChoiceAdventures.dynamicChoiceOptions(choice);
        assertNotNull(options);
        assertEquals(options.length, 2);
        String expected = "Get 0 (" + bits + ") and suffer " + damage + " " + element + " damage";
        assertThat(options[0].getName(), is(expected));
        assertThat(options[1].getName(), is("no reward, no damage"));
      }
    }

    public void checkNoResistanceChoiceSpoilers(
        int level, String property, String value, String element) {
      int bits = 0;
      int damage = 50 * level;
      int choice = levelToZone(level);
      var cleanups = new Cleanups(withProperty(property, value));
      try (cleanups) {
        var options = ChoiceAdventures.dynamicChoiceOptions(choice);
        assertNotNull(options);
        assertEquals(options.length, 2);
        String expected = "Get 0 (" + bits + ") and suffer " + damage + " " + element + " damage";
        assertThat(options[0].getName(), is(expected));
        assertThat(options[1].getName(), is("no reward, no damage"));
      }
    }

    public void checkDefenseChoiceSpoilers(int level, String defense, String element) {
      String property = "_cyberZone" + level + "Defense";
      checkNoResistanceChoiceSpoilers(level, property, defense, element);
      checkMaxResistanceChoiceSpoilers(level, property, defense, element);
    }

    public void checkEncounterChoiceSpoilers(int level, String encounter, String element) {
      String property = "lastEncounter";
      checkNoResistanceChoiceSpoilers(level, property, encounter, element);
      checkMaxResistanceChoiceSpoilers(level, property, encounter, element);
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
      checkEncounterChoiceSpoilers(level, "A Funny Thing Happened...", "hot");
      checkEncounterChoiceSpoilers(level, "A Turboclocked System", "hot");
      checkEncounterChoiceSpoilers(level, "Boiling Chrome", "hot");
      checkEncounterChoiceSpoilers(level, "Cracklin' Node", "hot");
      checkEncounterChoiceSpoilers(level, "A Breezy System", "cold");
      checkEncounterChoiceSpoilers(level, "A Frozen Network", "cold");
      checkEncounterChoiceSpoilers(level, "A Severely Underclocked Network", "cold");
      checkEncounterChoiceSpoilers(level, "Ice Cream Antisocial", "cold");
      checkEncounterChoiceSpoilers(level, "A Terminal Disease", "stench");
      checkEncounterChoiceSpoilers(level, "Arsenic & Old Spice", "stench");
      checkEncounterChoiceSpoilers(level, "One Man's TRS-80", "stench");
      checkEncounterChoiceSpoilers(level, "People Have Weird Hobbies Sometimes", "stench");
      checkEncounterChoiceSpoilers(level, "$1.00,$1.00,$1.00", "sleaze");
      checkEncounterChoiceSpoilers(level, "I Live, You Live...", "sleaze");
      checkEncounterChoiceSpoilers(level, "pr0n Central", "sleaze");
      checkEncounterChoiceSpoilers(level, "The Piggy Bank", "sleaze");
      checkEncounterChoiceSpoilers(level, "A spooky encounter", "spooky");
      checkEncounterChoiceSpoilers(level, "Grave Secrets", "spooky");
      checkEncounterChoiceSpoilers(level, "The Fall of the Homepage of Usher", "spooky");
      checkEncounterChoiceSpoilers(level, "The Skeleton Dance", "spooky");
      checkEncounterChoiceSpoilers(level, "Unknown Encounter", "elemental");
    }
  }

  @Test
  public void decoratesMapMonstersChoice() {
    var original = html("request/test_choice_map_monsters.html");
    var buffer = new StringBuffer(original);

    ChoiceAdventures.decorateChoice(1435, buffer, true);

    var output = buffer.toString();
    output = KoLConstants.LINE_BREAK_PATTERN.matcher(output).replaceAll("");

    assertThat(
        output,
        containsString(
            "<input type=\"hidden\" name=\"heyscriptswhatsupwinkwink\" value=\"100\" /><input type=\"submit\" class=\"button\" value=\"Ninja Snowman (Mask)\" />"));
  }

  @Test
  public void decoratesPeridotChoice() {
    var original = html("request/test_choice_peridot.html");
    var buffer = new StringBuffer(original);

    ChoiceAdventures.decorateChoice(1557, buffer, true);

    var output = buffer.toString();
    output = KoLConstants.LINE_BREAK_PATTERN.matcher(output).replaceAll("");

    assertThat(
        output,
        containsString(
            "<input type=\"hidden\" name=\"bandersnatch\" value=\"100\" /><input type=\"submit\" class=\"button\" value=\"Ninja Snowman (Mask)\" />"));
  }

  @Test
  public void decoratesMimicDnaChoice() {
    var original = html("request/test_choice_mimic_dna_bank.html");
    var buffer = new StringBuffer(original);

    ChoiceAdventures.decorateChoice(1517, buffer, true);

    var output = buffer.toString();

    assertThat(
        output,
        containsString(
            "<option value=\"354\" disabled>Astronomer (obsolete) (90 samples required)</option>"));
    assertThat(output, containsString("<option value=\"1163\" >Baa'baa'bu'ran </option>"));
  }

  @Test
  void catalogCardSpoilers() {
    var cleanups =
        new Cleanups(withProperty("merkinCatalogChoices", "AF531.55:1:stats,AW393.55:2:clue"));

    try (cleanups) {
      var req = new GenericRequest("choice.php?whichchoice=" + 704);
      req.responseText = html("request/test_choice_catalog_0.html");

      ChoiceManager.visitChoice(req);

      var options = ChoiceAdventures.dynamicChoiceOptions(704);
      assert options != null;
      assertThat(options[0].getName(), is("stats"));
      assertThat(options[1].getName(), is("clue"));
      assertThat(options[2].getName(), is("unknown"));
    }
  }

  @Nested
  class MobiusRing {
    @Test
    void decoratesMobiusChoice() {
      var original = html("request/test_choice_mobius_0.html");
      var buffer = new StringBuffer(original);

      ChoiceAdventures.decorateChoice(1562, buffer, true);

      var output = buffer.toString();

      assertThat(
          output,
          containsString(
              "value=\"Draw a goatee on yourself\"><br><font size=-1>(30 turns of +5 stats per fight"));
    }

    @Test
    void disablesAbsentChoices() {
      var original = html("request/test_choice_mobius_1.html");
      var buffer = new StringBuffer(original);

      ChoiceAdventures.decorateChoice(1562, buffer, true);

      var output = buffer.toString();

      assertThat(
          output,
          containsString(
              "<input disabled class=\"button disabled\" type=submit value=\"Go back and take a 20-year-long nap\">"));
    }
  }
}
