package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Speculation {
  private int MCD;
  public EnumMap<Slot, AdventureResult> equipment;
  private final ArrayList<AdventureResult> effects;
  private FamiliarData familiar, enthroned, bjorned;
  private String custom, horsery, boomBox;
  protected boolean calculated = false;
  protected Modifiers mods;
  private final Map<Modeable, String> modeables;

  public Speculation() {
    this.MCD = KoLCharacter.getMindControlLevel();
    this.equipment = EquipmentManager.allEquipment();
    this.effects = new ArrayList<>();
    this.effects.addAll(KoLConstants.activeEffects);
    // Strip out intrinsic effects granted by equipment - they will
    // be readded if appropriate via Intrinsic Effect modifiers.
    // We used to just strip out all intrinsics, back when non-equipment
    // intrinsics were all just flavor rather than possibly significant.
    for (var equip : this.equipment.values()) {
      if (equip == null) continue;
      int itemId = equip.getItemId();
      Modifiers mods = ModifierDatabase.getItemModifiers(itemId);
      if (mods == null) continue;
      String name = mods.getString(StringModifier.INTRINSIC_EFFECT);
      if (name.length() == 0) continue;
      int effectId = EffectDatabase.getEffectId(name);
      this.effects.remove(EffectPool.get(effectId));
    }
    this.familiar = KoLCharacter.currentFamiliar;
    this.enthroned = KoLCharacter.currentEnthroned;
    this.bjorned = KoLCharacter.currentBjorned;
    this.custom = null;
    this.horsery = Preferences.getString("_horsery");
    this.boomBox = Preferences.getString("boomBoxSong");
    this.modeables = Modeable.getStateMap();
  }

  public void setMindControlLevel(int MCD) {
    this.MCD = MCD;
  }

  public void setFamiliar(FamiliarData familiar) {
    this.familiar = familiar;
  }

  public void setEnthroned(FamiliarData familiar) {
    this.enthroned = familiar;
  }

  public void setBjorned(FamiliarData familiar) {
    this.bjorned = familiar;
  }

  public void setModeable(Modeable modeable, String value) {
    this.modeables.put(modeable, value);
  }

  public void setCustom(String custom) {
    this.custom = custom;
  }

  public void setHorsery(String horsery) {
    this.horsery = horsery;
  }

  public void setBoomBox(String boomBox) {
    this.boomBox = boomBox;
  }

  public FamiliarData getEnthroned() {
    return this.enthroned;
  }

  public FamiliarData getBjorned() {
    return this.bjorned;
  }

  public FamiliarData getFamiliar() {
    return this.familiar;
  }

  public String getCustom() {
    return this.custom;
  }

  public String getHorsery() {
    return this.horsery;
  }

  public String getBoomBox() {
    return this.boomBox;
  }

  public Map<Modeable, String> getModeables() {
    return this.modeables;
  }

  public void equip(Slot slot, AdventureResult item) {
    if (slot == Slot.NONE) return;
    this.equipment.put(slot, item);
    if (slot == Slot.WEAPON && EquipmentDatabase.getHands(item.getItemId()) > 1) {
      this.equipment.put(Slot.OFFHAND, EquipmentRequest.UNEQUIP);
    }
  }

  public boolean hasEffect(AdventureResult effect) {
    return this.effects.contains(effect);
  }

  public void addEffect(AdventureResult effect) {
    if (!this.effects.contains(effect)) {
      this.effects.add(effect);
    }
  }

  public void removeEffect(AdventureResult effect) {
    this.effects.remove(effect);
  }

  public Modifiers calculate() {
    this.mods =
        KoLCharacter.recalculateAdjustments(
            false,
            this.MCD,
            this.equipment,
            this.effects,
            this.familiar,
            this.enthroned,
            this.bjorned,
            this.custom,
            this.horsery,
            this.boomBox,
            this.modeables,
            true);
    this.calculated = true;
    return this.mods;
  }

  public Modifiers getModifiers() {
    if (!this.calculated) this.calculate();
    return this.mods;
  }

  public boolean parse(String text) {
    boolean quiet = false;
    String[] pieces = text.toLowerCase().split("\\s*;\\s*");
    for (String s : pieces) {
      String[] piece = s.split(" ", 2);
      String cmd = piece[0];
      String params = piece.length > 1 ? piece[1] : "";

      if (cmd.isEmpty()) {
        continue;
      }

      switch (cmd) {
        case "mcd" -> this.setMindControlLevel(StringUtilities.parseInt(params));
        case "equip" -> {
          piece = params.split(" ", 2);
          Slot slot = EquipmentRequest.slotNumber(piece[0]);
          if (slot != Slot.NONE) {
            params = piece[1];
          }

          AdventureResult match = ItemFinder.getFirstMatchingItem(params, Match.EQUIP);
          if (match == null) {
            return true;
          }
          if (slot == Slot.NONE) {
            slot = EquipmentRequest.chooseEquipmentSlot(match.getItemId());

            // If it can't be equipped, give up
            if (slot == Slot.NONE) {
              KoLmafia.updateDisplay(MafiaState.ERROR, "You can't equip a " + match.getName());
              return true;
            }
          }
          this.equip(slot, match);
        }
        case "unequip" -> {
          Slot slot = EquipmentRequest.slotNumber(params);
          if (slot == Slot.NONE) {
            KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown slot: " + params);
            return true;
          }
          this.equip(slot, EquipmentRequest.UNEQUIP);
        }
        case "familiar" -> {
          int id = FamiliarDatabase.getFamiliarId(params);
          if (id == -1 && !params.equals("none")) {
            KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown familiar: " + params);
            return true;
          }
          FamiliarData fam = KoLCharacter.usableFamiliar(id);
          if (fam == null) {
            fam = new FamiliarData(id);
          }
          this.setFamiliar(fam);
        }
        case "enthrone" -> {
          int id = FamiliarDatabase.getFamiliarId(params);
          if (id == -1 && !params.equals("none")) {
            KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown familiar: " + params);
            return true;
          }
          FamiliarData fam = new FamiliarData(id);
          this.setEnthroned(fam);
          this.equip(Slot.HAT, ItemPool.get(ItemPool.HATSEAT));
        }
        case "bjornify" -> {
          int id = FamiliarDatabase.getFamiliarId(params);
          if (id == -1 && !params.equals("none")) {
            KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown familiar: " + params);
            return true;
          }
          FamiliarData fam = new FamiliarData(id);
          this.setBjorned(fam);
          this.equip(Slot.CONTAINER, ItemPool.get(ItemPool.BUDDY_BJORN));
        }
        case "up" -> {
          List<String> effects = EffectDatabase.getMatchingNames(params);
          if (effects.isEmpty()) {
            KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown effect: " + params);
            return true;
          }

          int effectId = EffectDatabase.getEffectId(effects.get(0));
          AdventureResult effect = EffectPool.get(effectId);
          if (!this.hasEffect(effect)) {
            this.addEffect(effect);
          }
        }
        case "uneffect" -> {
          List<String> effects = EffectDatabase.getMatchingNames(params);
          if (effects.isEmpty()) {
            KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown effect: " + params);
            return true;
          }

          int effectId = EffectDatabase.getEffectId(effects.get(0));
          AdventureResult effect = EffectPool.get(effectId);
          this.removeEffect(effect);
        }
        case "quiet" -> quiet = true;
        default -> {
          var modeable = Modeable.find(cmd);
          if (modeable != null) {
            if (!modeable.validate(cmd, params)) {
              KoLmafia.updateDisplay(
                  MafiaState.ERROR, "Unknown parameter for " + cmd + ": " + params);
              return true;
            }

            this.setModeable(modeable, params);
            this.equip(KoLCharacter.equipmentSlot(modeable.getItem()), modeable.getItem());
          }
          KoLmafia.updateDisplay(MafiaState.ERROR, "I don't know how to speculate about " + cmd);
          return true;
        }
      }
    }
    return quiet;
  }
}
