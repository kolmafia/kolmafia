package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ZodiacZone;
import net.sourceforge.kolmafia.KoLConstants.filterType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.QuantumTerrariumRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.session.EffectAvailability;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MallPriceManager;
import net.sourceforge.kolmafia.swingui.MaximizerFrame;
import net.sourceforge.kolmafia.utilities.IntOrString;

/**
 * Application-facing coordinator for one maximize request.
 *
 * <p>This class owns the public entry points and the ordered workflow: capture current state,
 * compile and search equipment, turn the winning loadout into equipment changes, evaluate effect
 * sources, and publish {@link Boost} recommendations. Expression semantics belong to {@link
 * MaximizerTermRegistry} and {@link Evaluator}; equipment search belongs to {@link
 * EquipmentSearchRunner}; effect-specific availability belongs to {@link EffectSourceDispatcher}.
 *
 * <p>The static fields mirror the long-standing GUI and scripting API. Per-run mutable search state
 * is kept in {@link MaximizerSession} so it does not leak into the search algorithm.
 */
public class Maximizer {
  private static boolean firstTime = true;

  public static final LockableListModel<Boost> boosts = new LockableListModel<>();
  public static Evaluator eval;

  public static String[] maximizationCategories = {
    "_hoboPower",
    "_brimstone",
    "_cloathing",
    "_slimeHate",
    "_stickers",
    "_folderholder",
    "_cardsleeve",
    "_smithsness",
    "_mcHugeLarge",
  };

  static MaximizerLoadout best;
  static int bestChecked;
  static long bestUpdate;
  static long combinationLimit;
  private static MaximizerSession session;

  private Maximizer() {}

  public static boolean lastMaximizeSucceeded() {
    return best != null && best.isScored() && !best.failed();
  }

  public static SearchMetrics lastSearchMetrics() {
    return session == null ? SearchMetrics.EMPTY : session.metrics();
  }

  static Evaluator evaluator() {
    return eval;
  }

  static MaximizerLoadout best() {
    return session == null ? best : session.best;
  }

  static void setBest(MaximizerLoadout candidate) {
    best = candidate;
    if (session != null) session.best = candidate;
  }

  static void recordCandidateCounts(int catalog, int shortlisted) {
    if (session == null) return;
    session.catalogCandidates = catalog;
    session.shortlistedCandidates = shortlisted;
  }

  static void recordScoreCalculation() {
    if (session != null && session.searchingEquipment) ++session.scoreCalculations;
  }

  static void recordSearch(long nodes, long dominancePrunes, long boundPrunes, boolean optimal) {
    if (session == null) return;
    session.searchNodes += nodes;
    session.dominancePrunes += dominancePrunes;
    session.boundPrunes += boundPrunes;
    if (!optimal) session.searchComplete = false;
  }

  static void startSearch(boolean exhaustive) {
    if (session == null) return;
    session.startSearch(exhaustive ? 0 : Preferences.getInteger("maximizerSearchTimeLimit"));
  }

  static boolean keepSearching() {
    return session == null || session.keepSearching();
  }

  static void consider(MaximizerLoadout candidate) throws MaximizerInterruptedException {
    if (session == null) {
      throw new IllegalStateException("Cannot consider equipment outside a maximizer session");
    }
    try {
      session.consider(candidate);
    } finally {
      best = session.best;
      bestChecked = session.combinationsChecked;
      bestUpdate = session.nextProgressUpdate;
    }
  }

  static CharacterSnapshot character() {
    return session == null || !session.active || session.character == null
        ? CharacterSnapshot.capture(Maximizer.evaluator())
        : session.character;
  }

  public static boolean maximize(
      String maximizerString,
      int maxPrice,
      PriceLevel priceLevel,
      EquipScope equipScope,
      Set<filterType> filter) {
    MaximizerFrame.expressionSelect.setSelectedItem(maximizerString);

    // iECOC has to be turned off before actually maximizing as
    // it would cause all item lookups during the process to just
    // print the item name and return null.

    KoLmafiaCLI.isExecutingCheckOnlyCommand = false;

    Maximizer.maximize(maximizerString, equipScope, maxPrice, priceLevel, false, filter);

    if (!KoLmafia.permitsContinue()) {
      return false;
    }

    Modifiers mods = Maximizer.best().calculate();
    ModifierDatabase.overrideModifier(ModifierType.GENERATED, "_spec", mods);

    return !Maximizer.best().failed();
  }

  public static boolean maximize(
      String maximizerString, int maxPrice, PriceLevel priceLevel, boolean speculateOnly) {
    EquipScope equipScope = speculateOnly ? EquipScope.SPECULATE_INVENTORY : EquipScope.EQUIP_NOW;
    return maximize(
        maximizerString, maxPrice, priceLevel, equipScope, EnumSet.allOf(filterType.class));
  }

