package net.sourceforge.kolmafia.maximizer;

import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.WeaponType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.Speculation;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.BooleanArray;

public class MaximizerSpeculation extends Speculation
    implements Comparable<MaximizerSpeculation>, Cloneable {
  private boolean scored = false;
  private boolean tiebreakered = false;
  private boolean exceeded;
  private double score, tiebreaker;
  private int simplicity;
  private int beeosity;

  public boolean failed = false;
  public CheckedItem attachment;
  private boolean foldables = false;

  @Override
  public Object clone() {
    try {
      MaximizerSpeculation copy = (MaximizerSpeculation) super.clone();
      copy.equipment = this.equipment.clone();
      return copy;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  @Override
  public String toString() {
    if (this.attachment != null) {
      return this.attachment.getInstance((int) this.getScore()).toString();
    }
    return super.toString();
  }

  public void setUnscored() {
    this.scored = false;
    this.calculated = false;
  }

  public double getScore() {
    if (this.scored) return this.score;
    if (!this.calculated) this.calculate();
    this.score = Maximizer.eval.getScore(this.mods, this.equipment);
    if (KoLCharacter.inBeecore()) {
      this.beeosity = KoLCharacter.getBeeosity(this.equipment);
    }
    Maximizer.eval.checkEquipment(this.mods, this.equipment, this.beeosity);
    this.failed = Maximizer.eval.failed;
    if ((this.mods.getRawBitmap(Modifiers.MUTEX_VIOLATIONS)
            & ~KoLCharacter.currentRawBitmapModifier(Modifiers.MUTEX_VIOLATIONS))
        != 0) { // We're speculating about something that would create a
      // mutex problem that the player didn't already have.
      this.failed = true;
    }
    this.exceeded = Maximizer.eval.exceeded;
    this.scored = true;
    return this.score;
  }

  public double getTiebreaker() {
    if (this.tiebreakered) return this.tiebreaker;
    if (!this.calculated) this.calculate();
    this.tiebreaker = Maximizer.eval.getTiebreaker(this.mods);
    this.tiebreakered = true;
    this.simplicity = 0;
    for (int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot) {
      AdventureResult item = this.equipment[slot];
      if (item == null) item = EquipmentRequest.UNEQUIP;
      if (EquipmentManager.getEquipment(slot).equals(item)) {
        this.simplicity += 2;
      } else if (item.equals(EquipmentRequest.UNEQUIP)) {
        this.simplicity += slot == EquipmentManager.WEAPON ? -1 : 1;
      }
    }
    return this.tiebreaker;
  }

  public int compareTo(MaximizerSpeculation o) {
    if (!(o instanceof MaximizerSpeculation)) return 1;
    MaximizerSpeculation other = o;
    int rv = Double.compare(this.getScore(), other.getScore());
    // Always prefer success to failure
    if (this.failed != other.failed) return this.failed ? -1 : 1;
    // Prefer higher bonus
    if (rv != 0) return rv;
    // In Bees Hate You, prefer lower B count
    rv = other.beeosity - this.beeosity;
    if (rv != 0) return rv;
    // Get other comparisons
    int countThisEffects = 0;
    int countOtherEffects = 0;
    int countThisBreakables = 0;
    int countOtherBreakables = 0;
    int countThisDropsItems = 0;
    int countOtherDropsItems = 0;
    int countThisDropsMeat = 0;
    int countOtherDropsMeat = 0;
    for (int i = this.equipment.length - 1; i >= 0; --i) {
      if (this.equipment[i] == null) continue;
      int itemId = this.equipment[i].getItemId();
      Modifiers mods = Modifiers.getItemModifiers(itemId);
      if (mods == null) continue;
      String name = mods.getString(Modifiers.ROLLOVER_EFFECT);
      if (name.length() > 0) countThisEffects++;
      if (mods.getBoolean(Modifiers.BREAKABLE)) countThisBreakables++;
      if (mods.getBoolean(Modifiers.DROPS_ITEMS)) countThisDropsItems++;
      if (mods.getBoolean(Modifiers.DROPS_MEAT)) countThisDropsMeat++;
    }
    for (int i = other.equipment.length - 1; i >= 0; --i) {
      if (other.equipment[i] == null) continue;
      int itemId = other.equipment[i].getItemId();
      Modifiers mods = Modifiers.getItemModifiers(itemId);
      if (mods == null) continue;
      String name = mods.getString(Modifiers.ROLLOVER_EFFECT);
      if (name.length() > 0) countOtherEffects++;
      if (mods.getBoolean(Modifiers.BREAKABLE)) countOtherBreakables++;
      if (mods.getBoolean(Modifiers.DROPS_ITEMS)) countOtherDropsItems++;
      if (mods.getBoolean(Modifiers.DROPS_MEAT)) countOtherDropsMeat++;
    }
    // Prefer item droppers
    if (countThisDropsItems != countOtherDropsItems) {
      return countThisDropsItems > countOtherDropsItems ? 1 : -1;
    }
    // Prefer meat droppers
    if (countThisDropsMeat != countOtherDropsMeat) {
      return countThisDropsMeat > countOtherDropsMeat ? 1 : -1;
    }
    // Prefer higher tiebreaker account (unless -tie used)
    rv = Double.compare(this.getTiebreaker(), other.getTiebreaker());
    if (rv != 0) return rv;
    // Prefer rollover effects
    if (countThisEffects != countOtherEffects) {
      return countThisEffects > countOtherEffects ? 1 : -1;
    }
    // Prefer unbreakables
    if (countThisBreakables != countOtherBreakables) {
      return countThisBreakables < countOtherBreakables ? 1 : -1;
    }
    // Prefer worn
    rv = this.simplicity - other.simplicity;
    if (rv != 0) return rv;
    if (this.attachment != null && other.attachment != null) {
      // prefer items that you don't have to buy
      if (this.attachment.buyableFlag != other.attachment.buyableFlag) {
        return this.attachment.buyableFlag ? -1 : 1;
      }
      if (KoLCharacter.inBeecore()) { // prefer fewer Bs
        rv =
            KoLCharacter.getBeeosity(other.attachment.getName())
                - KoLCharacter.getBeeosity(this.attachment.getName());
      }

      // prefer items that you have
      // doesn't consider wanting multiple of the same item and not having enough
      if ((this.attachment.inventory > 0) != (other.attachment.inventory > 0)) {
        return this.attachment.inventory > 0 ? 1 : -1;
      }
      if ((this.attachment.initial > 0) != (other.attachment.initial > 0)) {
        return this.attachment.initial > 0 ? 1 : -1;
      }
    }
    return rv;
  }

  // Remember which equipment slots were null, so that this
  // state can be restored later.
  public Object mark() {
    return this.equipment.clone();
  }

  public void restore(Object mark) {
    System.arraycopy(mark, 0, this.equipment, 0, EquipmentManager.ALL_SLOTS);
  }

  public void tryAll(
      List<FamiliarData> familiars,
      List<FamiliarData> enthronedFamiliars,
      BooleanArray usefulOutfits,
      Map<AdventureResult, AdventureResult> outfitPieces,
      List<CheckedItem>[] possibles,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar,
      FamiliarData useBjornFamiliar)
      throws MaximizerInterruptedException {
    this.foldables = Preferences.getBoolean("maximizerFoldables");
    this.tryOutfits(
        enthronedFamiliars,
        usefulOutfits,
        outfitPieces,
        possibles,
        bestCard,
        useCrownFamiliar,
        useBjornFamiliar);
    for (int i = 0; i < familiars.size(); ++i) {
      this.setFamiliar(familiars.get(i));
      possibles[EquipmentManager.FAMILIAR] = possibles[EquipmentManager.ALL_SLOTS + i];
      this.tryOutfits(
          enthronedFamiliars,
          usefulOutfits,
          outfitPieces,
          possibles,
          bestCard,
          useCrownFamiliar,
          useBjornFamiliar);
    }
  }

  public void tryOutfits(
      List<FamiliarData> enthronedFamiliars,
      BooleanArray usefulOutfits,
      Map<AdventureResult, AdventureResult> outfitPieces,
      List<CheckedItem>[] possibles,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar,
      FamiliarData useBjornFamiliar)
      throws MaximizerInterruptedException {
    Object mark = this.mark();
    for (int outfit = usefulOutfits.size() - 1; outfit >= 0; --outfit) {
      if (!usefulOutfits.get(outfit)) continue;
      AdventureResult[] pieces = EquipmentDatabase.getOutfit(outfit).getPieces();
      pieceloop:
      for (int idx = pieces.length - 1; ; --idx) {
        if (idx == -1) { // all pieces successfully put on
          this.tryFamiliarItems(
              enthronedFamiliars, possibles, bestCard, useCrownFamiliar, useBjornFamiliar);
          break;
        }
        AdventureResult item = outfitPieces.get(pieces[idx]);
        if (item == null) break; // not available
        int count = item.getCount();
        int slot = EquipmentManager.itemIdToEquipmentType(item.getItemId());

        switch (slot) {
          case EquipmentManager.HAT:
          case EquipmentManager.PANTS:
          case EquipmentManager.SHIRT:
          case EquipmentManager.CONTAINER:
            if (item.equals(this.equipment[slot])) { // already worn
              continue pieceloop;
            }
            if (item.equals(this.equipment[EquipmentManager.FAMILIAR])) {
              --count;
            }
            break;
          case EquipmentManager.WEAPON:
          case EquipmentManager.OFFHAND:
            if (item.equals(this.equipment[EquipmentManager.WEAPON])
                || item.equals(this.equipment[EquipmentManager.OFFHAND])) { // already worn
              continue pieceloop;
            }
            if (item.equals(this.equipment[EquipmentManager.FAMILIAR])) {
              --count;
            }
            break;
          case EquipmentManager.ACCESSORY1:
            if (item.equals(this.equipment[EquipmentManager.ACCESSORY1])
                || item.equals(this.equipment[EquipmentManager.ACCESSORY2])
                || item.equals(this.equipment[EquipmentManager.ACCESSORY3])) { // already worn
              continue pieceloop;
            }
            if (item.equals(this.equipment[EquipmentManager.FAMILIAR])) {
              --count;
            }
            if (this.equipment[EquipmentManager.ACCESSORY3] == null) {
              slot = EquipmentManager.ACCESSORY3;
            } else if (this.equipment[EquipmentManager.ACCESSORY2] == null) {
              slot = EquipmentManager.ACCESSORY2;
            }
            break;
          default:
            break pieceloop; // don't know how to wear that
        }

        if (count <= 0) break; // none available
        if (this.equipment[slot] != null) break; // slot taken
        this.equipment[slot] = item;
      }
      this.restore(mark);
    }

    this.tryFamiliarItems(
        enthronedFamiliars, possibles, bestCard, useCrownFamiliar, useBjornFamiliar);
  }

  public void tryFamiliarItems(
      List<FamiliarData> enthronedFamiliars,
      List<CheckedItem>[] possibles,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar,
      FamiliarData useBjornFamiliar)
      throws MaximizerInterruptedException {
    Object mark = this.mark();
    if (this.equipment[EquipmentManager.FAMILIAR] == null) {
      List<CheckedItem> possible = possibles[EquipmentManager.FAMILIAR];
      boolean any = false;
      for (int pos = 0; pos < possible.size(); ++pos) {
        AdventureResult item = possible.get(pos);
        int count = item.getCount();
        if (item.equals(this.equipment[EquipmentManager.OFFHAND])) {
          --count;
        }
        if (item.equals(this.equipment[EquipmentManager.WEAPON])) {
          --count;
        }
        if (item.equals(this.equipment[EquipmentManager.HAT])) {
          --count;
        }
        if (item.equals(this.equipment[EquipmentManager.PANTS])) {
          --count;
        }
        List group = ItemDatabase.getFoldGroup(item.getName());
        if (group != null && this.foldables) {
          String groupName = (String) group.get(1);
          for (int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot) {
            if (slot != EquipmentManager.FAMILIAR && this.equipment[slot] != null) {
              List groupEquipped = ItemDatabase.getFoldGroup(this.equipment[slot].getName());
              if (groupEquipped != null && groupName.equals(groupEquipped.get(1))) {
                --count;
              }
            }
          }
        }
        if (count <= 0) continue;
        this.equipment[EquipmentManager.FAMILIAR] = item;
        this.tryContainers(
            enthronedFamiliars, possibles, bestCard, useCrownFamiliar, useBjornFamiliar);
        any = true;
        this.restore(mark);
      }

      if (any) return;
      this.equipment[EquipmentManager.FAMILIAR] = EquipmentRequest.UNEQUIP;
    }

    this.tryContainers(enthronedFamiliars, possibles, bestCard, useCrownFamiliar, useBjornFamiliar);
    this.restore(mark);
  }

  public void tryContainers(
      List<FamiliarData> enthronedFamiliars,
      List<CheckedItem>[] possibles,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar,
      FamiliarData useBjornFamiliar)
      throws MaximizerInterruptedException {
    Object mark = this.mark();
    if (this.equipment[EquipmentManager.CONTAINER] == null) {
      List<CheckedItem> possible = possibles[EquipmentManager.CONTAINER];
      boolean any = false;
      for (int pos = 0; pos < possible.size(); ++pos) {
        AdventureResult item = possible.get(pos);
        int count = item.getCount();
        List group = ItemDatabase.getFoldGroup(item.getName());
        if (group != null && this.foldables) {
          String groupName = (String) group.get(1);
          for (int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot) {
            if (slot != EquipmentManager.CONTAINER && this.equipment[slot] != null) {
              List groupEquipped = ItemDatabase.getFoldGroup(this.equipment[slot].getName());
              if (groupEquipped != null && groupName.equals(groupEquipped.get(1))) {
                --count;
              }
            }
          }
        }
        if (count <= 0) continue;
        this.equipment[EquipmentManager.CONTAINER] = item;
        if (item.getItemId() == ItemPool.BUDDY_BJORN) {
          if (useBjornFamiliar != FamiliarData.NO_FAMILIAR) {
            this.setBjorned(useBjornFamiliar);
            this.tryAccessories(enthronedFamiliars, possibles, 0, bestCard, useCrownFamiliar);
            any = true;
            this.restore(mark);
          } else {
            for (FamiliarData f : enthronedFamiliars) {
              this.setBjorned(f);
              this.tryAccessories(enthronedFamiliars, possibles, 0, bestCard, useCrownFamiliar);
              any = true;
              this.restore(mark);
            }
          }
        } else {
          this.tryAccessories(enthronedFamiliars, possibles, 0, bestCard, useCrownFamiliar);
          any = true;
          this.restore(mark);
        }
      }

      if (any) return;
      this.equipment[EquipmentManager.CONTAINER] = EquipmentRequest.UNEQUIP;
    }

    this.tryAccessories(enthronedFamiliars, possibles, 0, bestCard, useCrownFamiliar);
    this.restore(mark);
  }

  public void tryAccessories(
      List<FamiliarData> enthronedFamiliars,
      List<CheckedItem>[] possibles,
      int pos,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar)
      throws MaximizerInterruptedException {
    Object mark = this.mark();
    int free = 0;
    if (this.equipment[EquipmentManager.ACCESSORY1] == null) ++free;
    if (this.equipment[EquipmentManager.ACCESSORY2] == null) ++free;
    if (this.equipment[EquipmentManager.ACCESSORY3] == null) ++free;
    if (free > 0) {
      List<CheckedItem> possible = possibles[EquipmentManager.ACCESSORY1];
      boolean any = false;
      for (; pos < possible.size(); ++pos) {
        AdventureResult item = possible.get(pos);
        int count = item.getCount();
        if (item.equals(this.equipment[EquipmentManager.ACCESSORY1])) {
          --count;
        }
        if (item.equals(this.equipment[EquipmentManager.ACCESSORY2])) {
          --count;
        }
        if (item.equals(this.equipment[EquipmentManager.ACCESSORY3])) {
          --count;
        }
        List group = ItemDatabase.getFoldGroup(item.getName());
        if (group != null && this.foldables) {
          String groupName = (String) group.get(1);
          for (int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot) {
            if (this.equipment[slot] != null) {
              List groupEquipped = ItemDatabase.getFoldGroup(this.equipment[slot].getName());
              if (groupEquipped != null && groupName.equals(groupEquipped.get(1))) {
                --count;
              }
            }
          }
        }
        if (count <= 0) continue;
        for (count = Math.min(free, count); count > 0; --count) {
          if (this.equipment[EquipmentManager.ACCESSORY1] == null) {
            this.equipment[EquipmentManager.ACCESSORY1] = item;
          } else if (this.equipment[EquipmentManager.ACCESSORY2] == null) {
            this.equipment[EquipmentManager.ACCESSORY2] = item;
          } else if (this.equipment[EquipmentManager.ACCESSORY3] == null) {
            this.equipment[EquipmentManager.ACCESSORY3] = item;
          } else {
            System.out.println("no room left???");
            break; // no room left - shouldn't happen
          }

          this.tryAccessories(enthronedFamiliars, possibles, pos + 1, bestCard, useCrownFamiliar);
          any = true;
        }
        this.restore(mark);
      }

      if (any) return;

      if (this.equipment[EquipmentManager.ACCESSORY1] == null) {
        this.equipment[EquipmentManager.ACCESSORY1] = EquipmentRequest.UNEQUIP;
      }
      if (this.equipment[EquipmentManager.ACCESSORY2] == null) {
        this.equipment[EquipmentManager.ACCESSORY2] = EquipmentRequest.UNEQUIP;
      }
      if (this.equipment[EquipmentManager.ACCESSORY3] == null) {
        this.equipment[EquipmentManager.ACCESSORY3] = EquipmentRequest.UNEQUIP;
      }
    }

    this.trySwap(EquipmentManager.ACCESSORY1, EquipmentManager.ACCESSORY2);
    this.trySwap(EquipmentManager.ACCESSORY2, EquipmentManager.ACCESSORY3);
    this.trySwap(EquipmentManager.ACCESSORY3, EquipmentManager.ACCESSORY1);

    this.tryHats(enthronedFamiliars, possibles, bestCard, useCrownFamiliar);
    this.restore(mark);
  }

  public void tryHats(
      List<FamiliarData> enthronedFamiliars,
      List<CheckedItem>[] possibles,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar)
      throws MaximizerInterruptedException {
    Object mark = this.mark();
    if (this.equipment[EquipmentManager.HAT] == null) {
      List<CheckedItem> possible = possibles[EquipmentManager.HAT];
      boolean any = false;
      for (int pos = 0; pos < possible.size(); ++pos) {
        AdventureResult item = possible.get(pos);
        int count = item.getCount();
        if (item.equals(this.equipment[EquipmentManager.FAMILIAR])) {
          --count;
        }
        List group = ItemDatabase.getFoldGroup(item.getName());
        if (group != null && this.foldables) {
          String groupName = (String) group.get(1);
          for (int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot) {
            if (slot != EquipmentManager.HAT && this.equipment[slot] != null) {
              List groupEquipped = ItemDatabase.getFoldGroup(this.equipment[slot].getName());
              if (groupEquipped != null && groupName.equals(groupEquipped.get(1))) {
                --count;
              }
            }
          }
        }
        if (count <= 0) continue;
        this.equipment[EquipmentManager.HAT] = item;
        if (item.getItemId() == ItemPool.HATSEAT) {
          if (useCrownFamiliar != FamiliarData.NO_FAMILIAR) {
            this.setEnthroned(useCrownFamiliar);
            this.tryShirts(possibles, bestCard);
            any = true;
            this.restore(mark);
          } else {
            for (FamiliarData f : enthronedFamiliars) {
              // Cannot use same familiar for this and Bjorn
              if (f != this.getBjorned()) {
                this.setEnthroned(f);
                this.tryShirts(possibles, bestCard);
                any = true;
                this.restore(mark);
              }
            }
          }
        } else {
          this.tryShirts(possibles, bestCard);
          any = true;
          this.restore(mark);
        }
      }

      if (any) return;
      this.equipment[EquipmentManager.HAT] = EquipmentRequest.UNEQUIP;
    }

    this.tryShirts(possibles, bestCard);
    this.restore(mark);
  }

  public void tryShirts(List<CheckedItem>[] possibles, AdventureResult bestCard)
      throws MaximizerInterruptedException {
    Object mark = this.mark();
    if (this.equipment[EquipmentManager.SHIRT] == null) {
      boolean any = false;
      if (KoLCharacter.isTorsoAware()) {
        List<CheckedItem> possible = possibles[EquipmentManager.SHIRT];
        for (int pos = 0; pos < possible.size(); ++pos) {
          AdventureResult item = possible.get(pos);
          int count = item.getCount();
          if (item.equals(this.equipment[EquipmentManager.FAMILIAR])) {
            --count;
          }
          List group = ItemDatabase.getFoldGroup(item.getName());
          if (group != null && this.foldables) {
            String groupName = (String) group.get(1);
            for (int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot) {
              if (slot != EquipmentManager.SHIRT && this.equipment[slot] != null) {
                List groupEquipped = ItemDatabase.getFoldGroup(this.equipment[slot].getName());
                if (groupEquipped != null && groupName.equals(groupEquipped.get(1))) {
                  --count;
                }
              }
            }
          }
          if (count <= 0) continue;
          this.equipment[EquipmentManager.SHIRT] = item;
          this.tryPants(possibles, bestCard);
          any = true;
          this.restore(mark);
        }
      }

      if (any) return;
      this.equipment[EquipmentManager.SHIRT] = EquipmentRequest.UNEQUIP;
    }

    this.tryPants(possibles, bestCard);
    this.restore(mark);
  }

  public void tryPants(List<CheckedItem>[] possibles, AdventureResult bestCard)
      throws MaximizerInterruptedException {
    Object mark = this.mark();
    if (this.equipment[EquipmentManager.PANTS] == null) {
      List<CheckedItem> possible = possibles[EquipmentManager.PANTS];
      boolean any = false;
      for (int pos = 0; pos < possible.size(); ++pos) {
        AdventureResult item = possible.get(pos);
        int count = item.getCount();
        if (item.equals(this.equipment[EquipmentManager.FAMILIAR])) {
          --count;
        }
        List group = ItemDatabase.getFoldGroup(item.getName());
        if (group != null && this.foldables) {
          String groupName = (String) group.get(1);
          for (int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot) {
            if (slot != EquipmentManager.PANTS && this.equipment[slot] != null) {
              List groupEquipped = ItemDatabase.getFoldGroup(this.equipment[slot].getName());
              if (groupEquipped != null && groupName.equals(groupEquipped.get(1))) {
                --count;
              }
            }
          }
        }
        if (count <= 0) continue;
        this.equipment[EquipmentManager.PANTS] = item;
        this.trySixguns(possibles, bestCard);
        any = true;
        this.restore(mark);
      }

      if (any) return;
      this.equipment[EquipmentManager.PANTS] = EquipmentRequest.UNEQUIP;
    }

    this.trySixguns(possibles, bestCard);
    this.restore(mark);
  }

  public void trySixguns(List<CheckedItem>[] possibles, AdventureResult bestCard)
      throws MaximizerInterruptedException {
    Object mark = this.mark();
    if (this.equipment[EquipmentManager.HOLSTER] == null) {
      List<CheckedItem> possible = possibles[EquipmentManager.HOLSTER];
      boolean any = false;
      for (int pos = 0; pos < possible.size(); ++pos) {
        AdventureResult item = possible.get(pos);
        int count = item.getCount();
        if (count <= 0) continue;
        this.equipment[EquipmentManager.HOLSTER] = item;
        this.tryWeapons(possibles, bestCard);
        any = true;
        this.restore(mark);
      }

      if (any) return;
      this.equipment[EquipmentManager.HOLSTER] = EquipmentRequest.UNEQUIP;
    }

    this.tryWeapons(possibles, bestCard);
    this.restore(mark);
  }

  public void tryWeapons(List<CheckedItem>[] possibles, AdventureResult bestCard)
      throws MaximizerInterruptedException {
    Object mark = this.mark();
    boolean chefstaffable =
        KoLCharacter.hasSkill("Spirit of Rigatoni") || KoLCharacter.isJarlsberg();
    if (!chefstaffable && KoLCharacter.isSauceror()) {
      chefstaffable =
          this.equipment[EquipmentManager.ACCESSORY1].getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE
              || this.equipment[EquipmentManager.ACCESSORY2].getItemId()
                  == ItemPool.SPECIAL_SAUCE_GLOVE
              || this.equipment[EquipmentManager.ACCESSORY3].getItemId()
                  == ItemPool.SPECIAL_SAUCE_GLOVE;
    }
    if (this.equipment[EquipmentManager.WEAPON] == null) {
      List<CheckedItem> possible = possibles[EquipmentManager.WEAPON];
      // boolean any = false;
      for (int pos = 0; pos < possible.size(); ++pos) {
        AdventureResult item = possible.get(pos);
        if (!chefstaffable && EquipmentDatabase.getItemType(item.getItemId()).equals("chefstaff")) {
          continue;
        }
        int count = item.getCount();
        if (item.equals(this.equipment[EquipmentManager.OFFHAND])) {
          --count;
        }
        if (item.equals(this.equipment[EquipmentManager.FAMILIAR])) {
          --count;
        }
        List group = ItemDatabase.getFoldGroup(item.getName());
        if (group != null && this.foldables) {
          String groupName = (String) group.get(1);
          for (int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot) {
            if (slot != EquipmentManager.WEAPON && this.equipment[slot] != null) {
              List groupEquipped = ItemDatabase.getFoldGroup(this.equipment[slot].getName());
              if (groupEquipped != null && groupName.equals(groupEquipped.get(1))) {
                --count;
              }
            }
          }
        }
        if (count <= 0) continue;
        this.equipment[EquipmentManager.WEAPON] = item;
        this.tryOffhands(possibles, bestCard);
        // any = true;
        this.restore(mark);
      }

      // if ( any && <no unarmed items in shortlists> ) return;
      if (Maximizer.eval.melee < -1 || Maximizer.eval.melee > 1) {
        return;
      }
      this.equipment[EquipmentManager.WEAPON] = EquipmentRequest.UNEQUIP;
    } else if (!chefstaffable
        && EquipmentDatabase.getItemType(this.equipment[EquipmentManager.WEAPON].getItemId())
            .equals("chefstaff")) {
      return;
    }

    this.tryOffhands(possibles, bestCard);
    this.restore(mark);
  }

  public void tryOffhands(List<CheckedItem>[] possibles, AdventureResult bestCard)
      throws MaximizerInterruptedException {
    Object mark = this.mark();
    int weapon = this.equipment[EquipmentManager.WEAPON].getItemId();
    if (EquipmentDatabase.getHands(weapon) > 1) {
      this.equipment[EquipmentManager.OFFHAND] = EquipmentRequest.UNEQUIP;
    }

    if (this.equipment[EquipmentManager.OFFHAND] == null) {
      List<CheckedItem> possible;
      WeaponType weaponType = WeaponType.NONE;
      if (KoLCharacter.hasSkill("Double-Fisted Skull Smashing")) {
        weaponType = EquipmentDatabase.getWeaponType(weapon);
      }
      switch (weaponType) {
        case MELEE:
          possible = possibles[Evaluator.OFFHAND_MELEE];
          break;
        case RANGED:
          possible = possibles[Evaluator.OFFHAND_RANGED];
          break;
        default:
          possible = possibles[EquipmentManager.OFFHAND];
      }
      boolean any = false;

      for (int pos = 0; pos < possible.size(); ++pos) {
        AdventureResult item = possible.get(pos);
        int count = item.getCount();
        if (item.equals(this.equipment[EquipmentManager.WEAPON])) {
          --count;
        }
        if (item.equals(this.equipment[EquipmentManager.FAMILIAR])) {
          --count;
        }
        List group = ItemDatabase.getFoldGroup(item.getName());
        if (group != null && this.foldables) {
          String groupName = (String) group.get(1);
          for (int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot) {
            if (slot != EquipmentManager.OFFHAND && this.equipment[slot] != null) {
              List groupEquipped = ItemDatabase.getFoldGroup(this.equipment[slot].getName());
              if (groupEquipped != null && groupName.equals(groupEquipped.get(1))) {
                --count;
              }
            }
          }
        }
        if (count <= 0) continue;
        if (item.getItemId() == ItemPool.CARD_SLEEVE) {
          this.equipment[EquipmentManager.CARDSLEEVE] = bestCard;
        }
        this.equipment[EquipmentManager.OFFHAND] = item;
        this.tryOffhands(possibles, bestCard);
        any = true;
        this.restore(mark);
      }

      if (any && weapon > 0) return;
      this.equipment[EquipmentManager.OFFHAND] = EquipmentRequest.UNEQUIP;
    }

    // doit
    this.calculated = false;
    this.scored = false;
    this.tiebreakered = false;
    if (Maximizer.best == null) {
      RequestLogger.updateSessionLog(
          "Maximizer about to throw LimitExceeded because of null best.");
      // this isn't really what is happening but trying to understand why this is happening, first.
      throw new MaximizerLimitException();
    }
    if (this.compareTo(Maximizer.best) > 0) {
      Maximizer.best = (MaximizerSpeculation) this.clone();
    }
    Maximizer.bestChecked++;
    long t = System.currentTimeMillis();
    if (t > Maximizer.bestUpdate) {
      MaximizerSpeculation.showProgress();
      Maximizer.bestUpdate = t + 5000;
    }
    this.restore(mark);
    if (!KoLmafia.permitsContinue()) {
      throw new MaximizerInterruptedException();
    }
    if (this.exceeded) {
      throw new MaximizerExceededException();
    }
    long comboLimit = Preferences.getLong("maximizerCombinationLimit");
    if (comboLimit != 0 && Maximizer.bestChecked >= comboLimit) {
      throw new MaximizerLimitException();
    }
  }

  private static int getMutex(AdventureResult item) {
    Modifiers mods = Modifiers.getItemModifiers(item.getItemId());
    if (mods == null) {
      return 0;
    }
    return mods.getRawBitmap(Modifiers.MUTEX);
  }

  private void trySwap(int slot1, int slot2) {
    // If we are suggesting an accessory that's already being worn,
    // make sure we suggest the same slot (to minimize server hits).
    AdventureResult item1, item2, eq1, eq2;
    item1 = this.equipment[slot1];
    if (item1 == null) item1 = EquipmentRequest.UNEQUIP;
    eq1 = EquipmentManager.getEquipment(slot1);
    if (eq1.equals(item1)) return;
    item2 = this.equipment[slot2];
    if (item2 == null) item2 = EquipmentRequest.UNEQUIP;
    eq2 = EquipmentManager.getEquipment(slot2);
    if (eq2.equals(item2)) return;

    // The same thing applies to mutually exclusive accessories -
    // putting the new one in an earlier slot would cause an error
    // when the equipment is being changed.
    int imutex1, imutex2, emutex1, emutex2;
    imutex1 = getMutex(item1);
    emutex1 = getMutex(eq1);
    if ((imutex1 & emutex1) != 0) return;
    imutex2 = getMutex(item2);
    emutex2 = getMutex(eq2);
    if ((imutex2 & emutex2) != 0) return;

    if (eq1.equals(item2)
        || eq2.equals(item1)
        || (imutex1 & emutex2) != 0
        || (imutex2 & emutex1) != 0) {
      this.equipment[slot1] = item2;
      this.equipment[slot2] = item1;
    }
  }

  public static void showProgress() {
    StringBuilder msg = new StringBuilder();
    msg.append(Maximizer.bestChecked);
    msg.append(" combinations checked, best score ");
    double score = Maximizer.best.getScore();
    msg.append(KoLConstants.FLOAT_FORMAT.format(score));
    if (Maximizer.best.failed) {
      msg.append(" (FAIL)");
    }
    // if ( MaximizerFrame.best.tiebreakered )
    // {
    //	msg = msg + " / " + MaximizerFrame.best.getTiebreaker() + " / " +
    //		MaximizerFrame.best.simplicity;
    // }
    KoLmafia.updateDisplay(msg.toString());
  }
}
