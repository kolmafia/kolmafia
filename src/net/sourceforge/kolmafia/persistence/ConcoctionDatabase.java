package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.CoinmasterRegistry;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacter.Gender;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLConstants.CraftingMisc;
import net.sourceforge.kolmafia.KoLConstants.CraftingRequirements;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.VYKEACompanionData;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ConcoctionType;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.CrimboCafeRequest;
import net.sourceforge.kolmafia.request.DrinkItemRequest;
import net.sourceforge.kolmafia.request.EatItemRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HellKitchenRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;
import net.sourceforge.kolmafia.request.concoction.StillSuitRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.shop.ShopRow;
import net.sourceforge.kolmafia.shop.ShopRowDatabase;
import net.sourceforge.kolmafia.swingui.ItemManageFrame;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

@SuppressWarnings("incomplete-switch")
public class ConcoctionDatabase {

  private static final Set<AdventureResult> EMPTY_SET = new HashSet<>();
  private static final LockableListModel<CreateItemRequest> creatableList =
      new LockableListModel<>();
  private static final UsableConcoctions usableList = new UsableConcoctions();

  public static String excuse; // reason why creation is impossible

  private static boolean refreshNeeded = true;
  private static boolean recalculateAdventureRange = false;
  public static int refreshLevel = 0;

  public static int queuedAdventuresUsed = 0;
  public static int queuedFreeCraftingTurns = 0;
  public static int queuedStillsUsed = 0;
  public static int queuedTomesUsed = 0;
  public static int queuedExtrudesUsed = 0;
  public static int queuedPullsUsed = 0;
  public static int queuedMeatSpent = 0;
  public static boolean queuedFancyDog = false;
  public static int queuedSpeakeasyDrink = 0;
  public static int queuedSmores = 0;
  public static int queuedAffirmationCookies = 0;
  public static int queuedSpaghettiBreakfast = 0;
  public static int queuedEverfullGlass = 0;
  public static int queuedPirateFork = 0;
  public static boolean queuedMimeShotglass = false;
  public static int lastQueuedMayo = 0;

  private static int queuedFullness = 0;
  public static final LockableListModel<QueuedConcoction> queuedFood = new LockableListModel<>();
  private static final SortedListModel<AdventureResult> queuedFoodIngredients =
      new SortedListModel<>();

  private static int queuedInebriety = 0;
  public static final LockableListModel<QueuedConcoction> queuedBooze = new LockableListModel<>();
  private static final SortedListModel<AdventureResult> queuedBoozeIngredients =
      new SortedListModel<>();

  private static int queuedSpleenHit = 0;
  public static final LockableListModel<QueuedConcoction> queuedSpleen = new LockableListModel<>();
  private static final SortedListModel<AdventureResult> queuedSpleenIngredients =
      new SortedListModel<>();

  public static final LockableListModel<QueuedConcoction> queuedPotions = new LockableListModel<>();
  private static final SortedListModel<AdventureResult> queuedPotionIngredients =
      new SortedListModel<>();

  public static final Concoction stillsLimit = new Concoction(null, CraftingType.NOCREATE);
  public static final Concoction clipArtLimit = new Concoction(null, CraftingType.NOCREATE);
  public static final Concoction extrudeLimit = new Concoction(null, CraftingType.NOCREATE);
  public static final Concoction adventureLimit = new Concoction(null, CraftingType.NOCREATE);
  public static final Concoction adventureSmithingLimit =
      new Concoction(null, CraftingType.NOCREATE);
  public static final Concoction cookingLimit = new Concoction(null, CraftingType.NOCREATE);
  public static final Concoction cocktailcraftingLimit =
      new Concoction(null, CraftingType.NOCREATE);
  public static final Concoction turnFreeLimit = new Concoction(null, CraftingType.NOCREATE);
  public static final Concoction turnFreeCookingLimit = new Concoction(null, CraftingType.NOCREATE);
  public static final Concoction turnFreeCocktailcraftingLimit =
      new Concoction(null, CraftingType.NOCREATE);
  public static final Concoction turnFreeSmithingLimit =
      new Concoction(null, CraftingType.NOCREATE);
  public static final Concoction meatLimit = new Concoction(null, CraftingType.NOCREATE);

  public static final Map<Integer, Set<AdventureResult>> knownUses = new HashMap<>();

  public static final EnumSet<CraftingType> PERMIT_METHOD = EnumSet.noneOf(CraftingType.class);
  public static final Map<CraftingType, Integer> ADVENTURE_USAGE =
      new EnumMap<>(CraftingType.class);
  public static final Map<CraftingType, Long> CREATION_COST = new EnumMap<>(CraftingType.class);
  public static final Map<CraftingType, String> EXCUSE = new EnumMap<>(CraftingType.class);
  public static final EnumSet<CraftingRequirements> REQUIREMENT_MET =
      EnumSet.noneOf(CraftingRequirements.class);

  private static final AdventureResult[] NO_INGREDIENTS = new AdventureResult[0];

  public static final AdventureResult INIGO = EffectPool.get(EffectPool.INIGOS, 0);
  public static final AdventureResult CRAFT_TEA = EffectPool.get(EffectPool.CRAFT_TEA, 0);
  public static final AdventureResult COOKING_CONCENTRATE =
      EffectPool.get(EffectPool.COOKING_CONCENTRATE, 0);

  private static final HashMap<Integer, Concoction> chefStaff = new HashMap<>();
  private static final HashMap<Integer, Concoction> singleUse = new HashMap<>();
  private static final HashMap<Integer, Concoction> multiUse = new HashMap<>();
  private static final HashMap<Integer, Concoction> noodles = new HashMap<>();
  private static final HashMap<Integer, Concoction> meatStack = new HashMap<>();

  private static CraftingType mixingMethod = null;
  private static final EnumSet<CraftingRequirements> requirements =
      EnumSet.noneOf(CraftingRequirements.class);
  private static final EnumSet<CraftingMisc> info = EnumSet.noneOf(CraftingMisc.class);
  private static int row = 0;

  private ConcoctionDatabase() {}

  public static final void resetQueue() {
    LockableListModel<QueuedConcoction> queue = ConcoctionDatabase.queuedFood;
    while (queue.size() > 0) {
      ConcoctionDatabase.pop(ConcoctionType.FOOD);
    }
    queue = ConcoctionDatabase.queuedBooze;
    while (queue.size() > 0) {
      ConcoctionDatabase.pop(ConcoctionType.BOOZE);
    }
    queue = ConcoctionDatabase.queuedSpleen;
    while (queue.size() > 0) {
      ConcoctionDatabase.pop(ConcoctionType.SPLEEN);
    }
    queue = ConcoctionDatabase.queuedPotions;
    while (queue.size() > 0) {
      ConcoctionDatabase.pop(ConcoctionType.POTION);
    }
  }

  public static void resetUsableList() {
    // Construct the usable list from all known concoctions.
    // This includes all items
    ConcoctionDatabase.usableList.fill();
    ConcoctionDatabase.usableList.sort(true);
  }

