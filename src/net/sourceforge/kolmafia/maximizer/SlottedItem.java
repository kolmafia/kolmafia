package net.sourceforge.kolmafia.maximizer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;

interface SlottedItem<T> {
  List<Slot> slots();

  boolean accepts(Slot slot, T occupant);

  T get(MaximizerLoadout state, Slot slot);

  boolean put(MaximizerLoadout state, Slot slot, T occupant);

  Modifiers modifiers(T occupant);
}

enum ItemSlotGroup implements SlottedItem<AdventureResult> {
  STICKERS(
      List.copyOf(SlotSet.STICKER_SLOTS),
      ModifierType.ITEM,
      false,
      ItemPool.STICKER_SWORD,
      ItemPool.STICKER_CROSSBOW),
  CARD_SLEEVE(List.of(Slot.CARDSLEEVE), ModifierType.ITEM, true, ItemPool.CARD_SLEEVE),
  FOLDERS(
      List.copyOf(SlotSet.FOLDER_SLOTS),
      ModifierType.ITEM,
      false,
      ItemPool.FOLDER_HOLDER,
      ItemPool.REPLICA_FOLDER_HOLDER),
  BOOTS(List.of(Slot.BOOTSKIN, Slot.BOOTSPUR), ModifierType.ITEM, false, ItemPool.COWBOY_BOOTS),
  ETERNITY_CODPIECE(
      List.copyOf(SlotSet.CODPIECE_SLOTS),
      ModifierType.ETERNITY_CODPIECE,
      true,
      ItemPool.THE_ETERNITY_CODPIECE);

  private final List<Slot> slots;
  private final ModifierType modifierType;
  private final boolean searchable;
  private final List<Integer> parentIds;

  ItemSlotGroup(List<Slot> slots, ModifierType modifierType, boolean searchable, int... parentIds) {
    this.slots = slots;
    this.modifierType = modifierType;
    this.searchable = searchable;
    this.parentIds = Arrays.stream(parentIds).boxed().toList();
  }

  public boolean isParent(int itemId) {
    return this.parentIds.contains(itemId);
  }

  @Override
  public List<Slot> slots() {
    return this.slots;
  }

  @Override
  public boolean accepts(Slot slot, AdventureResult occupant) {
    if (!this.slots.contains(slot)) return false;
    int itemId = occupant == null ? -1 : occupant.getItemId();
    return itemId <= 0 || this.accepts(slot, itemId);
  }

  private boolean accepts(Slot slot, int itemId) {
    return this == ETERNITY_CODPIECE
        ? EquipmentDatabase.isCodpieceGem(itemId)
        : ItemDatabase.getConsumptionType(itemId) == this.slotType(slot);
  }

  boolean accepts(int itemId) {
    return itemId > 0 && this.slots.stream().anyMatch(slot -> this.accepts(slot, itemId));
  }

  boolean searchable() {
    return this.searchable;
  }

  int parentItemId() {
    return this.parentIds.getFirst();
  }

  @Override
  public AdventureResult get(MaximizerLoadout state, Slot slot) {
    return this.slots.contains(slot) ? state.equipment.get(slot) : null;
  }

  @Override
  public boolean put(MaximizerLoadout state, Slot slot, AdventureResult occupant) {
    if (!this.accepts(slot, occupant)) return false;
    state.equipment.put(slot, occupant);
    return true;
  }

  @Override
  public Modifiers modifiers(AdventureResult occupant) {
    return occupant == null ? null : this.modifiers(occupant.getItemId());
  }

  Modifiers modifiers(int itemId) {
    return itemId <= 0 ? null : ModifierDatabase.getModifiers(this.modifierType, itemId);
  }

  static ItemSlotGroup find(int parentItemId) {
    return PARENTS.get(parentItemId);
  }

  private ConsumptionType slotType(Slot slot) {
    return switch (this) {
      case STICKERS -> ConsumptionType.STICKER;
      case CARD_SLEEVE -> ConsumptionType.CARD;
      case FOLDERS -> ConsumptionType.FOLDER;
      case BOOTS ->
          switch (slot) {
            case BOOTSKIN -> ConsumptionType.BOOTSKIN;
            case BOOTSPUR -> ConsumptionType.BOOTSPUR;
            default -> ConsumptionType.UNKNOWN;
          };
      case ETERNITY_CODPIECE -> ConsumptionType.UNKNOWN;
    };
  }

  private static final Map<Integer, ItemSlotGroup> PARENTS =
      Map.of(
          ItemPool.STICKER_SWORD, STICKERS,
          ItemPool.STICKER_CROSSBOW, STICKERS,
          ItemPool.CARD_SLEEVE, CARD_SLEEVE,
          ItemPool.FOLDER_HOLDER, FOLDERS,
          ItemPool.REPLICA_FOLDER_HOLDER, FOLDERS,
          ItemPool.COWBOY_BOOTS, BOOTS,
          ItemPool.THE_ETERNITY_CODPIECE, ETERNITY_CODPIECE);
}

enum FamiliarSlotGroup implements SlottedItem<FamiliarData> {
  CROWN(ItemPool.HATSEAT, Slot.CROWNOFTHRONES),
  BJORN(ItemPool.BUDDY_BJORN, Slot.BUDDYBJORN);

  private final int parentItemId;
  private final List<Slot> slots;

  FamiliarSlotGroup(int parentItemId, Slot slot) {
    this.parentItemId = parentItemId;
    this.slots = List.of(slot);
  }

  public boolean isParent(int itemId) {
    return this.parentItemId == itemId;
  }

  @Override
  public List<Slot> slots() {
    return this.slots;
  }

  @Override
  public FamiliarData get(MaximizerLoadout state, Slot slot) {
    if (!this.slots.contains(slot)) return null;
    return switch (this) {
      case CROWN -> state.getEnthroned();
      case BJORN -> state.getBjorned();
    };
  }

  @Override
  public boolean accepts(Slot slot, FamiliarData occupant) {
    return this.slots.contains(slot) && occupant != null;
  }

  @Override
  public boolean put(MaximizerLoadout state, Slot slot, FamiliarData occupant) {
    if (!this.accepts(slot, occupant)) return false;
    switch (this) {
      case CROWN -> state.setEnthroned(occupant);
      case BJORN -> state.setBjorned(occupant);
    }
    return true;
  }

  @Override
  public Modifiers modifiers(FamiliarData occupant) {
    return occupant == null
        ? null
        : ModifierDatabase.getModifiers(
            this == CROWN ? ModifierType.THRONE : ModifierType.BJORN, occupant.getRace());
  }

  FamiliarData current() {
    return switch (this) {
      case CROWN -> KoLCharacter.getEnthroned();
      case BJORN -> KoLCharacter.getBjorned();
    };
  }

  int parentItemId() {
    return this.parentItemId;
  }

  static FamiliarSlotGroup find(int parentItemId) {
    return PARENTS.get(parentItemId);
  }

  private static final Map<Integer, FamiliarSlotGroup> PARENTS =
      Map.of(ItemPool.HATSEAT, CROWN, ItemPool.BUDDY_BJORN, BJORN);
}
