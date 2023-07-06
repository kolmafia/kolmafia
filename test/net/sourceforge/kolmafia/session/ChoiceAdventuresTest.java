package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ChoiceAdventuresTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ChoiceAdventures");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("ChoiceAdventures");
  }

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
}
