package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

/**
 * Eternity Codpiece gem support for the Modifier Maximizer.
 *
 * <p>Gems bypass ordinary equipment candidate loops, so they are discovered once and searched for
 * each complete equipment branch. Search only mutates the speculation's equipment map.
 */
final class CodpieceMaximizer {
  private enum Mode {
    INACTIVE,
    FIXED,
    SEARCHABLE
  }

  /**
   * Modifiers whose score contribution is an uncapped sum of raw source values. Anything else
   * disables the primary bound; a primary-neutral suffix may still use its tiebreaker bound.
   */
  private static final EnumSet<DoubleModifier> SUMMABLE_MODIFIERS =
      EnumSet.of(
          DoubleModifier.ADVENTURES,
          DoubleModifier.BOOZEDROP,
          DoubleModifier.BUGBEAR_DAMAGE,
          DoubleModifier.CANDYDROP,
          DoubleModifier.DAMAGE_ABSORPTION,
          DoubleModifier.DAMAGE_REDUCTION,
          DoubleModifier.ENCHANTMENT_COUNT,
          DoubleModifier.FAMILIAR_DAMAGE,
          DoubleModifier.FAMILIAR_EXP,
          DoubleModifier.FISHING_SKILL,
          DoubleModifier.FOODDROP,
          DoubleModifier.GHOST_DAMAGE,
          DoubleModifier.HP_REGEN_MAX,
          DoubleModifier.HP_REGEN_MIN,
          DoubleModifier.MONSTER_LEVEL,
          DoubleModifier.MOX_PCT,
          DoubleModifier.MP_PCT,
          DoubleModifier.MP_REGEN_MAX,
          DoubleModifier.MP_REGEN_MIN,
          DoubleModifier.MUS_PCT,
          DoubleModifier.MYS_PCT,
          DoubleModifier.PICKPOCKET_CHANCE,
          DoubleModifier.POOL_SKILL,
          DoubleModifier.PVP_FIGHTS,
          DoubleModifier.SEAL_DAMAGE,
          DoubleModifier.VAMPIRE_DAMAGE,
          DoubleModifier.WEREWOLF_DAMAGE,
          DoubleModifier.ZOMBIE_DAMAGE);

  private static final EnumSet<DoubleModifier> BOUNDABLE_TIEBREAKER_MODIFIERS =
      EnumSet.of(
          DoubleModifier.ACCESSORYDROP,
          DoubleModifier.BOOZEDROP,
          DoubleModifier.COLD_DAMAGE,
          DoubleModifier.COLD_RESISTANCE,
          DoubleModifier.COLD_SPELL_DAMAGE,
          DoubleModifier.CRITICAL_PCT,
          DoubleModifier.DAMAGE_ABSORPTION,
          DoubleModifier.DAMAGE_REDUCTION,
          DoubleModifier.DB_COMBAT_DAMAGE,
          DoubleModifier.FAMILIAR_EXP,
          DoubleModifier.FOODDROP,
          DoubleModifier.FUMBLE,
          DoubleModifier.HATDROP,
          DoubleModifier.HOT_DAMAGE,
          DoubleModifier.HOT_RESISTANCE,
          DoubleModifier.HOT_SPELL_DAMAGE,
          DoubleModifier.HP_REGEN_MAX,
          DoubleModifier.INITIATIVE,
          DoubleModifier.ITEMDROP,
          DoubleModifier.MEATDROP,
          DoubleModifier.MP_REGEN_MAX,
          DoubleModifier.OFFHANDDROP,
          DoubleModifier.PANTSDROP,
          DoubleModifier.RANGED_DAMAGE,
          DoubleModifier.SHIRTDROP,
          DoubleModifier.SIXGUN_DAMAGE,
          DoubleModifier.SLEAZE_DAMAGE,
          DoubleModifier.SLEAZE_RESISTANCE,
          DoubleModifier.SLEAZE_SPELL_DAMAGE,
          DoubleModifier.SPELL_DAMAGE,
          DoubleModifier.SPOOKY_DAMAGE,
          DoubleModifier.SPOOKY_RESISTANCE,
          DoubleModifier.SPOOKY_SPELL_DAMAGE,
          DoubleModifier.STENCH_DAMAGE,
          DoubleModifier.STENCH_RESISTANCE,
          DoubleModifier.STENCH_SPELL_DAMAGE,
          DoubleModifier.WEAPON_DAMAGE,
          DoubleModifier.WEAPONDROP);

