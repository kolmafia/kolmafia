package net.sourceforge.kolmafia.objectpool;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingMisc;
import net.sourceforge.kolmafia.KoLConstants.CraftingRequirements;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.Consumable;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.BarrelShrineRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.CombineMeatRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.StillSuitRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

/**
 * Internal class used to represent a single concoction. It contains all the information needed to
 * actually make the item.
 */
public class Concoction implements Comparable<Concoction> {
  public enum Priority {
    NONE,
    FOOD,
    BOOZE,
    SPLEEN
  }

  private String name;
  private final int hashCode;

  public final AdventureResult concoction;
  private CreateItemRequest request;
  private PurchaseRequest purchaseRequest;

  private final int yield;
  private CraftingType mixingMethod;
  private final EnumSet<CraftingRequirements> mixingRequirements;
  private final EnumSet<CraftingMisc> mixingMisc;
  private final int row;
  public Priority sortOrder;

  private final boolean isReagentPotion;

  private boolean wasPossible;

  private boolean visited;

  private final List<AdventureResult> ingredients;
  private int param;
  private AdventureResult[] ingredientArray;
  private int allocated;
  public static int debugId = Integer.MAX_VALUE;
  public static boolean debug = false;

  public int price;
  public String property;
  public int creatable;
  public int queued;
  public int queuedPulls;
  public int initial;
  public int pullable;
  public int mallable;
  public int total;
  public int visibleTotal;
  public int freeTotal;

  public boolean special;
  public boolean hotdog;
  public boolean fancydog;
  public boolean speakeasy;
  public boolean steelOrgan;

  private Consumable consumable;
  private String effectName;
  private double mainstatGain;

  private static final Set<String> steelOrgans =
      Set.of("steel margarita", "steel lasagna", "steel-scented air freshener");

  public Concoction(
      final AdventureResult concoction,
      final CraftingType mixingMethod,
      final EnumSet<CraftingRequirements> mixingRequirement,
      final EnumSet<CraftingMisc> mixingInfo,
      final int row) {
    this.concoction = concoction;

    this.wasPossible = false;
    this.mixingMethod = mixingMethod;
    this.mixingRequirements = mixingRequirement;
    this.mixingMisc = mixingInfo;
    this.row = row;

    if (concoction == null) {
      this.yield = 1;
      this.name = "unknown";
      this.hashCode = 0;
      this.isReagentPotion = false;
      this.mainstatGain = 0.0f;
      this.effectName = "";
    } else {
      this.yield = Math.max(concoction.getCount(), 1);

      int itemId = concoction.getItemId();
      String name = concoction.getName();
      this.name = itemId == -1 ? name : ItemDatabase.getItemDataName(itemId);
      this.hashCode = this.name.toLowerCase().hashCode();

      this.isReagentPotion = this.mixingMisc.contains(CraftingMisc.TRIPLE_SAUCE);

      this.setConsumptionData();
      if (CombineMeatRequest.getCost(concoction.getItemId()) > 0) {
        this.request = new CombineMeatRequest(this);
      }
      this.setEffectName();
    }

    this.ingredients = new ArrayList<>();
    this.ingredientArray = new AdventureResult[0];

    this.price = -1;
    this.property = null;
    this.special = false;
    this.steelOrgan = steelOrgans.contains(this.name);

    this.resetCalculations();
  }

  public Concoction(final AdventureResult concoction, final CraftingType mixingMethod) {
    this(
        concoction,
        mixingMethod,
        EnumSet.noneOf(CraftingRequirements.class),
        EnumSet.noneOf(CraftingMisc.class),
        0);
  }

  public Concoction(final String name, final int price) {
    this(null, CraftingType.NOCREATE);

    this.name = name;
    this.price = price;

    this.resetCalculations();
    this.setConsumptionData();
    this.setEffectName();
  }

  public Concoction(final String name) {
    this(null, CraftingType.NOCREATE);

    this.name = name;
    this.special = true;

    this.resetCalculations();
    this.setConsumptionData();
    this.setEffectName();
  }

  public Concoction(final String name, final String property) {
    this(null, CraftingType.NOCREATE);

    this.name = name;
    this.property = property;

    this.resetCalculations();
    this.setConsumptionData();
    this.setEffectName();
  }

  public Priority getSortOrder() {
    int itemId = this.concoction == null ? -1 : this.concoction.getItemId();
    if (this.getRawFullness() != null || itemId == ItemPool.QUANTUM_TACO) {
      return Priority.FOOD;
    }

    if (this.getRawInebriety() != null || itemId == ItemPool.SCHRODINGERS_THERMOS) {
      return Priority.BOOZE;
    }

    if (this.getRawSpleenHit() != null) {
      return Priority.SPLEEN;
    }

    return Priority.NONE;
  }

  public void setConsumptionData() {
    this.setConsumptionData(ConsumablesDatabase.getConsumableByName(this.name));
  }

  public void setConsumptionData(Consumable consumable) {
    this.consumable = consumable;

    this.sortOrder = getSortOrder();

    this.setStatGain();
  }

