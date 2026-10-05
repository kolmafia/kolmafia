package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withFamiliarInTerrariumWithItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class StillSuitManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("StillSuitManagerTest");
    Preferences.reset("StillSuitManagerTest");
  }

  @Nested
  public class SweatGain {
    @Test
    public void noSweatForUnequippedSuit() {
      try (var _ =
          new Cleanups(
              withProperty("familiarSweat", "42"),
              withFamiliar(FamiliarPool.PET_ROCK),
              withEquipped(ItemPool.TINY_COSTUME_WARDROBE),
              withEquippableItem(ItemPool.STILLSUIT))) {
        StillSuitManager.handleSweat("stillsuit.gif");
        assertThat("familiarSweat", isSetTo(42));
      }
    }

    @Test
    public void suitOnInactiveFamiliar() {
      try (var _ =
          new Cleanups(
              withProperty("familiarSweat", "42"),
              withFamiliar(FamiliarPool.PET_ROCK),
              withEquipped(ItemPool.TINY_COSTUME_WARDROBE),
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
              withFamiliarInTerrariumWithItem(FamiliarPool.BABY_GRAVY_FAIRY, ItemPool.STILLSUIT))) {
        StillSuitManager.handleSweat("stillsuit.gif");
        assertThat("familiarSweat", isSetTo(43));
      }
    }

    @Test
    public void suitOnActiveFamiliar() {
      try (var _ =
          new Cleanups(
              withProperty("familiarSweat", "42"),
              withFamiliar(FamiliarPool.PET_ROCK),
              withEquipped(Slot.FAMILIAR, ItemPool.STILLSUIT),
              withFamiliarInTerrarium(FamiliarPool.BABY_GRAVY_FAIRY))) {
        StillSuitManager.handleSweat("stillsuit.gif");
        assertThat("familiarSweat", isSetTo(45));
      }
    }

    @Test
    public void suitOnActiveFamiliarWithExtraSuit() {
      try (var _ =
          new Cleanups(
              withProperty("familiarSweat", "42"),
              withFamiliar(FamiliarPool.PET_ROCK),
              withEquipped(Slot.FAMILIAR, ItemPool.STILLSUIT),
              withFamiliarInTerrariumWithItem(FamiliarPool.BABY_GRAVY_FAIRY, ItemPool.STILLSUIT))) {
        StillSuitManager.handleSweat("stillsuit.gif");
        assertThat("familiarSweat", isSetTo(45));
      }
    }
  }
}