  public static void maximize(
      String maximizerString,
      EquipScope equipScope,
      int maxPrice,
      PriceLevel priceLevel,
      boolean includeAll,
      Set<filterType> filter) {
    maximize(maximizerString, equipScope, maxPrice, priceLevel, includeAll, filter, false);
  }

  static void maximizeExhaustively(
      String maximizerString,
      EquipScope equipScope,
      int maxPrice,
      PriceLevel priceLevel,
      boolean includeAll,
      Set<filterType> filter) {
    maximize(maximizerString, equipScope, maxPrice, priceLevel, includeAll, filter, true);
  }

  private static void maximize(
      String maximizerString,
      EquipScope equipScope,
      int maxPrice,
      PriceLevel priceLevel,
      boolean includeAll,
      Set<filterType> filter,
      boolean exhaustive) {
    var previousSession = Maximizer.session;
    try {
      maximizeRun(
          maximizerString, equipScope, maxPrice, priceLevel, includeAll, filter, exhaustive);
    } finally {
      if (Maximizer.session != previousSession) {
        Maximizer.session.finish();
      }
    }
  }

  private static void maximizeRun(
      String maximizerString,
      EquipScope equipScope,
      int maxPrice,
      PriceLevel priceLevel,
      boolean includeAll,
      Set<filterType> filter,
      boolean exhaustive) {
    KoLmafia.forceContinue();
    String maxMe = maximizerString;
    RequestLogger.printLine("Maximizer: " + maxMe);
    RequestLogger.updateSessionLog("Maximizer: " + maxMe);
    KoLConstants.maximizerMList.addItem(maxMe);
    Maximizer.eval = new Evaluator(maxMe);
    int filterCount = filter.size();
    var limitMode = KoLCharacter.getLimitMode();

    Maximizer.best = new MaximizerLoadout();
    Maximizer.combinationLimit = Preferences.getLong("maximizerCombinationLimit");
    Maximizer.session = new MaximizerSession(Maximizer.best, combinationLimit);
    Maximizer.bestChecked = 0;
    Maximizer.bestUpdate = 0;

    if (!KoLmafia.permitsContinue() || filterCount == 0) {
      return;
    }

    if (KoLCharacter.inQuantum()) {
      RequestThread.postRequest(new QuantumTerrariumRequest());
    }
    // Refreshing status discovers passive skills that can change modifier evaluation.
    ApiRequest.updateStatus();
    KoLCharacter.recalculateAdjustments();
    Maximizer.session.refreshCharacterSnapshot(Maximizer.evaluator());
    double current =
        Maximizer.evaluator()
            .getScore(
                KoLCharacter.getCurrentModifiers(),
                EquipmentManager.currentEquipment(),
                Modeable.getStateMap());

    if (maxPrice <= 0) {
      maxPrice = Preferences.getInteger("autoBuyPriceLimit");
    }

    KoLmafia.updateDisplay(
        Maximizer.firstTime ? "Maximizing (1st time may take a while)..." : "Maximizing...");
    Maximizer.firstTime = false;

    Maximizer.boosts.clear();
    if (filter.contains(KoLConstants.filterType.EQUIP)) {
      Maximizer.best().getScore();
      MaximizerLoadout currentEquipment = Maximizer.best().clone();
      // Allow an equal-scoring configuration to replace current equipment.
      Maximizer.best().markFailed();
      Maximizer.session.resetSearch();
      Maximizer.bestChecked = Maximizer.session.combinationsChecked;
      Maximizer.bestUpdate = Maximizer.session.nextProgressUpdate;
      try {
        try {
          Maximizer.evaluator().enumerateEquipment(equipScope, maxPrice, priceLevel, exhaustive);
        } finally {
          Maximizer.session.finishCandidateCompilation();
        }
        if (!Maximizer.session.searchComplete) {
          Maximizer.boosts.add(
              new Boost(
                  "",
                  "<font color=red>(hit search time limit, optimality not guaranteed)</font>",
                  Slot.NONE,
                  null,
                  0.0));
        }
      } catch (MaximizerExceededException e) {
        Maximizer.boosts.add(
            new Boost(
                "", "(maximum achieved, no further combinations checked)", Slot.NONE, null, 0.0));
      } catch (MaximizerLimitException e) {
        Maximizer.boosts.add(
            new Boost(
                "",
                "<font color=red>(hit combination limit, optimality not guaranteed)</font>",
                Slot.NONE,
                null,
                0.0));
      } catch (MaximizerInterruptedException e) {
        KoLmafia.forceContinue();
        Maximizer.boosts.add(
            new Boost(
                "",
                "<font color=red>(interrupted, optimality not guaranteed)</font>",
                Slot.NONE,
                null,
                0.0));
      }
      if (!currentEquipment.failed() && Maximizer.best().getScore() < currentEquipment.getScore()) {
        Maximizer.setBest(currentEquipment);
      }
      Maximizer.session.showProgress();

      EnumSet<Slot> alreadyDone = EnumSet.noneOf(Slot.class);

      for (Slot slot : SlotSet.ACCESSORY_SLOTS) {
        if (Maximizer.best().equipment.get(slot).getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE
            && EquipmentManager.getEquipment(slot).getItemId() != ItemPool.SPECIAL_SAUCE_GLOVE) {
          equipScope = Maximizer.emitSlot(slot, equipScope, maxPrice, priceLevel, current);
          alreadyDone.add(slot);
        }
      }

      for (var slot : SlotSet.ALL_SLOTS) {
        if (!alreadyDone.contains(slot)) {
          equipScope = Maximizer.emitSlot(slot, equipScope, maxPrice, priceLevel, current);
        }
      }
    }

    current =
        Maximizer.evaluator()
            .getScore(
                KoLCharacter.getCurrentModifiers(),
                EquipmentManager.currentEquipment(),
                Modeable.getStateMap());

    // Show only equipment
    if (filter.contains(filterType.EQUIP) && filterCount == 1) {
      return;
    }

    // Include skills from absorbing items in Noobcore
    if (KoLCharacter.inNoobcore()) {
      for (Map.Entry<IntOrString, String> entry :
          ModifierDatabase.getAllModifiersOfType(ModifierType.SKILL)) {
        if (!entry.getKey().isInt()) continue;
        int skillId = entry.getKey().getIntValue();
        if (skillId < 23001 || skillId > 23125) {
          continue;
        }
        if (KoLCharacter.hasSkill(skillId)) {
          continue;
        }
        int absorbsLeft = KoLCharacter.getAbsorbsLimit() - KoLCharacter.getAbsorbs();
        if (absorbsLeft < 1) {
          continue;
        }
        MaximizerLoadout loadout = new MaximizerLoadout();
        String mods = entry.getValue();
        loadout.setCustom(mods);
        double delta = loadout.getScore() - current;
        if (delta <= 0.0) {
          continue;
        }
        int[] itemList = ItemDatabase.getItemListByNoobSkillId(skillId);
        if (itemList == null) {
          continue;
        }
        int count = 0;
        for (int itemId : itemList) {
          var makeable = getAbsorbable(itemId, equipScope, maxPrice, priceLevel);
          if (!makeable.canMake()) continue;
          String cmd = makeable.cmd();
          String text = makeable.txt();
          text = text + KoLConstants.MODIFIER_FORMAT.format(delta) + ")";
          text = text + " [" + absorbsLeft + " absorbs remaining]";
          if (count > 0) {
            text = "  or " + text;
          }
          Maximizer.boosts.add(new Boost(cmd, text, ItemPool.get(itemId), delta));
          count++;
        }
      }

      // Include enchantments from absorbing equipment in Noobcore
      for (Map.Entry<IntOrString, String> entry :
          ModifierDatabase.getAllModifiersOfType(ModifierType.ITEM)) {
        if (!entry.getKey().isInt()) continue;
        int itemId = entry.getKey().getIntValue();
        int absorbsLeft = KoLCharacter.getAbsorbsLimit() - KoLCharacter.getAbsorbs();
        if (absorbsLeft < 1) {
          continue;
        }
        if (!ItemDatabase.isDiscardable(itemId)) {
          continue;
        }
        if (!ItemDatabase.isTradeable(itemId) && !ItemDatabase.isGiftItem(itemId)) {
          continue;
        }
        if (!ItemDatabase.isEquipment(itemId) || ItemDatabase.isFamiliarEquipment(itemId)) {
          continue;
        }
        MaximizerLoadout loadout = new MaximizerLoadout();
        Modifiers itemMods = ModifierDatabase.getItemModifiers(itemId);
        if (itemMods == null) {
          continue;
        }
        // Only take numeric modifiers, and not Surgeonosity, from Items in Noobcore
        StringJoiner mods = new StringJoiner(", ");
        for (var mod : DoubleModifier.DOUBLE_MODIFIERS) {
          if (itemMods.getDouble(mod) != 0.0)
            mods.add(mod.getName() + ": " + itemMods.getDouble(mod));
        }
        if (mods.length() == 0) {
          continue;
        }
        loadout.setCustom(mods.toString());
        double delta = loadout.getScore() - current;
        if (delta <= 0.0) {
          continue;
        }
        var makeable = getAbsorbable(itemId, equipScope, maxPrice, priceLevel);
        if (!makeable.canMake()) continue;
        String cmd = makeable.cmd();
        String text =
            makeable.txt()
                + "lasts til end of day, "
                + KoLConstants.MODIFIER_FORMAT.format(delta)
                + ")";
        ItemAvailability availability = makeable.checkedItem().availability();
        List<String> details = new ArrayList<>(List.of(absorbsLeft + " absorbs remaining"));
        if (availability.inventory() > 0) details.add(availability.inventory() + " in inventory");
        if (availability.initial() - availability.inventory() > 0)
          details.add((availability.initial() - availability.inventory()) + " obtainable");
        if (availability.creatable() > 0) details.add(availability.creatable() + " createable");
        if (availability.npcBuyable() > 0) details.add(availability.npcBuyable() + " NPC buyable");
        if (availability.pullable() > 0) details.add(availability.pullable() + " pullable");
        text += " [" + String.join(", ", details) + "]";
        Maximizer.boosts.add(new Boost(cmd, text, ItemPool.get(itemId), delta));
      }
    }

    if (filter.contains(KoLConstants.filterType.OTHER)) {
      for (Map.Entry<IntOrString, String> entry :
          ModifierDatabase.getAllModifiersOfType(ModifierType.HORSERY)) {
        if (!entry.getKey().isString()) continue;
        String name = entry.getKey().getStringValue();
        if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Horsery contract")) {
          continue;
        }
        MaximizerLoadout loadout = new MaximizerLoadout();
        loadout.setHorsery(name);
        double delta = loadout.getScore() - current;
        if (delta <= 0.0) continue;
        String cmd = "horsery " + name;
        String text = cmd;
        if (!Preferences.getBoolean("horseryAvailable")) {
          cmd = "";
          if (includeAll) {
            text = "(get a horsery and ride a " + name + ")";
          } else continue;
        }
        text += " (" + KoLConstants.MODIFIER_FORMAT.format(delta) + ")";
        int price = Preferences.getString("_horsery").isEmpty() ? 0 : 500;
        if (KoLCharacter.getAvailableMeat() < price) cmd = "";
        if (Preferences.getBoolean("verboseMaximizer")) text += " [" + price + " meat]";
        Maximizer.boosts.add(new Boost(cmd, text, name, delta));
      }

      for (Map.Entry<IntOrString, String> entry :
          ModifierDatabase.getAllModifiersOfType(ModifierType.BOOM_BOX)) {
        if (!entry.getKey().isString()) continue;
        String name = entry.getKey().getStringValue();
        MaximizerLoadout loadout = new MaximizerLoadout();
        loadout.setBoomBox(name);
        double delta = loadout.getScore() - current;
        if (delta <= 0.0) continue;
        String cmd = "boombox " + name.toLowerCase();
        String text = cmd;
        if (!InventoryManager.hasItem(ItemPool.BOOMBOX)) {
          cmd = "";
          if (includeAll) {
            text = "(get a SongBoom&trade; BoomBox and play " + name + ")";
          } else continue;
        }
        int usesRemaining = Preferences.getInteger("_boomBoxSongsLeft");
        text += " (" + KoLConstants.MODIFIER_FORMAT.format(delta) + ")";
        if (Preferences.getBoolean("verboseMaximizer"))
          text += " [" + usesRemaining + (usesRemaining == 1 ? " use" : " uses") + " remaining]";
        if (usesRemaining < 1) cmd = "";
        Maximizer.boosts.add(new Boost(cmd, text, (AdventureResult) null, delta));
      }

      if (KoLCharacter.mcdAvailable() || includeAll) {
        int max = KoLCharacter.getSignZone() == ZodiacZone.CANADIA ? 11 : 10;
        // Heuristic: compare only the extreme MCD settings.
        for (int i : new int[] {0, max}) {
          MaximizerLoadout loadout = new MaximizerLoadout();
          loadout.setMindControlLevel(i);
          double delta = loadout.getScore() - current;
          if (delta <= 0.0) continue;
          String cmd = "mcd " + i;
          String text = cmd;
          if (!KoLCharacter.mcdAvailable()) {
            cmd = "";
            text = "(ascend into a non-Bad Moon sign and mcd " + i + ")";
          }
          text += " (" + KoLConstants.MODIFIER_FORMAT.format(delta) + ")";
          Maximizer.boosts.add(new Boost(cmd, text, (AdventureResult) null, delta));
        }
      }
    }