  private static void reset() {
    // This begins by opening up the data file and preparing
    // a buffered reader; once this is done, every line is
    // examined and float-referenced: once in the name-lookup,
    // and again in the Id lookup.
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("concoctions.txt", KoLConstants.CONCOCTIONS_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        ConcoctionDatabase.addConcoction(data);
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  static {
    reset();
  }

  private static void addConcoction(final String[] data) {
    // Need at least concoction name and mixing method
    if (data.length < 2) {
      return;
    }

    boolean bogus = false;

    ConcoctionDatabase.mixingMethod = null;
    ConcoctionDatabase.requirements.clear();
    ConcoctionDatabase.info.clear();
    ConcoctionDatabase.row = 0;
    String name = data[0];
    String[] mixes = data[1].split("\\s*,\\s*");
    for (String mix : mixes) {
      ConcoctionDatabase.addCraftingData(mix, name);
    }

    if (ConcoctionDatabase.mixingMethod == null) {
      RequestLogger.printLine("No mixing method specified for concoction: " + name);
      bogus = true;
    }

    AdventureResult item = AdventureResult.parseItem(name, true);
    int itemId = item.getItemId();

    if (itemId < 0 && !ConcoctionDatabase.pseudoItemMixingMethod(ConcoctionDatabase.mixingMethod)) {
      RequestLogger.printLine("Unknown concoction: " + name);
      bogus = true;
    }

    AdventureResult[] ingredients = new AdventureResult[data.length - 2];
    int param = 0;
    if (data.length >= 2) {
      for (int i = 2; i < data.length; ++i) {
        if (StringUtilities.isNumeric(data[i])) {
          // Treat all-numeric element as parameter instead of item.
          // Up to 4 such parameters can be given if each fits in a byte.
          // Currently only used for Clip Art
          param = (param << 8) | StringUtilities.parseInt(data[i]);
          continue;
        }
        AdventureResult ingredient = AdventureResult.parseItem(data[i], true);
        if (ingredient == null || ingredient.getItemId() == -1 || ingredient.getName() == null) {
          RequestLogger.printLine("Unknown ingredient (" + data[i] + ") for concoction: " + name);
          bogus = true;
          continue;
        }

        ingredients[i - 2] = ingredient;
      }
    }

    if (!bogus) {
      Concoction concoction =
          new Concoction(
              item,
              ConcoctionDatabase.mixingMethod,
              ConcoctionDatabase.requirements.clone(),
              ConcoctionDatabase.info.clone(),
              ConcoctionDatabase.row);
      concoction.setParam(param);

      if (ingredients.length > 0) {
        for (AdventureResult ingredient : ingredients) {
          if (ingredient == null) {
            // Was a parameter, not an ingredient.
            continue;
          }
          concoction.addIngredient(ingredient);
          if (ingredient.getItemId() == ItemPool.MEAT_STACK) {
            ConcoctionDatabase.meatStack.put(itemId, concoction);
          }
        }
      }

      if (row != 0) {
        ShopRow shopRow = new ShopRow(row, item, concoction.getIngredients());
        ShopRowDatabase.registerShopRow(shopRow, mixingMethod);
      }

      Concoction existing = ConcoctionPool.get(item);
      if (concoction.getMisc().contains(CraftingMisc.MANUAL)
          || (existing != null && existing.getMixingMethod() != CraftingType.NOCREATE)) {
        // Until multiple recipes are supported...
        return;
      }

      ConcoctionPool.set(concoction);

      switch (ConcoctionDatabase.mixingMethod) {
        case STAFF -> ConcoctionDatabase.chefStaff.put(ingredients[0].getItemId(), concoction);
        case SINGLE_USE -> ConcoctionDatabase.singleUse.put(ingredients[0].getItemId(), concoction);
        case MULTI_USE -> ConcoctionDatabase.multiUse.put(ingredients[0].getItemId(), concoction);
      }

      if (ConcoctionDatabase.requirements.contains(CraftingRequirements.PASTA)) {
        ConcoctionDatabase.noodles.put(concoction.getItemId(), concoction);
      }
    }
  }

  public static Concoction chefStaffCreation(final int itemId) {
    return ConcoctionDatabase.chefStaff.get(itemId);
  }

  public static Concoction singleUseCreation(final int itemId) {
    return ConcoctionDatabase.singleUse.get(itemId);
  }

  public static Concoction multiUseCreation(final int itemId) {
    return ConcoctionDatabase.multiUse.get(itemId);
  }

  public static Concoction noodleCreation(final int itemId) {
    return ConcoctionDatabase.noodles.get(itemId);
  }

  public static Concoction meatStackCreation(final int itemId) {
    return ConcoctionDatabase.meatStack.get(itemId);
  }

  private static boolean pseudoItemMixingMethod(final CraftingType mixingMethod) {
    return mixingMethod == CraftingType.SUSHI
        || mixingMethod == CraftingType.VYKEA
        || mixingMethod == CraftingType.STILLSUIT;
  }

  public static final Set<AdventureResult> getKnownUses(final int itemId) {
    Set<AdventureResult> uses = ConcoctionDatabase.knownUses.get(itemId);
    return uses == null ? ConcoctionDatabase.EMPTY_SET : uses;
  }

  public static final Set<AdventureResult> getKnownUses(final AdventureResult item) {
    return ConcoctionDatabase.getKnownUses(item.getItemId());
  }

  public static final boolean isPermittedMethod(
      final CraftingType method, final EnumSet<CraftingRequirements> requirements) {
    // If we can't make anything via this method, punt
    if (!ConcoctionDatabase.PERMIT_METHOD.contains(method)) {
      return false;
    }

    // If we don't meet special creation requirements for this item, punt
    for (CraftingRequirements requirement : requirements) {
      if (!ConcoctionDatabase.REQUIREMENT_MET.contains(requirement)) {
        return false;
      }
    }

    // Otherwise, go for it!
    return true;
  }

  public static final boolean isPermittedMethod(AdventureResult item) {
    return ConcoctionDatabase.checkPermittedMethod(ConcoctionPool.get(item));
  }

  public static final boolean isPermittedMethod(Concoction conc) {
    // Also checks for Coinmaster accessibilty, since we know the item.

    if (conc == null) {
      return false;
    }

    // If this is a Coinmaster creation, the Coinmaster must be accessible
    CraftingType method = conc.getMixingMethod();
    if (method == CraftingType.COINMASTER) {
      String message = conc.getPurchaseRequest().accessible();
      if (message != null) {
        return false;
      }
    }

    EnumSet<CraftingRequirements> requirements = conc.getRequirements();
    return ConcoctionDatabase.isPermittedMethod(method, requirements);
  }

  public static final boolean checkPermittedMethod(Concoction conc) {
    // Same as isPermittedMethod(), but sets excuse.
    // Also checks for Coinmaster accessibilty, since we know the item.
    ConcoctionDatabase.excuse = null;

    if (conc == null) {
      return false;
    }

    CraftingType method = conc.getMixingMethod();

    if (!ConcoctionDatabase.PERMIT_METHOD.contains(method)) {
      ConcoctionDatabase.excuse = ConcoctionDatabase.EXCUSE.get(method);
      return false;
    }

    // If this is a Coinmaster creation, the Coinmaster must be accessible
    if (method == CraftingType.COINMASTER) {
      String message = conc.getPurchaseRequest().accessible();
      if (message != null) {
        ConcoctionDatabase.excuse = message;
        return false;
      }
    }

    EnumSet<CraftingRequirements> requirements = conc.getRequirements();
    for (CraftingRequirements next : requirements) {
      if (!ConcoctionDatabase.REQUIREMENT_MET.contains(next)) {
        ConcoctionDatabase.excuse =
            "You lack a skill or other prerequisite for creating that item ("
                + ConcoctionDatabase.mixingMethodDescription(method, requirements)
                + ").";
        return false;
      }
    }

    return true;
  }

  public static final LockableListModel<AdventureResult> getQueuedIngredients(ConcoctionType type) {
    return switch (type) {
      case FOOD -> ConcoctionDatabase.queuedFoodIngredients;
      case BOOZE -> ConcoctionDatabase.queuedBoozeIngredients;
      case SPLEEN -> ConcoctionDatabase.queuedSpleenIngredients;
      case POTION -> ConcoctionDatabase.queuedPotionIngredients;
      default -> null;
    };
  }

  public static final boolean canQueueFood(final int id) {
    switch (id) {
      case ItemPool.QUANTUM_TACO:
      case ItemPool.MUNCHIES_PILL:
      case ItemPool.WHETSTONE:
      case ItemPool.MAGICAL_SAUSAGE:
        return true;
    }
    if (ConcoctionDatabase.isMayo(id)
        && ConcoctionDatabase.lastQueuedMayo == 0
        && (!ConcoctionDatabase.queuedFood.isEmpty()
            || Preferences.getString("mayoInMouth").equals(""))) {
      AdventureResult workshedItem = CampgroundRequest.getCurrentWorkshedItem();
      if (workshedItem == null || workshedItem.getItemId() != ItemPool.MAYO_CLINIC) {
        return false;
      }
      return true;
    }
    return false;
  }

  public static final boolean isMayo(final int id) {
    return switch (id) {
      case ItemPool.MAYONEX,
          ItemPool.MAYODIOL,
          ItemPool.MAYOSTAT,
          ItemPool.MAYOZAPINE,
          ItemPool.MAYOFLEX -> true;
      default -> false;
    };
  }

  public static final boolean canQueueBooze(final int id) {
    return switch (id) {
      case ItemPool.SCHRODINGERS_THERMOS -> true;
      default -> false;
    };
  }

  public static final void push(final Concoction c, final int quantity) {
    LockableListModel<QueuedConcoction> queue;
    LockableListModel<AdventureResult> queuedIngredients;

    int id = c.getItemId();
    ConsumptionType consumpt = ItemDatabase.getConsumptionType(id);

    if (c.getFullness() > 0
        || consumpt == ConsumptionType.FOOD_HELPER
        || ConcoctionDatabase.canQueueFood(id)) {
      queue = ConcoctionDatabase.queuedFood;
      queuedIngredients = ConcoctionDatabase.queuedFoodIngredients;
      if (ConcoctionDatabase.lastQueuedMayo == ItemPool.MAYODIOL) {
        ConcoctionDatabase.queuedFullness--;
        ConcoctionDatabase.queuedInebriety++;
      }
    } else if (c.getInebriety() > 0
        || consumpt == ConsumptionType.DRINK_HELPER
        || ConcoctionDatabase.canQueueBooze(id)) {
      queue = ConcoctionDatabase.queuedBooze;
      queuedIngredients = ConcoctionDatabase.queuedBoozeIngredients;
    } else if (c.getSpleenHit() > 0) {
      queue = ConcoctionDatabase.queuedSpleen;
      queuedIngredients = ConcoctionDatabase.queuedSpleenIngredients;
    } else {
      queue = ConcoctionDatabase.queuedPotions;
      queuedIngredients = ConcoctionDatabase.queuedPotionIngredients;
    }

    // Handle items that affect more than one organ
    ConcoctionDatabase.queuedFullness += c.getFullness() * quantity;
    ConcoctionDatabase.queuedInebriety += c.getInebriety() * quantity;
    ConcoctionDatabase.queuedSpleenHit += c.getSpleenHit() * quantity;

    // Get current values of things that a concoction can consume
    int meat = ConcoctionDatabase.queuedMeatSpent;
    int pulls = ConcoctionDatabase.queuedPullsUsed;
    int tome = ConcoctionDatabase.queuedTomesUsed;
    int stills = ConcoctionDatabase.queuedStillsUsed;
    int extrudes = ConcoctionDatabase.queuedExtrudesUsed;
    int free = ConcoctionDatabase.queuedFreeCraftingTurns;
    int advs = ConcoctionDatabase.queuedAdventuresUsed;

    // Queue the ingredients used by this concoction
    ArrayList<AdventureResult> ingredients = new ArrayList<>();
    c.queue(queuedIngredients, ingredients, quantity);

    // Adjust lists to account for what just changed
    meat = ConcoctionDatabase.queuedMeatSpent - meat;
    if (meat != 0) {
      AdventureResult.addOrRemoveResultToList(
          queuedIngredients, new AdventureResult(AdventureResult.MEAT_SPENT, meat));
    }

    pulls = ConcoctionDatabase.queuedPullsUsed - pulls;
    if (pulls != 0) {
      AdventureResult.addOrRemoveResultToList(
          queuedIngredients, new AdventureResult(AdventureResult.PULL, pulls));
    }

    tome = ConcoctionDatabase.queuedTomesUsed - tome;
    if (tome != 0) {
      AdventureResult.addOrRemoveResultToList(
          queuedIngredients, new AdventureResult(AdventureResult.TOME, tome));
    }

    extrudes = ConcoctionDatabase.queuedExtrudesUsed - extrudes;
    if (extrudes != 0) {
      AdventureResult.addOrRemoveResultToList(
          queuedIngredients, new AdventureResult(AdventureResult.EXTRUDE, extrudes));
    }

    stills = ConcoctionDatabase.queuedStillsUsed - stills;
    if (stills != 0) {
      AdventureResult.addOrRemoveResultToList(
          queuedIngredients, new AdventureResult(AdventureResult.STILL, stills));
    }

    advs = ConcoctionDatabase.queuedAdventuresUsed - advs;
    if (advs != 0) {
      AdventureResult.addOrRemoveResultToList(
          queuedIngredients, new AdventureResult(AdventureResult.ADV, advs));
    }

    free = ConcoctionDatabase.queuedFreeCraftingTurns - free;
    if (free != 0) {
      AdventureResult.addOrRemoveResultToList(
          queuedIngredients, new AdventureResult(AdventureResult.FREE_CRAFT, free));
    }

    if (c.fancydog) {
      ConcoctionDatabase.queuedFancyDog = true;
    }

    if (c.speakeasy != null) {
      ConcoctionDatabase.queuedSpeakeasyDrink += quantity;
    }

    if (ConcoctionDatabase.isMayo(id)) {
      ConcoctionDatabase.lastQueuedMayo = id;
    } else {
      ConcoctionDatabase.lastQueuedMayo = 0;
    }

    if (c.getInebriety() == 1
        && !ConcoctionDatabase.queuedMimeShotglass
        && InventoryManager.getCount(ItemPool.MIME_SHOTGLASS) > 0
        && !Preferences.getBoolean("_mimeArmyShotglassUsed")) {
      ConcoctionDatabase.queuedInebriety--;
      ConcoctionDatabase.queuedMimeShotglass = true;
    }

    queue.add(
        new QueuedConcoction(
            c, quantity, ingredients, meat, pulls, tome, stills, extrudes, advs, free));

    if (c.getItemId() == ItemPool.SMORE) {
      ConcoctionDatabase.queuedSmores++;
      ConsumablesDatabase.setSmoresData();
    }

    if (c.getItemId() == ItemPool.AFFIRMATION_COOKIE) {
      ConcoctionDatabase.queuedAffirmationCookies++;
    }

    if (c.getItemId() == ItemPool.SPAGHETTI_BREAKFAST) {
      ConcoctionDatabase.queuedSpaghettiBreakfast++;
    }

    if (c.getItemId() == ItemPool.EVERFULL_GLASS) {
      ConcoctionDatabase.queuedEverfullGlass++;
    }

    if (c.getItemId() == ItemPool.PIRATE_FORK) {
      ConcoctionDatabase.queuedPirateFork++;
    }
  }

  public static final QueuedConcoction pop(ConcoctionType type) {
    LockableListModel<QueuedConcoction> queue;
    LockableListModel<AdventureResult> queuedIngredients;

    switch (type) {
      case FOOD:
        queue = ConcoctionDatabase.queuedFood;
        queuedIngredients = ConcoctionDatabase.queuedFoodIngredients;
        break;
      case BOOZE:
        queue = ConcoctionDatabase.queuedBooze;
        queuedIngredients = ConcoctionDatabase.queuedBoozeIngredients;
        break;
      case SPLEEN:
        queue = ConcoctionDatabase.queuedSpleen;
        queuedIngredients = ConcoctionDatabase.queuedSpleenIngredients;
        break;
      case POTION:
        queue = ConcoctionDatabase.queuedPotions;
        queuedIngredients = ConcoctionDatabase.queuedPotionIngredients;
        break;
        // Unreachable
      default:
        return null;
    }

    if (queue.isEmpty()) {
      return null;
    }

    QueuedConcoction qc = queue.remove(queue.size() - 1);
    Concoction c = qc.getConcoction();
    int quantity = qc.getCount();

    c.queued -= quantity;
    ConcoctionDatabase.queuedFullness -= c.getFullness() * quantity;
    ConcoctionDatabase.queuedInebriety -= c.getInebriety() * quantity;
    ConcoctionDatabase.queuedSpleenHit -= c.getSpleenHit() * quantity;

    for (AdventureResult ingredient : qc.getIngredients()) {
      AdventureResult.addOrRemoveResultToList(queuedIngredients, ingredient.getNegation());
    }

    int meat = qc.getMeat();
    if (meat != 0) {
      ConcoctionDatabase.queuedMeatSpent -= meat;
      AdventureResult.addOrRemoveResultToList(
          queuedIngredients, new AdventureResult(AdventureResult.MEAT_SPENT, -meat));
    }

    int pulls = qc.getPulls();
    if (pulls != 0) {
      c.queuedPulls -= pulls;
      ConcoctionDatabase.queuedPullsUsed -= pulls;
      AdventureResult.addOrRemoveResultToList(
          queuedIngredients, new AdventureResult(AdventureResult.PULL, -pulls));
    }

    int tome = qc.getTomes();
    if (tome != 0) {
      ConcoctionDatabase.queuedTomesUsed -= tome;
      AdventureResult.addOrRemoveResultToList(
          queuedIngredients, new AdventureResult(AdventureResult.TOME, -tome));
    }

    int stills = qc.getStills();
    if (stills != 0) {
      ConcoctionDatabase.queuedStillsUsed -= stills;
      AdventureResult.addOrRemoveResultToList(
          queuedIngredients, new AdventureResult(AdventureResult.STILL, -stills));
    }

    int extrudes = qc.getExtrudes();
    if (extrudes != 0) {
      ConcoctionDatabase.queuedExtrudesUsed -= extrudes;
      AdventureResult.addOrRemoveResultToList(
          queuedIngredients, new AdventureResult(AdventureResult.EXTRUDE, -extrudes));
    }

    int advs = qc.getAdventures();
    if (advs != 0) {
      ConcoctionDatabase.queuedAdventuresUsed -= advs;
      AdventureResult.addOrRemoveResultToList(
          queuedIngredients, new AdventureResult(AdventureResult.ADV, -advs));
    }

    int free = qc.getFreeCrafts();
    if (free != 0) {
      ConcoctionDatabase.queuedFreeCraftingTurns -= free;
      AdventureResult.addOrRemoveResultToList(
          queuedIngredients, new AdventureResult(AdventureResult.FREE_CRAFT, -free));
    }

    if (qc.getConcoction().fancydog) {
      ConcoctionDatabase.queuedFancyDog = false;
    }

    if (qc.getConcoction().speakeasy != null) {
      ConcoctionDatabase.queuedSpeakeasyDrink -= quantity;
    }

    if (ConcoctionDatabase.isMayo(c.getItemId())) {
      ConcoctionDatabase.lastQueuedMayo = 0;
    }

    // Is the new last item Mayo ?
    if (!queue.isEmpty()) {
      QueuedConcoction lqc = queue.get(queue.size() - 1);
      Concoction lc = lqc.getConcoction();
      int id = lc.getItemId();
      if (ConcoctionDatabase.isMayo(id)) {
        ConcoctionDatabase.lastQueuedMayo = id;
        if (id == ItemPool.MAYODIOL) {
          ConcoctionDatabase.queuedFullness++;
          ConcoctionDatabase.queuedInebriety--;
        }
      }
    }

    if (ConcoctionDatabase.queuedMimeShotglass && type == ConcoctionType.BOOZE) {
      // Did we remove last 1 fullness drink ?
      boolean shotglassUsed = true;
      for (QueuedConcoction drink : queue) {
        Concoction drinkConc = drink.getConcoction();
        if (drinkConc.getInebriety() == 1) {
          shotglassUsed = false;
        }
      }
      if (shotglassUsed) {
        ConcoctionDatabase.queuedInebriety++;
        ConcoctionDatabase.queuedMimeShotglass = false;
      }
    }

    if (c.getItemId() == ItemPool.SMORE) {
      ConcoctionDatabase.queuedSmores--;
      ConsumablesDatabase.setSmoresData();
      // The last s'more was smaller than the next one, so adjust queued fullness
      ConcoctionDatabase.queuedFullness++;
    }

    if (c.getItemId() == ItemPool.AFFIRMATION_COOKIE) {
      ConcoctionDatabase.queuedAffirmationCookies--;
    }

    if (c.getItemId() == ItemPool.SPAGHETTI_BREAKFAST) {
      ConcoctionDatabase.queuedSpaghettiBreakfast--;
    }

    if (c.getItemId() == ItemPool.EVERFULL_GLASS) {
      ConcoctionDatabase.queuedEverfullGlass--;
    }

    if (c.getItemId() == ItemPool.PIRATE_FORK) {
      ConcoctionDatabase.queuedPirateFork--;
    }

    return qc;
  }

  public static final void addUsableConcoction(final Concoction c) {
    ConcoctionDatabase.usableList.add(c);
    ConcoctionDatabase.usableList.sort();
  }

  public static final UsableConcoctions getUsables() {
    return ConcoctionDatabase.usableList;
  }

  public static final LockableListModel<CreateItemRequest> getCreatables() {
    return ConcoctionDatabase.creatableList;
  }

  public static final LockableListModel<QueuedConcoction> getQueue(ConcoctionType type) {
    return switch (type) {
      case FOOD -> ConcoctionDatabase.queuedFood;
      case BOOZE -> ConcoctionDatabase.queuedBooze;
      case SPLEEN -> ConcoctionDatabase.queuedSpleen;
      case POTION -> ConcoctionDatabase.queuedPotions;
      default -> null;
    };
    // Unreachable
  }

  private static AdventureResult currentConsumptionHelper(ConcoctionType type) {
    return switch (type) {
      case FOOD -> EatItemRequest.currentFoodHelper();
      case BOOZE -> DrinkItemRequest.currentDrinkHelper();
      default -> null;
    };
  }

  private static void clearConsumptionHelper(ConcoctionType type) {
    switch (type) {
      case FOOD -> EatItemRequest.clearFoodHelper();
      case BOOZE -> DrinkItemRequest.clearBoozeHelper();
    }
  }

  private static int lastUnconsumed(int quantity, ConcoctionType type) {
    return quantity
        - (type == ConcoctionType.FOOD
            ? EatItemRequest.foodConsumed
            : type == ConcoctionType.BOOZE ? DrinkItemRequest.boozeConsumed : 0);
  }

  public static final void handleQueue(ConcoctionType type, ConsumptionType consumptionType) {
    // consumptionType can be:
    //
    // ConsumptionType.NONE - create or retrieve items
    // ConsumptionType.EAT - eat food items
    // ConsumptionType.DRINK - drink booze items
    // ConsumptionType.SPLEEN - use spleen items
    // ConsumptionType.GLUTTONOUS_GHOST - binge ghost with food
    // ConsumptionType.SPIRIT_HOBO - binge hobo with booze
    // ConsumptionType.USE - use potions
    // ConsumptionType.USE_MULTIPLE - use potions
    // ConsumptionType.AVATAR_POTION - use potions

    QueuedConcoction currentItem;
    Stack<QueuedConcoction> toProcess = new Stack<>();

    // Remove items in inverse order from the queue and push them on a stack.
    while ((currentItem = ConcoctionDatabase.pop(type)) != null) {
      toProcess.push(currentItem);
    }

    // If we refreshed concoctions while there were items queued,
    // the creatable amounts assume that queued ingredients are
    // already spoken for. Refresh again now that the queue is
    // empty.
    ConcoctionDatabase.refreshConcoctionsNow();

    try (Checkpoint checkpoint = new Checkpoint()) {
      ConcoctionDatabase.handleQueue(toProcess, type, consumptionType);
    }

    // Refresh again now that ingredients have been deducted
    ConcoctionDatabase.refreshConcoctions();
  }

  private static void handleQueue(
      Stack<QueuedConcoction> toProcess, ConcoctionType type, ConsumptionType consumptionType) {
    // Keep track of current consumption helper. These can be
    // "queued" by simply "using" them. Account for that.
    AdventureResult helper = ConcoctionDatabase.currentConsumptionHelper(type);

    // Since items were pushed in inverse order from the queue,
    // popping the stack will get items in actual queued order.
    while (!toProcess.isEmpty()) {
      QueuedConcoction currentItem = toProcess.pop();
      Concoction c = currentItem.getConcoction();
      int quantity = currentItem.getCount();

      if (consumptionType != ConsumptionType.EAT
          && consumptionType != ConsumptionType.DRINK
          && consumptionType != ConsumptionType.SPLEEN) {
        // Binge familiar or create only

        // If it's not an actual item, it's a purchase from a cafe.
        // Those are invalid for anything except "use"
        if (c.getItem() == null) {
          continue;
        }

        ConsumptionType consumpt = ItemDatabase.getConsumptionType(c.getItemId());

        // Skip consumption helpers; we cannot binge a
        // familiar with them and we don't "create" them
        if (consumpt == ConsumptionType.FOOD_HELPER || consumpt == ConsumptionType.DRINK_HELPER) {
          continue;
        }

        // Certain items are virtual consumption
        // helpers, but are "used" first. Skip if
        // bingeing familiar.
        if ((consumptionType == ConsumptionType.GLUTTONOUS_GHOST && consumpt != ConsumptionType.EAT)
            || (consumptionType == ConsumptionType.SPIRIT_HOBO
                && consumpt != ConsumptionType.DRINK)) {
          continue;
        }

        // Retrieve the desired items, creating if necessary
        AdventureResult toConsume = c.getItem().getInstance(quantity);
        InventoryManager.retrieveItem(toConsume);

        if (consumptionType == ConsumptionType.GLUTTONOUS_GHOST
            | consumptionType == ConsumptionType.SPIRIT_HOBO) {
          // Binge the familiar!
          RequestThread.postRequest(UseItemRequest.getInstance(consumptionType, toConsume));
          continue;
        }

        if (consumptionType == ConsumptionType.NONE) {
          // Create only
          continue;
        }

        // CONSUME_USE, CONSUME_MULTIPLE, CONSUME_AVATAR - potions
        // Consume it.
      }

      // "using" the item will either queue a consumption
      // helper or actually consume the item.
      ConcoctionDatabase.consumeItem(c, quantity, consumptionType);

      if (!KoLmafia.permitsContinue()) {
        // Consumption failed.

        // Get current state of appropriate consumption helper
        helper = ConcoctionDatabase.currentConsumptionHelper(type);

        // If there was a consumption helper queued, clear it
        if (helper != null) {
          ConcoctionDatabase.clearConsumptionHelper(type);
        }

        // If the "consumption queue" is not visible to
        // the user, just quit now.
        if (!Preferences.getBoolean("addCreationQueue")) {
          break;
        }

        // Otherwise, put the item and remaining
        // unprocessed items back on queue

        // If there is a consumption helper queued, push that first.
        if (helper != null) {
          ConcoctionDatabase.push(ConcoctionPool.get(helper), helper.getCount());
        }

        // Push current item back on consumption queue
        ConcoctionDatabase.push(c, ConcoctionDatabase.lastUnconsumed(quantity, type));

        // Move items from unprocessed queue back to
        // consumption queue
        while (!toProcess.isEmpty()) {
          // Pop unprocessed items and push back
          // on appropriate consumption queue
          currentItem = toProcess.pop();
          c = currentItem.getConcoction();
          quantity = currentItem.getCount();
          ConcoctionDatabase.push(c, quantity);
        }

        // Done queuing items
        KoLmafia.forceContinue();
        ConcoctionDatabase.refreshConcoctions();
        ConcoctionDatabase.getUsables().sort();
        break;
      }

      // Get current state of appropriate consumption helper
      helper = ConcoctionDatabase.currentConsumptionHelper(type);
    }
  }

  private static void consumeItem(Concoction c, int quantity, ConsumptionType consumptionType) {
    AdventureResult item = c.getItem();

    // If it's food we're consuming, we have a MayoMinder set, and we are autostocking it, do so
    // Don't get Mayostat if it's a 1 fullness food, or it'd be wasted
    // Don't get Mayodiol if it'd cause you to overdrink
    String minderSetting = Preferences.getString("mayoMinderSetting");
    AdventureResult workshedItem = CampgroundRequest.getCurrentWorkshedItem();
    if (item != null
        && consumptionType == ConsumptionType.EAT
        && !ConcoctionDatabase.isMayo(item.getItemId())
        && !minderSetting.equals("")
        && Preferences.getBoolean("autoFillMayoMinder")
        && !(minderSetting.equals("Mayostat") && c.getFullness() == 1)
        && !(minderSetting.equals("Mayodiol")
            && KoLCharacter.getLiverCapacity() == KoLCharacter.getInebriety())
        && workshedItem != null
        && workshedItem.getItemId() == ItemPool.MAYO_CLINIC) {
      int mayoCount = Preferences.getString("mayoInMouth").equals("") ? 0 : 1;
      if (quantity > mayoCount && c.getFullness() != 0) {
        InventoryManager.retrieveItem(minderSetting, quantity - mayoCount);
      }
    }

    // If there's an actual item, it's not from a store
    if (item != null && c.speakeasy == null) {
      // If concoction is a normal item, use normal item acquisition methods.
      if (item.getItemId() > 0) {
        UseItemRequest request;

        // First, consume any items which appear in the inventory.
        int initial = Math.min(quantity, InventoryManager.getCount(item.getItemId()));
        if (initial > 0) {
          request = UseItemRequest.getInstance(consumptionType, item.getInstance(initial));
          RequestThread.postRequest(request);
          quantity -= initial;
        }

        // Second, let UseItemRequest acquire remaining items.
        if (quantity > 0) {
          request = UseItemRequest.getInstance(consumptionType, item.getInstance(quantity));
          RequestThread.postRequest(request);
        }
        return;
      }

      // Otherwise, making item will consume it.
      CreateItemRequest request = CreateItemRequest.getInstance(item.getInstance(quantity));
      request.setQuantityNeeded(quantity);
      RequestThread.postRequest(request);
      return;
    }

    // Otherwise, acquire them from the appropriate cafe.

    String name = c.getName();
    GenericRequest request;

    if (ClanLoungeRequest.isHotDog(name)) {
      request = ClanLoungeRequest.buyHotDogRequest(name);
    } else if (ClanLoungeRequest.isSpeakeasyDrink(name)) {
      request = ClanLoungeRequest.buySpeakeasyDrinkRequest(name);
    } else if (HellKitchenRequest.onMenu(name)) {
      request = new HellKitchenRequest(name);
    } else if (ChezSnooteeRequest.onMenu(name)) {
      request = new ChezSnooteeRequest(name);
    } else if (MicroBreweryRequest.onMenu(name)) {
      request = new MicroBreweryRequest(name);
    } else if (CrimboCafeRequest.onMenu(name)) {
      request = new CrimboCafeRequest(name);
    } else if (StillSuitRequest.isDistillate(name)) {
      request = new StillSuitRequest();
    } else {
      return;
    }

    // You can only buy one item at a time from a cafe
    for (int j = 0; j < quantity; ++j) {
      RequestThread.postRequest(request);
    }
  }

  public static final int getQueuedFullness() {
    return ConcoctionDatabase.queuedFullness;
  }

  public static final int getQueuedInebriety() {
    return ConcoctionDatabase.queuedInebriety;
  }

  public static final int getQueuedSpleenHit() {
    return ConcoctionDatabase.queuedSpleenHit;
  }

  private static List<AdventureResult> getAvailableIngredients() {
    boolean includeCloset = !KoLConstants.closet.isEmpty() && InventoryManager.canUseCloset();
    boolean includeStorage = !KoLConstants.storage.isEmpty() && InventoryManager.canUseStorage();
    boolean includeStash = InventoryManager.canUseClanStash() && !ClanManager.getStash().isEmpty();

    boolean includeQueue =
        !ConcoctionDatabase.queuedFoodIngredients.isEmpty()
            || !ConcoctionDatabase.queuedBoozeIngredients.isEmpty()
            || !ConcoctionDatabase.queuedSpleenIngredients.isEmpty()
            || !ConcoctionDatabase.queuedPotionIngredients.isEmpty();

    if (!includeCloset && !includeStorage && !includeStash && !includeQueue) {
      return KoLConstants.inventory;
    }

    SortedListModel<AdventureResult> availableIngredients = new SortedListModel<>();
    availableIngredients.addAll(KoLConstants.inventory);

    if (includeCloset) {
      for (AdventureResult item : KoLConstants.closet) {
        AdventureResult.addResultToList(availableIngredients, item);
      }
    }

    if (includeStorage) {
      for (AdventureResult item : KoLConstants.storage) {
        AdventureResult.addResultToList(availableIngredients, item);
      }
    }

    if (includeStash) {
      for (AdventureResult item : ClanManager.getStash()) {
        AdventureResult.addResultToList(availableIngredients, item);
      }
    }

    for (AdventureResult ingredient : ConcoctionDatabase.queuedFoodIngredients) {
      if (ingredient.isItem()) {
        AdventureResult.addResultToList(availableIngredients, ingredient.getNegation());
      }
    }

    for (AdventureResult ingredient : ConcoctionDatabase.queuedBoozeIngredients) {
      if (ingredient.isItem()) {
        AdventureResult.addResultToList(availableIngredients, ingredient.getNegation());
      }
    }

    for (AdventureResult ingredient : ConcoctionDatabase.queuedSpleenIngredients) {
      if (ingredient.isItem()) {
        AdventureResult.addResultToList(availableIngredients, ingredient.getNegation());
      }
    }

    for (AdventureResult ingredient : ConcoctionDatabase.queuedPotionIngredients) {
      if (ingredient.isItem()) {
        AdventureResult.addResultToList(availableIngredients, ingredient.getNegation());
      }
    }

    return availableIngredients;
  }

  public static final void deferRefresh(boolean flag) {
    if (flag) {
      ++ConcoctionDatabase.refreshLevel;
    } else if (ConcoctionDatabase.refreshLevel > 0) {
      if (--ConcoctionDatabase.refreshLevel == 0) {
        ConcoctionDatabase.refreshConcoctions(false);
      }
    }
  }

  public static final void setRefreshNeeded(int itemId) {
    switch (ItemDatabase.getConsumptionType(itemId)) {
      case EAT, DRINK, SPLEEN, USE, USE_MULTIPLE, FOOD_HELPER, DRINK_HELPER -> {
        ConcoctionDatabase.setRefreshNeeded(false);
        return;
      }
    }

    switch (itemId) {
        // Items that affect creatability of other items, but
        // aren't explicitly listed in their recipes:
      case ItemPool.WORTHLESS_TRINKET:
      case ItemPool.WORTHLESS_GEWGAW:
      case ItemPool.WORTHLESS_KNICK_KNACK:

        // Interchangeable ingredients, which might have been missed
        // by the getKnownUses check because the recipes are set to
        // use the other possible ingredient:
      case ItemPool.SCHLITZ:
      case ItemPool.WILLER:
      case ItemPool.KETCHUP:
      case ItemPool.CATSUP:
      case ItemPool.DYSPEPSI_COLA:
      case ItemPool.CLOACA_COLA:
      case ItemPool.TITANIUM_UMBRELLA:
      case ItemPool.GOATSKIN_UMBRELLA:
        ConcoctionDatabase.setRefreshNeeded(false);
        return;
    }

    for (AdventureResult use : ConcoctionDatabase.getKnownUses(itemId)) {
      CraftingType method = ConcoctionDatabase.getMixingMethod(use.getItemId());
      EnumSet<CraftingRequirements> requirements =
          ConcoctionDatabase.getRequirements(use.getItemId());

      if (ConcoctionDatabase.isPermittedMethod(method, requirements)) {
        ConcoctionDatabase.setRefreshNeeded(false);
        return;
      }
    }

    for (CoinmasterData coinmaster : CoinmasterRegistry.COINMASTERS) {
      AdventureResult item = coinmaster.getItem();
      if (item != null && itemId == item.getItemId()) {
        ConcoctionDatabase.setRefreshNeeded(false);
        return;
      }
    }
  }

  public static final void setRefreshNeeded(boolean recalculateAdventureRange) {
    ConcoctionDatabase.refreshNeeded = true;

    if (recalculateAdventureRange) {
      ConcoctionDatabase.recalculateAdventureRange = true;
    }
  }

  /**
   * Returns the concoctions which are available given the list of ingredients. The list returned
   * contains formal requests for item creation.
   */
  public static final void refreshConcoctions() {
    ConcoctionDatabase.refreshConcoctions(true);
  }

  public static final void refreshConcoctions(boolean force) {
    if (force) {
      // Remember that refresh is forced, even if deferred
      ConcoctionDatabase.refreshNeeded = true;
    }

    if (!ConcoctionDatabase.refreshNeeded) {
      // No refresh is currently needed
      return;
    }

    if (ConcoctionDatabase.refreshLevel > 0) {
      // Refreshing is deferred
      return;
    }

    if (FightRequest.initializingAfterFight() || ChoiceManager.initializingAfterChoice()) {
      // Implicit defer
      return;
    }

    ConcoctionDatabase.refreshConcoctionsNow();
  }

  public static final synchronized void refreshConcoctionsNow() {
    Preferences.increment("_concoctionDatabaseRefreshes");
    ConcoctionDatabase.refreshNeeded = false;

    List<AdventureResult> availableIngredientsList = ConcoctionDatabase.getAvailableIngredients();

    // In addition to the list, we create a second data structure here for better performance.
    // Because we do many lookups to the available ingredients to see how many there are,
    // having an O(1) lookup helps a lot. Initial size is set at list * 2 with default 0.75 load
    // factor.
    Map<Integer, AdventureResult> availableIngredients =
        new HashMap<>(availableIngredientsList.size() * 2);
    for (AdventureResult item : availableIngredientsList) {
      availableIngredients.put(item.getItemId(), item);
    }

    // Iterate through the concoction table, Initialize each one
    // appropriately depending on whether it is an NPC item, a Coin
    // Master item, or anything else.

    boolean useNPCStores = InventoryManager.canUseNPCStores();
    boolean useCoinmasters = InventoryManager.canUseCoinmasters();

    for (Concoction item : ConcoctionPool.concoctions()) {
      // Initialize all the variables
      item.resetCalculations();

      if (item.speakeasy != null) {
        // Has an item number, but can't appear in inventory
        continue;
      }

      AdventureResult concoction = item.concoction;
      if (concoction == null) {
        continue;
      }

      int itemId = concoction.getItemId();

      if (useNPCStores && NPCStoreDatabase.contains(itemId, true)) {
        if (itemId != ItemPool.FLAT_DOUGH) {
          // Don't buy flat dough from Degrassi Knoll Bakery -
          // buy wads of dough for 20 meat less, instead.

          item.price = NPCStoreDatabase.price(itemId);
          item.initial = concoction.getCount(availableIngredients);
          item.creatable = 0;
          item.total = item.initial;
          item.visibleTotal = item.total;
          continue;
        }
      }

      PurchaseRequest purchaseRequest = item.getPurchaseRequest();
      if (purchaseRequest != null) {
        purchaseRequest.setCanPurchase(useCoinmasters);
        int acquirable = purchaseRequest.canPurchase() ? purchaseRequest.affordableCount() : 0;
        item.price = 0;
        item.initial = concoction.getCount(availableIngredients);
        item.creatable = acquirable;
        item.total = item.initial + acquirable;
        item.visibleTotal = item.total;
        continue;
      }

      // Set initial quantity of all remaining items.

      // Switch to the better of any interchangeable ingredients. Only mutates the first argument.
      ConcoctionDatabase.getIngredients(item, item.getIngredients(), availableIngredientsList);

      item.initial = concoction.getCount(availableIngredients);
      item.price = 0;
      item.creatable = 0;
      item.total = item.initial;
      item.visibleTotal = item.total;
    }

    // Make assessment of availability of mixing methods.
    // This method will also calculate the availability of
    // chefs and bartenders automatically so a second call
    // is not needed.

    ConcoctionDatabase.cachePermitted(availableIngredientsList);

    // Finally, increment through all of the things which are
    // created any other way, making sure that it's a permitted
    // mixture before doing the calculation.

    for (Concoction item : ConcoctionPool.concoctions()) {
      item.calculate2();
      item.calculate3();
    }

    // Now, to update the list of creatables without removing
    // all creatable items.	 We do this by determining the
    // number of items inside of the old list.

    boolean changeDetected = false;
    boolean considerPulls =
        !KoLCharacter.canInteract()
            && !KoLCharacter.isHardcore()
            && ConcoctionDatabase.getPullsBudgeted() > ConcoctionDatabase.queuedPullsUsed;

    for (Concoction item : ConcoctionPool.concoctions()) {
      AdventureResult ar = item.getItem();
      if (ar == null) {
        continue;
      }

      if (considerPulls
          && ar.getItemId() > 0
          && item.getPrice() <= 0
          && ConsumablesDatabase.meetsLevelRequirement(item.getName())
          && ItemDatabase.isAllowed(ar)) {
        item.setPullable(
            Math.min(
                ar.getCount(KoLConstants.storage) - item.queuedPulls,
                ConcoctionDatabase.getPullsBudgeted() - ConcoctionDatabase.queuedPullsUsed));
      } else {
        item.setPullable(0);
      }

      CreateItemRequest instance = CreateItemRequest.getInstance(item, false);

      if (instance == null) {
        continue;
      }

      int creatable = Math.max(item.creatable, 0);
      int pullable = Math.max(item.pullable, 0);

      instance.setQuantityPossible(creatable);
      instance.setQuantityPullable(pullable);

      if (creatable + pullable == 0) {
        if (item.wasPossible()) {
          ConcoctionDatabase.creatableList.remove(instance);
          item.setPossible(false);
          changeDetected = true;
        }
      } else if (!item.wasPossible()) {
        ConcoctionDatabase.creatableList.add(instance);
        item.setPossible(true);
        changeDetected = true;
      }
    }

    if (ConcoctionDatabase.recalculateAdventureRange) {
      ConsumablesDatabase.calculateAllAverageAdventures();
      ConcoctionDatabase.recalculateAdventureRange = false;

      ConcoctionDatabase.queuedFood.touch();
      ConcoctionDatabase.queuedBooze.touch();
      ConcoctionDatabase.queuedSpleen.touch();
    }

    ConcoctionDatabase.creatableList.sort();
    ConcoctionDatabase.usableList.sort();

    // Now tell the GUI about the changes
    ConcoctionDatabase.creatableList.updateFilter(changeDetected);
    ConcoctionDatabase.usableList.updateFilter(changeDetected);
    ConcoctionDatabase.queuedFood.updateFilter(changeDetected);
    ConcoctionDatabase.queuedBooze.updateFilter(changeDetected);
    ConcoctionDatabase.queuedSpleen.updateFilter(changeDetected);
    ConcoctionDatabase.queuedPotions.updateFilter(changeDetected);
  }

  /** Reset concoction stat gains when you've logged in a new character. */
  public static final void resetConcoctionStatGains() {
    for (Concoction item : ConcoctionPool.concoctions()) {
      item.setStatGain();
    }

    ConcoctionDatabase.usableList.sort();
  }

  public static final void resetEffects() {
    for (Concoction item : ConcoctionPool.concoctions()) {
      item.setEffectName();
    }

    ConcoctionDatabase.usableList.sort();
  }

  private static void calculateBasicItems(final List<AdventureResult> availableIngredients) {
    // Meat paste and meat stacks can be created directly
    // and are dependent upon the amount of meat available.

    ConcoctionDatabase.setBuyableItem(availableIngredients, ItemPool.MEAT_PASTE, 10);
    ConcoctionDatabase.setBuyableItem(availableIngredients, ItemPool.MEAT_STACK, 100);
    ConcoctionDatabase.setBuyableItem(availableIngredients, ItemPool.DENSE_STACK, 1000);
  }

  private static void setBuyableItem(
      final List<AdventureResult> availableIngredients, final int itemId, final int price) {
    Concoction creation = ConcoctionPool.get(itemId);
    if (creation == null) {
      return;
    }

    creation.initial = ItemPool.get(itemId, 1).getCount(availableIngredients);
    creation.price = price;
    creation.creatable = 0;
    creation.total = creation.initial;
    creation.visibleTotal = creation.total;
  }

  /** Utility method used to cache the current permissions on item creation. */
  private static final AdventureResult THORS_PLIERS = ItemPool.get(ItemPool.THORS_PLIERS, 1);

  private static void cachePermitted(final List<AdventureResult> availableIngredients) {
    int toolCost = KoLCharacter.inBadMoon() ? 500 : 1000;
    boolean willBuyTool =
        KoLCharacter.getAvailableMeat() >= toolCost && InventoryManager.canUseNPCStores();
    boolean willBuyServant =
        Preferences.getBoolean("autoRepairBoxServants")
            && !KoLCharacter.inGLover()
            && (InventoryManager.canUseMall() || InventoryManager.canUseClanStash());

    // Adventures are considered Item #0 in the event that the
    // concoction will use ADVs.

    ConcoctionDatabase.adventureLimit.total =
        KoLCharacter.getAdventuresLeft() + ConcoctionDatabase.getFreeCraftingTurns();
    ConcoctionDatabase.adventureLimit.initial =
        ConcoctionDatabase.adventureLimit.total - ConcoctionDatabase.queuedAdventuresUsed;
    ConcoctionDatabase.adventureLimit.creatable = 0;
    ConcoctionDatabase.adventureLimit.visibleTotal = ConcoctionDatabase.adventureLimit.total;

    // Adventures are considered Item #0 in the event that the
    // concoction will use ADVs.

    ConcoctionDatabase.adventureSmithingLimit.total =
        KoLCharacter.getAdventuresLeft()
            + ConcoctionDatabase.getFreeCraftingTurns()
            + ConcoctionDatabase.getFreeSmithingTurns();
    ConcoctionDatabase.adventureSmithingLimit.initial =
        ConcoctionDatabase.adventureSmithingLimit.total - ConcoctionDatabase.queuedAdventuresUsed;
    ConcoctionDatabase.adventureSmithingLimit.creatable = 0;
    ConcoctionDatabase.adventureSmithingLimit.visibleTotal =
        ConcoctionDatabase.adventureSmithingLimit.total;

    ConcoctionDatabase.cookingLimit.total =
        KoLCharacter.getAdventuresLeft()
            + ConcoctionDatabase.getFreeCraftingTurns()
            + ConcoctionDatabase.getFreeCookingTurns();
    ConcoctionDatabase.cookingLimit.initial =
        ConcoctionDatabase.cookingLimit.total - ConcoctionDatabase.queuedAdventuresUsed;
    ConcoctionDatabase.cookingLimit.creatable = 0;
    ConcoctionDatabase.cookingLimit.visibleTotal = ConcoctionDatabase.cookingLimit.total;

    ConcoctionDatabase.cocktailcraftingLimit.total =
        KoLCharacter.getAdventuresLeft()
            + ConcoctionDatabase.getFreeCraftingTurns()
            + ConcoctionDatabase.getFreeCocktailcraftingTurns();
    ConcoctionDatabase.cocktailcraftingLimit.initial =
        ConcoctionDatabase.cocktailcraftingLimit.total - ConcoctionDatabase.queuedAdventuresUsed;
    ConcoctionDatabase.cocktailcraftingLimit.creatable = 0;
    ConcoctionDatabase.cocktailcraftingLimit.visibleTotal =
        ConcoctionDatabase.cocktailcraftingLimit.total;

    // If we want to do turn-free crafting, we can only use free turns in lieu of adventures.

    ConcoctionDatabase.turnFreeLimit.total = ConcoctionDatabase.getFreeCraftingTurns();
    ConcoctionDatabase.turnFreeLimit.initial =
        ConcoctionDatabase.turnFreeLimit.total - ConcoctionDatabase.queuedFreeCraftingTurns;
    ConcoctionDatabase.turnFreeLimit.creatable = 0;
    ConcoctionDatabase.turnFreeLimit.visibleTotal = ConcoctionDatabase.turnFreeLimit.total;

    // If we want to do turn-free smithing, we can only use free turns in lieu of adventures.
    // Smithing can't be queued

    ConcoctionDatabase.turnFreeSmithingLimit.total =
        ConcoctionDatabase.getFreeCraftingTurns() + ConcoctionDatabase.getFreeSmithingTurns();
    ConcoctionDatabase.turnFreeSmithingLimit.initial =
        ConcoctionDatabase.turnFreeSmithingLimit.total - ConcoctionDatabase.queuedFreeCraftingTurns;
    ConcoctionDatabase.turnFreeSmithingLimit.creatable = 0;
    ConcoctionDatabase.turnFreeSmithingLimit.visibleTotal =
        ConcoctionDatabase.turnFreeSmithingLimit.total;

    ConcoctionDatabase.turnFreeCookingLimit.total =
        ConcoctionDatabase.getFreeCraftingTurns() + ConcoctionDatabase.getFreeCookingTurns();
    ConcoctionDatabase.turnFreeCookingLimit.initial =
        ConcoctionDatabase.turnFreeCookingLimit.total - ConcoctionDatabase.queuedFreeCraftingTurns;
    ConcoctionDatabase.turnFreeCookingLimit.creatable = 0;
    ConcoctionDatabase.turnFreeCookingLimit.visibleTotal =
        ConcoctionDatabase.turnFreeCookingLimit.total;

    ConcoctionDatabase.turnFreeCocktailcraftingLimit.total =
        ConcoctionDatabase.getFreeCraftingTurns()
            + ConcoctionDatabase.getFreeCocktailcraftingTurns();
    ConcoctionDatabase.turnFreeCocktailcraftingLimit.initial =
        ConcoctionDatabase.turnFreeCocktailcraftingLimit.total
            - ConcoctionDatabase.queuedFreeCraftingTurns;
    ConcoctionDatabase.turnFreeCocktailcraftingLimit.creatable = 0;
    ConcoctionDatabase.turnFreeCocktailcraftingLimit.visibleTotal =
        ConcoctionDatabase.turnFreeCocktailcraftingLimit.total;

    // Stills are also considered Item #0 in the event that the
    // concoction will use stills.

    ConcoctionDatabase.stillsLimit.total = KoLCharacter.getStillsAvailable();
    ConcoctionDatabase.stillsLimit.initial =
        ConcoctionDatabase.stillsLimit.total - ConcoctionDatabase.queuedStillsUsed;
    ConcoctionDatabase.stillsLimit.creatable = 0;
    ConcoctionDatabase.stillsLimit.visibleTotal = ConcoctionDatabase.stillsLimit.total;

    // Tomes are also also also considered Item #0 in the event that the
    // concoction requires a tome summon.

    String pref = KoLCharacter.canInteract() ? "_clipartSummons" : "tomeSummons";
    ConcoctionDatabase.clipArtLimit.total = 3 - Preferences.getInteger(pref);
    ConcoctionDatabase.clipArtLimit.initial =
        ConcoctionDatabase.clipArtLimit.total - ConcoctionDatabase.queuedTomesUsed;
    ConcoctionDatabase.clipArtLimit.creatable = 0;
    ConcoctionDatabase.clipArtLimit.visibleTotal = ConcoctionDatabase.clipArtLimit.total;

    // Terminal Extrudes are also limited

    ConcoctionDatabase.extrudeLimit.total = 3 - Preferences.getInteger("_sourceTerminalExtrudes");
    ConcoctionDatabase.extrudeLimit.initial =
        ConcoctionDatabase.extrudeLimit.total - ConcoctionDatabase.queuedExtrudesUsed;
    ConcoctionDatabase.extrudeLimit.creatable = 0;
    ConcoctionDatabase.extrudeLimit.visibleTotal = ConcoctionDatabase.extrudeLimit.total;

    // Meat is also also considered Item #0 in the event that the
    // concoction will create paste/stacks or buy NPC items.

    ConcoctionDatabase.meatLimit.total = Concoction.getAvailableMeat();
    ConcoctionDatabase.meatLimit.initial =
        ConcoctionDatabase.meatLimit.total - ConcoctionDatabase.queuedMeatSpent;
    ConcoctionDatabase.meatLimit.creatable = 0;
    ConcoctionDatabase.meatLimit.visibleTotal = ConcoctionDatabase.meatLimit.total;

    ConcoctionDatabase.calculateBasicItems(availableIngredients);

    // Clear the maps
    ConcoctionDatabase.REQUIREMENT_MET.clear();
    ConcoctionDatabase.PERMIT_METHOD.clear();
    ConcoctionDatabase.ADVENTURE_USAGE.clear();
    ConcoctionDatabase.CREATION_COST.clear();
    ConcoctionDatabase.EXCUSE.clear();
    int freeCrafts = ConcoctionDatabase.getFreeCraftingTurns();

    if (KoLCharacter.getGender() == Gender.MALE) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.MALE);
    } else {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.FEMALE);
    }