  public void setEffectName() {
    this.effectName = Modifiers.getStringModifier("Item", this.name, "Effect");
  }

  public void setStatGain() {
    final String range =
        switch (KoLCharacter.mainStat()) {
          case MUSCLE -> ConsumablesDatabase.getStatRange(Consumable.MUSCLE, this.consumable);
          case MYSTICALITY -> ConsumablesDatabase.getStatRange(
              Consumable.MYSTICALITY, this.consumable);
          case MOXIE -> ConsumablesDatabase.getStatRange(Consumable.MOXIE, this.consumable);
          default -> "+0.0";
        };
    this.mainstatGain = StringUtilities.parseDouble(range);
  }

  public boolean usesIngredient(int itemId) {
    for (AdventureResult adventureResult : this.ingredientArray) {
      if (adventureResult.getItemId() == itemId) {
        return true;
      }
    }

    return false;
  }

  public boolean usesIngredient(AdventureResult ar) {
    return this.usesIngredient(ar.getItemId());
  }

  public int getYield() {
    if (KoLCharacter.tripleReagent() && this.isReagentPotion()) {
      return 3 * this.yield;
    }

    return this.yield;
  }

  public boolean isReagentPotion() {
    return this.isReagentPotion;
  }

  /*
    From the definition of Comparable<T>:

    This interface imposes a total ordering on the objects of each class
    that implements it. This ordering is referred to as the class's
    natural ordering, and the class's compareTo method is referred to as
    its natural comparison method.

    Lists (and arrays) of objects that implement this interface can be
    sorted automatically by Collections.sort (and Arrays.sort). Objects
    that implement this interface can be used as keys in a sorted map or
    as elements in a sorted set, without the need to specify a
    comparator.

    The natural ordering for a class C is said to be consistent with
    equals if and only if e1.compareTo(e2) == 0 has the same boolean
    value as e1.equals(e2) for every e1 and e2 of class C. Note that null
    is not an instance of any class, and e.compareTo(null) should throw a
    NullPointerException even though e.equals(null) returns false.

    It is strongly recommended (though not required) that natural
    orderings be consistent with equals. This is so because sorted sets
    (and sorted maps) without explicit comparators behave "strangely"
    when they are used with elements (or keys) whose natural ordering is
    inconsistent with equals. In particular, such a sorted set (or sorted
    map) violates the general contract for set (or map), which is defined
    in terms of the equals method.
  */

  /*
    From the definition of Object.equals:

    The equals method implements an equivalence relation on non-null object references:

    - It is reflexive: for any non-null reference value x, x.equals(x)
             should return true.
    - It is symmetric: for any non-null reference values x and y,
             x.equals(y) should return true if and only if y.equals(x) returns
             true.
    - It is transitive: for any non-null reference values x, y, and z, if
             x.equals(y) returns true and y.equals(z) returns true, then
             x.equals(z) should return true.
    - It is consistent: for any non-null reference values x and y,
             multiple invocations of x.equals(y) consistently return true or
             consistently return false, provided no information used in equals
             comparisons on the objects is modified.
    - For any non-null reference value x, x.equals(null) should return
             false.

      Note that it is generally necessary to override the hashCode method
      whenever this method is overridden, so as to maintain the general
      contract for the hashCode method, which states that equal objects
      must have equal hash codes.
  */

  @Override
  public boolean equals(final Object o) {
    // For any non-null reference value x, x.equals(null) should
    // return false.

    if (!(o instanceof Concoction other)) {
      return false;
    }

    // Concoction.compareTo() returns 0 only if the names match
    // ignoring case

    if (this.name == null) {
      return other.name == null;
    }

    if (other.name == null) {
      return false;
    }

    return nameCheckEquals(other);
  }

  private boolean nameCheckEquals(Concoction other) {
    boolean names = this.name.equalsIgnoreCase(other.name);
    boolean id = this.getItemId() == other.getItemId();
    return names && id;
  }

  private int nameCheckCompare(Concoction other) {
    int cname = this.name.compareToIgnoreCase(other.name);
    if (cname != 0) {
      return cname;
    } else {
      return Integer.compare(this.getItemId(), other.getItemId());
    }
  }

  /*
    From the definition of Object.hashcode:

    The general contract of hashCode is:

    - Whenever it is invoked on the same object more than once during an
             execution of a Java application, the hashCode method must
             consistently return the same integer, provided no information used
             in equals comparisons on the object is modified. This integer need
             not remain consistent from one execution of an application to
             another execution of the same application.

    - If two objects are equal according to the equals(Object) method,
             then calling the hashCode method on each of the two objects must
             produce the same integer result.

    - It is not required that if two objects are unequal according to the
             equals(java.lang.Object) method, then calling the hashCode method
             on each of the two objects must produce distinct integer
             results. However, the programmer should be aware that producing
             distinct integer results for unequal objects may improve the
             performance of hash tables.
  */

