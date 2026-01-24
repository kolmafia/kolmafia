package net.sourceforge.kolmafia.session;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.KoLConstants.WeaponType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.MultiStringModifier;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest.EquipmentRequestType;
import net.sourceforge.kolmafia.swingui.panel.GearChangePanel;
import net.sourceforge.kolmafia.textui.command.ConditionsCommand;
import net.sourceforge.kolmafia.utilities.LockableListFactory;
import net.sourceforge.kolmafia.utilities.SortedList;

@SuppressWarnings("incomplete-switch")
public class EquipmentManager {
  /** A list of all possible accessories. Is an interior list of equipmentLists for acc1, 2, 3 */
  private static final List<AdventureResult> accessories =
      LockableListFactory.getInstance(AdventureResult.class);

  // these three all use HAT through BOOTSPUR (i.e. all slots)
  /** A list indexed by Slot of current equipment */
  private static final Map<Slot, AdventureResult> equipment = new EnumMap<>(Slot.class);

  /** A list indexed by Slot of possible equipment. Interior list is used in GearChangeFrame */
  private static final Map<Slot, List<AdventureResult>> equipmentLists = new EnumMap<>(Slot.class);

  /** A list indexed by Slot of equipment we have previously equipped, in order */
  private static final Map<Slot, List<AdventureResult>> historyLists = new EnumMap<>(Slot.class);

  private static int fakeHandCount = 0;

  /** In the Hat Trick path, all equipped hats */
  private static List<Integer> hats = new ArrayList<>();

  private static final List<SpecialOutfit> normalOutfits =
      LockableListFactory.getInstance(SpecialOutfit.class);
  private static final List<SpecialOutfit> customOutfits =
      LockableListFactory.getInstance(SpecialOutfit.class);

  /** Map from Sticker[1|2|3] to turns remaining. */
  private static final EnumMap<Slot, Integer> turnsRemaining = new EnumMap<>(Slot.class);

  private static AdventureResult lockedFamiliarItem = EquipmentRequest.UNEQUIP;

  public static final AdventureResult FAKE_HAND = ItemPool.get(ItemPool.FAKE_HAND, 1);
  public static final AdventureResult CARD_SLEEVE = ItemPool.get(ItemPool.CARD_SLEEVE, 1);
  public static final AdventureResult CROWN_OF_THRONES = ItemPool.get(ItemPool.HATSEAT, 1);
  public static final AdventureResult BUDDY_BJORN = ItemPool.get(ItemPool.BUDDY_BJORN, 1);
  public static final AdventureResult FOLDER_HOLDER = ItemPool.get(ItemPool.FOLDER_HOLDER, 1);
  public static final AdventureResult REPLICA_FOLDER_HOLDER =
      ItemPool.get(ItemPool.REPLICA_FOLDER_HOLDER, 1);
  public static final AdventureResult COWBOY_BOOTS = ItemPool.get(ItemPool.COWBOY_BOOTS, 1);
  public static final AdventureResult ETERNITY_CODPIECE =
      ItemPool.get(ItemPool.THE_ETERNITY_CODPIECE, 1);

  static {
    for (var slot : SlotSet.ALL_SLOTS) {
      EquipmentManager.equipment.put(slot, EquipmentRequest.UNEQUIP);
      EquipmentManager.historyLists.put(slot, new ArrayList<>());

      switch (slot) {
        case ACCESSORY1, ACCESSORY2, ACCESSORY3 ->
            EquipmentManager.equipmentLists.put(
                slot, LockableListFactory.getMirror(EquipmentManager.accessories));
        default ->
            EquipmentManager.equipmentLists.put(
                slot, LockableListFactory.getSortedInstance(AdventureResult.class));
      }
    }
  }

  private EquipmentManager() {}

  public static void resetEquipment() {
    for (var list : EquipmentManager.equipmentLists.values()) {
      list.clear();
    }
    for (var list : EquipmentManager.historyLists.values()) {
      list.clear();
    }

    EquipmentManager.accessories.clear();
    GearChangePanel.clearEquipmentModels();

    EquipmentManager.equipment.clear();

    for (var slot : SlotSet.ALL_SLOTS) {
      EquipmentManager.equipment.put(slot, EquipmentRequest.UNEQUIP);
    }

    EquipmentManager.fakeHandCount = 0;
    EquipmentManager.hats.clear();
    EquipmentManager.lockedFamiliarItem = EquipmentRequest.UNEQUIP;
    EquipmentManager.normalOutfits.clear();
  }

  public static void resetCustomOutfits() {
    EquipmentManager.customOutfits.clear();
  }

  public static EnumMap<Slot, AdventureResult> emptyEquipmentArray() {
    return EquipmentManager.emptyEquipmentArray(false);
  }

  public static EnumMap<Slot, AdventureResult> emptyEquipmentArray(boolean all) {
    EnumSet<Slot> slots = all ? SlotSet.ALL_SLOTS : SlotSet.SLOTS;
    EnumMap<Slot, AdventureResult> map = new EnumMap<>(Slot.class);

    for (var slot : slots) {
      map.put(slot, EquipmentRequest.UNEQUIP);
    }

    return map;
  }

  public static EnumMap<Slot, AdventureResult> currentEquipment() {
    EnumMap<Slot, AdventureResult> map = new EnumMap<>(Slot.class);

    for (var slot : SlotSet.SLOTS) {
      map.put(slot, EquipmentManager.getEquipment(slot));
    }
    return map;
  }

  public static EnumMap<Slot, AdventureResult> allEquipment() {
    EnumMap<Slot, AdventureResult> map = new EnumMap<>(EquipmentManager.equipment);

    map.put(Slot.FAMILIAR, EquipmentManager.getFamiliarItem());
    return map;
  }

  public static final Collection<AdventureResult> allEquipmentAsCollection() {
    return EquipmentManager.equipment.values();
  }

  public static final void processResult(AdventureResult item) {
    int itemId = item.getItemId();

    // If your current familiar can use this item, add it to familiar item list
    if (KoLCharacter.getFamiliar().canEquip(item)) {
      AdventureResult.addResultToList(EquipmentManager.equipmentLists.get(Slot.FAMILIAR), item);
      if (ItemDatabase.getConsumptionType(itemId) == ConsumptionType.FAMILIAR_EQUIPMENT) {
        return;
      }
      // Even though the familiar can use it, it's not a
      // familiar item. Continue processing, in case the
      // character can also use the item
    }

    if (!EquipmentManager.canEquip(itemId)) {
      return;
    }

    ConsumptionType consumeType = ItemDatabase.getConsumptionType(itemId);
    if (consumeType == ConsumptionType.ACCESSORY) {
      AdventureResult.addResultToList(EquipmentManager.accessories, item);
    } else if (consumeType == ConsumptionType.STICKER) {
      // The stickers cannot be combined into a single list, as is done with
      // accessories, since stickers cannot be moved to a different slot.  If a
      // slot contains your last sticker of a particular type, then that type must
      // only appear for that slot (so that it can be the initially selected
      // value), not in the other two slots. There are only six types of stickers,
      // and no reason to believe that there will ever be many (or even any) more,
      // so this duplication should not present a problem.
      //
      // Make sure the current sticker in each slot remains in the list, even if
      // there are no more of that type in inventory.

      for (Slot slot : SlotSet.STICKER_SLOTS) {
        AdventureResult current = EquipmentManager.getEquipment(slot);
        AdventureResult.addResultToList(EquipmentManager.equipmentLists.get(slot), item);
        if (!EquipmentManager.equipmentLists.get(slot).contains(current)) {
          EquipmentManager.equipmentLists.get(slot).add(current);
        }
      }
    } else if (consumeType == ConsumptionType.FOLDER) {
      // Folders are similar to stickers

      for (Slot slot : SlotSet.FOLDER_SLOTS) {
        AdventureResult current = EquipmentManager.getEquipment(slot);
        AdventureResult.addResultToList(EquipmentManager.equipmentLists.get(slot), item);
        if (!EquipmentManager.equipmentLists.get(slot).contains(current)) {
          EquipmentManager.equipmentLists.get(slot).add(current);
        }
      }
    }

    // Codpiece gems can also have other consumption types (e.g. accessories),
    // so check for ETERNITY_CODPIECE modifiers separately
    if (EquipmentRequest.isCodpieceGem(itemId)) {
      for (Slot slot : SlotSet.CODPIECE_SLOTS) {
        AdventureResult current = EquipmentManager.getEquipment(slot);
        AdventureResult.addResultToList(EquipmentManager.equipmentLists.get(slot), item);
        if (!EquipmentManager.equipmentLists.get(slot).contains(current)) {
          EquipmentManager.equipmentLists.get(slot).add(current);
        }
      }
    }

    if (itemId == ItemPool.HATSEAT) {
      AdventureResult.addResultToList(EquipmentManager.equipmentLists.get(Slot.HAT), item);
    } else if (itemId == ItemPool.BUDDY_BJORN) {
      AdventureResult.addResultToList(EquipmentManager.equipmentLists.get(Slot.CONTAINER), item);
    } else {
      Slot equipmentType = EquipmentManager.consumeFilterToEquipmentType(consumeType);
      if (equipmentType != Slot.NONE) {
        AdventureResult.addResultToList(EquipmentManager.equipmentLists.get(equipmentType), item);
        GearChangePanel.updateSlot(equipmentType);
      }
    }

    if (EquipmentDatabase.getOutfitWithItem(item.getItemId()) != -1) {
      EquipmentManager.updateNormalOutfits();
    }
  }

