package net.sourceforge.kolmafia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ModifiersTest {
    @Test
    public void patriotSheildClassModifiers() {
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
}
