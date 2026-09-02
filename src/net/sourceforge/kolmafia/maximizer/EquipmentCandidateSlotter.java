package net.sourceforge.kolmafia.maximizer;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.WeaponType;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;

final class EquipmentCandidateSlotter {
  record Requirements(
      int hands,
      int melee,
      String weaponType,
      boolean shield,
      boolean club,
      boolean utensil,
      boolean sword,
      boolean knife,
      boolean accordion,
      boolean effective) {}

  record Placement(boolean accepted, Slot slot, Slot auxiliarySlot, boolean skipScoring) {
    static Placement reject() {
      return new Placement(false, Slot.NONE, Slot.NONE, false);
    }
  }

  private final Requirements requirements;
  private final boolean hoboPowerUseful;
  private final boolean itemDropUseful;
  private final boolean experienceUseful;
  private final long maxPrice;
  private final PriceLevel priceLevel;
  private boolean gloveAvailable;

  EquipmentCandidateSlotter(
      Requirements requirements,
      boolean hoboPowerUseful,
      boolean itemDropUseful,
      boolean experienceUseful,
      long maxPrice,
      PriceLevel priceLevel) {
    this.requirements = requirements;
    this.hoboPowerUseful = hoboPowerUseful;
    this.itemDropUseful = itemDropUseful;
    this.experienceUseful = experienceUseful;
    this.maxPrice = maxPrice;
    this.priceLevel = priceLevel;
  }

  Placement place(
      int itemId, String itemName, Slot slot, CheckedItem item, boolean familiarCanEquip)
      throws MaximizerInterruptedException {
    return switch (slot) {
      case FAMILIAR ->
          familiarCanEquip ? new Placement(true, slot, Slot.NONE, false) : Placement.reject();
      case WEAPON -> placeWeapon(itemId, item);
      case OFFHAND -> placeOffhand(itemId, itemName, item);
      case ACCESSORY1 -> placeAccessory(itemId, item);
      case SHIRT -> placeShirt(itemId, item);
      default -> new Placement(true, slot, Slot.NONE, false);
    };
  }

  private Placement placeWeapon(int itemId, CheckedItem item) {
    int hands = EquipmentDatabase.getHands(itemId);
    if ((this.requirements.hands() == 1 && hands != 1)
        || (this.requirements.hands() > 1 && hands < this.requirements.hands())) {
      return Placement.reject();
    }

    WeaponType weaponType = EquipmentDatabase.getWeaponType(itemId);
    if ((this.requirements.melee() > 0 && weaponType != WeaponType.MELEE)
        || (this.requirements.melee() < 0 && weaponType != WeaponType.RANGED)) {
      return Placement.reject();
    }

    String type = EquipmentDatabase.getItemType(itemId);
    if (this.requirements.weaponType() != null && !type.contains(this.requirements.weaponType())) {
      return Placement.reject();
    }

    Slot slot = Slot.WEAPON;
    Slot auxiliarySlot = Slot.NONE;
    if (hands == 1) {
      slot = Evaluator.WEAPON_1H;
      if (type.equals("chefstaff")) {
        if (!EquipmentManager.canEquipChefstaff(this.gloveAvailable)) {
          return Placement.reject();
        }
      } else if (!this.requirements.shield() && !EquipmentDatabase.isMainhandOnly(itemId)) {
        auxiliarySlot =
            switch (weaponType) {
              case MELEE -> Evaluator.OFFHAND_MELEE;
              case RANGED -> Evaluator.OFFHAND_RANGED;
              case NONE -> Slot.NONE;
            };
      }
    }

    if ((this.requirements.club() && !EquipmentDatabase.isClub(itemId))
        || (this.requirements.utensil() && !EquipmentDatabase.isUtensil(itemId))
        || (this.requirements.sword() && !EquipmentDatabase.isSword(itemId))
        || (this.requirements.knife() && !EquipmentDatabase.isKnife(itemId))
        || (this.requirements.accordion() && !EquipmentDatabase.isAccordion(itemId))) {
      slot = auxiliarySlot;
    }

    if (this.requirements.effective()
        && itemId != ItemPool.FOURTH_SABER
        && itemId != ItemPool.REPLICA_FOURTH_SABER
        && !ModifierDatabase.getBooleanModifier(
            ModifierType.ITEM, itemId, BooleanModifier.ATTACKS_CANT_MISS)) {
      if (KoLCharacter.getAdjustedMoxie() >= KoLCharacter.getAdjustedMuscle()
          && weaponType != WeaponType.RANGED
          && (!EquipmentDatabase.isKnife(itemId)
              || !KoLCharacter.hasSkill(SkillPool.TRICKY_KNIFEWORK))) {
        slot = auxiliarySlot;
      }
      if (KoLCharacter.getAdjustedMoxie() < KoLCharacter.getAdjustedMuscle()
          && weaponType != WeaponType.MELEE) {
        slot = auxiliarySlot;
      }
    }

    boolean skipScoring =
        itemId == ItemPool.BROKEN_CHAMPAGNE
            && this.itemDropUseful
            && (Preferences.getInteger("garbageChampagneCharge") > 0
                || !Preferences.getBoolean("_garbageItemChanged"));
    if (skipScoring) {
      item.requiredFlag = true;
      item.automaticFlag = true;
    }
    return new Placement(true, slot, auxiliarySlot, skipScoring);
  }

  private Placement placeOffhand(int itemId, String itemName, CheckedItem item) {
    if (this.requirements.shield()
        && !EquipmentDatabase.isShield(itemId)
        && itemId != ItemPool.UNBREAKABLE_UMBRELLA) {
      return Placement.reject();
    }
    if (this.hoboPowerUseful && itemName.startsWith("Hodgman's")) {
      Modifiers.hoboPower = 100.0;
      item.automaticFlag = true;
    }
    return new Placement(true, Slot.OFFHAND, Slot.NONE, false);
  }

  private Placement placeAccessory(int itemId, CheckedItem item)
      throws MaximizerInterruptedException {
    boolean enablesChefstaff =
        itemId == ItemPool.SPECIAL_SAUCE_GLOVE
            && EquipmentManager.canEquipChefstaff(true)
            && !EquipmentManager.canEquipChefstaff(false);
    if (!enablesChefstaff) {
      return new Placement(true, Slot.ACCESSORY1, Slot.NONE, false);
    }

    item.validate(this.maxPrice, this.priceLevel);
    if (item.getCount() == 0) {
      return Placement.reject();
    }
    item.automaticFlag = true;
    this.gloveAvailable = true;
    return new Placement(true, Slot.ACCESSORY1, Slot.NONE, true);
  }

  private Placement placeShirt(int itemId, CheckedItem item) {
    boolean skipScoring =
        itemId == ItemPool.MAKESHIFT_GARBAGE_SHIRT
            && this.experienceUseful
            && Preferences.getInteger("garbageShirtCharge") > 0;
    if (skipScoring) {
      item.requiredFlag = true;
      item.automaticFlag = true;
    }
    return new Placement(true, Slot.SHIRT, Slot.NONE, skipScoring);
  }
}
