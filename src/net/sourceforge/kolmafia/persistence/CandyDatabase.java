package net.sourceforge.kolmafia.persistence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.StoreManager;

public class CandyDatabase {
  public static Set<Integer> NO_CANDY = new HashSet<>(); // No candies
  public static Set<Integer> tier0Candy = new HashSet<>(); // Unspaded
  public static Set<Integer> tier1Candy = new HashSet<>(); // Simple
  public static Set<Integer> tier2Candy = new HashSet<>(); // Simple and Complex
  public static Set<Integer> tier3Candy = new HashSet<>(); // Complex
  public static Set<Integer> blacklist = new HashSet<>(); // Candies to not consider

  public static final String NONE = "none";
  public static final String UNSPADED = "unspaded";
  public static final String SIMPLE = "simple";
  public static final String COMPLEX = "complex";

  public static AdventureResult[] potionCandies = null;
  public static AdventureResult[] foodCandies = null;
  public static AdventureResult[] otherCandies = null;

  private CandyDatabase() {}

  public static void categorizeCandies() {
    if (potionCandies != null && foodCandies != null && otherCandies != null) {
      return;
    }

    List<AdventureResult> potions = new ArrayList<>();
    List<AdventureResult> foods = new ArrayList<>();
    List<AdventureResult> others = new ArrayList<>();

    for (Integer itemId : CandyDatabase.tier2Candy) {
      AdventureResult item = ItemPool.get(itemId.intValue());
      if (ConsumablesDatabase.getFullness(item.getName()) > 0) {
        foods.add(item);
      } else if (ItemDatabase.isPotion(item)) {
        potions.add(item);
      } else {
        others.add(item);
      }
    }

    CandyDatabase.potionCandies = potions.toArray(new AdventureResult[potions.size()]);
    CandyDatabase.foodCandies = foods.toArray(new AdventureResult[foods.size()]);
    CandyDatabase.otherCandies = others.toArray(new AdventureResult[others.size()]);
  }

  public static void updatePrices() {
    CandyDatabase.categorizeCandies();
    StoreManager.getMallPrices(CandyDatabase.potionCandies, 0.0f);
    StoreManager.getMallPrices(CandyDatabase.foodCandies, 0.0f);
    StoreManager.getMallPrices(CandyDatabase.otherCandies, 0.0f);
  }

  public static void registerCandy(final Integer itemId, final String type) {
    if (type.equals("candy")) {
      // Unspaded candy
      CandyDatabase.tier0Candy.add(itemId);
      return;
    }

    if (type.equals("candy1")) {
      // Simple candy
      CandyDatabase.tier1Candy.add(itemId);
      CandyDatabase.tier2Candy.add(itemId);
    } else if (type.equals("candy2")) {
      // Complex candy
      CandyDatabase.tier3Candy.add(itemId);
      CandyDatabase.tier2Candy.add(itemId);
    } else {
      return;
    }
  }

  public static final void loadBlacklist() {
    CandyDatabase.blacklist.clear();

    String blacklisted = Preferences.getString("sweetSynthesisBlacklist").trim();
    if (blacklisted.equals("")) {
      return;
    }

    String[] candies = blacklisted.split("\\s*,\\s*");
    for (String candy : candies) {
      AdventureResult[] itemList = ItemFinder.getMatchingItemList(candy, false, null, Match.CANDY);

      // Ignore non-candy
      if (itemList == null) {
        continue;
      }

      // There could be multiple matching items. Should we allow that?
      if (itemList.length != 1) {
        continue;
      }

      int itemId = itemList[0].getItemId();
      CandyDatabase.blacklist.add(itemId);
    }
  }

  public static final boolean isCandyBlacklisted(final int itemId) {
    return CandyDatabase.blacklist.contains(itemId);
  }

  public static final String getCandyType(final int itemId) {
    // We could look in our various candy sets, but more efficient
    // to just look at item attributes
    int attributes = ItemDatabase.getAttributes(itemId);
    return (attributes & ItemDatabase.ATTR_CANDY0) != 0
        ? UNSPADED
        : (attributes & ItemDatabase.ATTR_CANDY1) != 0
            ? SIMPLE
            : (attributes & ItemDatabase.ATTR_CANDY2) != 0 ? COMPLEX : NONE;
  }

  public static final int getEffectTier(final int itemId1, final int itemId2) {
    String candyType1 = CandyDatabase.getCandyType(itemId1);
    String candyType2 = CandyDatabase.getCandyType(itemId2);
    return (candyType1 == NONE || candyType2 == NONE)
        ? 0
        : (candyType1 == SIMPLE && candyType2 == SIMPLE)
            ? 1
            : (candyType1 == COMPLEX && candyType2 == COMPLEX) ? 3 : 2;
  }