    for (Map.Entry<IntOrString, String> entry :
        ModifierDatabase.getAllModifiersOfType(ModifierType.EFFECT)) {
      if (!entry.getKey().isInt()) continue;
      int effectId = entry.getKey().getIntValue();
      if (effectId == -1) continue;

      double delta;
      boolean isSpecial = false;
      MaximizerLoadout loadout = new MaximizerLoadout();
      AdventureResult effect = EffectPool.get(effectId);
      String name = effect.getName();
      boolean hasEffect = KoLConstants.activeEffects.contains(effect);
      List<String> sources;

      if (!hasEffect) {
        loadout.addEffect(effect);
        delta = loadout.getScore() - current;
        if ((loadout.getModifiers().getRawBitmap(BitmapModifier.MUTEX_VIOLATIONS)
                & ~KoLCharacter.currentRawBitmapModifier(BitmapModifier.MUTEX_VIOLATIONS))
            != 0) { // This effect creates a mutex problem that the player
          // didn't already have.  In the future, perhaps suggest
          // uneffecting the conflicting effect, but for now just skip.
          continue;
        }
        switch (Maximizer.evaluator()
            .checkConstraints(ModifierDatabase.getEffectModifiers(effectId))) {
          case VIOLATES:
            continue;
          case IRRELEVANT:
            if (delta <= 0.0) continue;
            break;
          case MEETS:
            isSpecial = true;
        }
        sources = EffectDatabase.getAllActions(effectId);
        if (EffectAvailability.cannotGain(effectId)) {
          sources = new ArrayList<>();
        }
        if (filter.contains(KoLConstants.filterType.WISH)
            && !EffectDatabase.hasAttribute(effectId, "nohookah")) {
          sources.add("monkeypaw effect " + name);
          sources.add("genie effect " + name);
        }
        if (sources.isEmpty()) {
          if (includeAll) {
            sources = Collections.singletonList("(no known source of " + name + ")");
          } else continue;
        }
      } else {
        loadout.removeEffect(effect);
        delta = loadout.getScore() - current;
        switch (Maximizer.evaluator()
            .checkConstraints(ModifierDatabase.getEffectModifiers(effectId))) {
          case MEETS:
            continue;
          case IRRELEVANT:
            if (delta <= 0.0) continue;
            break;
          case VIOLATES:
            isSpecial = true;
        }
        String cmd = MoodManager.getDefaultAction("gain_effect", name);
        if (cmd.length() == 0) {
          if (includeAll) {
            cmd = "(find some way to remove " + name + ")";
          } else continue;
        }
        sources = Collections.singletonList(cmd);
      }

      boolean haveVipKey = InventoryManager.getCount(ItemPool.VIP_LOUNGE_KEY) > 0;
      var sourceContext =
          new EffectSourceDispatcher.Context(
              name,
              effect,
              effectId,
              includeAll,
              equipScope,
              maxPrice,
              priceLevel,
              limitMode,
              haveVipKey);
      boolean orFlag = false;
      for (var source : sources) {
        if (!KoLmafia.permitsContinue()) {
          return;
        }

        String cmd = source;
        String text = source;

        String basecommand = cmd.trim().contains(" ") ? cmd.split(" ")[0] : cmd;

        filterType sourceType =
            switch (basecommand) {
              case "cast" -> filterType.CAST;
              case "synthesize", "chew" -> filterType.SPLEEN;
              case "drink" -> filterType.BOOZE;
              case "eat" -> filterType.FOOD;
              case "use" -> filterType.USABLE;
              case "genie", "monkeypaw" -> filterType.WISH;
              default -> filterType.OTHER;
            };
        if (!filter.contains(sourceType)) continue;

        if (cmd.startsWith("#")) { // usage note, no command
          if (includeAll) {
            if (cmd.contains("BM") && !KoLCharacter.inBadMoon()) {
              continue; // no use displaying this in non-BM
            }
            text = (orFlag ? "(...or get " : "(get ") + name + " via " + cmd.substring(1) + ")";
            orFlag = false;
            cmd = "";
          } else continue;
        }

        if (hasEffect && !cmd.toLowerCase().contains(name.toLowerCase())) {
          text = text + " (to remove " + name + ")";
        }

        var plan = EffectSourceDispatcher.dispatch(cmd, text, sourceContext);
        if (cmd.startsWith("(")) { // preformatted note
          plan.command = "";
          orFlag = false;
        }
        plan = EffectSourcePlanFinalizer.finish(plan, sourceContext, delta);
        if (plan.skip) continue;

        if (orFlag) {
          plan.text = "...or " + plan.text;
        }
        Maximizer.boosts.add(
            new Boost(plan.command, plan.text, effect, hasEffect, plan.item, delta, isSpecial));
        orFlag = true;
      }
    }

