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

final class CodpieceMaximizer {
  enum SearchMode {
    FIXED,
    EXACT
  }

  private enum Mode {
    INACTIVE,
    FIXED,
    SEARCHABLE
  }

  private record Gem(CheckedItem item, double delta, int count) {}

  private final Evaluator evaluator;
  private final List<Gem> catalogue = new ArrayList<>();
  private final Map<Integer, Integer> countsByItemId = new HashMap<>();
  private Mode mode = Mode.INACTIVE;
  private List<Gem> candidates = List.of();
  private int[] available = new int[0];
  private List<Slot> freeSlots = List.of();
  private MaximizerBranchBound[][] suffixBounds = new MaximizerBranchBound[0][0];
  private MaximizerBranchBound.InterchangeableSlotGroup interchangeableSlotGroup;
  private List<CheckedItem> candidateItems = List.of();
  private MaximizerSpeculation boundBaseline;

  CodpieceMaximizer(Evaluator evaluator) {
    this.evaluator = evaluator;
  }

  boolean isSearchable() {
    return this.mode == Mode.SEARCHABLE;
  }

  static SearchMode configuredSearchMode() {
    String value = Preferences.getString("maximizerConsiderCodpieceGems").trim().toLowerCase();
    return switch (value) {
      case "exact" -> SearchMode.EXACT;
      default -> SearchMode.FIXED;
    };
  }

  int reservedCopiesInGemSlots(Map<Slot, AdventureResult> equipment, AdventureResult item) {
    return this.mode == Mode.INACTIVE ? 0 : copiesInGemSlots(equipment, item);
  }

  int availableForOrdinaryEquipment(Map<Slot, AdventureResult> equipment, CheckedItem item) {
    if (this.mode == Mode.INACTIVE) return item.getCount();
    int reserved =
        InventoryManager.equippedOrInInventory(ItemPool.THE_ETERNITY_CODPIECE)
            ? copiesInGemSlots(equipment, item)
            : 0;
    int available = item.getCount() - reserved;
    return item.singleFlag ? Math.min(1, available) : available;
  }

