package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.WeaponType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase.FoldGroup;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

/**
 * The outer equipment search: one {@link AnytimeSearch.Problem} covering every equipment slot,
 * familiar switch, and outfit, replacing the old recursive {@code MaximizerSpeculation.tryAll}
 * traversal. Each slot group is a {@link Phase}; the Codpiece's gem search ({@link
 * CodpieceSpeculation.CodpieceSearch}) is folded in as the tail phase rather than run as a nested
 * search of its own.
 */
final class EquipmentSearchProblem
    implements AnytimeSearch.Problem<
        EquipmentSearchProblem.Choice, SolutionQuality, Void, MaximizerInterruptedException> {
  private static final List<Slot> SIMPLE_SLOTS = List.of(Slot.SHIRT, Slot.PANTS, Slot.HOLSTER);

  record Choice(BooleanSupplier apply, Runnable undo) {}

  private enum Phase {
    FAMILIAR_SWITCH,
    OUTFIT,
    FAMILIAR_ITEM,
    CONTAINER,
    ACCESSORY,
    HAT,
    SIMPLE_SLOT,
    WEAPON,
    OFFHAND,
    CODPIECE,
    DONE
  }

  private record Mark(
      EnumMap<Slot, AdventureResult> equipment,
      FamiliarData familiar,
      FamiliarData bjorned,
      FamiliarData enthroned,
      List<CheckedItem> familiarItems,
      int accessoryPos,
      int simpleSlotIndex,
      Phase phase) {}

  private final MaximizerSpeculation owner;
  private final List<FamiliarData> familiars;
  private final List<FamiliarData> carriedFamiliars;
  private final Map<Integer, Boolean> usefulOutfits;
  private final Map<AdventureResult, AdventureResult> outfitPieces;
  private final SlotList<CheckedItem> possibles;
  private final AdventureResult card;
  private final FamiliarData crownFamiliar;
  private final FamiliarData bjornFamiliar;
  private final boolean foldables;

  private final Deque<Mark> markStack = new ArrayDeque<>();
  private int accessoryPos;
  private int simpleSlotIndex;
  private Phase phase = Phase.FAMILIAR_SWITCH;

  private CodpieceSpeculation.Readiness codpieceReadiness;
  private CodpieceSpeculation.CodpieceSearch codpieceChooser;

  EquipmentSearchProblem(
      MaximizerSpeculation owner,
      List<FamiliarData> familiars,
      List<FamiliarData> carriedFamiliars,
      Map<Integer, Boolean> usefulOutfits,
      Map<AdventureResult, AdventureResult> outfitPieces,
      SlotList<CheckedItem> possibles,
      AdventureResult card,
      FamiliarData crownFamiliar,
      FamiliarData bjornFamiliar,
      boolean foldables) {
    this.owner = owner;
    this.familiars = familiars;
    this.carriedFamiliars = carriedFamiliars;
    this.usefulOutfits = usefulOutfits;
    this.outfitPieces = outfitPieces;
    this.possibles = possibles;
    this.card = card;
    this.crownFamiliar = crownFamiliar;
    this.bjornFamiliar = bjornFamiliar;
    this.foldables = foldables;
  }

  @Override
  public boolean complete() {
    return this.phase == Phase.DONE;
  }

  @Override
  public List<Choice> choices() {
    return switch (this.phase) {
      case FAMILIAR_SWITCH -> this.familiarSwitchChoices();
      case OUTFIT -> this.outfitChoices();
      case FAMILIAR_ITEM -> this.familiarItemChoices();
      case CONTAINER -> this.containerChoices();
      case ACCESSORY -> this.accessoryChoices();
      case HAT -> this.hatChoices();
      case SIMPLE_SLOT -> this.simpleSlotChoices();
      case WEAPON -> this.weaponChoices();
      case OFFHAND -> this.offhandChoices();
      case CODPIECE -> this.codpieceChoices();
      case DONE -> List.of();
    };
  }

  @Override
  public boolean choose(Choice choice) {
    return choice.apply().getAsBoolean();
  }

  @Override
  public void undo(Choice choice) {
    choice.undo().run();
  }

  @Override
  public boolean canBeat(SolutionQuality incumbent) {
    return this.codpieceChooser == null || this.codpieceChooser.canBeat(incumbent);
  }

  @Override
  public AnytimeSearch.Candidate<SolutionQuality, Void> candidate(SolutionQuality incumbent)
      throws MaximizerInterruptedException {
    if (this.codpieceReadiness == null) {
      return null;
    }
    if (this.codpieceReadiness == CodpieceSpeculation.Readiness.READY
        && !this.codpieceChooser.requirementsSatisfied()) {
      return null;
    }
    if (this.codpieceReadiness != CodpieceSpeculation.Readiness.READY
        && !this.owner.codpiece.hasEnoughCodpieceGems()) {
      return null;
    }
    if (this.codpieceChooser != null && !this.codpieceChooser.currentCanBeat(incumbent))
      return null;
    Maximizer.consider(this.owner);
    return new AnytimeSearch.Candidate<>(this.owner.quality(), null);
  }

  @Override
  public void finished(AnytimeSearch.Result<SolutionQuality, Void> result) {
    Maximizer.recordSearch(
        result.nodes(), result.dominancePrunes(), result.boundPrunes(), result.optimal());
  }

  private void pushMark() {
    this.markStack.push(
        new Mark(
            this.owner.mark(),
            this.owner.getFamiliar(),
            this.owner.getBjorned(),
            this.owner.getEnthroned(),
            this.possibles.get(Slot.FAMILIAR),
            this.accessoryPos,
            this.simpleSlotIndex,
            this.phase));
  }

  private void popMark() {
    Mark mark = this.markStack.pop();
    this.owner.codpiece.forget();
    this.codpieceReadiness = null;
    this.codpieceChooser = null;
    this.owner.restore(mark.equipment());
    this.owner.setFamiliar(mark.familiar());
    this.owner.setBjorned(mark.bjorned());
    this.owner.setEnthroned(mark.enthroned());
    this.possibles.set(Slot.FAMILIAR, mark.familiarItems());
    this.accessoryPos = mark.accessoryPos();
    this.simpleSlotIndex = mark.simpleSlotIndex();
    this.phase = mark.phase();
  }

  private Choice choice(Runnable action) {
    return this.choice(
        () -> {
          action.run();
          return true;
        });
  }

  private Choice choice(BooleanSupplier action) {
    return new Choice(
        () -> {
          this.pushMark();
          return action.getAsBoolean();
        },
        this::popMark);
  }

  private int availableCount(AdventureResult item, Slot foldTarget, Slot... duplicateSlots) {
    int count = item.getCount();
    for (Slot slot : duplicateSlots) {
      if (item.equals(this.owner.equipment.get(slot))) --count;
    }
    FoldGroup group = ItemDatabase.getFoldGroup(item.getName());
    if (group == null || !this.foldables) return count;

    String groupName = group.names.getFirst();
    for (Slot slot : SlotSet.SLOTS) {
      if (slot == foldTarget || this.owner.equipment.get(slot) == null) continue;
      FoldGroup equippedGroup = ItemDatabase.getFoldGroup(this.owner.equipment.get(slot).getName());
      if (equippedGroup != null && groupName.equals(equippedGroup.names.getFirst())) --count;
    }
    return count;
  }

  private static int mutex(AdventureResult item) {
    Modifiers modifiers = ModifierDatabase.getItemModifiers(item.getItemId());
    return modifiers == null ? 0 : modifiers.getRawBitmap(BitmapModifier.MUTEX);
  }

  /** Keeps current accessory slots when possible and avoids transient mutex conflicts. */
  private void preserveAccessorySlots(Slot first, Slot second) {
    AdventureResult item1 = this.owner.equipment.get(first);
    if (item1 == null) item1 = EquipmentRequest.UNEQUIP;
    AdventureResult equipped1 = EquipmentManager.getEquipment(first);
    if (equipped1.equals(item1)) return;
    AdventureResult item2 = this.owner.equipment.get(second);
    if (item2 == null) item2 = EquipmentRequest.UNEQUIP;
    AdventureResult equipped2 = EquipmentManager.getEquipment(second);
    if (equipped2.equals(item2)) return;

    int itemMutex1 = mutex(item1);
    int equippedMutex1 = mutex(equipped1);
    if ((itemMutex1 & equippedMutex1) != 0) return;
    int itemMutex2 = mutex(item2);
    int equippedMutex2 = mutex(equipped2);
    if ((itemMutex2 & equippedMutex2) != 0) return;

    if (equipped1.equals(item2)
        || equipped2.equals(item1)
        || (itemMutex1 & equippedMutex2) != 0
        || (itemMutex2 & equippedMutex1) != 0) {
      this.owner.equipment.put(first, item2);
      this.owner.equipment.put(second, item1);
    }
  }

  private void advance() {
    this.phase =
        switch (this.phase) {
          case FAMILIAR_SWITCH -> Phase.OUTFIT;
          case OUTFIT ->
              this.owner.equipment.get(Slot.FAMILIAR) == null
                  ? Phase.FAMILIAR_ITEM
                  : this.owner.equipment.get(Slot.CONTAINER) == null
                      ? Phase.CONTAINER
                      : Phase.ACCESSORY;
          case FAMILIAR_ITEM ->
              this.owner.equipment.get(Slot.CONTAINER) == null ? Phase.CONTAINER : Phase.ACCESSORY;
          case CONTAINER -> Phase.ACCESSORY;
          case ACCESSORY ->
              this.owner.equipment.get(Slot.HAT) == null ? Phase.HAT : Phase.SIMPLE_SLOT;
          case HAT -> Phase.SIMPLE_SLOT;
          case SIMPLE_SLOT ->
              ++this.simpleSlotIndex == SIMPLE_SLOTS.size() ? Phase.WEAPON : this.phase;
          case WEAPON -> Phase.OFFHAND;
          case OFFHAND ->
              this.codpieceReadiness == CodpieceSpeculation.Readiness.READY
                      && !this.codpieceChooser.complete()
                  ? Phase.CODPIECE
                  : Phase.DONE;
          case CODPIECE, DONE -> Phase.DONE;
        };
  }

  private List<Choice> familiarSwitchChoices() {
    List<Choice> choices = new ArrayList<>();
    choices.add(this.choice(this::advance));
    for (int i = 0; i < this.familiars.size(); i++) {
      FamiliarData familiar = this.familiars.get(i);
      List<CheckedItem> familiarItems = this.possibles.getFamiliar(i);
      choices.add(
          this.choice(
              () -> {
                this.owner.setFamiliar(familiar);
                this.possibles.set(Slot.FAMILIAR, familiarItems);
                this.advance();
              }));
    }
    return choices;
  }

  private List<Choice> outfitChoices() {
    List<Choice> choices = new ArrayList<>();
    for (var entry : this.usefulOutfits.entrySet()) {
      if (entry.getValue()) {
        int outfitId = entry.getKey();
        choices.add(
            this.choice(
                () -> {
                  boolean applied = this.applyOutfit(outfitId);
                  this.advance();
                  return applied;
                }));
      }
    }
    choices.add(this.choice(this::advance));
    return choices;
  }

  private boolean applyOutfit(int outfitId) {
    AdventureResult[] pieces = EquipmentDatabase.getOutfit(outfitId).getPieces();
    for (int idx = pieces.length - 1; idx >= 0; idx--) {
      AdventureResult item = this.outfitPieces.get(pieces[idx]);
      if (item == null) {
        return false; // not available
      }
      int count = item.getCount();
      Slot slot = EquipmentManager.itemIdToEquipmentType(item.getItemId());

      switch (slot) {
        case HAT, PANTS, SHIRT, CONTAINER -> {
          if (item.equals(this.owner.equipment.get(slot))) {
            continue; // already worn
          }
          if (item.equals(this.owner.equipment.get(Slot.FAMILIAR))) {
            --count;
          }
        }
        case WEAPON, OFFHAND -> {
          if (item.equals(this.owner.equipment.get(Slot.WEAPON))
              || item.equals(this.owner.equipment.get(Slot.OFFHAND))) {
            continue; // already worn
          }
          if (item.equals(this.owner.equipment.get(Slot.FAMILIAR))) {
            --count;
          }
        }
        case ACCESSORY1 -> {
          if (item.equals(this.owner.equipment.get(Slot.ACCESSORY1))
              || item.equals(this.owner.equipment.get(Slot.ACCESSORY2))
              || item.equals(this.owner.equipment.get(Slot.ACCESSORY3))) {
            continue; // already worn
          }
          if (item.equals(this.owner.equipment.get(Slot.FAMILIAR))) {
            --count;
          }
          if (this.owner.equipment.get(Slot.ACCESSORY3) == null) {
            slot = Slot.ACCESSORY3;
          } else if (this.owner.equipment.get(Slot.ACCESSORY2) == null) {
            slot = Slot.ACCESSORY2;
          }
        }
        default -> {
          return false; // don't know how to wear that
        }
      }

      if (count <= 0) {
        return false; // none available
      }
      if (this.owner.equipment.get(slot) != null) {
        return false; // slot taken
      }
      this.owner.equipment.put(slot, item);
    }
    return true;
  }

  private List<Choice> familiarItemChoices() {
    List<Choice> choices = new ArrayList<>();
    for (CheckedItem item : this.possibles.get(Slot.FAMILIAR)) {
      int count =
          this.availableCount(item, Slot.FAMILIAR, Slot.OFFHAND, Slot.WEAPON, Slot.HAT, Slot.PANTS);
      if (count <= 0) continue;
      choices.add(this.familiarItemChoice(item));
    }
    if (choices.isEmpty()) {
      choices.add(this.familiarItemChoice(EquipmentRequest.UNEQUIP));
    }
    return choices;
  }

  private Choice familiarItemChoice(AdventureResult item) {
    return this.choice(
        () -> {
          this.owner.equipment.put(Slot.FAMILIAR, item);
          this.advance();
        });
  }

  private List<Choice> containerChoices() {
    List<Choice> choices = new ArrayList<>();
    for (CheckedItem item : this.possibles.get(Slot.CONTAINER)) {
      int count = this.availableCount(item, Slot.CONTAINER);
      if (count <= 0) continue;
      if (item.equals(EquipmentManager.BUDDY_BJORN)) {
        List<FamiliarData> candidates =
            this.bjornFamiliar == null ? this.carriedFamiliars : List.of(this.bjornFamiliar);
        for (FamiliarData familiar : candidates) {
          choices.add(
              this.choice(
                  () -> {
                    this.owner.equipment.put(Slot.CONTAINER, item);
                    this.owner.setBjorned(familiar);
                    this.advance();
                  }));
        }
      } else {
        choices.add(this.containerChoice(item));
      }
    }
    if (choices.isEmpty()) {
      choices.add(this.containerChoice(EquipmentRequest.UNEQUIP));
    }
    return choices;
  }

  private Choice containerChoice(AdventureResult item) {
    return this.choice(
        () -> {
          this.owner.equipment.put(Slot.CONTAINER, item);
          this.advance();
        });
  }

  private int freeAccessorySlots() {
    int free = 0;
    if (this.owner.equipment.get(Slot.ACCESSORY1) == null) ++free;
    if (this.owner.equipment.get(Slot.ACCESSORY2) == null) ++free;
    if (this.owner.equipment.get(Slot.ACCESSORY3) == null) ++free;
    return free;
  }

  private List<Choice> accessoryChoices() {
    int free = this.freeAccessorySlots();
    if (free == 0) {
      return List.of(this.choice(this::finishAccessories));
    }

    List<CheckedItem> possible = this.possibles.get(Slot.ACCESSORY1);
    List<Choice> choices = new ArrayList<>();
    for (int pos = this.accessoryPos; pos < possible.size(); pos++) {
      CheckedItem item = possible.get(pos);
      int count =
          this.availableCount(item, Slot.NONE, Slot.ACCESSORY1, Slot.ACCESSORY2, Slot.ACCESSORY3);
      if (count <= 0) continue;
      for (int k = 1; k <= Math.min(free, count); k++) {
        choices.add(this.accessoryChoice(pos, k));
      }
    }
    if (choices.isEmpty()) {
      choices.add(this.choice(this::finishAccessories));
    }
    return choices;
  }

  private Choice accessoryChoice(int position, int count) {
    AdventureResult item = this.possibles.get(Slot.ACCESSORY1).get(position);
    return this.choice(
        () -> {
          int remaining = count;
          for (Slot slot : List.of(Slot.ACCESSORY1, Slot.ACCESSORY2, Slot.ACCESSORY3)) {
            if (remaining == 0) break;
            if (this.owner.equipment.get(slot) == null) {
              this.owner.equipment.put(slot, item);
              --remaining;
            }
          }
          this.accessoryPos = position + 1;
        });
  }

  private void finishAccessories() {
    for (Slot slot : List.of(Slot.ACCESSORY1, Slot.ACCESSORY2, Slot.ACCESSORY3)) {
      if (this.owner.equipment.get(slot) == null) {
        this.owner.equipment.put(slot, EquipmentRequest.UNEQUIP);
      }
    }
    this.preserveAccessorySlots(Slot.ACCESSORY1, Slot.ACCESSORY2);
    this.preserveAccessorySlots(Slot.ACCESSORY2, Slot.ACCESSORY3);
    this.preserveAccessorySlots(Slot.ACCESSORY3, Slot.ACCESSORY1);
    this.preserveAccessorySlots(Slot.ACCESSORY1, Slot.ACCESSORY2);
    this.advance();
  }

  private List<Choice> hatChoices() {
    List<Choice> choices = new ArrayList<>();
    for (CheckedItem item : this.possibles.get(Slot.HAT)) {
      int count = this.availableCount(item, Slot.HAT, Slot.FAMILIAR);
      if (count <= 0) continue;
      if (item.equals(EquipmentManager.CROWN_OF_THRONES)) {
        List<FamiliarData> candidates =
            this.crownFamiliar == null ? this.carriedFamiliars : List.of(this.crownFamiliar);
        for (FamiliarData familiar : candidates) {
          // Cannot use the same familiar for this and the Bjorn unless the slot is empty.
          if (this.crownFamiliar == null
              && familiar == this.owner.getBjorned()
              && familiar != FamiliarData.NO_FAMILIAR) {
            continue;
          }
          choices.add(
              this.choice(
                  () -> {
                    this.owner.equipment.put(Slot.HAT, item);
                    this.owner.setEnthroned(familiar);
                    this.advance();
                  }));
        }
      } else {
        choices.add(this.hatChoice(item));
      }
    }
    if (choices.isEmpty()) {
      choices.add(this.hatChoice(EquipmentRequest.UNEQUIP));
    }
    return choices;
  }

  private Choice hatChoice(AdventureResult item) {
    return this.choice(
        () -> {
          this.owner.equipment.put(Slot.HAT, item);
          this.advance();
        });
  }

  private List<Choice> simpleSlotChoices() {
    Slot slot = SIMPLE_SLOTS.get(this.simpleSlotIndex);
    AdventureResult current = this.owner.equipment.get(slot);
    if (current != null) {
      return List.of(this.simpleSlotChoice(slot, current));
    }
    if (slot == Slot.SHIRT && !KoLCharacter.isTorsoAware()) {
      return List.of(this.simpleSlotChoice(slot, EquipmentRequest.UNEQUIP));
    }

    List<Choice> choices = new ArrayList<>();
    for (CheckedItem item : this.possibles.get(slot)) {
      int count =
          slot == Slot.HOLSTER ? item.getCount() : this.availableCount(item, slot, Slot.FAMILIAR);
      if (count <= 0) continue;
      choices.add(this.simpleSlotChoice(slot, item));
    }
    if (choices.isEmpty()) {
      choices.add(this.simpleSlotChoice(slot, EquipmentRequest.UNEQUIP));
    }
    return choices;
  }

  private Choice simpleSlotChoice(Slot slot, AdventureResult item) {
    return this.choice(
        () -> {
          this.owner.equipment.put(slot, item);
          this.advance();
        });
  }

  private boolean chefstaffable() {
    return EquipmentManager.canEquipChefstaff(
        KoLCharacter.hasEquipped(
            this.owner.equipment, ItemPool.get(ItemPool.SPECIAL_SAUCE_GLOVE, 1)));
  }

  private List<Choice> weaponChoices() {
    AdventureResult weapon = this.owner.equipment.get(Slot.WEAPON);
    if (weapon != null) {
      if (!this.chefstaffable() && EquipmentDatabase.isChefStaff(weapon)) {
        return List.of(); // illegal preset chefstaff: dead end
      }
      return List.of(this.weaponChoice(weapon));
    }

    List<Choice> choices = new ArrayList<>();
    for (AdventureResult item : this.possibles.get(Slot.WEAPON)) {
      if (!this.chefstaffable() && EquipmentDatabase.isChefStaff(item)) continue;
      int count = this.availableCount(item, Slot.WEAPON, Slot.OFFHAND, Slot.FAMILIAR);
      if (count <= 0) continue;
      choices.add(this.weaponChoice(item));
    }
    if (!Maximizer.evaluator().forbidsUnarmed()) {
      choices.add(this.weaponChoice(EquipmentRequest.UNEQUIP));
    }
    return choices;
  }

  private Choice weaponChoice(AdventureResult item) {
    return this.choice(
        () -> {
          this.owner.equipment.put(Slot.WEAPON, item);
          this.advance();
        });
  }

  private List<Choice> offhandChoices() {
    int weapon = this.owner.equipment.get(Slot.WEAPON).getItemId();
    if (EquipmentDatabase.getHands(weapon) > 1) {
      return List.of(this.offhandChoice(EquipmentRequest.UNEQUIP, null));
    }
    AdventureResult offhand = this.owner.equipment.get(Slot.OFFHAND);
    if (offhand != null) {
      return List.of(this.offhandChoice(offhand, null));
    }

    WeaponType weaponType = WeaponType.NONE;
    if (KoLCharacter.hasSkill(SkillPool.DOUBLE_FISTED_SKULL_SMASHING)) {
      weaponType = EquipmentDatabase.getWeaponType(weapon);
    }
    List<CheckedItem> possible =
        switch (weaponType) {
          case MELEE -> this.possibles.get(Evaluator.OFFHAND_MELEE);
          case RANGED -> this.possibles.get(Evaluator.OFFHAND_RANGED);
          default -> this.possibles.get(Slot.OFFHAND);
        };

    List<Choice> choices = new ArrayList<>();
    for (CheckedItem item : possible) {
      int count = this.availableCount(item, Slot.OFFHAND, Slot.WEAPON, Slot.FAMILIAR);
      if (count <= 0) continue;
      choices.add(
          this.offhandChoice(item, item.equals(EquipmentManager.CARD_SLEEVE) ? this.card : null));
    }
    if (choices.isEmpty() || weapon <= 0) {
      choices.add(this.offhandChoice(EquipmentRequest.UNEQUIP, null));
    }
    return choices;
  }

  private Choice offhandChoice(AdventureResult item, AdventureResult card) {
    return this.choice(
        () -> {
          if (card != null) {
            this.owner.equipment.put(Slot.CARDSLEEVE, card);
          }
          this.owner.equipment.put(Slot.OFFHAND, item);
          var prepared = this.owner.codpiece.prepare(this.possibles.get(Slot.CODPIECE1));
          this.codpieceReadiness = prepared.readiness();
          this.codpieceChooser = prepared.chooser();
          this.advance();
        });
  }

  private List<Choice> codpieceChoices() {
    var search = this.codpieceChooser;
    return search.choices().stream()
        .map(
            index ->
                new Choice(
                    () -> {
                      boolean chosen = search.choose(index);
                      if (search.complete()) this.phase = Phase.DONE;
                      return chosen;
                    },
                    () -> {
                      this.phase = Phase.CODPIECE;
                      search.undo(index);
                    }))
        .toList();
  }
}