    if (Maximizer.boosts.size() == 0) {
      Maximizer.boosts.add(new Boost("", "(nothing useful found)", Slot.HAT, null, 0.0));
    }

    Maximizer.boosts.sort();
  }

  private static EquipScope emitSlot(
      Slot slot, EquipScope equipScope, int maxPrice, PriceLevel priceLevel, double current) {
    if (KoLCharacter.inHatTrick() && slot == Slot.HAT) {
      return equipScope;
    }
    if (slot == Slot.FAMILIAR) { // Insert any familiar switch at this point
      FamiliarData fam = Maximizer.best().getFamiliar();
      if (!fam.equals(KoLCharacter.getFamiliar())) {
        MaximizerLoadout loadout = new MaximizerLoadout();
        loadout.setFamiliar(fam);
        double delta = loadout.getScore() - current;
        String cmd = "familiar " + fam.getRace();
        String text = cmd + " (" + KoLConstants.MODIFIER_FORMAT.format(delta) + ")";

        Boost boost = new Boost(cmd, text, fam, delta);
        if (equipScope == EquipScope.EQUIP_NOW) { // called from CLI
          boost.execute(true);
          if (!KoLmafia.permitsContinue()) equipScope = EquipScope.SPECULATE_INVENTORY;
        } else {
          Maximizer.boosts.add(boost);
        }
      }
    }

    String slotname = slot.name;
    AdventureResult item = Maximizer.best().equipment.get(slot);
    int itemId = -1;
    FamiliarData enthroned = Maximizer.best().getEnthroned();
    FamiliarData bjorned = Maximizer.best().getBjorned();
    var modeables = Maximizer.best().getModeables();
    AdventureResult curr = EquipmentManager.getEquipment(slot);

    if (item == null || item.getItemId() == 0) {
      item = EquipmentRequest.UNEQUIP;
    } else {
      itemId = item.getItemId();
    }

    FamiliarSlotGroup familiarSlots = FamiliarSlotGroup.find(itemId);
    FamiliarData familiarOccupant = familiarSlots == FamiliarSlotGroup.CROWN ? enthroned : bjorned;
    boolean changeFamiliar = familiarSlots != null && familiarOccupant != familiarSlots.current();

    var modeable = Modeable.find(itemId);
    var changeModeable =
        modeable != null && !Objects.equals(modeables.get(modeable), modeable.getState());

    if (curr.equals(item)
        && !changeFamiliar
        && !changeModeable
        && !(itemId == ItemPool.BROKEN_CHAMPAGNE
            && Preferences.getInteger("garbageChampagneCharge") == 0
            && !Preferences.getBoolean("_garbageItemChanged"))
        && !(itemId == ItemPool.MAKESHIFT_GARBAGE_SHIRT
            && Preferences.getInteger("garbageShirtCharge") == 0
            && !Preferences.getBoolean("_garbageItemChanged"))) {
      if (!SlotSet.SLOTS.contains(slot)
          || curr.equals(EquipmentRequest.UNEQUIP)
          || equipScope == EquipScope.EQUIP_NOW) {
        return equipScope;
      }
      Maximizer.boosts.add(
          new Boost("", "keep " + slotname + ": " + item.getName(), Slot.NONE, item, 0.0));
      return equipScope;
    }
    MaximizerLoadout loadout = new MaximizerLoadout();
    double baseline = current;
    var codpiece = ItemSlotGroup.ETERNITY_CODPIECE;
    if (codpiece.slots().contains(slot)
        && !KoLCharacter.hasEquipped(EquipmentManager.ETERNITY_CODPIECE)) {
      for (Slot accessorySlot : SlotSet.ACCESSORY_SLOTS) {
        AdventureResult accessory = Maximizer.best().equipment.get(accessorySlot);
        if (accessory != null && codpiece.isParent(accessory.getItemId())) {
          MaximizerLoadout codpieceLoadout = new MaximizerLoadout();
          codpieceLoadout.equip(accessorySlot, accessory);
          baseline = codpieceLoadout.getScore();
          loadout.equip(accessorySlot, accessory);
          break;
        }
      }
    }
    loadout.equip(slot, item);
    if (familiarSlots != null) {
      familiarSlots.put(loadout, familiarSlots.slots().getFirst(), familiarOccupant);
    } else if (modeable != null) {
      loadout.setModeable(modeable, modeables.get(modeable));
    }

    double delta = loadout.getScore() - baseline;

    String cmd, text;
    if (item.equals(EquipmentRequest.UNEQUIP)) {
      item = curr;
      cmd = "unequip " + slotname;
      text = cmd + " (" + curr.getName() + ", " + KoLConstants.MODIFIER_FORMAT.format(delta) + ")";
    } else {
      if (changeFamiliar) {
        cmd =
            (familiarSlots == FamiliarSlotGroup.CROWN ? "enthrone " : "bjornify ")
                + familiarOccupant.getRace();
        text = cmd;
      } else if (changeModeable) {
        text = modeable.getCommand() + " " + modeables.get(modeable);
        cmd = text;
        if (modeable.mustEquipAfterChange())
          cmd += "; equip " + slotname + " \u00B6" + item.getItemId();
      } else {
        cmd = "equip " + slotname + " \u00B6" + item.getItemId();
        text = "equip " + slotname + " " + item.getName();
      }
      text = text + " (";

      CheckedItem checkedItem =
          new CheckedItem(
              itemId, equipScope, maxPrice, priceLevel, codpiece.slots().contains(slot));

      long price = 0L;

      // How many have been needed so far to make this maximization set?
      // We need 1 + that number to equip this item, not just 1
      int count = 0;

      // Assumption: immediate execution has already changed earlier live slots, while speculative
      // output has not. TODO: verify this ordering contract end to end.
      if (equipScope == EquipScope.EQUIP_NOW) {
        for (var piece : SlotSet.ALL_SLOTS) {
          if (piece.ordinal() >= slot.ordinal()) break;
          AdventureResult equipped = EquipmentManager.getEquipment(piece);
          if (equipped != null && item.getItemId() == equipped.getItemId()) {
            count++;
          }
        }
      } else {
        for (var piece : SlotSet.ALL_SLOTS) {
          if (piece.ordinal() >= slot.ordinal()) break;
          if (item.equals(Maximizer.best().equipment.get(piece))) {
            count++;
          }
        }
      }

      // We might want to fold for a new Garbage item, even if we already have it, to reset it
      if ((itemId == ItemPool.BROKEN_CHAMPAGNE
              && Preferences.getInteger("garbageChampagneCharge") == 0)
          || (itemId == ItemPool.MAKESHIFT_GARBAGE_SHIRT
              && Preferences.getInteger("garbageShirtCharge") == 0
              && !Preferences.getBoolean("_garbageItemChanged"))) {
        if (checkedItem.availability().initial() > count) {
          text = "fold & " + text;
          cmd = "fold \u00B6" + item.getItemId() + ";" + cmd;
        }
        if (curr.equals(item)) {
          text = "unequip & " + text;
          cmd = "unequip " + slotname + ";" + cmd;
        }
      }

      // Accessible copies may come from inventory, closet, or storage; retrieveItem chooses which.
      if (!curr.equals(item)) {
        switch (checkedItem.acquisitionMethod(count)) {
          case ACCESSIBLE -> {
            // simRetrieveItem needs the requested copy count, not the candidate's aggregate count.
            String method =
                InventoryManager.simRetrieveItem(
                    ItemPool.get(item.getItemId(), count + 1),
                    equipScope == EquipScope.EQUIP_NOW,
                    false);
            if (!method.equals("have")) {
              text = method + " & " + text;
            }
            cmd =
                switch (method) {
                  case "uncloset" -> "closet take 1 \u00B6" + item.getItemId() + ";" + cmd;
                  case "unstash" -> "stash take 1 \u00B6" + item.getItemId() + ";" + cmd;
                  // Should be only hitting this after Ronin I think
                  case "pull" -> "pull 1 \u00B6" + item.getItemId() + ";" + cmd;
                  default -> cmd;
                };
          }
          case CREATE -> {
            text = "make & " + text;
            cmd = "make \u00B6" + item.getItemId() + ";" + cmd;
            price = ConcoctionPool.get(item).price;
          }
          case NPC_BUY -> {
            text = "buy & " + text;
            cmd = "buy 1 \u00B6" + item.getItemId() + ";" + cmd;
            price = ConcoctionPool.get(item).price;
          }
          case FOLD -> {
            // Availability records one representative fold source; this can choose incorrectly
            // when several source item types are available.
            String method =
                InventoryManager.simRetrieveItem(
                    ItemPool.get(checkedItem.availability().foldItemId(), count + 1));
            if (method.equals("have") || method.equals("remove")) {
              text = "fold & " + text;
              cmd = "fold \u00B6" + item.getItemId() + ";" + cmd;
            } else {
              text = method + " & fold & " + text;
              cmd =
                  "acquire 1 \u00B6"
                      + checkedItem.availability().foldItemId()
                      + ";fold \u00B6"
                      + item.getItemId()
                      + ";"
                      + cmd;
            }
          }
          case PULL -> {
            text = "pull & " + text;
            cmd = "pull \u00B6" + item.getItemId() + ";" + cmd;
          }
          case PULL_FOLD -> {
            // Availability records one representative fold source; this can choose incorrectly
            // when several source item types are available.
            text = "pull & fold & " + text;
            cmd =
                "pull 1 \u00B6"
                    + checkedItem.availability().foldItemId()
                    + ";fold \u00B6"
                    + item.getItemId()
                    + ";"
                    + cmd;
          }
          case STORAGE_BUY -> {
            text = "buy & pull & " + text;
            cmd = "buy using storage 1 \u00B6" + itemId + ";pull \u00B6" + itemId + ";" + cmd;
            if (priceLevel != PriceLevel.DONT_CHECK) {
              price = MallPriceManager.getMallPrice(itemId);
            }
          }
          case MALL_BUY -> {
            text = "acquire & " + text;
            if (priceLevel != PriceLevel.DONT_CHECK) {
              price = MallPriceManager.getMallPrice(itemId);
            }
          }
          default -> throw new IllegalStateException("Unsupported acquisition method");
        }
      }

      if (price > 0) {
        text = text + KoLConstants.COMMA_FORMAT.format(price) + " meat, ";
      }
      text = text + KoLConstants.MODIFIER_FORMAT.format(delta) + ")";
    }

    Boost boost = new Boost(cmd, text, slot, item, delta, enthroned, bjorned, modeables);
    if (equipScope == EquipScope.EQUIP_NOW) { // called from CLI
      boost.execute(true);
      if (!KoLmafia.permitsContinue()) {
        equipScope = EquipScope.SPECULATE_INVENTORY;
        Maximizer.boosts.add(boost);
      }
    } else {
      Maximizer.boosts.add(boost);
    }
    return equipScope;
  }

  private record Makeable(String cmd, String txt, boolean canMake, CheckedItem checkedItem) {}

  private static Makeable getAbsorbable(
      int itemId, EquipScope equipScope, int maxPrice, PriceLevel priceLevel) {
    CheckedItem checkedItem = new CheckedItem(itemId, equipScope, maxPrice, priceLevel);
    // Unavailable absorbables are omitted because listing every theoretical source is overwhelming.
    long price = 0L;
    AdventureResult item = ItemPool.get(itemId);
    ItemAvailability availability = checkedItem.availability();
    String cmd = "absorb \u00B6" + itemId;
    String text = "absorb " + item.getName() + " (";
    AcquisitionMethod acquisition =
        availability.firstMethod(
            candidate ->
                candidate != AcquisitionMethod.FOLD && candidate != AcquisitionMethod.PULL_FOLD);
    boolean canMake = acquisition != null;
    if (canMake) {
      switch (acquisition) {
        case ACCESSIBLE -> {
          if (availability.inventory() == 0) {
            String method =
                InventoryManager.simRetrieveItem(item, equipScope == EquipScope.EQUIP_NOW, false);
            if (!method.equals("have")) {
              text = method + " & " + text;
            }
            if (method.equals("uncloset")) {
              cmd = "closet take 1 \u00B6" + itemId + ";" + cmd;
            } else if (method.equals("pull")) {
              cmd = "pull 1 \u00B6" + itemId + ";" + cmd;
            }
          }
        }
        case CREATE -> {
          text = "make & " + text;
          cmd = "make \u00B6" + itemId + ";" + cmd;
          price = ConcoctionPool.get(item).price;
        }
        case NPC_BUY -> {
          text = "buy & " + text;
          cmd = "buy 1 \u00B6" + itemId + ";" + cmd;
          price = ConcoctionPool.get(item).price;
        }
        case PULL -> {
          text = "pull & " + text;
          cmd = "pull \u00B6" + itemId + ";" + cmd;
        }
        case MALL_BUY -> {
          text = "acquire & " + text;
          if (priceLevel != PriceLevel.DONT_CHECK) {
            price = MallPriceManager.getMallPrice(itemId);
          }
        }
        case STORAGE_BUY -> {
          text = "buy & pull & " + text;
          cmd = "buy using storage 1 \u00B6" + itemId + ";pull \u00B6" + itemId + ";" + cmd;
          if (priceLevel != PriceLevel.DONT_CHECK) {
            price = MallPriceManager.getMallPrice(itemId);
          }
        }
        case FOLD, PULL_FOLD -> throw new IllegalStateException("Unsupported acquisition method");
      }
    }
    if (price > 0) text += KoLConstants.COMMA_FORMAT.format(price) + " meat, ";
    return new Makeable(cmd, text, canMake, checkedItem);
  }
}
