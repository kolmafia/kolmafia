package net.sourceforge.kolmafia.session;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.BlackAndWhiteApronManager.Ingredient;
import net.sourceforge.kolmafia.session.BlackAndWhiteApronManager.MainIngredient;
import net.sourceforge.kolmafia.session.BlackAndWhiteApronManager.MealKit;
import org.junit.jupiter.api.Test;

public class BlackAndWhiteApronManagerTest {
  @Test
  public void generatesMealKit() {
    MealKit mealKit =
        BlackAndWhiteApronManager.getMealKitOptions(Path.STANDARD, AscensionClass.TURTLE_TAMER, 1);
    MainIngredient mainIngredient = mealKit.mainIngredient();
    assertThat(mainIngredient.name(), is("potatoes"));
    assertThat(mainIngredient.effect().getEffectId(), is(EffectPool.SPUDLY));
    assertThat(mainIngredient.effect().getCount(), is(100));

    // Spot-check a few items in each meal
    Ingredient[] meal = mealKit.meals()[0];
    assertThat(meal[2].ingredient().getItemId(), is(ItemPool.GHUOL_EGG));
    assertThat(meal[2].meat(), is(100));
    assertThat(meal[3].ingredient().getItemId(), is(ItemPool.BOTTLE_OF_VODKA));
    assertThat(meal[3].mox(), is(75));

    meal = mealKit.meals()[1];
    assertThat(meal[1].ingredient().getItemId(), is(ItemPool.MOUNTAIN_STREAM_SODA));
    assertThat(meal[1].other(), is("Restore 250 MP"));
    assertThat(meal[4].ingredient().getItemId(), is(ItemPool.SLICK_FISH_MEAT));
    assertThat(meal[4].mox(), is(250));

    meal = mealKit.meals()[2];
    assertThat(meal[0].ingredient().getItemId(), is(ItemPool.BOTTLE_OF_RUM));
    assertThat(meal[0].mus(), is(75));
    assertThat(meal[2].ingredient().getItemId(), is(ItemPool.WAD_OF_TOFU));
    assertThat(meal[2].effect().getEffectId(), is(EffectPool.EMPATHY));
    assertThat(meal[2].effect().getCount(), is(50));
  }
}