  @Override
  public int hashCode() {
    // Concoctions are equal only if their names are equal
    // We precalculated the hashCode
    return this.hashCode;
  }

  /*
    From the definition of Comparable.compareTo:

    Compares this object with the specified object for order. Returns a
    negative integer, zero, or a positive integer as this object is less
    than, equal to, or greater than the specified object.

    The implementor must ensure
      sgn(x.compareTo(y)) == -sgn(y.compareTo(x)
    for all x and y. (This implies that x.compareTo(y) must throw an
    exception iff y.compareTo(x) throws an exception.)

    The implementor must also ensure that the relation is transitive:
    (x.compareTo(y)>0 && y.compareTo(z)>0) implies x.compareTo(z)>0.

    Finally, the implementor must ensure that x.compareTo(y)==0 implies
    that sgn(x.compareTo(z)) == sgn(y.compareTo(z)), for all z.

    It is strongly recommended, but not strictly required that
    (x.compareTo(y)==0) == (x.equals(y)). Generally speaking, any class
    that implements the Comparable interface and violates this condition
    should clearly indicate this fact. The recommended language is "Note:
    this class has a natural ordering that is inconsistent with equals."

    In the foregoing description, the notation sgn(expression) designates
    the mathematical signum function, which is defined to return one of
    -1, 0, or 1 according to whether the value of expression is negative,
    zero or positive.
  */

  @Override
  public int compareTo(final Concoction o) {
    // Note that null is not an instance of any class, and
    // e.compareTo(null) should throw a NullPointerException even
    // though e.equals(null) returns false.

    if (o == null) {
      throw new NullPointerException();
    }

    if (this.equals(o)) {
      return 0;
    }

    if (this.name == null) {
      return o.name == null ? 0 : 1;
    }

    if (o.name == null) {
      return -1;
    }

    if (this.sortOrder != o.sortOrder) {
      return this.sortOrder.compareTo(o.sortOrder);
    }

    if (this.sortOrder == Priority.NONE) {
      return nameCheckCompare(o);
    }

    // Sort steel organs to the top.
    if (this.steelOrgan) {
      return -1;
    } else if (o.steelOrgan) {
      return 1;
    }

    if (Preferences.getBoolean("sortByRoom")) {
      int limit;
      boolean thisCantConsume = false;
      boolean oCantConsume = false;

      switch (this.sortOrder) {
        case FOOD -> {
          limit =
              KoLCharacter.getFullnessLimit()
                  - KoLCharacter.getFullness()
                  - ConcoctionDatabase.getQueuedFullness();
          thisCantConsume = this.getFullness() > limit;
          oCantConsume = o.getFullness() > limit;
        }
        case BOOZE -> {
          limit =
              KoLCharacter.getInebrietyLimit()
                  - KoLCharacter.getInebriety()
                  - ConcoctionDatabase.getQueuedInebriety();
          thisCantConsume = this.getInebriety() > limit;
          oCantConsume = o.getInebriety() > limit;
        }
        case SPLEEN -> {
          limit =
              KoLCharacter.getSpleenLimit()
                  - KoLCharacter.getSpleenUse()
                  - ConcoctionDatabase.getQueuedSpleenHit();
          thisCantConsume = this.getSpleenHit() > limit;
          oCantConsume = o.getSpleenHit() > limit;
        }
      }

      if (thisCantConsume != oCantConsume) {
        return thisCantConsume ? 1 : -1;
      }
    }

    double adventures1 = ConsumablesDatabase.getAverageAdventures(this.consumable);
    double adventures2 = ConsumablesDatabase.getAverageAdventures(o.consumable);

    if (adventures1 != adventures2) {
      return adventures2 > adventures1 ? 1 : -1;
    }

    int fullness1 = this.getFullness();
    int fullness2 = o.getFullness();

    if (fullness1 != fullness2) {
      return fullness2 - fullness1;
    }

    int inebriety1 = this.getInebriety();
    int inebriety2 = o.getInebriety();

    if (inebriety1 != inebriety2) {
      return inebriety2 - inebriety1;
    }

    int spleenhit1 = this.getSpleenHit();
    int spleenhit2 = o.getSpleenHit();

    if (spleenhit1 != spleenhit2) {
      return spleenhit2 - spleenhit1;
    }

    double gain1 = this.mainstatGain;
    double gain2 = o.mainstatGain;

    if (gain1 != gain2) {
      return gain2 > gain1 ? 1 : -1;
    }

    return nameCheckCompare(o);
  }

  public AdventureResult getItem() {
    return this.concoction;
  }

  public int getItemId() {
    return this.concoction == null ? -1 : this.concoction.getItemId();
  }

  public String getName() {
    return this.name;
  }

  public int getInitial() {
    return this.initial;
  }

  public int getAvailable() {
    return this.visibleTotal;
  }

  public int getTurnFreeAvailable() {
    return this.freeTotal;
  }

  public int getQueued() {
    return this.queued;
  }

  public int getPrice() {
    return this.price;
  }