  void releaseInstalledCopyForOrdinaryUse(Map<Slot, AdventureResult> equipment, CheckedItem item) {
    if (this.mode != Mode.INACTIVE
        || !InventoryManager.equippedOrInInventory(ItemPool.THE_ETERNITY_CODPIECE)) {
      return;
    }

    int installed = copiesInGemSlots(equipment, item);
    if (installed == 0) return;
    if (countInEquipment(equipment, item, SlotSet.SLOTS) < item.initial - installed) return;

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

  static int maxUsefulGemCopies(int itemId) {
    Modifiers modifiers = ModifierDatabase.getItemModifiers(itemId);
    int equipmentLimit =
        !ItemDatabase.isEquipment(itemId)
            ? 0
            : modifiers != null && modifiers.getBoolean(BooleanModifier.SINGLE) ? 1 : 3;
    return SlotSet.CODPIECE_SLOTS.size() + equipmentLimit;
  }

  void initialize(
      boolean accessoryAvailable,
      EquipScope equipScope,
      int maxPrice,
      PriceLevel priceLevel,
      double nullScore)
      throws MaximizerInterruptedException {
    this.mode = Mode.INACTIVE;
    this.catalogue.clear();
    this.countsByItemId.clear();
    if (configuredSearchMode() == SearchMode.FIXED) {
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

  private void discoverGems(
      EquipScope equipScope, int maxPrice, PriceLevel priceLevel, double nullScore)
      throws MaximizerInterruptedException {
    for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
      if (!entry.getKey().isInt()) continue;
      int itemId = entry.getKey().getIntValue();
      AdventureResult gem = ItemPool.get(itemId, 1);
      if (this.evaluator.isForbidden(gem)) continue;
      Modifiers modifiers = ModifierDatabase.getModifiers(ModifierType.ETERNITY_CODPIECE, itemId);
      if (modifiers == null) continue;

      int slotted = liveCopiesInGemSlots(gem);
      boolean required = this.evaluator.isRequired(gem);
      double delta =
          this.evaluator.getScore(modifiers, Map.of(Slot.NONE, gem), Map.of()) - nullScore;
      boolean improvesTiebreaker =
          this.evaluator.isUsingTiebreaker() && this.evaluator.getTiebreaker(modifiers) > 0.0;
      Evaluator.Constraint constraint = this.evaluator.checkConstraints(modifiers);
      boolean otherwiseRelevant =
          required
              || improvesTiebreaker
              || constraint != Evaluator.Constraint.IRRELEVANT
              || this.evaluator.contributesToThreshold(modifiers)
              || this.contributesToInteractingScore(modifiers);
      if (constraint == Evaluator.Constraint.VIOLATES || (delta < 0.0 && !otherwiseRelevant)) {
        continue;
      }
      if (delta == 0.0
          && slotted == 0
          && !otherwiseRelevant
          && !hasScoreRelevantModifiers(modifiers)) {
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
      this.catalogue.add(new Gem(item, delta, count));
    }
    this.catalogue.sort(
        Comparator.comparingDouble(Gem::delta)
            .reversed()
            .thenComparingInt(gem -> gem.item().getItemId()));
  }

  private boolean contributesToInteractingScore(Modifiers modifiers) {
    if (this.evaluator.contributesToCombinedScore(modifiers)) return true;
    if (!this.evaluator.scoresAny(List.of(DoubleModifier.PRISMATIC_DAMAGE))) return false;
    return modifiers.getDouble(DoubleModifier.HOT_DAMAGE) != 0.0
        || modifiers.getDouble(DoubleModifier.COLD_DAMAGE) != 0.0
        || modifiers.getDouble(DoubleModifier.SPOOKY_DAMAGE) != 0.0
        || modifiers.getDouble(DoubleModifier.STENCH_DAMAGE) != 0.0
        || modifiers.getDouble(DoubleModifier.SLEAZE_DAMAGE) != 0.0;
  }

  List<CheckedItem> branchBoundCandidates() {
    return this.catalogue.stream().map(Gem::item).toList();
  }

  Map<Integer, Integer> branchBoundCounts() {
    return Map.copyOf(this.countsByItemId);
  }

  void search(MaximizerSpeculation spec) throws MaximizerInterruptedException {
    this.freeSlots = new ArrayList<>();
    for (Slot slot : SlotSet.CODPIECE_SLOTS) {
      if (spec.equipment.get(slot) == null) this.freeSlots.add(slot);
    }
    if (this.freeSlots.isEmpty()) {
      spec.checkBest();
      return;
    }

    if (!isCodpieceWorn(spec.equipment)) {
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

    for (Slot slot : this.freeSlots) spec.equipment.put(slot, EquipmentRequest.UNEQUIP);
    int filled = this.selectCandidates(spec);
    this.prepareSuffixBounds(spec, filled);
    this.seedIncumbent(spec, filled);
    this.searchGems(spec, filled, 0);
  }

  private void seedIncumbent(MaximizerSpeculation spec, int filled)
      throws MaximizerInterruptedException {
    var mark = spec.mark();
    for (int pos = 0; pos < this.candidates.size() && filled < this.freeSlots.size(); pos++) {
      AdventureResult item = this.candidates.get(pos).item();
      int copies = Math.min(this.available[pos], this.freeSlots.size() - filled);
      for (int used = 0; used < copies; used++) {
        spec.equipment.put(this.claimSlot(item, filled++), item);
      }
    }
    spec.checkBest();
    spec.restore(mark);
  }

  private void prepareSuffixBounds(MaximizerSpeculation spec, int filled) {
    int capacity = this.freeSlots.size() - filled;
    this.interchangeableSlotGroup = CodpieceBranchBound.create(this.evaluator, spec, capacity);
    this.candidateItems = this.candidates.stream().map(Gem::item).toList();
    this.boundBaseline = spec.clone();
    this.suffixBounds = new MaximizerBranchBound[this.candidates.size() + 1][capacity + 1];
  }

  private int selectCandidates(MaximizerSpeculation spec) {
    this.candidates = new ArrayList<>();
    List<Integer> counts = new ArrayList<>();
    int filled = 0;
    for (Gem gem : this.catalogue) {
      AdventureResult item = gem.item();
      int count = gem.count() - countInEquipment(spec.equipment, item, spec.equipment.keySet());
      if (count <= 0) continue;

      if (filled < this.freeSlots.size()
          && this.evaluator.isRequired(item)
          && !KoLCharacter.hasEquipped(spec.equipment, item)
          && countInEquipment(spec.equipment, item, SlotSet.CODPIECE_SLOTS) == 0) {
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

  private void searchGems(MaximizerSpeculation spec, int filled, int start)
      throws MaximizerInterruptedException {
    spec.checkBest();
    int slotsLeft = this.freeSlots.size() - filled;
    if (slotsLeft == 0) return;
    if (start >= this.candidates.size() || this.cannotBeat(spec, start, slotsLeft)) return;

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

  private boolean cannotBeat(MaximizerSpeculation spec, int start, int slotsLeft) {
    if (this.evaluator.hasFiniteScoreMaximum()) return false;
    var bound = this.suffixBound(start, slotsLeft);
    return bound.cannotBeat(spec, Maximizer.currentBest(), MaximizerBranchBound.Stage.CODPIECE);
  }

  private MaximizerBranchBound suffixBound(int start, int slotsLeft) {
    var bound = this.suffixBounds[start][slotsLeft];
    if (bound != null) return bound;
    var remaining =
        CodpieceBranchBound.remaining(
            this.interchangeableSlotGroup, this.candidateItems, this.available, start, slotsLeft);
    bound =
        MaximizerBranchBound.create(
            this.evaluator,
            this.boundBaseline,
            new SlotList<>(0),
            0.0,
            EnumSet.noneOf(MaximizerBranchBound.Category.class),
            List.of(remaining));
    this.suffixBounds[start][slotsLeft] = bound;
    return bound;
  }

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

  private static boolean hasScoreRelevantModifiers(Modifiers modifiers) {
    return EnumSet.allOf(BitmapModifier.class).stream()
            .anyMatch(modifier -> modifiers.getRawBitmap(modifier) != 0)
        || EnumSet.allOf(BooleanModifier.class).stream().anyMatch(modifiers::getBoolean)
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
                        ? !modifiers.getStrings(modifier).isEmpty()
                        : !modifiers.getString(modifier).isEmpty());
  }

  private static int countInEquipment(
      Map<Slot, AdventureResult> equipment, AdventureResult item, Iterable<Slot> slots) {
    if (item == null || item == EquipmentRequest.UNEQUIP) return 0;
    int count = 0;
    for (Slot slot : slots) {
      AdventureResult equipped = equipment.get(slot);
      if (equipped != null && equipped.getItemId() == item.getItemId()) count++;
    }
    return count;
  }

  private static int copiesInGemSlots(Map<Slot, AdventureResult> equipment, AdventureResult item) {
    return countInEquipment(equipment, item, SlotSet.CODPIECE_SLOTS);
  }

  private static int liveCopiesInGemSlots(AdventureResult item) {
    if (item == null || item == EquipmentRequest.UNEQUIP) return 0;
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

  private static boolean isCodpieceWorn(Map<Slot, AdventureResult> equipment) {
    return KoLCharacter.hasEquipped(equipment, EquipmentManager.ETERNITY_CODPIECE);
  }
}
