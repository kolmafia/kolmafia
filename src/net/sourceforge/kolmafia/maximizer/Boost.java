package net.sourceforge.kolmafia.maximizer;

import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.equipment.Slot;

public class Boost implements Comparable<Boost> {
  private boolean isEquipment, isShrug, priority;
  private final String cmd;
  private String text;
  private Slot slot;
  private final double boost;
  private final AdventureResult item;
  private AdventureResult effect;
  private FamiliarData fam, enthroned, bjorned;
  private String horse;
  private Map<Modeable, String> modeables;

  public Boost(String cmd, String text, AdventureResult item, double boost) {
    this.cmd = cmd;
    this.text = text;
    this.item = item;
    this.boost = boost;
    if (cmd.length() == 0) {
      this.text = "<html><font color=gray>" + text.replaceAll("&", "&amp;") + "</font></html>";
    }
  }

  public Boost(
      String cmd,
      String text,
      AdventureResult effect,
      boolean isShrug,
      AdventureResult item,
      double boost,
      boolean priority) {
    this(cmd, text, item, boost);
    this.isEquipment = false;
    this.effect = effect;
    this.isShrug = isShrug;
    this.priority = priority;
  }

  public Boost(String cmd, String text, Slot slot, AdventureResult item, double boost) {
    this(cmd, text, item, boost);
    this.isEquipment = true;
    this.slot = slot;
  }

  public Boost(String cmd, String text, String horse, double boost) {
    this(cmd, text, (AdventureResult) null, boost);
    this.isEquipment = false;
    this.horse = horse;
  }

  public Boost(
      String cmd,
      String text,
      Slot slot,
      AdventureResult item,
      double boost,
      FamiliarData enthroned,
      FamiliarData bjorned,
      Map<Modeable, String> modeables) {
    this(cmd, text, item, boost);
    this.isEquipment = true;
    this.slot = slot;
    this.enthroned = enthroned;
    this.bjorned = bjorned;
    this.modeables = modeables;
  }

  public Boost(String cmd, String text, FamiliarData fam, double boost) {
    this(cmd, text, (AdventureResult) null, boost);
    this.isEquipment = true;
    this.fam = fam;
    this.slot = Slot.NONE;
  }

  @Override
  public String toString() {
    return this.text;
  }

  @Override
  public int compareTo(Boost o) {
    if (o == null) return -1;

    if (this.isEquipment != o.isEquipment) {
      return this.isEquipment ? -1 : 1;
    }
    if (this.priority != o.priority) {
      return this.priority ? -1 : 1;
    }
    if (this.isEquipment) return 0; // preserve order of addition
    return Double.compare(o.boost, this.boost);
  }

  public boolean execute(boolean equipOnly) {
    if (equipOnly && !this.isEquipment) return false;
    if (this.cmd.length() == 0) return false;
    KoLmafiaCLI.DEFAULT_SHELL.executeLine(this.cmd);
    return true;
  }

  public void addTo(MaximizerSpeculation spec) {
    if (this.isEquipment) {
      if (this.fam != null) {
        spec.setFamiliar(fam);
      } else if (this.slot != Slot.NONE && this.item != null) {
        spec.equip(slot, this.item);
        if (this.enthroned != null) {
          spec.setEnthroned(this.enthroned);
        }
        if (this.bjorned != null) {
          spec.setBjorned(this.bjorned);
        }
        this.modeables.forEach(
            (k, v) -> {
              if (v != null) spec.setModeable(k, v);
            });
      }
    } else if (this.effect != null) {
      if (this.isShrug) {
        spec.removeEffect(this.effect);
      } else {
        spec.addEffect(this.effect);
      }
    } else if (this.horse != null) {
      spec.setHorsery(this.horse);
    }
  }

  public AdventureResult getItem() {
    return getItem(true);
  }

  public AdventureResult getItem(boolean preferEffect) {
    if (this.effect != null && preferEffect) return this.effect;
    return this.item;
  }

  public double getBoost() {
    return this.boost;
  }

  public String getCmd() {
    return this.cmd;
  }

  public Slot getSlot() {
    return this.slot;
  }

  public boolean isEquipment() {
    return this.isEquipment;
  }
}
