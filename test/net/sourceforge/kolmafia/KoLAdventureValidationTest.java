package net.sourceforge.kolmafia;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.HttpClientWrapper.setupFakeClient;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withAscensions;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLevel;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.helpers.Player.withRestricted;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

import internal.helpers.Cleanups;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
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
import org.junitpioneer.jupiter.cartesian.CartesianTest;

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
    private static final Map<Integer, KoLAdventure> sleazeZones = new HashMap<>();
    private static final Map<Integer, KoLAdventure> spookyZones = new HashMap<>();
    private static final Map<Integer, KoLAdventure> stenchZones = new HashMap<>();
    private static final Map<Integer, KoLAdventure> hotZones = new HashMap<>();
    private static final Map<Integer, KoLAdventure> coldZones = new HashMap<>();

    static {
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
  class Cola {
    private static final KoLAdventure COLA_NONE =
        AdventureDatabase.getAdventureByName("Battlefield (No Uniform)");
    private static final KoLAdventure COLA_CLOACA =
        AdventureDatabase.getAdventureByName("Battlefield (Cloaca Uniform)");
    private static final KoLAdventure COLA_DYSPEPSI =
        AdventureDatabase.getAdventureByName("Battlefield (Dyspepsi Uniform)");

    @Test
    public void mustMeetZonePrerequesites() {
      var cleanups =
          new Cleanups(withAscensions(1), withLevel(4), withQuestProgress(Quest.EGO, "step1"));
      try (cleanups) {
        assertTrue(COLA_NONE.canAdventure());
        assertFalse(COLA_CLOACA.canAdventure());
        assertFalse(COLA_DYSPEPSI.canAdventure());
      }
    }

    @Test
    public void mustHaveAscended() {
      var cleanups =
          new Cleanups(withAscensions(0), withLevel(4), withQuestProgress(Quest.EGO, "step1"));
      try (cleanups) {
        assertFalse(COLA_NONE.canAdventure());
      }
    }

    @Test
    public void mustBeAtLeastLevel4() {
      var cleanups =
          new Cleanups(withAscensions(1), withLevel(3), withQuestProgress(Quest.EGO, "step1"));
      try (cleanups) {
        assertFalse(COLA_NONE.canAdventure());
      }
    }

    @Test
    public void mustBeNoMoreThanLevel5() {
      var cleanups =
          new Cleanups(withAscensions(1), withLevel(6), withQuestProgress(Quest.EGO, "step1"));
      try (cleanups) {
        assertFalse(COLA_NONE.canAdventure());
      }
    }

    @Test
    public void mustHaveRecoveredKey() {
      var cleanups =
          new Cleanups(
              withAscensions(1), withLevel(4), withQuestProgress(Quest.EGO, QuestDatabase.STARTED));
      try (cleanups) {
        assertFalse(COLA_NONE.canAdventure());
      }
    }

    @Test
    public void canAdventureWithCloacaUniformEquipped() {
      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquipped(EquipmentManager.HAT, ItemPool.CLOACA_HELMET),
              withEquipped(EquipmentManager.OFFHAND, ItemPool.CLOACA_SHIELD),
              withEquipped(EquipmentManager.PANTS, ItemPool.CLOACA_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_NONE.canAdventure());
        assertTrue(COLA_CLOACA.canAdventure());
        assertFalse(COLA_DYSPEPSI.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureCloacaEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquipped(EquipmentManager.HAT, ItemPool.CLOACA_HELMET),
              withEquipped(EquipmentManager.OFFHAND, ItemPool.CLOACA_SHIELD),
              withEquipped(EquipmentManager.PANTS, ItemPool.CLOACA_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_CLOACA.canAdventure());
        assertTrue(COLA_CLOACA.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canAdventureWithCloacaUniformAvailable() {
      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquippableItem(ItemPool.CLOACA_HELMET),
              withEquippableItem(ItemPool.CLOACA_SHIELD),
              withEquippableItem(ItemPool.CLOACA_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_NONE.canAdventure());
        assertTrue(COLA_CLOACA.canAdventure());
        assertFalse(COLA_DYSPEPSI.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureCloacaAvailable() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquippableItem(ItemPool.CLOACA_HELMET),
              withEquippableItem(ItemPool.CLOACA_SHIELD),
              withEquippableItem(ItemPool.CLOACA_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_CLOACA.canAdventure());
        assertTrue(COLA_CLOACA.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.CLOACA_UNIFORM + "&ajax=1");
      }
    }

    @Test
    public void canAdventureWithDyspepsiUniformEquipped() {
      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquipped(EquipmentManager.HAT, ItemPool.DYSPEPSI_HELMET),
              withEquipped(EquipmentManager.OFFHAND, ItemPool.DYSPEPSI_SHIELD),
              withEquipped(EquipmentManager.PANTS, ItemPool.DYSPEPSI_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_NONE.canAdventure());
        assertFalse(COLA_CLOACA.canAdventure());
        assertTrue(COLA_DYSPEPSI.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureDyspepsiEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquipped(EquipmentManager.HAT, ItemPool.DYSPEPSI_HELMET),
              withEquipped(EquipmentManager.OFFHAND, ItemPool.DYSPEPSI_SHIELD),
              withEquipped(EquipmentManager.PANTS, ItemPool.DYSPEPSI_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_DYSPEPSI.canAdventure());
        assertTrue(COLA_DYSPEPSI.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canAdventureWithDyspepsiUniformAvailable() {
      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquippableItem(ItemPool.DYSPEPSI_HELMET),
              withEquippableItem(ItemPool.DYSPEPSI_SHIELD),
              withEquippableItem(ItemPool.DYSPEPSI_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_NONE.canAdventure());
        assertFalse(COLA_CLOACA.canAdventure());
        assertTrue(COLA_DYSPEPSI.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureDyspepsiAvailable() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquippableItem(ItemPool.DYSPEPSI_HELMET),
              withEquippableItem(ItemPool.DYSPEPSI_SHIELD),
              withEquippableItem(ItemPool.DYSPEPSI_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_DYSPEPSI.canAdventure());
        assertTrue(COLA_DYSPEPSI.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.DYSPEPSI_UNIFORM + "&ajax=1");
      }
    }

    @Test
    public void canPrepareForAdventureWithNoUniform() {
      setupFakeClient();

      var cleanups =
          new Cleanups(withAscensions(1), withLevel(4), withQuestProgress(Quest.EGO, "step1"));
      try (cleanups) {
        assertTrue(COLA_NONE.canAdventure());
        assertTrue(COLA_NONE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canPrepareForAdventureAndRemoveCloacaUniform() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquipped(EquipmentManager.HAT, ItemPool.CLOACA_HELMET),
              withEquipped(EquipmentManager.OFFHAND, ItemPool.CLOACA_SHIELD),
              withEquipped(EquipmentManager.PANTS, ItemPool.CLOACA_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_NONE.canAdventure());
        assertTrue(COLA_NONE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=unequip&type=offhand");
      }
    }

    @Test
    public void canPrepareForAdventureAndRemoveDyspepsiUniform() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquipped(EquipmentManager.HAT, ItemPool.DYSPEPSI_HELMET),
              withEquipped(EquipmentManager.OFFHAND, ItemPool.DYSPEPSI_SHIELD),
              withEquipped(EquipmentManager.PANTS, ItemPool.DYSPEPSI_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_NONE.canAdventure());
        assertTrue(COLA_NONE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=unequip&type=offhand");
      }
    }
  }

  @Nested
  class DwarfFactory {
    private static final KoLAdventure WAREHOUSE =
        AdventureDatabase.getAdventureByName("Dwarven Factory Warehouse");
    private static final KoLAdventure OFFICE =
        AdventureDatabase.getAdventureByName("The Mine Foremens' Office");

    @Test
    public void mustHaveStartedQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.FACTORY, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertFalse(WAREHOUSE.canAdventure());
        assertFalse(OFFICE.canAdventure());
      }
    }

    @Test
    public void mustHaveOutfit() {
      var cleanups = new Cleanups(withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED));
      try (cleanups) {
        assertFalse(WAREHOUSE.canAdventure());
        assertFalse(OFFICE.canAdventure());
      }
    }

    @Test
    public void canAdventureWithMiningOutfitEquipped() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED),
              withEquipped(EquipmentManager.HAT, "miner's helmet"),
              withEquipped(EquipmentManager.WEAPON, "7-Foot Dwarven mattock"),
              withEquipped(EquipmentManager.PANTS, "miner's pants"));
      try (cleanups) {
        assertTrue(WAREHOUSE.canAdventure());
        assertTrue(OFFICE.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureWithMiningOutfitEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED),
              withEquipped(EquipmentManager.HAT, "miner's helmet"),
              withEquipped(EquipmentManager.WEAPON, "7-Foot Dwarven mattock"),
              withEquipped(EquipmentManager.PANTS, "miner's pants"));
      try (cleanups) {
        assertTrue(WAREHOUSE.canAdventure());
        assertTrue(WAREHOUSE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canAdventureWithMiningOutfitInInventory() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED),
              withEquippableItem("miner's helmet"),
              withEquippableItem("7-Foot Dwarven mattock"),
              withEquippableItem("miner's pants"));
      try (cleanups) {
        assertTrue(WAREHOUSE.canAdventure());
        assertTrue(OFFICE.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureWithMiningOutfitInInventory() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED),
              withEquippableItem("miner's helmet"),
              withEquippableItem("7-Foot Dwarven mattock"),
              withEquippableItem("miner's pants"));
      try (cleanups) {
        assertTrue(WAREHOUSE.canAdventure());
        assertTrue(WAREHOUSE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.MINING_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canAdventureWithDwarvishUniformEquipped() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED),
              withEquipped(EquipmentManager.HAT, "dwarvish war helmet"),
              withEquipped(EquipmentManager.WEAPON, "dwarvish war mattock"),
              withEquipped(EquipmentManager.PANTS, "dwarvish war kilt"));
      try (cleanups) {
        assertTrue(WAREHOUSE.canAdventure());
        assertTrue(OFFICE.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureWithDwarvishUniformEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED),
              withEquipped(EquipmentManager.HAT, "dwarvish war helmet"),
              withEquipped(EquipmentManager.WEAPON, "dwarvish war mattock"),
              withEquipped(EquipmentManager.PANTS, "dwarvish war kilt"));
      try (cleanups) {
        assertTrue(WAREHOUSE.canAdventure());
        assertTrue(WAREHOUSE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canAdventureWithDwarvishUniformInInventory() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED),
              withEquippableItem("dwarvish war helmet"),
              withEquippableItem("dwarvish war mattock"),
              withEquippableItem("dwarvish war kilt"));
      try (cleanups) {
        assertTrue(WAREHOUSE.canAdventure());
        assertTrue(OFFICE.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureWithDwarvishUniformInInventory() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED),
              withEquippableItem("dwarvish war helmet"),
              withEquippableItem("dwarvish war mattock"),
              withEquippableItem("dwarvish war kilt"));
      try (cleanups) {
        assertTrue(WAREHOUSE.canAdventure());
        assertTrue(WAREHOUSE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.DWARVISH_UNIFORM + "&ajax=1");
      }
    }
  }

  @Nested
  class CobbsKnob {

    private static final KoLAdventure OUTSKIRTS_OF_THE_KNOB =
        AdventureDatabase.getAdventureByName("The Outskirts of Cobb's Knob");
    private static final KoLAdventure COBB_BARRACKS =
        AdventureDatabase.getAdventureByName("Cobb's Knob Barracks");
    private static final KoLAdventure COBB_KITCHEN =
        AdventureDatabase.getAdventureByName("Cobb's Knob Kitchens");
    private static final KoLAdventure COBB_HAREM =
        AdventureDatabase.getAdventureByName("Cobb's Knob Harem");
    private static final KoLAdventure COBB_TREASURY =
        AdventureDatabase.getAdventureByName("Cobb's Knob Treasury");
    private static final KoLAdventure COBB_LABORATORY =
        AdventureDatabase.getAdventureByName("Cobb's Knob Laboratory");
    private static final KoLAdventure KNOB_SHAFT =
        AdventureDatabase.getAdventureByName("The Knob Shaft");
    private static final KoLAdventure MENAGERIE_LEVEL_1 =
        AdventureDatabase.getAdventureByName("Cobb's Knob Menagerie, Level 1");
    private static final KoLAdventure MENAGERIE_LEVEL_2 =
        AdventureDatabase.getAdventureByName("Cobb's Knob Menagerie, Level 2");
    private static final KoLAdventure MENAGERIE_LEVEL_3 =
        AdventureDatabase.getAdventureByName("Cobb's Knob Menagerie, Level 3");
    private static final KoLAdventure THRONE_ROOM =
        AdventureDatabase.getAdventureByName("Throne Room");

    @Test
    public void canVisitCobbsKnobBeforeQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertTrue(OUTSKIRTS_OF_THE_KNOB.canAdventure());
        assertFalse(COBB_BARRACKS.canAdventure());
        assertFalse(COBB_KITCHEN.canAdventure());
        assertFalse(COBB_HAREM.canAdventure());
        assertFalse(COBB_TREASURY.canAdventure());
        assertFalse(THRONE_ROOM.canAdventure());
      }
    }

    @Test
    public void canVisitCobbsKnobBeforeDecrypting() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(OUTSKIRTS_OF_THE_KNOB.canAdventure());
        assertFalse(COBB_BARRACKS.canAdventure());
        assertFalse(COBB_KITCHEN.canAdventure());
        assertFalse(COBB_HAREM.canAdventure());
        assertFalse(COBB_TREASURY.canAdventure());
        assertFalse(THRONE_ROOM.canAdventure());
      }
    }

    @Test
    public void canVisitCobbsKnobAfterDecrypting() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, "step1"));
      try (cleanups) {
        assertTrue(OUTSKIRTS_OF_THE_KNOB.canAdventure());
        assertTrue(COBB_BARRACKS.canAdventure());
        assertTrue(COBB_KITCHEN.canAdventure());
        assertTrue(COBB_HAREM.canAdventure());
        assertTrue(COBB_TREASURY.canAdventure());
        assertTrue(THRONE_ROOM.canAdventure());
      }
    }

    @Test
    public void canVisitCobbsKnobAfterDefeatingKing() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, QuestDatabase.FINISHED));
      try (cleanups) {
        assertTrue(OUTSKIRTS_OF_THE_KNOB.canAdventure());
        assertTrue(COBB_BARRACKS.canAdventure());
        assertTrue(COBB_KITCHEN.canAdventure());
        assertTrue(COBB_HAREM.canAdventure());
        assertTrue(COBB_TREASURY.canAdventure());
        assertFalse(THRONE_ROOM.canAdventure());
      }
    }

    @Test
    public void cannotVisitCobbsKnobLaboratoryWithoutKey() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, "step1"));
      try (cleanups) {
        assertFalse(COBB_LABORATORY.canAdventure());
        assertFalse(KNOB_SHAFT.canAdventure());
      }
    }

    @Test
    public void canVisitCobbsKnobLaboratoryWithKey() {
      var cleanups =
          new Cleanups(withItem("Cobb's Knob lab key"), withQuestProgress(Quest.GOBLIN, "step1"));
      try (cleanups) {
        assertTrue(COBB_LABORATORY.canAdventure());
        assertTrue(KNOB_SHAFT.canAdventure());
      }
    }

    @Test
    public void cannotVisitCobbsKnobMenagerieWithoutKey() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, "step1"));
      try (cleanups) {
        assertFalse(MENAGERIE_LEVEL_1.canAdventure());
        assertFalse(MENAGERIE_LEVEL_2.canAdventure());
        assertFalse(MENAGERIE_LEVEL_3.canAdventure());
      }
    }

    @Test
    public void canVisitCobbsKnobMenagerieWithKey() {
      var cleanups =
          new Cleanups(
              withItem("Cobb's Knob Menagerie key"), withQuestProgress(Quest.GOBLIN, "step1"));
      try (cleanups) {
        assertTrue(MENAGERIE_LEVEL_1.canAdventure());
        assertTrue(MENAGERIE_LEVEL_2.canAdventure());
        assertTrue(MENAGERIE_LEVEL_3.canAdventure());
      }
    }
  }

  @Nested
  class Cyrpt {

    private static final KoLAdventure DEFILED_ALCOVE =
        AdventureDatabase.getAdventureByName("The Defiled Alcove");
    private static final KoLAdventure DEFILED_CRANNY =
        AdventureDatabase.getAdventureByName("The Defiled Cranny");
    private static final KoLAdventure DEFILED_NICHE =
        AdventureDatabase.getAdventureByName("The Defiled Niche");
    private static final KoLAdventure DEFILED_NOOK =
        AdventureDatabase.getAdventureByName("The Defiled Nook");
    private static final KoLAdventure HAERT =
        AdventureDatabase.getAdventureByName("Haert of the Cyrpt");

    @Test
    public void cannotVisitCyrptBeforeQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.CYRPT, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertFalse(DEFILED_ALCOVE.canAdventure());
        assertFalse(DEFILED_CRANNY.canAdventure());
        assertFalse(DEFILED_NICHE.canAdventure());
        assertFalse(DEFILED_NOOK.canAdventure());
        assertFalse(HAERT.canAdventure());
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
        assertTrue(DEFILED_ALCOVE.canAdventure());
        assertTrue(DEFILED_CRANNY.canAdventure());
        assertTrue(DEFILED_NICHE.canAdventure());
        assertTrue(DEFILED_NOOK.canAdventure());
        assertFalse(HAERT.canAdventure());
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
        assertFalse(DEFILED_ALCOVE.canAdventure());
        assertTrue(DEFILED_CRANNY.canAdventure());
        assertTrue(DEFILED_NICHE.canAdventure());
        assertTrue(DEFILED_NOOK.canAdventure());
        assertFalse(HAERT.canAdventure());
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
        assertTrue(DEFILED_ALCOVE.canAdventure());
        assertFalse(DEFILED_CRANNY.canAdventure());
        assertTrue(DEFILED_NICHE.canAdventure());
        assertTrue(DEFILED_NOOK.canAdventure());
        assertFalse(HAERT.canAdventure());
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
        assertTrue(DEFILED_ALCOVE.canAdventure());
        assertTrue(DEFILED_CRANNY.canAdventure());
        assertFalse(DEFILED_NICHE.canAdventure());
        assertTrue(DEFILED_NOOK.canAdventure());
        assertFalse(HAERT.canAdventure());
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
        assertTrue(DEFILED_ALCOVE.canAdventure());
        assertTrue(DEFILED_CRANNY.canAdventure());
        assertTrue(DEFILED_NICHE.canAdventure());
        assertFalse(DEFILED_NOOK.canAdventure());
        assertFalse(HAERT.canAdventure());
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
        assertFalse(DEFILED_ALCOVE.canAdventure());
        assertFalse(DEFILED_CRANNY.canAdventure());
        assertFalse(DEFILED_NICHE.canAdventure());
        assertFalse(DEFILED_NOOK.canAdventure());
        assertTrue(HAERT.canAdventure());
      }
    }

    @Test
    public void cannotVisitCyrptWhenQuestFinished() {
      var cleanups = new Cleanups(withQuestProgress(Quest.CYRPT, QuestDatabase.FINISHED));
      try (cleanups) {
        assertFalse(DEFILED_ALCOVE.canAdventure());
        assertFalse(DEFILED_CRANNY.canAdventure());
        assertFalse(DEFILED_NICHE.canAdventure());
        assertFalse(DEFILED_NOOK.canAdventure());
        assertFalse(HAERT.canAdventure());
      }
    }
  }

  @Nested
  class Pirate {

    private static final KoLAdventure PIRATE_COVE =
        AdventureDatabase.getAdventureByName("The Obligatory Pirate's Cove");
    private static final KoLAdventure BARRRNEYS_BARRR =
        AdventureDatabase.getAdventureByName("Barrrney's Barrr");
    private static final KoLAdventure FCLE = AdventureDatabase.getAdventureByName("The F'c'le");
    private static final KoLAdventure POOP_DECK =
        AdventureDatabase.getAdventureByName("The Poop Deck");
    private static final KoLAdventure BELOWDECKS =
        AdventureDatabase.getAdventureByName("Belowdecks");

    @Test
    public void cannotVisitPiratesWithoutIslandAccess() {
      var cleanups = new Cleanups(withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertFalse(PIRATE_COVE.canAdventure());
        assertFalse(BARRRNEYS_BARRR.canAdventure());
        assertFalse(FCLE.canAdventure());
        assertFalse(POOP_DECK.canAdventure());
        assertFalse(BELOWDECKS.canAdventure());
      }
    }

    @Test
    public void canVisitPiratesUndisguised() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertTrue(PIRATE_COVE.canAdventure());
        assertFalse(BARRRNEYS_BARRR.canAdventure());
        assertFalse(FCLE.canAdventure());
        assertFalse(POOP_DECK.canAdventure());
        assertFalse(BELOWDECKS.canAdventure());
      }
    }

    @Test
    public void cannotVisitPiratesDuringWar() {
      var cleanups =
          new Cleanups(withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, "step1"));
      try (cleanups) {
        assertFalse(PIRATE_COVE.canAdventure());
        assertFalse(BARRRNEYS_BARRR.canAdventure());
        assertFalse(FCLE.canAdventure());
        assertFalse(POOP_DECK.canAdventure());
        assertFalse(BELOWDECKS.canAdventure());
      }
    }

    @Test
    public void canVisitPirateShipDisguised() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquippableItem("eyepatch"),
              withEquippableItem("swashbuckling pants"),
              withEquippableItem("stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, QuestDatabase.FINISHED));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP));
        assertTrue(BARRRNEYS_BARRR.canAdventure());
        assertTrue(FCLE.canAdventure());
        assertTrue(POOP_DECK.canAdventure());
        assertTrue(BELOWDECKS.canAdventure());
      }
    }

    @Test
    public void canVisitPirateShipFledged() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquippableItem("pirate fledges"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, QuestDatabase.FINISHED));
      try (cleanups) {
        assertTrue(BARRRNEYS_BARRR.canAdventure());
        assertTrue(FCLE.canAdventure());
        assertTrue(POOP_DECK.canAdventure());
        assertTrue(BELOWDECKS.canAdventure());
      }
    }

    @Test
    public void canVisitPirateShipBeforeQuest() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquippableItem("eyepatch"),
              withEquippableItem("swashbuckling pants"),
              withEquippableItem("stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP));
        assertTrue(BARRRNEYS_BARRR.canAdventure());
        assertFalse(FCLE.canAdventure());
        assertFalse(POOP_DECK.canAdventure());
        assertFalse(BELOWDECKS.canAdventure());
      }
    }

    @Test
    public void canVisitPirateShipFcleDuringQuest() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquippableItem("eyepatch"),
              withEquippableItem("swashbuckling pants"),
              withEquippableItem("stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, "step5"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP));
        assertTrue(BARRRNEYS_BARRR.canAdventure());
        assertTrue(FCLE.canAdventure());
        assertFalse(POOP_DECK.canAdventure());
        assertFalse(BELOWDECKS.canAdventure());
      }
    }

    @Test
    public void canVisitPirateShipPoopDeckDuringQuest() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquippableItem("eyepatch"),
              withEquippableItem("swashbuckling pants"),
              withEquippableItem("stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, "step6"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP));
        assertTrue(BARRRNEYS_BARRR.canAdventure());
        assertTrue(FCLE.canAdventure());
        assertTrue(POOP_DECK.canAdventure());
        assertFalse(BELOWDECKS.canAdventure());
      }
    }

    @Test
    public void canVisitPirateShipBelowdecksAfterQuest() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquippableItem("eyepatch"),
              withEquippableItem("swashbuckling pants"),
              withEquippableItem("stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, QuestDatabase.FINISHED));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP));
        assertTrue(BARRRNEYS_BARRR.canAdventure());
        assertTrue(FCLE.canAdventure());
        assertTrue(POOP_DECK.canAdventure());
        assertTrue(BELOWDECKS.canAdventure());
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

    private static final KoLAdventure HIPPY_CAMP =
        AdventureDatabase.getAdventureByName("Hippy Camp");
    private static final KoLAdventure HIPPY_CAMP_DISGUISED =
        AdventureDatabase.getAdventureByName("Hippy Camp (Hippy Disguise)");
    private static final KoLAdventure WARTIME_HIPPY_CAMP =
        AdventureDatabase.getAdventureByName("Wartime Hippy Camp");
    private static final KoLAdventure WARTIME_HIPPY_CAMP_DISGUISED =
        AdventureDatabase.getAdventureByName("Wartime Hippy Camp (Frat Disguise)");
    private static final KoLAdventure BOMBED_HIPPY_CAMP =
        AdventureDatabase.getAdventureByName("The Hippy Camp (Bombed Back to the Stone Age)");

    @Test
    public void cannotVisitHippyCampWithoutIslandAccess() {
      assertFalse(HIPPY_CAMP.canAdventure());
      assertFalse(HIPPY_CAMP_DISGUISED.canAdventure());
      assertFalse(WARTIME_HIPPY_CAMP.canAdventure());
      assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
      assertFalse(BOMBED_HIPPY_CAMP.canAdventure());
    }

    @Test
    public void canVisitHippyCampBeforeWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertTrue(HIPPY_CAMP.canAdventure());
        assertFalse(HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(BOMBED_HIPPY_CAMP.canAdventure());
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
        assertTrue(HIPPY_CAMP.canAdventure());
        // We check only quest status, not available equipment
        assertTrue(HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(BOMBED_HIPPY_CAMP.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureDisguisedEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withEquipped(EquipmentManager.HAT, "filthy knitted dread sack"),
              withEquipped(EquipmentManager.PANTS, "filthy corduroys"));
      try (cleanups) {
        assertTrue(HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(HIPPY_CAMP_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canPrepareForAdventureDisguisedUnEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withEquippableItem("filthy knitted dread sack"),
              withEquippableItem("filthy corduroys"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.HIPPY_OUTFIT));
        assertTrue(HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(HIPPY_CAMP_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.HIPPY_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canPickHippyOutfitToAdventureBeforeWar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              // Have War Hippy Fatigues
              withItem("reinforced beaded headband"),
              withItem("bullet-proof corduroys"),
              withItem("round purple sunglasses"),
              // Filthy Hippy Disguise
              withEquippableItem("filthy knitted dread sack"),
              withEquippableItem("filthy corduroys"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.HIPPY_OUTFIT));
        assertFalse(EquipmentManager.hasOutfit(OutfitPool.WAR_HIPPY_OUTFIT));
        assertTrue(HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(HIPPY_CAMP_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.HIPPY_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canPickWarHippyOutfitToAdventureBeforeWar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              // Have War Hippy Fatigues
              withEquippableItem("reinforced beaded headband"),
              withEquippableItem("bullet-proof corduroys"),
              withEquippableItem("round purple sunglasses"),
              // Filthy Hippy Disguise
              withEquippableItem("filthy knitted dread sack"),
              withEquippableItem("filthy corduroys"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.HIPPY_OUTFIT));
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.WAR_HIPPY_OUTFIT));
        assertTrue(HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(HIPPY_CAMP_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.WAR_HIPPY_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canVisitHippyCampOnVergeOfWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED));
      try (cleanups) {
        // KoL does not require going directly to verge-of-war zones
        assertTrue(HIPPY_CAMP.canAdventure());
        assertFalse(HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(WARTIME_HIPPY_CAMP.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(BOMBED_HIPPY_CAMP.canAdventure());
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
        // KoL does not require going directly to verge-of-war zones
        assertTrue(HIPPY_CAMP.canAdventure());
        assertTrue(HIPPY_CAMP_DISGUISED.canAdventure());
        // ... but it allows it.
        assertTrue(WARTIME_HIPPY_CAMP.canAdventure());
        assertTrue(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(BOMBED_HIPPY_CAMP.canAdventure());
      }
    }

    @Test
    public void canPickFratOutfitToAdventureOnVergeOfWar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED),
              // War Frat Fatigues
              withItem("beer helmet"),
              withItem("distressed denim pants"),
              withItem("bejeweled pledge pin"),
              // Frat Boy Ensemble
              withEquippableItem("Orcish baseball cap"),
              withEquippableItem("Orcish cargo shorts"),
              withEquippableItem("Orcish frat-paddle"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.FRAT_OUTFIT));
        assertFalse(EquipmentManager.hasOutfit(OutfitPool.WAR_FRAT_OUTFIT));
        assertTrue(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(WARTIME_HIPPY_CAMP_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.FRAT_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canPickWarFratOutfitToAdventureOnVergeOfWar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED),
              // War Frat Fatigues
              withEquippableItem("beer helmet"),
              withEquippableItem("distressed denim pants"),
              withEquippableItem("bejeweled pledge pin"),
              // Frat Boy Ensemble
              withEquippableItem("Orcish baseball cap"),
              withEquippableItem("Orcish cargo shorts"),
              withEquippableItem("Orcish frat-paddle"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.FRAT_OUTFIT));
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.WAR_FRAT_OUTFIT));
        assertTrue(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(WARTIME_HIPPY_CAMP_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.WAR_FRAT_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void cannotVisitHippyCampDuringWar() {
      var cleanups =
          new Cleanups(withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, "step1"));
      try (cleanups) {
        assertFalse(HIPPY_CAMP.canAdventure());
        assertFalse(HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(BOMBED_HIPPY_CAMP.canAdventure());
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
        assertTrue(HIPPY_CAMP.canAdventure());
        assertFalse(HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(BOMBED_HIPPY_CAMP.canAdventure());
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
        assertTrue(HIPPY_CAMP.canAdventure());
        assertTrue(HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(BOMBED_HIPPY_CAMP.canAdventure());
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
        assertFalse(HIPPY_CAMP.canAdventure());
        assertFalse(HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(BOMBED_HIPPY_CAMP.canAdventure());
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
        assertFalse(HIPPY_CAMP.canAdventure());
        assertFalse(HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(BOMBED_HIPPY_CAMP.canAdventure());
      }
    }

    @Test
    public void changesIntoWarFratOutfitIfNecessary() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquippableItem("beer helmet"),
              withEquippableItem("distressed denim pants"),
              withEquippableItem("bejeweled pledge pin"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED));

      try (cleanups) {
        assertTrue(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(WARTIME_HIPPY_CAMP_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/inv_equip.php", "which=2&action=outfit&whichoutfit=33&ajax=1");
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

    private static final KoLAdventure FRAT_HOUSE =
        AdventureDatabase.getAdventureByName("Frat House");
    private static final KoLAdventure FRAT_HOUSE_DISGUISED =
        AdventureDatabase.getAdventureByName("Frat House (Frat Disguise)");
    private static final KoLAdventure WARTIME_FRAT_HOUSE =
        AdventureDatabase.getAdventureByName("Wartime Frat House");
    private static final KoLAdventure WARTIME_FRAT_HOUSE_DISGUISED =
        AdventureDatabase.getAdventureByName("Wartime Frat House (Hippy Disguise)");
    private static final KoLAdventure BOMBED_FRAT_HOUSE =
        AdventureDatabase.getAdventureByName(
            "The Orcish Frat House (Bombed Back to the Stone Age)");

    @Test
    public void cannotVisitFratHouseWithoutIslandAccess() {
      assertFalse(FRAT_HOUSE.canAdventure());
      assertFalse(FRAT_HOUSE_DISGUISED.canAdventure());
      assertFalse(WARTIME_FRAT_HOUSE.canAdventure());
      assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
      assertFalse(BOMBED_FRAT_HOUSE.canAdventure());
    }

    @Test
    public void canVisitFratHouseBeforeWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertTrue(FRAT_HOUSE.canAdventure());
        assertFalse(FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(BOMBED_FRAT_HOUSE.canAdventure());
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
        assertTrue(FRAT_HOUSE.canAdventure());
        assertTrue(FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(BOMBED_FRAT_HOUSE.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureDisguisedEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withEquipped(EquipmentManager.HAT, "Orcish baseball cap"),
              withEquipped(EquipmentManager.PANTS, "Orcish cargo shorts"),
              withEquipped(EquipmentManager.WEAPON, "Orcish frat-paddle"));
      try (cleanups) {
        assertTrue(FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(FRAT_HOUSE_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canPrepareForAdventureDisguisedUnEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withEquippableItem("Orcish baseball cap"),
              withEquippableItem("Orcish cargo shorts"),
              withEquippableItem("Orcish frat-paddle"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.FRAT_OUTFIT));
        assertTrue(FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(FRAT_HOUSE_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.FRAT_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canPickFratOutfitToAdventureBeforeWar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              // War Frat Fatigues
              withItem("beer helmet"),
              withItem("distressed denim pants"),
              withItem("bejeweled pledge pin"),
              // Frat Boy Ensemble
              withEquippableItem("Orcish baseball cap"),
              withEquippableItem("Orcish cargo shorts"),
              withEquippableItem("Orcish frat-paddle"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.FRAT_OUTFIT));
        assertFalse(EquipmentManager.hasOutfit(OutfitPool.WAR_FRAT_OUTFIT));
        assertTrue(FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(FRAT_HOUSE_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.FRAT_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canPickWarFratOutfitToAdventureBeforeWar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              // War Frat Fatigues
              withEquippableItem("beer helmet"),
              withEquippableItem("distressed denim pants"),
              withEquippableItem("bejeweled pledge pin"),
              // Frat Boy Ensemble
              withEquippableItem("Orcish baseball cap"),
              withEquippableItem("Orcish cargo shorts"),
              withEquippableItem("Orcish frat-paddle"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.FRAT_OUTFIT));
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.WAR_FRAT_OUTFIT));
        assertTrue(FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(FRAT_HOUSE_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.WAR_FRAT_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canVisitFratHouseOnVergeOfWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED));
      try (cleanups) {
        // KoL does not require going directly to verge-of-war zones
        assertTrue(FRAT_HOUSE.canAdventure());
        assertFalse(FRAT_HOUSE_DISGUISED.canAdventure());
        // ... but it allows it.
        assertTrue(WARTIME_FRAT_HOUSE.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(BOMBED_FRAT_HOUSE.canAdventure());
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
        // KoL does not require going directly to verge-of-war zones
        assertTrue(FRAT_HOUSE.canAdventure());
        assertTrue(FRAT_HOUSE_DISGUISED.canAdventure());
        // ... but it allows it.
        assertTrue(WARTIME_FRAT_HOUSE.canAdventure());
        assertTrue(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(BOMBED_FRAT_HOUSE.canAdventure());
      }
    }

    @Test
    public void canPickHippyOutfitToAdventureOnVergeOfWar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED),
              // Have War Hippy Fatigues
              withItem("reinforced beaded headband"),
              withItem("bullet-proof corduroys"),
              withItem("round purple sunglasses"),
              // Filthy Hippy Disguise
              withEquippableItem("filthy knitted dread sack"),
              withEquippableItem("filthy corduroys"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.HIPPY_OUTFIT));
        assertFalse(EquipmentManager.hasOutfit(OutfitPool.WAR_HIPPY_OUTFIT));
        assertTrue(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(WARTIME_FRAT_HOUSE_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.HIPPY_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canPickWarFratOutfitToAdventureOnVergeOfWar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED),
              // Have War Hippy Fatigues
              withEquippableItem("reinforced beaded headband"),
              withEquippableItem("bullet-proof corduroys"),
              withEquippableItem("round purple sunglasses"),
              // Filthy Hippy Disguise
              withEquippableItem("filthy knitted dread sack"),
              withEquippableItem("filthy corduroys"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.HIPPY_OUTFIT));
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.WAR_HIPPY_OUTFIT));
        assertTrue(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(WARTIME_FRAT_HOUSE_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.WAR_HIPPY_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void cannotVisitFratHouseDuringWar() {
      var cleanups =
          new Cleanups(withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, "step1"));
      try (cleanups) {
        assertFalse(FRAT_HOUSE.canAdventure());
        assertFalse(FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(BOMBED_FRAT_HOUSE.canAdventure());
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
        assertTrue(FRAT_HOUSE.canAdventure());
        assertFalse(FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(BOMBED_FRAT_HOUSE.canAdventure());
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
        assertTrue(FRAT_HOUSE.canAdventure());
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.FRAT_OUTFIT));
        assertTrue(FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(BOMBED_FRAT_HOUSE.canAdventure());
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
        assertFalse(FRAT_HOUSE.canAdventure());
        assertFalse(FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(BOMBED_FRAT_HOUSE.canAdventure());
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
        assertFalse(FRAT_HOUSE.canAdventure());
        assertFalse(FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(BOMBED_FRAT_HOUSE.canAdventure());
      }
    }
  }

  @Nested
  class RabbitHole {
    private static final KoLAdventure RABBIT_HOLE =
        AdventureDatabase.getAdventureByName("The Red Queen's Garden");

    @Test
    public void cannotAdventureWithoutEffectOrItem() {
      assertThat(RABBIT_HOLE.canAdventure(), is(false));
    }

    @Test
    public void canAdventureWithEffectActive() {
      var cleanups = new Cleanups(withEffect(EffectPool.DOWN_THE_RABBIT_HOLE));
      try (cleanups) {
        assertThat(RABBIT_HOLE.canAdventure(), is(true));
      }
    }

    @Test
    public void canAdventureWithItemInInventory() {
      var cleanups = new Cleanups(withItem(ItemPool.DRINK_ME_POTION));
      try (cleanups) {
        assertThat(RABBIT_HOLE.canAdventure(), is(true));
      }
    }

    @Test
    public void cannotPrepareForAdventureWithoutItemAndEffect() {
      assertThat(RABBIT_HOLE.prepareForAdventure(), is(false));
    }

    @Test
    public void canPrepareForAdventureWithEffect() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withEffect(EffectPool.DOWN_THE_RABBIT_HOLE), withItem(ItemPool.DRINK_ME_POTION));
      try (cleanups) {
        assertTrue(RABBIT_HOLE.canAdventure());
        assertTrue(RABBIT_HOLE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canPrepareForAdventureWithItem() {
      setupFakeClient();

      var cleanups = new Cleanups(withItem(ItemPool.DRINK_ME_POTION));
      try (cleanups) {
        assertTrue(RABBIT_HOLE.canAdventure());
        assertTrue(RABBIT_HOLE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.DRINK_ME_POTION + "&ajax=1");
      }
    }
  }

  @Nested
  class Spacegate {
    private static final KoLAdventure SPACEGATE =
        AdventureDatabase.getAdventureByName("Through the Spacegate");

    @Test
    void canAdventure() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", true),
              withProperty("_spacegateCoordinates", "XXXXX"),
              withProperty("_spacegateTurnsLeft", 2));

      try (cleanups) {
        assertThat(SPACEGATE.canAdventure(), is(true));
      }
    }

    @Test
    void canAdventureWithDayTicket() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", false),
              withProperty("_spacegateToday", true),
              withProperty("_spacegateCoordinates", "XXXXX"),
              withProperty("_spacegateTurnsLeft", 2));

      try (cleanups) {
        assertThat(SPACEGATE.canAdventure(), is(true));
      }
    }

    @Test
    void cannotAdventureWithoutAccess() {
      var cleanups =
          new Cleanups(
              withProperty("_spacegateCoordinates", "XXXXX"),
              withProperty("_spacegateTurnsLeft", 2));

      try (cleanups) {
        assertThat(SPACEGATE.canAdventure(), is(false));
      }
    }

    @Test
    void cannotAdventureWithoutDialledCoordinates() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", true), withProperty("_spacegateTurnsLeft", 2));

      try (cleanups) {
        assertThat(SPACEGATE.canAdventure(), is(false));
      }
    }

    @Test
    void cannotAdventureWithoutTurnsRemaining() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", true),
              withProperty("_spacegateCoordinates", "XXXXX"),
              withProperty("_spacegateTurnsLeft", 0));

      try (cleanups) {
        assertThat(SPACEGATE.canAdventure(), is(false));
      }
    }

    @Test
    void cannotAdventureInKoE() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", true),
              withProperty("_spacegateCoordinates", "XXXXX"),
              withProperty("_spacegateTurnsLeft", 2),
              withPath(Path.KINGDOM_OF_EXPLOATHING));

      try (cleanups) {
        assertThat(SPACEGATE.canAdventure(), is(false));
      }
    }
  }

  @Nested
  class Orchard {
    private enum Chambers {
      HATCHING("The Hatching Chamber", -1, -1),
      FEEDING(
          "The Feeding Chamber",
          EffectPool.FILTHWORM_LARVA_STENCH,
          ItemPool.FILTHWORM_HATCHLING_GLAND),
      GUARDS(
          "The Royal Guard Chamber",
          EffectPool.FILTHWORM_DRONE_STENCH,
          ItemPool.FILTHWORM_DRONE_GLAND),
      QUEENS(
          "The Filthworm Queen's Chamber",
          EffectPool.FILTHWORM_GUARD_STENCH,
          ItemPool.FILTHWORM_GUARD_GLAND);

      private KoLAdventure adventure;
      private int effectId;
      private int itemId;

      Chambers(final String adventureName, final int effectId, final int itemId) {
        this.adventure = AdventureDatabase.getAdventureByName(adventureName);
        this.effectId = effectId;
        this.itemId = itemId;
      }

      public boolean canAdventure() {
        return adventure.canAdventure();
      }

      public boolean prepareForAdventure() {
        return adventure.prepareForAdventure();
      }

      public int getEffectId() {
        return effectId;
      }

      public int getItemId() {
        return itemId;
      }
    }

    @Test
    void cannotAdventureOutsideWar() {
      var cleanups = new Cleanups(withQuestProgress(Quest.ISLAND_WAR, "unstarted"));

      try (cleanups) {
        assertThat(Chambers.HATCHING.canAdventure(), is(false));
      }
    }

    @Test
    void cannotAdventureWhenQueenIsSlain() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.ISLAND_WAR, "step1"),
              withItem(ItemPool.FILTHWORM_QUEEN_HEART));

      try (cleanups) {
        assertThat(Chambers.HATCHING.canAdventure(), is(false));
      }
    }

    @Test
    void cannotAdventureWhenQueenHeartIsHandedIn() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.ISLAND_WAR, "step1"),
              withProperty("sidequestOrchardCompleted", "fratboy"));

      try (cleanups) {
        assertThat(Chambers.HATCHING.canAdventure(), is(false));
      }
    }

    @CartesianTest
    void canAdventureInChambersWithRightEffect(
        @CartesianTest.Enum Chambers chamber,
        @Values(booleans = {true, false}) final boolean haveEffect) {
      var cleanups = new Cleanups(withQuestProgress(Quest.ISLAND_WAR, "step1"));

      if (haveEffect) cleanups.add(withEffect(chamber.getEffectId()));

      try (cleanups) {
        assertThat(chamber.canAdventure(), is(haveEffect || chamber == Chambers.HATCHING));
      }
    }

    @CartesianTest
    void canAdventureInChambersWithRightGland(
        @CartesianTest.Enum Chambers chamber,
        @Values(booleans = {true, false}) final boolean haveGland) {
      var cleanups = new Cleanups(withQuestProgress(Quest.ISLAND_WAR, "step1"));

      if (haveGland) cleanups.add(withItem(chamber.getItemId()));

      try (cleanups) {
        assertThat(chamber.canAdventure(), is(haveGland || chamber == Chambers.HATCHING));
      }
    }

    private static final KoLAdventure UNKNOWN =
        new KoLAdventure("Orchard", "adventure.php", "696969", "The Chamber of Filthworm Commerce");

    @Test
    void cannotAdventureInUnknownNewOrchardZone() {
      var cleanups = new Cleanups(withQuestProgress(Quest.ISLAND_WAR, "step1"));

      try (cleanups) {
        assertThat(UNKNOWN.canAdventure(), is(false));
      }
    }

    @CartesianTest
    void preparingForChamberUsesGland(
        @CartesianTest.Enum Chambers chamber,
        @Values(booleans = {true, false}) final boolean haveGland,
        @Values(booleans = {true, false}) final boolean haveEffect) {
      setupFakeClient();

      var cleanups = new Cleanups(withQuestProgress(Quest.ISLAND_WAR, "step1"));

      if (haveGland) cleanups.add(withItem(chamber.getItemId()));
      if (haveEffect) cleanups.add(withEffect(chamber.getEffectId()));

      try (cleanups) {
        var success = chamber.prepareForAdventure();
        var requests = getRequests();

        var nothingToDo = chamber == Chambers.HATCHING || haveEffect;

        if (nothingToDo || !haveGland) {
          assertThat(requests, hasSize(0));
        } else {
          assertThat(requests, hasSize(1));
          assertPostRequest(
              requests.get(0), "/inv_use.php", "whichitem=" + chamber.getItemId() + "&ajax=1");
        }

        assertThat(success, is(nothingToDo || haveGland));
      }
    }
  }

  @Nested
  class FantasyRealm {
    private static final KoLAdventure BANDITS =
        AdventureDatabase.getAdventureByName("The Bandit Crossroads");

    private static final KoLAdventure MOUNTAINS =
        AdventureDatabase.getAdventureByName("The Towering Mountains");

    @Test
    void canAdventure() {
      var cleanups =
          new Cleanups(
              withProperty("frAlways", true),
              withProperty("_frHoursLeft", 5),
              withProperty("_frAreasUnlocked", "The Bandit Crossroads,"));

      try (cleanups) {
        assertThat(BANDITS.canAdventure(), is(true));
      }
    }

    @Test
    void canAdventureWithDayPass() {
      var cleanups =
          new Cleanups(
              withProperty("frAlways", false),
              withProperty("_frToday", true),
              withProperty("_frHoursLeft", 5),
              withProperty("_frAreasUnlocked", "The Bandit Crossroads,"));

      try (cleanups) {
        assertThat(BANDITS.canAdventure(), is(true));
      }
    }

    @Test
    void cannotAdventureWithoutAccess() {
      var cleanups =
          new Cleanups(
              withProperty("frAlways", false),
              withProperty("_frToday", false),
              withProperty("_frHoursLeft", 5),
              withProperty("_frAreasUnlocked", "The Bandit Crossroads,"));

      try (cleanups) {
        assertThat(BANDITS.canAdventure(), is(false));
      }
    }

    @Test
    void cannotAdventureWithoutHours() {
      var cleanups =
          new Cleanups(
              withProperty("frAlways", true),
              withProperty("_frToday", false),
              withProperty("_frHoursLeft", 0),
              withProperty("_frAreasUnlocked", "The Bandit Crossroads,"));

      try (cleanups) {
        assertThat(BANDITS.canAdventure(), is(false));
      }
    }

    @Test
    void cannotAdventureWithoutUnlock() {
      var cleanups =
          new Cleanups(
              withProperty("frAlways", true),
              withProperty("_frToday", false),
              withProperty("_frHoursLeft", 5),
              withProperty("_frAreasUnlocked", "The Bandit Crossroads,"));

      try (cleanups) {
        assertThat(MOUNTAINS.canAdventure(), is(false));
      }
    }

    @Test
    void cannotPrepareWithoutGem() {
      var cleanups =
          new Cleanups(
              withProperty("frAlways", true),
              withProperty("_frToday", false),
              withProperty("_frHoursLeft", 5),
              withProperty("_frAreasUnlocked", "The Bandit Crossroads,"));

      try (cleanups) {
        assertThat(BANDITS.prepareForAdventure(), is(false));
      }
    }

    @Test
    void preparingEquipsGem() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withProperty("frAlways", true),
              withProperty("_frToday", false),
              withProperty("_frHoursLeft", 5),
              withProperty("_frAreasUnlocked", "The Bandit Crossroads,"),
              withItem(ItemPool.FANTASY_REALM_GEM));

      try (cleanups) {
        var success = BANDITS.prepareForAdventure();

        var requests = getRequests();

        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/inv_equip.php", "which=2&ajax=1&slot=1&action=equip&whichitem=9837");
        assertThat(success, is(true));
      }
    }

    @Test
    void preparingRemovesFamiliar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withProperty("frAlways", true),
              withProperty("_frToday", false),
              withProperty("_frHoursLeft", 5),
              withProperty("_frAreasUnlocked", "The Bandit Crossroads,"),
              withEquipped(EquipmentManager.ACCESSORY1, ItemPool.FANTASY_REALM_GEM),
              withFamiliar(FamiliarPool.PARROT));

      try (cleanups) {
        var success = BANDITS.prepareForAdventure();

        var requests = getRequests();

        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/familiar.php", "action=putback&ajax=1");
        assertThat(success, is(true));
      }
    }
  }

  @Nested
  class MarketQuests {
    private static final KoLAdventure SKELETON_STORE =
        AdventureDatabase.getAdventureByName("The Skeleton Store");
    private static final KoLAdventure MADNESS_BAKERY =
        AdventureDatabase.getAdventureByName("Madness Bakery");
    private static final KoLAdventure OVERGROWN_LOT =
        AdventureDatabase.getAdventureByName("The Overgrown Lot");

    // For each zone:
    //   If have access, prepareForAdventure immediately returns true
    //   If have no access and have item, prepareForAdventure uses item.
    //   If have no access and don't have item, prepareForAdventure starts quest with NPC

    void withAccessToSkeletonStoreMakesNoRequests() {
      var cleanups = new Cleanups(withProperty("skeletonStoreAvailable", true));
      setupFakeClient();
      try (cleanups) {
        var success = SKELETON_STORE.prepareForAdventure();
        var requests = getRequests();
        assertThat(requests, hasSize(0));
        assertThat(success, is(true));
      }
    }

    void withSkeletonStoreItemUsesItem() {
      var cleanups =
          new Cleanups(
              withProperty("skeletonStoreAvailable", false),
              withItem(ItemPool.BONE_WITH_A_PRICE_TAG));
      setupFakeClient();
      try (cleanups) {
        var success = SKELETON_STORE.prepareForAdventure();
        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_use.php",
            "whichitem=" + ItemPool.BONE_WITH_A_PRICE_TAG + "&ajax=1");
        assertThat(success, is(true));
      }
    }

    void withoutSkeletonStoreItemStartsQuest() {
      var cleanups = new Cleanups(withProperty("skeletonStoreAvailable", false));
      setupFakeClient();
      try (cleanups) {
        var success = SKELETON_STORE.prepareForAdventure();
        var requests = getRequests();
        assertThat(requests, hasSize(3));
        assertPostRequest(requests.get(0), "/shop.php", "whichshop=meatsmith");
        assertPostRequest(requests.get(1), "/shop.php", "whichshop=meatsmith&action=talk");
        assertPostRequest(requests.get(2), "/choice.php", "whichchoice=1059&option=1");
        assertThat(success, is(true));
      }
    }

    void withAccessToMadnessBakeryStoreMakesNoRequests() {
      var cleanups = new Cleanups(withProperty("madnessBakeryAvailable", true));
      setupFakeClient();
      try (cleanups) {
        var success = MADNESS_BAKERY.prepareForAdventure();
        var requests = getRequests();
        assertThat(requests, hasSize(0));
        assertThat(success, is(true));
      }
    }

    void withMadnessBakeryItemUsesItem() {
      var cleanups =
          new Cleanups(
              withProperty("madnessBakeryAvailable", false),
              withItem(ItemPool.HYPNOTIC_BREADCRUMBS));
      setupFakeClient();
      try (cleanups) {
        var success = MADNESS_BAKERY.prepareForAdventure();
        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_use.php",
            "whichitem=" + ItemPool.HYPNOTIC_BREADCRUMBS + "&ajax=1");
        assertThat(success, is(true));
      }
    }

    void withoutMadnessBakeryItemStartsQuest() {
      var cleanups = new Cleanups(withProperty("madnessBakeryStoreAvailable", false));
      setupFakeClient();
      try (cleanups) {
        var success = MADNESS_BAKERY.prepareForAdventure();
        var requests = getRequests();
        assertThat(requests, hasSize(3));
        assertPostRequest(requests.get(0), "/shop.php", "whichshop=armory");
        assertPostRequest(requests.get(1), "/shop.php", "whichshop=armory&action=talk");
        assertPostRequest(requests.get(2), "/choice.php", "whichchoice=1065&option=1");
        assertThat(success, is(true));
      }
    }

    void withAccessToOvergrownLotMakesNoRequests() {
      var cleanups = new Cleanups(withProperty("overgrownLotAvailable", true));
      setupFakeClient();
      try (cleanups) {
        var success = OVERGROWN_LOT.prepareForAdventure();
        var requests = getRequests();
        assertThat(requests, hasSize(0));
        assertThat(success, is(true));
      }
    }

    void withOvergrownLotItemUsesItem() {
      var cleanups =
          new Cleanups(withProperty("overgrownLotAvailable", false), withItem(ItemPool.BOOZE_MAP));
      setupFakeClient();
      try (cleanups) {
        var success = OVERGROWN_LOT.prepareForAdventure();
        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.BOOZE_MAP + "&ajax=1");
        assertThat(success, is(true));
      }
    }

    void withoutOvergrownLotItemStartsQuest() {
      var cleanups = new Cleanups(withProperty("overgrownLotAvailable", false));
      setupFakeClient();
      try (cleanups) {
        var success = OVERGROWN_LOT.prepareForAdventure();
        var requests = getRequests();
        assertThat(requests, hasSize(3));
        assertPostRequest(requests.get(0), "/shop.php", "whichshop=doc");
        assertPostRequest(requests.get(1), "/shop.php", "whichshop=doc&action=talk");
        assertPostRequest(requests.get(2), "/choice.php", "whichchoice=1064&option=1");
        assertThat(success, is(true));
      }
    }
  }
}
