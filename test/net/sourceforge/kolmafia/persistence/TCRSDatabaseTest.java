package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TCRSDatabaseTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("TCRSDatabaseTest");
  }

  @Test
  public void shouldLoadNewModifiers() {
    var cleanups =
        new Cleanups(
            withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
            withClass(AscensionClass.SEAL_CLUBBER),
            withSign(ZodiacSign.MONGOOSE));

    try (cleanups) {
      TCRSDatabase.loadTCRSData();
      Modifiers mods = ModifierDatabase.getModifiers(ModifierType.ITEM, ItemPool.ASPARAGUS_KNIFE);
      assertEquals(50.0, mods.getDouble(DoubleModifier.SLEAZE_SPELL_DAMAGE));
      assertEquals(0.0, mods.getDouble(DoubleModifier.STENCH_DAMAGE));
    }
  }
}