  public static final void autoequipItem(AdventureResult newItem) {
    EquipmentManager.autoequipItem(newItem, true);
  }

  public static final void autoequipItem(AdventureResult newItem, boolean addToCheckpoints) {
    Slot slot = EquipmentManager.itemIdToEquipmentType(newItem.getItemId());
    if (slot == Slot.NONE) {
      return;
    }

    if (addToCheckpoints) {
      SpecialOutfit.replaceEquipmentInSlot(newItem, slot);
    }

    AdventureResult oldItem = EquipmentManager.getEquipment(slot);

    if (newItem.getItemId() == oldItem.getItemId()) {
      return;
    }

    // Manually subtract item from inventory to avoid
    // excessive list updating.

    if (newItem != EquipmentRequest.UNEQUIP) {
      AdventureResult.addResultToList(KoLConstants.inventory, newItem.getInstance(-1));
    }

    if (oldItem != EquipmentRequest.UNEQUIP) {
      AdventureResult.addResultToList(KoLConstants.inventory, oldItem.getInstance(1));
    }

    EquipmentManager.setEquipment(slot, newItem);
  }

  public static final void setEquipment(final Slot slot, AdventureResult item) {
    // Variable slots do not include the fake hand
    if (slot == Slot.FAKEHAND) {
      return;
    } else if (slot == Slot.HATS) {
      // we want to add skills and nothing else
      EquipmentManager.addHatTrickHat(item.getItemId());
      EquipmentManager.addConditionalSkills(Slot.HAT, item);
      return;
    }

    AdventureResult old = EquipmentManager.getEquipment(slot);

    // Accessories are special in terms of testing for existence
    // in equipment lists -- they are all mirrors of accessories.

    switch (slot) {
      case NONE: // unknown item - ignore it
        return;

      case ACCESSORY1:
      case ACCESSORY2:
      case ACCESSORY3:
        int index = EquipmentManager.accessories.indexOf(item);
        if (index == -1) {
          EquipmentManager.accessories.add(item);
        } else {
          item = EquipmentManager.accessories.get(index);
        }
        break;

      default:
        if (!EquipmentManager.equipmentLists.get(slot).contains(item)) {
          EquipmentManager.equipmentLists.get(slot).add(item);
        }
        break;
    }

    EquipmentManager.equipment.put(slot, item);
    LockableListFactory.setSelectedItem(EquipmentManager.equipmentLists.get(slot), item);
    EquipmentManager.historyLists.get(slot).remove(item);
    EquipmentManager.historyLists.get(slot).add(item);

    // Certain equipment slots require special update handling
    // in addition to the above code.
    if (slot == Slot.FAMILIAR && KoLCharacter.currentFamiliar != null) {
      KoLCharacter.currentFamiliar.setItem(item);
    }
    EquipmentManager.checkFamiliar(slot);
    GearChangePanel.updateSlot(slot);

    // Certain items provide additional skills when equipped.
    // Handle the addition / removal of those skills here.

    // Remove skill first if item being removed had one
    if (old.getItemId() != item.getItemId()) {
      ConsumptionType consumption = ItemDatabase.getConsumptionType(old);
      boolean removed = true;
      // Some items could be in multiple slots
      switch (slot) {
        case HAT -> {
          // Mad Hatrack wears hats and grants conditional skills
          AdventureResult hat = EquipmentManager.getEquipment(Slot.HAT);
          AdventureResult familiar = EquipmentManager.getEquipment(Slot.FAMILIAR);
          removed = hat.getItemId() != old.getItemId() && familiar.getItemId() != old.getItemId();
        }
        case WEAPON -> {
          // Disembodied Hand wields weapons and grants conditional skills
          AdventureResult offhand = EquipmentManager.getEquipment(Slot.OFFHAND);
          AdventureResult familiar = EquipmentManager.getEquipment(Slot.FAMILIAR);
          removed =
              offhand.getItemId() != old.getItemId() && familiar.getItemId() != old.getItemId();
        }
        case OFFHAND -> {
          // Left-Hand Man Hand wields offhand items and grants conditional skills
          AdventureResult weapon = EquipmentManager.getEquipment(Slot.WEAPON);
          AdventureResult offhand = EquipmentManager.getEquipment(Slot.OFFHAND);
          AdventureResult familiar = EquipmentManager.getEquipment(Slot.FAMILIAR);
          removed =
              weapon.getItemId() != old.getItemId()
                  && offhand.getItemId() != old.getItemId()
                  && familiar.getItemId() != old.getItemId();
        }
        case PANTS -> {
          // Fancypants Scarecrow wears pants and grants conditional skills
          AdventureResult pants = EquipmentManager.getEquipment(Slot.PANTS);
          AdventureResult familiar = EquipmentManager.getEquipment(Slot.FAMILIAR);
          removed = pants.getItemId() != old.getItemId() && familiar.getItemId() != old.getItemId();
        }
        case FAMILIAR -> {
          // Mad Hatrack wears hats and grants conditional skills
          // Disembodied Hand wields weapons and grants conditional skills
          // Fancypants Scarecrow wears pants and grants conditional skills
          AdventureResult hat = EquipmentManager.getEquipment(Slot.HAT);
          AdventureResult weapon = EquipmentManager.getEquipment(Slot.WEAPON);
          AdventureResult offhand = EquipmentManager.getEquipment(Slot.OFFHAND);
          AdventureResult pants = EquipmentManager.getEquipment(Slot.PANTS);
          removed =
              switch (consumption) {
                case HAT -> hat.getItemId() != old.getItemId();
                case WEAPON, OFFHAND ->
                    weapon.getItemId() != old.getItemId() && offhand.getItemId() != old.getItemId();
                case PANTS -> pants.getItemId() != old.getItemId();
                default -> removed;
              };
        }
        case ACCESSORY1 -> {
          AdventureResult acc2 = EquipmentManager.getEquipment(Slot.ACCESSORY2);
          AdventureResult acc3 = EquipmentManager.getEquipment(Slot.ACCESSORY3);
          removed = acc2.getItemId() != old.getItemId() && acc3.getItemId() != old.getItemId();
        }
        case ACCESSORY2 -> {
          AdventureResult acc1 = EquipmentManager.getEquipment(Slot.ACCESSORY1);
          AdventureResult acc3 = EquipmentManager.getEquipment(Slot.ACCESSORY3);
          removed = acc1.getItemId() != old.getItemId() && acc3.getItemId() != old.getItemId();
        }
        case ACCESSORY3 -> {
          AdventureResult acc1 = EquipmentManager.getEquipment(Slot.ACCESSORY1);
          AdventureResult acc2 = EquipmentManager.getEquipment(Slot.ACCESSORY2);
          removed = acc1.getItemId() != old.getItemId() && acc2.getItemId() != old.getItemId();
        }
      }

      // If removed, remove conditional skill
      if (removed) {
        EquipmentManager.removeConditionalSkills(slot, old);
      }
    }

    // Add skill if appropriate
    EquipmentManager.addConditionalSkills(slot, item);

    // If Tuxedo Shirt put on or off, and autoTuxedo not set, several booze adventure gains change
    if (!Preferences.getBoolean("autoTuxedo")
        && (old.getItemId() == ItemPool.TUXEDO_SHIRT
            || item.getItemId() == ItemPool.TUXEDO_SHIRT)) {
      ConcoctionDatabase.setRefreshNeeded(true);
    }

    // If Mafia Pinky Ring put on or off, and autoPinkyRing not set, several booze adventure gains
    // change
    if (!Preferences.getBoolean("autoPinkyRing")
        && (old.getItemId() == ItemPool.MAFIA_PINKY_RING
            || item.getItemId() == ItemPool.MAFIA_PINKY_RING)) {
      ConcoctionDatabase.setRefreshNeeded(true);
    }
  }

  public static void removeConditionalSkills(final Slot slot, AdventureResult item) {
    manageConditionalSkills(false, slot, item);
  }

