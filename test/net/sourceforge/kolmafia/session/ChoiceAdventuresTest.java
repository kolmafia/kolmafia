package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.ChoiceAdventures.ShadowTheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
  class ShadowLabyrinth {
    @Test
    void canDetectHotAdjective() {
      var text = "Opt for the white-hot cleft";
      ShadowTheme theme = ChoiceAdventures.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = ChoiceAdventures.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.FIRE);
      assertEquals("90-100 Muscle substats", spoiler.toString());
    }

    @Test
    void canDetectColdAdjective() {
      var text = "Opt for the iced-over passage";
      ShadowTheme theme = ChoiceAdventures.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = ChoiceAdventures.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.COLD);
      assertEquals("30 Shadow's Chill: Maximum MP +300%", spoiler.toString());
    }

    void canDetectWaterAdjective() {
      var text = "Leap into the sodden hole";
      ShadowTheme theme = ChoiceAdventures.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = ChoiceAdventures.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.WATER);
      assertEquals("90-100 Moxie substats", spoiler.toString());
    }

    @Test
    void canDetectMathAdjective() {
      var text = "Try to reach the irrational portal";
      ShadowTheme theme = ChoiceAdventures.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = ChoiceAdventures.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.MATH);
      assertEquals("90-100 Mysticality substats", spoiler.toString());
    }

    @Test
    void canDetectTimeAdjective() {
      var text = "Try to reach the old opening";
      ShadowTheme theme = ChoiceAdventures.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = ChoiceAdventures.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.TIME);
      assertEquals("+3 turns to 3 random effects", spoiler.toString());
    }

    @Test
    void canDetectBloodAdjective() {
      var text = "Walk to the vein-shot lane";
      ShadowTheme theme = ChoiceAdventures.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = ChoiceAdventures.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.BLOOD);
      assertEquals("30 Shadow's Heart: Maximum HP +300%", spoiler.toString());
    }

    @Test
    void canDetectGhostAdjective() {
      var text = "Try to reach the nearly invisible hole";
      ShadowTheme theme = ChoiceAdventures.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = ChoiceAdventures.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.GHOST);
      assertEquals(
          "30 Shadow's Thickness: Superhuman (+5) Spooky, Hot, Sleaze resistance",
          spoiler.toString());
    }

    @ParameterizedTest
    @CsvSource({
      "Opt for the white-hot cleft, shadow lighter",
      "Opt for the iced-over passage, shadow snowflake",
      "Leap into the sodden hole, shadow bucket",
      "Try to reach the irrational portal, shadow heptahedron",
      "Walk to the vein-shot lane, shadow heart",
      "Try to reach the nearly invisible hole, shadow wave"
    })
    public void checkShadowArtifacts(String text, String artifact) {
      var cleanups =
          new Cleanups(
              withProperty("questRufus", "started"),
              withProperty("rufusQuestType", "artifact"),
              withProperty("rufusQuestTarget", artifact));
      try (cleanups) {
        ChoiceOption spoiler = ChoiceAdventures.shadowLabyrinthSpoiler(text);
        assertEquals(artifact, spoiler.toString());
      }
    }

    @Test
    void canParseLabyrinthOfShadows() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups = new Cleanups(withHttpClientBuilder(builder));
      try (cleanups) {
        String html = html("request/test_visit_labyrinth_of_shadows.html");
        client.addResponse(200, html);
        var request = new GenericRequest("choice.php?forceoption=0");
        request.run();

        var spoilers = ChoiceAdventures.choiceSpoilers(1499, new StringBuffer(html));
        var options = spoilers.getOptions();
        assertEquals("Randomize themes", options[0].toString());
        assertEquals("90-100 Muscle substats", options[1].toString());
        assertEquals("+3 turns to 3 random effects", options[2].toString());
        assertEquals("30 Shadow's Heart: Maximum HP +300%", options[3].toString());
        assertEquals("Randomize themes", options[4].toString());
        assertEquals("Leave with nothing", options[5].toString());
      }
    }
  }
}
