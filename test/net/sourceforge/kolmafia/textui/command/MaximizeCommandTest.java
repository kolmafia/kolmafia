package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * There are several ways to invoke the Maximizer. They all eventually get to the same code. These
 * tests invoke the Maximizer via the cli command. The data is based upon a possible failure to
 * select/equip but the test passes. It is somewhat redundant given RuntimeLibrary and Maximizer
 * tests but it isn't hurting anything and may prove useful. itShouldSelectAndEquipAWeapon and
 * itShouldSelectAndEquipAnEffectiveWeapon are identical except for the addition of the effective
 * keyword. They are candidates for a parameterized test but that has not been done. This test
 * relies on the output from the Equip command and but not its actual execution.
 */
public class MaximizeCommandTest extends AbstractCommandTestBase {
  public MaximizeCommandTest() {
    this.command = "maximize";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("MaximizeCommandTest");
  }

  @Test
  public void itShouldSelectAndEquipAWeapon() {
    String maxStr =
        "5item,meat,0.5initiative,0.1da 1000max,dr,0.5all res,1.5mainstat,-fumble,mox,0.4hp,0.2mp 1000max,3mp regen,0.25spell damage,1.75spell damage percent,2familiar weight,5familiar exp,10exp,5Mysticality experience percent,200combat 20max,+200bonus mafia thumb ring";
    var cleanups =
        new Cleanups(
            withEquippableItem("candy cane sword cane"),
            withEquippableItem("pasta spoon"),
            withEquippableItem("Rain-Doh violet bo"),
            withEquippableItem("Rain-Doh yellow laser gun"),
            withEquippableItem("saucepan"),
            withEquippableItem("toy accordion"),
            withEquippableItem("turtle totem"),
            withEquippableItem("psychic's crystal ball"),
            withEquippableItem("Rain-Doh green lantern"),
            withEquippableItem("stuffed baby gravy fairy"),
            withEquippableItem("stuffed key"),
            withEquippableItem("unbreakable umbrella (broken)"),
            withStats(2, 27, 1),
            withSkill(SkillPool.MASTER_OF_THE_SURPRISING_FIST));
    String out;
    try (cleanups) {
      out = execute(maxStr);
    }
    assertFalse(out.isEmpty());
    assertTrue(out.contains("Wielding candy cane sword cane..."));
    assertTrue(out.contains("Folding umbrella"));
    assertTrue(out.contains("Holding unbreakable umbrella..."));
  }

  @Test
  public void itShouldSelectAndEquipAnEffectiveWeapon() {
    String maxStr =
        "5item,meat,0.5initiative,0.1da 1000max,dr,0.5all res,1.5mainstat,-fumble,mox,0.4hp,0.2mp 1000max,3mp regen,0.25spell damage,1.75spell damage percent,2familiar weight,5familiar exp,10exp,5Mysticality experience percent,200combat 20max,+200bonus mafia thumb ring, effective";
    var cleanups =
        new Cleanups(
            withEquippableItem("candy cane sword cane"),
            withEquippableItem("pasta spoon"),
            withEquippableItem("Rain-Doh violet bo"),
            withEquippableItem("Rain-Doh yellow laser gun"),
            withEquippableItem("saucepan"),
            withEquippableItem("toy accordion"),
            withEquippableItem("turtle totem"),
            withEquippableItem("psychic's crystal ball"),
            withEquippableItem("Rain-Doh green lantern"),
            withEquippableItem("stuffed baby gravy fairy"),
            withEquippableItem("stuffed key"),
            withEquippableItem("unbreakable umbrella (broken)"),
            withStats(2, 27, 1),
            withSkill(SkillPool.MASTER_OF_THE_SURPRISING_FIST));
    String out;
    try (cleanups) {
      out = execute(maxStr);
    }
    assertFalse(out.isEmpty());
    assertTrue(out.contains("Wielding candy cane sword cane..."));
    assertTrue(out.contains("Folding umbrella"));
    assertTrue(out.contains("Holding unbreakable umbrella..."));
  }
}
