package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.WeaponType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.Speculation;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase.FoldGroup;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

public class MaximizerSpeculation extends Speculation
    implements Comparable<MaximizerSpeculation>, Cloneable {
  private static final Slot[] CODPIECE_SLOTS = SlotSet.CODPIECE_SLOTS.toArray(Slot[]::new);
  private static final EnumSet<DoubleModifier> FAMILIAR_CALCULATION_DOUBLE_MODIFIERS =
      EnumSet.of(
          DoubleModifier.FAMILIAR_WEIGHT,
          DoubleModifier.HIDDEN_FAMILIAR_WEIGHT,
          DoubleModifier.FAMILIAR_WEIGHT_PCT,
          DoubleModifier.FAMILIAR_WEIGHT_CAP,
          DoubleModifier.VOLLEYBALL_WEIGHT,
          DoubleModifier.VOLLEYBALL_EFFECTIVENESS,
          DoubleModifier.FAMILIAR_TUNING_MUSCLE,
          DoubleModifier.FAMILIAR_TUNING_MYSTICALITY,
          DoubleModifier.FAMILIAR_TUNING_MOXIE,
          DoubleModifier.SOMBRERO_WEIGHT,
          DoubleModifier.SOMBRERO_BONUS,
          DoubleModifier.SOMBRERO_EFFECTIVENESS,
          DoubleModifier.LEPRECHAUN_WEIGHT,
          DoubleModifier.LEPRECHAUN_EFFECTIVENESS,
          DoubleModifier.FAIRY_WEIGHT,
          DoubleModifier.FAIRY_EFFECTIVENESS,
          DoubleModifier.FOOD_FAIRY_WEIGHT,
          DoubleModifier.FOOD_FAIRY_EFFECTIVENESS,
          DoubleModifier.BOOZE_FAIRY_WEIGHT,
          DoubleModifier.BOOZE_FAIRY_EFFECTIVENESS,
          DoubleModifier.CANDY_FAIRY_WEIGHT,
          DoubleModifier.CANDY_FAIRY_EFFECTIVENESS);

  boolean scored = false;
  private boolean tiebreakered = false;
  private boolean exceeded;
  private double score, tiebreaker;
  private int simplicity;
  private int beeosity;

  public boolean failed = false;
  public CheckedItem attachment;
  private boolean foldables = false;
  private CodpieceSearch codpieceSearch;
  private Map<FamiliarContributionKey, Map<DoubleModifier, Double>> familiarContributionCache =
      new HashMap<>();

  private static final class FamiliarContributionKey {
    private final int familiarId;
    private final int effectiveFamiliarId;
    private final int familiarWeight;
    private final boolean familiarFeasted;
    private final int familiarSoupWeight;
    private final int familiarItemId;
    private final int gemItemId;
    private final int copies;
    private final long[] calculationValues;
    private final int hashCode;

    private FamiliarContributionKey(
        FamiliarData familiar,
        AdventureResult familiarItem,
        int gemItemId,
        int copies,
        Modifiers baseline) {
      this.familiarId = familiar.getId();
      this.effectiveFamiliarId = familiar.getEffectiveId();
      this.familiarWeight = familiar.getUncappedWeight();
      this.familiarFeasted = familiar.getFeasted();
      this.familiarSoupWeight = familiar.getSoupWeight();
      this.familiarItemId = familiarItem == null ? -1 : familiarItem.getItemId();
      this.gemItemId = gemItemId;
      this.copies = copies;
      this.calculationValues = CodpiecePruning.familiarCalculationValues(baseline);
      int hash = Integer.hashCode(this.familiarId);
      hash = 31 * hash + Integer.hashCode(this.effectiveFamiliarId);
      hash = 31 * hash + Integer.hashCode(this.familiarWeight);
      hash = 31 * hash + Boolean.hashCode(this.familiarFeasted);
      hash = 31 * hash + Integer.hashCode(this.familiarSoupWeight);
      hash = 31 * hash + Integer.hashCode(this.familiarItemId);
      hash = 31 * hash + Integer.hashCode(this.gemItemId);
      hash = 31 * hash + Integer.hashCode(this.copies);
      this.hashCode = 31 * hash + Arrays.hashCode(this.calculationValues);
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof FamiliarContributionKey key
          && this.familiarId == key.familiarId
          && this.effectiveFamiliarId == key.effectiveFamiliarId
          && this.familiarWeight == key.familiarWeight
          && this.familiarFeasted == key.familiarFeasted
          && this.familiarSoupWeight == key.familiarSoupWeight
          && this.familiarItemId == key.familiarItemId
          && this.gemItemId == key.gemItemId
          && this.copies == key.copies
          && Arrays.equals(this.calculationValues, key.calculationValues);
    }

    @Override
    public int hashCode() {
      return this.hashCode;
    }
  }

  private static final class LateCodpieceCache {
    private final Modifiers baseline;
    private final Modifiers fightMods;
    private final List<Slot> slots;
    private final Modifiers[] gemModifiers;
    private final boolean[] familiarDependentGems;
    private final CodpiecePruning.FamiliarScoreContributions familiarScoreContributions;
    private final Modifiers[] slotModifiers;
    private final int[] slotGemIndexes;
    private final Map<List<Integer>, KoLCharacter.AdjustmentPrefix> prefixes;
    private int familiarSlots;

    private LateCodpieceCache(
        Modifiers baseline,
        Modifiers fightMods,
        List<Slot> slots,
        Modifiers[] gemModifiers,
        boolean[] familiarDependentGems,
        CodpiecePruning.FamiliarScoreContributions familiarScoreContributions,
        Map<List<Integer>, KoLCharacter.AdjustmentPrefix> prefixes) {
      this.baseline = baseline;
      this.fightMods = fightMods;
      this.slots = slots;
      this.gemModifiers = gemModifiers;
      this.familiarDependentGems = familiarDependentGems;
      this.familiarScoreContributions = familiarScoreContributions;
      this.slotModifiers = new Modifiers[slots.size()];
      this.slotGemIndexes = new int[slots.size()];
      this.prefixes = prefixes;
    }

    private void select(int slotIndex, int gemIndex) {
      this.slotModifiers[slotIndex] = this.gemModifiers[gemIndex];
      this.slotGemIndexes[slotIndex] = gemIndex + 1;
      if (this.familiarDependentGems[gemIndex]) {
        this.familiarSlots++;
      }
    }

    private void deselect(int slotIndex) {
      int gemIndex = this.slotGemIndexes[slotIndex] - 1;
      if (gemIndex >= 0 && this.familiarDependentGems[gemIndex]) {
        this.familiarSlots--;
      }
      this.slotModifiers[slotIndex] = null;
      this.slotGemIndexes[slotIndex] = 0;
    }

    private KoLCharacter.AdjustmentPrefix getPrefix(
        Supplier<KoLCharacter.AdjustmentPrefix> factory) {
      List<Integer> familiarGemIndexes = new ArrayList<>();
      for (int encodedGemIndex : this.slotGemIndexes) {
        if (encodedGemIndex == 0) {
          continue;
        }
        int gemIndex = encodedGemIndex - 1;
        if (this.familiarDependentGems[gemIndex]) {
          familiarGemIndexes.add(gemIndex);
        }
      }
      return this.prefixes.computeIfAbsent(
          List.copyOf(familiarGemIndexes), ignored -> factory.get());
    }
  }

  @Override
  public MaximizerSpeculation clone() {
    try {
      MaximizerSpeculation copy = (MaximizerSpeculation) super.clone();
      copy.equipment = this.equipment.clone();
      copy.setModeables(new EnumMap<>(this.getModeables()));
      if (this.mods != null) {
        copy.mods = new Modifiers(this.mods);
      }
      copy.codpieceSearch = null;
      copy.familiarContributionCache = new HashMap<>();
      return copy;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  @Override
  public Modifiers calculate() {
    LateCodpieceCache cache = this.codpieceSearch == null ? null : this.codpieceSearch.cache;
    if (cache == null) {
      return super.calculate();
    }

    Modifiers newModifiers;
    Modifiers fightMods;
    if (cache.familiarSlots == 0) {
      newModifiers = new Modifiers(cache.baseline);
      for (Modifiers gemModifiers : cache.slotModifiers) {
        newModifiers.add(gemModifiers);
      }
      fightMods = cache.fightMods;
    } else {
      var prefix = cache.getPrefix(this::primeFamiliarCodpiecePrefix);
      newModifiers = new Modifiers(prefix.modifiers());
      for (int encodedGemIndex : cache.slotGemIndexes) {
        if (encodedGemIndex == 0) {
          continue;
        }
        int gemIndex = encodedGemIndex - 1;
        if (!cache.familiarDependentGems[gemIndex]) {
          newModifiers.add(cache.gemModifiers[gemIndex]);
        }
      }
      fightMods = prefix.fightMods();
    }
    this.mods =
        KoLCharacter.applyAdjustmentSuffix(
            false, newModifiers, fightMods, this.equipment, this.getEffects(), true);
    this.calculated = true;
    return this.mods;
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
    this.tiebreakered = false;
    this.calculated = false;
  }

  public double getScore() {
    if (this.scored) return this.score;
    if (!this.calculated) this.calculate();
    this.score = Maximizer.eval.getScore(this.mods, this.equipment, this.getModeables());
    if (KoLCharacter.inBeecore()) {
      this.beeosity = KoLCharacter.getBeeosity(this.equipment);
    }
    Maximizer.eval.checkEquipment(this.mods, this.equipment, this.beeosity);
    this.failed = Maximizer.eval.failed;
    if ((this.mods.getRawBitmap(BitmapModifier.MUTEX_VIOLATIONS)
            & ~KoLCharacter.currentRawBitmapModifier(BitmapModifier.MUTEX_VIOLATIONS))
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
    for (var slot : SlotSet.ALL_SLOTS) {
      AdventureResult item = this.equipment.get(slot);
      if (item == null) item = EquipmentRequest.UNEQUIP;
      if (EquipmentManager.getEquipment(slot).equals(item)) {
        this.simplicity += 2;
      } else if (item.equals(EquipmentRequest.UNEQUIP)) {
        this.simplicity += slot == Slot.WEAPON ? -1 : 1;
      }
    }
    // When an equipment-type keyword (club, sword, shield, etc.) is active, prefer any
    // qualifying item over leaving the slot empty: give it a nudge above UNEQUIP's +2.
    if (Maximizer.eval.isWeaponTypeRequired()) {
      AdventureResult weapon = this.equipment.get(Slot.WEAPON);
      if (weapon != null && !weapon.equals(EquipmentRequest.UNEQUIP)) {
        this.simplicity += 3;
      }
    }
    if (Maximizer.eval.isShieldRequired()) {
      AdventureResult offhand = this.equipment.get(Slot.OFFHAND);
      if (offhand != null && !offhand.equals(EquipmentRequest.UNEQUIP)) {
        this.simplicity += 3;
      }
    }
    return this.tiebreaker;
  }

  @Override
  public int compareTo(MaximizerSpeculation o) {
    if (o == null) return 1;
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
    for (var equip : this.equipment.values()) {
      if (equip == null) continue;
      int itemId = equip.getItemId();
      Modifiers mods = ModifierDatabase.getItemModifiers(itemId);
      if (mods == null) continue;
      if (mods.hasString(StringModifier.ROLLOVER_EFFECT)) countThisEffects++;
      if (mods.getBoolean(BooleanModifier.BREAKABLE)) countThisBreakables++;
      if (mods.getBoolean(BooleanModifier.DROPS_ITEMS)) countThisDropsItems++;
      if (mods.getBoolean(BooleanModifier.DROPS_MEAT)) countThisDropsMeat++;
    }
    for (var equip : other.equipment.values()) {
      if (equip == null) continue;
      int itemId = equip.getItemId();
      Modifiers mods = ModifierDatabase.getItemModifiers(itemId);
      if (mods == null) continue;
      if (mods.hasString(StringModifier.ROLLOVER_EFFECT)) countOtherEffects++;
      if (mods.getBoolean(BooleanModifier.BREAKABLE)) countOtherBreakables++;
      if (mods.getBoolean(BooleanModifier.DROPS_ITEMS)) countOtherDropsItems++;
      if (mods.getBoolean(BooleanModifier.DROPS_MEAT)) countOtherDropsMeat++;
    }
    // Prefer item droppers
    if (Maximizer.eval.isUsingTiebreaker() && countThisDropsItems != countOtherDropsItems) {
      return countThisDropsItems > countOtherDropsItems ? 1 : -1;
    }
    // Prefer meat droppers
    if (Maximizer.eval.isUsingTiebreaker() && countThisDropsMeat != countOtherDropsMeat) {
      return countThisDropsMeat > countOtherDropsMeat ? 1 : -1;
    }
    // Prefer higher tiebreaker account (unless -tie used)
    rv = Double.compare(this.getTiebreaker(), other.getTiebreaker());
    if (rv != 0) return rv;
    // Prefer rollover effects
    if (Maximizer.eval.isUsingTiebreaker() && countThisEffects != countOtherEffects) {
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
  public EnumMap<Slot, AdventureResult> mark() {
    return this.equipment.clone();
  }

  public void restore(EnumMap<Slot, AdventureResult> mark) {
    this.equipment.putAll(mark);
  }

  private int availableCount(AdventureResult item, Slot foldTarget, Slot... duplicateSlots) {
    int count = item.getCount();
    for (Slot slot : duplicateSlots) {
      if (item.equals(this.equipment.get(slot))) {
        --count;
      }
    }
    FoldGroup group = ItemDatabase.getFoldGroup(item.getName());
    if (group == null || !this.foldables) {
      return count;
    }
    String groupName = group.names.get(0);
    for (Slot slot : SlotSet.SLOTS) {
      if (slot == foldTarget || this.equipment.get(slot) == null) {
        continue;
      }
      FoldGroup equippedGroup = ItemDatabase.getFoldGroup(this.equipment.get(slot).getName());
      if (equippedGroup != null && groupName.equals(equippedGroup.names.get(0))) {
        --count;
      }
    }
    return count;
  }

  public void tryAll(
      List<FamiliarData> familiars,
      List<FamiliarData> enthronedFamiliars,
      Map<Integer, Boolean> usefulOutfits,
      Map<AdventureResult, AdventureResult> outfitPieces,
      SlotList<CheckedItem> possibles,
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
      possibles.set(Slot.FAMILIAR, possibles.getFamiliar(i));
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
      Map<Integer, Boolean> usefulOutfits,
      Map<AdventureResult, AdventureResult> outfitPieces,
      SlotList<CheckedItem> possibles,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar,
      FamiliarData useBjornFamiliar)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    for (Integer outfit : usefulOutfits.keySet()) {
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
        Slot slot = EquipmentManager.itemIdToEquipmentType(item.getItemId());

        switch (slot) {
          case HAT:
          case PANTS:
          case SHIRT:
          case CONTAINER:
            if (item.equals(this.equipment.get(slot))) { // already worn
              continue pieceloop;
            }
            if (item.equals(this.equipment.get(Slot.FAMILIAR))) {
              --count;
            }
            break;
          case WEAPON:
          case OFFHAND:
            if (item.equals(this.equipment.get(Slot.WEAPON))
                || item.equals(this.equipment.get(Slot.OFFHAND))) { // already worn
              continue pieceloop;
            }
            if (item.equals(this.equipment.get(Slot.FAMILIAR))) {
              --count;
            }
            break;
          case ACCESSORY1:
            if (item.equals(this.equipment.get(Slot.ACCESSORY1))
                || item.equals(this.equipment.get(Slot.ACCESSORY2))
                || item.equals(this.equipment.get(Slot.ACCESSORY3))) { // already worn
              continue pieceloop;
            }
            if (item.equals(this.equipment.get(Slot.FAMILIAR))) {
              --count;
            }
            if (this.equipment.get(Slot.ACCESSORY3) == null) {
              slot = Slot.ACCESSORY3;
            } else if (this.equipment.get(Slot.ACCESSORY2) == null) {
              slot = Slot.ACCESSORY2;
            }
            break;
          default:
            break pieceloop; // don't know how to wear that
        }

        if (count <= 0) break; // none available
        if (this.equipment.get(slot) != null) break; // slot taken
        this.equipment.put(slot, item);
      }
      this.restore(mark);
    }

    this.tryFamiliarItems(
        enthronedFamiliars, possibles, bestCard, useCrownFamiliar, useBjornFamiliar);
  }

  public void tryFamiliarItems(
      List<FamiliarData> enthronedFamiliars,
      SlotList<CheckedItem> possibles,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar,
      FamiliarData useBjornFamiliar)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    if (this.equipment.get(Slot.FAMILIAR) == null) {
      List<CheckedItem> possible = possibles.get(Slot.FAMILIAR);
      boolean any = false;
      for (AdventureResult item : possible) {
        int count =
            this.availableCount(
                item, Slot.FAMILIAR, Slot.OFFHAND, Slot.WEAPON, Slot.HAT, Slot.PANTS);
        if (count <= 0) continue;
        this.equipment.put(Slot.FAMILIAR, item);
        this.tryContainers(
            enthronedFamiliars, possibles, bestCard, useCrownFamiliar, useBjornFamiliar);
        any = true;
        this.restore(mark);
      }

      if (any) return;
      this.equipment.put(Slot.FAMILIAR, EquipmentRequest.UNEQUIP);
    }

    this.tryContainers(enthronedFamiliars, possibles, bestCard, useCrownFamiliar, useBjornFamiliar);
    this.restore(mark);
  }

  public void tryContainers(
      List<FamiliarData> enthronedFamiliars,
      SlotList<CheckedItem> possibles,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar,
      FamiliarData useBjornFamiliar)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    if (this.equipment.get(Slot.CONTAINER) == null) {
      List<CheckedItem> possible = possibles.get(Slot.CONTAINER);
      boolean any = false;
      for (CheckedItem item : possible) {
        int count = this.availableCount(item, Slot.CONTAINER);
        if (count <= 0) continue;
        this.equipment.put(Slot.CONTAINER, item);
        if (item.getItemId() == ItemPool.BUDDY_BJORN) {
          if (useBjornFamiliar != null) {
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
      this.equipment.put(Slot.CONTAINER, EquipmentRequest.UNEQUIP);
    }

    this.tryAccessories(enthronedFamiliars, possibles, 0, bestCard, useCrownFamiliar);
    this.restore(mark);
  }

  public void tryAccessories(
      List<FamiliarData> enthronedFamiliars,
      SlotList<CheckedItem> possibles,
      int pos,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    int free = 0;
    if (this.equipment.get(Slot.ACCESSORY1) == null) ++free;
    if (this.equipment.get(Slot.ACCESSORY2) == null) ++free;
    if (this.equipment.get(Slot.ACCESSORY3) == null) ++free;
    if (free > 0) {
      List<CheckedItem> possible = possibles.get(Slot.ACCESSORY1);
      boolean any = false;
      for (; pos < possible.size(); ++pos) {
        AdventureResult item = possible.get(pos);
        int count =
            this.availableCount(item, Slot.NONE, Slot.ACCESSORY1, Slot.ACCESSORY2, Slot.ACCESSORY3);
        if (count <= 0) continue;
        for (count = Math.min(free, count); count > 0; --count) {
          if (this.equipment.get(Slot.ACCESSORY1) == null) {
            this.equipment.put(Slot.ACCESSORY1, item);
          } else if (this.equipment.get(Slot.ACCESSORY2) == null) {
            this.equipment.put(Slot.ACCESSORY2, item);
          } else if (this.equipment.get(Slot.ACCESSORY3) == null) {
            this.equipment.put(Slot.ACCESSORY3, item);
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

      if (this.equipment.get(Slot.ACCESSORY1) == null) {
        this.equipment.put(Slot.ACCESSORY1, EquipmentRequest.UNEQUIP);
      }
      if (this.equipment.get(Slot.ACCESSORY2) == null) {
        this.equipment.put(Slot.ACCESSORY2, EquipmentRequest.UNEQUIP);
      }
      if (this.equipment.get(Slot.ACCESSORY3) == null) {
        this.equipment.put(Slot.ACCESSORY3, EquipmentRequest.UNEQUIP);
      }
    }

    this.trySwap(Slot.ACCESSORY1, Slot.ACCESSORY2);
    this.trySwap(Slot.ACCESSORY2, Slot.ACCESSORY3);
    this.trySwap(Slot.ACCESSORY3, Slot.ACCESSORY1);
    this.trySwap(Slot.ACCESSORY1, Slot.ACCESSORY2);

    this.tryHats(enthronedFamiliars, possibles, bestCard, useCrownFamiliar);
    this.restore(mark);
  }

  public void tryHats(
      List<FamiliarData> enthronedFamiliars,
      SlotList<CheckedItem> possibles,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    if (this.equipment.get(Slot.HAT) == null) {
      List<CheckedItem> possible = possibles.get(Slot.HAT);
      boolean any = false;
      for (CheckedItem item : possible) {
        int count = this.availableCount(item, Slot.HAT, Slot.FAMILIAR);
        if (count <= 0) continue;
        this.equipment.put(Slot.HAT, item);
        if (item.getItemId() == ItemPool.HATSEAT) {
          if (useCrownFamiliar != null) {
            this.setEnthroned(useCrownFamiliar);
            this.tryShirts(possibles, bestCard);
            any = true;
            this.restore(mark);
          } else {
            for (FamiliarData f : enthronedFamiliars) {
              // Cannot use same familiar for this and Bjorn
              if (f != this.getBjorned() || f == FamiliarData.NO_FAMILIAR) {
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
      this.equipment.put(Slot.HAT, EquipmentRequest.UNEQUIP);
    }

    this.tryShirts(possibles, bestCard);
    this.restore(mark);
  }

  public void tryShirts(SlotList<CheckedItem> possibles, AdventureResult bestCard)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    if (this.equipment.get(Slot.SHIRT) == null) {
      boolean any = false;
      if (KoLCharacter.isTorsoAware()) {
        List<CheckedItem> possible = possibles.get(Slot.SHIRT);
        for (AdventureResult item : possible) {
          int count = this.availableCount(item, Slot.SHIRT, Slot.FAMILIAR);
          if (count <= 0) continue;
          this.equipment.put(Slot.SHIRT, item);
          this.tryPants(possibles, bestCard);
          any = true;
          this.restore(mark);
        }
      }

      if (any) return;
      this.equipment.put(Slot.SHIRT, EquipmentRequest.UNEQUIP);
    }

    this.tryPants(possibles, bestCard);
    this.restore(mark);
  }

  public void tryPants(SlotList<CheckedItem> possibles, AdventureResult bestCard)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    if (this.equipment.get(Slot.PANTS) == null) {
      List<CheckedItem> possible = possibles.get(Slot.PANTS);
      boolean any = false;
      for (AdventureResult item : possible) {
        int count = this.availableCount(item, Slot.PANTS, Slot.FAMILIAR);
        if (count <= 0) continue;
        this.equipment.put(Slot.PANTS, item);
        this.trySixguns(possibles, bestCard);
        any = true;
        this.restore(mark);
      }

      if (any) return;
      this.equipment.put(Slot.PANTS, EquipmentRequest.UNEQUIP);
    }

    this.trySixguns(possibles, bestCard);
    this.restore(mark);
  }

  public void trySixguns(SlotList<CheckedItem> possibles, AdventureResult bestCard)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    if (this.equipment.get(Slot.HOLSTER) == null) {
      List<CheckedItem> possible = possibles.get(Slot.HOLSTER);
      boolean any = false;
      for (AdventureResult item : possible) {
        int count = item.getCount();
        if (count <= 0) continue;
        this.equipment.put(Slot.HOLSTER, item);
        this.tryWeapons(possibles, bestCard);
        any = true;
        this.restore(mark);
      }

      if (any) return;
      this.equipment.put(Slot.HOLSTER, EquipmentRequest.UNEQUIP);
    }

    this.tryWeapons(possibles, bestCard);
    this.restore(mark);
  }

  public void tryWeapons(SlotList<CheckedItem> possibles, AdventureResult bestCard)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    boolean chefstaffable =
        KoLCharacter.hasSkill(SkillPool.SPIRIT_OF_RIGATONI) || KoLCharacter.isJarlsberg();
    if (!chefstaffable && KoLCharacter.isSauceror()) {
      chefstaffable =
          this.equipment.get(Slot.ACCESSORY1).getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE
              || this.equipment.get(Slot.ACCESSORY2).getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE
              || this.equipment.get(Slot.ACCESSORY3).getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE;
    }
    if (this.equipment.get(Slot.WEAPON) == null) {
      List<CheckedItem> possible = possibles.get(Slot.WEAPON);
      // boolean any = false;
      for (AdventureResult item : possible) {
        if (!chefstaffable && EquipmentDatabase.getItemType(item.getItemId()).equals("chefstaff")) {
          continue;
        }
        int count = this.availableCount(item, Slot.WEAPON, Slot.OFFHAND, Slot.FAMILIAR);
        if (count <= 0) continue;
        this.equipment.put(Slot.WEAPON, item);
        this.tryOffhands(possibles, bestCard);
        // any = true;
        this.restore(mark);
      }

      // if ( any && <no unarmed items in shortlists> ) return;
      if (Maximizer.eval.melee < -1 || Maximizer.eval.melee > 1) {
        return;
      }
      this.equipment.put(Slot.WEAPON, EquipmentRequest.UNEQUIP);
    } else if (!chefstaffable
        && EquipmentDatabase.getItemType(this.equipment.get(Slot.WEAPON).getItemId())
            .equals("chefstaff")) {
      return;
    }

    this.tryOffhands(possibles, bestCard);
    this.restore(mark);
  }

  public void tryOffhands(SlotList<CheckedItem> possibles, AdventureResult bestCard)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    int weapon = this.equipment.get(Slot.WEAPON).getItemId();
    if (EquipmentDatabase.getHands(weapon) > 1) {
      this.equipment.put(Slot.OFFHAND, EquipmentRequest.UNEQUIP);
    }

    if (this.equipment.get(Slot.OFFHAND) == null) {
      List<CheckedItem> possible;
      WeaponType weaponType = WeaponType.NONE;
      if (KoLCharacter.hasSkill(SkillPool.DOUBLE_FISTED_SKULL_SMASHING)) {
        weaponType = EquipmentDatabase.getWeaponType(weapon);
      }
      possible =
          switch (weaponType) {
            case MELEE -> possibles.get(Evaluator.OFFHAND_MELEE);
            case RANGED -> possibles.get(Evaluator.OFFHAND_RANGED);
            default -> possibles.get(Slot.OFFHAND);
          };
      boolean any = false;

      for (AdventureResult item : possible) {
        int count = this.availableCount(item, Slot.OFFHAND, Slot.WEAPON, Slot.FAMILIAR);
        if (count <= 0) continue;
        if (item.getItemId() == ItemPool.CARD_SLEEVE) {
          this.equipment.put(Slot.CARDSLEEVE, bestCard);
        }
        this.equipment.put(Slot.OFFHAND, item);
        this.tryOffhands(possibles, bestCard);
        any = true;
        this.restore(mark);
      }

      if (any && weapon > 0) return;
      this.equipment.put(Slot.OFFHAND, EquipmentRequest.UNEQUIP);
    }

    boolean wearingCodpiece =
        this.equipment.values().stream()
            .anyMatch(item -> item != null && item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE);
    if (!wearingCodpiece) {
      this.releaseCodpieceGemsNeededElsewhere();
      this.checkBest();
      this.restore(mark);
      return;
    }

    List<Slot> codpieceSlots =
        SlotSet.CODPIECE_SLOTS.stream().filter(Maximizer.eval::slotEnabled).toList();
    for (Slot slot : codpieceSlots) {
      this.equipment.put(slot, EquipmentRequest.UNEQUIP);
    }
    if (!this.hasEnoughCodpieceGems()) {
      this.restore(mark);
      return;
    }

    List<CheckedItem> codpieceGems =
        possibles.get(Slot.CODPIECE1).stream()
            .filter(gem -> gem.getCount() > 0 && EquipmentRequest.isCodpieceGem(gem.getItemId()))
            .filter(gem -> this.countEquipped(gem.getItemId()) < gem.getCount())
            .toList();
    try {
      boolean canCollapseSaturatedScore =
          !Maximizer.eval.isUsingTiebreaker()
              && !KoLCharacter.inBeecore()
              && codpieceSlots.stream()
                  .allMatch(
                      slot -> EquipmentManager.getEquipment(slot).equals(EquipmentRequest.UNEQUIP));
      LateCodpieceCache cache =
          this.canUseLateCodpieceCache(codpieceGems, codpieceSlots)
              ? this.primeLateCodpieceCache(codpieceGems, codpieceSlots)
              : null;
      CodpieceSearch search =
          new CodpieceSearch(codpieceGems, codpieceSlots, cache, canCollapseSaturatedScore);
      this.codpieceSearch = search;
      this.calculated = false;
      if (Maximizer.eval.isUsingTiebreaker()
          && Maximizer.eval.areScoreModifiersSaturated(this.calculate())) {
        codpieceGems = Maximizer.eval.prioritizeCodpieceGems(codpieceGems);
        cache = this.primeLateCodpieceCache(codpieceGems, codpieceSlots);
        search = new CodpieceSearch(codpieceGems, codpieceSlots, cache, false);
      }
      this.codpieceSearch = search;
      this.codpieceSearch.run();
    } finally {
      this.codpieceSearch = null;
      this.restore(mark);
    }
  }

  private void releaseCodpieceGemsNeededElsewhere() {
    for (Slot slot : CODPIECE_SLOTS) {
      if (!Maximizer.eval.slotEnabled(slot)) {
        continue;
      }

      AdventureResult gem = this.equipment.get(slot);
      if (gem == null || gem.equals(EquipmentRequest.UNEQUIP)) {
        continue;
      }

      CheckedItem equippedElsewhere =
          this.equipment.entrySet().stream()
              .filter(entry -> !SlotSet.CODPIECE_SLOTS.contains(entry.getKey()))
              .map(Map.Entry::getValue)
              .filter(gem::equals)
              .filter(CheckedItem.class::isInstance)
              .map(CheckedItem.class::cast)
              .findFirst()
              .orElse(null);
      if (equippedElsewhere == null) {
        continue;
      }

      long used = this.equipment.values().stream().filter(gem::equals).count();
      if (used > equippedElsewhere.getAvailableCount()) {
        this.equipment.put(slot, EquipmentRequest.UNEQUIP);
      }
    }
  }

  private boolean canUseLateCodpieceCache(List<CheckedItem> possibles, List<Slot> slots) {
    for (Slot slot : slots) {
      AdventureResult equipped = this.equipment.get(slot);
      if (equipped != null
          && equipped != EquipmentRequest.UNEQUIP
          && !this.isSafeLateCodpieceGem(equipped.getItemId())) {
        return false;
      }
    }

    for (CheckedItem possible : possibles) {
      if (!this.isSafeLateCodpieceGem(possible.getItemId())) {
        return false;
      }
    }

    return true;
  }

  private LateCodpieceCache primeLateCodpieceCache(List<CheckedItem> possibles, List<Slot> slots) {
    var mark = this.mark();
    try {
      for (Slot slot : slots) {
        this.equipment.put(slot, EquipmentRequest.UNEQUIP);
      }

      var prefix = this.recalculateCodpiecePrefix(this.equipment);
      Modifiers[] gemModifiers = new Modifiers[possibles.size()];
      boolean[] familiarDependentGems = new boolean[possibles.size()];
      for (int i = 0; i < possibles.size(); i++) {
        gemModifiers[i] =
            ModifierDatabase.getModifiers(
                ModifierType.ETERNITY_CODPIECE, possibles.get(i).getItemId());
        familiarDependentGems[i] = CodpiecePruning.affectsFamiliarCalculation(gemModifiers[i]);
      }
      Map<List<Integer>, KoLCharacter.AdjustmentPrefix> prefixes = new HashMap<>();
      prefixes.put(List.of(), prefix);
      CodpiecePruning.FamiliarScoreContributions familiarScoreContributions =
          this.findFamiliarScoreContributions(
              possibles, slots, prefix.modifiers(), familiarDependentGems);
      return new LateCodpieceCache(
          prefix.modifiers(),
          prefix.fightMods(),
          slots,
          gemModifiers,
          familiarDependentGems,
          familiarScoreContributions,
          prefixes);
    } finally {
      this.restore(mark);
    }
  }

  private CodpiecePruning.FamiliarScoreContributions findFamiliarScoreContributions(
      List<CheckedItem> possibles,
      List<Slot> slots,
      Modifiers baseline,
      boolean[] familiarDependentGems) {
    EnumSet<DoubleModifier> scored = Maximizer.eval.familiarDependentScoreModifiers();
    if (scored.isEmpty()) {
      return new CodpiecePruning.FamiliarScoreContributions(-1, Map.of());
    }
    int familiarGemIndex = -1;
    for (int i = 0; i < familiarDependentGems.length; i++) {
      if (!familiarDependentGems[i]) {
        continue;
      }
      if (familiarGemIndex != -1) {
        return null;
      }
      familiarGemIndex = i;
    }
    if (familiarGemIndex == -1) {
      return new CodpiecePruning.FamiliarScoreContributions(-1, Map.of());
    }

    Map<DoubleModifier, Double> ceilings = new EnumMap<>(DoubleModifier.class);
    Map<DoubleModifier, Double> previous = new EnumMap<>(DoubleModifier.class);
    for (DoubleModifier modifier : scored) {
      previous.put(modifier, Evaluator.scoreValue(modifier, baseline, null));
    }
    CheckedItem gem = possibles.get(familiarGemIndex);
    int copies = Math.min(slots.size(), gem.getCount() - (int) this.countEquipped(gem.getItemId()));
    var key =
        new FamiliarContributionKey(
            this.getFamiliar(),
            this.equipment.get(Slot.FAMILIAR),
            gem.getItemId(),
            copies,
            baseline);
    var cached = this.familiarContributionCache.get(key);
    if (cached != null) {
      return new CodpiecePruning.FamiliarScoreContributions(familiarGemIndex, cached);
    }

    for (int copy = 0; copy < copies; copy++) {
      this.equipment.put(slots.get(copy), gem);
      var prefix = this.recalculateCodpiecePrefix(this.equipment);
      for (DoubleModifier modifier : scored) {
        double value = Evaluator.scoreValue(modifier, prefix.modifiers(), null);
        double contribution = value - previous.put(modifier, value);
        if (contribution == 0.0) {
          continue;
        }
        ceilings.merge(modifier, contribution, Math::max);
      }
    }
    for (Slot slot : slots) {
      this.equipment.put(slot, EquipmentRequest.UNEQUIP);
    }
    var cachedCeilings = Map.copyOf(ceilings);
    this.familiarContributionCache.put(key, cachedCeilings);
    return new CodpiecePruning.FamiliarScoreContributions(familiarGemIndex, cachedCeilings);
  }

  private KoLCharacter.AdjustmentPrefix primeFamiliarCodpiecePrefix() {
    LateCodpieceCache cache = this.codpieceSearch.cache;
    Map<Slot, AdventureResult> equipment = new EnumMap<>(this.equipment);
    for (int slotIndex = 0; slotIndex < cache.slots.size(); slotIndex++) {
      int encodedGemIndex = cache.slotGemIndexes[slotIndex];
      if (encodedGemIndex == 0 || !cache.familiarDependentGems[encodedGemIndex - 1]) {
        equipment.put(cache.slots.get(slotIndex), EquipmentRequest.UNEQUIP);
      }
    }

    return this.recalculateCodpiecePrefix(equipment);
  }

  private KoLCharacter.AdjustmentPrefix recalculateCodpiecePrefix(
      Map<Slot, AdventureResult> equipment) {
    return KoLCharacter.recalculateAdjustmentsPrefix(
        false,
        this.getMindControlLevel(),
        equipment,
        this.getEffects(),
        this.getFamiliar(),
        this.getEnthroned(),
        this.getBjorned(),
        this.getCustom(),
        this.getHorsery(),
        this.getBoomBox(),
        this.getModeables(),
        true);
  }

  private boolean isSafeLateCodpieceGem(int itemId) {
    return CodpiecePruning.hasOnlySupportedLateCalculationModifiers(
        ModifierDatabase.getModifiers(ModifierType.ETERNITY_CODPIECE, itemId));
  }

  private int countEquipmentWith(BooleanModifier modifier) {
    int count = 0;
    for (AdventureResult item : this.equipment.values()) {
      if (item == null) {
        continue;
      }
      Modifiers modifiers = ModifierDatabase.getItemModifiers(item.getItemId());
      if (modifiers != null && modifiers.getBoolean(modifier)) {
        count++;
      }
    }
    return count;
  }

  private final class CodpieceSearch {
    private final List<CheckedItem> gems;
    private final List<Slot> slots;
    private final int[] remaining;
    private final int[] initialRemaining;
    private final boolean[] required;
    private final int requiredCount;
    private final LateCodpieceCache cache;
    private final boolean canCollapseSaturatedScore;
    private CodpiecePruning.BranchBounds bounds;
    private boolean tiebreakBoundInitialized;

    private CodpieceSearch(
        List<CheckedItem> gems,
        List<Slot> slots,
        LateCodpieceCache cache,
        boolean canCollapseSaturatedScore) {
      this.gems = gems;
      this.slots = slots;
      this.remaining = new int[gems.size()];
      this.required = new boolean[gems.size()];
      this.cache = cache;
      this.canCollapseSaturatedScore = canCollapseSaturatedScore;

      int requiredCount = 0;
      for (int i = 0; i < gems.size(); i++) {
        CheckedItem gem = gems.get(i);
        long used = MaximizerSpeculation.this.countEquipped(gem.getItemId());
        this.remaining[i] = gem.getCount() - (int) used;
        if (gem.requiredFlag && used == 0) {
          this.required[i] = true;
          requiredCount++;
        }
      }
      this.initialRemaining = this.remaining.clone();
      this.requiredCount = requiredCount;
    }

    private void run() throws MaximizerInterruptedException {
      CodpiecePruning.ScoreUpperBound scoreUpperBound = this.createScoreUpperBound();
      this.bounds =
          new CodpiecePruning.BranchBounds(
              scoreUpperBound,
              null,
              new CodpiecePruning.BooleanUpperBound(
                  this.gems, this.remaining, this.slots.size(), BooleanModifier.DROPS_ITEMS),
              new CodpiecePruning.BooleanUpperBound(
                  this.gems, this.remaining, this.slots.size(), BooleanModifier.DROPS_MEAT));
      this.search(0, 0, this.requiredCount);
    }

    private CodpiecePruning.ScoreUpperBound createScoreUpperBound() {
      return this.cache == null
          ? null
          : Maximizer.eval.createTheoreticalCodpieceScoreUpperBound(
              this.cache.baseline,
              this.cache.gemModifiers,
              this.remaining,
              this.slots.size(),
              MaximizerSpeculation.this.equipment,
              MaximizerSpeculation.this.getModeables(),
              this.gems,
              this.cache.familiarScoreContributions);
    }

    private void search(int start, int slotIndex, int requiredCount)
        throws MaximizerInterruptedException {
      boolean scoreSaturated = false;
      boolean canMeetRequirements = true;
      if (this.bounds.score() != null) {
        int remainingSlots = this.slots.size() - slotIndex;
        double upperScore = this.bounds.score().estimate(start, this.remaining, remainingSlots);
        canMeetRequirements &=
            this.bounds.score().canMeetMinimum(start, this.remaining, remainingSlots, upperScore);
        if (!Maximizer.best.failed || !canMeetRequirements) {
          double bestScore = Maximizer.best.getScore();
          if (upperScore < bestScore) {
            return;
          }
          if (Double.compare(upperScore, bestScore) == 0 && !KoLCharacter.inBeecore()) {
            CodpiecePruning.ScoreUpperBound tiebreakUpperBound =
                this.getTiebreakUpperBound(slotIndex);
            int bestItemDroppers = Maximizer.best.countEquipmentWith(BooleanModifier.DROPS_ITEMS);
            int itemDropperCeiling =
                MaximizerSpeculation.this.countEquipmentWith(BooleanModifier.DROPS_ITEMS)
                    + this.bounds
                        .itemDroppers()
                        .estimateAdditional(start, this.remaining, remainingSlots);
            if (itemDropperCeiling < bestItemDroppers) {
              return;
            }
            if (itemDropperCeiling == bestItemDroppers && tiebreakUpperBound != null) {
              int bestMeatDroppers = Maximizer.best.countEquipmentWith(BooleanModifier.DROPS_MEAT);
              int meatDropperCeiling =
                  MaximizerSpeculation.this.countEquipmentWith(BooleanModifier.DROPS_MEAT)
                      + this.bounds
                          .meatDroppers()
                          .estimateAdditional(start, this.remaining, remainingSlots);
              if (meatDropperCeiling < bestMeatDroppers
                  || (meatDropperCeiling == bestMeatDroppers
                      && tiebreakUpperBound.estimate(start, this.remaining, remainingSlots)
                          < Maximizer.best.getTiebreaker())) {
                return;
              }
            }
          }
        }
        scoreSaturated = this.bounds.score().isScoreSaturated(start, this.remaining, upperScore);
      }
      if (requiredCount == 0) {
        MaximizerSpeculation.this.checkBest(true);
        if (this.canCollapseSaturatedScore
            && scoreSaturated
            && (!MaximizerSpeculation.this.failed || !canMeetRequirements)) {
          return;
        }
      }
      if (slotIndex == this.slots.size() || requiredCount > this.slots.size() - slotIndex) {
        return;
      }

      int firstRequired = -1;
      for (int i = start; i < this.required.length; i++) {
        if (this.required[i]) {
          firstRequired = i;
          break;
        }
      }

      Slot slot = this.slots.get(slotIndex);
      for (int i = start; i < this.gems.size(); i++) {
        if (firstRequired != -1 && i > firstRequired) {
          break;
        }
        if (this.remaining[i] == 0) {
          continue;
        }

        boolean satisfiesRequirement = this.required[i];
        this.remaining[i]--;
        this.required[i] = false;
        MaximizerSpeculation.this.equipment.put(slot, this.gems.get(i));
        if (this.cache != null) {
          this.cache.select(slotIndex, i);
        }
        this.bounds.select(i);
        this.search(i, slotIndex + 1, requiredCount - (satisfiesRequirement ? 1 : 0));
        this.bounds.deselect(i);
        MaximizerSpeculation.this.equipment.put(slot, EquipmentRequest.UNEQUIP);
        if (this.cache != null) {
          this.cache.deselect(slotIndex);
        }
        this.required[i] = satisfiesRequirement;
        this.remaining[i]++;
      }
    }

    private CodpiecePruning.ScoreUpperBound getTiebreakUpperBound(int selectedCount) {
      if (this.tiebreakBoundInitialized) {
        return this.bounds.tiebreaker();
      }

      this.tiebreakBoundInitialized = true;
      CodpiecePruning.ScoreUpperBound tiebreakUpperBound =
          Maximizer.eval.createTheoreticalCodpieceTiebreakerUpperBound(
              this.cache.baseline,
              this.cache.gemModifiers,
              this.initialRemaining,
              this.slots.size(),
              this.cache.familiarScoreContributions);
      for (int i = 0; tiebreakUpperBound != null && i < selectedCount; i++) {
        tiebreakUpperBound.select(this.cache.slotGemIndexes[i] - 1);
      }
      this.bounds =
          new CodpiecePruning.BranchBounds(
              this.bounds.score(),
              tiebreakUpperBound,
              this.bounds.itemDroppers(),
              this.bounds.meatDroppers());
      return tiebreakUpperBound;
    }
  }

  /** Applies each candidate to its own clone of baseline and returns the best-scoring one. */
  public static <T> MaximizerSpeculation bestOf(
      MaximizerSpeculation baseline,
      Iterable<T> candidates,
      BiConsumer<MaximizerSpeculation, T> mutator) {
    MaximizerSpeculation best = baseline;
    for (T candidate : candidates) {
      MaximizerSpeculation spec = baseline.clone();
      mutator.accept(spec, candidate);
      spec.setUnscored(); // clone() may carry baseline's cached score
      if (spec.compareTo(best) > 0) {
        best = spec;
      }
    }
    return best;
  }

  private void checkBest() throws MaximizerInterruptedException {
    this.checkBest(false);
  }

  private void checkBest(boolean codpieceCountsValid) throws MaximizerInterruptedException {
    if (!codpieceCountsValid && !this.hasEnoughCodpieceGems()) {
      return;
    }

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
      Maximizer.best = this.clone();
    }
    Maximizer.bestChecked++;
    if ((Maximizer.bestChecked & 0x3FF) == 0) {
      long t = System.currentTimeMillis();
      if (t > Maximizer.bestUpdate) {
        MaximizerSpeculation.showProgress();
        Maximizer.bestUpdate = t + 5000;
      }
    }
    if (!KoLmafia.permitsContinue()) {
      throw new MaximizerInterruptedException();
    }
    if (this.exceeded) {
      throw new MaximizerExceededException();
    }
    if (Maximizer.combinationLimit != 0 && Maximizer.bestChecked >= Maximizer.combinationLimit) {
      throw new MaximizerLimitException();
    }
  }

  private long countEquipped(int itemId) {
    return this.equipment.values().stream()
        .filter(item -> item != null && item.getItemId() == itemId)
        .count();
  }

  private boolean hasEnoughCodpieceGems() {
    Map<Integer, Integer> used = new HashMap<>();
    Map<Integer, Integer> available = new HashMap<>();
    for (AdventureResult item : this.equipment.values()) {
      if (item == null || !EquipmentRequest.isCodpieceGem(item.getItemId())) {
        continue;
      }

      int itemId = item.getItemId();
      used.merge(itemId, 1, Integer::sum);
      if (item instanceof CheckedItem checked) {
        available.put(itemId, checked.getAvailableCount());
      }
    }

    for (var entry : available.entrySet()) {
      if (used.get(entry.getKey()) > entry.getValue()) {
        return false;
      }
    }
    return true;
  }

  private static int getMutex(AdventureResult item) {
    Modifiers mods = ModifierDatabase.getItemModifiers(item.getItemId());
    if (mods == null) {
      return 0;
    }
    return mods.getRawBitmap(BitmapModifier.MUTEX);
  }

  private void trySwap(Slot slot1, Slot slot2) {
    // If we are suggesting an accessory that's already being worn,
    // make sure we suggest the same slot (to minimize server hits).
    AdventureResult item1, item2, eq1, eq2;
    item1 = this.equipment.get(slot1);
    if (item1 == null) item1 = EquipmentRequest.UNEQUIP;
    eq1 = EquipmentManager.getEquipment(slot1);
    if (eq1.equals(item1)) return;
    item2 = this.equipment.get(slot2);
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
      this.equipment.put(slot1, item2);
      this.equipment.put(slot2, item1);
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