  public static final int getEffectTier(final int effectId) {
    switch (effectId) {
      case EffectPool.SYNTHESIS_HOT:
      case EffectPool.SYNTHESIS_COLD:
      case EffectPool.SYNTHESIS_PUNGENT:
      case EffectPool.SYNTHESIS_SCARY:
      case EffectPool.SYNTHESIS_GREASY:
        return 1;
      case EffectPool.SYNTHESIS_STRONG:
      case EffectPool.SYNTHESIS_SMART:
      case EffectPool.SYNTHESIS_COOL:
      case EffectPool.SYNTHESIS_HARDY:
      case EffectPool.SYNTHESIS_ENERGY:
        return 2;
      case EffectPool.SYNTHESIS_GREED:
      case EffectPool.SYNTHESIS_COLLECTION:
      case EffectPool.SYNTHESIS_MOVEMENT:
      case EffectPool.SYNTHESIS_LEARNING:
      case EffectPool.SYNTHESIS_STYLE:
        return 3;
      default:
        return 0;
    }
  }

  public static final int getEffectModulus(final int effectId) {
    switch (effectId) {
      case EffectPool.SYNTHESIS_HOT:
      case EffectPool.SYNTHESIS_STRONG:
      case EffectPool.SYNTHESIS_GREED:
        return 0;
      case EffectPool.SYNTHESIS_COLD:
      case EffectPool.SYNTHESIS_SMART:
      case EffectPool.SYNTHESIS_COLLECTION:
        return 1;
      case EffectPool.SYNTHESIS_PUNGENT:
      case EffectPool.SYNTHESIS_COOL:
      case EffectPool.SYNTHESIS_MOVEMENT:
        return 2;
      case EffectPool.SYNTHESIS_SCARY:
      case EffectPool.SYNTHESIS_HARDY:
      case EffectPool.SYNTHESIS_LEARNING:
        return 3;
      case EffectPool.SYNTHESIS_GREASY:
      case EffectPool.SYNTHESIS_ENERGY:
      case EffectPool.SYNTHESIS_STYLE:
        return 4;
      default:
        return -1;
    }
  }

  public static final int effectTierBase(final int tier) {
    switch (tier) {
      case 1:
        return EffectPool.SYNTHESIS_HOT;
      case 2:
        return EffectPool.SYNTHESIS_STRONG;
      case 3:
        return EffectPool.SYNTHESIS_GREED;
    }
    return -1;
  }

  public static final int FLAG_AVAILABLE = 0x1;
  public static final int FLAG_CHOCOLATE = 0x2;
  public static final int FLAG_NO_BLACKLIST = 0x4;
  public static final int ALL_FLAGS = FLAG_AVAILABLE | FLAG_CHOCOLATE | FLAG_NO_BLACKLIST;
  public static final int ALL_CANDY = FLAG_CHOCOLATE | FLAG_NO_BLACKLIST;

  public static int makeFlags(
      final boolean available, final boolean chocolate, final boolean noblacklist) {
    return (available ? FLAG_AVAILABLE : 0)
        | (chocolate ? FLAG_CHOCOLATE : 0)
        | (noblacklist ? FLAG_NO_BLACKLIST : 0);
  }

  public static int defaultFlags() {
    boolean loggedIn = KoLCharacter.getUserId() > 0;
    boolean available = loggedIn && !KoLCharacter.canInteract();
    boolean chocolate = false;
    boolean noblacklist = false;
    return CandyDatabase.makeFlags(available, chocolate, noblacklist);
  }

  public static Set<Integer> candyForTier(final int tier) {
    return CandyDatabase.candyForTier(tier, CandyDatabase.defaultFlags());
  }

  public static Set<Integer> candyForTier(final int tier, final int flags) {
    if (tier < 0 || tier > 3) {
      return null;
    }

    Set<Integer> candies =
        tier == 0
            ? CandyDatabase.tier0Candy
            : tier == 1
                ? CandyDatabase.tier1Candy
                : tier == 2
                    ? CandyDatabase.tier2Candy
                    : tier == 3 ? CandyDatabase.tier3Candy : null;

    boolean available = (flags & FLAG_AVAILABLE) != 0;
    boolean chocolate = (flags & FLAG_CHOCOLATE) != 0;
    boolean noblacklist = (flags & FLAG_NO_BLACKLIST) != 0;

    // If we don't need to filter the candies, just return raw set
    boolean nofilter = !available && chocolate && noblacklist;
    if (nofilter) {
      return candies;
    }

    // Otherwise, we must filter
    Set<Integer> result = new HashSet<Integer>();

    for (Integer itemId : candies) {
      if (available && InventoryManager.getAccessibleCount(itemId) == 0) {
        continue;
      }
      if (!chocolate && ItemDatabase.isChocolateItem(itemId)) {
        continue;
      }
      if (!noblacklist && CandyDatabase.blacklist.contains(itemId)) {
        continue;
      }
      result.add(itemId);
    }

    return result;
  }

