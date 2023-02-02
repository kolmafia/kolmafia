package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest.EquipmentRequestType;
import net.sourceforge.kolmafia.swingui.panel.GearChangePanel;
import net.sourceforge.kolmafia.textui.command.ConditionsCommand;
import net.sourceforge.kolmafia.utilities.LockableListFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EquipmentManager {
  public static final int NONE = -1;

  // Mutable equipment slots
  public static final int HAT = 0;
  public static final int WEAPON = 1;
  public static final int HOLSTER = 2;
  public static final int OFFHAND = 3;
  public static final int CONTAINER = 4;
  public static final int SHIRT = 5;
  public static final int PANTS = 6;
  public static final int ACCESSORY1 = 7;
  public static final int ACCESSORY2 = 8;
  public static final int ACCESSORY3 = 9;
  public static final int FAMILIAR = 10;

  // Count of slots visible on equipment page: HAT to FAMILIAR
  public static final int SLOTS = 11;

  // Pseudo-equipment slots
  public static final int CROWNOFTHRONES = 11;

  public static final int STICKER1 = 12;
  public static final int STICKER2 = 13;
  public static final int STICKER3 = 14;

  public static final int CARDSLEEVE = 15;

  public static final int FOLDER1 = 16;
  public static final int FOLDER2 = 17;
  public static final int FOLDER3 = 18;
  public static final int FOLDER4 = 19;
  public static final int FOLDER5 = 20;

  public static final int BUDDYBJORN = 21;

  public static final int BOOTSKIN = 22;
  public static final int BOOTSPUR = 23;

  // Count of all equipment slots: HAT to BOOTSPUR
  public static final int ALL_SLOTS = 24;

  public static final int FAKEHAND = 24;

  private static final List<AdventureResult> equipment =
      LockableListFactory.getInstance(AdventureResult.class);
  private static final List<AdventureResult> accessories =
      LockableListFactory.getInstance(AdventureResult.class);
  private static final List<List<AdventureResult>> equipmentLists =
      new ArrayList<>(EquipmentManager.ALL_SLOTS);
  private static final List<List<AdventureResult>> historyLists =
      new ArrayList<>(EquipmentManager.ALL_SLOTS);

  public static final int[] FOLDER_SLOTS =
      new int[] {
        EquipmentManager.FOLDER1,
        EquipmentManager.FOLDER2,
        EquipmentManager.FOLDER3,
        EquipmentManager.FOLDER4,
        EquipmentManager.FOLDER5,
      };

  public static final int[] FOLDER_SLOTS_AFTERCORE =
      new int[] {
        EquipmentManager.FOLDER1, EquipmentManager.FOLDER2, EquipmentManager.FOLDER3,
      };

  public static final int[] ACCESSORY_SLOTS =
      new int[] {
        EquipmentManager.ACCESSORY1, EquipmentManager.ACCESSORY2, EquipmentManager.ACCESSORY3
      };

  public static final int[] STICKER_SLOTS =
      new int[] {EquipmentManager.STICKER1, EquipmentManager.STICKER2, EquipmentManager.STICKER3};

  private static int fakeHandCount = 0;
  private static int stinkyCheeseLevel = 0;

  private static final List<SpecialOutfit> normalOutfits =
      LockableListFactory.getInstance(SpecialOutfit.class);
  private static final List<SpecialOutfit> customOutfits =
      LockableListFactory.getInstance(SpecialOutfit.class);

  private static final int[] turnsRemaining = new int[3];

  private static AdventureResult lockedFamiliarItem = EquipmentRequest.UNEQUIP;

  public static final AdventureResult FAKE_HAND = ItemPool.get(ItemPool.FAKE_HAND, 1);
  public static final AdventureResult CARD_SLEEVE = ItemPool.get(ItemPool.CARD_SLEEVE, 1);
  public static final AdventureResult CROWN_OF_THRONES = ItemPool.get(ItemPool.HATSEAT, 1);
  public static final AdventureResult BUDDY_BJORN = ItemPool.get(ItemPool.BUDDY_BJORN, 1);
  public static final AdventureResult FOLDER_HOLDER = ItemPool.get(ItemPool.FOLDER_HOLDER, 1);
  public static final AdventureResult COWBOY_BOOTS = ItemPool.get(ItemPool.COWBOY_BOOTS, 1);

  static {
    for (int i = 0; i < EquipmentManager.ALL_SLOTS; ++i) {
      EquipmentManager.equipment.add(EquipmentRequest.UNEQUIP);
      EquipmentManager.historyLists.add(new ArrayList<>());

      switch (i) {
        case EquipmentManager.ACCESSORY1,
            EquipmentManager.ACCESSORY2,
            EquipmentManager.ACCESSORY3 -> EquipmentManager.equipmentLists.add(
            LockableListFactory.getMirror(EquipmentManager.accessories));
        default -> EquipmentManager.equipmentLists.add(
            LockableListFactory.getSortedInstance(AdventureResult.class));
      }
    }
  }

  private EquipmentManager() {}

  public static void resetEquipment() {
    for (int i = 0; i < EquipmentManager.equipmentLists.size(); ++i) {
      EquipmentManager.equipmentLists.get(i).clear();
      EquipmentManager.historyLists.get(i).clear();
    }

    EquipmentManager.accessories.clear();
    GearChangePanel.clearEquipmentModels();

    EquipmentManager.equipment.clear();

    for (int i = 0; i < EquipmentManager.ALL_SLOTS; ++i) {
      EquipmentManager.equipment.add(EquipmentRequest.UNEQUIP);
    }

    EquipmentManager.fakeHandCount = 0;
    EquipmentManager.stinkyCheeseLevel = 0;
    EquipmentManager.lockedFamiliarItem = EquipmentRequest.UNEQUIP;
    EquipmentManager.normalOutfits.clear();
  }

  public static void resetCustomOutfits() {
    EquipmentManager.customOutfits.clear();
  }

  public static AdventureResult[] emptyEquipmentArray() {
    return EquipmentManager.emptyEquipmentArray(false);
  }

  public static AdventureResult[] emptyEquipmentArray(boolean all) {
    int length = all ? EquipmentManager.ALL_SLOTS : EquipmentManager.SLOTS;
    AdventureResult[] array = new AdventureResult[length];

    for (int i = 0; i < length; ++i) {
      array[i] = EquipmentRequest.UNEQUIP;
    }

    return array;
  }

  public static AdventureResult[] currentEquipment() {
    AdventureResult[] array = new AdventureResult[EquipmentManager.SLOTS];

    for (int i = 0; i < EquipmentManager.SLOTS; ++i) {
      array[i] = EquipmentManager.getEquipment(i);
    }
    return array;
  }

  public static AdventureResult[] allEquipment() {
    AdventureResult[] array = new AdventureResult[EquipmentManager.ALL_SLOTS];
    array = EquipmentManager.equipment.toArray(array);
    array[EquipmentManager.FAMILIAR] = EquipmentManager.getFamiliarItem();
    return array;
  }

  public static final List<AdventureResult> allEquipmentAsList() {
    return EquipmentManager.equipment;
  }

  public static final void processResult(AdventureResult item) {
    int itemId = item.getItemId();

    // If your current familiar can use this item, add it to familiar item list
    if (KoLCharacter.getFamiliar().canEquip(item)) {
      AdventureResult.addResultToList(
          EquipmentManager.equipmentLists.get(EquipmentManager.FAMILIAR), item);
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

      for (int slot : EquipmentManager.STICKER_SLOTS) {
        AdventureResult current = EquipmentManager.getEquipment(slot);
        AdventureResult.addResultToList(EquipmentManager.equipmentLists.get(slot), item);
        if (!EquipmentManager.equipmentLists.get(slot).contains(current)) {
          EquipmentManager.equipmentLists.get(slot).add(current);
        }
      }
    } else if (consumeType == ConsumptionType.FOLDER) {
      // Folders are similar to stickers

      for (int slot : EquipmentManager.FOLDER_SLOTS) {
        AdventureResult current = EquipmentManager.getEquipment(slot);
        AdventureResult.addResultToList(EquipmentManager.equipmentLists.get(slot), item);
        if (!EquipmentManager.equipmentLists.get(slot).contains(current)) {
          EquipmentManager.equipmentLists.get(slot).add(current);
        }
      }
    } else if (itemId == ItemPool.HATSEAT) {
      AdventureResult.addResultToList(
          EquipmentManager.equipmentLists.get(EquipmentManager.HAT), item);
    } else if (itemId == ItemPool.BUDDY_BJORN) {
      AdventureResult.addResultToList(
          EquipmentManager.equipmentLists.get(EquipmentManager.CONTAINER), item);
    } else {
      int equipmentType = EquipmentManager.consumeFilterToEquipmentType(consumeType);
      if (equipmentType != -1) {
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
    int slot = EquipmentManager.itemIdToEquipmentType(newItem.getItemId());
    if (slot < 0) {
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

  public static final void setEquipment(final int slot, AdventureResult item) {
    // Variable slots do not include the fake hand
    if (slot >= EquipmentManager.equipmentLists.size()) {
      return;
    }

    AdventureResult old = EquipmentManager.getEquipment(slot);

    // Accessories are special in terms of testing for existence
    // in equipment lists -- they are all mirrors of accessories.

    switch (slot) {
      case -1: // unknown item - ignore it
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

    EquipmentManager.equipment.set(slot, item);
    LockableListFactory.setSelectedItem(EquipmentManager.equipmentLists.get(slot), item);
    EquipmentManager.historyLists.get(slot).remove(item);
    EquipmentManager.historyLists.get(slot).add(item);

    // Certain equipment slots require special update handling
    // in addition to the above code.
    if (slot == FAMILIAR && KoLCharacter.currentFamiliar != null) {
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
        case EquipmentManager.HAT -> {
          // Mad Hatrack wears hats and grants conditional skills
          AdventureResult hat = EquipmentManager.getEquipment(HAT);
          AdventureResult familiar = EquipmentManager.getEquipment(FAMILIAR);
          removed = hat.getItemId() != old.getItemId() && familiar.getItemId() != old.getItemId();
        }
        case EquipmentManager.WEAPON -> {
          // Disembodied Hand wields weapons and grants conditional skills
          AdventureResult offhand = EquipmentManager.getEquipment(OFFHAND);
          AdventureResult familiar = EquipmentManager.getEquipment(FAMILIAR);
          removed =
              offhand.getItemId() != old.getItemId() && familiar.getItemId() != old.getItemId();
        }
        case EquipmentManager.OFFHAND -> {
          // Left-Hand Man Hand wields offhand items and grants conditional skills
          AdventureResult weapon = EquipmentManager.getEquipment(WEAPON);
          AdventureResult offhand = EquipmentManager.getEquipment(OFFHAND);
          AdventureResult familiar = EquipmentManager.getEquipment(FAMILIAR);
          removed =
              weapon.getItemId() != old.getItemId()
                  && offhand.getItemId() != old.getItemId()
                  && familiar.getItemId() != old.getItemId();
        }
        case EquipmentManager.PANTS -> {
          // Fancypants Scarecrow wears pants and grants conditional skills
          AdventureResult pants = EquipmentManager.getEquipment(PANTS);
          AdventureResult familiar = EquipmentManager.getEquipment(FAMILIAR);
          removed = pants.getItemId() != old.getItemId() && familiar.getItemId() != old.getItemId();
        }
        case EquipmentManager.FAMILIAR -> {
          // Mad Hatrack wears hats and grants conditional skills
          // Disembodied Hand wields weapons and grants conditional skills
          // Fancypants Scarecrow wears pants and grants conditional skills
          AdventureResult hat = EquipmentManager.getEquipment(HAT);
          AdventureResult weapon = EquipmentManager.getEquipment(WEAPON);
          AdventureResult offhand = EquipmentManager.getEquipment(OFFHAND);
          AdventureResult pants = EquipmentManager.getEquipment(PANTS);
          removed =
              switch (consumption) {
                case HAT -> hat.getItemId() != old.getItemId();
                case WEAPON, OFFHAND -> weapon.getItemId() != old.getItemId()
                    && offhand.getItemId() != old.getItemId();
                case PANTS -> pants.getItemId() != old.getItemId();
                default -> removed;
              };
        }
        case EquipmentManager.ACCESSORY1 -> {
          AdventureResult acc2 = EquipmentManager.getEquipment(ACCESSORY2);
          AdventureResult acc3 = EquipmentManager.getEquipment(ACCESSORY3);
          removed = acc2.getItemId() != old.getItemId() && acc3.getItemId() != old.getItemId();
        }
        case EquipmentManager.ACCESSORY2 -> {
          AdventureResult acc1 = EquipmentManager.getEquipment(ACCESSORY1);
          AdventureResult acc3 = EquipmentManager.getEquipment(ACCESSORY3);
          removed = acc1.getItemId() != old.getItemId() && acc3.getItemId() != old.getItemId();
        }
        case EquipmentManager.ACCESSORY3 -> {
          AdventureResult acc1 = EquipmentManager.getEquipment(ACCESSORY1);
          AdventureResult acc2 = EquipmentManager.getEquipment(ACCESSORY2);
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

    // If we are either swapping out or in a stinky cheese item,
    // recalculate stinky cheese level.
    if (ItemDatabase.isStinkyCheeseItem(old.getItemId())
        || ItemDatabase.isStinkyCheeseItem(item.getItemId())) {
      AdventureResult weapon = EquipmentManager.getEquipment(WEAPON);
      AdventureResult offhand = EquipmentManager.getEquipment(OFFHAND);
      AdventureResult pants = EquipmentManager.getEquipment(PANTS);
      AdventureResult acc1 = EquipmentManager.getEquipment(ACCESSORY1);
      AdventureResult acc2 = EquipmentManager.getEquipment(ACCESSORY2);
      AdventureResult acc3 = EquipmentManager.getEquipment(ACCESSORY3);
      AdventureResult fam = EquipmentManager.getEquipment(FAMILIAR);

      boolean sword =
          weapon.getItemId() == ItemPool.STINKY_CHEESE_SWORD
              || offhand.getItemId() == ItemPool.STINKY_CHEESE_SWORD
              || fam.getItemId() == ItemPool.STINKY_CHEESE_SWORD;
      boolean staff = weapon.getItemId() == ItemPool.STINKY_CHEESE_STAFF;
      boolean diaper = pants.getItemId() == ItemPool.STINKY_CHEESE_DIAPER;
      boolean wheel = offhand.getItemId() == ItemPool.STINKY_CHEESE_WHEEL;
      boolean eye =
          acc1.getItemId() == ItemPool.STINKY_CHEESE_EYE
              || acc2.getItemId() == ItemPool.STINKY_CHEESE_EYE
              || acc3.getItemId() == ItemPool.STINKY_CHEESE_EYE;

      EquipmentManager.stinkyCheeseLevel =
          (sword ? 1 : 0) + (staff ? 1 : 0) + (diaper ? 1 : 0) + (wheel ? 1 : 0) + (eye ? 1 : 0);
    }

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

  public static final void removeConditionalSkills(final int slot, AdventureResult item) {
    // Certain items can be equipped either in their normal slot or
    // on a familiar. Granted skills may or may not be available.
    //
    // hat - Mad Hatrack - willowy bonnet - YES
    // weapon - Disembodied Hand - bottle rocket crossbow - YES
    // offhand - Left-Hand Man - latte lovers member's mug - YES
    // pants - Fancypants Scarecrow - crotchety pants - YES

    switch (item.getItemId()) {
      case ItemPool.BOTTLE_ROCKET:
        KoLCharacter.removeAvailableSkill(SkillPool.FIRE_RED_BOTTLE_ROCKET);
        KoLCharacter.removeAvailableSkill(SkillPool.FIRE_BLUE_BOTTLE_ROCKET);
        KoLCharacter.removeAvailableSkill(SkillPool.FIRE_ORANGE_BOTTLE_ROCKET);
        KoLCharacter.removeAvailableSkill(SkillPool.FIRE_PURPLE_BOTTLE_ROCKET);
        KoLCharacter.removeAvailableSkill(SkillPool.FIRE_BLACK_BOTTLE_ROCKET);
        break;
      case ItemPool.JEWEL_EYED_WIZARD_HAT:
        KoLCharacter.removeAvailableSkill(SkillPool.MAGIC_MISSILE);
        break;
      case ItemPool.BAKULA:
        KoLCharacter.removeAvailableSkill(SkillPool.GIVE_IN_TO_YOUR_VAMPIRIC_URGES);
        break;
      case ItemPool.JOYBUZZER:
        KoLCharacter.removeAvailableSkill(SkillPool.SHAKE_HANDS);
        break;
      case ItemPool.V_MASK:
        KoLCharacter.removeAvailableSkill(SkillPool.CREEPY_GRIN);
        break;
      case ItemPool.MAYFLY_BAIT_NECKLACE:
        KoLCharacter.removeAvailableSkill(SkillPool.MAYFLY_SWARM);
        break;
      case ItemPool.HODGMANS_PORKPIE_HAT:
      case ItemPool.HODGMANS_LOBSTERSKIN_PANTS:
      case ItemPool.HODGMANS_BOW_TIE:
        KoLCharacter.removeAvailableSkill(SkillPool.SUMMON_HOBO);
        break;
      case ItemPool.WILLOWY_BONNET:
        KoLCharacter.removeAvailableSkill(SkillPool.ROUSE_SAPLING);
        break;
      case ItemPool.SACCHARINE_MAPLE_PENDANT:
        KoLCharacter.removeAvailableSkill(SkillPool.SPRAY_SAP);
        break;
      case ItemPool.CROTCHETY_PANTS:
        KoLCharacter.removeAvailableSkill(SkillPool.PUT_DOWN_ROOTS);
        break;
      case ItemPool.FIREWORKS:
        KoLCharacter.removeAvailableSkill(SkillPool.FIRE_OFF_A_ROMAN_CANDLE);
        break;
      case ItemPool.HAIKU_KATANA:
        KoLCharacter.removeAvailableSkill(SkillPool.SPRING_RAINDROP_ATTACK);
        KoLCharacter.removeAvailableSkill(SkillPool.SUMMER_SIESTA);
        KoLCharacter.removeAvailableSkill(SkillPool.FALLING_LEAF_WHIRLWIND);
        KoLCharacter.removeAvailableSkill(SkillPool.WINTERS_BITE_TECHNIQUE);
        KoLCharacter.removeAvailableSkill(SkillPool.THE_17_CUTS);
        break;
      case ItemPool.PARASITIC_CLAW:
      case ItemPool.PARASITIC_TENTACLES:
      case ItemPool.PARASITIC_HEADGNAWER:
      case ItemPool.PARASITIC_STRANGLEWORM:
        KoLCharacter.removeAvailableSkill(SkillPool.DISARM);
        KoLCharacter.removeAvailableSkill(SkillPool.ENTANGLE);
        KoLCharacter.removeAvailableSkill(SkillPool.STRANGLE);
        break;
      case ItemPool.ELVISH_SUNGLASSES:
        KoLCharacter.removeAvailableSkill(SkillPool.PLAY_AN_ACCORDION_SOLO);
        KoLCharacter.removeAvailableSkill(SkillPool.PLAY_A_GUITAR_SOLO);
        KoLCharacter.removeAvailableSkill(SkillPool.PLAY_A_DRUM_SOLO);
        KoLCharacter.removeAvailableSkill(SkillPool.PLAY_A_FLUTE_SOLO);
        break;
      case ItemPool.BAG_O_TRICKS:
        KoLCharacter.removeAvailableSkill(SkillPool.OPEN_THE_BAG_O_TRICKS);
        break;
      case ItemPool.FOUET_DE_TORTUE_DRESSAGE:
        KoLCharacter.removeAvailableSkill(SkillPool.APPRIVOISEZ_LA_TORTUE);
        break;
      case ItemPool.RED_AND_GREEN_SWEATER:
        KoLCharacter.removeAvailableSkill(SkillPool.STATIC_SHOCK__RED_AND_GREEN_SWEATER);
        break;
      case ItemPool.STINKY_CHEESE_EYE:
        KoLCharacter.removeAvailableSkill(SkillPool.STINKEYE);
        break;
      case ItemPool.SLEDGEHAMMER_OF_THE_VAELKYR:
        KoLCharacter.removeAvailableSkill(SkillPool.BASHING_SLAM_SMASH);
        break;
      case ItemPool.FLAIL_OF_THE_SEVEN_ASPECTS:
        KoLCharacter.removeAvailableSkill(SkillPool.TURTLE_OF_SEVEN_TAILS);
        break;
      case ItemPool.WRATH_OF_THE_PASTALORDS:
        KoLCharacter.removeAvailableSkill(SkillPool.NOODLES_OF_FIRE);
        break;
      case ItemPool.WINDSOR_PAN_OF_THE_SOURCE:
        KoLCharacter.removeAvailableSkill(SkillPool.SAUCEMAGEDDON);
        break;
      case ItemPool.SEEGERS_BANJO:
        KoLCharacter.removeAvailableSkill(SkillPool.FUNK_BLUEGRASS_FUSION);
        break;
      case ItemPool.TRICKSTER_TRIKITIXA:
        KoLCharacter.removeAvailableSkill(SkillPool.EXTREME_HIGH_NOTE);
        break;
      case ItemPool.BOTTLE_OF_GOLDENSCHNOCKERED:
        KoLCharacter.removeAvailableSkill(SkillPool.GOLDENSHOWER);
        break;
      case ItemPool.SPIDER_RING:
        KoLCharacter.removeAvailableSkill(SkillPool.SHOOT_WEB);
        break;
      case ItemPool.STRESS_BALL:
        KoLCharacter.removeAvailableSkill(SkillPool.SQUEEZE_STRESS_BALL);
        break;
      case ItemPool.PATRIOT_SHIELD:
        KoLCharacter.removeAvailableSkill(SkillPool.THROW_SHIELD);
        break;
      case ItemPool.PLASTIC_VAMPIRE_FANGS:
        KoLCharacter.removeAvailableSkill(SkillPool.FEED);
        break;
      case ItemPool.LORD_FLAMEFACES_CLOAK:
        KoLCharacter.removeAvailableSkill(SkillPool.SWIRL_CLOAK);
        break;
      case ItemPool.RIGHT_BEAR_ARM:
        KoLCharacter.removeAvailableSkill(SkillPool.KODIAK_MOMENT);
        KoLCharacter.removeAvailableSkill(SkillPool.GRIZZLY_SCENE);
        KoLCharacter.removeAvailableSkill(SkillPool.BEAR_HUG);
        KoLCharacter.removeAvailableSkill(SkillPool.I_CAN_BEARLY_HEAR_YOU_OVER_THE_APPLAUSE);
        break;
      case ItemPool.LEFT_BEAR_ARM:
        KoLCharacter.removeAvailableSkill(SkillPool.BEAR_BACKRUB);
        KoLCharacter.removeAvailableSkill(SkillPool.BEARLY_LEGAL);
        KoLCharacter.removeAvailableSkill(SkillPool.BEAR_HUG);
        KoLCharacter.removeAvailableSkill(SkillPool.I_CAN_BEARLY_HEAR_YOU_OVER_THE_APPLAUSE);
        break;
      case ItemPool.ELECTRONIC_DULCIMER_PANTS:
        KoLCharacter.removeAvailableSkill(SkillPool.PLAY_HOG_FIDDLE);
        break;
      case ItemPool.HAGGIS_SOCKS:
        KoLCharacter.removeAvailableSkill(SkillPool.HAGGIS_KICK);
        break;
      case ItemPool.MARK_V_STEAM_HAT:
        KoLCharacter.removeAvailableSkill(SkillPool.FIRE_DEATH_RAY);
        break;
      case ItemPool.VIOLENCE_LENS:
        KoLCharacter.removeAvailableSkill(SkillPool.VIOLENT_GAZE);
        break;
      case ItemPool.VIOLENCE_BRAND:
        KoLCharacter.removeAvailableSkill(SkillPool.BRAND);
        break;
      case ItemPool.VIOLENCE_PANTS:
        KoLCharacter.removeAvailableSkill(SkillPool.MOSH);
        break;
      case ItemPool.VIOLENCE_STOMPERS:
        KoLCharacter.removeAvailableSkill(SkillPool.STOMP_ASS);
        break;
      case ItemPool.HATRED_LENS:
        KoLCharacter.removeAvailableSkill(SkillPool.HATEFUL_GAZE);
        break;
      case ItemPool.HATRED_STONE:
        KoLCharacter.removeAvailableSkill(SkillPool.CHILLING_GRIP);
        break;
      case ItemPool.HATRED_PANTS:
        KoLCharacter.removeAvailableSkill(SkillPool.STATIC_SHOCK__PANTALOONS_OF_HATRED);
        break;
      case ItemPool.HATRED_GIRDLE:
        KoLCharacter.removeAvailableSkill(SkillPool.TIGHTEN_GIRDLE);
        break;
      case ItemPool.ANGER_BLASTER:
        KoLCharacter.removeAvailableSkill(SkillPool.RAGE_FLAME);
        break;
      case ItemPool.DOUBT_CANNON:
        KoLCharacter.removeAvailableSkill(SkillPool.DOUBT_SHACKLES);
        break;
      case ItemPool.FEAR_CONDENSER:
        KoLCharacter.removeAvailableSkill(SkillPool.FEAR_VAPOR);
        break;
      case ItemPool.REGRET_HOSE:
        KoLCharacter.removeAvailableSkill(SkillPool.TEAR_WAVE);
        break;
      case ItemPool.GREAT_WOLFS_LEFT_PAW:
      case ItemPool.GREAT_WOLFS_RIGHT_PAW:
        KoLCharacter.removeAvailableSkill(SkillPool.GREAT_SLASH);
        break;
      case ItemPool.GREAT_WOLFS_ROCKET_LAUNCHER:
        KoLCharacter.removeAvailableSkill(SkillPool.FIRE_ROCKET);
        break;
      case ItemPool.MAYOR_GHOSTS_GAVEL:
        KoLCharacter.removeAvailableSkill(SkillPool.HAMMER_GHOST);
        break;
      case ItemPool.PANTSGIVING:
        KoLCharacter.removeAvailableSkill(SkillPool.TALK_ABOUT_POLITICS);
        KoLCharacter.removeAvailableSkill(SkillPool.POCKET_CRUMBS);
        KoLCharacter.removeAvailableSkill(SkillPool.AIR_DIRTY_LAUNDRY);
        break;
      case ItemPool.WARBEAR_OIL_PAN:
        if (KoLCharacter.isSauceror()) {
          KoLCharacter.removeAvailableSkill(SkillPool.SPRAY_HOT_GREASE);
        }
        break;
      case ItemPool.WOLF_WHISTLE:
        KoLCharacter.removeAvailableSkill(SkillPool.BLOW_WOLF_WHISTLE);
        break;
      case ItemPool.TOMMY_GUN:
        KoLCharacter.removeAvailableSkill(SkillPool.UNLOAD_TOMMY_GUN);
        break;
      case ItemPool.CREEPY_VOICE_BOX:
        KoLCharacter.removeAvailableSkill(SkillPool.PULL_VOICE_BOX_STRING);
        break;
      case ItemPool.COAL_SHOVEL:
        KoLCharacter.removeAvailableSkill(SkillPool.SHOVEL_HOT_COAL);
        break;
      case ItemPool.SPACE_HEATER:
        KoLCharacter.removeAvailableSkill(SkillPool.HEAT_SPACE);
        break;
      case ItemPool.CAP_GUN:
        KoLCharacter.removeAvailableSkill(SkillPool.BANG_BANG_BANG_BANG);
        break;
      case ItemPool.THORS_PLIERS:
        KoLCharacter.removeAvailableSkill(SkillPool.PLY_REALITY);
        break;
      case ItemPool.CUDDLY_TEDDY_BEAR:
        KoLCharacter.removeAvailableSkill(SkillPool.OVERLOAD_TEDDY_BEAR);
        break;
      case ItemPool.TOY_CRIMBOT_FACE:
        KoLCharacter.removeAvailableSkill(SkillPool.LIGHT);
        break;
      case ItemPool.TOY_CRIMBOT_GLOVE:
        KoLCharacter.removeAvailableSkill(SkillPool.ZAP);
        break;
      case ItemPool.TOY_CRIMBOT_FIST:
        KoLCharacter.removeAvailableSkill(SkillPool.POW);
        break;
      case ItemPool.TOY_CRIMBOT_LEGS:
        KoLCharacter.removeAvailableSkill(SkillPool.BURN);
        break;
      case ItemPool.RING_OF_TELLING_SKELETONS_WHAT_TO_DO:
        KoLCharacter.removeAvailableSkill(SkillPool.TELL_A_SKELETON_WHAT_TO_DO);
        KoLCharacter.removeAvailableSkill(SkillPool.TELL_THIS_SKELETON_WHAT_TO_DO);
        break;
      case ItemPool.SEWAGE_CLOGGED_PISTOL:
        KoLCharacter.removeAvailableSkill(SkillPool.FIRE_SEWAGE_PISTOL);
        break;
      case ItemPool.LOTS_ENGAGEMENT_RING:
        KoLCharacter.removeAvailableSkill(SkillPool.PROPOSE_TO_YOUR_OPPONENT);
        break;
      case ItemPool.PROTON_ACCELERATOR:
        KoLCharacter.removeAvailableSkill(SkillPool.SHOOT_GHOST);
        KoLCharacter.removeAvailableSkill(SkillPool.TRAP_GHOST);
        break;
      case ItemPool.STANDARDS_AND_PRACTICES:
        KoLCharacter.removeAvailableSkill(SkillPool.CENSORIOUS_LECTURE);
        break;
      case ItemPool.KREMLIN_BRIEFCASE:
        KoLCharacter.removeAvailableSkill(SkillPool.KGB_TRANQUILIZER_DART);
        break;
      case ItemPool.GABARDINE_GIRDLE:
        KoLCharacter.removeAvailableSkill(SkillPool.UNLEASH_DISCO_PUDGE);
        break;
      case ItemPool.PAINT_PALETTE:
        KoLCharacter.removeAvailableSkill(SkillPool.PAINT_JOB);
        break;
      case ItemPool.PARTYCRASHER:
        KoLCharacter.removeAvailableSkill(SkillPool.PARTY_CRASH);
        break;
      case ItemPool.LATTE_MUG:
        KoLCharacter.removeAvailableSkill(SkillPool.THROW_LATTE);
        KoLCharacter.removeAvailableSkill(SkillPool.OFFER_LATTE);
        KoLCharacter.removeAvailableSkill(SkillPool.GULP_LATTE);
        break;
      case ItemPool.DOCTOR_BAG:
        KoLCharacter.removeAvailableSkill(SkillPool.OTOSCOPE);
        KoLCharacter.removeAvailableSkill(SkillPool.REFLEX_HAMMER);
        KoLCharacter.removeAvailableSkill(SkillPool.CHEST_X_RAY);
        break;
      case ItemPool.FOURTH_SABER:
        KoLCharacter.removeAvailableSkill(SkillPool.USE_THE_FORCE);
        break;
      case ItemPool.HEWN_MOON_RUNE_SPOON:
        if (KoLCharacter.isMuscleClass()) {
          KoLCharacter.removeAvailableSkill(SkillPool.DRAGOON_PLATOON);
        } else if (KoLCharacter.isMysticalityClass()) {
          KoLCharacter.removeAvailableSkill(SkillPool.SPITTOON_MONSOON);
        } else if (KoLCharacter.isMoxieClass()) {
          KoLCharacter.removeAvailableSkill(SkillPool.FESTOON_BUFFOON);
        }
        break;
      case ItemPool.BEACH_COMB:
        KoLCharacter.removeAvailableSkill(SkillPool.BEACH_COMBO);
        break;
      case ItemPool.POWERFUL_GLOVE:
        // These are only the combat skills, we make the noncombat skills always available
        KoLCharacter.removeAvailableSkill(SkillPool.REPLACE_ENEMY);
        KoLCharacter.removeAvailableSkill(SkillPool.SHRINK_ENEMY);
        break;
      case ItemPool.RED_PLUMBERS_BOOTS:
        KoLCharacter.removeAvailableSkill(SkillPool.PLUMBER_JUMP);
        break;
      case ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE:
        ItemDatabase.setCapeSkills();
        break;
      case ItemPool.BLART:
        KoLCharacter.removeAvailableSkill(SkillPool.BLART_SPRAY_NARROW);
        KoLCharacter.removeAvailableSkill(SkillPool.BLART_SPRAY_MEDIUM);
        KoLCharacter.removeAvailableSkill(SkillPool.BLART_SPRAY_WIDE);
        break;
      case ItemPool.INDUSTRIAL_FIRE_EXTINGUISHER:
        KoLCharacter.removeAvailableSkill(SkillPool.FIRE_EXTINGUISHER__FOAM_EM_UP);
        KoLCharacter.removeAvailableSkill(SkillPool.FIRE_EXTINGUISHER__POLAR_VORTEX);
        KoLCharacter.removeAvailableSkill(SkillPool.FIRE_EXTINGUISHER__FOAM_YOURSELF);
        KoLCharacter.removeAvailableSkill(SkillPool.FIRE_EXTINGUISHER__BLAST_THE_AREA);
        KoLCharacter.removeAvailableSkill(SkillPool.FIRE_EXTINGUISHER__ZONE_SPECIFIC);
        break;
      case ItemPool.DESIGNER_SWEATPANTS:
        // These are only the combat skills, we make the noncombat skills always available
        KoLCharacter.removeAvailableSkill(SkillPool.SWEAT_FLICK);
        KoLCharacter.removeAvailableSkill(SkillPool.SWEAT_FLOOD);
        KoLCharacter.removeAvailableSkill(SkillPool.SWEAT_SPRAY);
        KoLCharacter.removeAvailableSkill(SkillPool.SWEAT_SIP);
        break;
    }
  }

  public static final void addConditionalSkills(final int slot, AdventureResult item) {
    // Certain items can be equipped either in their normal slot or
    // on a familiar. Granted skills may or may not be available.
    //
    // hat - Mad Hatrack - willowy bonnet - YES
    // weapon - Disembodied Hand - bottle rocket crossbow - YES
    // offhand - Left-Hand Man - latte lovers member's mug - YES
    // pants - Fancypants Scarecrow - crotchety pants - YES

    int id = item.getItemId();

    // If we are equipping a new sword or gun we may be changing the capabilities of the retrocape
    if (slot == EquipmentManager.WEAPON) {
      if (EquipmentDatabase.isSword(id)
          || EquipmentDatabase.isGun(id)
          || EquipmentDatabase.isPistol(id)
          || EquipmentDatabase.isRifle(id)) {
        ItemDatabase.setCapeSkills();
      }
    }

    switch (id) {
      case ItemPool.BOTTLE_ROCKET:
        KoLCharacter.addAvailableSkill(SkillPool.FIRE_RED_BOTTLE_ROCKET);
        KoLCharacter.addAvailableSkill(SkillPool.FIRE_BLUE_BOTTLE_ROCKET);
        KoLCharacter.addAvailableSkill(SkillPool.FIRE_ORANGE_BOTTLE_ROCKET);
        KoLCharacter.addAvailableSkill(SkillPool.FIRE_PURPLE_BOTTLE_ROCKET);
        KoLCharacter.addAvailableSkill(SkillPool.FIRE_BLACK_BOTTLE_ROCKET);
        break;
      case ItemPool.JEWEL_EYED_WIZARD_HAT:
        KoLCharacter.addAvailableSkill(SkillPool.MAGIC_MISSILE);
        break;
      case ItemPool.BAKULA:
        KoLCharacter.addAvailableSkill(SkillPool.GIVE_IN_TO_YOUR_VAMPIRIC_URGES);
        break;
      case ItemPool.JOYBUZZER:
        KoLCharacter.addAvailableSkill(SkillPool.SHAKE_HANDS);
        break;
      case ItemPool.V_MASK:
        KoLCharacter.addAvailableSkill(SkillPool.CREEPY_GRIN);
        break;
      case ItemPool.MAYFLY_BAIT_NECKLACE:
        KoLCharacter.addAvailableSkill(SkillPool.MAYFLY_SWARM);
        break;
      case ItemPool.HODGMANS_PORKPIE_HAT:
      case ItemPool.HODGMANS_LOBSTERSKIN_PANTS:
      case ItemPool.HODGMANS_BOW_TIE:
        if (EquipmentManager.isWearingOutfit(OutfitPool.HODGMANS_REGAL_FRIPPERY)) {
          KoLCharacter.addAvailableSkill(SkillPool.SUMMON_HOBO);
        }
        break;
      case ItemPool.WILLOWY_BONNET:
        KoLCharacter.addAvailableSkill(SkillPool.ROUSE_SAPLING);
        break;
      case ItemPool.SACCHARINE_MAPLE_PENDANT:
        KoLCharacter.addAvailableSkill(SkillPool.SPRAY_SAP);
        break;
      case ItemPool.CROTCHETY_PANTS:
        KoLCharacter.addAvailableSkill(SkillPool.PUT_DOWN_ROOTS);
        break;
      case ItemPool.FIREWORKS:
        KoLCharacter.addAvailableSkill(SkillPool.FIRE_OFF_A_ROMAN_CANDLE);
        break;
      case ItemPool.HAIKU_KATANA:
        KoLCharacter.addAvailableSkill(SkillPool.SPRING_RAINDROP_ATTACK);
        KoLCharacter.addAvailableSkill(SkillPool.SUMMER_SIESTA);
        KoLCharacter.addAvailableSkill(SkillPool.FALLING_LEAF_WHIRLWIND);
        KoLCharacter.addAvailableSkill(SkillPool.WINTERS_BITE_TECHNIQUE);
        KoLCharacter.addAvailableSkill(SkillPool.THE_17_CUTS);
        break;
      case ItemPool.PARASITIC_CLAW:
      case ItemPool.PARASITIC_TENTACLES:
      case ItemPool.PARASITIC_HEADGNAWER:
      case ItemPool.PARASITIC_STRANGLEWORM:
        if (EquipmentManager.isWearingOutfit(OutfitPool.MUTANT_COUTURE)) {
          KoLCharacter.addAvailableSkill(SkillPool.DISARM);
          KoLCharacter.addAvailableSkill(SkillPool.ENTANGLE);
          KoLCharacter.addAvailableSkill(SkillPool.STRANGLE);
        }
        break;
      case ItemPool.ELVISH_SUNGLASSES:
        KoLCharacter.addAvailableSkill(SkillPool.PLAY_AN_ACCORDION_SOLO);
        KoLCharacter.addAvailableSkill(SkillPool.PLAY_A_GUITAR_SOLO);
        KoLCharacter.addAvailableSkill(SkillPool.PLAY_A_DRUM_SOLO);
        KoLCharacter.addAvailableSkill(SkillPool.PLAY_A_FLUTE_SOLO);
        break;
      case ItemPool.BAG_O_TRICKS:
        KoLCharacter.addAvailableSkill(SkillPool.OPEN_THE_BAG_O_TRICKS);
        break;
      case ItemPool.FOUET_DE_TORTUE_DRESSAGE:
        KoLCharacter.addAvailableSkill(SkillPool.APPRIVOISEZ_LA_TORTUE);
        break;
      case ItemPool.RED_AND_GREEN_SWEATER:
        KoLCharacter.addAvailableSkill(SkillPool.STATIC_SHOCK__RED_AND_GREEN_SWEATER);
        break;
      case ItemPool.STINKY_CHEESE_EYE:
        KoLCharacter.addAvailableSkill(SkillPool.STINKEYE);
        break;
      case ItemPool.SLEDGEHAMMER_OF_THE_VAELKYR:
        KoLCharacter.addAvailableSkill(SkillPool.BASHING_SLAM_SMASH);
        break;
      case ItemPool.FLAIL_OF_THE_SEVEN_ASPECTS:
        KoLCharacter.addAvailableSkill(SkillPool.TURTLE_OF_SEVEN_TAILS);
        break;
      case ItemPool.WRATH_OF_THE_PASTALORDS:
        KoLCharacter.addAvailableSkill(SkillPool.NOODLES_OF_FIRE);
        break;
      case ItemPool.WINDSOR_PAN_OF_THE_SOURCE:
        KoLCharacter.addAvailableSkill(SkillPool.SAUCEMAGEDDON);
        break;
      case ItemPool.SEEGERS_BANJO:
        KoLCharacter.addAvailableSkill(SkillPool.FUNK_BLUEGRASS_FUSION);
        break;
      case ItemPool.TRICKSTER_TRIKITIXA:
        KoLCharacter.addAvailableSkill(SkillPool.EXTREME_HIGH_NOTE);
        break;
      case ItemPool.BOTTLE_OF_GOLDENSCHNOCKERED:
        KoLCharacter.addAvailableSkill(SkillPool.GOLDENSHOWER);
        break;
      case ItemPool.SPIDER_RING:
        KoLCharacter.addAvailableSkill(SkillPool.SHOOT_WEB);
        break;
      case ItemPool.STRESS_BALL:
        KoLCharacter.addAvailableSkill(SkillPool.SQUEEZE_STRESS_BALL);
        break;
      case ItemPool.PATRIOT_SHIELD:
        KoLCharacter.addAvailableSkill(SkillPool.THROW_SHIELD);
        break;
      case ItemPool.PLASTIC_VAMPIRE_FANGS:
        KoLCharacter.addAvailableSkill(SkillPool.FEED);
        break;
      case ItemPool.LORD_FLAMEFACES_CLOAK:
        KoLCharacter.addAvailableSkill(SkillPool.SWIRL_CLOAK);
        break;
      case ItemPool.RIGHT_BEAR_ARM:
        KoLCharacter.addAvailableSkill(SkillPool.KODIAK_MOMENT);
        KoLCharacter.addAvailableSkill(SkillPool.GRIZZLY_SCENE);
        if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.LEFT_BEAR_ARM, 1))) {
          KoLCharacter.addAvailableSkill(SkillPool.BEAR_HUG);
          KoLCharacter.addAvailableSkill(SkillPool.I_CAN_BEARLY_HEAR_YOU_OVER_THE_APPLAUSE);
        }
        break;
      case ItemPool.LEFT_BEAR_ARM:
        KoLCharacter.addAvailableSkill(SkillPool.BEAR_BACKRUB);
        KoLCharacter.addAvailableSkill(SkillPool.BEARLY_LEGAL);
        if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.RIGHT_BEAR_ARM, 1))) {
          KoLCharacter.addAvailableSkill(SkillPool.BEAR_HUG);
          KoLCharacter.addAvailableSkill(SkillPool.I_CAN_BEARLY_HEAR_YOU_OVER_THE_APPLAUSE);
        }
        break;
      case ItemPool.ELECTRONIC_DULCIMER_PANTS:
        KoLCharacter.addAvailableSkill(SkillPool.PLAY_HOG_FIDDLE);
        break;
      case ItemPool.HAGGIS_SOCKS:
        KoLCharacter.addAvailableSkill(SkillPool.HAGGIS_KICK);
        break;
      case ItemPool.MARK_V_STEAM_HAT:
        KoLCharacter.addAvailableSkill(SkillPool.FIRE_DEATH_RAY);
        break;
      case ItemPool.VIOLENCE_LENS:
        KoLCharacter.addAvailableSkill(SkillPool.VIOLENT_GAZE);
        break;
      case ItemPool.VIOLENCE_BRAND:
        KoLCharacter.addAvailableSkill(SkillPool.BRAND);
        break;
      case ItemPool.VIOLENCE_PANTS:
        KoLCharacter.addAvailableSkill(SkillPool.MOSH);
        break;
      case ItemPool.VIOLENCE_STOMPERS:
        KoLCharacter.addAvailableSkill(SkillPool.STOMP_ASS);
        break;
      case ItemPool.HATRED_LENS:
        KoLCharacter.addAvailableSkill(SkillPool.HATEFUL_GAZE);
        break;
      case ItemPool.HATRED_STONE:
        KoLCharacter.addAvailableSkill(SkillPool.CHILLING_GRIP);
        break;
      case ItemPool.HATRED_PANTS:
        KoLCharacter.addAvailableSkill(SkillPool.STATIC_SHOCK__PANTALOONS_OF_HATRED);
        break;
      case ItemPool.HATRED_GIRDLE:
        KoLCharacter.addAvailableSkill(SkillPool.TIGHTEN_GIRDLE);
        break;
      case ItemPool.ANGER_BLASTER:
        KoLCharacter.addAvailableSkill(SkillPool.RAGE_FLAME);
        break;
      case ItemPool.DOUBT_CANNON:
        KoLCharacter.addAvailableSkill(SkillPool.DOUBT_SHACKLES);
        break;
      case ItemPool.FEAR_CONDENSER:
        KoLCharacter.addAvailableSkill(SkillPool.FEAR_VAPOR);
        break;
      case ItemPool.REGRET_HOSE:
        KoLCharacter.addAvailableSkill(SkillPool.TEAR_WAVE);
        break;
      case ItemPool.GREAT_WOLFS_LEFT_PAW:
      case ItemPool.GREAT_WOLFS_RIGHT_PAW:
        KoLCharacter.addAvailableSkill(SkillPool.GREAT_SLASH);
        break;
      case ItemPool.GREAT_WOLFS_ROCKET_LAUNCHER:
        KoLCharacter.addAvailableSkill(SkillPool.FIRE_ROCKET);
        break;
      case ItemPool.MAYOR_GHOSTS_GAVEL:
        KoLCharacter.addAvailableSkill(SkillPool.HAMMER_GHOST);
        break;
      case ItemPool.PANTSGIVING:
        KoLCharacter.addAvailableSkill(SkillPool.TALK_ABOUT_POLITICS);
        KoLCharacter.addAvailableSkill(SkillPool.POCKET_CRUMBS);
        KoLCharacter.addAvailableSkill(SkillPool.AIR_DIRTY_LAUNDRY);
        break;
      case ItemPool.WARBEAR_OIL_PAN:
        if (KoLCharacter.isSauceror()) {
          KoLCharacter.addAvailableSkill(SkillPool.SPRAY_HOT_GREASE);
        }
        break;
      case ItemPool.WOLF_WHISTLE:
        KoLCharacter.addAvailableSkill(SkillPool.BLOW_WOLF_WHISTLE);
        break;
      case ItemPool.TOMMY_GUN:
        KoLCharacter.addAvailableSkill(SkillPool.UNLOAD_TOMMY_GUN);
        break;
      case ItemPool.CREEPY_VOICE_BOX:
        KoLCharacter.addAvailableSkill(SkillPool.PULL_VOICE_BOX_STRING);
        break;
      case ItemPool.COAL_SHOVEL:
        KoLCharacter.addAvailableSkill(SkillPool.SHOVEL_HOT_COAL);
        break;
      case ItemPool.SPACE_HEATER:
        KoLCharacter.addAvailableSkill(SkillPool.HEAT_SPACE);
        break;
      case ItemPool.CAP_GUN:
        KoLCharacter.addAvailableSkill(SkillPool.BANG_BANG_BANG_BANG);
        break;
      case ItemPool.THORS_PLIERS:
        KoLCharacter.addAvailableSkill(SkillPool.PLY_REALITY);
        break;
      case ItemPool.CUDDLY_TEDDY_BEAR:
        KoLCharacter.addAvailableSkill(SkillPool.OVERLOAD_TEDDY_BEAR);
        break;
      case ItemPool.TOY_CRIMBOT_FACE:
        KoLCharacter.addAvailableSkill(SkillPool.LIGHT);
        break;
      case ItemPool.TOY_CRIMBOT_GLOVE:
        KoLCharacter.addAvailableSkill(SkillPool.ZAP);
        break;
      case ItemPool.TOY_CRIMBOT_FIST:
        KoLCharacter.addAvailableSkill(SkillPool.POW);
        break;
      case ItemPool.TOY_CRIMBOT_LEGS:
        KoLCharacter.addAvailableSkill(SkillPool.BURN);
        break;
      case ItemPool.RING_OF_TELLING_SKELETONS_WHAT_TO_DO:
        KoLCharacter.addAvailableSkill(SkillPool.TELL_A_SKELETON_WHAT_TO_DO);
        KoLCharacter.addAvailableSkill(SkillPool.TELL_THIS_SKELETON_WHAT_TO_DO);
        break;
      case ItemPool.SEWAGE_CLOGGED_PISTOL:
        KoLCharacter.addAvailableSkill(SkillPool.FIRE_SEWAGE_PISTOL);
        break;
      case ItemPool.LOTS_ENGAGEMENT_RING:
        KoLCharacter.addAvailableSkill(SkillPool.PROPOSE_TO_YOUR_OPPONENT);
        break;
      case ItemPool.PROTON_ACCELERATOR:
        KoLCharacter.addAvailableSkill(SkillPool.SHOOT_GHOST);
        KoLCharacter.addAvailableSkill(SkillPool.TRAP_GHOST);
        break;
      case ItemPool.STANDARDS_AND_PRACTICES:
        KoLCharacter.addAvailableSkill(SkillPool.CENSORIOUS_LECTURE);
        break;
      case ItemPool.KREMLIN_BRIEFCASE:
        KoLCharacter.addAvailableSkill(SkillPool.KGB_TRANQUILIZER_DART);
        break;
      case ItemPool.GABARDINE_GIRDLE:
        KoLCharacter.addAvailableSkill(SkillPool.UNLEASH_DISCO_PUDGE);
        break;
      case ItemPool.PAINT_PALETTE:
        KoLCharacter.addAvailableSkill(SkillPool.PAINT_JOB);
        break;
      case ItemPool.PARTYCRASHER:
        KoLCharacter.addAvailableSkill(SkillPool.PARTY_CRASH);
        break;
      case ItemPool.LATTE_MUG:
        KoLCharacter.addAvailableSkill(SkillPool.THROW_LATTE);
        KoLCharacter.addAvailableSkill(SkillPool.OFFER_LATTE);
        KoLCharacter.addAvailableSkill(SkillPool.GULP_LATTE);
        break;
      case ItemPool.DOCTOR_BAG:
        KoLCharacter.addAvailableSkill(SkillPool.OTOSCOPE);
        KoLCharacter.addAvailableSkill(SkillPool.REFLEX_HAMMER);
        KoLCharacter.addAvailableSkill(SkillPool.CHEST_X_RAY);
        break;
      case ItemPool.FOURTH_SABER:
        KoLCharacter.addAvailableSkill(SkillPool.USE_THE_FORCE);
        break;
      case ItemPool.HEWN_MOON_RUNE_SPOON:
        if (KoLCharacter.isMuscleClass()) {
          KoLCharacter.addAvailableSkill(SkillPool.DRAGOON_PLATOON);
        } else if (KoLCharacter.isMysticalityClass()) {
          KoLCharacter.addAvailableSkill(SkillPool.SPITTOON_MONSOON);
        } else if (KoLCharacter.isMoxieClass()) {
          KoLCharacter.addAvailableSkill(SkillPool.FESTOON_BUFFOON);
        }
        break;
      case ItemPool.BEACH_COMB:
        KoLCharacter.addAvailableSkill(SkillPool.BEACH_COMBO);
        break;
      case ItemPool.POWERFUL_GLOVE:
        // *** Special case: the buffs are always available
        // These are only the combat skills, we make the noncombat skills always available
        KoLCharacter.addAvailableSkill(SkillPool.REPLACE_ENEMY);
        KoLCharacter.addAvailableSkill(SkillPool.SHRINK_ENEMY);
        break;
      case ItemPool.RED_PLUMBERS_BOOTS:
        KoLCharacter.addAvailableSkill(SkillPool.PLUMBER_JUMP);
        break;
      case ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE:
        ItemDatabase.setCapeSkills();
        break;
      case ItemPool.BLART:
        KoLCharacter.addAvailableSkill(SkillPool.BLART_SPRAY_NARROW);
        KoLCharacter.addAvailableSkill(SkillPool.BLART_SPRAY_MEDIUM);
        KoLCharacter.addAvailableSkill(SkillPool.BLART_SPRAY_WIDE);
        break;
      case ItemPool.INDUSTRIAL_FIRE_EXTINGUISHER:
        KoLCharacter.addAvailableSkill(SkillPool.FIRE_EXTINGUISHER__FOAM_EM_UP);
        KoLCharacter.addAvailableSkill(SkillPool.FIRE_EXTINGUISHER__POLAR_VORTEX);
        KoLCharacter.addAvailableSkill(SkillPool.FIRE_EXTINGUISHER__FOAM_YOURSELF);
        KoLCharacter.addAvailableSkill(SkillPool.FIRE_EXTINGUISHER__BLAST_THE_AREA);
        KoLCharacter.addAvailableSkill(SkillPool.FIRE_EXTINGUISHER__ZONE_SPECIFIC);
        break;
      case ItemPool.DESIGNER_SWEATPANTS:
        // *** Special case: the buffs are always available
        // These are only the combat skills, we make the noncombat skills always available
        KoLCharacter.addAvailableSkill(SkillPool.SWEAT_FLICK);
        KoLCharacter.addAvailableSkill(SkillPool.SWEAT_FLOOD);
        KoLCharacter.addAvailableSkill(SkillPool.SWEAT_SPRAY);
        KoLCharacter.addAvailableSkill(SkillPool.SWEAT_SIP);
        break;
    }
  }

  public static final void transformEquipment(AdventureResult before, AdventureResult after) {
    SpecialOutfit.replaceEquipment(before, after);
    for (int slot = 0; slot <= EquipmentManager.FAMILIAR; ++slot) {
      if (KoLCharacter.hasEquipped(before, slot)) {
        EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);
        // FamiliarData.setItem moved the current
        // familiar item to inventory when we
        // unequipped it above
        if (slot != EquipmentManager.FAMILIAR) {
          AdventureResult.addResultToList(KoLConstants.inventory, before);
        }
        ResultProcessor.processResult(before.getInstance(-1));
        EquipmentManager.setEquipment(slot, after);
        return;
      }
    }
    RequestLogger.printLine("(unable to determine slot of transformed equipment)");
  }

  public static final int removeEquipment(final int itemId) {
    return EquipmentManager.removeEquipment(ItemPool.get(itemId, 1));
  }

  public static final int removeEquipment(final AdventureResult item) {
    for (int slot = 0; slot <= EquipmentManager.FAMILIAR; ++slot) {
      if (EquipmentManager.removeEquipment(item, slot)) {
        return slot;
      }
    }
    return -1;
  }

  public static final boolean removeEquipment(final AdventureResult item, final int slot) {
    if (!KoLCharacter.hasEquipped(item, slot)) {
      return false;
    }

    EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);

    // FamiliarData.setItem moved the current familiar item to
    // inventory when we unequipped it above
    if (slot != EquipmentManager.FAMILIAR) {
      AdventureResult.addResultToList(KoLConstants.inventory, item);
    }

    return true;
  }

  public static final void removeAllEquipment() {
    for (int slot = 0; slot <= EquipmentManager.FAMILIAR; ++slot) {
      AdventureResult item = EquipmentManager.getEquipment(slot);
      if (!item.equals(EquipmentRequest.UNEQUIP)) {
        EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);
        // FamiliarData.setItem moved the current familiar item to
        // inventory when we unequipped it above
        if (slot != EquipmentManager.FAMILIAR) {
          AdventureResult.addResultToList(KoLConstants.inventory, item);
        }
      }
    }
  }

  public static final int discardEquipment(final int itemId) {
    return EquipmentManager.discardEquipment(itemId, true);
  }

  public static final int discardEquipment(final int itemId, boolean deleteFromCheckpoints) {
    return EquipmentManager.discardEquipment(ItemPool.get(itemId, 1), deleteFromCheckpoints);
  }

  public static final int discardEquipment(final AdventureResult item) {
    return EquipmentManager.discardEquipment(item, true);
  }

  public static final int discardEquipment(
      final AdventureResult item, boolean deleteFromCheckpoints) {
    if (deleteFromCheckpoints) {
      SpecialOutfit.forgetEquipment(item);
    }
    int slot = EquipmentManager.removeEquipment(item);
    if (slot != -1) {
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
        && !EquipmentManager.removeEquipment(item, EquipmentManager.OFFHAND)) {
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
          ItemPool.SUGAR_SHORTS -> Preferences.setInteger("sugarCounter" + itemId, 0);

        // Breaking cozy equipment resets cozy counter
      case ItemPool.COZY_SCIMITAR, ItemPool.COZY_STAFF, ItemPool.COZY_BAZOOKA -> Preferences
          .setInteger("cozyCounter" + itemId, 0);
    }

    // Discard the item, but do not clear it from outfit checkpoints yet.
    int slot = EquipmentManager.discardEquipment(itemId, false);
    if (slot == -1) {
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
          || (slot == EquipmentManager.FAMILIAR && !KoLCharacter.getFamiliar().canEquip(prev))) {
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

  public static final void checkFamiliar(final int slot) {
    switch (KoLCharacter.getFamiliar().getId()) {
      case FamiliarPool.HATRACK:
        if (slot == EquipmentManager.HAT) {
          EquipmentManager.updateEquipmentList(EquipmentManager.FAMILIAR);
        } else if (slot == EquipmentManager.FAMILIAR) {
          EquipmentManager.updateEquipmentList(EquipmentManager.HAT);
        }
        break;

      case FamiliarPool.HAND:
        if (slot == EquipmentManager.WEAPON || slot == EquipmentManager.OFFHAND) {
          EquipmentManager.updateEquipmentList(EquipmentManager.FAMILIAR);
        } else if (slot == EquipmentManager.FAMILIAR) {
          EquipmentManager.updateEquipmentList(EquipmentManager.WEAPON);
          EquipmentManager.updateEquipmentList(EquipmentManager.OFFHAND);
        }
        break;

      case FamiliarPool.LEFT_HAND:
        if (slot == EquipmentManager.OFFHAND) {
          EquipmentManager.updateEquipmentList(EquipmentManager.FAMILIAR);
        } else if (slot == EquipmentManager.FAMILIAR) {
          EquipmentManager.updateEquipmentList(EquipmentManager.OFFHAND);
        }
        break;

      case FamiliarPool.SCARECROW:
        if (slot == EquipmentManager.PANTS) {
          EquipmentManager.updateEquipmentList(EquipmentManager.FAMILIAR);
        } else if (slot == EquipmentManager.FAMILIAR) {
          EquipmentManager.updateEquipmentList(EquipmentManager.PANTS);
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
   * @param equipment All of the available equipment, stored in an array index by the constants
   */
  public static final void setEquipment(final AdventureResult[] equipment) {
    // Sanity check: must set ALL equipment slots

    if (equipment.length < EquipmentManager.SLOTS) {
      StaticEntity.printStackTrace(
          "Equipment array slot mismatch: "
              + EquipmentManager.SLOTS
              + " expected, "
              + equipment.length
              + " provided.");
      return;
    }

    // Defer updating so that we don't regenerate every GearChangeFrame list once for each slot.
    GearChangePanel.deferUpdate();
    for (int i = 0; i < EquipmentManager.ALL_SLOTS && i < equipment.length; ++i) {
      if (equipment[i] == null) {
      } else if (equipment[i].equals(EquipmentRequest.UNEQUIP)) {
        setEquipment(i, EquipmentRequest.UNEQUIP);
      } else {
        setEquipment(i, equipment[i]);
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
    AdventureResult mainhand = EquipmentManager.equipment.get(EquipmentManager.WEAPON);
    AdventureResult offhand = EquipmentManager.equipment.get(EquipmentManager.OFFHAND);

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
    return EquipmentManager.stinkyCheeseLevel;
  }

  /**
   * Accessor method to retrieve the name of a piece of equipment
   *
   * @param type the type of equipment
   * @return The name of the equipment, <code>none</code> if no such item exists
   */
  public static final AdventureResult getEquipment(final int type) {
    if (type == EquipmentManager.FAMILIAR) {
      return getFamiliarItem();
    }

    if (type >= 0 && type < equipment.size()) {
      return equipment.get(type);
    }

    return EquipmentRequest.UNEQUIP;
  }

  public static final int getTurns(int slot) {
    return EquipmentManager.turnsRemaining[slot - EquipmentManager.STICKER1];
  }

  public static final void setTurns(int slot, int minTurns, int maxTurns) {
    int curr = EquipmentManager.turnsRemaining[slot - EquipmentManager.STICKER1];
    if (curr > maxTurns) {
      curr = maxTurns;
    }
    if (curr < minTurns) {
      curr = minTurns;
    }
    EquipmentManager.turnsRemaining[slot - EquipmentManager.STICKER1] = curr;
    GearChangePanel.updateStickers(
        EquipmentManager.turnsRemaining[0],
        EquipmentManager.turnsRemaining[1],
        EquipmentManager.turnsRemaining[2]);
  }

  public static final boolean isStickerWeapon(AdventureResult item) {
    return item != null && isStickerWeapon(item.getItemId());
  }

  public static final boolean isStickerWeapon(int itemId) {
    return itemId == ItemPool.STICKER_SWORD || itemId == ItemPool.STICKER_CROSSBOW;
  }

  public static final boolean usingStickerWeapon() {
    return isStickerWeapon(getEquipment(EquipmentManager.WEAPON))
        || isStickerWeapon(getEquipment(EquipmentManager.OFFHAND))
        || isStickerWeapon(getEquipment(EquipmentManager.FAMILIAR));
  }

  public static final boolean usingStickerWeapon(AdventureResult[] equipment) {
    return isStickerWeapon(equipment[EquipmentManager.WEAPON])
        || isStickerWeapon(equipment[EquipmentManager.OFFHAND])
        || isStickerWeapon(equipment[EquipmentManager.FAMILIAR]);
  }

  public static final boolean hasStickerWeapon() {
    return EquipmentManager.usingStickerWeapon()
        || InventoryManager.hasItem(ItemPool.STICKER_SWORD)
        || InventoryManager.hasItem(ItemPool.STICKER_CROSSBOW);
  }

  public static final void incrementEquipmentCounters() {
    for (int i = 0; i < EquipmentManager.SLOTS; ++i) {
      int itemId = EquipmentManager.getEquipment(i).getItemId();
      switch (itemId) {
        case ItemPool.SUGAR_CHAPEAU,
            ItemPool.SUGAR_SHANK,
            ItemPool.SUGAR_SHIELD,
            ItemPool.SUGAR_SHILLELAGH,
            ItemPool.SUGAR_SHIRT,
            ItemPool.SUGAR_SHOTGUN,
            ItemPool.SUGAR_SHORTS -> Preferences.increment("sugarCounter" + itemId, 1);
        case ItemPool.COZY_SCIMITAR, ItemPool.COZY_STAFF, ItemPool.COZY_BAZOOKA -> Preferences
            .increment("cozyCounter" + itemId, 1);
      }
    }
  }

  public static final void decrementTurns() {
    if (usingStickerWeapon()) {
      GearChangePanel.updateStickers(
          --EquipmentManager.turnsRemaining[0],
          --EquipmentManager.turnsRemaining[1],
          --EquipmentManager.turnsRemaining[2]);
    }

    EquipmentManager.incrementEquipmentCounters();
  }

  public static final void stickersExpired(int count) {
    for (int i = 0; i < 3; ++i) {
      if (EquipmentManager.turnsRemaining[i] <= 0
          && getEquipment(EquipmentManager.STICKER1 + i) != EquipmentRequest.UNEQUIP) {
        setEquipment(EquipmentManager.STICKER1 + i, EquipmentRequest.UNEQUIP);
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
  public static final List<List<AdventureResult>> getEquipmentLists() {
    return EquipmentManager.equipmentLists;
  }

  public static final void updateEquipmentList(final int listIndex) {
    ConsumptionType consumeFilter = EquipmentManager.equipmentTypeToConsumeFilter(listIndex);
    if (consumeFilter == ConsumptionType.UNKNOWN) {
      return;
    }

    AdventureResult equippedItem = EquipmentManager.getEquipment(listIndex);

    switch (listIndex) {
      case EquipmentManager.ACCESSORY1:
      case EquipmentManager.ACCESSORY2:
        return; // do all the work when updating ACC3

      case EquipmentManager.ACCESSORY3:
        EquipmentManager.updateEquipmentList(consumeFilter, EquipmentManager.accessories);
        AdventureResult accessory = EquipmentManager.getEquipment(EquipmentManager.ACCESSORY1);
        if (accessory != EquipmentRequest.UNEQUIP) {
          AdventureResult.addResultToList(EquipmentManager.accessories, accessory);
        }
        accessory = EquipmentManager.getEquipment(EquipmentManager.ACCESSORY2);
        if (accessory != EquipmentRequest.UNEQUIP) {
          AdventureResult.addResultToList(EquipmentManager.accessories, accessory);
        }
        accessory = EquipmentManager.getEquipment(EquipmentManager.ACCESSORY3);
        if (accessory != EquipmentRequest.UNEQUIP) {
          AdventureResult.addResultToList(EquipmentManager.accessories, accessory);
        }
        break;

      case EquipmentManager.FAMILIAR:

        // If we are looking at familiar items, include those
        // which can be universally equipped, but are currently
        // on another familiar.

        EquipmentManager.updateEquipmentList(
            consumeFilter, EquipmentManager.equipmentLists.get(EquipmentManager.FAMILIAR));

        FamiliarData[] familiarList = KoLCharacter.ownedFamiliars().toArray(new FamiliarData[0]);

        FamiliarData currentFamiliar = KoLCharacter.getFamiliar();

        for (FamiliarData familiarData : familiarList) {
          AdventureResult currentItem = familiarData.getItem();
          if (currentItem != EquipmentRequest.UNEQUIP && currentFamiliar.canEquip(currentItem)) {
            AdventureResult.addResultToList(
                EquipmentManager.equipmentLists.get(EquipmentManager.FAMILIAR), currentItem);
          }
        }

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
    List<SpecialOutfit> newOutfits = LockableListFactory.getSortedInstance(SpecialOutfit.class);
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
    for (int i = 0; i < EquipmentManager.ALL_SLOTS; ++i) {
      EquipmentManager.updateEquipmentList(i);
    }
    EquipmentManager.updateNormalOutfits();
  }

  public static final ConsumptionType equipmentTypeToConsumeFilter(final int equipmentType) {
    return switch (equipmentType) {
      case EquipmentManager.HAT -> ConsumptionType.HAT;
      case EquipmentManager.WEAPON -> ConsumptionType.WEAPON;
      case EquipmentManager.OFFHAND -> ConsumptionType.OFFHAND;
      case EquipmentManager.SHIRT -> ConsumptionType.SHIRT;
      case EquipmentManager.PANTS -> ConsumptionType.PANTS;
      case EquipmentManager.CONTAINER -> ConsumptionType.CONTAINER;
      case EquipmentManager.ACCESSORY1,
          EquipmentManager.ACCESSORY2,
          EquipmentManager.ACCESSORY3 -> ConsumptionType.ACCESSORY;
      case EquipmentManager.FAMILIAR -> ConsumptionType.FAMILIAR_EQUIPMENT;
      case EquipmentManager.STICKER1,
          EquipmentManager.STICKER2,
          EquipmentManager.STICKER3 -> ConsumptionType.STICKER;
      case EquipmentManager.CARDSLEEVE -> ConsumptionType.CARD;
      case EquipmentManager.FOLDER1,
          EquipmentManager.FOLDER2,
          EquipmentManager.FOLDER3,
          EquipmentManager.FOLDER4,
          EquipmentManager.FOLDER5 -> ConsumptionType.FOLDER;
      case EquipmentManager.BOOTSKIN -> ConsumptionType.BOOTSKIN;
      case EquipmentManager.BOOTSPUR -> ConsumptionType.BOOTSPUR;
      case EquipmentManager.HOLSTER -> ConsumptionType.SIXGUN;
      default -> ConsumptionType.UNKNOWN;
    };
  }

  public static final int consumeFilterToEquipmentType(final ConsumptionType consumeFilter) {
    return switch (consumeFilter) {
      case HAT -> EquipmentManager.HAT;
      case WEAPON -> EquipmentManager.WEAPON;
      case OFFHAND -> EquipmentManager.OFFHAND;
      case SHIRT -> EquipmentManager.SHIRT;
      case PANTS -> EquipmentManager.PANTS;
      case CONTAINER -> EquipmentManager.CONTAINER;
      case ACCESSORY -> EquipmentManager.ACCESSORY1;
      case FAMILIAR_EQUIPMENT -> EquipmentManager.FAMILIAR;
      case STICKER -> EquipmentManager.STICKER1;
      case CARD -> EquipmentManager.CARDSLEEVE;
      case FOLDER -> EquipmentManager.FOLDER1;
      case BOOTSKIN -> EquipmentManager.BOOTSKIN;
      case BOOTSPUR -> EquipmentManager.BOOTSPUR;
      case SIXGUN -> EquipmentManager.HOLSTER;
      default -> -1;
    };
  }

  public static final int itemIdToEquipmentType(final int itemId) {
    return EquipmentManager.consumeFilterToEquipmentType(ItemDatabase.getConsumptionType(itemId));
  }

  /**
   * Accessor method to retrieve # of hands character's weapon uses
   *
   * @return int number of hands needed
   */
  public static final int getWeaponHandedness() {
    return EquipmentDatabase.getHands(
        EquipmentManager.getEquipment(EquipmentManager.WEAPON).getItemId());
  }

  /**
   * Accessor method to determine if character is currently dual-wielding
   *
   * @return boolean true if character has two weapons equipped
   */
  public static final boolean usingTwoWeapons() {
    return EquipmentDatabase.getHands(
            EquipmentManager.getEquipment(EquipmentManager.OFFHAND).getItemId())
        == 1;
  }

  /**
   * Accessor method to determine if character's weapon is a chefstaff
   *
   * @return boolean true if weapon is a chefstaff
   */
  public static final boolean usingChefstaff() {
    return EquipmentDatabase.isChefStaff(EquipmentManager.getEquipment(EquipmentManager.WEAPON));
  }

  /**
   * Accessor method to determine if character's off-hand is a bean can
   *
   * @return boolean true if off-hand is a bean can
   */
  public static final boolean usingCanOfBeans() {
    return EquipmentDatabase.isCanOfBeans(EquipmentManager.getEquipment(EquipmentManager.OFFHAND));
  }

  /**
   * Accessor method to determine if character has a sixgun holstered
   *
   * @return boolean true if off-hand is a bean can
   */
  public static final boolean holsteredSixgun() {
    return EquipmentManager.getEquipment(EquipmentManager.HOLSTER) != EquipmentRequest.UNEQUIP;
  }

  /**
   * Accessor method to determine available battery power in equipped Powerful Glove
   *
   * @return int 0 if not wearing Powerful Glove, otherwise remaining battery power
   */
  public static final int powerfulGloveUsableBatteryPower() {
    return KoLCharacter.hasEquipped(ItemPool.get(ItemPool.POWERFUL_GLOVE, 1))
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
        EquipmentDatabase.getItemType(
            EquipmentManager.getEquipment(EquipmentManager.WEAPON).getItemId());
    return type.equals("club")
        || (includeEffect
            && KoLConstants.activeEffects.contains(EquipmentManager.IRON_PALMS)
            && type.equals("sword"));
  }

  public static final boolean wieldingKnife() {
    String type =
        EquipmentDatabase.getItemType(
            EquipmentManager.getEquipment(EquipmentManager.WEAPON).getItemId());
    return type.equals("knife");
  }

  public static final boolean wieldingAccordion() {
    String type =
        EquipmentDatabase.getItemType(
            EquipmentManager.getEquipment(EquipmentManager.WEAPON).getItemId());
    return type.equals("accordion");
  }

  public static final boolean wieldingSword() {
    return EquipmentManager.wieldingSword(true);
  }

  public static final boolean wieldingSword(final boolean includeEffect) {
    boolean sword =
        EquipmentDatabase.isSword(
            EquipmentManager.getEquipment(EquipmentManager.WEAPON).getItemId());
    return sword
        && (!includeEffect || !KoLConstants.activeEffects.contains(EquipmentManager.IRON_PALMS));
  }

  public static final boolean wieldingGun() {
    int id = EquipmentManager.getEquipment(EquipmentManager.WEAPON).getItemId();
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
    return EquipmentDatabase.getItemType(EquipmentManager.getEquipment(OFFHAND).getItemId())
        .equals("shield");
  }

  /**
   * Accessor method to determine what type of weapon the character is wielding.
   *
   * @return int MELEE or RANGED
   */
  public static final WeaponType getWeaponType() {
    return EquipmentDatabase.getWeaponType(
        EquipmentManager.getEquipment(EquipmentManager.WEAPON).getItemId());
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
        if (EquipmentManager.getEquipment(EquipmentManager.WEAPON).getItemId()
            == ItemPool.FOURTH_SABER) {
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
        if (Modifiers.unarmed && KoLCharacter.hasSkill(SkillPool.MASTER_OF_THE_SURPRISING_FIST)) {
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

  public static final SpecialOutfit currentOutfit(AdventureResult[] equipment) {
    // Go through any outfit that any worn item belongs to.
    int hash = SpecialOutfit.equipmentHash(equipment);
    List<Integer> checkedOutfits = new ArrayList<>();
    for (int i = EquipmentManager.HAT; i < EquipmentManager.FAMILIAR; i++) {
      AdventureResult item = equipment[i];
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
    for (int i = 0; i < EquipmentManager.SLOTS; ++i) {
      if (item.equals(EquipmentManager.getEquipment(i))) {
        count++;
      }
    }
    return count;
  }

  public static final void parseStatus(final JSONObject JSON) throws JSONException {
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
    // "stickers":[0,0,0],
    // "folder_holder":["01","22","12","00","00"]

    AdventureResult[] current = EquipmentManager.allEquipment();
    AdventureResult[] equipment = EquipmentManager.emptyEquipmentArray(true);
    int fakeHands = 0;

    JSONObject equip = JSON.getJSONObject("equipment");
    Iterator<String> keys = equip.keys();
    while (keys.hasNext()) {
      String slotName = keys.next();
      if (slotName.equals("fakehands")) {
        fakeHands = equip.getInt(slotName);
        continue;
      }

      int slot = EquipmentRequest.phpSlotNumber(slotName);
      if (slot == -1) {
        continue;
      }

      equipment[slot] = EquipmentManager.equippedItem(equip.getInt(slotName));
    }

    // Read stickers
    JSONArray stickers = JSON.getJSONArray("stickers");
    for (int i = 0; i < 3; ++i) {
      AdventureResult item = EquipmentManager.equippedItem(stickers.getInt(i));
      equipment[EquipmentManager.STICKER1 + i] = item;
    }

    // Read folders
    JSONArray folders = JSON.getJSONArray("folder_holder");
    for (int i = 0; i < 5; ++i) {
      int folder = folders.getInt(i);
      AdventureResult item =
          folder == 0 ? EquipmentRequest.UNEQUIP : ItemPool.get(ItemPool.FOLDER_01 - 1 + folder, 1);
      equipment[EquipmentManager.FOLDER1 + i] = item;
    }

    // We can't read these from api.php (yet?)
    equipment[EquipmentManager.CROWNOFTHRONES] = current[EquipmentManager.CROWNOFTHRONES];
    equipment[EquipmentManager.BUDDYBJORN] = current[EquipmentManager.BUDDYBJORN];
    equipment[EquipmentManager.BOOTSKIN] = current[EquipmentManager.BOOTSKIN];
    equipment[EquipmentManager.BOOTSPUR] = current[EquipmentManager.BOOTSPUR];
    equipment[EquipmentManager.HOLSTER] = current[EquipmentManager.HOLSTER];

    // For debug purposes, print wherever KoLmafia's model differs
    // from KoL's model.

    if (!KoLmafia.isRefreshing()) {
      for (int i = 0; i < EquipmentManager.ALL_SLOTS; ++i) {
        // Quantum Terrarium will have a familiar item in api.php even
        // if the particular familiar can't equip it. Ignore that.
        if (i == EquipmentManager.FAMILIAR && KoLCharacter.inQuantum()) {
          continue;
        }
        if (!current[i].equals(equipment[i])) {
          String slotName = EquipmentRequest.slotNames[i];
          String message =
              "*** slot "
                  + slotName
                  + ": KoL has "
                  + equipment[i]
                  + " but KoLmafia has "
                  + current[i];
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
  }
}
