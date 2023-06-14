package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.*;

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
      assertThat(mods.getDouble(DoubleModifier.SLEAZE_SPELL_DAMAGE), is(50.0));
      assertThat(mods.getDouble(DoubleModifier.STENCH_DAMAGE), is(0.0));
    }
  }

  @Test
  public void campgroundItemsRetainModifiers() {
    var cleanups =
        new Cleanups(
            withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
            withClass(AscensionClass.SEAL_CLUBBER),
            withSign(ZodiacSign.MONGOOSE));

    try (cleanups) {
      TCRSDatabase.loadTCRSData();
      Modifiers mods = ModifierDatabase.getModifiers(ModifierType.ITEM, ItemPool.MAID);
      assertThat(mods.getDouble(DoubleModifier.ADVENTURES), is(4.0));
    }
  }

  @Test
  public void chateauItemsRetainModifiers() {
    var cleanups =
        new Cleanups(
            withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
            withClass(AscensionClass.SEAL_CLUBBER),
            withSign(ZodiacSign.MONGOOSE));

    try (cleanups) {
      TCRSDatabase.loadTCRSData();
      Modifiers mods = ModifierDatabase.getModifiers(ModifierType.ITEM, ItemPool.CHATEAU_SKYLIGHT);
      assertThat(mods.getDouble(DoubleModifier.ADVENTURES), is(3.0));
    }
  }
}