  public static int synthesisResult(final int itemId1, final int itemId2) {
    if (!ItemDatabase.isCandyItem(itemId1) || !ItemDatabase.isCandyItem(itemId2)) {
      return -1;
    }

    int tier = CandyDatabase.getEffectTier(itemId1, itemId2);
    if (tier == 0) {
      return -1;
    }

    int base = CandyDatabase.effectTierBase(tier);
    int modulus = (itemId1 + itemId2) % 5;

    return base + modulus;
  }

  public static Set<Integer> sweetSynthesisPairing(final int effectId, final int itemId1) {
    return CandyDatabase.sweetSynthesisPairing(effectId, itemId1, CandyDatabase.defaultFlags());
  }

  public static Set<Integer> sweetSynthesisPairing(
      final int effectId, final int itemId1, final int flags) {
    Set<Integer> result = new HashSet<Integer>();

    int tier = CandyDatabase.getEffectTier(effectId);
    if (tier < 1 || tier > 3) {
      return result;
    }

    String candyType = CandyDatabase.getCandyType(itemId1);
    if (candyType != SIMPLE && candyType != COMPLEX) {
      return result;
    }

    Set<Integer> candidates = CandyDatabase.NO_CANDY;

    switch (tier) {
      case 1:
        candidates = (candyType == SIMPLE) ? CandyDatabase.tier1Candy : CandyDatabase.NO_CANDY;
        break;
      case 2:
        candidates = (candyType == SIMPLE) ? CandyDatabase.tier3Candy : CandyDatabase.tier1Candy;
        break;
      case 3:
        candidates = (candyType == COMPLEX) ? CandyDatabase.tier3Candy : CandyDatabase.NO_CANDY;
        break;
    }

    int desiredModulus = CandyDatabase.getEffectModulus(effectId);
    boolean available = (flags & FLAG_AVAILABLE) != 0;
    boolean chocolate = (flags & FLAG_CHOCOLATE) != 0;
    boolean noblacklist = (flags & FLAG_NO_BLACKLIST) != 0;

    for (int itemId2 : candidates) {
      if ((itemId1 + itemId2) % 5 != desiredModulus) {
        continue;
      }
      // The caller can give us a chocolate as candy1,
      // whether or not chocolates are allowed for candy2
      if (!chocolate && ItemDatabase.isChocolateItem(itemId2)) {
        continue;
      }
      // The caller can give us a blacklisted candy1, whether
      // or not candy2 is blacklisted
      if (!noblacklist && CandyDatabase.blacklist.contains(itemId2)) {
        continue;
      }
      if (available) {
        // You can synthesize two of the same candy.
        // If using available candy and you only have
        // one, can't reuse it.
        int candy2Count = InventoryManager.getAccessibleCount(itemId2);
        if ((candy2Count == 0) || (itemId1 == itemId2 && candy2Count == 1)) {
          continue;
        }
      }
      result.add(itemId2);
    }

    return result;
  }

  // *** Phase 5 methods ***

  // Here will go fancy code to choose combinations of candies that are
  // either cheap (aftercore) or available (in-run) using the provided
  // Comparators to sort Candy lists appropriately

  // Use ASCENDING_MALL_PRICE_COMPARATOR in aftercore
  // Use DESCENDING_COUNT_COMPARATOR in-run

  // Pseudo-price for a non-tradeable item
  public static final int NON_TRADEABLE_PRICE = 999999999;

  public static class Candy implements Comparable<Candy> {
    private final int itemId;
    private final String name;
    private final boolean isChocolate;
    private int count;
    private int mallprice;

    public Candy(final int itemId) {
      this.itemId = itemId;
      this.name = ItemDatabase.getDataName(itemId);
      this.isChocolate = ItemDatabase.isChocolateItem(itemId);
      this.count = InventoryManager.getAccessibleCount(itemId);
      this.mallprice = ItemDatabase.isTradeable(itemId) ? MallPriceDatabase.getPrice(itemId) : 0;
    }

    @Override
    public boolean equals(final Object o) {
      return (o instanceof Candy) && (this.itemId == ((Candy) o).itemId);
    }

    @Override
    public int compareTo(final Candy o) {
      if (o == null) {
        throw new NullPointerException();
      }

      return this.itemId - o.itemId;
    }

    public int getItemId() {
      return this.itemId;
    }

    public String getName() {
      return this.name;
    }

    public boolean isChocolate() {
      return this.isChocolate;
    }

    public boolean isBlacklisted() {
      return CandyDatabase.blacklist.contains(this.itemId);
    }

    public int getCount() {
      return this.count;
    }

    public int getCost() {
      return this.mallprice == 0 ? CandyDatabase.NON_TRADEABLE_PRICE : this.mallprice;
    }

