package net.sourceforge.kolmafia;

import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withStats;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class KoLAdventureValidationTest {

  @BeforeAll
  public static void beforeAll() {
    // Simulate logging out and back in again.
    Preferences.saveSettingsToFile = false;
  }

  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("KoLAdventure");
    Preferences.reset("KoLAdventure");
    KoLConstants.inventory.clear();
  }

  @AfterAll
  public static void afterAll() {
    Preferences.saveSettingsToFile = true;
  }

  @Nested
  class Standard {
    @Test
    public void restrictedItemZonesNotAllowedUnderStandard() {
      var cleanups = new Cleanups(withRestricted(true));
      try (cleanups) {
        // From the tiny bottle of absinthe - a very old item
        KoLAdventure area = AdventureDatabase.getAdventureByName("The Stately Pleasure Dome");
        assertFalse(area.canAdventure());
      }
    }

    @Test
    public void nonItemZonesAllowedUnderStandard() {
      var cleanups = new Cleanups(withRestricted(true));
      try (cleanups) {
        KoLAdventure area = AdventureDatabase.getAdventureByName("The Outskirts of Cobb's Knob");
        assertTrue(area.canAdventure());
      }
    }
  }

  @Nested
  class BugbearInvasion {
    @Test
    public void cannotVisitMothershipUnlessBugbearsAreInvading() {
      var cleanups = new Cleanups(withPath(Path.NONE));
      try (cleanups) {
        KoLAdventure area = AdventureDatabase.getAdventureByName("Medbay");
        assertFalse(area.canAdventure());
      }
    }

    @Test
    public void canVisitMothershipUnlessBugbearsAreInvading() {
      var cleanups = new Cleanups(withPath(Path.BUGBEAR_INVASION));
      try (cleanups) {
        KoLAdventure area = AdventureDatabase.getAdventureByName("Medbay");
        assertTrue(area.canAdventure());
      }
    }
  }

  @Nested
  class AirportCharters {

    // Simplify looking up the KoLAdventure based on adventure ID.
    private static Map<Integer, KoLAdventure> sleazeZones = new HashMap<>();
    private static Map<Integer, KoLAdventure> spookyZones = new HashMap<>();
    private static Map<Integer, KoLAdventure> stenchZones = new HashMap<>();
    private static Map<Integer, KoLAdventure> hotZones = new HashMap<>();
    private static Map<Integer, KoLAdventure> coldZones = new HashMap<>();

    @BeforeAll
    public static void beforeAll() {
      sleazeZones.put(
          AdventurePool.FUN_GUY_MANSION,
          AdventureDatabase.getAdventureByName("The Fun-Guy Mansion"));
      sleazeZones.put(
          AdventurePool.SLOPPY_SECONDS_DINER,
          AdventureDatabase.getAdventureByName("Sloppy Seconds Diner"));
      sleazeZones.put(
          AdventurePool.YACHT, AdventureDatabase.getAdventureByName("The Sunken Party Yacht"));

      spookyZones.put(
          AdventurePool.DR_WEIRDEAUX,
          AdventureDatabase.getAdventureByName("The Mansion of Dr. Weirdeaux"));
      spookyZones.put(
          AdventurePool.SECRET_GOVERNMENT_LAB,
          AdventureDatabase.getAdventureByName("The Secret Government Laboratory"));
      spookyZones.put(
          AdventurePool.DEEP_DARK_JUNGLE,
          AdventureDatabase.getAdventureByName("The Deep Dark Jungle"));

      stenchZones.put(
          AdventurePool.BARF_MOUNTAIN, AdventureDatabase.getAdventureByName("Barf Mountain"));
      stenchZones.put(
          AdventurePool.GARBAGE_BARGES,
          AdventureDatabase.getAdventureByName("Pirates of the Garbage Barges"));
      stenchZones.put(
          AdventurePool.TOXIC_TEACUPS, AdventureDatabase.getAdventureByName("The Toxic Teacups"));
      stenchZones.put(
          AdventurePool.LIQUID_WASTE_SLUICE,
          AdventureDatabase.getAdventureByName(
              "Uncle Gator's Country Fun-Time Liquid Waste Sluice"));

      hotZones.put(
          AdventurePool.SMOOCH_ARMY_HQ, AdventureDatabase.getAdventureByName("The SMOOCH Army HQ"));
      hotZones.put(
          AdventurePool.VELVET_GOLD_MINE,
          AdventureDatabase.getAdventureByName("The Velvet / Gold Mine"));
      hotZones.put(
          AdventurePool.LAVACO_LAMP_FACTORY,
          AdventureDatabase.getAdventureByName("LavaCo&trade; Lamp Factory"));
      hotZones.put(
          AdventurePool.BUBBLIN_CALDERA,
          AdventureDatabase.getAdventureByName("The Bubblin' Caldera"));

      coldZones.put(AdventurePool.ICE_HOTEL, AdventureDatabase.getAdventureByName("The Ice Hotel"));
      coldZones.put(AdventurePool.VYKEA, AdventureDatabase.getAdventureByName("VYKEA"));
      coldZones.put(AdventurePool.ICE_HOLE, AdventureDatabase.getAdventureByName("The Ice Hole"));
    }

    @AfterAll
    public static void afterAll() {
      sleazeZones.clear();
      spookyZones.clear();
      stenchZones.clear();
      hotZones.clear();
      coldZones.clear();
    }

    private void testElementalZoneNoAccess(Map<Integer, KoLAdventure> zones) {
      for (Integer key : zones.keySet()) {
        assertFalse(zones.get(key).canAdventure());
      }
    }

    private void testElementalZoneWithAccess(Map<Integer, KoLAdventure> zones, String property) {
      var cleanups = new Cleanups(withProperty(property, true));
      try (cleanups) {
        for (Integer key : zones.keySet()) {
          assertTrue(zones.get(key).canAdventure());
        }
      }
    }

    @Test
    public void cannotVisitElementalZonesWithoutAccess() {
      testElementalZoneNoAccess(sleazeZones);
      testElementalZoneNoAccess(spookyZones);
      testElementalZoneNoAccess(stenchZones);
      testElementalZoneNoAccess(hotZones);
      testElementalZoneNoAccess(coldZones);
    }

    @Test
    public void canVisitElementalZonesWithAllAccess() {
      testElementalZoneWithAccess(sleazeZones, "sleazeAirportAlways");
      testElementalZoneWithAccess(spookyZones, "spookyAirportAlways");
      testElementalZoneWithAccess(stenchZones, "stenchAirportAlways");
      testElementalZoneWithAccess(hotZones, "hotAirportAlways");
      testElementalZoneWithAccess(coldZones, "coldAirportAlways");
    }

    @Test
    public void canVisitElementalZonesWithDailyAccess() {
      testElementalZoneWithAccess(sleazeZones, "_sleazeAirportToday");
      testElementalZoneWithAccess(spookyZones, "_spookyAirportToday");
      testElementalZoneWithAccess(stenchZones, "_stenchAirportToday");
      testElementalZoneWithAccess(hotZones, "_hotAirportToday");
      testElementalZoneWithAccess(coldZones, "_coldAirportToday");
    }
  }

  @Nested
  class CobbsKnob {

    // Simplify looking up the KoLAdventure based on adventure ID.
    private static Map<Integer, KoLAdventure> zones = new HashMap<>();
    private static KoLAdventure throneRoom = AdventureDatabase.getAdventureByName("Throne Room");

    @BeforeAll
    public static void beforeAll() {
      zones.put(
          AdventurePool.OUTSKIRTS_OF_THE_KNOB,
          AdventureDatabase.getAdventureByName("The Outskirts of Cobb's Knob"));
      zones.put(
          AdventurePool.COBB_BARRACKS,
          AdventureDatabase.getAdventureByName("Cobb's Knob Barracks"));
      zones.put(
          AdventurePool.COBB_KITCHEN, AdventureDatabase.getAdventureByName("Cobb's Knob Kitchens"));
      zones.put(
          AdventurePool.COBB_HAREM, AdventureDatabase.getAdventureByName("Cobb's Knob Harem"));
      zones.put(
          AdventurePool.COBB_TREASURY,
          AdventureDatabase.getAdventureByName("Cobb's Knob Treasury"));
      zones.put(
          AdventurePool.COBB_LABORATORY,
          AdventureDatabase.getAdventureByName("Cobb's Knob Laboratory"));
      zones.put(AdventurePool.KNOB_SHAFT, AdventureDatabase.getAdventureByName("The Knob Shaft"));
      zones.put(
          AdventurePool.MENAGERIE_LEVEL_1,
          AdventureDatabase.getAdventureByName("Cobb's Knob Menagerie, Level 1"));
      zones.put(
          AdventurePool.MENAGERIE_LEVEL_2,
          AdventureDatabase.getAdventureByName("Cobb's Knob Menagerie, Level 2"));
      zones.put(
          AdventurePool.MENAGERIE_LEVEL_3,
          AdventureDatabase.getAdventureByName("Cobb's Knob Menagerie, Level 3"));
    }

    @AfterAll
    public static void afterAll() {
      zones.clear();
      throneRoom = null;
    }

    @Test
    public void canVisitCobbsKnobBeforeQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.OUTSKIRTS_OF_THE_KNOB).canAdventure());
        assertFalse(zones.get(AdventurePool.COBB_BARRACKS).canAdventure());
        assertFalse(zones.get(AdventurePool.COBB_KITCHEN).canAdventure());
        assertFalse(zones.get(AdventurePool.COBB_HAREM).canAdventure());
        assertFalse(zones.get(AdventurePool.COBB_TREASURY).canAdventure());
        assertFalse(throneRoom.canAdventure());
      }
    }

    @Test
    public void canVisitCobbsKnobBeforeDecrypting() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.OUTSKIRTS_OF_THE_KNOB).canAdventure());
        assertFalse(zones.get(AdventurePool.COBB_BARRACKS).canAdventure());
        assertFalse(zones.get(AdventurePool.COBB_KITCHEN).canAdventure());
        assertFalse(zones.get(AdventurePool.COBB_HAREM).canAdventure());
        assertFalse(zones.get(AdventurePool.COBB_TREASURY).canAdventure());
        assertFalse(throneRoom.canAdventure());
      }
    }

    @Test
    public void canVisitCobbsKnobAfterDecrypting() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, "step1"));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.OUTSKIRTS_OF_THE_KNOB).canAdventure());
        assertTrue(zones.get(AdventurePool.COBB_BARRACKS).canAdventure());
        assertTrue(zones.get(AdventurePool.COBB_KITCHEN).canAdventure());
        assertTrue(zones.get(AdventurePool.COBB_HAREM).canAdventure());
        assertTrue(zones.get(AdventurePool.COBB_TREASURY).canAdventure());
        assertTrue(throneRoom.canAdventure());
      }
    }

    @Test
    public void canVisitCobbsKnobAfterDefeatingKing() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, QuestDatabase.FINISHED));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.OUTSKIRTS_OF_THE_KNOB).canAdventure());
        assertTrue(zones.get(AdventurePool.COBB_BARRACKS).canAdventure());
        assertTrue(zones.get(AdventurePool.COBB_KITCHEN).canAdventure());
        assertTrue(zones.get(AdventurePool.COBB_HAREM).canAdventure());
        assertTrue(zones.get(AdventurePool.COBB_TREASURY).canAdventure());
        assertFalse(throneRoom.canAdventure());
      }
    }

    @Test
    public void cannotVisitCobbsKnobLaboratoryWithoutKey() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, "step1"));
      try (cleanups) {
        assertFalse(zones.get(AdventurePool.COBB_LABORATORY).canAdventure());
        assertFalse(zones.get(AdventurePool.KNOB_SHAFT).canAdventure());
      }
    }

    @Test
    public void canVisitCobbsKnobLaboratoryWithKey() {
      var cleanups =
          new Cleanups(withItem("Cobb's Knob lab key"), withQuestProgress(Quest.GOBLIN, "step1"));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.COBB_LABORATORY).canAdventure());
        assertTrue(zones.get(AdventurePool.KNOB_SHAFT).canAdventure());
      }
    }

    @Test
    public void cannotVisitCobbsKnobMenagerieWithoutKey() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, "step1"));
      try (cleanups) {
        assertFalse(zones.get(AdventurePool.MENAGERIE_LEVEL_1).canAdventure());
        assertFalse(zones.get(AdventurePool.MENAGERIE_LEVEL_2).canAdventure());
        assertFalse(zones.get(AdventurePool.MENAGERIE_LEVEL_3).canAdventure());
      }
    }

    @Test
    public void canVisitCobbsKnobMenagerieWithKey() {
      var cleanups =
          new Cleanups(
              withItem("Cobb's Knob Menagerie key"), withQuestProgress(Quest.GOBLIN, "step1"));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.MENAGERIE_LEVEL_1).canAdventure());
        assertTrue(zones.get(AdventurePool.MENAGERIE_LEVEL_2).canAdventure());
        assertTrue(zones.get(AdventurePool.MENAGERIE_LEVEL_3).canAdventure());
      }
    }
  }

  @Nested
  class Cyrpt {

    // Simplify looking up the KoLAdventure based on adventure ID.
    private static Map<Integer, KoLAdventure> zones = new HashMap<>();
    private static KoLAdventure haert = AdventureDatabase.getAdventureByName("Haert of the Cyrpt");

    @BeforeAll
    public static void beforeAll() {
      zones.put(
          AdventurePool.DEFILED_ALCOVE, AdventureDatabase.getAdventureByName("The Defiled Alcove"));
      zones.put(
          AdventurePool.DEFILED_CRANNY, AdventureDatabase.getAdventureByName("The Defiled Cranny"));
      zones.put(
          AdventurePool.DEFILED_NICHE, AdventureDatabase.getAdventureByName("The Defiled Niche"));
      zones.put(
          AdventurePool.DEFILED_NOOK, AdventureDatabase.getAdventureByName("The Defiled Nook"));
    }

    @AfterAll
    public static void afterAll() {
      zones.clear();
      haert = null;
    }

    @Test
    public void cannotVisitCyrptBeforeQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.CYRPT, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertFalse(zones.get(AdventurePool.DEFILED_ALCOVE).canAdventure());
        assertFalse(zones.get(AdventurePool.DEFILED_CRANNY).canAdventure());
        assertFalse(zones.get(AdventurePool.DEFILED_NICHE).canAdventure());
        assertFalse(zones.get(AdventurePool.DEFILED_NOOK).canAdventure());
        assertFalse(haert.canAdventure());
      }
    }

    @Test
    public void canVisitCyrptWhenQuestStarted() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.CYRPT, QuestDatabase.STARTED),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptTotalEvilness", 200));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.DEFILED_ALCOVE).canAdventure());
        assertTrue(zones.get(AdventurePool.DEFILED_CRANNY).canAdventure());
        assertTrue(zones.get(AdventurePool.DEFILED_NICHE).canAdventure());
        assertTrue(zones.get(AdventurePool.DEFILED_NOOK).canAdventure());
        assertFalse(haert.canAdventure());
      }
    }

    @Test
    public void canVisitCyrptWhenAlcoveClear() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.CYRPT, QuestDatabase.STARTED),
              withProperty("cyrptAlcoveEvilness", 0),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptTotalEvilness", 150));
      try (cleanups) {
        assertFalse(zones.get(AdventurePool.DEFILED_ALCOVE).canAdventure());
        assertTrue(zones.get(AdventurePool.DEFILED_CRANNY).canAdventure());
        assertTrue(zones.get(AdventurePool.DEFILED_NICHE).canAdventure());
        assertTrue(zones.get(AdventurePool.DEFILED_NOOK).canAdventure());
        assertFalse(haert.canAdventure());
      }
    }

    @Test
    public void canVisitCyrptWhenCrannyClear() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.CYRPT, QuestDatabase.STARTED),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptCrannyEvilness", 0),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptTotalEvilness", 150));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.DEFILED_ALCOVE).canAdventure());
        assertFalse(zones.get(AdventurePool.DEFILED_CRANNY).canAdventure());
        assertTrue(zones.get(AdventurePool.DEFILED_NICHE).canAdventure());
        assertTrue(zones.get(AdventurePool.DEFILED_NOOK).canAdventure());
        assertFalse(haert.canAdventure());
      }
    }

    @Test
    public void canVisitCyrptWhenNicheClear() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.CYRPT, QuestDatabase.STARTED),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptNicheEvilness", 0),
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptTotalEvilness", 150));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.DEFILED_ALCOVE).canAdventure());
        assertTrue(zones.get(AdventurePool.DEFILED_CRANNY).canAdventure());
        assertFalse(zones.get(AdventurePool.DEFILED_NICHE).canAdventure());
        assertTrue(zones.get(AdventurePool.DEFILED_NOOK).canAdventure());
        assertFalse(haert.canAdventure());
      }
    }

    @Test
    public void canVisitCyrptWhenNookClear() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.CYRPT, QuestDatabase.STARTED),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptNookEvilness", 0),
              withProperty("cyrptTotalEvilness", 150));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.DEFILED_ALCOVE).canAdventure());
        assertTrue(zones.get(AdventurePool.DEFILED_CRANNY).canAdventure());
        assertTrue(zones.get(AdventurePool.DEFILED_NICHE).canAdventure());
        assertFalse(zones.get(AdventurePool.DEFILED_NOOK).canAdventure());
        assertFalse(haert.canAdventure());
      }
    }

    @Test
    public void canVisitHaertWhenCyrptClear() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.CYRPT, QuestDatabase.STARTED),
              withProperty("cyrptAlcoveEvilness", 0),
              withProperty("cyrptCrannyEvilness", 0),
              withProperty("cyrptNicheEvilness", 0),
              withProperty("cyrptNookEvilness", 0),
              withProperty("cyrptTotalEvilness", 0));
      try (cleanups) {
        assertFalse(zones.get(AdventurePool.DEFILED_ALCOVE).canAdventure());
        assertFalse(zones.get(AdventurePool.DEFILED_CRANNY).canAdventure());
        assertFalse(zones.get(AdventurePool.DEFILED_NICHE).canAdventure());
        assertFalse(zones.get(AdventurePool.DEFILED_NOOK).canAdventure());
        assertTrue(haert.canAdventure());
      }
    }

    @Test
    public void cannotVisitCyrptWhenQuestFinished() {
      var cleanups = new Cleanups(withQuestProgress(Quest.CYRPT, QuestDatabase.FINISHED));
      try (cleanups) {
        assertFalse(zones.get(AdventurePool.DEFILED_ALCOVE).canAdventure());
        assertFalse(zones.get(AdventurePool.DEFILED_CRANNY).canAdventure());
        assertFalse(zones.get(AdventurePool.DEFILED_NICHE).canAdventure());
        assertFalse(zones.get(AdventurePool.DEFILED_NOOK).canAdventure());
        assertFalse(haert.canAdventure());
      }
    }
  }

  @Nested
  class Pirate {

    // Simplify looking up the KoLAdventure based on adventure ID.
    private static Map<Integer, KoLAdventure> zones = new HashMap<>();

    @BeforeAll
    public static void beforeAll() {
      zones.put(
          AdventurePool.PIRATE_COVE,
          AdventureDatabase.getAdventureByName("The Obligatory Pirate's Cove"));
      zones.put(
          AdventurePool.BARRRNEYS_BARRR, AdventureDatabase.getAdventureByName("Barrrney's Barrr"));
      zones.put(AdventurePool.FCLE, AdventureDatabase.getAdventureByName("The F'c'le"));
      zones.put(AdventurePool.POOP_DECK, AdventureDatabase.getAdventureByName("The Poop Deck"));
      zones.put(AdventurePool.BELOWDECKS, AdventureDatabase.getAdventureByName("Belowdecks"));
    }

    @AfterAll
    public static void afterAll() {
      zones.clear();
    }

    @AfterEach
    public void afterEach() {
      EquipmentManager.updateNormalOutfits();
    }

    @Test
    public void cannotVisitPiratesWithoutIslandAccess() {
      var cleanups = new Cleanups(withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertFalse(zones.get(AdventurePool.PIRATE_COVE).canAdventure());
        assertFalse(zones.get(AdventurePool.BARRRNEYS_BARRR).canAdventure());
        assertFalse(zones.get(AdventurePool.FCLE).canAdventure());
        assertFalse(zones.get(AdventurePool.POOP_DECK).canAdventure());
        assertFalse(zones.get(AdventurePool.BELOWDECKS).canAdventure());
      }
    }

    @Test
    public void canVisitPiratesUndisguised() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.PIRATE_COVE).canAdventure());
        assertFalse(zones.get(AdventurePool.BARRRNEYS_BARRR).canAdventure());
        assertFalse(zones.get(AdventurePool.FCLE).canAdventure());
        assertFalse(zones.get(AdventurePool.POOP_DECK).canAdventure());
        assertFalse(zones.get(AdventurePool.BELOWDECKS).canAdventure());
      }
    }

    @Test
    public void cannotVisitPiratesDuringWar() {
      var cleanups =
          new Cleanups(withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, "step1"));
      try (cleanups) {
        assertFalse(zones.get(AdventurePool.PIRATE_COVE).canAdventure());
        assertFalse(zones.get(AdventurePool.BARRRNEYS_BARRR).canAdventure());
        assertFalse(zones.get(AdventurePool.FCLE).canAdventure());
        assertFalse(zones.get(AdventurePool.POOP_DECK).canAdventure());
        assertFalse(zones.get(AdventurePool.BELOWDECKS).canAdventure());
      }
    }

    @Test
    public void canVisitPirateShipDisguised() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withStats(25, 25, 25),
              withItem("eyepatch"),
              withItem("swashbuckling pants"),
              withItem("stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, QuestDatabase.FINISHED));
      try (cleanups) {
        EquipmentManager.updateNormalOutfits();
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP));
        assertTrue(zones.get(AdventurePool.BARRRNEYS_BARRR).canAdventure());
        assertTrue(zones.get(AdventurePool.FCLE).canAdventure());
        assertTrue(zones.get(AdventurePool.POOP_DECK).canAdventure());
        assertTrue(zones.get(AdventurePool.BELOWDECKS).canAdventure());
      }
    }

    @Test
    public void canVisitPirateShipFledged() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withStats(25, 60, 25),
              withItem("pirate fledges"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, QuestDatabase.FINISHED));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.BARRRNEYS_BARRR).canAdventure());
        assertTrue(zones.get(AdventurePool.FCLE).canAdventure());
        assertTrue(zones.get(AdventurePool.POOP_DECK).canAdventure());
        assertTrue(zones.get(AdventurePool.BELOWDECKS).canAdventure());
      }
    }

    @Test
    public void canVisitPirateShipBeforeQuest() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withStats(25, 25, 25),
              withItem("eyepatch"),
              withItem("swashbuckling pants"),
              withItem("stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, QuestDatabase.UNSTARTED));
      try (cleanups) {
        EquipmentManager.updateNormalOutfits();
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP));
        assertTrue(zones.get(AdventurePool.BARRRNEYS_BARRR).canAdventure());
        assertFalse(zones.get(AdventurePool.FCLE).canAdventure());
        assertFalse(zones.get(AdventurePool.POOP_DECK).canAdventure());
        assertFalse(zones.get(AdventurePool.BELOWDECKS).canAdventure());
      }
    }

    @Test
    public void canVisitPirateShipFcleDuringQuest() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withStats(25, 25, 25),
              withItem("eyepatch"),
              withItem("swashbuckling pants"),
              withItem("stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, "step5"));
      try (cleanups) {
        EquipmentManager.updateNormalOutfits();
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP));
        assertTrue(zones.get(AdventurePool.BARRRNEYS_BARRR).canAdventure());
        assertTrue(zones.get(AdventurePool.FCLE).canAdventure());
        assertFalse(zones.get(AdventurePool.POOP_DECK).canAdventure());
        assertFalse(zones.get(AdventurePool.BELOWDECKS).canAdventure());
      }
    }

    @Test
    public void canVisitPirateShipPoopDeckDuringQuest() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withStats(25, 25, 25),
              withItem("eyepatch"),
              withItem("swashbuckling pants"),
              withItem("stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, "step6"));
      try (cleanups) {
        EquipmentManager.updateNormalOutfits();
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP));
        assertTrue(zones.get(AdventurePool.BARRRNEYS_BARRR).canAdventure());
        assertTrue(zones.get(AdventurePool.FCLE).canAdventure());
        assertTrue(zones.get(AdventurePool.POOP_DECK).canAdventure());
        assertFalse(zones.get(AdventurePool.BELOWDECKS).canAdventure());
      }
    }

    @Test
    public void canVisitPirateShipBelowdecksAfterQuest() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withStats(25, 25, 25),
              withItem("eyepatch"),
              withItem("swashbuckling pants"),
              withItem("stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, QuestDatabase.FINISHED));
      try (cleanups) {
        EquipmentManager.updateNormalOutfits();
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP));
        assertTrue(zones.get(AdventurePool.BARRRNEYS_BARRR).canAdventure());
        assertTrue(zones.get(AdventurePool.FCLE).canAdventure());
        assertTrue(zones.get(AdventurePool.POOP_DECK).canAdventure());
        assertTrue(zones.get(AdventurePool.BELOWDECKS).canAdventure());
      }
    }
  }

  @Nested
  class HippyCamp {

    // Adventures available in The Hippy Camp on the Mysterious Island depend
    // on whether the island is peaceful, on the verge of war, or at
    // war. Post-war, the camp may revert to its prewar peace - or may be
    // bombed back to the stone age, if the hippies lost the war.
    //
    // Wearing a hippy or fratboy disguise also changes available encounters.
    //
    // Externally (the image in the browser), the adventure URL always goes to
    // AdventurePool.HIPPY_CAMP, regardless of quest state or disguise. But
    // after adventuring, the "Adventure Again" link in the response and the
    // "Last Adventure" link in the charpane may refer to a different
    // URL. These URLs can also be used, regardless of whether the conditions
    // still hold.
    //
    // AdventurePool.HIPPY_CAMP
    // AdventurePool.HIPPY_CAMP_DISGUISED
    // AdventurePool.WARTIME_HIPPY_CAMP
    // AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED
    //
    // If the hippies lost the war:
    //
    // AdventurePool.BOMBED_HIPPY_CAMP

    // Simplify looking up the KoLAdventure based on adventure ID.
    private static Map<Integer, KoLAdventure> zones = new HashMap<>();

    @BeforeAll
    public static void beforeAll() {
      zones.put(AdventurePool.HIPPY_CAMP, AdventureDatabase.getAdventureByName("Hippy Camp"));
      zones.put(
          AdventurePool.HIPPY_CAMP_DISGUISED,
          AdventureDatabase.getAdventureByName("Hippy Camp (Hippy Disguise)"));
      zones.put(
          AdventurePool.WARTIME_HIPPY_CAMP,
          AdventureDatabase.getAdventureByName("Wartime Hippy Camp"));
      zones.put(
          AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED,
          AdventureDatabase.getAdventureByName("Wartime Hippy Camp (Frat Disguise)"));
      zones.put(
          AdventurePool.BOMBED_HIPPY_CAMP,
          AdventureDatabase.getAdventureByName("The Hippy Camp (Bombed Back to the Stone Age)"));
    }

    @AfterAll
    public static void afterAll() {
      zones.clear();
    }

    @Test
    public void cannotVisitHippyCampWithoutIslandAccess() {
      assertFalse(zones.get(AdventurePool.HIPPY_CAMP).canAdventure());
      assertFalse(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).canAdventure());
      assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).canAdventure());
      assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).canAdventure());
      assertFalse(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).canAdventure());
    }

    @Test
    public void canVisitHippyCampBeforeWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP).canAdventure());
        // We check only quest status, not available equipment
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).canAdventure());
      }
    }

    @Test
    public void canVisitHippyCampInDisguiseBeforeWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withEquipped(EquipmentManager.HAT, "filthy knitted dread sack"),
              withEquipped(EquipmentManager.PANTS, "filthy corduroys"));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP).canAdventure());
        // We check only quest status, not available equipment
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).canAdventure());
      }
    }

    @Test
    public void canVisitHippyCampOnVergeOfWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED));
      try (cleanups) {
        // KoL does not require going directly to verge-od-war zones
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP).canAdventure());
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).canAdventure());
        // ... but it allows it.
        assertTrue(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).canAdventure());
        assertTrue(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).canAdventure());
      }
    }

    @Test
    public void canVisitHippyCampInDisguiseOnVergeOfWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED),
              withEquipped(EquipmentManager.HAT, "Orcish baseball cap"),
              withEquipped(EquipmentManager.PANTS, "Orcish cargo shorts"),
              withEquipped(EquipmentManager.WEAPON, "Orcish frat-paddle"));
      try (cleanups) {
        // KoL does not require going directly to verge-od-war zones
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP).canAdventure());
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).canAdventure());
        // ... but it allows it.
        assertTrue(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).canAdventure());
        assertTrue(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).canAdventure());
      }
    }

    @Test
    public void cannotVisitHippyCampDuringWar() {
      var cleanups =
          new Cleanups(withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, "step1"));
      try (cleanups) {
        assertFalse(zones.get(AdventurePool.HIPPY_CAMP).canAdventure());
        assertFalse(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).canAdventure());
      }
    }

    @Test
    public void canVisitHippyCampAfterWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED),
              withProperty("sideDefeated", "fratboys"));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP).canAdventure());
        // We check only quest status, not available equipment
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).canAdventure());
      }
    }

    @Test
    public void canVisitHippyCampInDisguiseAfterWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED),
              withProperty("sideDefeated", "fratboys"),
              withEquipped(EquipmentManager.HAT, "filthy knitted dread sack"),
              withEquipped(EquipmentManager.PANTS, "filthy corduroys"));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP).canAdventure());
        // We check only quest status, not available equipment
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).canAdventure());
      }
    }

    @Test
    public void canVisitBombedHippyCampAfterLostWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED),
              withProperty("sideDefeated", "hippies"));
      try (cleanups) {
        assertFalse(zones.get(AdventurePool.HIPPY_CAMP).canAdventure());
        assertFalse(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).canAdventure());
        assertTrue(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).canAdventure());
      }
    }

    @Test
    public void canVisitBombedHippyCampAfterWossname() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED),
              withProperty("sideDefeated", "both"));
      try (cleanups) {
        assertFalse(zones.get(AdventurePool.HIPPY_CAMP).canAdventure());
        assertFalse(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).canAdventure());
        assertTrue(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).canAdventure());
      }
    }
  }

  @Nested
  class FratHouse {

    // Adventures available in The Frat House on the Mysterious Island depend
    // on whether the island is peaceful, on the verge of war, or at
    // war. Post-war, the camp may revert to its prewar peace - or may be
    // bombed back to the stone age, if the hippies lost the war.
    //
    // Wearing a hippy or fratboy disguise also changes available encounters.
    //
    // Externally (the image in the browser), the adventure URL always goes to
    // AdventurePool.FRAT_HOUSE, regardless of quest state or disguise. But
    // after adventuring, the "Adventure Again" link in the response and the
    // "Last Adventure" link in the charpane may refer to a different
    // URL. These URLs can also be used, regardless of whether the conditions
    // still hold.
    //
    // AdventurePool.FRAT_HOUSE
    // AdventurePool.FRAT_HOUSE_DISGUISED
    // AdventurePool.WARTIME_FRAT_HOUSE
    // AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED
    //
    // If the hippies lost the war:
    //
    // AdventurePool.BOMBED_FRAT_HOUSE

    // Simplify looking up the KoLAdventure based on adventure ID.
    private static Map<Integer, KoLAdventure> zones = new HashMap<>();

    @BeforeAll
    public static void beforeAll() {
      zones.put(AdventurePool.FRAT_HOUSE, AdventureDatabase.getAdventureByName("Frat House"));
      zones.put(
          AdventurePool.FRAT_HOUSE_DISGUISED,
          AdventureDatabase.getAdventureByName("Frat House (Frat Disguise)"));
      zones.put(
          AdventurePool.WARTIME_FRAT_HOUSE,
          AdventureDatabase.getAdventureByName("Wartime Frat House"));
      zones.put(
          AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED,
          AdventureDatabase.getAdventureByName("Wartime Frat House (Hippy Disguise)"));
      zones.put(
          AdventurePool.BOMBED_FRAT_HOUSE,
          AdventureDatabase.getAdventureByName(
              "The Orcish Frat House (Bombed Back to the Stone Age)"));
    }

    @AfterAll
    public static void afterAll() {
      zones.clear();
    }

    @Test
    public void cannotVisitFratHouseWithoutIslandAccess() {
      assertFalse(zones.get(AdventurePool.FRAT_HOUSE).canAdventure());
      assertFalse(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).canAdventure());
      assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).canAdventure());
      assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).canAdventure());
      assertFalse(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).canAdventure());
    }

    @Test
    public void canVisitFratHouseBeforeWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE).canAdventure());
        // We check only quest status, not available equipment
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).canAdventure());
      }
    }

    @Test
    public void canVisitFratHouseInDisguiseBeforeWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withEquipped(EquipmentManager.HAT, "Orcish baseball cap"),
              withEquipped(EquipmentManager.PANTS, "Orcish cargo shorts"),
              withEquipped(EquipmentManager.WEAPON, "Orcish frat-paddle"));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE).canAdventure());
        // We check only quest status, not available equipment
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).canAdventure());
      }
    }

    @Test
    public void canVisitFratHouseOnVergeOfWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED));
      try (cleanups) {
        // KoL does not require going directly to verge-od-war zones
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE).canAdventure());
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).canAdventure());
        // ... but it allows it.
        assertTrue(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).canAdventure());
        assertTrue(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).canAdventure());
      }
    }

    @Test
    public void canVisitFratHouseInDisguiseOnVergeOfWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED),
              withEquipped(EquipmentManager.HAT, "filthy knitted dread sack"),
              withEquipped(EquipmentManager.PANTS, "filthy corduroys"));
      try (cleanups) {
        // KoL does not require going directly to verge-od-war zones
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE).canAdventure());
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).canAdventure());
        // ... but it allows it.
        assertTrue(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).canAdventure());
        assertTrue(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).canAdventure());
      }
    }

    @Test
    public void cannotVisitFratHouseDuringWar() {
      var cleanups =
          new Cleanups(withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, "step1"));
      try (cleanups) {
        assertFalse(zones.get(AdventurePool.FRAT_HOUSE).canAdventure());
        assertFalse(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).canAdventure());
      }
    }

    @Test
    public void canVisitFratHouseAfterWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED),
              withProperty("sideDefeated", "hippies"));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE).canAdventure());
        // We check only quest status, not available equipment
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).canAdventure());
      }
    }

    @Test
    public void canVisitFratHouseInDisguiseAfterWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED),
              withProperty("sideDefeated", "hippies"),
              withEquipped(EquipmentManager.HAT, "Orcish baseball cap"),
              withEquipped(EquipmentManager.PANTS, "Orcish cargo shorts"),
              withEquipped(EquipmentManager.WEAPON, "Orcish frat-paddle"));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE).canAdventure());
        // We check only quest status, not available equipment
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).canAdventure());
      }
    }

    @Test
    public void canVisitBombedFratHouseAfterLostWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED),
              withProperty("sideDefeated", "fratboys"));
      try (cleanups) {
        assertFalse(zones.get(AdventurePool.FRAT_HOUSE).canAdventure());
        assertFalse(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).canAdventure());
        assertTrue(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).canAdventure());
      }
    }

    @Test
    public void canVisitBombedFratHouseAfterWossname() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED),
              withProperty("sideDefeated", "both"));
      try (cleanups) {
        assertFalse(zones.get(AdventurePool.FRAT_HOUSE).canAdventure());
        assertFalse(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).canAdventure());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).canAdventure());
        assertTrue(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).canAdventure());
      }
    }
  }
}
