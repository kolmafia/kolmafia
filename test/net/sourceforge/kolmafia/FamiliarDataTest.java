package net.sourceforge.kolmafia;

import static internal.helpers.Networking.html;
import static internal.helpers.Networking.json;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withNotAllowedInStandard;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class FamiliarDataTest {

  @BeforeAll
  public static void beforeAll() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("familiar data test");
  }

  @Test
  public void canTellIfFamiliarIsTrainable() {
    var fam = new FamiliarData(FamiliarPool.MOSQUITO);
    assertTrue(fam.trainable());
  }

  @Test
  public void canTellIfFamiliarIsNotTrainable() {
    var fam = new FamiliarData(FamiliarPool.PET_ROCK);
    assertFalse(fam.trainable());
  }

  @Test
  void familiarReportsModifiedWeightIncludingFidoxene() {
    var cleanups = withEffect("Fidoxene");

    try (cleanups) {
      var familiar = FamiliarData.registerFamiliar(FamiliarPool.ALIEN, 0);

      assertThat(familiar.getModifiedWeight(), equalTo(20));
    }
  }

  @Test
  void familiarReportsModifiedWeightCorrectlyDespiteFidoxene() {
    var cleanups = withEffect("Fidoxene");

    try (cleanups) {
      var familiar = FamiliarData.registerFamiliar(FamiliarPool.ALIEN, 400);

      assertThat(familiar.getModifiedWeight(), equalTo(20));
    }
  }

  @Test
  void fidoxeneWorksWithNonstandardMaxBaseWeightFamiliars() {
    var cleanups = withEffect("Fidoxene");

    try (cleanups) {
      var familiar = FamiliarData.registerFamiliar(FamiliarPool.STOCKING_MIMIC, 900);

      assertThat(familiar.getModifiedWeight(), equalTo(30));
    }
  }

  @Test
  public void canHandleGreyGooseCombatSkills() {
    // We are currently in no path
    KoLCharacter.setPath(Path.NONE);

    // Make a one-pound familiar
    var fam = new FamiliarData(FamiliarPool.GREY_GOOSE);

    // Activate it. (i.e., take out of the terrarium)
    fam.activate();
    assertTrue(fam.isActive());

    // Verify that no combat skills are available
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.RE_PROCESS_MATTER));
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.MEATIFY_MATTER));
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.EMIT_MATTER_DUPLICATING_DRONES));
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_PROTEIN));
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_ENERGY));
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_POMADE));

    // Make it level 6
    fam.setWeight(6);

    // Verify that non-Grey You combat skills are available
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.RE_PROCESS_MATTER));
    assertTrue(KoLCharacter.hasCombatSkill(SkillPool.MEATIFY_MATTER));
    assertTrue(KoLCharacter.hasCombatSkill(SkillPool.EMIT_MATTER_DUPLICATING_DRONES));
    assertTrue(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_PROTEIN));
    assertTrue(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_ENERGY));
    assertTrue(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_POMADE));

    // Reset it to level 5
    fam.setWeight(5);

    // Verify that no combat skills are available
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.RE_PROCESS_MATTER));
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.MEATIFY_MATTER));
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.EMIT_MATTER_DUPLICATING_DRONES));
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_PROTEIN));
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_ENERGY));
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_POMADE));

    // Go into Grey You
    KoLCharacter.setPath(Path.GREY_YOU);

    // Make it level 6
    fam.setWeight(6);

    // Verify that all combat skills are available
    assertTrue(KoLCharacter.hasCombatSkill(SkillPool.RE_PROCESS_MATTER));
    assertTrue(KoLCharacter.hasCombatSkill(SkillPool.MEATIFY_MATTER));
    assertTrue(KoLCharacter.hasCombatSkill(SkillPool.EMIT_MATTER_DUPLICATING_DRONES));
    assertTrue(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_PROTEIN));
    assertTrue(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_ENERGY));
    assertTrue(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_POMADE));

    // Reset it to level 5
    fam.setWeight(5);

    // Indicate we've cast MeatifyMatter already today
    Preferences.setBoolean("_meatifyMatterUsed", true);

    // Make it level 6
    fam.setWeight(6);

    // Verify that Meatify Matter is no longer available
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.MEATIFY_MATTER));

    // Deactivate it. (i.e., put back into the terrarium)
    fam.deactivate();
    assertFalse(fam.isActive());

    // Verify that no combat skills are available
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.RE_PROCESS_MATTER));
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.MEATIFY_MATTER));
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.EMIT_MATTER_DUPLICATING_DRONES));
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_PROTEIN));
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_ENERGY));
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_POMADE));
  }

  @Test
  public void dropsTodayFalse() {
    var di =
        new FamiliarData.DropInfo(
            FamiliarPool.COOKBOOKBAT, -1, "cookbookbat recipe", "_cookbookbatRecipeDrops", 1);
    new Cleanups(withProperty("_cookbookbatRecipeDrops", false));

    assertThat(di.dropsToday(), equalTo(0));
  }

  @Test
  public void dropsTodayTrue() {
    var di =
        new FamiliarData.DropInfo(
            FamiliarPool.COOKBOOKBAT, -1, "cookbookbat recipe", "_cookbookbatRecipeDrops", 1);
    new Cleanups(withProperty("_cookbookbatRecipeDrops", true));

    assertThat(di.dropsToday(), equalTo(1));
  }

  @Test
  public void dropsTodayNumeric() {
    var di =
        new FamiliarData.DropInfo(
            FamiliarPool.MS_PUCK_MAN,
            ItemPool.POWER_PILL,
            "power pill",
            "_powerPillDrops",
            Math.min(1 + KoLCharacter.getCurrentDays(), 11));

    new Cleanups(withProperty("_powerPillDrops", 7));

    assertThat(di.dropsToday(), equalTo(7));
  }

  @ParameterizedTest
  @CsvSource({"1, 12", "5, 56"})
  public void homemadeRobotWeightCalculationIgnoresExp(Integer upgrades, Integer weight) {
    var fam = new FamiliarData(FamiliarPool.HOMEMADE_ROBOT);

    var cleanups = new Cleanups(withProperty("homemadeRobotUpgrades", upgrades));
    try (cleanups) {
      // This experience should be ignored
      fam.setExperience(69);
      assertThat(fam.getWeight(), equalTo(weight));
    }
  }

  @Nested
  class Terrarium {
    @BeforeAll
    public static void beforeAll() {
      // Other tests add familiars to the character
      // Start clean.
      KoLCharacter.reset("");
      KoLCharacter.reset("familiar data test");
    }

    @AfterEach
    public void afterEach() {
      // We're going to register a lot of familiars
      // Reset the character to eliminate leaks to other tests
      KoLCharacter.reset("");
      KoLCharacter.reset("familiar data test");
    }

    @Test
    public void registersExpiredFamiliars() {
      var cleanups =
          new Cleanups(
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.FAMILIARS, "Pet Rock"));

      try (cleanups) {
        FamiliarData.registerFamiliarData(html("request/test_terrarium_standard.html"));

        assertTrue(KoLCharacter.ownedFamiliar(FamiliarPool.PET_ROCK).isPresent());
        assertThat(KoLCharacter.usableFamiliar(FamiliarPool.PET_ROCK), nullValue());
      }
    }
  }

  @Nested
  class API {
    @BeforeAll
    public static void beforeAll() {
      // Other tests add familiars to the character
      // Start clean.
      KoLCharacter.reset("");
      KoLCharacter.reset("familiar data test");
    }

    @AfterEach
    public void afterEach() {
      // ApiRequest.parseStatus() sets all sorts of stuff
      // Reset the character to eliminate leaks to other tests
      KoLCharacter.reset("");
      KoLCharacter.reset("familiar data test");
    }

    @Test
    public void canSetQuantumFamiliarFromApi() {
      String text = html("request/test_quantum_terrarium_api.json");
      JSONObject JSON = json(text);

      // Here are the attributes relevant to familiars
      int famId = JSON.getInt("familiar");
      int famExp = JSON.getInt("familiarexp");
      String famPic = JSON.getString("familiarpic");
      int famLevel = JSON.getInt("famlevel");
      boolean feasted = JSON.getInt("familiar_wellfed") == 1;

      // Stats affect Familiar Weight in Quantum Familiar
      int basemuscle = JSON.getInt("basemuscle");
      int basemysticality = JSON.getInt("basemysticality");
      int basemoxie = JSON.getInt("basemoxie");

      Cleanups cleanups =
          new Cleanups(
              withPath(Path.QUANTUM),
              withClass(AscensionClass.ACCORDION_THIEF),
              withStats(basemuscle, basemysticality, basemoxie),
              withSkill("Amphibian Sympathy"));

      try (cleanups) {
        ApiRequest.parseStatus(JSON);
        FamiliarData current = KoLCharacter.getFamiliar();
        assertEquals(famId, current.getId());
        assertEquals(famExp, current.getTotalExperience());
        // Base Weight
        assertEquals(Math.min(20, (int) Math.sqrt(famExp)), current.getWeight());
        assertEquals(feasted, current.getFeasted());
        // Image can change, so current image is in KoLCharacter
        assertEquals(famPic + ".gif", KoLCharacter.getFamiliarImage());
        assertEquals(EquipmentManager.getEquipment(Slot.FAMILIAR), current.getItem());
        // Modified Weight
        assertEquals(famLevel, current.getModifiedWeight());
      }
    }

    @Test
    public void canSetQuantumCrimboGhostFromApi() {
      String text = html("request/test_quantum_terrarium_api2.json");
      JSONObject JSON = json(text);

      // Here are the attributes relevant to familiars
      int famId = JSON.getInt("familiar");
      assertEquals(famId, FamiliarPool.GHOST_CHEER);
      int famExp = JSON.getInt("familiarexp");
      String famPic = JSON.getString("familiarpic");
      int famLevel = JSON.getInt("famlevel");
      boolean feasted = JSON.getInt("familiar_wellfed") == 1;

      // Stats affect Familiar Weight in Quantum Familiar
      int basemuscle = JSON.getInt("basemuscle");
      int basemysticality = JSON.getInt("basemysticality");
      int basemoxie = JSON.getInt("basemoxie");

      Cleanups cleanups =
          new Cleanups(
              withPath(Path.QUANTUM),
              withClass(AscensionClass.ACCORDION_THIEF),
              withStats(basemuscle, basemysticality, basemoxie),
              withSkill("Amphibian Sympathy"),
              withEquipped(Slot.HAT, "Daylight Shavings Helmet"));

      try (cleanups) {
        ApiRequest.parseStatus(JSON);
        FamiliarData current = KoLCharacter.getFamiliar();
        assertEquals(famId, current.getId());
        // *** KoL bug: exp for Crimbo Ghosts is not accurate
        // *** We corrected the weight from KoL's level
        // *** and set experience to minimum required for that weight
        assertEquals(12, current.getWeight());
        assertEquals(144, current.getTotalExperience());
        assertEquals(feasted, current.getFeasted());
        // Image can change, so current image is in KoLCharacter
        assertEquals(famPic + ".gif", KoLCharacter.getFamiliarImage());
        // In Quantum Terrarium, an item may still be in the "familiar" slot,
        // but Crimbo ghosts cannot equip items.
        assertEquals(EquipmentRequest.UNEQUIP, current.getItem());
        // Modified Weight
        assertEquals(famLevel, current.getModifiedWeight());
      }
    }

    @Test
    public void canSetPathlessCrimboGhostFromApi() {
      String terrarium = html("request/test_crimbo_ghost_terrarium.html");
      String text = html("request/test_crimbo_ghost_api.json");
      JSONObject JSON = json(text);

      // Here are the attributes relevant to familiars
      int famId = JSON.getInt("familiar");
      assertEquals(famId, FamiliarPool.GHOST_CHEER);
      int famExp = JSON.getInt("familiarexp");
      String famPic = JSON.getString("familiarpic");
      int famLevel = JSON.getInt("famlevel");
      boolean feasted = JSON.getInt("familiar_wellfed") == 1;

      Cleanups cleanups =
          new Cleanups(
              withPath(Path.NONE),
              withEquipped(Slot.HAT, "Daylight Shavings Helmet"),
              withSkill("Amphibian Sympathy"),
              withEffect("Cute Vision"),
              withEffect("Empathy"),
              withEffect("Leash of Linguini"));

      try (cleanups) {
        // Register all familiars from the terrarium
        FamiliarData.registerFamiliarData(terrarium);

        // Parse the API response to handle current familiar
        ApiRequest.parseStatus(JSON);
        FamiliarData current = KoLCharacter.getFamiliar();
        assertEquals(famId, current.getId());
        // *** KoL bug: exp for Crimbo Ghosts is not accurate
        // However, the exp read from the Terrarium is accurate
        assertEquals(110, current.getTotalExperience());
        // Base Weight
        assertEquals(Math.min(20, (int) Math.sqrt(110)), current.getWeight());
        assertEquals(feasted, current.getFeasted());
        // Image can change, so current image is in KoLCharacter
        assertEquals(famPic + ".gif", KoLCharacter.getFamiliarImage());
        // In Quantum Terrarium, an item may still be in the "familiar" slot,
        // but Crimbo ghosts cannot equip items.
        assertEquals(EquipmentRequest.UNEQUIP, current.getItem());
        // Modified Weight
        assertEquals(famLevel, current.getModifiedWeight());
      }
    }
  }

  @Nested
  class Comma {
    @Test
    public void correctEffectiveIdWhenCommaImitating() {
      var cleanups =
          new Cleanups(
              withProperty("commaFamiliar", "Mosquito"), withFamiliar(FamiliarPool.CHAMELEON));

      try (cleanups) {
        assertThat(KoLCharacter.currentFamiliar.getEffectiveId(), is(FamiliarPool.MOSQUITO));
      }
    }

    @Test
    public void correctEffectiveRaceWhenCommaImitating() {
      var cleanups =
          new Cleanups(
              withProperty("commaFamiliar", "Mosquito"), withFamiliar(FamiliarPool.CHAMELEON));

      try (cleanups) {
        assertThat(KoLCharacter.currentFamiliar.getEffectiveRace(), is("Mosquito"));
      }
    }

    @Test
    public void correctEffectiveIdWhenCommaEmpty() {
      var cleanups =
          new Cleanups(withProperty("commaFamiliar", ""), withFamiliar(FamiliarPool.CHAMELEON));

      try (cleanups) {
        assertThat(KoLCharacter.currentFamiliar.getEffectiveId(), is(FamiliarPool.CHAMELEON));
      }
    }

    @Test
    public void correctEffectiveRaceWhenCommaEmpty() {
      var cleanups =
          new Cleanups(withProperty("commaFamiliar", ""), withFamiliar(FamiliarPool.CHAMELEON));

      try (cleanups) {
        assertThat(KoLCharacter.currentFamiliar.getEffectiveRace(), is("Comma Chameleon"));
      }
    }

    @Test
    public void commaHasCorrectWeightWhenHomemadeRobot() {
      var cleanups =
          new Cleanups(
              withProperty("commaFamiliar", "Homemade Robot"),
              withProperty("homemadeRobotUpgrades", 3),
              withFamiliar(FamiliarPool.CHAMELEON));

      try (cleanups) {
        assertThat(KoLCharacter.currentFamiliar.getWeight(), is(34));
      }
    }

    @Test
    public void commaDoesNotInheritAttributes() {
      var cleanups =
          new Cleanups(
              withProperty("commaFamiliar", "Hovering Sombrero"),
              withFamiliar(FamiliarPool.CHAMELEON));

      try (cleanups) {
        assertThat(KoLCharacter.currentFamiliar.isUndead(), is(false));
      }
    }

    @Test
    public void commaDoesInheritWaterBreathing() {
      var cleanups =
          new Cleanups(
              withProperty("commaFamiliar", "Urchin Urchin"), withFamiliar(FamiliarPool.CHAMELEON));

      try (cleanups) {
        assertThat(KoLCharacter.currentFamiliar.waterBreathing(), is(true));
      }
    }
  }
}
