package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class SpacegateManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("SpacegateManager");
    Preferences.reset("SpacegateManager");
    KoLConstants.inventory.clear();
  }

  @Test
  void allHazardsHaveValidItems() {
    for (SpacegateManager.Hazard hazard : SpacegateManager.HAZARDS) {
      assertTrue(ItemDatabase.contains(hazard.gear()));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"place.php?whichplace=spacegate", "adventure.php?snarfblat=494"})
  void justBeingInSpacegateWithoutPermanentAccessMeansDaypass() {
    var cleanups =
        new Cleanups(
            withProperty("spacegateAlways", false), withProperty("_spacegateToday", false));
    try (cleanups) {
      var request = new GenericRequest("place.php?whichplace=spacegate");
      request.responseText = "anything";
      QuestManager.handleQuestChange(request);
      assertThat("_spacegateToday", isSetTo(true));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"place.php?whichplace=spacegate", "adventure.php?snarfblat=494"})
  void justBeingInSpacegateWithPermanentAccessDoesNotMeanDaypass() {
    var cleanups =
        new Cleanups(withProperty("spacegateAlways", true), withProperty("_spacegateToday", false));
    try (cleanups) {
      var request = new GenericRequest("place.php?whichplace=spacegate");
      request.responseText = "anything";
      QuestManager.handleQuestChange(request);

      assertThat("_spacegateToday", isSetTo(false));
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"choice.php?forceoption=0", "place.php?whichplace=spacegate&action=sg_Terminal"})
  void canParseSpacegateTerminal(String url) {
    var cleanups =
        new Cleanups(
            withProperty("_spacegateAnimalLife"),
            withProperty("_spacegateCoordinates"),
            withProperty("_spacegateGear"),
            withProperty("_spacegateHazards"),
            withProperty("_spacegateIntelligentLife"),
            withProperty("_spacegateMurderbot"),
            withProperty("_spacegatePlanetIndex"),
            withProperty("_spacegatePlanetName"),
            withProperty("_spacegatePlantLife"),
            withProperty("_spacegateRuins"),
            withProperty("_spacegateSpant"),
            withProperty("_spacegateTurnsLeft"));
    try (cleanups) {
      var request = new GenericRequest(url);
      request.responseText = html("request/test_spacegate_terminal_earddyk.html");
      QuestManager.handleQuestChange(request);
      assertThat("_spacegateCoordinates", isSetTo("EARDDYK"));
      assertThat("_spacegatePlanetIndex", isSetTo(4));
      assertThat("_spacegatePlanetName", isSetTo("Diverticulus Isaacson IX"));
      assertThat("_spacegateHazards", isSetTo("high winds"));
      assertThat("_spacegateGear", isSetTo("high-friction boots"));
      assertThat("_spacegateAnimalLife", isSetTo("primitive (hostile)"));
      assertThat("_spacegatePlantLife", isSetTo("primitive"));
      assertThat("_spacegateIntelligentLife", isSetTo("none detected"));
      assertThat("_spacegateMurderbot", isSetTo(false));
      assertThat("_spacegateRuins", isSetTo(false));
      assertThat("_spacegateSpant", isSetTo(false));
      assertThat("_spacegateTurnsLeft", isSetTo(20));
    }
  }

  @Test
  void canActivateSpacegateAndAcquireGear() {
    var builder = new FakeHttpClientBuilder();
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withProperty("_spacegateAnimalLife"),
            withProperty("_spacegateCoordinates"),
            withProperty("_spacegateGear"),
            withProperty("_spacegateHazards"),
            withProperty("_spacegateIntelligentLife"),
            withProperty("_spacegateMurderbot"),
            withProperty("_spacegatePlanetIndex"),
            withProperty("_spacegatePlanetName"),
            withProperty("_spacegatePlantLife"),
            withProperty("_spacegateRuins"),
            withProperty("_spacegateSpant"),
            withProperty("_spacegateTurnsLeft"));
    try (cleanups) {
      // Approach the terminal and prepare to activate it
      var request = new GenericRequest("place.php?whichplace=spacegate&action=sg_Terminal");
      builder.client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
      builder.client.addResponse(200, html("request/test_spacegate_terminal.html"));
      request.run();

      // Select a random planet
      request = new GenericRequest("choice.php?whichchoice=1235&pwd&option=3");
      builder.client.addResponse(200, html("request/test_spacegate_activate.html"));
      builder.client.addResponse(200, ""); // api.php
      request.run();

      assertThat("_spacegateCoordinates", isSetTo("ECWSIJJ"));
      assertThat("_spacegatePlanetIndex", isSetTo(4));
      assertThat("_spacegatePlanetName", isSetTo("Beta Fritus IX"));
      assertThat("_spacegateHazards", isSetTo("irradiated"));
      assertThat("_spacegateGear", isSetTo("rad cloak"));
      assertThat("_spacegateAnimalLife", isSetTo("primitive"));
      assertThat("_spacegatePlantLife", isSetTo("none detected"));
      assertThat("_spacegateIntelligentLife", isSetTo("none detected"));
      assertThat("_spacegateMurderbot", isSetTo(true));
      assertThat("_spacegateRuins", isSetTo(false));
      assertThat("_spacegateSpant", isSetTo(true));
      assertThat("_spacegateTurnsLeft", isSetTo(20));

      // Attempt to adventure.
      assertFalse(InventoryManager.hasItem(ItemPool.RAD_CLOAK));
      request = new GenericRequest("adventure.php?snarfblat=494");
      builder.client.addResponse(200, html("request/test_spacegate_hazards_1.html"));
      builder.client.addResponse(200, ""); // api.php
      request.run();
      assertTrue(InventoryManager.hasItem(ItemPool.RAD_CLOAK));
    }
  }

  @Test
  void canActivatePortableSpacegateAndAcquireGear() {
    var builder = new FakeHttpClientBuilder();
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withItem(ItemPool.PORTABLE_SPACEGATE),
            withProperty("_spacegateGear"),
            withProperty("_spacegateHazards"),
            withProperty("_spacegateTurnsLeft"));
    try (cleanups) {
      // Use your portable spacegate
      var request = new GenericRequest("inv_use.php?pwd&which=3&whichitem=9465&ajax=1");
      builder.client.addResponse(200, html("request/test_portable_spacegate_activate.html"));
      builder.client.addResponse(200, ""); // api.php
      request.run();
      assertFalse(InventoryManager.hasItem(ItemPool.PORTABLE_SPACEGATE));
      assertTrue(InventoryManager.hasItem(ItemPool.OPEN_PORTABLE_SPACEGATE));
      assertThat("_spacegateTurnsLeft", isSetTo(20));

      // It gave us a random planet. Look at the gate.
      request = new GenericRequest("place.php?whichplace=spacegate_portable");
      builder.client.addResponse(200, html("request/test_portable_spacegate.html"));
      request.run();

      // It didn't tell us a thing about it. Dangerous!
      // Attempt to adventure.
      request = new GenericRequest("adventure.php?snarfblat=494");
      builder.client.addResponse(200, html("request/test_spacegate_hazards_2.html"));
      request.run();
      assertThat("_spacegateHazards", isSetTo("high gravity"));
      assertThat("_spacegateGear", isSetTo("exo-servo leg braces"));
      assertTrue(InventoryManager.hasItem(ItemPool.EXO_SERVO_LEG_BRACES));
    }
  }

  @Nested
  class Prediction {
    @ParameterizedTest
    @CsvSource({
      "AAAAAAA,toxic atmosphere,,none,friendly anomalous,none,,0,none,none,none,0,3",
      "EZXXNYT,high gravity,,hostile simple,hostile simple,hostile,primitive alien totem,0,drones and soldiers,drones,procrastinator,2,1",
      "GRDCPRN,high winds,scary noises,friendly simple,none,none,,0,none,drones,space pirates,3,3",
      "LHRSHZO,toxic atmosphere|irradiated,scary noises,none,friendly simple,hostile,primitive alien totem,0,none,drones and soldiers,none,0,2",
      "SCEQTCC,high gravity|magnetic storms,hot solar flares|nasty gasses,friendly simple,hostile complex,friendly,primitive alien medicine,27100,none,none,space baby,1,2",
      "TNGVKPY,toxic atmosphere|high gravity|irradiated,hot solar flares|lewd rock formations,hostile simple,hostile complex,friendly,primitive alien mask,110000,artifact,none,none,0,2",
    })
    void predictsPlanetTraits(
        String coords,
        String envHazardsStr,
        String elemHazardsStr,
        String plants,
        String animals,
        String aliens,
        String alienItem,
        int alienItemPrice,
        String spants,
        String murderbots,
        String quest,
        int questStage,
        int rocksLevel) {
      SpacegateManager.SpacegatePlanet planet = SpacegateManager.predictPlanet(coords);

      String[] envHazards = envHazardsStr == null ? new String[0] : envHazardsStr.split("\\|");
      assertThat(planet.envHazards().size(), is(envHazards.length));
      assertThat(planet.envHazards(), contains(envHazards));

      String[] elemHazards = elemHazardsStr == null ? new String[0] : elemHazardsStr.split("\\|");
      assertThat(planet.elemHazards().size(), is(elemHazards.length));
      if (elemHazards.length > 0) {
        assertThat(planet.elemHazards(), contains(elemHazards));
      }

      assertThat(planet.plants(), is(plants));
      assertThat(planet.animals(), is(animals));
      assertThat(planet.aliens(), is(aliens));
      if (alienItem == null) {
        assertThat(planet.alienItem(), nullValue());
      } else {
        assertThat(planet.alienItem().getName(), is(alienItem));
      }
      assertThat(planet.alienItemPrice(), is(alienItemPrice));
      assertThat(planet.spants(), is(spants));
      assertThat(planet.murderbots(), is(murderbots));
      assertThat(planet.quest(), is(quest));
      assertThat(planet.questStage(), is(questStage));
      assertThat(planet.rocksLevel(), is(rocksLevel));
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
          "CADENCE|Tau Reaganus XI|The sky is pink fading into red near the horizon.|The system's twin orange suns loom above you.|A single massive moon dominates the dome of the sky.",
          "NAKGNIC|Hippidipsus Wallace V|The sky is golden fading into pink near the horizon.|The landscape is lit by a pale blue sun.|Several craggy moons pepper the planet's ecliptic plane.",
          "PRICKLY|Kappa Harris VIII|The planet's sky is a dim yellow, punctuated by mottled pink and lilac-colored clouds.|The landscape is lit by a medium-sized red sun.|There are no moons visible.",
          "TIMEWAR|Xi North VII|The sky is a hazy blue, dominated by a system of grey rings.|A bright yellow, almost Sol-like sun hangs overhead.|Three oblong moons hang in the sky.",
        })
    void predictsPlanetCosmetics(
        String coords, String name, String desc1, String desc2, String desc3) {
      SpacegateManager.SpacegatePlanet planet = SpacegateManager.predictPlanet(coords);

      assertThat(planet.name(), is(name));
      assertThat(planet.description().size(), is(3));
      assertThat(planet.description().get(0), is(desc1));
      assertThat(planet.description().get(1), is(desc2));
      assertThat(planet.description().get(2), is(desc3));
    }
  }
}
