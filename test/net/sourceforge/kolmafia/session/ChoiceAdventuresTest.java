package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.session.ChoiceAdventures.ShadowTheme;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ChoiceAdventuresTest {
  @Nested
  class GreatOverlookLodge {
    private static final int GREAT_OVERLOOK_LODGE = 606;

    @Test
    void itemDropTestWorks() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, "Radio KoL Maracas"),
              withEffect(EffectPool.THERES_NO_N_IN_LOVE));

      try (cleanups) {
        var options = ChoiceAdventures.dynamicChoiceOptions(GREAT_OVERLOOK_LODGE);
        assert options != null;
        assertThat(options[1].getName(), is("need +50% item drop, have 115%"));
      }
    }

    @Test
    void itemDropTestDoesntConsiderItemFairy() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 400),
              withEquipped(Slot.ACCESSORY1, "Radio KoL Maracas"));

      try (cleanups) {
        var options = ChoiceAdventures.dynamicChoiceOptions(GREAT_OVERLOOK_LODGE);
        assert options != null;
        assertThat(options[1].getName(), is("need +50% item drop, have 15%"));
      }
    }

    @Test
    void itemDropTestDoesntConsiderFoodFairy() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.COOKBOOKBAT, 400),
              withEquipped(Slot.ACCESSORY1, "Radio KoL Maracas"));

      try (cleanups) {
        var options = ChoiceAdventures.dynamicChoiceOptions(GREAT_OVERLOOK_LODGE);
        assert options != null;
        assertThat(options[1].getName(), is("need +50% item drop, have 15%"));
      }
    }
  }

  @Nested
  class ShadowLabyrinth {
    @Test
    void canDetectWaterAdjective() {
      var text = "Leap into the sodden hole";
      ShadowTheme theme = ChoiceAdventures.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = ChoiceAdventures.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.WATER);
      assertEquals("90-100 Moxie substats or shadow bucket", spoiler.toString());
    }

    @Test
    void canDetectMathAdjective() {
      var text = "Try to reach the irrational portal";
      ShadowTheme theme = ChoiceAdventures.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = ChoiceAdventures.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.MATH);
      assertEquals("90-100 Mysticality substats or shadow heptahedron", spoiler.toString());
    }

    @Test
    void canDetectBloodAdjective() {
      var text = "Walk to the vein-shot lane";
      ShadowTheme theme = ChoiceAdventures.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = ChoiceAdventures.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.BLOOD);
      assertEquals("Shadow's Heart: Maximum HP +300% or shadow heart", spoiler.toString());
    }

  }
}
