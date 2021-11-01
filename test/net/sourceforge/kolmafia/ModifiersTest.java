package net.sourceforge.kolmafia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map.Entry;
import org.junit.jupiter.api.Test;

public class ModifiersTest {
  @Test
  public void patriotShieldClassModifiers() {
    // Wide-reaching unit test for getModifiers
    KoLCharacter.setAscensionClass(AscensionClass.AVATAR_OF_JARLSBERG);
    Modifiers mods = Modifiers.getModifiers("Item", "Patriot Shield");

    // Always has
    assertEquals(3, mods.get(Modifiers.EXPERIENCE));

    // Has because of class
    assertEquals(5.0, mods.get(Modifiers.MP_REGEN_MIN));
    assertEquals(6.0, mods.get(Modifiers.MP_REGEN_MAX));
    assertEquals(20.0, mods.get(Modifiers.SPELL_DAMAGE));

    // Does not have because of class
    assertEquals(0, mods.get(Modifiers.HP_REGEN_MAX));
    assertEquals(0, mods.get(Modifiers.HP_REGEN_MIN));
    assertEquals(0, mods.get(Modifiers.WEAPON_DAMAGE));
    assertEquals(0, mods.get(Modifiers.DAMAGE_REDUCTION));
    assertEquals(0, mods.get(Modifiers.FAMILIAR_WEIGHT));
    assertEquals(0, mods.get(Modifiers.RANGED_DAMAGE));
    assertEquals(false, mods.getBoolean(Modifiers.FOUR_SONGS));
    assertEquals(0, mods.get(Modifiers.COMBAT_MANA_COST));
  }

  @Test
  public void testSynergies() {
    // The "synergy" bitmap modifier is assigned dynamically, based on appearance order in
    // Modifiers.txt
    // The first Synergetic item seen gets 0b00001, the 2nd: 0b00010, 3rd: 0b00100, etc.

    for (Entry<String, Integer> entry : Modifiers.getSynergies()) {
      String name = entry.getKey();
      int mask = entry.getValue().intValue();

      int manualMask = 0;
      for (String piece : name.split("/")) {
        Modifiers mods = Modifiers.getModifiers("Item", piece);
        manualMask |= mods.getRawBitmap(Modifiers.SYNERGETIC);
      }

      assertEquals(manualMask, mask, name);
    }
  }
}
