package net.sourceforge.kolmafia;

import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class KoLAdventureValidationTest {

  @BeforeAll
  private static void beforeAll() {
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
  private static void afterAll() {
    Preferences.saveSettingsToFile = true;
  }

  @Nested
  class BugbearInvasion {
    @Test
    public void cannotVisitMothershipUnlessBugbearsAreInvading() {
      var cleanups = new Cleanups(withPath(Path.NONE));
      try (cleanups) {
        KoLAdventure area = AdventureDatabase.getAdventureByName("Medbay");
        assertFalse(area.isCurrentlyAccessible());
      }
    }

    @Test
    public void canVisitMothershipUnlessBugbearsAreInvading() {
      var cleanups = new Cleanups(withPath(Path.BUGBEAR_INVASION));
      try (cleanups) {
        KoLAdventure area = AdventureDatabase.getAdventureByName("Medbay");
        assertTrue(area.isCurrentlyAccessible());
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
    private static void beforeAll() {
      zones.put(AdventurePool.HIPPY_CAMP, AdventureDatabase.getAdventureByName("Hippy Camp"));
      zones.put(
          AdventurePool.HIPPY_CAMP_DISGUISED,
          AdventureDatabase.getAdventureByName("Hippy Camp (In Disguise)"));
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
    private static void afterAll() {
      zones.clear();
    }

    @Test
    public void cannotVisitHippyCampWithoutIslandAccess() {
      assertFalse(zones.get(AdventurePool.HIPPY_CAMP).isCurrentlyAccessible());
      assertFalse(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
      assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).isCurrentlyAccessible());
      assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
      assertFalse(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).isCurrentlyAccessible());
    }

    @Test
    public void canVisitHippyCampBeforeWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP).isCurrentlyAccessible());
        // We check only quest status, not available equipment
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).isCurrentlyAccessible());
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
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP).isCurrentlyAccessible());
        // We check only quest status, not available equipment
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).isCurrentlyAccessible());
      }
    }

    @Test
    public void canVisitHippyCampOnVergeOfWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED));
      try (cleanups) {
        // KoL does not require going directly to verge-od-war zones
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP).isCurrentlyAccessible());
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        // ... but it allows it.
        assertTrue(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).isCurrentlyAccessible());
        assertTrue(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).isCurrentlyAccessible());
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
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP).isCurrentlyAccessible());
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        // ... but it allows it.
        assertTrue(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).isCurrentlyAccessible());
        assertTrue(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).isCurrentlyAccessible());
      }
    }

    @Test
    public void cannotVisitHippyCampDuringWar() {
      var cleanups =
          new Cleanups(withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, "step1"));
      try (cleanups) {
        assertFalse(zones.get(AdventurePool.HIPPY_CAMP).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).isCurrentlyAccessible());
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
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP).isCurrentlyAccessible());
        // We check only quest status, not available equipment
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).isCurrentlyAccessible());
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
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP).isCurrentlyAccessible());
        // We check only quest status, not available equipment
        assertTrue(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).isCurrentlyAccessible());
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
        assertFalse(zones.get(AdventurePool.HIPPY_CAMP).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        assertTrue(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).isCurrentlyAccessible());
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
        assertFalse(zones.get(AdventurePool.HIPPY_CAMP).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED).isCurrentlyAccessible());
        assertTrue(zones.get(AdventurePool.BOMBED_HIPPY_CAMP).isCurrentlyAccessible());
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
    private static void beforeAll() {
      zones.put(AdventurePool.FRAT_HOUSE, AdventureDatabase.getAdventureByName("Frat House"));
      zones.put(
          AdventurePool.FRAT_HOUSE_DISGUISED,
          AdventureDatabase.getAdventureByName("Frat House (In Disguise)"));
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
    private static void afterAll() {
      zones.clear();
    }

    @Test
    public void cannotVisitFratHouseWithoutIslandAccess() {
      assertFalse(zones.get(AdventurePool.FRAT_HOUSE).isCurrentlyAccessible());
      assertFalse(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
      assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).isCurrentlyAccessible());
      assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
      assertFalse(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).isCurrentlyAccessible());
    }

    @Test
    public void canVisitFratHouseBeforeWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE).isCurrentlyAccessible());
        // We check only quest status, not available equipment
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).isCurrentlyAccessible());
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
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE).isCurrentlyAccessible());
        // We check only quest status, not available equipment
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).isCurrentlyAccessible());
      }
    }

    @Test
    public void canVisitFratHouseOnVergeOfWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED));
      try (cleanups) {
        // KoL does not require going directly to verge-od-war zones
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE).isCurrentlyAccessible());
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        // ... but it allows it.
        assertTrue(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).isCurrentlyAccessible());
        assertTrue(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).isCurrentlyAccessible());
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
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE).isCurrentlyAccessible());
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        // ... but it allows it.
        assertTrue(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).isCurrentlyAccessible());
        assertTrue(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).isCurrentlyAccessible());
      }
    }

    @Test
    public void cannotVisitFratHouseDuringWar() {
      var cleanups =
          new Cleanups(withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, "step1"));
      try (cleanups) {
        assertFalse(zones.get(AdventurePool.FRAT_HOUSE).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).isCurrentlyAccessible());
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
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE).isCurrentlyAccessible());
        // We check only quest status, not available equipment
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).isCurrentlyAccessible());
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
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE).isCurrentlyAccessible());
        // We check only quest status, not available equipment
        assertTrue(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).isCurrentlyAccessible());
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
        assertFalse(zones.get(AdventurePool.FRAT_HOUSE).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        assertTrue(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).isCurrentlyAccessible());
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
        assertFalse(zones.get(AdventurePool.FRAT_HOUSE).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE).isCurrentlyAccessible());
        assertFalse(zones.get(AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED).isCurrentlyAccessible());
        assertTrue(zones.get(AdventurePool.BOMBED_FRAT_HOUSE).isCurrentlyAccessible());
      }
    }
  }
}
