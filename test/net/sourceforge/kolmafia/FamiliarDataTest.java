package net.sourceforge.kolmafia;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
  public void canHandleGreyGooseCombatSkills() {
    // We are currently in no path
    KoLCharacter.setPath(Path.NONE);

    // Make a one-pound familiar
    var fam = new FamiliarData(FamiliarPool.GREY_GOOSE);

    // Activate it. (i.e., take out of the terrarium)
    fam.activate();
    assertTrue(fam.isActive());

    // Verify that no combat skills are available
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.RE_PROCESS_MATTER));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.MEATIFY_MATTER));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.EMIT_MATTER_DUPLICATING_DRONES));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.CONVERT_MATTER_TO_PROTEIN));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.CONVERT_MATTER_TO_ENERGY));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.CONVERT_MATTER_TO_POMADE));

    // Make it level 6
    fam.setWeight(6);

    // Verify that non-Grey You combat skills are available
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.RE_PROCESS_MATTER));
    assertTrue(KoLCharacter.availableCombatSkill(SkillPool.MEATIFY_MATTER));
    assertTrue(KoLCharacter.availableCombatSkill(SkillPool.EMIT_MATTER_DUPLICATING_DRONES));
    assertTrue(KoLCharacter.availableCombatSkill(SkillPool.CONVERT_MATTER_TO_PROTEIN));
    assertTrue(KoLCharacter.availableCombatSkill(SkillPool.CONVERT_MATTER_TO_ENERGY));
    assertTrue(KoLCharacter.availableCombatSkill(SkillPool.CONVERT_MATTER_TO_POMADE));

    // Reset it to level 5
    fam.setWeight(5);

    // Verify that no combat skills are available
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.RE_PROCESS_MATTER));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.MEATIFY_MATTER));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.EMIT_MATTER_DUPLICATING_DRONES));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.CONVERT_MATTER_TO_PROTEIN));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.CONVERT_MATTER_TO_ENERGY));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.CONVERT_MATTER_TO_POMADE));

    // Go into Grey You
    KoLCharacter.setPath(Path.GREY_YOU);

    // Make it level 6
    fam.setWeight(6);

    // Verify that all combat skills are available
    assertTrue(KoLCharacter.availableCombatSkill(SkillPool.RE_PROCESS_MATTER));
    assertTrue(KoLCharacter.availableCombatSkill(SkillPool.MEATIFY_MATTER));
    assertTrue(KoLCharacter.availableCombatSkill(SkillPool.EMIT_MATTER_DUPLICATING_DRONES));
    assertTrue(KoLCharacter.availableCombatSkill(SkillPool.CONVERT_MATTER_TO_PROTEIN));
    assertTrue(KoLCharacter.availableCombatSkill(SkillPool.CONVERT_MATTER_TO_ENERGY));
    assertTrue(KoLCharacter.availableCombatSkill(SkillPool.CONVERT_MATTER_TO_POMADE));

    // Reset it to level 5
    fam.setWeight(5);

    // Indicate we've cast MeatifyMatter already today
    Preferences.setBoolean("_meatifyMatterUsed", true);

    // Make it level 6
    fam.setWeight(6);

    // Verify that Meatify Matter is no longer available
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.MEATIFY_MATTER));

    // Deactivate it. (i.e., put back into the terrarium)
    fam.deactivate();
    assertFalse(fam.isActive());

    // Verify that no combat skills are available
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.RE_PROCESS_MATTER));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.MEATIFY_MATTER));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.EMIT_MATTER_DUPLICATING_DRONES));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.CONVERT_MATTER_TO_PROTEIN));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.CONVERT_MATTER_TO_ENERGY));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.CONVERT_MATTER_TO_POMADE));
  }
}
