package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.utilities.PHPRandom;

public class BlackAndWhiteApronManager {
  private BlackAndWhiteApronManager() {}

  private static final List<MainIngredient> MAIN_INGREDIENTS =
      List.of(
          new MainIngredient("beef", "Beefy Heart"),
          new MainIngredient("chicken", "Winner, Winner, Chicken!"),
          new MainIngredient("potatoes", "Spudly"),
          new MainIngredient("lettuce", "Lettuce Go!"),
          new MainIngredient("pork", "Porked Up"),
          new MainIngredient("flounder", "Fishing for Meat"));

  private static final List<Ingredient> INGREDIENTS =
      List.of(
          otherIngredient("wad of dough", "Restore 100 HP"),
          meatIngredient("ghuol egg", 100),
          famXpIngredient("dry noodles", 20),
          statsIngredient("spices", 100, 100, 100),
          statsIngredient("cranberries", 0, 150, 0),
          otherIngredient("cherry", "1.5x stat gain"),
          statsIngredient("eye of newt", 0, 100, 0),
          effectIngredient("tomato", "Tomato Power", 50),
          effectIngredient("grapefruit", "Scared Stiff", 50),
          effectIngredient("lemon", "Lemon Enlightenment", 50),
          effectIngredient("olive", "Superhuman Sarcasm", 50),
          effectIngredient("orange", "Pronounced Potency", 50),
          effectIngredient("strawberry", "Ermine Eyes", 50),
          effectIngredient("jabañero pepper", "Eau D'enmity", 50),
          effectIngredient("cranberries", "Cranberry Cordiality", 50),
          effectIngredient("gr8ps", "Gr8ness", 50),
          effectIngredient("tangerine", "On the Shoulders of Giants", 50),
          effectIngredient("blackberry", "Blackberry Politeness", 50),
          statsIngredient("bottle of gin", 75, 0, 0),
          statsIngredient("bottle of vodka", 0, 0, 75),
          statsIngredient("bottle of whiskey", 0, 75, 0),
          statsIngredient("bottle of rum", 75, 0, 0),
          statsIngredient("bottle of tequila", 0, 0, 75),
          statsIngredient("boxed wine", 0, 75, 0),
          effectIngredient("skewer", "Sharp Weapon", 200),
          otherIngredient("batgut", "Restore 200 HP"),
          effectIngredient("Gnollish pie tin", "Litely Baked", 10),
          statsIngredient("lihc eye", 0, 100, 0),
          statsIngredient("batgut", 0, 0, 100),
          statsIngredient("bat wing", 0, 100, 0),
          effectIngredient("wad of tofu", "Empathy", 50),
          effectIngredient("Knob sausage", "Incredibly Hulking", 10),
          otherIngredient("Mountain Stream soda", "Restore 250 MP"),
          effectIngredient("cocoa eggshell fragment", "Sugar Rush", 50),
          otherIngredient("royal jelly", "2x stat gain"),
          effectIngredient("displaced fish", "Fishy", 50),
          meatIngredient("philosopher's scone", 500),
          effectIngredient("bubblin' crude", "Greasy Visage", 50),
          effectIngredient("soda water", "Bubbly", 25),
          effectIngredient("enchanted bean", "Bubbly", 500),
          statsIngredient("long pork", 150, 150, 150),
          famXpIngredient("centipede eggs", 30),
          effectIngredient("loose teeth", "Tenacity of the Snapper", 50),
          effectIngredient("turtle totem", "Astral Shell", 50),
          effectIngredient("ballroom blintz", "Ode to Booze", 15),
          effectIngredient("hot date", "Well Fed", 100),
          effectIngredient("black pudding", "Well Fed", 50),
          statsIngredient("exotic jungle fruit", 50, 50, 50));

  private static final List<Ingredient> SPECIAL_INGREDIENTS =
      List.of(
          statsIngredient("slick fish meat", 0, 0, 250),
          statsIngredient("glistening fish meat", 0, 250, 0),
          statsIngredient("beefy fish meat", 250, 0, 0),
          effectIngredient("kiwi", "Incredibly Hulking", 50),
          effectIngredient("cocktail onion", "Cock of the Walk", 50),
          effectIngredient("kumquat", "Ponderous Potency", 50),
          effectIngredient("raspberry", "Ocelot Eyes", 50),
          effectIngredient("dill", "Contemptible Emanations", 50),
          effectIngredient("tonic water", "Concentrated Concentration", 50),
          effectIngredient("fishy fish", "Fishy", 100),
          famXpIngredient("bunny liver", 30),
          statsIngredient("spooky fairy gravy", 100, 100, 100),
          otherIngredient("gourd potion", "1.5x stat gain"));

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
    public MainIngredient(String name, String effectName) {
      this(name, EffectPool.get(EffectDatabase.getEffectId(effectName), 100));
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
        String ingredient,
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

  private static Ingredient effectIngredient(String ingredient, String effectName, int turns) {
    AdventureResult effect = EffectPool.get(EffectDatabase.getEffectId(effectName), turns);
    return new Ingredient(ingredient, effect, 0, 0, 0, 0, 0, "");
  }

  private static Ingredient meatIngredient(String ingredient, int meat) {
    return new Ingredient(ingredient, null, 0, 0, 0, meat, 0, "");
  }

  private static Ingredient famXpIngredient(String ingredient, int famXp) {
    return new Ingredient(ingredient, null, 0, 0, 0, 0, famXp, "");
  }

  private static Ingredient statsIngredient(String ingredient, int mus, int mys, int mox) {
    return new Ingredient(ingredient, null, mus, mys, mox, 0, 0, "");
  }

  private static Ingredient otherIngredient(String ingredient, String other) {
    return new Ingredient(ingredient, null, 0, 0, 0, 0, 0, other);
  }
}