  private record Gem(
      CheckedItem item,
      Modifiers mods,
      double delta,
      int count,
      boolean affectsPrimaryScore,
      boolean hasBoundableTiebreaker,
      boolean dropsItems,
      boolean dropsMeat) {}

  private final Evaluator evaluator;

  private final List<Gem> catalogue = new ArrayList<>();
  private final Map<Integer, Integer> countsByItemId = new HashMap<>();
  private Mode mode = Mode.INACTIVE;
  private List<Gem> candidates = List.of();
  private int[] available = new int[0];
  private List<Slot> freeSlots = List.of();
  private ScoreBound bound;

  CodpieceMaximizer(Evaluator evaluator) {
    this.evaluator = evaluator;
  }

  boolean isSearchable() {
    return this.mode == Mode.SEARCHABLE;
  }

  int reservedCopiesInGemSlots(Map<Slot, AdventureResult> equipment, AdventureResult item) {
    return this.mode == Mode.INACTIVE ? 0 : copiesInGemSlots(equipment, item);
  }

  int availableForOrdinaryEquipment(Map<Slot, AdventureResult> equipment, CheckedItem item) {
    if (this.mode == Mode.INACTIVE) return item.getCount();

    int available = item.getCount() - copiesInGemSlots(equipment, item);
    return item.singleFlag ? Math.min(1, available) : available;
  }

  void releaseInstalledCopyForOrdinaryUse(Map<Slot, AdventureResult> equipment, CheckedItem item) {
    if (this.mode != Mode.INACTIVE
        || !InventoryManager.equippedOrInInventory(ItemPool.THE_ETERNITY_CODPIECE)) {
      return;
    }

    int installed = copiesInGemSlots(equipment, item);
    if (installed == 0) return;
    if (countInEquipment(equipment, item, SlotSet.SLOTS) < item.initial - installed) {
      return;
    }

    for (Slot slot : SlotSet.CODPIECE_SLOTS) {
      AdventureResult gem = equipment.get(slot);
      if (gem != null && gem.getItemId() == item.getItemId()) {
        equipment.put(slot, EquipmentRequest.UNEQUIP);
        return;
      }
    }
  }

  boolean hasEquippedGem(Map<Slot, AdventureResult> equipment, AdventureResult item) {
    return this.mode != Mode.INACTIVE
        && KoLCharacter.hasEquipped(equipment, EquipmentManager.ETERNITY_CODPIECE)
        && copiesInGemSlots(equipment, item) > 0;
  }

  private int availableCopies(AdventureResult item) {
    if (item == null || item == EquipmentRequest.UNEQUIP) return 0;
    Integer count = this.countsByItemId.get(item.getItemId());
    if (count != null) return count;
    count =
        ItemDatabase.isAllowed(item)
            ? InventoryManager.getAccessibleCount(item)
            : InventoryManager.getCount(item) + InventoryManager.getEquippedCount(item);
    if (!InventoryManager.equippedOrInInventory(ItemPool.THE_ETERNITY_CODPIECE)) {
      count += liveCopiesInGemSlots(item);
    }
    return count;
  }

  private static int countInEquipment(
      Map<Slot, AdventureResult> equipment, AdventureResult item, Iterable<Slot> slots) {
    int count = 0;
    for (Slot slot : slots) {
      AdventureResult equipped = equipment.get(slot);
      if (equipped != null && equipped.getItemId() == item.getItemId()) count++;
    }
    return count;
  }

  private static int liveCopiesInGemSlots(AdventureResult item) {
    int count = 0;
    for (Slot slot : SlotSet.CODPIECE_SLOTS) {
      AdventureResult gem = EquipmentManager.getEquipment(slot);
      if (gem != null && gem.getItemId() == item.getItemId()) count++;
    }
    return count;
  }

  private static boolean hasInstalledGems() {
    return SlotSet.CODPIECE_SLOTS.stream()
        .map(EquipmentManager::getEquipment)
        .anyMatch(item -> item != null && item != EquipmentRequest.UNEQUIP);
  }