    public int getMallPrice() {
      return this.mallprice;
    }

    public Candy update() {
      this.count = InventoryManager.getAccessibleCount(this.itemId);
      this.mallprice = MallPriceDatabase.getPrice(this.itemId);
      return this;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  public static List<Candy> itemIdSetToCandyList(Set<Integer> itemIds) {
    ArrayList<Candy> list = new ArrayList<Candy>();

    for (int itemId : itemIds) {
      list.add(new Candy(itemId));
    }

    return list;
  }

  // Compare by lowest mall price, then largest quantity, then alphabetically
  private static class MallPriceComparator implements Comparator<Candy> {
    @Override
    public int compare(Candy o1, Candy o2) {
      int cost1 = o1.getCost();
      int cost2 = o2.getCost();
      if (cost1 != cost2) {
        return cost1 - cost2;
      }
      int count1 = o1.getCount();
      int count2 = o2.getCount();
      if (count1 != count2) {
        return count2 - count1;
      }
      return o1.getName().compareToIgnoreCase(o2.getName());
    }
  }

  public static final Comparator<Candy> ASCENDING_MALL_PRICE_COMPARATOR = new MallPriceComparator();

  // Compare by largest quantity, then by lowest mall price, then alphabetically
  private static class InverseCountComparator implements Comparator<Candy> {
    @Override
    public int compare(Candy o1, Candy o2) {
      int count1 = o1.getCount();
      int count2 = o2.getCount();
      if (count1 != count2) {
        return count2 - count1;
      }
      return o1.getName().compareToIgnoreCase(o2.getName());
    }
  }

  public static final Comparator<Candy> DESCENDING_COUNT_COMPARATOR = new InverseCountComparator();

  public static final Candy[] NO_PAIR = new Candy[0];

  public static Candy[] synthesisPair(final int effectId) {
    return CandyDatabase.synthesisPair(effectId, CandyDatabase.defaultFlags());
  }

  public static Candy[] synthesisPair(final int effectId, final int flags) {
    boolean available = (flags & FLAG_AVAILABLE) != 0;
    return available
        ? CandyDatabase.synthesisPairByCount(effectId, flags)
        : CandyDatabase.synthesisPairByCost(effectId, flags);
  }

  private static Candy[] synthesisPairByCount(final int effectId, final int flags) {
    int tier = CandyDatabase.getEffectTier(effectId);

    List<Candy> candy1List =
        CandyDatabase.itemIdSetToCandyList(CandyDatabase.candyForTier(tier, flags));
    candy1List.sort(DESCENDING_COUNT_COMPARATOR);

    for (Candy candy : candy1List) {
      if (candy.getCount() == 0) {
        // Ran out of available candies
        return NO_PAIR;
      }

      int itemId = candy.getItemId();
      List<Candy> candy2List =
          CandyDatabase.itemIdSetToCandyList(
              CandyDatabase.sweetSynthesisPairing(effectId, itemId, flags));
      candy2List.sort(DESCENDING_COUNT_COMPARATOR);

      for (Candy pairing : candy2List) {
        int count = pairing.getCount();
        if (count == 0) {
          // Nothing left in this list. Select a new candy1
          break;
        }

        if (candy.equals(pairing) && count == 1) {
          // Pairs with itself but only have one.
          continue;
        }

        Candy[] result = new Candy[2];
        result[0] = candy;
        result[1] = pairing;
        return result;
      }
    }

    return NO_PAIR;
  }

  private static Candy[] synthesisPairByCost(final int effectId, final int flags) {
    int tier = CandyDatabase.getEffectTier(effectId);

    int bestCost = Integer.MAX_VALUE;
    Candy candy1 = null;
    Candy candy2 = null;

    List<Candy> candy1List =
        CandyDatabase.itemIdSetToCandyList(CandyDatabase.candyForTier(tier, flags));
    candy1List.sort(ASCENDING_MALL_PRICE_COMPARATOR);

    for (Candy candy : candy1List) {
      int cost1 = candy.getCost();
      if (cost1 > bestCost) {
        break;
      }

      int itemId = candy.getItemId();
      List<Candy> candy2List =
          CandyDatabase.itemIdSetToCandyList(
              CandyDatabase.sweetSynthesisPairing(effectId, itemId, flags));
      candy2List.sort(ASCENDING_MALL_PRICE_COMPARATOR);

      for (Candy pairing : candy2List) {
        int cost2 = pairing.getCost();
        int currentCost = cost1 + cost2;

        if (currentCost >= bestCost) {
          break;
        }

        candy1 = candy;
        candy2 = pairing;
        bestCost = currentCost;
      }
    }

    if (candy1 == null || candy2 == null) {
      return NO_PAIR;
    }

    Candy[] result = new Candy[2];
    result[0] = candy1;
    result[1] = candy2;

    return result;
  }
}
