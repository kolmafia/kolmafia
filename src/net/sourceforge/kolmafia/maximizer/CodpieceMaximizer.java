package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
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

  private record Gem(CheckedItem item, Modifiers mods, double delta, int count) {}

  private final Evaluator evaluator;

  private final List<Gem> catalogue = new ArrayList<>();
  private final Map<Integer, Integer> countsByItemId = new HashMap<>();
  private Mode mode = Mode.INACTIVE;
  private List<Gem> candidates = List.of();
  private int[] available = new int[0];
  private List<Slot> freeSlots = List.of();

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
      this.catalogue.add(new Gem(item, mods, delta, count));
    }
    this.catalogue.sort(
        Comparator.comparingDouble(Gem::delta)
            .reversed()
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
      AdventureResult item = this.candidates.get(pos).item();
      int copies = Math.min(this.available[pos], slotsLeft);
      for (int used = 1; used <= copies; used++) {
        spec.equipment.put(this.claimSlot(item, filled + used - 1), item);
        this.searchGems(spec, filled + used, pos + 1);
      }
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
}
