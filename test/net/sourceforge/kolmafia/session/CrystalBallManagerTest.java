package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withCurrentRun;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withLastLocation;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
  @Disabled("Mafia cannot detect the actual monster of the ambiguous name 'Ninja Snowman'")
  public void canParseSnowmanNinja() {
    String html = html("request/test_ponder_orb_snowman_ninja.html");

    CrystalBallManager.parsePonder(html);
    assertThat("crystalBallPredictions", isSetTo("0:Lair of the Ninja Snowmen:Ninja Snowman"));
  }

  @Test
  public void canParseWhiteKnight() {
    String html = html("request/test_ponder_white_knight_whitey_grove.html");

    CrystalBallManager.parsePonder(html);
    assertThat("crystalBallPredictions", isSetTo("0:Whitey's Grove:Knight in White Satin"));
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
      KoLAdventure.lastVanillaLocationName = adventure.getAdventureName();

      CrystalBallManager.updateCrystalBallPredictions();

      assertThat("crystalBallPredictions", isSetTo(""));
    }
  }
}
