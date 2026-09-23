package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.utilities.PHPRandom;

public class BlackAndWhiteApronManager {
  private BlackAndWhiteApronManager() {}

  private static final List<MainIngredient> MAIN_INGREDIENTS =
      List.of(
          new MainIngredient("beef", EffectPool.BEEFY_HEART),
          new MainIngredient("chicken", EffectPool.WINNER_WINNER_CHICKEN),
          new MainIngredient("potatoes", EffectPool.SPUDLY),
          new MainIngredient("lettuce", EffectPool.LETTUCE_GO),
          new MainIngredient("pork", EffectPool.PORKED_UP),
          new MainIngredient("flounder", EffectPool.FISHING_FOR_MEAT));

  private static final List<Ingredient> INGREDIENTS =
      List.of(
          otherIngredient(ItemPool.DOUGH, "Restore 100 HP"),
          meatIngredient(ItemPool.GHUOL_EGG, 100),
          famXpIngredient(ItemPool.DRY_NOODLES, 20),
          statsIngredient(ItemPool.SPICES, 100, 100, 100),
          statsIngredient(ItemPool.CRANBERRIES, 0, 150, 0),
          otherIngredient(ItemPool.CHERRY, "1.5x stat gain"),
          statsIngredient(ItemPool.EYE_OF_NEWT, 0, 100, 0),
          effectIngredient(ItemPool.TOMATO, EffectPool.TOMATO_POWER, 50),
          effectIngredient(ItemPool.GRAPEFRUIT, EffectPool.SCARED_STIFF, 50),
          effectIngredient(ItemPool.LEMON, EffectPool.LEMON_ENLIGHTENMENT, 50),
          effectIngredient(ItemPool.OLIVE, EffectPool.SUPERHUMAN_SARCASM, 50),
          effectIngredient(ItemPool.ORANGE, EffectPool.PRONOUNCED_POTENCY, 50),
          effectIngredient(ItemPool.STRAWBERRY, EffectPool.ERMINE_EYES, 50),
          effectIngredient(ItemPool.JABANERO_PEPPER, EffectPool.EAU_DENMITY, 50),
          effectIngredient(ItemPool.CRANBERRIES, EffectPool.CRANBERRY_CORDIALITY, 50),
          effectIngredient(ItemPool.GR8PS, EffectPool.GR8NESS, 50),
          effectIngredient(ItemPool.TANGERINE, EffectPool.ON_THE_SHOULDERS_OF_GIANTS, 50),
          effectIngredient(ItemPool.BLACKBERRY, EffectPool.BLACKBERRY_POLITENESS, 50),
          statsIngredient(ItemPool.BOTTLE_OF_GIN, 75, 0, 0),
          statsIngredient(ItemPool.BOTTLE_OF_VODKA, 0, 0, 75),
          statsIngredient(ItemPool.BOTTLE_OF_WHISKEY, 0, 75, 0),
          statsIngredient(ItemPool.BOTTLE_OF_RUM, 75, 0, 0),
          statsIngredient(ItemPool.TEQUILA, 0, 0, 75),
          statsIngredient(ItemPool.BOXED_WINE, 0, 75, 0),
          effectIngredient(ItemPool.SKEWER, EffectPool.SHARP_WEAPON, 200),
          otherIngredient(ItemPool.BATGUT, "Restore 200 HP"),
          effectIngredient(ItemPool.GNOLLISH_PIE_TIN, EffectPool.LITELY_BAKED, 10),
          statsIngredient(ItemPool.LIHC_EYE, 0, 100, 0),
          statsIngredient(ItemPool.BATGUT, 0, 0, 100),
          statsIngredient(ItemPool.BAT_WING, 0, 100, 0),
          effectIngredient(ItemPool.WAD_OF_TOFU, EffectPool.EMPATHY, 50),
          effectIngredient(ItemPool.KNOB_SAUSAGE, EffectPool.INCREDIBLY_HULKING, 10),
          otherIngredient(ItemPool.MOUNTAIN_STREAM_SODA, "Restore 250 MP"),
          effectIngredient(ItemPool.COCOA_EGGSHELL_FRAGMENT, EffectPool.SUGAR_RUSH, 50),
          otherIngredient(ItemPool.ROYAL_JELLY, "2x stat gain"),
          effectIngredient(ItemPool.DISPLACED_FISH, EffectPool.FISHY, 50),
          meatIngredient(ItemPool.PHILOSOPHERS_SCONE, 500),
          effectIngredient(ItemPool.BUBBLIN_CRUDE, EffectPool.GREASY_VISAGE, 50),
          effectIngredient(ItemPool.SODA_WATER, EffectPool.BUBBLY, 25),
          effectIngredient(ItemPool.ENCHANTED_BEAN, EffectPool.BUBBLY, 500),
          statsIngredient(ItemPool.LONG_PORK, 150, 150, 150),
          famXpIngredient(ItemPool.CENTIPEDE_EGGS, 30),
          effectIngredient(ItemPool.LOOSE_TEETH, EffectPool.TENACITY_OF_THE_SNAPPER, 50),
          effectIngredient(ItemPool.TURTLE_TOTEM, EffectPool.ASTRAL_SHELL, 50),
          effectIngredient(ItemPool.BALLROOM_BLINTZ, EffectPool.ODE, 15),
          effectIngredient(ItemPool.HOT_DATE, EffectPool.WELL_FED, 100),
          effectIngredient(ItemPool.BLACK_PUDDING, EffectPool.WELL_FED, 50),
          statsIngredient(ItemPool.EXOTIC_JUNGLE_FRUIT, 50, 50, 50));