  public Integer getRawFullness() {
    return this.consumable != null ? this.consumable.getRawFullness() : null;
  }

  public int getFullness() {
    return this.consumable != null ? this.consumable.getFullness() : 0;
  }

  public Integer getRawInebriety() {
    return this.consumable != null ? this.consumable.getRawInebriety() : null;
  }

  public int getInebriety() {
    return this.consumable != null ? this.consumable.getInebriety() : 0;
  }

  public Integer getRawSpleenHit() {
    return this.consumable != null ? this.consumable.getRawSpleenHit() : null;
  }

  public int getSpleenHit() {
    return this.consumable != null ? this.consumable.getSpleenHit() : 0;
  }

  public String getEffectName() {
    return this.effectName;
  }

  public int getRow() {
    return this.row;
  }

  public CreateItemRequest getRequest() {
    if (this.request == null && this.mixingMethod != CraftingType.NOCREATE) {
      this.request = CreateItemRequest.constructInstance(this);
    }
    return this.request;
  }

  public PurchaseRequest getPurchaseRequest() {
    return this.purchaseRequest;
  }

  public void setPurchaseRequest(final PurchaseRequest purchaseRequest) {
    this.purchaseRequest = purchaseRequest;
  }

  public boolean hasIngredients(final AdventureResult[] ingredients) {
    AdventureResult[] ingredientTest = this.ingredientArray;
    if (ingredientTest.length != ingredients.length) {
      return false;
    }

    int[] ingredientTestIds = new int[ingredients.length];
    for (int j = 0; j < ingredientTestIds.length; ++j) {
      ingredientTestIds[j] = ingredientTest[j].getItemId();
    }

    boolean foundMatch = true;
    for (int j = 0; j < ingredients.length && foundMatch; ++j) {
      foundMatch = false;
      for (int k = 0; k < ingredientTestIds.length && !foundMatch; ++k) {
        foundMatch = ingredients[j].getItemId() == ingredientTestIds[k];
        if (foundMatch) {
          ingredientTestIds[k] = -1;
        }
      }
    }

    return foundMatch;
  }

  public void queue(
      final List<AdventureResult> globalChanges,
      final List<AdventureResult> localChanges,
      final int amount) {
    this.queue(globalChanges, localChanges, amount, true);
  }

  public void queue(
      final List<AdventureResult> globalChanges,
      final List<AdventureResult> localChanges,
      final int amount,
      boolean adjust) {
    if (amount <= 0) {
      return;
    }

    if (this.concoction == null) {
      if (adjust) {
        this.queued += amount;
      }

      return;
    }

    int decrementAmount = Math.min(this.initial, amount);
    int creatableAmount = Math.max(this.creatable, 0);
    int overAmount = Math.min(creatableAmount, amount - decrementAmount);
    int pullAmount = amount - decrementAmount - overAmount;
    if (this.price > 0 || this.property != null || this.special) {
      pullAmount = 0;
    }

    if (pullAmount != 0) {
      ConcoctionDatabase.queuedPullsUsed += pullAmount;
    }

    // Tiny plastic swords and Legend keys are special in that they
    // are not used up.

    if (this.concoction.getItemId() != ItemPool.PLASTIC_SWORD
        && this.concoction.getItemId() != ItemPool.BORIS_KEY
        && this.concoction.getItemId() != ItemPool.JARLSBERG_KEY
        && this.concoction.getItemId() != ItemPool.SNEAKY_PETE_KEY) {
      AdventureResult ingredient = this.concoction.getInstance(decrementAmount);
      AdventureResult.addResultToList(globalChanges, ingredient);
      AdventureResult.addResultToList(localChanges, ingredient);
    }

    int advs = ConcoctionDatabase.getAdventureUsage(this.mixingMethod) * overAmount;
    if (advs != 0) {
      for (int i = 0; i < advs; ++i) {
        if (ConcoctionDatabase.queuedFreeCraftingTurns
            < ConcoctionDatabase.getFreeCraftingTurns()) {
          ++ConcoctionDatabase.queuedFreeCraftingTurns;
        } else ++ConcoctionDatabase.queuedAdventuresUsed;
      }
    }

    if (this.mixingMethod == CraftingType.STILL) {
      ConcoctionDatabase.queuedStillsUsed += overAmount;
    }

    if (this.mixingMethod == CraftingType.CLIPART) {
      ConcoctionDatabase.queuedTomesUsed += overAmount;
    }

    if (this.mixingMethod == CraftingType.TERMINAL) {
      ConcoctionDatabase.queuedExtrudesUsed += overAmount;
    }

    if (adjust) {
      this.queued += amount;
      this.queuedPulls += pullAmount;
    }

    if (this.price > 0) {
      ConcoctionDatabase.queuedMeatSpent += this.price * (amount - decrementAmount - overAmount);
    }

    if (this.price > 0
        || this.property != null
        || this.special
        || !ConcoctionDatabase.isPermittedMethod(this.mixingMethod, this.mixingRequirements)) {
      return;
    }

    // Recipes that yield multiple units require smaller
    // quantities of ingredients.

    int mult = this.getYield();
    int icount = (overAmount + (mult - 1)) / mult;
    for (AdventureResult ingredient : this.ingredientArray) {
      Concoction c = ConcoctionPool.get(ingredient);
      if (c == null) {
        continue;
      }
      c.queue(globalChanges, localChanges, icount * ingredient.getCount(), false);
    }

    // Recipes that yield multiple units might result in
    // extra product which can be used for other recipes.
    // Commented out code could start to handle that
    // int excess = mult * icount - overAmount;
    // if (excess > 0) {}
  }

