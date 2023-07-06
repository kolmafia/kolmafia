package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TCRSDatabaseTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("TCRSDatabaseTest");
  }

  @AfterAll
  static void afterAll() throws IOException {
    ConsumablesDatabase.clearAndRebuild();
    try (var walker = Files.walk(KoLConstants.DATA_LOCATION.toPath())) {
      walker
          .map(java.nio.file.Path::toFile)
          .filter(f -> f.getName().startsWith("TCRS_"))
          .filter(f -> f.getName().endsWith(".txt"))
          .forEach(File::delete);
    }
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