  private static final List<Ingredient> SPECIAL_INGREDIENTS =
      List.of(
          statsIngredient(ItemPool.SLICK_FISH_MEAT, 0, 0, 250),
          statsIngredient(ItemPool.GLISTENING_FISH_MEAT, 0, 250, 0),
          statsIngredient(ItemPool.BEEFY_FISH_MEAT, 250, 0, 0),
          effectIngredient(ItemPool.KIWI, EffectPool.INCREDIBLY_HULKING, 50),
          effectIngredient(ItemPool.COCKTAIL_ONION, EffectPool.COCK_OF_THE_WALK, 50),
          effectIngredient(ItemPool.KUMQUAT, EffectPool.PONDEROUS_POTENCY, 50),
          effectIngredient(ItemPool.RASPBERRY, EffectPool.OCELOT_EYES, 50),
          effectIngredient(ItemPool.DILL, EffectPool.CONTEMPTIBLE_EMANATIONS, 50),
          effectIngredient(ItemPool.TONIC_WATER, EffectPool.CONCENTRATED_CONCENTRATION, 50),
          effectIngredient(ItemPool.FISHY_FISH, EffectPool.FISHY, 100),
          famXpIngredient(ItemPool.BUNNY_LIVER, 30),
          statsIngredient(ItemPool.SPOOKY_FAIRY_GRAVY, 100, 100, 100),
          otherIngredient(ItemPool.GOURD_POTION, "1.5x stat gain"));

  public static MealKit getMealKitOptions(
      AscensionPath.Path path, AscensionClass clazz, int prevEaten) {
    int pathId = path.getId();
    int classId = clazz.getId();

    List<MainIngredient> mainList =
        shuffleIngredients(MAIN_INGREDIENTS, pathId, classId, prevEaten, 0);
    MainIngredient mainIngredient = mainList.getFirst();

    Ingredient[][] meals = new Ingredient[3][];
    for (int i = 0; i < 3; i++) {
      Ingredient[] meal = new Ingredient[5];

      List<Ingredient> ingList = shuffleIngredients(INGREDIENTS, pathId, classId, prevEaten, i);
      for (int option = 0; option < 4; option++) {
        meal[option] = ingList.get(option);
      }

      ingList = shuffleIngredients(SPECIAL_INGREDIENTS, pathId, classId, prevEaten, i);
      meal[4] = ingList.getFirst();
      meals[i] = meal;
    }

    return new MealKit(mainIngredient, meals);
  }

  private static <T> List<T> shuffleIngredients(
      List<T> list, int pathId, int classId, int prevEaten, int mealIdx) {
    int seed = 37 * pathId + 69 * classId + 42 * prevEaten + 111 * mealIdx;
    PHPRandom rng = new PHPRandom(seed);
    List<T> rv = new ArrayList<>(list);
    rng.shuffle(rv);
    return rv;
  }

  public record MealKit(MainIngredient mainIngredient, Ingredient[][] meals) {}

  public record MainIngredient(String name, AdventureResult effect) {
    public MainIngredient(String name, int effectId) {
      this(name, EffectPool.get(effectId, 100));
    }
  }

  public record Ingredient(
      AdventureResult ingredient,
      AdventureResult effect,
      int mus,
      int mys,
      int mox,
      int meat,
      int familiarXp,
      String other) {
    public Ingredient(
        int ingredient,
        AdventureResult effect,
        int mus,
        int mys,
        int mox,
        int meat,
        int familiarXp,
        String other) {
      this(ItemPool.get(ingredient), effect, mus, mys, mox, meat, familiarXp, other);
    }
  }

  private static Ingredient effectIngredient(int ingredient, int effectId, int turns) {
    AdventureResult effect = EffectPool.get(effectId, turns);
    return new Ingredient(ingredient, effect, 0, 0, 0, 0, 0, "");
  }

  private static Ingredient meatIngredient(int ingredient, int meat) {
    return new Ingredient(ingredient, null, 0, 0, 0, meat, 0, "");
  }

  private static Ingredient famXpIngredient(int ingredient, int famXp) {
    return new Ingredient(ingredient, null, 0, 0, 0, 0, famXp, "");
  }

  private static Ingredient statsIngredient(int ingredient, int mus, int mys, int mox) {
    return new Ingredient(ingredient, null, mus, mys, mox, 0, 0, "");
  }

  private static Ingredient otherIngredient(int ingredient, String other) {
    return new Ingredient(ingredient, null, 0, 0, 0, 0, 0, other);
  }
}