  public static int getAvailableMeat() {
    // This package assumes that available Meat fits in an int.
    // Currently, we use a long to store it, since there no such limit.
    // Additionally, KoL allows at most 2^24-1 items in any location:
    // inventory, storage, etc.
    //
    // Limit Meat we will consider using to an Integer's worth
    return (int) Math.min(Integer.MAX_VALUE, KoLCharacter.getAvailableMeat());
  }

  public final void resetCalculations() {
    this.initial = -1;
    this.creatable = 0;
    this.pullable = 0;
    this.mallable = 0;
    this.total = 0;
    this.visibleTotal = 0;

    this.allocated = 0;

    if (this.speakeasy) {
      this.initial =
          Math.min(
              Concoction.getAvailableMeat() / this.price,
              3 - Preferences.getInteger("_speakeasyDrinksDrunk"));
      this.creatable = 0;
      this.total = this.initial;
      this.freeTotal = this.initial;
      this.visibleTotal = this.initial;
      return;
    }

    if (this.concoction == null && this.name != null) {
      this.initial =
          this.price > 0
              ? Concoction.getAvailableMeat() / this.price
              : this.property != null ? Preferences.getInteger(property) : this.special ? 1 : 0;
      this.creatable = 0;
      this.total = this.initial;
      this.freeTotal = this.initial;
      this.visibleTotal = this.initial;
    }
  }

  public void setPullable(final int pullable) {
    this.pullable = pullable;
    this.total += pullable;
    this.visibleTotal += pullable;
  }

  public void setPossible(final boolean wasPossible) {
    this.wasPossible = wasPossible;
  }

  public boolean wasPossible() {
    return this.wasPossible;
  }

  public void resetIngredients() {
    this.ingredients.clear();
    this.ingredientArray = new AdventureResult[0];
  }

  public void addIngredient(final AdventureResult ingredient) {
    int itemId = ingredient.getItemId();
    if (itemId >= 0) {
      Set<AdventureResult> uses = ConcoctionDatabase.knownUses.get(itemId);
      if (uses == null) {
        uses = new TreeSet<>();
        ConcoctionDatabase.knownUses.put(ingredient.getItemId(), uses);
      }

      uses.add(this.concoction);
    }

    this.ingredients.add(ingredient);

    this.ingredientArray = new AdventureResult[this.ingredients.size()];
    this.ingredients.toArray(this.ingredientArray);
  }

  // Allow an arbitrary parameter to be set, to indicate creation details
  // that can't be expressed via ingredients.
  public void setParam(int param) {
    this.param = param;
  }

  public int getParam() {
    return this.param;
  }

  public CraftingType getMixingMethod() {
    return this.mixingMethod;
  }

  public void setMixingMethod(final CraftingType mixingMethod) {
    this.mixingMethod = mixingMethod;
  }

  public EnumSet<CraftingRequirements> getRequirements() {
    return this.mixingRequirements;
  }

  public EnumSet<CraftingMisc> getMisc() {
    return this.mixingMisc;
  }

  public AdventureResult[] getIngredients() {
    return this.ingredientArray;
  }

  public void calculate2() {
    if (this.speakeasy) {
      return;
    }

    int id = this.getItemId();
    int maxSuccess = this.initial;
    int minFailure = Integer.MAX_VALUE;
    int guess = maxSuccess + 1;
    ArrayList<Concoction> visited = new ArrayList<>();

    if (id == Concoction.debugId) {
      Concoction.debug = true;
    }

    while (true) {
      int res = this.canMake(guess, visited);

      if (Concoction.debug) {
        RequestLogger.printLine(this.name + ".canMake(" + guess + ") => " + res);
        RequestLogger.printLine();
      }

      if (res >= guess) { // guess was good, try a higher guess
        maxSuccess = guess;
      } else { // guess was too high, try lower
        minFailure = guess;
        // In this situation, the algorithm tends to produce estimates
        // that are way too low.  Bump it up to the midpoint of the
        // range, so that the worst-case behavior is the O(log n) of
        // a binary search, not the O(n) of a linear search.
        res = Math.max(res, (maxSuccess + minFailure) / 2);
      }

      if (maxSuccess + 1 >= minFailure) break;

      guess = Math.min(Math.max(res, maxSuccess + 1), minFailure - 1);

      for (Concoction c : visited) { // clean up for next iteration of this item
        c.allocated = 0;
      }
    }

    for (Concoction c : visited) { // clean up for next item
      c.allocated = 0;
      c.visited = false;
    }

    if (id == Concoction.debugId) {
      Concoction.debug = false;
    }

    this.total = maxSuccess;
    this.creatable = this.total - this.initial;
    if (this.price > 0
        && id != ItemPool.MEAT_PASTE
        && id != ItemPool.MEAT_STACK
        && id != ItemPool.DENSE_STACK) {
      this.creatable -= Concoction.getAvailableMeat() / this.price;
    }
    this.visibleTotal = this.total;
  }