  static int maxUsefulGemCopies(int itemId) {
    Modifiers modifiers = ModifierDatabase.getItemModifiers(itemId);
    int equipmentLimit =
        !ItemDatabase.isEquipment(itemId)
            ? 0
            : modifiers != null && modifiers.getBoolean(BooleanModifier.SINGLE) ? 1 : 3;
    return SlotSet.CODPIECE_SLOTS.size() + equipmentLimit;
  }

  private static int copiesInGemSlots(Map<Slot, AdventureResult> equipment, AdventureResult item) {
    return countInEquipment(equipment, item, SlotSet.CODPIECE_SLOTS);
  }

  void initialize(
      boolean accessoryAvailable,
      EquipScope equipScope,
      int maxPrice,
      PriceLevel priceLevel,
      double nullScore)
      throws MaximizerInterruptedException {
    this.mode = Mode.INACTIVE;
    this.countsByItemId.clear();
    if (!Preferences.getBoolean("maximizerConsiderCodpieceGems")) {
      if (hasInstalledGems()) this.mode = Mode.FIXED;
      return;
    }

    boolean codpieceWorn = KoLCharacter.hasEquipped(ItemPool.THE_ETERNITY_CODPIECE);
    AdventureResult codpiece = EquipmentManager.ETERNITY_CODPIECE;
    if ((!accessoryAvailable && !codpieceWorn)
        || (this.evaluator.isForbidden(codpiece) && !codpieceWorn)
        || (!EquipmentManager.canEquip(ItemPool.THE_ETERNITY_CODPIECE) && !codpieceWorn)) {
      return;
    }

    CheckedItem checkedCodpiece =
        new CheckedItem(ItemPool.THE_ETERNITY_CODPIECE, equipScope, maxPrice, priceLevel);
    if (checkedCodpiece.getCount() == 0 || !ItemDatabase.isAllowed(checkedCodpiece)) return;

    this.discoverGems(equipScope, maxPrice, priceLevel, nullScore);
    if (!this.catalogue.isEmpty()) this.mode = Mode.SEARCHABLE;
  }