    // It is never possible to create items which are flagged
    // NOCREATE

    // It is always possible to create items through meat paste
    // combination.

    ConcoctionDatabase.PERMIT_METHOD.add(CraftingType.COMBINE);
    ConcoctionDatabase.CREATION_COST.put(CraftingType.COMBINE, 10L);
    ConcoctionDatabase.ADVENTURE_USAGE.put(CraftingType.COMBINE, 0);

    // Un-untinkerable Amazing Ideas
    ConcoctionDatabase.PERMIT_METHOD.add(CraftingType.ACOMBINE);
    ConcoctionDatabase.CREATION_COST.put(CraftingType.ACOMBINE, 10L);
    ConcoctionDatabase.ADVENTURE_USAGE.put(CraftingType.ACOMBINE, 0);

    // The gnomish tinkerer is available if the person is in a
    // gnome sign and they can access the Desert Beach.

    if (KoLCharacter.gnomadsAvailable() && !KoLCharacter.inZombiecore()) {
      permitNoCost(CraftingType.GNOME_TINKER);
    }
    ConcoctionDatabase.EXCUSE.put(
        CraftingType.GNOME_TINKER, "Only gnome signs can use the Supertinkerer.");
    if (KoLCharacter.inZombiecore()) {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.GNOME_TINKER, "Zombies cannot use the Supertinkerer.");
    }

    // Smithing of items is possible whenever the person
    // has a hammer.

    if (InventoryManager.hasItem(ItemPool.TENDER_HAMMER) || willBuyTool) {
      ConcoctionDatabase.PERMIT_METHOD.add(CraftingType.SMITH);
      ConcoctionDatabase.CREATION_COST.put(CraftingType.SMITH, 0L);
      ConcoctionDatabase.ADVENTURE_USAGE.put(CraftingType.SMITH, 1);
    }

    if (InventoryManager.hasItem(ItemPool.GRIMACITE_HAMMER)) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.GRIMACITE);
    }

    // Advanced smithing is available whenever the person can
    // smith.  The appropriate skill is checked separately.

    if (ConcoctionDatabase.PERMIT_METHOD.contains(CraftingType.SMITH)) {
      ConcoctionDatabase.PERMIT_METHOD.add(CraftingType.SSMITH);
      ConcoctionDatabase.CREATION_COST.put(CraftingType.SSMITH, 0L);
      ConcoctionDatabase.ADVENTURE_USAGE.put(CraftingType.SSMITH, 1);
    }

    // Standard smithing is also possible if the person is in
    // a knoll sign.

    if (KoLCharacter.knollAvailable() && !KoLCharacter.inZombiecore()) {
      permitNoCost(CraftingType.SMITH);
    }

    if (KoLCharacter.canSmithWeapons()) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.SUPER_MEATSMITHING);
    }

    if (KoLCharacter.canSmithArmor()) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.ARMORCRAFTINESS);
    }

    // Jewelry making is possible as long as the person has the
    // appropriate pliers.
    if (InventoryManager.hasItem(ItemPool.JEWELRY_PLIERS)
        || ConcoctionDatabase.THORS_PLIERS.getCount(KoLConstants.closet) > 0
        || ConcoctionDatabase.THORS_PLIERS.getCount(KoLConstants.inventory) > 0
        || InventoryManager.getEquippedCount(ConcoctionDatabase.THORS_PLIERS) > 0) {
      permitNoCost(CraftingType.JEWELRY);
    }

    if (KoLCharacter.canCraftExpensiveJewelry()) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.EXPENSIVE);
    }

    // Star chart recipes are always available to all players.

    permitNoCost(CraftingType.STARCHART);
    permitNoCost(CraftingType.MULTI_USE);
    permitNoCost(CraftingType.SINGLE_USE);
    permitNoCost(CraftingType.SUGAR_FOLDING);

    // Pixel recipes are not available in Kingdom of Exploathing
    if (!KoLCharacter.isKingdomOfExploathing()) {
      permitNoCost(CraftingType.PIXEL);
    }

    // A rolling pin or unrolling pin can be always used in item
    // creation because we can get the same effect even without the
    // tool.

    permitNoCost(CraftingType.ROLLING_PIN);

    // Rodoric will make chefstaves for mysticality class
    // characters who can get to the guild.

    if (KoLCharacter.isMysticalityClass() && KoLCharacter.getGuildStoreOpen()) {
      permitNoCost(CraftingType.STAFF);
    }
    ConcoctionDatabase.EXCUSE.put(
        CraftingType.STAFF, "Only mysticality classes can make chefstaves.");

    // Phineas will make things for Seal Clubbers who have defeated
    // their Nemesis, and hence have their ULEW

    if (InventoryManager.hasItem(ItemPool.SLEDGEHAMMER_OF_THE_VAELKYR)) {
      permitNoCost(CraftingType.PHINEAS);
    }
    ConcoctionDatabase.EXCUSE.put(
        CraftingType.PHINEAS, "Only Seal Clubbers who have defeated Gorgolok can use Phineas.");

    // It's not possible to ask Uncle Crimbo 2005 to make toys
    // It's not possible to ask Ugh Crimbo 2006 to make toys
    // It's not possible to ask Uncle Crimbo 2007 to make toys
    // It's not possible to ask Uncle Crimbo 2012 to make toys

    // Next, increment through all the box servant creation methods.
    // This allows future appropriate calculation for cooking/drinking.

    ConcoctionPool.get(ItemPool.CHEF).calculate2();
    ConcoctionPool.get(ItemPool.CLOCKWORK_CHEF).calculate2();
    ConcoctionPool.get(ItemPool.BARTENDER).calculate2();
    ConcoctionPool.get(ItemPool.CLOCKWORK_BARTENDER).calculate2();

    // Cooking is permitted, so long as the person has an oven or a
    // range installed in their kitchen

    if (KoLCharacter.hasOven() || KoLCharacter.hasRange() || KoLCharacter.inWereProfessor()) {
      permitNoCost(CraftingType.COOK);
    }
    ConcoctionDatabase.EXCUSE.put(CraftingType.COOK, "You cannot cook without an oven or a range.");

    // If we have a range and a chef installed, cooking fancy foods
    // costs no adventure. If we have no chef, cooking takes
    // adventures unless we have free crafts.

    // If you don't have a range, you can't cook fancy food
    // We could auto buy & install a range if the character
    // has at least 1,000 Meat and autoSatisfyWithNPCs = true
    if (!KoLCharacter.hasRange() && !willBuyTool) {
      ConcoctionDatabase.ADVENTURE_USAGE.put(CraftingType.COOK_FANCY, 0);
      ConcoctionDatabase.CREATION_COST.put(CraftingType.COOK_FANCY, 0L);
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.COOK_FANCY, "You cannot cook fancy foods without a range.");
    }
    // If you have (or will have) a chef, fancy cooking is free
    else if (KoLCharacter.hasChef()
        || willBuyServant
        || ConcoctionDatabase.isAvailable(ItemPool.CHEF, ItemPool.CLOCKWORK_CHEF)) {
      ConcoctionDatabase.PERMIT_METHOD.add(CraftingType.COOK_FANCY);
      ConcoctionDatabase.ADVENTURE_USAGE.put(CraftingType.COOK_FANCY, 0);
      ConcoctionDatabase.CREATION_COST.put(
          CraftingType.COOK_FANCY, MallPriceDatabase.getPrice(ItemPool.CHEF) / 90);
    }
    // We might not care if cooking takes adventures
    else if (Preferences.getBoolean("requireBoxServants") && !KoLCharacter.inGLover()) {
      ConcoctionDatabase.ADVENTURE_USAGE.put(CraftingType.COOK_FANCY, 0);
      ConcoctionDatabase.CREATION_COST.put(CraftingType.COOK_FANCY, 0L);
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.COOK_FANCY,
          "You have chosen not to cook fancy food without a chef-in-the-box.");
    }
    // Otherwise, spend those adventures!
    else {
      if (KoLCharacter.getAdventuresLeft() + freeCrafts + getFreeCookingTurns() > 0) {
        ConcoctionDatabase.PERMIT_METHOD.add(CraftingType.COOK_FANCY);
      }
      ConcoctionDatabase.ADVENTURE_USAGE.put(CraftingType.COOK_FANCY, 1);
      ConcoctionDatabase.CREATION_COST.put(CraftingType.COOK_FANCY, 0L);
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.COOK_FANCY, "You cannot cook fancy foods without adventures.");
    }

    // Cooking may require an additional skill.

    if (KoLCharacter.canSummonReagent()) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.REAGENT);
    }

    if (KoLCharacter.hasSkill(SkillPool.THE_WAY_OF_SAUCE)) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.WAY);
    }

    if (KoLCharacter.hasSkill(SkillPool.DEEP_SAUCERY)) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.DEEP_SAUCERY);
    }

    if (KoLCharacter.canSummonNoodles()) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.PASTA);
    }

    if (KoLCharacter.hasSkill(SkillPool.TRANSCENDENTAL_NOODLECRAFTING)) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.TRANSNOODLE);
    }

    if (KoLCharacter.hasSkill(SkillPool.TEMPURAMANCY)) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.TEMPURAMANCY);
    }

    if (KoLCharacter.hasSkill(SkillPool.PATENT_MEDICINE)) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.PATENT);
    }

    if (KoLCharacter.hasSkill(SkillPool.ELDRITCH_INTELLECT)) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.ELDRITCH);
    }

    // Mixing is permitted, so long as the person has a shaker or a
    // cocktailcrafting kit installed in their kitchen

    if (KoLCharacter.hasShaker()
        || KoLCharacter.hasCocktailKit()
        || KoLCharacter.inWereProfessor()) {
      permitNoCost(CraftingType.MIX);
    }
    ConcoctionDatabase.EXCUSE.put(
        CraftingType.MIX, "You cannot mix without a shaker or a cocktailcrafting kit.");

    // If we have a kit and a bartender installed, mixing fancy drinks
    // costs no adventure. If we have no bartender, mixing takes
    // adventures unless we have free crafts.

    // If you don't have a cocktailcrafting kit, you can't mix fancy drinks
    // We will auto buy & install a cocktailcrafting kit if the character
    // has at least 1,000 Meat and autoSatisfyWithNPCs = true
    if (!KoLCharacter.hasCocktailKit() && !willBuyTool) {
      ConcoctionDatabase.ADVENTURE_USAGE.put(CraftingType.MIX_FANCY, 0);
      ConcoctionDatabase.CREATION_COST.put(CraftingType.MIX_FANCY, 0L);
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.MIX_FANCY, "You cannot mix fancy drinks without a cocktailcrafting kit.");
    }
    // If you have (or will have) a bartender, fancy mixing is free
    else if (KoLCharacter.hasBartender()
        || willBuyServant
        || ConcoctionDatabase.isAvailable(ItemPool.BARTENDER, ItemPool.CLOCKWORK_BARTENDER)) {
      ConcoctionDatabase.PERMIT_METHOD.add(CraftingType.MIX_FANCY);
      ConcoctionDatabase.ADVENTURE_USAGE.put(CraftingType.MIX_FANCY, 0);
      ConcoctionDatabase.CREATION_COST.put(
          CraftingType.MIX_FANCY, MallPriceDatabase.getPrice(ItemPool.BARTENDER) / 90);
      ConcoctionDatabase.EXCUSE.put(CraftingType.MIX_FANCY, null);
    }
    // If you are Sneaky Pete with Cocktail Magic, fancy mixing is free
    else if (KoLCharacter.hasSkill(SkillPool.COCKTAIL_MAGIC)) {
      permitNoCost(CraftingType.MIX_FANCY);
      ConcoctionDatabase.EXCUSE.put(CraftingType.MIX_FANCY, null);
    }
    // We might not care if mixing takes adventures
    else if (Preferences.getBoolean("requireBoxServants") && !KoLCharacter.inGLover()) {
      ConcoctionDatabase.ADVENTURE_USAGE.put(CraftingType.MIX_FANCY, 0);
      ConcoctionDatabase.CREATION_COST.put(CraftingType.MIX_FANCY, 0L);
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.MIX_FANCY,
          "You have chosen not to mix fancy drinks without a bartender-in-the-box.");
    }
    // Otherwise, spend those adventures!
    else {
      if (KoLCharacter.getAdventuresLeft() + freeCrafts + getFreeCocktailcraftingTurns() > 0) {
        ConcoctionDatabase.PERMIT_METHOD.add(CraftingType.MIX_FANCY);
      }
      ConcoctionDatabase.ADVENTURE_USAGE.put(CraftingType.MIX_FANCY, 1);
      ConcoctionDatabase.CREATION_COST.put(CraftingType.MIX_FANCY, 0L);
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.MIX_FANCY, "You cannot mix fancy drinks without adventures.");
    }

    // Mixing may require an additional skill.

    if (KoLCharacter.canSummonShore() || KoLCharacter.hasSkill(SkillPool.MIXOLOGIST)) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.AC);
    }

    if (KoLCharacter.hasSkill(SkillPool.SUPER_COCKTAIL)
        || KoLCharacter.hasSkill(SkillPool.MIXOLOGIST)) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.SHC);
    }

    if (KoLCharacter.hasSkill(SkillPool.SALACIOUS_COCKTAILCRAFTING)) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.SALACIOUS);
    }

    if (KoLCharacter.hasSkill(SkillPool.TIKI_MIXOLOGY)) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.TIKI);
    }

    // Using Crosby Nash's Still is possible if the person has
    // Superhuman Cocktailcrafting and is a Moxie class character.

    if (ConcoctionDatabase.stillsLimit.total > 0) {
      ConcoctionDatabase.PERMIT_METHOD.add(CraftingType.STILL);
      ConcoctionDatabase.ADVENTURE_USAGE.put(CraftingType.STILL, 0);
      ConcoctionDatabase.CREATION_COST.put(CraftingType.STILL, Preferences.getLong("valueOfStill"));
    }
    ConcoctionDatabase.EXCUSE.put(
        CraftingType.STILL,
        KoLCharacter.isMoxieClass()
            ? "You have no Still uses remaining."
            : "Only moxie classes can use the Still.");

    // Summoning Clip Art is possible if the person has that tome,
    // and isn't in Bad Moon

    boolean inBadMoon = KoLCharacter.inBadMoon() && !KoLCharacter.skillsRecalled();

    boolean hasClipArt = KoLCharacter.hasSkill(SkillPool.CLIP_ART) && !inBadMoon;
    boolean clipArtSummonsRemaining =
        hasClipArt
            && (KoLCharacter.canInteract()
                ? Preferences.getInteger("_clipartSummons") < 3
                : Preferences.getInteger("tomeSummons") < 3);
    if (hasClipArt && clipArtSummonsRemaining) {
      ConcoctionDatabase.PERMIT_METHOD.add(CraftingType.CLIPART);
      ConcoctionDatabase.ADVENTURE_USAGE.put(CraftingType.CLIPART, 0);
      ConcoctionDatabase.CREATION_COST.put(
          CraftingType.CLIPART, Preferences.getLong("valueOfTome"));
    }
    ConcoctionDatabase.EXCUSE.put(
        CraftingType.CLIPART,
        hasClipArt ? "You have no Tome uses remaining." : "You don't have the Tome of Clip Art.");

    // Using the Malus of Forethought is possible if the person has
    // Pulverize and is a Muscle class character.

    if (KoLCharacter.canUseMalus()) {
      permitNoCost(CraftingType.MALUS);
    }
    ConcoctionDatabase.EXCUSE.put(
        CraftingType.MALUS, "You require Malus access to be able to pulverize.");

    // You can make Sushi if you have a sushi-rolling mat installed
    // in your kitchen.

    if (KoLCharacter.hasSushiMat()) {
      permitNoCost(CraftingType.SUSHI);
    }
    ConcoctionDatabase.EXCUSE.put(
        CraftingType.SUSHI, "You cannot make sushi without a sushi-rolling mat.");

    // You can ask Grandma to make stuff if you have rescued her.
    if (QuestDatabase.isQuestLaterThan(Quest.SEA_MONKEES, "step8")) {
      permitNoCost(CraftingType.GRANDMA);
    }
    ConcoctionDatabase.EXCUSE.put(CraftingType.GRANDMA, "You must rescue Grandma first.");

    // KOLHS concoctions are "permitted" so that we can calculate
    // how many items are allowed given available ingredients
    // But only in KOLHS!
    if (KoLCharacter.inHighschool()) {
      permitNoCost(CraftingType.CHEMCLASS);
      permitNoCost(CraftingType.ARTCLASS);
      permitNoCost(CraftingType.SHOPCLASS);
    }
    ConcoctionDatabase.EXCUSE.put(
        CraftingType.CHEMCLASS, "You cannot make that as you are not at school.");
    ConcoctionDatabase.EXCUSE.put(
        CraftingType.ARTCLASS, "You cannot make that as you are not at school.");
    ConcoctionDatabase.EXCUSE.put(
        CraftingType.SHOPCLASS, "You cannot make that as you are not at school.");

    // Making stuff with Beer Garden ingredients needs
    permitNoCost(CraftingType.BEER);

    // Making stuff with the Junk Magazine requires the magazine
    if (InventoryManager.hasItem(ItemPool.WORSE_HOMES_GARDENS)) {
      permitNoCost(CraftingType.JUNK);
    }
    ConcoctionDatabase.EXCUSE.put(
        CraftingType.JUNK, "You can't make that without a copy of Worse Homes and Gardens.");

    // Making stuff with Winter Garden ingredients is always allowed
    permitNoCost(CraftingType.WINTER);

    // Making stuff with Rumplestiltskin's Workshop is allowed when have access to it
    if (Preferences.getString("grimstoneMaskPath").equals("gnome")) {
      permitNoCost(CraftingType.RUMPLE);
    }
    ConcoctionDatabase.EXCUSE.put(
        CraftingType.RUMPLE, "You need access to Rumplestiltskin's Workshop to make that.");

    // You trade tokens to Coin Masters if you have opted in to do so,

    if (Preferences.getBoolean("autoSatisfyWithCoinmasters")) {
      permitNoCost(CraftingType.COINMASTER);
    }
    ConcoctionDatabase.EXCUSE.put(
        CraftingType.COINMASTER, "You have not selected the option to trade with coin masters.");

    if (InventoryManager.getCount(ItemPool.SAUSAGE_O_MATIC) > 0
        || KoLCharacter.hasEquipped(ItemPool.SAUSAGE_O_MATIC, Slot.OFFHAND)
        || (KoLCharacter.inLegacyOfLoathing()
            && InventoryManager.equippedOrInInventory(ItemPool.REPLICA_SAUSAGE_O_MATIC))) {
      ConcoctionDatabase.PERMIT_METHOD.add(CraftingType.SAUSAGE_O_MATIC);
      ConcoctionDatabase.ADVENTURE_USAGE.put(CraftingType.SAUSAGE_O_MATIC, 0);
      ConcoctionDatabase.CREATION_COST.put(
          CraftingType.SAUSAGE_O_MATIC, 111 * (1 + Preferences.getLong("_sausagesMade")));
    }
    ConcoctionDatabase.EXCUSE.put(
        CraftingType.SAUSAGE_O_MATIC, "You do not have a Kramco Sausage-o-Matic&trade;.");

    if (InventoryManager.getCount(ItemPool.FIVE_D_PRINTER) > 0) {
      permitNoCost(CraftingType.FIVE_D);
    }
    ConcoctionDatabase.EXCUSE.put(CraftingType.FIVE_D, "You do not have a Xiblaxian 5D printer.");

    if (VYKEACompanionData.currentCompanion() != VYKEACompanionData.NO_COMPANION) {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.VYKEA, "You can only build one VYKEA Companion a day.");
    } else if (!InventoryManager.hasItem(ItemPool.VYKEA_HEX_KEY)) {
      ConcoctionDatabase.EXCUSE.put(CraftingType.VYKEA, "You do not have a VYKEA hex key.");
    } else {
      permitNoCost(CraftingType.VYKEA);
    }

    // Making stuff with globs of melted wax is always allowed
    permitNoCost(CraftingType.WAX);

    // Making stuff with spant chitin/tendons is always allowed
    permitNoCost(CraftingType.SPANT);

    // Making stuff with Xes/Os is always allowed
    permitNoCost(CraftingType.XO);

    // Making stuff with Slime is always allowed
    permitNoCost(CraftingType.SLIEMCE);

    // Making stuff with burning newspaper is always allowed
    permitNoCost(CraftingType.NEWSPAPER);

    // Making stuff with metal meteoroid is always allowed
    permitNoCost(CraftingType.METEOROID);

    // Pulling stuff out of the sewer is always allowed
    permitNoCost(CraftingType.SEWER);

    // Making stuff with grubby wool is always allowed
    permitNoCost(CraftingType.WOOL);

    // Making stuff at The Shadow Forge is only allowed if you have not
    // spent any adventures since you last encountered it.
    if (Preferences.getInteger("lastShadowForgeUnlockAdventure") == KoLCharacter.getCurrentRun()) {
      permitNoCost(CraftingType.SHADOW_FORGE);
    } else {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.SHADOW_FORGE, "You need to be at The Shadow Forge to make that.");
    }

    // You need a gnome to pick a gnome part and can only do so once per day.
    if (Preferences.getBoolean("_gnomePart")) {
      ConcoctionDatabase.EXCUSE.put(CraftingType.GNOME_PART, "You have already picked a part.");
    } else if (!KoLCharacter.canUseFamiliar(FamiliarPool.REAGNIMATED_GNOME)) {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.GNOME_PART, "You need a Reagnimated Gnome to pick its parts.");
    } else {
      permitNoCost(CraftingType.GNOME_PART);
    }

    // Making stuff with Burning Leaves is always allowed

    boolean burningLeaves =
        KoLConstants.campground.contains(ItemPool.get(ItemPool.A_GUIDE_TO_BURNING_LEAVES));
    if (burningLeaves) {
      permitNoCost(CraftingType.BURNING_LEAVES);
    } else {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.BURNING_LEAVES,
          "You need to have a Pile of Burning Leaves in your campsite to make that.");
    }

    // Making stuff at the Tinkering Bench is allowed if you are in the
    // WereProfessor path and are in Professor form.

    boolean tinker = KoLCharacter.isMildManneredProfessor();
    if (tinker) {
      permitNoCost(CraftingType.TINKERING_BENCH);
    } else {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.TINKERING_BENCH,
          "Only a mild-mannered professor can work at their Tinkering Bench.");
    }

    // Other creatability flags

    if (KoLCharacter.isTorsoAware()) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.TORSO);
    }

    if (HolidayDatabase.getHoliday().contains("St. Sneaky Pete's Day")
        || HolidayDatabase.getHoliday().contains("Drunksgiving")) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.SSPD);
    }

    if (!KoLCharacter.inBeecore()) {
      ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.NOBEE);
    }

    if (KoLCharacter.isJarlsberg()) {
      permitNoCost(CraftingType.JARLS);

      if (KoLCharacter.hasSkill(SkillPool.BAKE)) {
        ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.BAKE);
      }
      if (KoLCharacter.hasSkill(SkillPool.BLEND)) {
        ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.BLEND);
      }
      if (KoLCharacter.hasSkill(SkillPool.BOIL)) {
        ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.BOIL);
      }
      if (KoLCharacter.hasSkill(SkillPool.CHOP)) {
        ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.CHOP);
      }
      if (KoLCharacter.hasSkill(SkillPool.CURDLE)) {
        ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.CURDLE);
      }
      if (KoLCharacter.hasSkill(SkillPool.FREEZE)) {
        ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.FREEZE);
      }
      if (KoLCharacter.hasSkill(SkillPool.FRY)) {
        ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.FRY);
      }
      if (KoLCharacter.hasSkill(SkillPool.GRILL)) {
        ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.GRILL);
      }
      if (KoLCharacter.hasSkill(SkillPool.SLICE)) {
        ConcoctionDatabase.REQUIREMENT_MET.add(CraftingRequirements.SLICE);
      }
    }
    ConcoctionDatabase.EXCUSE.put(CraftingType.JARLS, "You are not an Avatar of Jarlsberg");

    if (Preferences.getBoolean("coldAirportAlways")
        || Preferences.getBoolean("hotAirportAlways")
        || Preferences.getBoolean("spookyAirportAlways")
        || Preferences.getBoolean("stenchAirportAlways")
        || Preferences.getBoolean("sleazeAirportAlways")
        || Preferences.getBoolean("_coldAirportToday")
        || Preferences.getBoolean("_hotAirportToday")
        || Preferences.getBoolean("_spookyAirportToday")
        || Preferences.getBoolean("_stenchAirportToday")
        || Preferences.getBoolean("_sleazeAirportToday")) {
      permitNoCost(CraftingType.DUTYFREE);
    }

    // It's Crimbo, so allow creation!
    // permitNoCost( CraftingType.CRIMBO16 );

    boolean clanFloundry =
        ClanLoungeRequest.hasClanLoungeItem(ItemPool.get(ItemPool.CLAN_FLOUNDRY, 1));
    boolean gotFloundryItem =
        InventoryManager.hasItem(ItemPool.CARPE)
            || InventoryManager.hasItem(ItemPool.CODPIECE)
            || InventoryManager.hasItem(ItemPool.TROUTSERS)
            || InventoryManager.hasItem(ItemPool.BASS_CLARINET)
            || InventoryManager.hasItem(ItemPool.FISH_HATCHET)
            || InventoryManager.hasItem(ItemPool.TUNAC);
    boolean floundryUsable =
        StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Clan Floundry") && !inBadMoon;
    if (clanFloundry && !gotFloundryItem && floundryUsable) {
      permitNoCost(CraftingType.FLOUNDRY);
    }
    if (!floundryUsable) {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.FLOUNDRY, "You can't use the Floundry in this path.");
    } else if (gotFloundryItem) {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.FLOUNDRY, "You have already got a Floundry item today.");
    } else if (!clanFloundry) {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.FLOUNDRY, "Your current clan does not have a Floundry.");
    }

    boolean gotBarrelShrine = Preferences.getBoolean("barrelShrineUnlocked");
    boolean gotBarrelItem = Preferences.getBoolean("_barrelPrayer");
    boolean barrelUsable =
        StandardRequest.isAllowed(RestrictedItemType.ITEMS, "shrine to the Barrel god")
            && !inBadMoon;
    if (gotBarrelShrine && !gotBarrelItem && barrelUsable) {
      permitNoCost(CraftingType.BARREL);
    }
    if (!barrelUsable) {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.BARREL, "You can't use the Barrel Shrine in this path.");
    } else if (gotBarrelItem) {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.BARREL, "You have already got a Barrel Shrine item today.");
    } else if (!gotBarrelShrine) {
      ConcoctionDatabase.EXCUSE.put(CraftingType.BARREL, "You do not have a Barrel Shrine.");
    }

    boolean sourceTerminal =
        KoLConstants.campground.contains(ItemPool.get(ItemPool.SOURCE_TERMINAL))
            || KoLConstants.falloutShelter.contains(ItemPool.get(ItemPool.SOURCE_TERMINAL));
    boolean sourceTerminalUsable = Preferences.getInteger("_sourceTerminalExtrudes") < 3;
    if (sourceTerminal && sourceTerminalUsable) {
      permitNoCost(CraftingType.TERMINAL);
    } else if (sourceTerminal && !sourceTerminalUsable) {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.TERMINAL, "You have used all your extrudes for today.");
    } else {
      ConcoctionDatabase.EXCUSE.put(CraftingType.TERMINAL, "You do not have a Source Terminal.");
    }

    boolean spacegateUsable =
        StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Spacegate access badge") && !inBadMoon;
    if (Preferences.getBoolean("spacegateAlways") && spacegateUsable) {
      permitNoCost(CraftingType.SPACEGATE);
    } else {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.SPACEGATE, "You do not have access to Spacegate Equipment Requisition.");
    }

    boolean fantasyRealmUsable =
        StandardRequest.isAllowed(RestrictedItemType.ITEMS, "FantasyRealm membership packet")
            && !inBadMoon
            && !StringUtilities.isNumeric(Preferences.getString("_frHoursLeft"));
    if ((Preferences.getBoolean("frAlways") || Preferences.getBoolean("_frToday"))
        && fantasyRealmUsable) {
      permitNoCost(CraftingType.FANTASY_REALM);
    } else {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.FANTASY_REALM, "You do not have access to Fantasy Realm welcome center.");
    }

    // You can't use Kringle's workshop on demand; it is a shop
    // found in a non-combat adventure. However, if you are in
    // that shop, you can "create" the item normally.
    permitNoCost(CraftingType.KRINGLE);
    ConcoctionDatabase.EXCUSE.put(CraftingType.KRINGLE, "You must be in Kringle's workshop.");

    boolean stillsuitUsable =
        StandardRequest.isAllowed(RestrictedItemType.ITEMS, "tiny stillsuit")
            && InventoryManager.hasItem(ItemPool.STILLSUIT);
    if (stillsuitUsable) {
      permitNoCost(CraftingType.STILLSUIT);
    } else {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.STILLSUIT, "You do not have access to a tiny stillsuit.");
    }

    boolean mayamCalendarUsable =
        StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Mayam Calendar")
            && !inBadMoon
            && InventoryManager.hasItem(ItemPool.MAYAM_CALENDAR);
    if (mayamCalendarUsable) {
      permitNoCost(CraftingType.MAYAM);
    } else {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.MAYAM, "You need to have a Mayam Calendar to make that.");
    }

    boolean clanPhotoBooth =
        ClanLoungeRequest.hasClanLoungeItem(ItemPool.get(ItemPool.CLAN_PHOTO_BOOTH, 1));
    boolean canTakePhotoBoothProp = Preferences.getInteger("_photoBoothEquipment") < 3;
    if (clanPhotoBooth && canTakePhotoBoothProp) {
      permitNoCost(CraftingType.PHOTO_BOOTH);
    }
    if (!canTakePhotoBoothProp) {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.PHOTO_BOOTH, "You have already taken too many Photo Booth props today.");
    } else if (!clanPhotoBooth) {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.PHOTO_BOOTH, "Your current clan does not have a Photo Booth.");
    }

    AdventureResult workshedItem = CampgroundRequest.getCurrentWorkshedItem();
    boolean takerspace =
        workshedItem != null && workshedItem.getItemId() == ItemPool.TAKERSPACE_LETTER_OF_MARQUE;
    if (takerspace) {
      permitNoCost(CraftingType.TAKERSPACE);
    } else {
      ConcoctionDatabase.EXCUSE.put(
          CraftingType.TAKERSPACE, "You need a TakerSpace as your Workshed.");
    }

    // Now, go through all the cached adventure usage values and if
    // the number of adventures left is zero and the request requires
    // adventures, it is not permitted.

    int value = Preferences.getInteger("valueOfAdventure");
    for (CraftingType method : CraftingType.values()) {
      if (ConcoctionDatabase.PERMIT_METHOD.contains(method)) {
        int adv = ConcoctionDatabase.getAdventureUsage(method);
        if (adv == 0) {
          continue;
        }
        int usableFreeCrafts = getFreeCraftingTurns();
        if (method == CraftingType.SMITH || method == CraftingType.SSMITH) {
          usableFreeCrafts += getFreeSmithingTurns();
        } else if (method == CraftingType.COOK_FANCY) {
          usableFreeCrafts += getFreeCookingTurns();
        } else if (method == CraftingType.MIX_FANCY) {
          usableFreeCrafts += getFreeCocktailcraftingTurns();
        }
        if (adv > KoLCharacter.getAdventuresLeft() + usableFreeCrafts) {
          ConcoctionDatabase.PERMIT_METHOD.remove(method);
          ConcoctionDatabase.EXCUSE.put(
              method, "You don't have enough adventures left to create that.");
        } else {
          long cost = ConcoctionDatabase.getCreationCost(method);
          ConcoctionDatabase.CREATION_COST.put(method, (cost + ((long) adv * value)));
        }
      }
    }
  }

  private static void permitNoCost(CraftingType craft) {
    ConcoctionDatabase.PERMIT_METHOD.add(craft);
    ConcoctionDatabase.CREATION_COST.put(craft, 0L);
    ConcoctionDatabase.ADVENTURE_USAGE.put(craft, 0);
    ConcoctionDatabase.EXCUSE.remove(craft);
  }

  public static int getAdventureUsage(CraftingType method) {
    Integer advs = ConcoctionDatabase.ADVENTURE_USAGE.get(method);
    return advs == null ? 0 : advs.intValue();
  }

  public static long getCreationCost(CraftingType method) {
    Long advs = ConcoctionDatabase.CREATION_COST.get(method);
    return advs == null ? 0 : advs;
  }

  public static int getFreeCraftingTurns() {
    return ConcoctionDatabase.INIGO.getCount(KoLConstants.activeEffects) / 5
        + (KoLCharacter.hasSkill(SkillPool.RAPID_PROTOTYPING)
                && StandardRequest.isAllowed(RestrictedItemType.SKILLS, "Rapid Prototyping")
            ? 5 - Preferences.getInteger("_rapidPrototypingUsed")
            : 0)
        + (KoLCharacter.hasSkill(SkillPool.EXPERT_CORNER_CUTTER)
                && StandardRequest.isAllowed(RestrictedItemType.SKILLS, "Expert Corner-Cutter")
                &&
                // KoL bug: this is the only skill that does not work
                // unless you have at least one turn available, even
                // though crafting will not use that turn.
                (KoLCharacter.getAdventuresLeft() > 0)
            ? 5 - Preferences.getInteger("_expertCornerCutterUsed")
            : 0)
        + (ConcoctionDatabase.CRAFT_TEA.getCount(KoLConstants.activeEffects) / 5)
        + (StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Cold Medicine Cabinet")
            ? Preferences.getInteger("homebodylCharges")
            : 0)
        + (KoLCharacter.hasSkill(SkillPool.HOLIDAY_MULTITASKING)
                && StandardRequest.isAllowed(RestrictedItemType.SKILLS, "Holiday Multitasking")
            ? 3 - Preferences.getInteger("_holidayMultitaskingUsed")
            : 0)
        + (StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Leprecondo")
            ? Preferences.getInteger("craftingPlansCharges")
            : 0);
  }

  public static int getFreeCookingTurns() {
    // assume bat works if allowed in standard & in terrarium, like the collective
    boolean haveBat =
        StandardRequest.isAllowed(RestrictedItemType.FAMILIARS, "Cookbookbat")
            && KoLCharacter.ownedFamiliar(FamiliarPool.COOKBOOKBAT).isPresent();
    return (haveBat ? 5 - Preferences.getInteger("_cookbookbatCrafting") : 0)
        + (ConcoctionDatabase.COOKING_CONCENTRATE.getCount(KoLConstants.activeEffects) / 5)
        + (KoLCharacter.hasSkill(SkillPool.ELF_GUARD_COOKING)
                && StandardRequest.isAllowed(RestrictedItemType.SKILLS, "Elf Guard Cooking")
            ? 3 - Preferences.getInteger("_elfGuardCookingUsed")
            : 0);
  }

  public static int getFreeCocktailcraftingTurns() {
    return (KoLCharacter.hasSkill(SkillPool.OLD_SCHOOL_COCKTAILCRAFTING)
            && StandardRequest.isAllowed(RestrictedItemType.SKILLS, "Old-School Cocktailcrafting")
        ? 3 - Preferences.getInteger("_oldSchoolCocktailCraftingUsed")
        : 0);
  }

  public static int getFreeSmithingTurns() {
    AdventureResult workshedItem = CampgroundRequest.getCurrentWorkshedItem();
    boolean haveWarbearAutoanvil =
        workshedItem != null && workshedItem.getItemId() == ItemPool.AUTO_ANVIL;
    boolean haveJackhammer = InventoryManager.hasItem(ItemPool.LOATHING_LEGION_JACKHAMMER);
    boolean havePliers =
        ConcoctionDatabase.THORS_PLIERS.getCount(KoLConstants.closet) > 0
            || ConcoctionDatabase.THORS_PLIERS.getCount(KoLConstants.inventory) > 0
            || InventoryManager.getEquippedCount(ConcoctionDatabase.THORS_PLIERS) > 0;
    return (haveWarbearAutoanvil ? 5 - Preferences.getInteger("_warbearAutoAnvilCrafting") : 0)
        + (haveJackhammer ? 3 - Preferences.getInteger("_legionJackhammerCrafting") : 0)
        + (havePliers ? 10 - Preferences.getInteger("_thorsPliersCrafting") : 0);
  }

  private static boolean isAvailable(final int servantId, final int clockworkId) {
    // Otherwise, return whether or not the quantity possible for
    // the given box servants is non-zero.	This works because
    // cooking tests are made after item creation tests.

    return Preferences.getBoolean("autoRepairBoxServants")
        && !KoLCharacter.inGLover()
        && (ConcoctionPool.get(servantId).total > 0 || ConcoctionPool.get(clockworkId).total > 0);
  }

  /** Returns the mixing method for the item with the given Id. */
  public static final CraftingType getMixingMethod(final Concoction item) {
    return item == null ? CraftingType.NOCREATE : item.getMixingMethod();
  }

  public static final CraftingType getMixingMethod(final int itemId) {
    return ConcoctionDatabase.getMixingMethod(ConcoctionPool.get(itemId));
  }

  public static final CraftingType getMixingMethod(final int itemId, final String name) {
    return ConcoctionDatabase.getMixingMethod(ConcoctionPool.get(itemId, name));
  }

  public static final CraftingType getMixingMethod(final AdventureResult ar) {
    return ConcoctionDatabase.getMixingMethod(ConcoctionPool.get(ar));
  }

  public static final boolean hasMixingMethod(final int itemId) {
    return getMixingMethod(itemId) != CraftingType.NOCREATE;
  }

  public static final EnumSet<CraftingRequirements> getRequirements(final int itemId) {
    Concoction item = ConcoctionPool.get(itemId);
    return item == null ? EnumSet.noneOf(CraftingRequirements.class) : item.getRequirements();
  }

  /**
   * Describes a method of creation in terms of the means of creation and the restrictions, if any.
   *
   * @param mixingMethod the method to describe
   * @return the description
   */
  public static String mixingMethodDescription(
      final CraftingType mixingMethod, EnumSet<CraftingRequirements> mixingRequirements) {
    if (mixingMethod == CraftingType.NOCREATE) {
      return "[cannot be created]";
    }

    StringBuilder result = new StringBuilder();

    switch (mixingMethod) {
      case COMBINE -> result.append("Meatpasting");
      case COOK -> result.append("Cooking");
      case MIX -> result.append("Mixing");
      case SMITH -> result.append("Meatsmithing");
      case SSMITH -> result.append("Meatsmithing (not Innabox)");
      case STILL -> result.append("Nash Crosby's Still");
      case MALUS -> result.append("Malus of Forethought");
      case JEWELRY -> result.append("Jewelry-making pliers");
      case STARCHART -> result.append("star chart");
      case SUGAR_FOLDING -> result.append("sugar sheet");
      case PIXEL -> result.append("Crackpot Mystic");
      case CHEMCLASS -> result.append("Chemistry Class");
      case ARTCLASS -> result.append("Art Class");
      case SHOPCLASS -> result.append("Shop Class");
      case RUMPLE -> result.append("Rumpelstiltskin's Workshop");
      case ROLLING_PIN -> result.append("rolling pin/unrolling pin");
      case GNOME_TINKER -> result.append("Supertinkering");
      case STAFF -> result.append("Rodoric, the Staffcrafter");
      case SUSHI -> result.append("sushi-rolling mat");
      case SINGLE_USE -> result.append("single-use");
      case MULTI_USE -> result.append("multi-use");
      case CRIMBO05 -> result.append("Crimbo Town Toy Factory (Crimbo 2005)");
      case CRIMBO06 -> result.append("Uncle Crimbo's Mobile Home (Crimboween 2006)");
      case CRIMBO07 -> result.append("Uncle Crimbo's Mobile Home (Crimbo 2007)");
      case CRIMBO12 -> result.append("Uncle Crimbo's Futuristic Trailer (Crimboku 2012)");
      case CRIMBO16 -> result.append("Crimbo Lumps Shop (Crimbo 2016)");
      case PHINEAS -> result.append("Phineas");
      case COOK_FANCY -> result.append("Cooking (fancy)");
      case MIX_FANCY -> result.append("Mixing (fancy)");
      case ACOMBINE -> result.append("Meatpasting (not untinkerable)");
      case COINMASTER -> result.append("Coin Master purchase");
      case CLIPART -> result.append("Summon Clip Art");
      case JARLS -> result.append("Jarlsberg's Kitchen");
      case GRANDMA -> result.append("Grandma Sea Monkee");
      case BEER -> result.append("Beer Garden");
      case JUNK -> result.append("Worse Homes and Gardens");
      case WINTER -> result.append("Winter Garden");
      case FIVE_D -> result.append("Xiblaxian 5D printer");
      case VYKEA -> result.append("VYKEA");
      case DUTYFREE -> result.append("Elemental International Airport Duty Free Shop");
      case FLOUNDRY -> result.append("Clan Floundry");
      case TERMINAL -> result.append("Source Terminal");
      case BARREL -> result.append("shrine to the Barrel god");
      case WAX -> result.append("globs of wax");
      case SPANT -> result.append("spant pieces");
      case SPACEGATE -> result.append("Spacegate Equipment Requisition");
      case XO -> result.append("XO Shop");
      case SLIEMCE -> result.append("Mad Sliemce");
      case NEWSPAPER -> result.append("burning newspaper");
      case METEOROID -> result.append("metal meteoroid");
      case SAUSAGE_O_MATIC -> result.append("Kramco Sausage-o-Matic");
      case SEWER -> result.append("chewing gum");
      case FANTASY_REALM -> result.append("Fantasy Realm Welcome Center");
      case KRINGLE -> result.append("Kringle's workshop");
      case STILLSUIT -> result.append("tiny stillsuit");
      case WOOL -> result.append("grubby wool");
      case SHADOW_FORGE -> result.append("The Shadow Forge");
      case BURNING_LEAVES -> result.append("Pile of Burning Leaves");
      case TINKERING_BENCH -> result.append("Tinkering Bench");
      case MAYAM -> result.append("Mayam Calendar");
      case PHOTO_BOOTH -> result.append("Clan Photo Booth");
      case TAKERSPACE -> result.append("TakerSpace");
    }
    if (result.isEmpty()) {
      result.append("[unknown method of creation]");
    }

    if (mixingRequirements.contains(CraftingRequirements.MALE)) result.append(" (males only)");

    if (mixingRequirements.contains(CraftingRequirements.FEMALE)) result.append(" (females only)");

    if (mixingRequirements.contains(CraftingRequirements.SSPD))
      result.append(" (St. Sneaky Pete's Day only)");

    if (mixingRequirements.contains(CraftingRequirements.HAMMER))
      result.append(" (tenderizing hammer)");

    if (mixingRequirements.contains(CraftingRequirements.GRIMACITE))
      result.append(" (depleted Grimacite hammer)");

    if (mixingRequirements.contains(CraftingRequirements.TORSO))
      result.append(" (Torso Awareness)");

    if (mixingRequirements.contains(CraftingRequirements.SUPER_MEATSMITHING))
      result.append(" (Super-Advanced Meatsmithing)");

    if (mixingRequirements.contains(CraftingRequirements.ARMORCRAFTINESS))
      result.append(" (Armorcraftiness)");

    if (mixingRequirements.contains(CraftingRequirements.ELDRITCH))
      result.append(" (Eldritch Intellect)");

    if (mixingRequirements.contains(CraftingRequirements.EXPENSIVE))
      result.append(" (Really Expensive Jewelrycrafting)");

    if (mixingRequirements.contains(CraftingRequirements.REAGENT))
      result.append(" (Advanced Saucecrafting)");

    if (mixingRequirements.contains(CraftingRequirements.WAY)) result.append(" (The Way of Sauce)");

    if (mixingRequirements.contains(CraftingRequirements.DEEP_SAUCERY))
      result.append(" (Deep Saucery)");

    if (mixingRequirements.contains(CraftingRequirements.PASTA)) result.append(" (Pastamastery)");

    if (mixingRequirements.contains(CraftingRequirements.TRANSNOODLE))
      result.append(" (Transcendental Noodlecraft)");

    if (mixingRequirements.contains(CraftingRequirements.TEMPURAMANCY))
      result.append(" (Tempuramancy)");

    if (mixingRequirements.contains(CraftingRequirements.PATENT))
      result.append(" (Patent Medicine)");

    if (mixingRequirements.contains(CraftingRequirements.AC))
      result.append(" (Advanced Cocktailcrafting)");

    if (mixingRequirements.contains(CraftingRequirements.SHC))
      result.append(" (Superhuman Cocktailcrafting)");

    if (mixingRequirements.contains(CraftingRequirements.SALACIOUS))
      result.append(" (Salacious Cocktailcrafting)");

    if (mixingRequirements.contains(CraftingRequirements.TIKI)) result.append(" (Tiki Mixology)");

    if (mixingRequirements.contains(CraftingRequirements.NOBEE))
      result.append(" (Unavailable in Beecore)");

    if (mixingRequirements.contains(CraftingRequirements.BAKE)) result.append(" (Bake)");

    if (mixingRequirements.contains(CraftingRequirements.BLEND)) result.append(" (Blend)");

    if (mixingRequirements.contains(CraftingRequirements.BOIL)) result.append(" (Boil)");

    if (mixingRequirements.contains(CraftingRequirements.CHOP)) result.append(" (Chop)");

    if (mixingRequirements.contains(CraftingRequirements.CURDLE)) result.append(" (Curdle)");

    if (mixingRequirements.contains(CraftingRequirements.FREEZE)) result.append(" (Freeze)");

    if (mixingRequirements.contains(CraftingRequirements.FRY)) result.append(" (Fry)");

    if (mixingRequirements.contains(CraftingRequirements.GRILL)) result.append(" (Grill)");

    if (mixingRequirements.contains(CraftingRequirements.SLICE)) result.append(" (Slice)");

    return result.toString();
  }

  /**
   * Returns the item Ids of the ingredients for the given item. Note that if there are no
   * ingredients, then <code>null</code> will be returned instead.
   */
  public static final AdventureResult[] getIngredients(final int itemId) {
    return ConcoctionDatabase.getIngredients(ConcoctionPool.get(itemId));
  }

  public static final AdventureResult[] getIngredients(final String name) {
    return ConcoctionDatabase.getIngredients(ConcoctionPool.get(-1, name));
  }

  public static final AdventureResult[] getIngredients(Concoction c) {
    AdventureResult[] ingredients = ConcoctionDatabase.getStandardIngredients(c);
    return ConcoctionDatabase.getIngredients(c, ingredients);
  }

  public static final AdventureResult[] getIngredients(
      Concoction c, AdventureResult[] ingredients) {
    List<AdventureResult> availableIngredients = ConcoctionDatabase.getAvailableIngredients();
    return ConcoctionDatabase.getIngredients(c, ingredients, availableIngredients);
  }

  private static AdventureResult[] getIngredients(
      Concoction concoction,
      AdventureResult[] ingredients,
      List<AdventureResult> availableIngredients) {
    // Ensure that you're retrieving the same ingredients that
    // were used in the calculations.  Usually this is the case,
    // but ice-cold beer and ketchup are tricky cases.

    if (ingredients.length > 2) {
      // This is not a standard crafting recipe - and in the one case
      // where such a recipe uses one of these ingredients (Sir Schlitz
      // for the Staff of the Short Order Cook), it's not interchangeable.
      return ingredients;
    }

    switch (concoction.getItemId()) {
      case ItemPool.NICE_WARM_BEER -> {
        // nice warm beer works only with ice-cold Sir Schlitz
        return ingredients;
      }
    }

    for (int i = 0; i < ingredients.length; ++i) {
      switch (ingredients[i].getItemId()) {
        case ItemPool.SCHLITZ, ItemPool.WILLER -> ingredients[i] =
            ConcoctionDatabase.getBetterIngredient(
                ItemPool.SCHLITZ, ItemPool.WILLER, availableIngredients);
        case ItemPool.KETCHUP, ItemPool.CATSUP -> ingredients[i] =
            ConcoctionDatabase.getBetterIngredient(
                ItemPool.KETCHUP, ItemPool.CATSUP, availableIngredients);
        case ItemPool.DYSPEPSI_COLA, ItemPool.CLOACA_COLA -> ingredients[i] =
            ConcoctionDatabase.getBetterIngredient(
                ItemPool.DYSPEPSI_COLA, ItemPool.CLOACA_COLA, availableIngredients);
        case ItemPool.TITANIUM_UMBRELLA, ItemPool.GOATSKIN_UMBRELLA -> ingredients[i] =
            ConcoctionDatabase.getBetterIngredient(
                ItemPool.TITANIUM_UMBRELLA, ItemPool.GOATSKIN_UMBRELLA, availableIngredients);
      }
    }
    return ingredients;
  }

  public static final int getYield(final int itemId) {
    Concoction item = ConcoctionPool.get(itemId);
    return item == null ? 1 : item.getYield();
  }

  public static final AdventureResult[] getStandardIngredients(final int itemId) {
    return ConcoctionDatabase.getStandardIngredients(ConcoctionPool.get(itemId));
  }

  public static final AdventureResult[] getStandardIngredients(
      final int itemId, final String name) {
    return ConcoctionDatabase.getStandardIngredients(ConcoctionPool.get(itemId, name));
  }

  public static final AdventureResult[] getStandardIngredients(final Concoction item) {
    return item == null ? ConcoctionDatabase.NO_INGREDIENTS : item.getIngredients();
  }

  private static AdventureResult getBetterIngredient(
      final int itemId1, final int itemId2, final List<AdventureResult> availableIngredients) {
    AdventureResult ingredient1 = ItemPool.get(itemId1, 1);
    AdventureResult ingredient2 = ItemPool.get(itemId2, 1);
    long diff =
        ingredient1.getCount(availableIngredients) - ingredient2.getCount(availableIngredients);
    if (diff == 0) {
      diff = MallPriceDatabase.getPrice(itemId2) - MallPriceDatabase.getPrice(itemId1);
    }
    return diff > 0 ? ingredient1 : ingredient2;
  }

  public static final int getPullsBudgeted() {
    return ConcoctionDatabase.pullsBudgeted;
  }

  public static int pullsBudgeted = 0;
  public static int pullsRemaining = 0;

  public static final int getPullsRemaining() {
    return pullsRemaining;
  }

  private static void addCraftingData(String mix, String name) {
    CraftingType currentMixingMethod = ConcoctionDatabase.mixingMethod;
    // Items anybody can create using meat paste or The Plunger
    switch (mix) {
      case "COMBINE" -> ConcoctionDatabase.mixingMethod = CraftingType.COMBINE;
        // Items anybody can create with an E-Z Cook Oven or Dramatic Range
      case "COOK" -> ConcoctionDatabase.mixingMethod = CraftingType.COOK;
        // Items anybody can create with a Shaker or Cocktailcrafting Kit
      case "MIX" -> ConcoctionDatabase.mixingMethod = CraftingType.MIX;
        // Items anybody can create with a tenderizing hammer or via Innabox
      case "SMITH" -> ConcoctionDatabase.mixingMethod = CraftingType.SMITH;
        // Items that can only be created with a tenderizing hammer, not via Innabox
      case "SSMITH" -> ConcoctionDatabase.mixingMethod = CraftingType.SSMITH;
        // Items requiring access to Nash Crosby's Still
      case "STILL" -> ConcoctionDatabase.mixingMethod = CraftingType.STILL;
        // Items requiring access to the Malus of Forethought
      case "MALUS" -> ConcoctionDatabase.mixingMethod = CraftingType.MALUS;
        // Items anybody can create with jewelry-making pliers
      case "JEWEL" -> ConcoctionDatabase.mixingMethod = CraftingType.JEWELRY;
        // Items anybody can create with starcharts, stars, and lines
      case "STAR" -> ConcoctionDatabase.mixingMethod = CraftingType.STARCHART;
        // Items anybody can create by folding sugar sheets
      case "SUGAR" -> ConcoctionDatabase.mixingMethod = CraftingType.SUGAR_FOLDING;
        // Items anybody can create with pixels
      case "PIXEL" -> ConcoctionDatabase.mixingMethod = CraftingType.PIXEL;
        // Items anybody can create in KOLHS
      case "CHEMCLASS" -> ConcoctionDatabase.mixingMethod = CraftingType.CHEMCLASS;
      case "ARTCLASS" -> ConcoctionDatabase.mixingMethod = CraftingType.ARTCLASS;
      case "SHOPCLASS" -> ConcoctionDatabase.mixingMethod = CraftingType.SHOPCLASS;
        // Items created with a rolling pin or and an unrolling pin
      case "ROLL" -> ConcoctionDatabase.mixingMethod = CraftingType.ROLLING_PIN;
        // Items requiring access to the Gnome supertinker
      case "TINKER" -> ConcoctionDatabase.mixingMethod = CraftingType.GNOME_TINKER;
        // Items requiring access to Roderick the Staffmaker
      case "STAFF" -> ConcoctionDatabase.mixingMethod = CraftingType.STAFF;
        // Items anybody can create with a sushi-rolling mat
      case "SUSHI" -> ConcoctionDatabase.mixingMethod = CraftingType.SUSHI;
        // Items created by single (or multi) using a single item.
        // Extra ingredients might also be consumed.
        // Multi-using multiple of the item creates multiple results.
      case "SUSE" -> ConcoctionDatabase.mixingMethod = CraftingType.SINGLE_USE;
        // Items created by multi-using specific # of a single item.
        // Extra ingredients might also be consumed.
        // You must create multiple result items one at a time.
      case "MUSE" -> ConcoctionDatabase.mixingMethod = CraftingType.MULTI_USE;
      case "SEWER" -> ConcoctionDatabase.mixingMethod = CraftingType.SEWER;
        // Items formerly creatable in Crimbo Town during Crimbo 2005
      case "CRIMBO05" -> ConcoctionDatabase.mixingMethod = CraftingType.CRIMBO05;
        // Items formerly creatable in Crimbo Town during Crimbo 2006
      case "CRIMBO06" -> ConcoctionDatabase.mixingMethod = CraftingType.CRIMBO06;
        // Items formerly creatable in Crimbo Town during Crimbo 2007
      case "CRIMBO07" -> ConcoctionDatabase.mixingMethod = CraftingType.CRIMBO07;
        // Items formerly creatable in Crimbo Town during Crimbo 2012
      case "CRIMBO12" -> ConcoctionDatabase.mixingMethod = CraftingType.CRIMBO12;
        // Items creatable in Crimbo Town during Crimbo 2016
      case "CRIMBO16" -> ConcoctionDatabase.mixingMethod = CraftingType.CRIMBO16;
        // Items requiring access to Phineas
      case "PHINEAS" -> ConcoctionDatabase.mixingMethod = CraftingType.PHINEAS;
        // Items that require a Dramatic Range
      case "COOK_FANCY" -> ConcoctionDatabase.mixingMethod = CraftingType.COOK_FANCY;
        // Items that require a Cocktailcrafting Kit
      case "MIX_FANCY" -> ConcoctionDatabase.mixingMethod = CraftingType.MIX_FANCY;
        // Un-untinkerable Meatpasting
      case "ACOMBINE" -> ConcoctionDatabase.mixingMethod = CraftingType.ACOMBINE;
        // Summon Clip Art items
      case "CLIPART" -> ConcoctionDatabase.mixingMethod = CraftingType.CLIPART;
      case "MALE" -> ConcoctionDatabase.requirements.add(CraftingRequirements.MALE);
      case "FEMALE" -> ConcoctionDatabase.requirements.add(CraftingRequirements.FEMALE);
        // Can only be made on St. Sneaky Pete's Day
      case "SSPD" -> ConcoctionDatabase.requirements.add(CraftingRequirements.SSPD);
        // Requires tenderizing hammer (implied for SMITH & SSMITH)
      case "HAMMER" -> ConcoctionDatabase.requirements.add(CraftingRequirements.HAMMER);
        // Requires depleted Grimacite hammer
      case "GRIMACITE" -> ConcoctionDatabase.requirements.add(CraftingRequirements.GRIMACITE);
        // Requires Torso Awareness
      case "TORSO" -> ConcoctionDatabase.requirements.add(CraftingRequirements.TORSO);
        // Requires Super-Advanced Meatsmithing
      case "WEAPON" -> ConcoctionDatabase.requirements.add(CraftingRequirements.SUPER_MEATSMITHING);
        // Requires Armorcraftiness
      case "ARMOR" -> ConcoctionDatabase.requirements.add(CraftingRequirements.ARMORCRAFTINESS);
        // Requires Eldritch Intellect
      case "ELDRITCH" -> ConcoctionDatabase.requirements.add(CraftingRequirements.ELDRITCH);
        // Requires Really Expensive Jewelrycrafting
      case "EXPENSIVE" -> ConcoctionDatabase.requirements.add(CraftingRequirements.EXPENSIVE);
        // Requires Advanced Saucecrafting
      case "REAGENT" -> ConcoctionDatabase.requirements.add(CraftingRequirements.REAGENT);
        // Requires The Way of Sauce
      case "WAY" -> ConcoctionDatabase.requirements.add(CraftingRequirements.WAY);
        // Requires Deep Saucery
      case "DEEP" -> ConcoctionDatabase.requirements.add(CraftingRequirements.DEEP_SAUCERY);
        // Requires Pastamastery but not dry noodles
      case "PASTAMASTERY" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.COOK;
        ConcoctionDatabase.requirements.add(CraftingRequirements.PASTA);
      }
        // Items requiring Pastamastery
      case "PASTA" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.COOK_FANCY;
        ConcoctionDatabase.requirements.add(CraftingRequirements.PASTA);
      }
        // Requires Transcendental Noodlecraft
        // Requires Tempuramancy
      case "TEMPURAMANCY" -> ConcoctionDatabase.requirements.add(CraftingRequirements.TEMPURAMANCY);
        // Requires Patent Medicine
      case "PATENT" -> ConcoctionDatabase.requirements.add(CraftingRequirements.PATENT);
        // Requires Advanced Cocktailcrafting
      case "AC" -> ConcoctionDatabase.requirements.add(CraftingRequirements.AC);
        // Requires Superhuman Cocktailcrafting
      case "SHC" -> ConcoctionDatabase.requirements.add(CraftingRequirements.SHC);
        // Requires Salacious Cocktailcrafting
      case "SALACIOUS" -> ConcoctionDatabase.requirements.add(CraftingRequirements.SALACIOUS);
        // Items creatable only if not on Bees Hate You path
      case "NOBEE" -> ConcoctionDatabase.requirements.add(CraftingRequirements.NOBEE);
        // Saucerors make 3 of this item at a time
      case "SX3" -> ConcoctionDatabase.info.add(CraftingMisc.TRIPLE_SAUCE);
        // Recipe unexpectedly does not appear in Discoveries, even though
        // it uses a discoverable crafting type
      case "NODISCOVERY" -> ConcoctionDatabase.info.add(CraftingMisc.NODISCOVERY);
        // Recipe should never be used automatically
      case "MANUAL" -> ConcoctionDatabase.info.add(CraftingMisc.MANUAL);
        // Items requiring Transcendental Noodlecraft
      case "TNOODLE", "TRANSNOODLE" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.COOK_FANCY;
        ConcoctionDatabase.requirements.add(CraftingRequirements.TRANSNOODLE);
      }
        // Items requiring Tempuramancy
      case "TEMPURA" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.COOK_FANCY;
        ConcoctionDatabase.requirements.add(CraftingRequirements.TEMPURAMANCY);
      }
        // Items requiring Super-Advanced Meatsmithing
      case "WSMITH" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.SSMITH;
        ConcoctionDatabase.requirements.add(CraftingRequirements.SUPER_MEATSMITHING);
      }
        // Items requiring Armorcraftiness
      case "ASMITH" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.SSMITH;
        ConcoctionDatabase.requirements.add(CraftingRequirements.ARMORCRAFTINESS);
      }
        // Items requiring Advanced Cocktailcrafting
      case "ACOCK" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.MIX_FANCY;
        ConcoctionDatabase.requirements.add(CraftingRequirements.AC);
      }
        // Items requiring Superhuman Cocktailcrafting
      case "SCOCK" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.MIX_FANCY;
        ConcoctionDatabase.requirements.add(CraftingRequirements.SHC);
      }
        // Items requiring Salacious Cocktailcrafting
      case "SACOCK" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.MIX_FANCY;
        ConcoctionDatabase.requirements.add(CraftingRequirements.SALACIOUS);
      }
        // Items requiring Tiki Mixology
      case "TIKI" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.MIX;
        ConcoctionDatabase.requirements.add(CraftingRequirements.TIKI);
      }
        // Items requiring pliers and Really Expensive Jewelrycrafting
      case "EJEWEL" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.JEWELRY;
        ConcoctionDatabase.requirements.add(CraftingRequirements.EXPENSIVE);
      }
        // Items requiring Advanced Saucecrafting
      case "SAUCE" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.COOK_FANCY;
        ConcoctionDatabase.requirements.add(CraftingRequirements.REAGENT);
      }
        // Items requiring The Way of Sauce
      case "SSAUCE" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.COOK_FANCY;
        ConcoctionDatabase.requirements.add(CraftingRequirements.WAY);
      }
        // Items requiring Deep Saucery
      case "DSAUCE" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.COOK_FANCY;
        ConcoctionDatabase.requirements.add(CraftingRequirements.DEEP_SAUCERY);
      }
      case "JARLS" -> ConcoctionDatabase.mixingMethod = CraftingType.JARLS;
      case "JARLSBAKE" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.JARLS;
        ConcoctionDatabase.requirements.add(CraftingRequirements.BAKE);
      }
      case "JARLSBLEND" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.JARLS;
        ConcoctionDatabase.requirements.add(CraftingRequirements.BLEND);
      }
      case "JARLSBOIL" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.JARLS;
        ConcoctionDatabase.requirements.add(CraftingRequirements.BOIL);
      }
      case "JARLSCHOP" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.JARLS;
        ConcoctionDatabase.requirements.add(CraftingRequirements.CHOP);
      }
      case "JARLSCURDLE" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.JARLS;
        ConcoctionDatabase.requirements.add(CraftingRequirements.CURDLE);
      }
      case "JARLSFREEZE" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.JARLS;
        ConcoctionDatabase.requirements.add(CraftingRequirements.FREEZE);
      }
      case "JARLSFRY" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.JARLS;
        ConcoctionDatabase.requirements.add(CraftingRequirements.FRY);
      }
      case "JARLSGRILL" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.JARLS;
        ConcoctionDatabase.requirements.add(CraftingRequirements.GRILL);
      }
      case "JARLSSLICE" -> {
        ConcoctionDatabase.mixingMethod = CraftingType.JARLS;
        ConcoctionDatabase.requirements.add(CraftingRequirements.SLICE);
      }
      case "GRANDMA" -> ConcoctionDatabase.mixingMethod = CraftingType.GRANDMA;
      case "KRINGLE" -> ConcoctionDatabase.mixingMethod = CraftingType.KRINGLE;
      case "BEER" -> ConcoctionDatabase.mixingMethod = CraftingType.BEER;
      case "JUNK" -> ConcoctionDatabase.mixingMethod = CraftingType.JUNK;
      case "WINTER" -> ConcoctionDatabase.mixingMethod = CraftingType.WINTER;
      case "RUMPLE" -> ConcoctionDatabase.mixingMethod = CraftingType.RUMPLE;
      case "5D" -> ConcoctionDatabase.mixingMethod = CraftingType.FIVE_D;
      case "VYKEA" -> ConcoctionDatabase.mixingMethod = CraftingType.VYKEA;
      case "DUTYFREE" -> ConcoctionDatabase.mixingMethod = CraftingType.DUTYFREE;
      case "TERMINAL" -> ConcoctionDatabase.mixingMethod = CraftingType.TERMINAL;
      case "BARREL" -> ConcoctionDatabase.mixingMethod = CraftingType.BARREL;
      case "WAX" -> ConcoctionDatabase.mixingMethod = CraftingType.WAX;
      case "SPANT" -> ConcoctionDatabase.mixingMethod = CraftingType.SPANT;
      case "XOSHOP" -> ConcoctionDatabase.mixingMethod = CraftingType.XO;
      case "SLIEMCE" -> ConcoctionDatabase.mixingMethod = CraftingType.SLIEMCE;
      case "SPACEGATE" -> ConcoctionDatabase.mixingMethod = CraftingType.SPACEGATE;
      case "NEWSPAPER" -> ConcoctionDatabase.mixingMethod = CraftingType.NEWSPAPER;
      case "METEOROID" -> ConcoctionDatabase.mixingMethod = CraftingType.METEOROID;
      case "SAUSAGE_O_MATIC" -> ConcoctionDatabase.mixingMethod = CraftingType.SAUSAGE_O_MATIC;
      case "FANTASY_REALM" -> ConcoctionDatabase.mixingMethod = CraftingType.FANTASY_REALM;
      case "STILLSUIT" -> ConcoctionDatabase.mixingMethod = CraftingType.STILLSUIT;
      case "WOOL" -> ConcoctionDatabase.mixingMethod = CraftingType.WOOL;
      case "SHADOW_FORGE" -> ConcoctionDatabase.mixingMethod = CraftingType.SHADOW_FORGE;
      case "BURNING_LEAVES" -> ConcoctionDatabase.mixingMethod = CraftingType.BURNING_LEAVES;
      case "TINKERING_BENCH" -> ConcoctionDatabase.mixingMethod = CraftingType.TINKERING_BENCH;
      case "MAYAM" -> ConcoctionDatabase.mixingMethod = CraftingType.MAYAM;
      case "PHOTO_BOOTH" -> ConcoctionDatabase.mixingMethod = CraftingType.PHOTO_BOOTH;
      case "TAKERSPACE" -> ConcoctionDatabase.mixingMethod = CraftingType.TAKERSPACE;
      case "GNOME_PART" -> ConcoctionDatabase.mixingMethod = CraftingType.GNOME_PART;
      default -> {
        if (mix.startsWith("ROW")) {
          ConcoctionDatabase.row = StringUtilities.parseInt(mix.substring(3));
        } else {
          RequestLogger.printLine(
              "Unknown mixing method or flag (" + mix + ") for concoction: " + name);
        }
      }
    }

    if (currentMixingMethod != null && currentMixingMethod != ConcoctionDatabase.mixingMethod) {
      RequestLogger.printLine("Multiple mixing methods for concoction: " + name);
    }
  }

  public static final void setPullsRemaining(final int pullsRemaining) {
    ConcoctionDatabase.pullsRemaining = pullsRemaining;

    if (!StaticEntity.isHeadless()) {
      ItemManageFrame.updatePullsRemaining(pullsRemaining);
      NamedListenerRegistry.fireChange("(pullsremaining)");
    }

    if (pullsRemaining < pullsBudgeted) {
      ConcoctionDatabase.setPullsBudgeted(pullsRemaining);
    }
  }

  public static final void setPullsBudgeted(int pullsBudgeted) {
    if (pullsBudgeted < queuedPullsUsed) {
      pullsBudgeted = queuedPullsUsed;
    }

    if (pullsBudgeted > pullsRemaining) {
      pullsBudgeted = pullsRemaining;
    }

    ConcoctionDatabase.pullsBudgeted = pullsBudgeted;

    if (!StaticEntity.isHeadless()) {
      ItemManageFrame.updatePullsBudgeted(pullsBudgeted);
    }
  }

  public static void retrieveCafeMenus() {
    // The Crimbo Cafe is open
    if (KoLConstants.cafeItems.isEmpty()) {
      CrimboCafeRequest.getMenu();
    }

    // If the person is in Bad Moon, retrieve
    // information from Hell's Kitchen.

    if (KoLCharacter.inBadMoon() && KoLConstants.kitchenItems.isEmpty()) {
      HellKitchenRequest.getMenu();
    }

    // If the person is in a canadia sign, retrieve
    // information from the restaurant.

    if (KoLCharacter.canEat()
        && KoLCharacter.canadiaAvailable()
        && KoLConstants.restaurantItems.isEmpty()) {
      ChezSnooteeRequest.getMenu();
    }

    // If the person is in a gnomad sign and the beach is
    // open, retrieve information from the microbrewery.

    if (KoLCharacter.canDrink()
        && KoLCharacter.gnomadsAvailable()
        && KoLConstants.microbreweryItems.isEmpty()) {
      MicroBreweryRequest.getMenu();
    }
  }

  public static class QueuedConcoction {
    private final Concoction concoction;
    private final int count;
    private final ArrayList<AdventureResult> ingredients;
    private final int meat;
    private final int pulls;
    private final int tomes;
    private final int stills;
    private final int extrudes;
    private final int adventures;
    private final int freeCrafts;

    public QueuedConcoction(
        final Concoction c,
        final int count,
        final ArrayList<AdventureResult> ingredients,
        final int meat,
        final int pulls,
        final int tomes,
        final int stills,
        final int extrudes,
        final int adventures,
        final int freeCrafts) {
      this.concoction = c;
      this.count = count;
      this.ingredients = ingredients;
      this.meat = meat;
      this.pulls = pulls;
      this.tomes = tomes;
      this.stills = stills;
      this.extrudes = extrudes;
      this.adventures = adventures;
      this.freeCrafts = freeCrafts;
    }

    public Concoction getConcoction() {
      return this.concoction;
    }

    public int getCount() {
      return this.count;
    }

    public ArrayList<AdventureResult> getIngredients() {
      return this.ingredients;
    }

    public int getMeat() {
      return this.meat;
    }

    public int getPulls() {
      return this.pulls;
    }

    public int getTomes() {
      return this.tomes;
    }

    public int getStills() {
      return this.stills;
    }

    public int getExtrudes() {
      return this.extrudes;
    }

    public int getAdventures() {
      return this.adventures;
    }

    public int getFreeCrafts() {
      return this.freeCrafts;
    }

    public String getName() {
      return this.concoction.getName();
    }

    public int getItemId() {
      return this.concoction.getItemId();
    }

    @Override
    public String toString() {
      return this.concoction.getName();
    }

    @Override
    public boolean equals(final Object o) {
      return o instanceof QueuedConcoction
          && this.concoction.equals(((QueuedConcoction) o).concoction)
          && this.count == ((QueuedConcoction) o).count;
    }

    @Override
    public int hashCode() {
      int hash = (this.concoction != null ? this.concoction.hashCode() : 0);
      hash = 31 * hash + this.count;
      return hash;
    }
  }

  public static class UsableConcoctions {
    private Map<ConcoctionType, LockableListModel<Concoction>> usableMap = new TreeMap<>();

    public UsableConcoctions() {
      for (ConcoctionType type : ConcoctionType.values()) {
        usableMap.put(type, new LockableListModel<>());
      }
    }

    public void fill() {
      this.usableMap.clear();
      this.usableMap.putAll(
          ConcoctionPool.concoctions().stream()
              .collect(
                  Collectors.groupingBy(
                      c -> c.type, Collectors.toCollection(LockableListModel<Concoction>::new))));
    }

    private static <T extends Comparable<T>> boolean isSorted(Iterable<T> iterable) {
      T last = null;
      boolean sorted = true;
      for (T current : iterable) {
        if (last != null && last.compareTo(current) > 0) {
          sorted = false;
        }
        last = current;
      }
      return sorted;
    }

    public int size() {
      return this.usableMap.values().stream().mapToInt(List::size).sum();
    }

    public boolean contains(Concoction c) {
      return this.usableMap.get(c.type).contains(c);
    }

    public LockableListModel<Concoction> get(ConcoctionType type) {
      return this.usableMap.get(type);
    }

    public Collection<LockableListModel<Concoction>> values() {
      return this.usableMap.values();
    }

    public void clear() {
      this.usableMap.values().stream().forEach(List::clear);
    }

    public void add(Concoction c) {
      // Insert the concoction so the list remains sorted.
      LockableListModel<Concoction> list = this.usableMap.get(c.type);
      int index = Collections.binarySearch(list, c);
      if (index < 0) {
        // binarySearch returns negative if it's not in the list, and tells us where to put it.
        index = -index - 1;
        list.add(index, c);
      } else {
        list.set(index, c);
      }
    }

    public void addAll(Collection<Concoction> toAdd) {
      for (Map.Entry<ConcoctionType, LockableListModel<Concoction>> entry :
          this.usableMap.entrySet()) {
        ConcoctionType type = entry.getKey();
        LockableListModel<Concoction> list = entry.getValue();

        Collection<Concoction> toAddWithOrder =
            toAdd.stream().filter(c -> c.type == type).collect(Collectors.toList());
        // Choose strategy based on size m of addition list. Adding/sorting takes O(m+nlogn), and
        // inserting repeatedly takes O(mn), so the rough breakpoint is m=logn.
        if (toAddWithOrder.size() > Math.log(list.size()) / Math.log(2)) {
          // long list of additions. append dumbly and then sort.
          list.addAll(toAddWithOrder);
          list.sort();
        } else {
          // short list. insert one-by-one.
          for (Concoction c : toAddWithOrder) {
            this.add(c);
          }
        }
      }
    }

    public void removeAll(Collection<Concoction> toRemove) {
      for (Map.Entry<ConcoctionType, LockableListModel<Concoction>> entry :
          this.usableMap.entrySet()) {
        ConcoctionType type = entry.getKey();
        LockableListModel<Concoction> list = entry.getValue();

        Collection<Concoction> toRemoveWithOrder =
            toRemove.stream().filter(c -> c.type == type).collect(Collectors.toList());
        list.removeAll(toRemoveWithOrder);
      }
    }

    public void sort() {
      this.sort(false);
    }

    public void sort(boolean sortNone) {
      for (Map.Entry<ConcoctionType, LockableListModel<Concoction>> entry :
          this.usableMap.entrySet()) {
        ConcoctionType type = entry.getKey();
        List<Concoction> concoctions = entry.getValue();
        if ((type != ConcoctionType.NONE || sortNone) && !UsableConcoctions.isSorted(concoctions)) {
          Collections.sort(concoctions);
        }
      }
    }

    public void updateFilter(boolean changeDetected) {
      this.usableMap.values().stream().forEach(l -> l.updateFilter(changeDetected));
    }
  }
}