  // Like calculate2, but just calculates turn-free creations.

  public void calculate3() {
    if (this.speakeasy) {
      return;
    }

    int maxSuccess = this.initial;
    int minFailure = Integer.MAX_VALUE;
    int guess = maxSuccess + 1;
    ArrayList<Concoction> visited = new ArrayList<>();

    while (true) {
      int res = this.canMake(guess, visited, true);

      if (res >= guess) {
        maxSuccess = guess;
      } else {
        minFailure = guess;
        res = Math.max(res, (maxSuccess + minFailure) / 2);
      }

      if (maxSuccess + 1 >= minFailure) break;

      guess = Math.min(Math.max(res, maxSuccess + 1), minFailure - 1);

      for (Concoction c : visited) {
        c.allocated = 0;
      }
    }

    for (Concoction c : visited) {
      c.allocated = 0;
      c.visited = false;
    }

    this.freeTotal = maxSuccess;
  }

  // Determine if the requested amount of this item can be made from
  // available ingredients.  Return value must be >= requested if true,
  // < requested if false.  The return value is treated as an estimate
  // of the exact amount that can be made, but isn't assumed to be
  // accurate.  This method will be called with distinct requested
  // values until some N is found to be possible, while N+1 is impossible.

  private int canMake(int requested, ArrayList<Concoction> visited) {
    return canMake(requested, visited, false);
  }

