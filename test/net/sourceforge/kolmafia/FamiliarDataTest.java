package net.sourceforge.kolmafia;

import static internal.helpers.Networking.html;
import static internal.helpers.Networking.json;
import static internal.helpers.Player.addEffect;
import static internal.helpers.Player.addSkill;
import static internal.helpers.Player.equip;
import static internal.helpers.Player.inPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class FamiliarDataTest {

  @BeforeAll
  private static void beforeAll() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("familiar data test");
    Preferences.saveSettingsToFile = false;
  }

  @AfterAll
  private static void afterAll() {
    Preferences.saveSettingsToFile = true;
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
    var cleanups = addEffect("Fidoxene");

    try (cleanups) {
      var familiar = FamiliarData.registerFamiliar(FamiliarPool.ALIEN, 0);

      assertThat(familiar.getModifiedWeight(), equalTo(20));
    }
  }

  @Test
  void familiarReportsModifiedWeightCorrectlyDespiteFidoxene() {
    var cleanups = addEffect("Fidoxene");

    try (cleanups) {
      var familiar = FamiliarData.registerFamiliar(FamiliarPool.ALIEN, 400);

      assertThat(familiar.getModifiedWeight(), equalTo(20));
    }
  }

  @Test
  void fidoxeneWorksWithNonstandardMaxBaseWeightFamiliars() {
    var cleanups = addEffect("Fidoxene");

    try (cleanups) {
      var familiar = FamiliarData.registerFamiliar(FamiliarPool.HOMEMADE_ROBOT, 900);

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

  @Nested
  class API {
    @BeforeAll
    private static void beforeAll() {
      // Other tests add familiars to the character
      // Start clean.
      KoLCharacter.reset("");
      KoLCharacter.reset("familiar data test");
    }

    @AfterEach
    private void afterEach() {
      // ApiRequest.parseStatus() sets all sorts of stuff
      // Reset the character to eliminate leaks to other tests
      KoLCharacter.reset("");
      KoLCharacter.reset("familiar data test");
    }

    @Test
    public void canSetFamiliarFromApi() {
      String text = html("request/test_quantum_terrarium_api.json");
      JSONObject JSON = json(text);

      // Here are the attributes relevant to familiars
      int famId = JSON.getInt("familiar");
      int famExp = JSON.getInt("familiarexp");
      String famPic = JSON.getString("familiarpic");
      int famLevel = JSON.getInt("famlevel");
      boolean feasted = JSON.getInt("familiar_wellfed") == 1;

      Cleanups cleanups =
          new Cleanups(
              inPath(Path.QUANTUM),
              addSkill("Amphibian Sympathy"),
              equip(EquipmentManager.FAMILIAR, "astral pet sweater"));

      try (cleanups) {
        ApiRequest.parseStatus(JSON);
        FamiliarData current = KoLCharacter.getFamiliar();
        assertEquals(famId, current.getId());
        assertEquals(famExp, current.getTotalExperience());
        assertEquals(feasted, current.getFeasted());
        // Image can change, so current image is in KoLCharacter
        assertEquals(famPic + ".gif", KoLCharacter.getFamiliarImage());
        // Base Weight
        assertEquals(Math.min(20, Math.sqrt(famExp)), current.getWeight());
        // Modified Weight
        assertEquals(famLevel, current.getModifiedWeight());
      }
    }
  }
}
