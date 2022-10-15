package net.sourceforge.kolmafia.objectpool;

import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import java.util.Arrays;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/* This test was triggered by a runtime error traced back to sorting usable concoctions that
said "Comparison method violates its general contract!"  But it has been replaced by the cli
command checkconcoctions for the contract checking portion.
 */

public class ConcoctionTest {
  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset(true);
    KoLCharacter.reset("ConcoctionTest");
    Preferences.reset("ConcoctionTest");
  }

  // This test should never fail but may generate a error if data is introduced that is not
  // properly handled by the compareTo.
  @Test
  public void itShouldSortUsables() {
    var useables = ConcoctionDatabase.getUsables();
    int sizeBeforeSort = useables.size();
    useables.sort();
    assertThat(useables.size(), equalTo(sizeBeforeSort));
  }

  @Test
  public void steelOrgansAreSortedToTheTop() {
    var steelMargarita = ConcoctionPool.get(ItemPool.STEEL_LIVER);
    var nonSteelOrgan = ConcoctionPool.get(ItemPool.PERFECT_NEGRONI);
    assertThat(steelMargarita.compareTo(nonSteelOrgan), is(-1));
    assertThat(nonSteelOrgan.compareTo(steelMargarita), is(1));
  }

  @Test
  public void concoctionsCanBeComparedToThemselves() {
    var steelMargarita = ConcoctionPool.get(ItemPool.STEEL_LIVER);
    assertThat(steelMargarita, comparesEqualTo(steelMargarita));
  }

  @Test
  public void foodsAreSortedBeforeDrinks() {
    var steelMargarita = ConcoctionPool.get(ItemPool.STEEL_LIVER);
    var steelLasagna = ConcoctionPool.get(ItemPool.STEEL_STOMACH);
    assertThat(steelMargarita.compareTo(steelLasagna), is(1));
    assertThat(steelLasagna.compareTo(steelMargarita), is(-1));
  }

  @Test
  public void exerciseSameNames() {
    // Exercise compareTo and equals for Concoctions with the same name
    var e1 = ConcoctionPool.get(ItemPool.EYE_OF_ED);
    var e2 = ConcoctionPool.get(ItemPool.ED_EYE);

    int c1 = e1.compareTo(e2);
    int c2 = e2.compareTo(e1);
    boolean b1 = e1.equals(e2);
    boolean b2 = e2.equals(e1);
    // they should not be equal by compareTo
    assertThat(c1, not(is(0)));
    // but they should be quasi symmetric
    assertThat(c1, equalTo(-c2));
    // they should not be equal by equals
    assertThat(b1, is(false));
    assertThat(b2, is(false));
    assertThat(e1, not(equalTo(e2)));
  }

  @Test
  public void fancyIngredientsCorrespondToFancyRecipes() {
    for (Concoction concoction : ConcoctionPool.concoctions()) {
      Boolean isFancy =
          switch (concoction.getMixingMethod()) {
            case MIX_FANCY, COOK_FANCY -> true;
            case MIX, COOK -> false;
            default -> null;
          };

      if (isFancy == null) {
        continue;
      }

      boolean hasFancyIngredient =
          Arrays.stream(concoction.getIngredients())
              .map(AdventureResult::getItemId)
              .anyMatch(ItemDatabase::isFancyItem);

      assertThat(
          "concoction "
              + concoction
              + " with mixing method "
              + concoction.getMixingMethod()
              + " has fancy ingredients",
          isFancy,
          equalTo(hasFancyIngredient));
    }
  }

  @Nested
  class StillsuitDistillate {
    private static Concoction DISTILLATE = ConcoctionPool.get(-1, "stillsuit distillate");

    @ParameterizedTest
    @ValueSource(ints = {0, 10, 20})
    void cannotMakeDistillateWithoutDrams(int drams) {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.CARNIE),
              withEquipped(EquipmentManager.FAMILIAR, ItemPool.STILLSUIT),
              withProperty("familiarSweat", drams));

      try (cleanups) {
        DISTILLATE.calculate3();
        assertThat(DISTILLATE.freeTotal, is(drams >= 10 ? 1 : 0));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void cannotMakeDistillateWithoutStillSuit(boolean hasStillSuit) {
      var cleanups =
          new Cleanups(withFamiliar(FamiliarPool.CARNIE), withProperty("familiarSweat", 20));

      if (hasStillSuit) cleanups.add(withEquipped(EquipmentManager.FAMILIAR, ItemPool.STILLSUIT));

      try (cleanups) {
        DISTILLATE.calculate3();
        assertThat(DISTILLATE.freeTotal, is(hasStillSuit ? 1 : 0));
      }
    }
  }
}