  private int canMake(int requested, ArrayList<Concoction> visited, boolean turnFreeOnly) {
    if (!this.visited) {
      visited.add(this);
      this.visited = true;
    }

    int alreadyHave = this.initial - this.allocated;
    if (alreadyHave < 0
        || requested <= 0) { // Already overspent this ingredient - either due to it being
      // present multiple times in the recipe, or being part of a
      // creation loop with insufficient initial quantities.
      return 0;
    }

    this.allocated += requested;
    int needToMake = requested - alreadyHave;
    if (needToMake > 0 && this.price > 0) {
      Concoction c = ConcoctionDatabase.meatLimit;
      int buyable = c.canMake(needToMake * this.price, visited, turnFreeOnly) / this.price;
      if (Concoction.debug) {
        RequestLogger.printLine(
            "- " + this.name + " limited to " + buyable + " by price " + this.price);
      }
      alreadyHave += buyable;
      buyable = Math.min(buyable, needToMake);
      this.allocated -= buyable;
      needToMake -= buyable;
    }

    switch (this.mixingMethod) {
      case NOCREATE: // No recipe for this item - don't bother with checking
        // ingredients, because there aren't any.
        return alreadyHave;
      case SINGLE_USE:
      case MULTI_USE:
        if (KoLCharacter.inBeecore()
            && ItemDatabase.unusableInBeecore(this.ingredientArray[0].getItemId())) {
          return alreadyHave;
        }

        if (KoLCharacter.inGLover()
            && ItemDatabase.unusableInGLover(this.ingredientArray[0].getItemId())) {
          return alreadyHave;
        }
        break;
      case COINMASTER:
        // Check if Coin Master is available
        PurchaseRequest purchaseRequest = this.purchaseRequest;
        if (purchaseRequest == null || !purchaseRequest.canPurchase()) {
          return alreadyHave;
        }

        return alreadyHave + purchaseRequest.affordableCount();
    }

    if (!ConcoctionDatabase.isPermittedMethod(this.mixingMethod, this.mixingRequirements)
        || Preferences.getBoolean(
            "unknownRecipe" + this.getItemId())) { // Impossible to create any more of this item.
      return alreadyHave;
    }

    switch (this.mixingMethod) {
      case FLOUNDRY:
        return alreadyHave + (ClanLoungeRequest.availableFloundryItem(this.name) ? 1 : 0);
      case BARREL:
        return alreadyHave + (BarrelShrineRequest.availableBarrelItem(this.name) ? 1 : 0);
      case TERMINAL:
        // Check that we know the file for this
        String known = Preferences.getString("sourceTerminalExtrudeKnown");
        if (this.name.equals("Source terminal GRAM chip") && !known.contains("gram.ext")) {
          return alreadyHave;
        }
        if (this.name.equals("Source terminal PRAM chip") && !known.contains("pram.ext")) {
          return alreadyHave;
        }
        if (this.name.equals("Source terminal SPAM chip") && !known.contains("spam.ext")) {
          return alreadyHave;
        }
        if (this.name.equals("Source terminal CRAM chip") && !known.contains("cram.ext")) {
          return alreadyHave;
        }
        if (this.name.equals("Source terminal DRAM chip") && !known.contains("dram.ext")) {
          return alreadyHave;
        }
        if (this.name.equals("Source terminal TRAM chip") && !known.contains("tram.ext")) {
          return alreadyHave;
        }
        if (this.name.equals("software bug") && !known.contains("familiar.ext")) {
          return alreadyHave;
        }
        break;
      case SPACEGATE:
        // If you have one in inventory, you cannot get more
        return this.initial == 0 ? alreadyHave + 1 : alreadyHave;
      case FANTASY_REALM:
        return alreadyHave
            + (StringUtilities.isNumeric(Preferences.getString("_frHoursLeft")) ? 0 : 1);
      case STILLSUIT:
        return StillSuitRequest.canMake() ? 1 : 0;
    }

    if (needToMake <= 0) { // Have enough on hand already.
      // Don't bother with calculating the number creatable:
      // * If this item is part of a creation loop, doing so
      // would result in an infinite recursion, as the excess
      // quantity gets chased around the loop.
      // * It may be completely wasted effort, if another ingredient
      // turns out to be the limiting factor.  If that doesn't turn
      // out to be the case, calc2 will eventually call us again, with
      // a requested amount that's large enough that this code block
      // won't be executed.
      return alreadyHave;
    }

    int yield = this.getYield();
    needToMake = (needToMake + yield - 1) / yield;
    int minMake = Integer.MAX_VALUE;

    // Debugging variable
    int lastMinMake = minMake;

    int len = this.ingredientArray.length;
    for (int i = 0; minMake > 0 && i < len; ++i) {
      AdventureResult ingredient = this.ingredientArray[i];
      Concoction c = ConcoctionPool.get(ingredient);
      if (c == null) continue;
      int count = ingredient.getCount();

      if (i == 0 && len == 2 && this.ingredientArray[1].equals(ingredient)) {
        // Two identical ingredients - this is a moderately common
        // situation, and the algorithm produces better estimates if
        // it considers both quantities at once.
        count += this.ingredientArray[1].getCount();
        len = 1;
      }

      // Special case: if the ingredient is made by a
      // Coinmaster using the item we are trying to make,
      // skip making this item.

      if (c.mixingMethod == CraftingType.COINMASTER) {
        PurchaseRequest purchaseRequest = c.purchaseRequest;
        if (this.getItemId() == purchaseRequest.getCost().getItemId()) {
          minMake = c.initial;
          break;
        }
      }

      minMake = Math.min(minMake, c.canMake(needToMake * count, visited, turnFreeOnly) / count);
      if (Concoction.debug) {
        RequestLogger.printLine(
            "- "
                + this.name
                + (lastMinMake == minMake ? " not limited" : " limited to ")
                + minMake
                + " by "
                + c.name);
        lastMinMake = minMake;
      }
    }

    // Meat paste is an implicit ingredient

    if (minMake > 0
        && (this.mixingMethod == CraftingType.COMBINE || this.mixingMethod == CraftingType.ACOMBINE)
        && (!KoLCharacter.knollAvailable() || KoLCharacter.inZombiecore())) {
      Concoction c = ConcoctionPool.get(ItemPool.MEAT_PASTE);
      minMake = Math.min(minMake, c.canMake(needToMake, visited));
      if (Concoction.debug) {
        RequestLogger.printLine(
            "- "
                + this.name
                + (lastMinMake == minMake ? " not limited" : " limited to " + minMake)
                + " by implicit meat paste");
        lastMinMake = minMake;
      }
    }

    // Adventures are also considered an ingredient

    int advs = ConcoctionDatabase.getAdventureUsage(this.mixingMethod);
    if (minMake > 0 && advs != 0) {
      // Free crafting turns are counted as implicit adventures in this step.
      Concoction c;
      if (this.mixingMethod == CraftingType.SMITH || this.mixingMethod == CraftingType.SSMITH) {
        c =
            (turnFreeOnly
                ? ConcoctionDatabase.turnFreeSmithingLimit
                : ConcoctionDatabase.adventureSmithingLimit);
      } else {
        c = (turnFreeOnly ? ConcoctionDatabase.turnFreeLimit : ConcoctionDatabase.adventureLimit);
      }
      minMake = Math.min(minMake, c.canMake(needToMake * advs, visited, turnFreeOnly) / advs);
      if (Concoction.debug) {
        RequestLogger.printLine(
            "- "
                + this.name
                + (lastMinMake == minMake ? " not limited" : " limited to " + minMake)
                + " by adventures");
        lastMinMake = minMake;
      }
    }

    // Still uses are also considered an ingredient.

    if (minMake > 0 && this.mixingMethod == CraftingType.STILL) {
      Concoction c = ConcoctionDatabase.stillsLimit;
      minMake = Math.min(minMake, c.canMake(needToMake, visited, turnFreeOnly));
      if (Concoction.debug) {
        RequestLogger.printLine(
            "- "
                + this.name
                + (lastMinMake == minMake ? " not limited" : " limited to " + minMake)
                + " by stills");
        lastMinMake = minMake;
      }
    }

    // Tome summons are also considered an ingredient.

    if (minMake > 0 && (this.mixingMethod == CraftingType.CLIPART)) {
      Concoction c = ConcoctionDatabase.clipArtLimit;
      minMake = Math.min(minMake, c.canMake(needToMake, visited, turnFreeOnly));
      if (Concoction.debug) {
        RequestLogger.printLine(
            "- "
                + this.name
                + (lastMinMake == minMake ? " not limited" : " limited to " + minMake)
                + " by tome summons");
        lastMinMake = minMake;
      }
    }

    // Terminal extrudes are also considered an ingredient.

    if (minMake > 0 && (this.mixingMethod == CraftingType.TERMINAL)) {
      Concoction c = ConcoctionDatabase.extrudeLimit;
      minMake = Math.min(minMake, c.canMake(needToMake, visited, turnFreeOnly));
      if (Concoction.debug) {
        RequestLogger.printLine(
            "- "
                + this.name
                + (lastMinMake == minMake ? " not limited" : " limited to " + minMake)
                + " by terminal extrudes");
      }
    }

    if (minMake > 0 && (this.mixingMethod == CraftingType.JARLS)) {
      if (this.name.contains("Staff")) {
        if (KoLConstants.inventory.contains(this.concoction)
            || KoLCharacter.hasEquipped(this.concoction)) {
          return alreadyHave;
        } else {
          return 1;
        }
      }

      if (this.concoction.equals(ItemPool.get(ItemPool.COSMIC_SIX_PACK, 1))) {
        if (Preferences.getBoolean("_cosmicSixPackConjured")) {
          return alreadyHave;
        } else {
          return alreadyHave + 1;
        }
      }
    }

    this.allocated -= Math.min(minMake, needToMake) * yield;
    return alreadyHave + minMake * yield;
  }

