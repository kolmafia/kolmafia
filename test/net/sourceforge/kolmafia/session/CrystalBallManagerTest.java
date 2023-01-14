package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withCurrentRun;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withLastLocation;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacter.Gender;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CrystalBallManagerTest {
  static final MonsterData NAILER = MonsterDatabase.findMonster("smut orc nailer");
  static final MonsterData SKELELTON = MonsterDatabase.findMonster("spiny skelelton");
  static final MonsterData SKELTEON = MonsterDatabase.findMonster("party skelteon");

  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("fakeUserName");
    Preferences.reset("fakeUsername");
    Preferences.setString(
        "crystalBallPredictions",
        "0:The Smut Orc Logging Camp:smut orc nailer|0:The Defiled Nook:party skelteon");
    CrystalBallManager.reset();
  }

  @Test
  public void crystalBallZoneTest() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.BADGER),
            withEquipped(EquipmentManager.FAMILIAR, "miniature crystal ball"));

    try (cleanups) {
      assertTrue(CrystalBallManager.isCrystalBallZone("The Smut Orc Logging Camp"));
      assertTrue(CrystalBallManager.isCrystalBallZone("The Defiled Nook"));
      assertFalse(CrystalBallManager.isCrystalBallZone("The Defiled Niche"));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "smut orc nailer, The Smut Orc Logging Camp, true",
    "party skelteon, The Defiled Nook, true",
    "spiny skelelton, The Defiled Niche, false"
  })
  public void canIdentifyPredictedMonster(String monsterName, String locationName, String result) {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.BADGER),
            withEquipped(EquipmentManager.FAMILIAR, "miniature crystal ball"));
    try (cleanups) {
      // From String
      assertEquals(
          Boolean.parseBoolean(result),
          CrystalBallManager.isCrystalBallMonster(monsterName, locationName));
      // Or from MonsterData
      var monster = MonsterDatabase.findMonster(monsterName);
      assertEquals(
          Boolean.parseBoolean(result),
          CrystalBallManager.isCrystalBallMonster(monster, locationName));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "Ninja Snowman (Chopsticks), Ninja Snowman (Hilt), true",
    "spider gremlin, spider gremlin (tool), true",
    "spider gremlin (tool), spider gremlin, true",
    "spider gremlin (tool), spider gremlin (tool), true",
    "spider gremlin (tool), batwinged gremlin (tool), false",
  })
  public void canIdentifyAmbiguousPredictedMonster(
      String predictionMonster, String monsterName, String result) {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.BADGER),
            withEquipped(EquipmentManager.FAMILIAR, "miniature crystal ball"),
            withProperty("crystalBallPredictions", "0:The Defiled Nook:" + predictionMonster));
    try (cleanups) {
      CrystalBallManager.reset();
      // From String
      assertEquals(
          Boolean.parseBoolean(result),
          CrystalBallManager.isCrystalBallMonster(monsterName, "The Defiled Nook"));
      // Or from MonsterData
      var monster = MonsterDatabase.findMonster(monsterName);
      assertEquals(
          Boolean.parseBoolean(result),
          CrystalBallManager.isCrystalBallMonster(monster, "The Defiled Nook"));
    }
  }

  @Test
  public void checkMonsterIsNotPredictedWithoutEquippingOrb() {
    assertFalse(CrystalBallManager.isCrystalBallMonster(NAILER, "The Smut Orc Logging Camp"));
  }

  @Test
  public void crystalBallMonsterTestNextEncounter() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.BADGER),
            withEquipped(EquipmentManager.FAMILIAR, "miniature crystal ball"));
    try (cleanups) {
      assertFalse(CrystalBallManager.isCrystalBallMonster());

      MonsterStatusTracker.setNextMonster(SKELTEON);
      assertFalse(CrystalBallManager.isCrystalBallMonster());

      Preferences.setString("nextAdventure", "The Defiled Nook");
      assertTrue(CrystalBallManager.isCrystalBallMonster());
    }
  }

  @Test
  public void canParsePonder() {
    String html = html("request/test_ponder_orb_one_prediction.html");
    CrystalBallManager.parsePonder(html);
    assertEquals("0:Twin Peak:Creepy Ginger Twin", Preferences.getString("crystalBallPredictions"));
  }

  @Test
  public void canParsePonderWithoutUpdatingTurn() {
    var cleanups =
        new Cleanups(
            withCurrentRun(1),
            withProperty("crystalBallPredictions", "0:Twin Peak:Creepy Ginger Twin"));

    try (cleanups) {
      CrystalBallManager.reset();
      String html = html("request/test_ponder_orb_one_prediction.html");
      CrystalBallManager.parsePonder(html);
      assertThat("crystalBallPredictions", isSetTo("0:Twin Peak:Creepy Ginger Twin"));
    }
  }

  @Test
  public void canParsePonderWithMultiple() {
    String html = html("request/test_ponder_orb_two_predictions.html");
    CrystalBallManager.parsePonder(html);
    assertEquals(
        "0:A-Boo Peak:Dusken Raider Ghost|0:Twin Peak:Creepy Ginger Twin",
        Preferences.getString("crystalBallPredictions"),
        Preferences.getString("crystalBallPredictions"));
  }

  @Test
  public void canParsePonderWithArticles() {
    String html = html("request/test_ponder_orb_some_article.html");
    CrystalBallManager.parsePonder(html);
    assertEquals(
        "0:The Haunted Ballroom:zombie waltzers", Preferences.getString("crystalBallPredictions"));
  }

  @Test
  public void canParsePonderWithNameStartingWithArticle() {
    String html = html("request/test_ponder_orb_the_gunk.html");
    CrystalBallManager.parsePonder(html);
    assertEquals(
        "0:The Haunted Laboratory:the gunk", Preferences.getString("crystalBallPredictions"));
  }

  @Test
  public void canHandleAmbiguousSnowmanNinja() {
    String html = html("request/test_ponder_orb_snowman_ninja.html");

    CrystalBallManager.parsePonder(html);
    assertThat(
        "crystalBallPredictions",
        isSetTo("0:Lair of the Ninja Snowmen:Ninja Snowman (Chopsticks)"));
  }

  @Test
  public void canParseWhiteKnight() {
    String html = html("request/test_ponder_white_knight_whitey_grove.html");

    CrystalBallManager.parsePonder(html);
    assertThat("crystalBallPredictions", isSetTo("0:Whitey's Grove:Knight in White Satin"));
  }

  @Test
  public void canParseBatwingedGremlin() {
    String html = html("request/test_ponder_batwinged_gremlin_burning_barrel.html");

    CrystalBallManager.parsePonder(html);
    assertThat(
        "crystalBallPredictions",
        isSetTo("0:Next to that Barrel with Something Burning in it:batwinged gremlin"));
  }

  @Test
  public void canParseKnightInBurningBarrel() {
    // The html for this was edited
    String html = html("request/test_ponder_knight_white_satin_burning_barrel.html");

    CrystalBallManager.parsePonder(html);
    assertThat(
        "crystalBallPredictions",
        isSetTo("0:Next to that Barrel with Something Burning in it:Knight in White Satin"));
  }

  @Test
  public void testPredictionDoesNotExpireSameZone() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.BADGER),
            withEquipped(EquipmentManager.FAMILIAR, "miniature crystal ball"),
            withCurrentRun(0),
            withProperty("crystalBallPredictions", "0:The Smut Orc Logging Camp:smut orc nailer"),
            withLastLocation("The Smut Orc Logging Camp"));
    try (cleanups) {
      CrystalBallManager.reset();
      KoLCharacter.setCurrentRun(2);

      CrystalBallManager.updateCrystalBallPredictions();

      assertThat("crystalBallPredictions", isSetTo("0:The Smut Orc Logging Camp:smut orc nailer"));
    }
  }

  @Test
  public void testPredictionDoesNotExpireSameTurn() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.BADGER),
            withEquipped(EquipmentManager.FAMILIAR, "miniature crystal ball"),
            withCurrentRun(0),
            withLastLocation("The Middle Chamber"),
            withProperty("crystalBallPredictions", "0:The Smut Orc Logging Camp:smut orc nailer"));
    try (cleanups) {
      CrystalBallManager.reset();
      KoLAdventure.setLastAdventure(AdventureDatabase.getAdventure(406));

      CrystalBallManager.updateCrystalBallPredictions();

      assertThat("crystalBallPredictions", isSetTo("0:The Smut Orc Logging Camp:smut orc nailer"));
    }
  }

  @Test
  public void testPredictionExpiresNewZoneNewTurn() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.BADGER),
            withEquipped(EquipmentManager.FAMILIAR, "miniature crystal ball"),
            withCurrentRun(0),
            withLastLocation("The Smut Orc Logging Camp"),
            withProperty("crystalBallPredictions", "0:The Smut Orc Logging Camp:smut orc nailer"));
    try (cleanups) {
      CrystalBallManager.reset();
      KoLCharacter.setCurrentRun(2);

      var adventure = AdventureDatabase.getAdventure(406);
      KoLAdventure.setLastAdventure(adventure);
      KoLAdventure.lastZoneName = adventure.getAdventureName();

      CrystalBallManager.updateCrystalBallPredictions();

      assertThat("crystalBallPredictions", isSetTo(""));
    }
  }

  @Test
  public void testVanillaLocationIsntUpdated() {
    // The Throne Room does not count as a new location to KoL for the purposes of predictions
    // However, mafia does count it as a location change

    var cleanups =
        new Cleanups(
            withLastLocation("Cobb's Knob Harem"),
            withNextResponse(
                new FakeHttpResponse<>(302, Map.of("location", List.of("fight.php")), ""),
                new FakeHttpResponse<>(
                    200,
                    new HashMap<>(),
                    html("request/test_adventure_last_vanilla_location_unchanged.html"))));

    try (cleanups) {
      GenericRequest request = new GenericRequest("cobbsknob.php?action=throneroom");
      request.run();

      assertEquals("Throne Room", KoLAdventure.lastVisitedLocation().getAdventureName());
      assertEquals("Cobb's Knob Harem", KoLAdventure.lastZoneName);
    }
  }

  @Test
  public void testVanillaAndMafiaTracksNormalLocation() {
    var cleanups =
        new Cleanups(
            withLastLocation("Cobb's Knob Harem"),
            withNextResponse(
                new FakeHttpResponse<>(302, Map.of("location", List.of("fight.php")), ""),
                new FakeHttpResponse<>(
                    200,
                    new HashMap<>(),
                    html("request/test_adventure_last_vanilla_location_unchanged.html"))));

    // The Throne Room does not count as a new location to KoL for the purposes of predictions
    // However, mafia does count it as one.
    try (cleanups) {
      GenericRequest request = new GenericRequest("cobbsknob.php?action=throneroom");
      request.run();

      assertEquals("Throne Room", KoLAdventure.lastVisitedLocation().getAdventureName());
      assertEquals("Cobb's Knob Harem", KoLAdventure.lastZoneName);
    }
  }

  @Test
  public void testVanillaLocationIsUpdated() {
    // The Cake Arena counts as a new location to KoL
    // However, mafia does not count this as a real zone

    var cleanups =
        new Cleanups(
            withLastLocation("Cobb's Knob Harem"),
            withFamiliar(FamiliarPool.GREY_GOOSE),
            withMeat(500),
            withNextResponse(
                new FakeHttpResponse<>(
                    200,
                    new HashMap<>(),
                    html("request/test_adventure_internally_tracks_cake_arena_visit.html")),
                new FakeHttpResponse<>(
                    200,
                    new HashMap<>(),
                    html("request/test_adventure_internally_tracks_cake_arena_train.html")),
                new FakeHttpResponse<>(
                    200,
                    new HashMap<>(),
                    html("request/test_adventure_internally_tracks_cake_arena_status.json"))));

    try (cleanups) {
      new GenericRequest("arena.php").run();
      new GenericRequest("arena.php?action=go&whichopp=3&event=1").run();

      // Mafia does not track the cake arena
      assertEquals("Cobb's Knob Harem", KoLAdventure.lastVisitedLocation().getAdventureName());
      // KoL does track the cake arena for purposes of crystal ball prediction
      assertEquals("The Cake-Shaped Arena", KoLAdventure.lastZoneName);
    }
  }

  @Test
  public void testCrystalBallInvalidatesWithoutApiStatus() {
    // The Cake Arena counts as a new location to KoL
    // However, mafia does not count this as a real zone

    var cleanups =
        new Cleanups(
            withLastLocation("The Hidden Office Building"),
            withFamiliar(FamiliarPool.GREY_GOOSE),
            withNextResponse(
                new FakeHttpResponse<>(
                    200,
                    new HashMap<>(),
                    html(
                        "request/test_adventure_crystal_ball_invalidates_properly_choice_status.json")),
                new FakeHttpResponse<>(
                    302, Map.of("location", List.of("choice.php?forceoption=0")), ""),
                new FakeHttpResponse<>(
                    200,
                    new HashMap<>(),
                    html("request/test_adventure_crystal_ball_invalidates_properly_choice.html")),
                new FakeHttpResponse<>(
                    200,
                    new HashMap<>(),
                    html(
                        "request/test_adventure_crystal_ball_invalidates_properly_choice_result.html"))),
            withProperty(
                "crystalBallPredictions", "291:The Hidden Office Building:pygmy headhunter"),
            withGender(Gender.FEMALE),
            withCurrentRun(0));

    try (cleanups) {
      CrystalBallManager.reset();

      ApiRequest.updateStatus(true);

      assertThat(
          "crystalBallPredictions", isSetTo("291:The Hidden Office Building:pygmy headhunter"));

      assertEquals("The Hidden Office Building", KoLAdventure.lastZoneName);
      assertEquals(
          "The Hidden Office Building", KoLAdventure.lastVisitedLocation().getAdventureName());

      new GenericRequest("adventure.php?snarfblat=348").run();
      new GenericRequest("choice.php?pwd&whichchoice=785&option=2").run();

      assertEquals("An Overgrown Shrine (Northeast)", KoLAdventure.lastZoneName);

      assertThat("crystalBallPredictions", isSetTo(""));
    }
  }

  @Test
  public void testCrystalBallDoesInvalidateProperly() {
    // The Cake Arena counts as a new location to KoL
    // However, mafia does not count this as a real zone

    var cleanups =
        new Cleanups(
            withLastLocation("Cobb's Knob Harem"),
            withFamiliar(FamiliarPool.GREY_GOOSE),
            withNextResponse(
                new FakeHttpResponse<>(
                    200,
                    new HashMap<>(),
                    html(
                        "request/test_adventure_crystal_ball_handles_noncombat_api_preadventure.json")),
                new FakeHttpResponse<>(
                    200,
                    new HashMap<>(),
                    html("request/test_adventure_crystal_ball_handles_noncombat.html")),
                new FakeHttpResponse<>(
                    200,
                    new HashMap<>(),
                    html(
                        "request/test_adventure_crystal_ball_handles_noncombat_api_afteradventure.json"))),
            withProperty("crystalBallPredictions", "522:The Middle Chamber:tomb rat"),
            withGender(Gender.FEMALE),
            withCurrentRun(0));

    try (cleanups) {
      CrystalBallManager.reset();

      ApiRequest.updateStatus(true);

      assertThat("crystalBallPredictions", isSetTo("522:The Middle Chamber:tomb rat"));

      new GenericRequest("adventure.php?snarfblat=407").run();

      assertEquals("The Middle Chamber", KoLAdventure.lastZoneName);
      assertEquals("The Middle Chamber", KoLAdventure.lastVisitedLocation().getAdventureName());
      assertThat("crystalBallPredictions", isSetTo("522:The Middle Chamber:tomb rat"));
    }
  }
}