  public static void addConditionalSkills(final Slot slot, AdventureResult item) {
    manageConditionalSkills(true, slot, item);
  }

  private static void manageConditionalSkillsFromOutfit(
      final boolean add, final int id, final Consumer<Integer> cb) {
    var outfit = normalOutfits.stream().filter(o -> o.containsPiece(id)).findAny().orElse(null);

    if (outfit == null) return;
    var outfitMods = ModifierDatabase.getModifiers(ModifierType.OUTFIT, outfit.getName());
    if (outfitMods == null) return;

    if (add && !outfit.isWearing()) return;

    outfitMods.getStrings(MultiStringModifier.CONDITIONAL_SKILL_EQUIPPED).stream()
        .map(SkillDatabase::getSkillId)
        .forEach(cb);
  }

  private static void manageConditionalSkills(
      final boolean add, final Slot slot, AdventureResult item) {
    Consumer<Integer> cb =
        add ? KoLCharacter::addAvailableSkill : KoLCharacter::removeAvailableSkill;
    // Certain items can be equipped either in their normal slot or
    // on a familiar. Granted skills may or may not be available.
    //
    // hat - Mad Hatrack - willowy bonnet - YES
    // weapon - Disembodied Hand - bottle rocket crossbow - YES
    // offhand - Left-Hand Man - latte lovers member's mug - YES
    // pants - Fancypants Scarecrow - crotchety pants - YES

    int id = item.getItemId();

    // If we are equipping a new sword or gun we may be changing the capabilities of the retrocape
    if (slot == Slot.WEAPON) {
      if (EquipmentDatabase.isSword(id)
          || EquipmentDatabase.isGun(id)
          || EquipmentDatabase.isPistol(id)
          || EquipmentDatabase.isRifle(id)) {
        ItemDatabase.setCapeSkills();
      }
    }

    var mods = ModifierDatabase.getItemModifiers(id);
    if (mods != null) {
      mods.getStrings(MultiStringModifier.CONDITIONAL_SKILL_EQUIPPED).stream()
          .map(SkillDatabase::getSkillId)
          .filter(Predicate.not(SkillDatabase::isNonCombat))
          .forEach(cb);
    }

    manageConditionalSkillsFromOutfit(add, id, cb);

    switch (id) {
      case ItemPool.RIGHT_BEAR_ARM, ItemPool.LEFT_BEAR_ARM -> {
        if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.RIGHT_BEAR_ARM, 1))
            && KoLCharacter.hasEquipped(ItemPool.get(ItemPool.LEFT_BEAR_ARM, 1))) {
          cb.accept(SkillPool.BEAR_HUG);
          cb.accept(SkillPool.I_CAN_BEARLY_HEAR_YOU_OVER_THE_APPLAUSE);
        }
      }
      case ItemPool.WARBEAR_OIL_PAN -> {
        if (KoLCharacter.isSauceror()) {
          cb.accept(SkillPool.SPRAY_HOT_GREASE);
        }
      }
      case ItemPool.HEWN_MOON_RUNE_SPOON, ItemPool.REPLICA_HEWN_MOON_RUNE_SPOON -> {
        if (KoLCharacter.isMuscleClass()) {
          cb.accept(SkillPool.DRAGOON_PLATOON);
        } else if (KoLCharacter.isMysticalityClass()) {
          cb.accept(SkillPool.SPITTOON_MONSOON);
        } else if (KoLCharacter.isMoxieClass()) {
          cb.accept(SkillPool.FESTOON_BUFFOON);
        }
      }
      case ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE -> ItemDatabase.setCapeSkills();
      case ItemPool.SHERIFF_BADGE, ItemPool.SHERIFF_PISTOL, ItemPool.SHERIFF_MOUSTACHE -> {
        if (KoLCharacter.hasEquipped(ItemPool.SHERIFF_PISTOL)
            && KoLCharacter.hasEquipped(ItemPool.SHERIFF_BADGE)
            && KoLCharacter.hasEquipped(ItemPool.SHERIFF_MOUSTACHE)) {
          cb.accept(SkillPool.ASSERT_YOUR_AUTHORITY);
        }
      }
    }
  }

  public static final void transformEquipment(AdventureResult before, AdventureResult after) {
    SpecialOutfit.replaceEquipment(before, after);
    for (var slot : SlotSet.SLOTS) {
      if (KoLCharacter.hasEquipped(before, slot)) {
        EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);
        // FamiliarData.setItem moved the current
        // familiar item to inventory when we
        // unequipped it above
        if (slot != Slot.FAMILIAR) {
          AdventureResult.addResultToList(KoLConstants.inventory, before);
        }
        ResultProcessor.processResult(before.getInstance(-1));
        EquipmentManager.setEquipment(slot, after);
        return;
      }
    }
    RequestLogger.printLine("(unable to determine slot of transformed equipment)");
  }

  public static final Slot removeEquipment(final int itemId) {
    return EquipmentManager.removeEquipment(ItemPool.get(itemId, 1));
  }

  public static final Slot removeEquipment(final AdventureResult item) {
    for (var slot : SlotSet.SLOTS) {
      if (EquipmentManager.removeEquipment(item, slot)) {
        return slot;
      }
    }
    return Slot.NONE;
  }

  public static final boolean removeEquipment(final AdventureResult item, final Slot slot) {
    if (!KoLCharacter.hasEquipped(item, slot)) {
      return false;
    }

    EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);

    // FamiliarData.setItem moved the current familiar item to
    // inventory when we unequipped it above
    if (slot != Slot.FAMILIAR) {
      AdventureResult.addResultToList(KoLConstants.inventory, item);
    }

    return true;
  }

  public static final void removeAllEquipment() {
    for (var slot : SlotSet.SLOTS) {
      AdventureResult item = EquipmentManager.getEquipment(slot);
      if (!item.equals(EquipmentRequest.UNEQUIP)) {
        EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);
        // FamiliarData.setItem moved the current familiar item to
        // inventory when we unequipped it above
        if (slot != Slot.FAMILIAR) {
          AdventureResult.addResultToList(KoLConstants.inventory, item);
        }
      }
    }
  }

  public static final Slot discardEquipment(final int itemId) {
    return EquipmentManager.discardEquipment(itemId, true);
  }

  public static final Slot discardEquipment(final int itemId, boolean deleteFromCheckpoints) {
    return EquipmentManager.discardEquipment(ItemPool.get(itemId, 1), deleteFromCheckpoints);
  }

  public static final Slot discardEquipment(final AdventureResult item) {
    return EquipmentManager.discardEquipment(item, true);
  }

  public static final Slot discardEquipment(
      final AdventureResult item, boolean deleteFromCheckpoints) {
    if (deleteFromCheckpoints) {
      SpecialOutfit.forgetEquipment(item);
    }
    Slot slot = EquipmentManager.removeEquipment(item);
    if (slot != Slot.NONE) {
      ResultProcessor.processItem(item.getItemId(), -1);
    }
    return slot;
  }

  public static final void discardSpelunkyEquipment(final int itemId) {
    // We only discard Spelunky equipment when we throw it from the offhand slot.
    // If we kill the monster by doing that and find and autoequip an offhand item
    // - even the same one - the previous item will be in inventory, not equipped.
    // Therefore, if the item is in inventory, take it from there.
    // Otherwise, take it from the offhand slot.

    AdventureResult item = ItemPool.get(itemId, 1);

    // Remove from saved checkpoint
    SpecialOutfit.forgetEquipment(item);

    // Move from offhand to inventory if necessary
    if (InventoryManager.getCount(item) == 0
        && !EquipmentManager.removeEquipment(item, Slot.OFFHAND)) {
      // Not equipped. How odd.
      return;
    }

    // Discard it from inventory
    ResultProcessor.processItem(item.getItemId(), -1);
  }

  public static final void breakEquipment(int itemId, String msg) {
    switch (itemId) {
      // Breaking sugar equipment resets sugar counter
      case ItemPool.SUGAR_CHAPEAU,
          ItemPool.SUGAR_SHANK,
          ItemPool.SUGAR_SHIELD,
          ItemPool.SUGAR_SHILLELAGH,
          ItemPool.SUGAR_SHIRT,
          ItemPool.SUGAR_SHOTGUN,
          ItemPool.SUGAR_SHORTS ->
          Preferences.setInteger("sugarCounter" + itemId, 0);

      // Breaking cozy equipment resets cozy counter
      case ItemPool.COZY_SCIMITAR, ItemPool.COZY_STAFF, ItemPool.COZY_BAZOOKA ->
          Preferences.setInteger("cozyCounter" + itemId, 0);
    }

    // Discard the item, but do not clear it from outfit checkpoints yet.
    Slot slot = EquipmentManager.discardEquipment(itemId, false);
    if (slot == Slot.NONE) {
      RequestLogger.printLine("(unable to determine slot of broken equipment)");
      return;
    }
    AdventureResult item = ItemPool.get(itemId, 1);

    int action = Preferences.getInteger("breakableHandling" + itemId);
    if (action == 0) {
      action = Preferences.getInteger("breakableHandling");
    }
    // 1: abort
    // 2: equip previous
    // 3: re-equip from inventory, or abort
    // 4: re-equip from inventory, or previous
    // 5: acquire & re-equip
    if (action >= 5) {
      InventoryManager.retrieveItem(item);
      action -= 2;
    }
    if (action >= 3) {
      if (InventoryManager.hasItem(item)) {
        RequestLogger.printLine(msg);
        RequestThread.postRequest(new EquipmentRequest(item, slot));
        return;
      }
      action -= 2;
    }
    if (action <= 1) {
      SpecialOutfit.forgetEquipment(item);
      KoLmafia.updateDisplay(MafiaState.PENDING, msg);
      return;
    }
    List<AdventureResult> list = EquipmentManager.historyLists.get(slot);
    for (int i = list.size() - 1; i >= 0; --i) {
      AdventureResult prev = list.get(i);
      if (prev.equals(EquipmentRequest.UNEQUIP)
          || prev.equals(item)
          || !InventoryManager.hasItem(prev)
          || (slot == Slot.FAMILIAR && !KoLCharacter.getFamiliar().canEquip(prev))) {
        continue;
      }

      SpecialOutfit.replaceEquipment(item, prev);
      RequestLogger.printLine(msg);
      RequestThread.postRequest(new EquipmentRequest(prev, slot));
      return;
    }
    SpecialOutfit.forgetEquipment(item);
    KoLmafia.updateDisplay(msg + "  No previous item to equip.");
  }

  public static final void checkFamiliar(final Slot slot) {
    switch (KoLCharacter.getFamiliar().getId()) {
      case FamiliarPool.HATRACK:
        if (slot == Slot.HAT) {
          EquipmentManager.updateEquipmentList(Slot.FAMILIAR);
        } else if (slot == Slot.FAMILIAR) {
          EquipmentManager.updateEquipmentList(Slot.HAT);
        }
        break;

      case FamiliarPool.HAND:
        if (slot == Slot.WEAPON || slot == Slot.OFFHAND) {
          EquipmentManager.updateEquipmentList(Slot.FAMILIAR);
        } else if (slot == Slot.FAMILIAR) {
          EquipmentManager.updateEquipmentList(Slot.WEAPON);
          EquipmentManager.updateEquipmentList(Slot.OFFHAND);
        }
        break;

      case FamiliarPool.LEFT_HAND:
        if (slot == Slot.OFFHAND) {
          EquipmentManager.updateEquipmentList(Slot.FAMILIAR);
        } else if (slot == Slot.FAMILIAR) {
          EquipmentManager.updateEquipmentList(Slot.OFFHAND);
        }
        break;

      case FamiliarPool.SCARECROW:
        if (slot == Slot.PANTS) {
          EquipmentManager.updateEquipmentList(Slot.FAMILIAR);
        } else if (slot == Slot.FAMILIAR) {
          EquipmentManager.updateEquipmentList(Slot.PANTS);
        }
        break;
    }
  }

  /**
   * Accessor method to set the equipment the character is currently using. This does not take into
   * account the power of the item or anything of that nature; only the item's name is stored. Note
   * that if no item is equipped, the value should be <code>none</code>, not <code>null</code> or
   * the empty string.
   *
   * @param equipment All of the available equipment, stored as a map
   */
  public static final void setEquipment(final Map<Slot, AdventureResult> equipment) {
    // Defer updating so that we don't regenerate every GearChangeFrame list once for each slot.
    GearChangePanel.deferUpdate();
    for (var slot : SlotSet.ALL_SLOTS) {
      var equipped = equipment.get(slot);
      if (equipped == null) {
      } else if (equipped.equals(EquipmentRequest.UNEQUIP)) {
        setEquipment(slot, EquipmentRequest.UNEQUIP);
      } else {
        setEquipment(slot, equipped);
      }
    }
    GearChangePanel.resolveDeferredUpdate();
  }

  public static final void setCustomOutfits(final List<SpecialOutfit> newOutfits) {
    // Rebuild outfits if given a new list
    if (newOutfits != null) {
      EquipmentManager.customOutfits.clear();
      EquipmentManager.customOutfits.add(SpecialOutfit.NO_CHANGE);
      EquipmentManager.customOutfits.addAll(newOutfits);
    }
  }

  public static final boolean isDualWielding() {
    AdventureResult mainhand = EquipmentManager.equipment.get(Slot.WEAPON);
    AdventureResult offhand = EquipmentManager.equipment.get(Slot.OFFHAND);

    return !mainhand.equals(EquipmentRequest.UNEQUIP)
        && ItemDatabase.getConsumptionType(offhand) == ConsumptionType.WEAPON;
  }

  /**
   * Accessor method to retrieve the name of the item equipped on the character's familiar.
   *
   * @return The name of the item equipped on the character's familiar, <code>none</code> if no such
   *     item exists
   */
  public static final AdventureResult getFamiliarItem() {
    return KoLCharacter.currentFamiliar == null
        ? EquipmentRequest.UNEQUIP
        : KoLCharacter.currentFamiliar.getItem();
  }

  public static final AdventureResult lockedFamiliarItem() {
    return EquipmentManager.lockedFamiliarItem;
  }

  public static final boolean familiarItemLockable() {
    return FamiliarData.lockableItem(EquipmentManager.getFamiliarItem());
  }

  public static final boolean familiarItemLocked() {
    return EquipmentManager.lockedFamiliarItem() != EquipmentRequest.UNEQUIP;
  }

  public static final void lockFamiliarItem() {
    EquipmentManager.lockFamiliarItem(
        EquipmentManager.familiarItemLocked() && EquipmentManager.familiarItemLockable());
  }

  public static final void lockFamiliarItem(boolean lock) {
    EquipmentManager.lockFamiliarItem(
        lock ? EquipmentManager.getFamiliarItem() : EquipmentRequest.UNEQUIP);
  }

  public static final void lockFamiliarItem(AdventureResult item) {
    if (EquipmentManager.lockedFamiliarItem != item) {
      EquipmentManager.lockedFamiliarItem = item;
      NamedListenerRegistry.fireChange("(familiarLock)");
    }
  }

  public static final int getFakeHands() {
    return EquipmentManager.fakeHandCount;
  }

  public static final void setFakeHands(final int hands) {
    if (EquipmentManager.fakeHandCount != hands) {
      EquipmentManager.fakeHandCount = hands;
      NamedListenerRegistry.fireChange("(fakehands)");
    }
  }

  public static final int getStinkyCheeseLevel() {
    return KoLCharacter.currentBitmapModifier(BitmapModifier.STINKYCHEESE);
  }

  /**
   * Accessor method to retrieve the name of a piece of equipment
   *
   * @param type the type of equipment
   * @return The name of the equipment, <code>none</code> if no such item exists
   */
  public static final AdventureResult getEquipment(final Slot type) {
    if (type == Slot.FAMILIAR) {
      return getFamiliarItem();
    }

    return equipment.getOrDefault(type, EquipmentRequest.UNEQUIP);
  }

  public static final int getTurns(Slot slot) {
    return EquipmentManager.turnsRemaining.getOrDefault(slot, 0);
  }

  public static final void setTurns(Slot slot, int minTurns, int maxTurns) {
    int curr = EquipmentManager.turnsRemaining.getOrDefault(slot, 0);
    if (curr > maxTurns) {
      curr = maxTurns;
    }
    if (curr < minTurns) {
      curr = minTurns;
    }
    EquipmentManager.turnsRemaining.put(slot, curr);
    GearChangePanel.updateStickers(
        EquipmentManager.turnsRemaining.getOrDefault(Slot.STICKER1, 0),
        EquipmentManager.turnsRemaining.getOrDefault(Slot.STICKER2, 0),
        EquipmentManager.turnsRemaining.getOrDefault(Slot.STICKER3, 0));
  }

  public static final boolean isStickerWeapon(AdventureResult item) {
    return item != null && isStickerWeapon(item.getItemId());
  }

  public static final boolean isStickerWeapon(int itemId) {
    return itemId == ItemPool.STICKER_SWORD || itemId == ItemPool.STICKER_CROSSBOW;
  }

  public static final boolean usingStickerWeapon() {
    return isStickerWeapon(getEquipment(Slot.WEAPON))
        || isStickerWeapon(getEquipment(Slot.OFFHAND))
        || isStickerWeapon(getEquipment(Slot.FAMILIAR));
  }

  public static final boolean hasStickerWeapon() {
    return EquipmentManager.usingStickerWeapon()
        || InventoryManager.hasItem(ItemPool.STICKER_SWORD)
        || InventoryManager.hasItem(ItemPool.STICKER_CROSSBOW);
  }

  public static final void incrementEquipmentCounters() {
    for (var slot : SlotSet.SLOTS) {
      int itemId = EquipmentManager.getEquipment(slot).getItemId();
      incrementEquipmentCounters(itemId);
    }
    if (KoLCharacter.inHatTrick()) {
      for (var hat : EquipmentManager.getHatTrickHats()) {
        incrementEquipmentCounters(hat);
      }
    }
  }

  private static final void incrementEquipmentCounters(int itemId) {
    switch (itemId) {
      case ItemPool.SUGAR_CHAPEAU,
          ItemPool.SUGAR_SHANK,
          ItemPool.SUGAR_SHIELD,
          ItemPool.SUGAR_SHILLELAGH,
          ItemPool.SUGAR_SHIRT,
          ItemPool.SUGAR_SHOTGUN,
          ItemPool.SUGAR_SHORTS ->
          Preferences.increment("sugarCounter" + itemId, 1);
      case ItemPool.COZY_SCIMITAR, ItemPool.COZY_STAFF, ItemPool.COZY_BAZOOKA ->
          Preferences.increment("cozyCounter" + itemId, 1);
    }
  }

  public static final void decrementTurns() {
    if (usingStickerWeapon()) {
      for (var slot : SlotSet.STICKER_SLOTS) {
        EquipmentManager.turnsRemaining.compute(slot, (k, v) -> v == null ? -1 : v - 1);
      }
      GearChangePanel.updateStickers(
          EquipmentManager.turnsRemaining.getOrDefault(Slot.STICKER1, 0),
          EquipmentManager.turnsRemaining.getOrDefault(Slot.STICKER2, 0),
          EquipmentManager.turnsRemaining.getOrDefault(Slot.STICKER3, 0));
    }

    EquipmentManager.incrementEquipmentCounters();
  }

  public static final void stickersExpired(int count) {
    for (var slot : SlotSet.STICKER_SLOTS) {
      if (EquipmentManager.turnsRemaining.getOrDefault(slot, 0) <= 0
          && getEquipment(slot) != EquipmentRequest.UNEQUIP) {
        setEquipment(slot, EquipmentRequest.UNEQUIP);
        --count;
      }
    }
    if (count != 0) { // we've lost count somewhere, refresh
      RequestThread.postRequest(new EquipmentRequest(EquipmentRequestType.BEDAZZLEMENTS));
    }
  }

  /**
   * Accessor method to retrieve a list of all available items which can be equipped by familiars.
   * Note this lists items which the current familiar cannot equip.
   */
  public static final Map<Slot, List<AdventureResult>> getEquipmentLists() {
    return EquipmentManager.equipmentLists;
  }

  public static final void updateEquipmentList(final Slot listIndex) {
    ConsumptionType consumeFilter = EquipmentManager.equipmentTypeToConsumeFilter(listIndex);
    // Codpiece slots don't have a ConsumptionType, handle them specially
    if (consumeFilter == ConsumptionType.UNKNOWN && !SlotSet.CODPIECE_SLOTS.contains(listIndex)) {
      return;
    }

    AdventureResult equippedItem = EquipmentManager.getEquipment(listIndex);

    switch (listIndex) {
      case ACCESSORY1:
      case ACCESSORY2:
        return; // do all the work when updating ACC3
      case ACCESSORY3:
        EquipmentManager.updateEquipmentList(consumeFilter, EquipmentManager.accessories);
        AdventureResult accessory = EquipmentManager.getEquipment(Slot.ACCESSORY1);
        if (accessory != EquipmentRequest.UNEQUIP) {
          AdventureResult.addResultToList(EquipmentManager.accessories, accessory);
        }
        accessory = EquipmentManager.getEquipment(Slot.ACCESSORY2);
        if (accessory != EquipmentRequest.UNEQUIP) {
          AdventureResult.addResultToList(EquipmentManager.accessories, accessory);
        }
        accessory = EquipmentManager.getEquipment(Slot.ACCESSORY3);
        if (accessory != EquipmentRequest.UNEQUIP) {
          AdventureResult.addResultToList(EquipmentManager.accessories, accessory);
        }
        break;

      case FAMILIAR:

        // If we are looking at familiar items, include those
        // which can be universally equipped, but are currently
        // on another familiar.

        EquipmentManager.updateEquipmentList(
            consumeFilter, EquipmentManager.equipmentLists.get(Slot.FAMILIAR));

        FamiliarData[] familiarList = KoLCharacter.ownedFamiliars().toArray(new FamiliarData[0]);

        FamiliarData currentFamiliar = KoLCharacter.getFamiliar();

        for (FamiliarData familiarData : familiarList) {
          AdventureResult currentItem = familiarData.getItem();
          if (currentItem != EquipmentRequest.UNEQUIP && currentFamiliar.canEquip(currentItem)) {
            AdventureResult.addResultToList(
                EquipmentManager.equipmentLists.get(Slot.FAMILIAR), currentItem);
          }
        }

        break;

      case CODPIECE1:
      case CODPIECE2:
      case CODPIECE3:
      case CODPIECE4:
        return; // do all the work when updating CODPIECE5
      case CODPIECE5:
        EquipmentManager.updateCodpieceList();
        break;

      default:
        EquipmentManager.updateEquipmentList(
            consumeFilter, EquipmentManager.equipmentLists.get(listIndex));
        if (!EquipmentManager.equipmentLists.get(listIndex).contains(equippedItem)) {
          EquipmentManager.equipmentLists.get(listIndex).add(equippedItem);
        }

        break;
    }

    LockableListFactory.setSelectedItem(
        EquipmentManager.equipmentLists.get(listIndex), equippedItem);
  }

  private static void updateEquipmentList(
      final ConsumptionType filterId, final List<AdventureResult> currentList) {
    ArrayList<AdventureResult> temporary = new ArrayList<>();
    temporary.add(EquipmentRequest.UNEQUIP);

    // If the character is currently equipped with a one-handed
    // weapon and the character has the ability to dual-wield
    // weapons, then also allow one-handed weapons in the off-hand.

    boolean dual =
        getWeaponHandedness() == 1 && KoLCharacter.hasSkill(SkillPool.DOUBLE_FISTED_SKULL_SMASHING);
    WeaponType weaponType = EquipmentManager.getWeaponType();
    FamiliarData currentFamiliar = KoLCharacter.getFamiliar();

    for (AdventureResult currentItem : KoLConstants.inventory) {
      String currentItemName = currentItem.getName();

      int itemId = currentItem.getItemId();
      ConsumptionType type = ItemDatabase.getConsumptionType(itemId);

      // If we want off-hand items and we can dual wield,
      // allow one-handed weapons of same type

      if (filterId == ConsumptionType.OFFHAND && type == ConsumptionType.WEAPON && dual) {
        if (EquipmentDatabase.isMainhandOnly(itemId)
            || EquipmentDatabase.getWeaponType(itemId) != weaponType) {
          continue;
        }
      }

      // If we are equipping familiar items, make sure
      // current familiar can use this one

      else if (filterId == ConsumptionType.FAMILIAR_EQUIPMENT) {
        if (currentFamiliar.canEquip(currentItem)) {
          temporary.add(currentItem.getInstance(1));
        }

        continue;
      }

      // Otherwise, slot and item type must match

      else if (filterId != type) {
        continue;
      } else if (filterId == ConsumptionType.WEAPON && dual) {
        if (EquipmentDatabase.getHands(itemId) == 1
            && EquipmentDatabase.getWeaponType(itemId) != weaponType) {
          continue;
        }
      }

      temporary.add(currentItem);
    }

    currentList.retainAll(temporary);
    temporary.removeAll(currentList);
    currentList.addAll(temporary);
  }

  private static void updateCodpieceList() {
    // Codpiece gems are identified by having ETERNITY_CODPIECE modifiers
    ArrayList<AdventureResult> temporary = new ArrayList<>();
    temporary.add(EquipmentRequest.UNEQUIP);

    for (AdventureResult currentItem : KoLConstants.inventory) {
      int itemId = currentItem.getItemId();
      if (EquipmentRequest.isCodpieceGem(itemId)) {
        temporary.add(currentItem);
      }
    }

    // Update all codpiece slot lists
    for (Slot slot : SlotSet.CODPIECE_SLOTS) {
      List<AdventureResult> currentList = EquipmentManager.equipmentLists.get(slot);
      AdventureResult equippedItem = EquipmentManager.getEquipment(slot);

      currentList.retainAll(temporary);
      ArrayList<AdventureResult> toAdd = new ArrayList<>(temporary);
      toAdd.removeAll(currentList);
      currentList.addAll(toAdd);

      // Make sure currently equipped item is in the list
      if (equippedItem != EquipmentRequest.UNEQUIP && !currentList.contains(equippedItem)) {
        currentList.add(equippedItem);
      }

      LockableListFactory.setSelectedItem(currentList, equippedItem);
    }
  }

  /**
   * Accessor method to retrieve a list of the custom outfits available to this character, based on
   * the last time the equipment screen was requested.
   *
   * @return A <code>List</code> of the available outfits
   */
  public static final List<SpecialOutfit> getCustomOutfits() {
    return EquipmentManager.customOutfits;
  }

  public static SpecialOutfit getCustomOutfit(int id) {
    for (SpecialOutfit outfit : EquipmentManager.customOutfits) {
      if (outfit.getOutfitId() == id) {
        return outfit;
      }
    }
    return null;
  }

  public static SpecialOutfit getCustomOutfit(String name) {
    for (SpecialOutfit outfit : EquipmentManager.customOutfits) {
      if (outfit.getName().equals(name)) {
        return outfit;
      }
    }
    return null;
  }

  /**
   * Accessor method to add or replace a custom outfit in the list of custom outfits available to
   * this character
   */
  public static void addCustomOutfit(SpecialOutfit outfit) {
    List<SpecialOutfit> newOutfits = new SortedList<>();
    String name = outfit.getName();

    for (SpecialOutfit current : EquipmentManager.customOutfits) {
      if (current == SpecialOutfit.NO_CHANGE) {
        continue;
      }
      if (!current.getName().equals(name)) {
        newOutfits.add(current);
      }
    }
    newOutfits.add(outfit);
    EquipmentManager.setCustomOutfits(newOutfits);
  }

  /**
   * Accessor method to retrieve a list of the all the outfits available to this character, based on
   * the last time the equipment screen was requested.
   *
   * @return A <code>List</code> of the available outfits
   */
  public static final List<SpecialOutfit> getOutfits() {
    return EquipmentManager.normalOutfits;
  }

  public static final void updateEquipmentLists() {
    KoLCharacter.resetTriggers();
    for (var slot : SlotSet.ALL_SLOTS) {
      EquipmentManager.updateEquipmentList(slot);
    }
    EquipmentManager.updateNormalOutfits();
  }

  public static final ConsumptionType equipmentTypeToConsumeFilter(final Slot equipmentType) {
    return switch (equipmentType) {
      case HAT -> ConsumptionType.HAT;
      case WEAPON -> ConsumptionType.WEAPON;
      case OFFHAND -> ConsumptionType.OFFHAND;
      case SHIRT -> ConsumptionType.SHIRT;
      case PANTS -> ConsumptionType.PANTS;
      case CONTAINER -> ConsumptionType.CONTAINER;
      case ACCESSORY1, ACCESSORY2, ACCESSORY3 -> ConsumptionType.ACCESSORY;
      case FAMILIAR -> ConsumptionType.FAMILIAR_EQUIPMENT;
      case STICKER1, STICKER2, STICKER3 -> ConsumptionType.STICKER;
      case CARDSLEEVE -> ConsumptionType.CARD;
      case FOLDER1, FOLDER2, FOLDER3, FOLDER4, FOLDER5 -> ConsumptionType.FOLDER;
      case BOOTSKIN -> ConsumptionType.BOOTSKIN;
      case BOOTSPUR -> ConsumptionType.BOOTSPUR;
      case HOLSTER -> ConsumptionType.SIXGUN;
      default -> ConsumptionType.UNKNOWN;
    };
  }

  public static final Slot consumeFilterToEquipmentType(final ConsumptionType consumeFilter) {
    return switch (consumeFilter) {
      case HAT -> Slot.HAT;
      case WEAPON -> Slot.WEAPON;
      case OFFHAND -> Slot.OFFHAND;
      case SHIRT -> Slot.SHIRT;
      case PANTS -> Slot.PANTS;
      case CONTAINER -> Slot.CONTAINER;
      case ACCESSORY -> Slot.ACCESSORY1;
      case FAMILIAR_EQUIPMENT -> Slot.FAMILIAR;
      case STICKER -> Slot.STICKER1;
      case CARD -> Slot.CARDSLEEVE;
      case FOLDER -> Slot.FOLDER1;
      case BOOTSKIN -> Slot.BOOTSKIN;
      case BOOTSPUR -> Slot.BOOTSPUR;
      case SIXGUN -> Slot.HOLSTER;
      default -> Slot.NONE;
    };
  }

  public static final Slot itemIdToEquipmentType(final int itemId) {
    return EquipmentManager.consumeFilterToEquipmentType(ItemDatabase.getConsumptionType(itemId));
  }

  /**
   * Accessor method to retrieve # of hands character's weapon uses
   *
   * @return int number of hands needed
   */
  public static final int getWeaponHandedness() {
    return EquipmentDatabase.getHands(EquipmentManager.getEquipment(Slot.WEAPON).getItemId());
  }

  /**
   * Accessor method to determine if character is currently dual-wielding
   *
   * @return boolean true if character has two weapons equipped
   */
  public static final boolean usingTwoWeapons() {
    return EquipmentDatabase.getHands(EquipmentManager.getEquipment(Slot.OFFHAND).getItemId()) == 1;
  }

  /**
   * Accessor method to determine if character's weapon is a chefstaff
   *
   * @return boolean true if weapon is a chefstaff
   */
  public static final boolean usingChefstaff() {
    return EquipmentDatabase.isChefStaff(EquipmentManager.getEquipment(Slot.WEAPON));
  }

  /**
   * Accessor method to determine if character's off-hand is a bean can
   *
   * @return boolean true if off-hand is a bean can
   */
  public static final boolean usingCanOfBeans() {
    return EquipmentDatabase.isCanOfBeans(EquipmentManager.getEquipment(Slot.OFFHAND));
  }

  /**
   * Accessor method to determine if character has a sixgun holstered
   *
   * @return boolean true if off-hand is a bean can
   */
  public static final boolean holsteredSixgun() {
    return EquipmentManager.getEquipment(Slot.HOLSTER) != EquipmentRequest.UNEQUIP;
  }

  /**
   * Accessor method to determine available battery power in equipped Powerful Glove
   *
   * @return int 0 if not wearing Powerful Glove, otherwise remaining battery power
   */
  public static final int powerfulGloveUsableBatteryPower() {
    return (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.POWERFUL_GLOVE, 1))
            || (KoLCharacter.inLegacyOfLoathing()
                && KoLCharacter.hasEquipped(ItemPool.get(ItemPool.REPLICA_POWERFUL_GLOVE, 1))))
        ? EquipmentManager.powerfulGloveAvailableBatteryPower()
        : 0;
  }

  public static final int powerfulGloveAvailableBatteryPower() {
    return 100 - Preferences.getInteger("_powerfulGloveBatteryPowerUsed");
  }

  public static final int fireExtinguisherAvailableFoam() {
    return Preferences.getInteger("_fireExtinguisherCharge");
  }

  /**
   * Accessor method to determine if character's weapon's is a club
   *
   * @return boolean true if weapon is a club
   */
  public static final boolean wieldingClub() {
    return EquipmentManager.wieldingClub(true);
  }

  /**
   * Here's a version which allows you to include or exclude the Iron Palm effect for the purpose of
   * determining whether a sword counts as a club. Mother Hellseals require an actual club...
   */
  public static final AdventureResult IRON_PALMS = EffectPool.get(EffectPool.IRON_PALMS);

  public static final boolean wieldingClub(final boolean includeEffect) {
    String type =
        EquipmentDatabase.getItemType(EquipmentManager.getEquipment(Slot.WEAPON).getItemId());
    return type.equals("club")
        || (includeEffect
            && KoLConstants.activeEffects.contains(EquipmentManager.IRON_PALMS)
            && type.equals("sword"));
  }

  public static final boolean wieldingKnife() {
    String type =
        EquipmentDatabase.getItemType(EquipmentManager.getEquipment(Slot.WEAPON).getItemId());
    return type.equals("knife");
  }

  public static final boolean wieldingAccordion() {
    String type =
        EquipmentDatabase.getItemType(EquipmentManager.getEquipment(Slot.WEAPON).getItemId());
    return type.equals("accordion");
  }

  public static final boolean wieldingSword() {
    return EquipmentManager.wieldingSword(true);
  }

  public static final boolean wieldingSword(final boolean includeEffect) {
    boolean sword =
        EquipmentDatabase.isSword(EquipmentManager.getEquipment(Slot.WEAPON).getItemId());
    return sword
        && (!includeEffect || !KoLConstants.activeEffects.contains(EquipmentManager.IRON_PALMS));
  }

  public static final boolean wieldingGun() {
    int id = EquipmentManager.getEquipment(Slot.WEAPON).getItemId();
    // These are the weapons retrocape considers a "gun"
    return EquipmentDatabase.isGun(id)
        || EquipmentDatabase.isPistol(id)
        || EquipmentDatabase.isRifle(id);
  }

  /**
   * Accessor method to determine if character is currently using a shield
   *
   * @return boolean true if character has a shield equipped
   */
  public static final boolean usingShield() {
    return EquipmentDatabase.getItemType(EquipmentManager.getEquipment(Slot.OFFHAND).getItemId())
        .equals("shield");
  }

  /**
   * Accessor method to determine what type of weapon the character is wielding.
   *
   * @return int MELEE or RANGED
   */
  public static final WeaponType getWeaponType() {
    return EquipmentDatabase.getWeaponType(EquipmentManager.getEquipment(Slot.WEAPON).getItemId());
  }

  /**
   * Accessor method to determine which stat determines the character's chance to hit.
   *
   * @return int MOXIE or MUSCLE
   */
  public static final Stat getHitStatType() {
    switch (EquipmentManager.getWeaponType()) {
      case RANGED:
        return Stat.MOXIE;
      default:
        if (KoLCharacter.getAdjustedMoxie() >= KoLCharacter.getAdjustedMuscle()
            && EquipmentManager.wieldingKnife()
            && KoLCharacter.hasSkill(SkillPool.TRICKY_KNIFEWORK)) {
          return Stat.MOXIE;
        }
        var mainhand = EquipmentManager.getEquipment(Slot.WEAPON).getItemId();
        if (mainhand == ItemPool.FOURTH_SABER || mainhand == ItemPool.REPLICA_FOURTH_SABER) {
          // Fourth of May Cosplay Saber uses highest buffed stat
          if (KoLCharacter.getAdjustedMoxie() >= KoLCharacter.getAdjustedMuscle()
              && KoLCharacter.getAdjustedMoxie() >= KoLCharacter.getAdjustedMysticality()) {
            return Stat.MOXIE;
          }
          if (KoLCharacter.getAdjustedMysticality() >= KoLCharacter.getAdjustedMuscle()
              && KoLCharacter.getAdjustedMysticality() >= KoLCharacter.getAdjustedMoxie()) {
            return Stat.MYSTICALITY;
          }
        }
        return Stat.MUSCLE;
    }
  }

  /**
   * Accessor method to determine character's adjusted hit stat
   *
   * @return int adjusted muscle, mysticality, or moxie
   */
  public static final int getAdjustedHitStat() {
    int hitStat;
    if (KoLCharacter.currentBooleanModifier(BooleanModifier.ATTACKS_CANT_MISS)) {
      return Integer.MAX_VALUE;
    }
    switch (getHitStatType()) {
      default:
      case MUSCLE:
        hitStat = KoLCharacter.getAdjustedMuscle();
        if (KoLCharacter.isUnarmed()
            && KoLCharacter.hasSkill(SkillPool.MASTER_OF_THE_SURPRISING_FIST)) {
          hitStat += 20;
        }
        return hitStat;
      case MYSTICALITY:
        return KoLCharacter.getAdjustedMysticality();
      case MOXIE:
        hitStat = KoLCharacter.getAdjustedMoxie();
        if (EquipmentManager.wieldingAccordion()
            && KoLCharacter.hasSkill(SkillPool.CRAB_CLAW_TECHNIQUE)) {
          hitStat += 50;
        }
        return hitStat;
    }
  }

  public static final boolean hasOutfit(final int id) {
    return EquipmentManager.normalOutfits.contains(EquipmentDatabase.normalOutfits.get(id));
  }

  public static final void updateNormalOutfits() {
    ArrayList<SpecialOutfit> available = new ArrayList<>();

    for (SpecialOutfit outfit : EquipmentDatabase.normalOutfits.values()) {
      if (outfit != null && outfit.hasAllPieces()) {
        available.add(outfit);
      }
    }

    for (SpecialOutfit outfit : EquipmentDatabase.weirdOutfits) {
      if (outfit != null && outfit.hasAllPieces()) {
        available.add(outfit);
      }
    }

    Collections.sort(available);

    EquipmentManager.normalOutfits.clear();

    // Start with the three constant outfits
    EquipmentManager.normalOutfits.add(SpecialOutfit.NO_CHANGE);
    EquipmentManager.normalOutfits.add(SpecialOutfit.BIRTHDAY_SUIT);
    EquipmentManager.normalOutfits.add(SpecialOutfit.PREVIOUS_OUTFIT);

    // Finally any standard outfits
    EquipmentManager.normalOutfits.addAll(available);

    // We may have gotten the war hippy or frat outfits
    NamedListenerRegistry.fireChange("(outfit)");
  }

  /**
   * Utility method which determines whether or not the equipment corresponding to the given outfit
   * is already equipped.
   */
  public static final boolean isWearingOutfit(final int outfitId) {
    if (outfitId < 0) {
      return true;
    }

    if (outfitId == 0) {
      return false;
    }

    return EquipmentManager.isWearingOutfit(EquipmentDatabase.normalOutfits.get(outfitId));
  }

  /**
   * Utility method which determines whether or not the equipment corresponding to the given outfit
   * is already equipped.
   */
  public static final boolean isWearingOutfit(final SpecialOutfit outfit) {
    return outfit != null && outfit.isWearing();
  }

  public static final boolean retrieveOutfit(final SpecialOutfit outfit) {
    return outfit != null && outfit.retrieve();
  }

  public static final boolean addOutfitConditions(final KoLAdventure adventure) {
    int outfitId = EquipmentDatabase.getOutfitId(adventure);
    if (outfitId <= 0) {
      return false;
    }

    EquipmentManager.addOutfitConditions(outfitId);
    return true;
  }

  public static final void addOutfitConditions(final int outfitId) {
    // Ignore custom outfits, since there's
    // no way to know what they are (yet).

    if (outfitId < 0) {
      return;
    }

    AdventureResult[] pieces = EquipmentDatabase.normalOutfits.get(outfitId).getPieces();
    for (int i = 0; i < pieces.length; ++i) {
      if (!KoLCharacter.hasEquipped(pieces[i])) {
        ConditionsCommand.update("set", pieces[i].getName());
      }
    }
  }

  /** Utility method which determines the outfit ID the character is currently wearing */
  public static final SpecialOutfit currentOutfit() {
    for (SpecialOutfit outfit : EquipmentDatabase.normalOutfits.values()) {
      if (outfit == null) {
        continue;
      }
      if (outfit.isWearing()) {
        return outfit;
      }
    }

    return null;
  }

  public static final SpecialOutfit currentOutfit(Map<Slot, AdventureResult> equipment) {
    // Go through any outfit that any worn item belongs to.
    int hash = SpecialOutfit.equipmentHash(equipment);
    List<Integer> checkedOutfits = new ArrayList<>();
    for (var slot : SlotSet.CORE_EQUIP_SLOTS) {
      AdventureResult item = equipment.getOrDefault(slot, null);
      if (item == null) continue;
      int outfitId = EquipmentDatabase.getOutfitWithItem(item.getItemId());
      if (checkedOutfits.contains(outfitId)) continue;

      SpecialOutfit outfit = EquipmentDatabase.getOutfit(outfitId);
      if (outfit != null) {
        if (outfit.isWearing(equipment, hash)) {
          return outfit;
        }
        checkedOutfits.add(outfitId);
      }
    }

    return null;
  }

  public static final boolean canEquip(final AdventureResult item) {
    return EquipmentManager.canEquip(item.getItemId());
  }

  public static final boolean canEquip(final String itemName) {
    return EquipmentManager.canEquip(ItemDatabase.getItemId(itemName));
  }

  public static final boolean canEquip(final int itemId) {
    if (itemId == -1) {
      return false;
    }

    ConsumptionType type = ItemDatabase.getConsumptionType(itemId);

    if (type == ConsumptionType.SIXGUN) {
      return KoLCharacter.isAWoLClass();
    }

    if (type == ConsumptionType.SHIRT && !KoLCharacter.isTorsoAware()) {
      return false;
    }

    if (type == ConsumptionType.FAMILIAR_EQUIPMENT) {
      return KoLCharacter.getFamiliar().canEquip(ItemPool.get(itemId, 1));
    }

    if (KoLCharacter.inRobocore() && !YouRobotManager.canEquip(type)) {
      return false;
    }

    if (KoLCharacter.inFistcore()
        && (type == ConsumptionType.WEAPON || type == ConsumptionType.OFFHAND)) {
      return false;
    }

    if (KoLCharacter.inAxecore()
        && (type == ConsumptionType.WEAPON || type == ConsumptionType.OFFHAND)) {
      return itemId == ItemPool.TRUSTY;
    }

    if (KoLCharacter.isHardcore()) {
      Modifiers mods = ModifierDatabase.getItemModifiers(itemId);
      if (mods != null && mods.getBoolean(BooleanModifier.SOFTCORE)) {
        return false;
      }
    }

    if (!KoLCharacter.isAccordionThief() && EquipmentDatabase.isSpecialAccordion(itemId)) {
      return false;
    }

    return EquipmentManager.meetsStatRequirements(itemId);
  }

  public static final boolean meetsStatRequirements(final int itemId) {
    if (KoLCharacter.inSmallcore()) {
      // stat requirements are ignored
      return true;
    }

    EquipmentRequirement req =
        new EquipmentRequirement(EquipmentDatabase.getEquipRequirement(itemId));

    if (req.isMuscle()) {
      return KoLCharacter.getBaseMuscle() >= req.getAmount()
          || KoLCharacter.muscleTrigger(req.getAmount(), itemId);
    }

    if (req.isMysticality()) {
      return KoLCharacter.getBaseMysticality() >= req.getAmount()
          || KoLCharacter.mysticalityTrigger(req.getAmount(), itemId);
    }

    if (req.isMoxie()) {
      return KoLCharacter.getBaseMoxie() >= req.getAmount()
          || KoLCharacter.moxieTrigger(req.getAmount(), itemId);
    }

    return true;
  }

  public static final SpecialOutfit getMatchingOutfit(final String name) {
    String lowercaseName = name.toLowerCase().trim();

    if (lowercaseName.equals("birthday suit") || lowercaseName.equals("nothing")) {
      return SpecialOutfit.BIRTHDAY_SUIT;
    }

    if (lowercaseName.equals("last")) {
      return SpecialOutfit.PREVIOUS_OUTFIT;
    }

    // Check for exact matches.
    for (SpecialOutfit outfit : EquipmentManager.customOutfits) {
      if (outfit == SpecialOutfit.NO_CHANGE) {
        continue;
      }
      if (lowercaseName.equals(outfit.toString().toLowerCase())) {
        return outfit;
      }
    }

    for (SpecialOutfit outfit : EquipmentDatabase.normalOutfits.values()) {
      if (outfit == null) {
        continue;
      }
      if (lowercaseName.equals(outfit.toString().toLowerCase())) {
        return outfit;
      }
    }

    // Check for substring matches.

    for (SpecialOutfit outfit : EquipmentManager.customOutfits) {
      if (outfit == SpecialOutfit.NO_CHANGE) {
        continue;
      }
      if (outfit.toString().toLowerCase().contains(lowercaseName)) {
        return outfit;
      }
    }

    for (SpecialOutfit outfit : EquipmentDatabase.normalOutfits.values()) {
      if (outfit == null) {
        continue;
      }
      if (outfit.toString().toLowerCase().contains(lowercaseName)) {
        return outfit;
      }
    }

    return null;
  }

  public static AdventureResult equippedItem(final int itemId) {
    if (itemId == 0) {
      return EquipmentRequest.UNEQUIP;
    }

    String name = ItemDatabase.getItemDataName(itemId);
    if (name == null) {
      // Fetch descid from api.php?what=item
      // and register new item.
      ItemDatabase.registerItem(itemId);
    }

    return ItemPool.get(itemId, 1);
  }

  public static final int equippedCount(final AdventureResult item) {
    int count = 0;
    for (var slot : SlotSet.SLOTS) {
      if (item.equals(EquipmentManager.getEquipment(slot))) {
        count++;
      }
    }
    if (KoLCharacter.inHatTrick()) {
      for (var hat : EquipmentManager.getHatTrickHats()) {
        if (hat == item.getItemId()) {
          count++;
        }
      }
    }
    return count;
  }

  public static final List<Integer> getHatTrickHats() {
    return new ArrayList<>(hats);
  }

  public static final void setHatTrickHats(List<Integer> hats) {
    EquipmentManager.hats = hats;
  }

  public static final void addHatTrickHat(int itemId) {
    EquipmentManager.hats.add(itemId);
  }

  public static final boolean hasHatTrickHat(final int itemId) {
    return hats.contains(itemId);
  }

  public static final void parseStatus(final JSONObject json) throws JSONException {
    // "equipment":{
    //    "hat":"1323",
    //    "shirt":"2586",
    //    "pants":"1324",
    //    "weapon":"1325",
    //    "offhand":"1325",
    //    "acc1":"3337",
    //    "acc2":"1232",
    //    "acc3":"1226",
    //    "container":"482",
    //    "familiarequip":"3343",
    //    "fakehands":0,
    //    "card sleeve":"4968"
    // },
    // "hats":["11565","2283"]
    // "stickers":[0,0,0],
    // "folder_holder":["01","22","12","00","00"]
    // "eternitycod":[10963,11274,0,0,11273]

    EnumMap<Slot, AdventureResult> current = EquipmentManager.allEquipment();
    EnumMap<Slot, AdventureResult> equipment = EquipmentManager.emptyEquipmentArray(true);
    int fakeHands = 0;
    List<Integer> hatTrickHats = new ArrayList<>();

    JSONObject equip = json.getJSONObject("equipment");
    for (String slotName : equip.keySet()) {
      if (slotName.equals("fakehands")) {
        fakeHands = equip.getIntValue(slotName);
        continue;
      }

      Slot slot = Slot.byCaselessPhpName(slotName);
      if (slot == Slot.NONE) {
        continue;
      }

      equipment.put(slot, EquipmentManager.equippedItem(equip.getIntValue(slotName)));
    }
    JSONArray hats = json.getJSONArray("hats");
    if (hats != null) {
      for (int i = 0; i < hats.size(); i++) {
        hatTrickHats.add(hats.getIntValue(i));
      }
    }

    // Read stickers
    JSONArray stickers = json.getJSONArray("stickers");
    int i = 0;
    for (var slot : SlotSet.STICKER_SLOTS) {
      AdventureResult item = EquipmentManager.equippedItem(stickers.getIntValue(i++));
      equipment.put(slot, item);
    }

    // Read folders
    JSONArray folders = json.getJSONArray("folder_holder");
    i = 0;
    for (var slot : SlotSet.FOLDER_SLOTS) {
      int folder = folders.getIntValue(i++);
      AdventureResult item =
          folder == 0 ? EquipmentRequest.UNEQUIP : ItemPool.get(ItemPool.FOLDER_01 - 1 + folder, 1);
      equipment.put(slot, item);
    }

    // Read gems
    JSONArray eternitycod = json.getJSONArray("eternitycod");
    if (eternitycod != null) {
      i = 0;
      for (var slot : SlotSet.CODPIECE_SLOTS) {
        AdventureResult item = EquipmentManager.equippedItem(eternitycod.getIntValue(i++));
        equipment.put(slot, item);
      }
    }

    // We can't read these from api.php (yet?)
    equipment.put(Slot.CROWNOFTHRONES, current.get(Slot.CROWNOFTHRONES));
    equipment.put(Slot.BUDDYBJORN, current.get(Slot.BUDDYBJORN));
    equipment.put(Slot.BOOTSKIN, current.get(Slot.BOOTSKIN));
    equipment.put(Slot.BOOTSPUR, current.get(Slot.BOOTSPUR));
    equipment.put(Slot.HOLSTER, current.get(Slot.HOLSTER));

    // For debug purposes, print wherever KoLmafia's model differs
    // from KoL's model.

    if (!KoLmafia.isRefreshing()) {
      for (var slot : SlotSet.ALL_SLOTS) {
        // Quantum Terrarium will have a familiar item in api.php even
        // if the particular familiar can't equip it. Ignore that.
        if (slot == Slot.FAMILIAR && KoLCharacter.inQuantum()) {
          continue;
        }
        // we use api.php to update our knowledge of codpiece slots
        if (SlotSet.CODPIECE_SLOTS.contains(slot)) {
          continue;
        }
        if (!current.get(slot).equals(equipment.get(slot))) {
          String slotName = slot.name;
          String message =
              "*** slot "
                  + slotName
                  + ": KoL has "
                  + equipment.get(slot)
                  + " but KoLmafia has "
                  + current.get(slot);
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
        }
      }
    }

    // Shuffle inventory and load up KoL's model of equipment
    EquipmentRequest.switchEquipment(current, equipment);

    // *** Locked familiar item

    // Fake hands must be handled separately
    EquipmentManager.setFakeHands(fakeHands);
    EquipmentManager.setHatTrickHats(hatTrickHats);
  }
}