  public int getMeatPasteNeeded(final int quantityNeeded) {
    // Avoid mutual recursion.

    int create = quantityNeeded - this.initial;
    if (create <= 0
        || (this.mixingMethod != CraftingType.COMBINE
            && this.mixingMethod != CraftingType.ACOMBINE
            && this.mixingMethod != CraftingType.JEWELRY)
        || (KoLCharacter.knollAvailable() && !KoLCharacter.inZombiecore())) {
      return 0;
    }

    // Count all the meat paste from the different
    // levels in the creation tree.

    int runningTotal = create;
    for (AdventureResult adventureResult : this.ingredientArray) {
      Concoction ingredient = ConcoctionPool.get(adventureResult);

      runningTotal += ingredient.getMeatPasteNeeded(create);
    }

    return runningTotal;
  }

  public int getAdventuresNeeded(final int quantityNeeded) {
    return this.getAdventuresNeeded(quantityNeeded, false);
  }

  public int getAdventuresNeeded(final int quantityNeeded, boolean considerInigos) {
    // If we can't make this item, it costs no adventures to use
    // the quantity on hand.
    if (!ConcoctionDatabase.isPermittedMethod(this.mixingMethod, this.mixingRequirements)) {
      return 0;
    }

    int create = quantityNeeded - this.initial;
    if (create <= 0) {
      return 0;
    }

    // Heuristic/kludge: don't count making base booze by
    // fermenting juniper berries, for example.

    if (concoction.getCount() > 1) {
      return 0;
    }

    int runningTotal = ConcoctionDatabase.getAdventureUsage(this.mixingMethod) * create;
    int yield = this.getYield();
    if (yield > 1) {
      // Determine how many turns will really be used to make
      // this many of the item.
      runningTotal = (runningTotal + yield - 1) / yield;
    }

    // If this creation method takes no adventures, no recursion

    if (runningTotal == 0) {
      return 0;
    }

    // Count adventures from all levels in the creation tree.

    for (AdventureResult adventureResult : this.ingredientArray) {
      Concoction ingredient = ConcoctionPool.get(adventureResult);

      if (ingredient == null) {
        return 0;
      }
      runningTotal += ingredient.getAdventuresNeeded(create);
    }

    if (this.mixingMethod == CraftingType.SMITH || this.mixingMethod == CraftingType.SSMITH) {
      return Math.max(
          runningTotal
              - (!considerInigos
                  ? 0
                  : ConcoctionDatabase.getFreeCraftingTurns()
                      + ConcoctionDatabase.getFreeSmithingTurns()
                      + ConcoctionDatabase.getFreeSmithJewelTurns()),
          0);
    }
    return Math.max(
        runningTotal - (!considerInigos ? 0 : ConcoctionDatabase.getFreeCraftingTurns()), 0);
  }

  /**
   * Returns the string form of this concoction. This is basically the display name for the item
   * created.
   */
  @Override
  public String toString() {
    return concoction == null ? this.name : this.concoction.getName();
  }
}