  /**
   * Finds usable gems, retaining installed, required, threshold, and interacting candidates even
   * when their isolated score is not positive.
   */
  private void discoverGems(
      EquipScope equipScope, int maxPrice, PriceLevel priceLevel, double nullScore)
      throws MaximizerInterruptedException {
    for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
      if (!entry.getKey().isInt()) continue;
      int itemId = entry.getKey().getIntValue();
      AdventureResult gem = ItemPool.get(itemId, 1);
      if (this.evaluator.isForbidden(gem)) continue;
      Modifiers mods = ModifierDatabase.getModifiers(ModifierType.ETERNITY_CODPIECE, itemId);
      if (mods == null) continue;

      int slotted = liveCopiesInGemSlots(gem);
      boolean required = this.evaluator.isRequired(gem);
      double delta = this.evaluator.getScore(mods, Map.of(Slot.NONE, gem), Map.of()) - nullScore;
      boolean improvesTiebreaker =
          this.evaluator.isUsingTiebreaker() && this.evaluator.getTiebreaker(mods) > 0.0;
      Evaluator.Constraint constraint = this.evaluator.checkConstraints(mods);
      boolean contributesToThreshold = this.evaluator.contributesToThreshold(mods);
      boolean contributesToInteractingScore = this.contributesToInteractingScore(mods);
      boolean hasNonNumericModifiers = hasScoreRelevantNonNumericModifiers(mods);
      if (constraint == Evaluator.Constraint.VIOLATES) continue;
      boolean otherwiseRelevant =
          required
              || improvesTiebreaker
              || constraint != Evaluator.Constraint.IRRELEVANT
              || contributesToThreshold
              || contributesToInteractingScore;
      if (delta < 0.0 && !otherwiseRelevant) continue;
      if (delta == 0.0 && slotted == 0 && !otherwiseRelevant && !hasNonNumericModifiers) {
        continue;
      }

      boolean affectsPrimaryScore =
          delta != 0.0
              || constraint != Evaluator.Constraint.IRRELEVANT
              || contributesToThreshold
              || contributesToInteractingScore
              || hasNonNumericModifiers;
      boolean hasBoundableTiebreaker =
          !this.evaluator.contributesToNonlinearTiebreaker(mods, BOUNDABLE_TIEBREAKER_MODIFIERS)
              && !hasNonNumericModifiers;
      Modifiers itemMods = ModifierDatabase.getItemModifiers(itemId);

      CheckedItem item =
          new CheckedItem(
              itemId, equipScope, maxPrice, priceLevel, maxUsefulGemCopies(itemId), true);
      item.validate(maxPrice, priceLevel);
      int count = item.getCount();
      if (!InventoryManager.equippedOrInInventory(ItemPool.THE_ETERNITY_CODPIECE)) {
        count += slotted;
      }
      if (count == 0) continue;
      this.countsByItemId.put(itemId, count);
      this.catalogue.add(
          new Gem(
              item,
              mods,
              delta,
              count,
              affectsPrimaryScore,
              hasBoundableTiebreaker,
              itemMods != null && itemMods.getBoolean(BooleanModifier.DROPS_ITEMS),
              itemMods != null && itemMods.getBoolean(BooleanModifier.DROPS_MEAT)));
    }
    this.catalogue.sort(
        Comparator.comparing(Gem::affectsPrimaryScore)
            .reversed()
            .thenComparing(Comparator.comparingDouble(Gem::delta).reversed())
            .thenComparing(Gem::hasBoundableTiebreaker)
            .thenComparingInt(gem -> gem.item().getItemId()));
  }

  private boolean contributesToInteractingScore(Modifiers mods) {
    if (this.evaluator.contributesToNonlinearScore(mods, SUMMABLE_MODIFIERS)) return true;
    return this.evaluator.getActiveScoreModifiers().stream()
        .anyMatch(
            term ->
                term.modifier() == DoubleModifier.PRISMATIC_DAMAGE
                    && (mods.getDouble(DoubleModifier.HOT_DAMAGE) != 0.0
                        || mods.getDouble(DoubleModifier.COLD_DAMAGE) != 0.0
                        || mods.getDouble(DoubleModifier.SPOOKY_DAMAGE) != 0.0
                        || mods.getDouble(DoubleModifier.STENCH_DAMAGE) != 0.0
                        || mods.getDouble(DoubleModifier.SLEAZE_DAMAGE) != 0.0));
  }

  /** Scores gem combinations for one complete equipment branch, replacing its normal leaf score. */
  void search(MaximizerSpeculation spec) throws MaximizerInterruptedException {
    this.freeSlots = new ArrayList<>();
    for (Slot slot : SlotSet.CODPIECE_SLOTS) {
      if (spec.equipment.get(slot) == null) {
        this.freeSlots.add(slot);
      }
    }
    if (this.freeSlots.isEmpty()) {
      spec.checkBest();
      return;
    }

    if (!isCodpieceWorn(spec.equipment)) {
      // Gems only enchant while the codpiece is worn, and a codpiece that is not worn keeps
      // whatever it already holds, unless the ordinary equipment search has committed that copy
      // elsewhere.
      for (Slot slot : this.freeSlots) {
        AdventureResult gem = EquipmentManager.getEquipment(slot);
        int available = this.availableCopies(gem);
        spec.equipment.put(
            slot,
            gem != null
                    && gem != EquipmentRequest.UNEQUIP
                    && countInEquipment(spec.equipment, gem, spec.equipment.keySet()) < available
                ? gem
                : EquipmentRequest.UNEQUIP);
      }
      spec.checkBest();
      return;
    }

    for (Slot slot : this.freeSlots) {
      spec.equipment.put(slot, EquipmentRequest.UNEQUIP);
    }
    int filled = this.selectCandidates(spec);
    this.bound = this.buildBound(spec, this.freeSlots.size() - filled);
    this.searchGems(spec, filled, 0);
  }

  /**
   * Selects available candidates and places required gems, subtracting copies committed elsewhere.
   *
   * @return how many free gem slots were filled with required gems
   */
  private int selectCandidates(MaximizerSpeculation spec) {
    this.candidates = new ArrayList<>();
    List<Integer> counts = new ArrayList<>();
    int filled = 0;
    for (Gem gem : this.catalogue) {
      AdventureResult item = gem.item();
      int count = gem.count() - countInEquipment(spec.equipment, item, spec.equipment.keySet());
      if (count <= 0) continue;

      // Placing a required gem first is only a shortcut. One that does not fit still fails the
      // speculation, through Evaluator.checkEquipment, on every leaf this branch reaches.
      if (filled < this.freeSlots.size()
          && this.evaluator.isRequired(item)
          && !KoLCharacter.hasEquipped(spec.equipment, item)
          && copiesInGemSlots(spec.equipment, item) == 0) {
        spec.equipment.put(this.claimSlot(item, filled++), item);
        count--;
      }
      if (count <= 0) continue;

      this.candidates.add(gem);
      counts.add(Math.min(count, this.freeSlots.size()));
    }
    this.available = counts.stream().mapToInt(Integer::intValue).toArray();
    return filled;
  }

  /** Enumerates each gem multiset once and scores every node, remaining exact without a bound. */
  private void searchGems(MaximizerSpeculation spec, int filled, int start)
      throws MaximizerInterruptedException {
    spec.checkBest();

    int slotsLeft = this.freeSlots.size() - filled;
    if (slotsLeft == 0) return;

    var mark = spec.mark();
    for (int pos = start; pos < this.candidates.size(); pos++) {
      if (this.bound != null && this.bound.cannotBeat(spec, pos, slotsLeft, Maximizer.best)) {
        // Nothing candidates[pos..] can add reaches the incumbent, so every later suffix is also
        // dead.
        break;
      }
      AdventureResult item = this.candidates.get(pos).item();
      int copies = Math.min(this.available[pos], slotsLeft);
      for (int used = 1; used <= copies; used++) {
        spec.equipment.put(this.claimSlot(item, filled + used - 1), item);
        if (this.bound != null) this.bound.select(pos, 1);
        this.searchGems(spec, filled + used, pos + 1);
      }
      if (this.bound != null) this.bound.select(pos, -copies);
      spec.restore(mark);
    }
  }

  /** Prefers a slot already holding the gem; gem slot identity does not affect score. */
  private Slot claimSlot(AdventureResult item, int index) {
    for (int i = index; i < this.freeSlots.size(); i++) {
      AdventureResult live = EquipmentManager.getEquipment(this.freeSlots.get(i));
      if (live != null && live.getItemId() == item.getItemId()) {
        Slot preferred = this.freeSlots.get(i);
        this.freeSlots.set(i, this.freeSlots.get(index));
        this.freeSlots.set(index, preferred);
        break;
      }
    }
    return this.freeSlots.get(index);
  }

  private ScoreBound buildBound(MaximizerSpeculation spec, int slotsLeft) {
    if (slotsLeft == 0 || this.candidates.isEmpty()) return null;
    List<Evaluator.ScoreModifier> scoreTerms = null;
    if (this.evaluator.scoreHasBoundablePrimaryTerms()) {
      scoreTerms = this.evaluator.getActiveScoreModifiers();
      for (Evaluator.ScoreModifier term : scoreTerms) {
        if (!SUMMABLE_MODIFIERS.contains(term.modifier())) {
          scoreTerms = null;
          break;
        }
      }
    }
    spec.setUnscored();
    return new ScoreBound(
        this.evaluator,
        scoreTerms,
        this.evaluator.getActiveTiebreakerScoreModifiers(),
        spec.calculate(),
        spec.equipment,
        spec.getModeables(),
        this.candidates,
        this.available,
        slotsLeft);
  }

  private static boolean hasScoreRelevantNonNumericModifiers(Modifiers mods) {
    return EnumSet.allOf(BitmapModifier.class).stream()
            .anyMatch(modifier -> mods.getRawBitmap(modifier) != 0)
        || EnumSet.allOf(BooleanModifier.class).stream().anyMatch(mods::getBoolean)
        || EnumSet.allOf(StringModifier.class).stream()
            .filter(
                modifier ->
                    modifier != StringModifier.MODIFIERS
                        && modifier != StringModifier.EVALUATED_MODIFIERS
                        && modifier != StringModifier.CONDITIONAL_SKILL_EQUIPPED
                        && modifier != StringModifier.CONDITIONAL_SKILL_INVENTORY)
            .anyMatch(
                modifier ->
                    modifier.isMultiple()
                        ? !mods.getStrings(modifier).isEmpty()
                        : !mods.getString(modifier).isEmpty());
  }

  private static boolean isCodpieceWorn(Map<Slot, AdventureResult> equipment) {
    return KoLCharacter.hasEquipped(equipment, EquipmentManager.ETERNITY_CODPIECE);
  }

  private static int countBooleanItems(
      Map<Slot, AdventureResult> equipment, BooleanModifier modifier) {
    int count = 0;
    for (AdventureResult item : equipment.values()) {
      if (item == null) continue;
      Modifiers mods = ModifierDatabase.getItemModifiers(item.getItemId());
      if (mods != null && mods.getBoolean(modifier)) count++;
    }
    return count;
  }

  /**
   * Lexicographic upper bounds in {@link MaximizerSpeculation#compareTo} order. Each suffix table
   * stores the most favorable raw contribution reachable with its remaining candidates and slots;
   * directed rounding keeps the bound optimistic. Unsupported semantics retain exact enumeration.
   */
  private static final class ScoreBound {
    private final Evaluator evaluator;
    private final List<Evaluator.ScoreModifier> scoreTerms;
    private final List<Evaluator.ScoreModifier> tiebreakerTerms;
    private final double constant;
    private final double[][] scorePerCopy;
    private final double[] scoreBaseline;
    private final double[] selected;
    private final double[][][] scoreSuffix;
    private final double itemBonusBaseline;
    private final double[] itemBonusPerCopy;
    private final double modBonusBaseline;
    private final double[] modBonusPerCopy;
    private final int[] selectedBonusCopies;
    private final double[][] itemBonusSuffix;
    private final double[][] modBonusSuffix;
    private final double[][] tiebreakerPerCopy;
    private final double[][][] tiebreakerSuffix;
    private final int[][] itemDropperSuffix;
    private final int[][] meatDropperSuffix;
    private final boolean[] primaryNeutralSuffix;
    private final boolean[] boundableTiebreakerSuffix;

    ScoreBound(
        Evaluator evaluator,
        List<Evaluator.ScoreModifier> scoreTerms,
        List<Evaluator.ScoreModifier> tiebreakerTerms,
        Modifiers baseMods,
        Map<Slot, AdventureResult> equipment,
        Map<Modeable, String> modeables,
        List<Gem> candidates,
        int[] available,
        int slotsLeft) {
      this.evaluator = evaluator;
      this.scoreTerms = scoreTerms;
      this.tiebreakerTerms = tiebreakerTerms;
      this.constant = baseMods.hasString(StringModifier.ROLLOVER_EFFECT) ? 0.01f : 0.0;
      int size = scoreTerms == null ? 0 : scoreTerms.size();
      this.scoreBaseline = new double[size];
      this.selected = new double[size];
      this.scorePerCopy = new double[candidates.size()][size];
      for (int t = 0; t < size; t++) {
        DoubleModifier modifier = (DoubleModifier) scoreTerms.get(t).modifier();
        this.scoreBaseline[t] = baseMods.getDouble(modifier);
        for (int c = 0; c < candidates.size(); c++) {
          this.scorePerCopy[c][t] =
              modifier == DoubleModifier.ADVENTURES && !KoLCharacter.canGainRolloverAdventures()
                  ? 0.0
                  : candidates.get(c).mods().getDouble(modifier);
        }
      }
      this.scoreSuffix = new double[size][][];
      for (int t = 0; t < size; t++) {
        this.scoreSuffix[t] =
            this.buildSuffix(scoreTerms, this.scorePerCopy, t, available, slotsLeft);
      }

      double itemBonusBaseline = 0.0;
      double modBonusBaseline = 0.0;
      for (AdventureResult item : equipment.values()) {
        if (item == null) continue;
        itemBonusBaseline =
            Math.nextUp(itemBonusBaseline + evaluator.getItemBonus(item, modeables));
        modBonusBaseline = Math.nextUp(modBonusBaseline + evaluator.getModBonus(item, modeables));
      }
      this.itemBonusBaseline = itemBonusBaseline;
      this.modBonusBaseline = modBonusBaseline;
      this.itemBonusPerCopy = new double[candidates.size()];
      this.modBonusPerCopy = new double[candidates.size()];
      for (int c = 0; c < candidates.size(); c++) {
        AdventureResult item = candidates.get(c).item();
        this.itemBonusPerCopy[c] = evaluator.getItemBonus(item, modeables);
        this.modBonusPerCopy[c] = evaluator.getModBonus(item, modeables);
      }
      this.selectedBonusCopies = new int[candidates.size()];
      this.itemBonusSuffix = this.buildBonusSuffix(this.itemBonusPerCopy, available, slotsLeft);
      this.modBonusSuffix = this.buildBonusSuffix(this.modBonusPerCopy, available, slotsLeft);

      this.tiebreakerPerCopy = new double[candidates.size()][tiebreakerTerms.size()];
      for (int t = 0; t < tiebreakerTerms.size(); t++) {
        DoubleModifier modifier = (DoubleModifier) tiebreakerTerms.get(t).modifier();
        for (int c = 0; c < candidates.size(); c++) {
          double value =
              this.evaluator.getTiebreakerScoreValue(
                      tiebreakerTerms.get(t), candidates.get(c).mods())
                  - this.evaluator.getTiebreakerScoreValue(tiebreakerTerms.get(t), new Modifiers());
          this.tiebreakerPerCopy[c][t] =
              value > 0.0 ? value * tiebreakerMultiplier(modifier) : value;
        }
      }
      this.tiebreakerSuffix = new double[tiebreakerTerms.size()][][];
      for (int t = 0; t < tiebreakerTerms.size(); t++) {
        this.tiebreakerSuffix[t] =
            this.buildSuffix(tiebreakerTerms, this.tiebreakerPerCopy, t, available, slotsLeft);
      }
      this.itemDropperSuffix = this.buildCountSuffix(candidates, available, slotsLeft, true);
      this.meatDropperSuffix = this.buildCountSuffix(candidates, available, slotsLeft, false);

      this.primaryNeutralSuffix = new boolean[candidates.size() + 1];
      this.boundableTiebreakerSuffix = new boolean[candidates.size() + 1];
      this.primaryNeutralSuffix[candidates.size()] = true;
      this.boundableTiebreakerSuffix[candidates.size()] = true;
      for (int c = candidates.size() - 1; c >= 0; c--) {
        this.primaryNeutralSuffix[c] =
            this.primaryNeutralSuffix[c + 1] && !candidates.get(c).affectsPrimaryScore();
        this.boundableTiebreakerSuffix[c] =
            this.boundableTiebreakerSuffix[c + 1] && candidates.get(c).hasBoundableTiebreaker();
      }
    }

    private int[][] buildCountSuffix(
        List<Gem> candidates, int[] available, int maxSlots, boolean itemDropper) {
      int size = candidates.size();
      int[][] suffix = new int[size + 1][maxSlots + 1];
      for (int c = size - 1; c >= 0; c--) {
        int value =
            (itemDropper ? candidates.get(c).dropsItems() : candidates.get(c).dropsMeat()) ? 1 : 0;
        for (int slots = 1; slots <= maxSlots; slots++) {
          suffix[c][slots] = Math.min(slots, value * available[c] + suffix[c + 1][slots]);
        }
      }
      return suffix;
    }

    private static double tiebreakerMultiplier(DoubleModifier modifier) {
      return switch (modifier) {
        case INITIATIVE -> 2.0;
        case COLD_DAMAGE,
            COLD_SPELL_DAMAGE,
            HOT_DAMAGE,
            HOT_SPELL_DAMAGE,
            SLEAZE_DAMAGE,
            SLEAZE_SPELL_DAMAGE,
            SPOOKY_DAMAGE,
            SPOOKY_SPELL_DAMAGE,
            STENCH_DAMAGE,
            STENCH_SPELL_DAMAGE ->
            3.0;
        default -> 1.0;
      };
    }

    private double[][] buildSuffix(
        List<Evaluator.ScoreModifier> terms,
        double[][] perCopy,
        int term,
        int[] available,
        int maxSlots) {
      int size = perCopy.length;
      boolean maximize = terms.get(term).weight() >= 0.0;
      // suffix[size][*] stays zero: with no candidates left nothing more can be contributed.
      double[][] suffix = new double[size + 1][maxSlots + 1];
      for (int c = size - 1; c >= 0; c--) {
        double value = perCopy[c][term];
        for (int slots = 1; slots <= maxSlots; slots++) {
          double best = suffix[c + 1][slots];
          for (int used = 1; used <= Math.min(available[c], slots); used++) {
            double total = used * value + suffix[c + 1][slots - used];
            total = maximize ? Math.nextUp(total) : Math.nextDown(total);
            best = maximize ? Math.max(best, total) : Math.min(best, total);
          }
          suffix[c][slots] = best;
        }
      }
      return suffix;
    }

    void select(int candidate, int copies) {
      for (int t = 0; t < this.selected.length; t++) {
        this.selected[t] += copies * this.scorePerCopy[candidate][t];
      }
      this.selectedBonusCopies[candidate] += copies;
    }

    boolean cannotBeat(
        MaximizerSpeculation spec, int pos, int slotsLeft, MaximizerSpeculation incumbent) {
      double incumbentScore = incumbent.getScore();
      if (!this.primaryNeutralSuffix[pos] && this.scoreTerms != null) {
        if (incumbent.failed) return false;
        return this.scoreUpperBound(pos, slotsLeft) < incumbentScore;
      }
      if (!this.primaryNeutralSuffix[pos]) return false;

      double score = spec.getScore();
      if (spec.failed != incumbent.failed) return spec.failed;
      if (score < incumbentScore) return true;
      if (score > incumbentScore
          || KoLCharacter.inBeecore()
          || !this.boundableTiebreakerSuffix[pos]) {
        return false;
      }

      int incumbentItemDroppers =
          countBooleanItems(incumbent.equipment, BooleanModifier.DROPS_ITEMS);
      int itemDroppers =
          countBooleanItems(spec.equipment, BooleanModifier.DROPS_ITEMS)
              + this.itemDropperSuffix[pos][slotsLeft];
      if (itemDroppers < incumbentItemDroppers) return true;
      if (itemDroppers > incumbentItemDroppers) return false;

      int incumbentMeatDroppers =
          countBooleanItems(incumbent.equipment, BooleanModifier.DROPS_MEAT);
      int meatDroppers =
          countBooleanItems(spec.equipment, BooleanModifier.DROPS_MEAT)
              + this.meatDropperSuffix[pos][slotsLeft];
      if (meatDroppers < incumbentMeatDroppers) return true;
      if (meatDroppers > incumbentMeatDroppers) return false;

      double tiebreakerBound = spec.getTiebreaker();
      Modifiers mods = spec.getModifiers();
      for (int t = 0; t < this.tiebreakerTerms.size(); t++) {
        Evaluator.ScoreModifier term = this.tiebreakerTerms.get(t);
        DoubleModifier modifier = (DoubleModifier) term.modifier();
        double value = this.evaluator.getTiebreakerScoreValue(term, mods);
        double withRemaining = value + this.tiebreakerSuffix[t][pos][slotsLeft];
        double improvement =
            term.weight() * (Math.min(withRemaining, term.max()) - Math.min(value, term.max()));
        tiebreakerBound = Math.nextUp(tiebreakerBound + Math.nextUp(improvement));
      }
      return tiebreakerBound < incumbent.getTiebreaker();
    }

    private double scoreUpperBound(int pos, int slotsLeft) {
      double score = 0.0;
      for (int t = 0; t < this.scoreTerms.size(); t++) {
        Evaluator.ScoreModifier term = this.scoreTerms.get(t);
        double value =
            this.scoreBaseline[t] + this.selected[t] + this.scoreSuffix[t][pos][slotsLeft];
        score = Math.nextUp(score + Math.nextUp(term.weight() * Math.min(value, term.max())));
      }
      score = Math.nextUp(score + this.itemBonusBaseline);
      for (int c = 0; c < this.itemBonusPerCopy.length; c++) {
        score = Math.nextUp(score + this.selectedBonusCopies[c] * this.itemBonusPerCopy[c]);
      }
      score = Math.nextUp(score + this.itemBonusSuffix[pos][slotsLeft]);
      score = Math.nextUp(score + this.modBonusBaseline);
      for (int c = 0; c < this.modBonusPerCopy.length; c++) {
        score = Math.nextUp(score + this.selectedBonusCopies[c] * this.modBonusPerCopy[c]);
      }
      score = Math.nextUp(score + this.modBonusSuffix[pos][slotsLeft]);
      score = Math.nextUp(score + this.constant);
      return score;
    }

    private double[][] buildBonusSuffix(double[] perCopy, int[] available, int maxSlots) {
      int size = perCopy.length;
      double[][] suffix = new double[size + 1][maxSlots + 1];
      for (int c = size - 1; c >= 0; c--) {
        for (int slots = 1; slots <= maxSlots; slots++) {
          double best = suffix[c + 1][slots];
          for (int used = 1; used <= Math.min(available[c], slots); used++) {
            double total = Math.nextUp(used * perCopy[c] + suffix[c + 1][slots - used]);
            best = Math.max(best, total);
          }
          suffix[c][slots] = best;
        }
      }
      return suffix;
    }
  }
}
