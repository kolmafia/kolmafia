package net.sourceforge.kolmafia.maximizer;

import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.ExpressionOverrides;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;

final class OrdinaryCandidateEvaluator {
  enum Disposition {
    REJECT,
    CATALOG_ONLY,
    RANKED
  }

  record Requirements(
      Map<Slot, Integer> slots,
      Map<Modeable, String> forcedModeables,
      boolean current,
      int clownosity,
      int raveosity,
      int surgeonosity,
      int stinkycheese) {}

  private final Evaluator evaluator;
  private final EquipmentSetEvaluator setEvaluator;
  private final Requirements requirements;
  private final double nullScore;
  private final Map<Modeable, Boolean> modeablesNeeded = Modeable.getBooleanMap();
  private final boolean hoboPowerUseful;
  private final boolean smithsnessUseful;
  private final boolean brimstoneUseful;
  private final boolean cloathingUseful;
  private final boolean slimeHateUseful;
  private final boolean mcHugeLargeUseful;
  private int carriedFamiliarsNeeded;
  private boolean cardNeeded;

  OrdinaryCandidateEvaluator(
      Evaluator evaluator,
      EquipmentSetEvaluator setEvaluator,
      Requirements requirements,
      double nullScore) {
    this.evaluator = evaluator;
    this.setEvaluator = setEvaluator;
    this.requirements = requirements;
    this.nullScore = nullScore;
    this.hoboPowerUseful = this.isCategoryUseful("_hoboPower");
    this.smithsnessUseful = this.isCategoryUseful("_smithsness");
    this.brimstoneUseful = this.isCategoryUseful("_brimstone");
    this.cloathingUseful = this.isCategoryUseful("_cloathing");
    this.slimeHateUseful = this.isCategoryUseful("_slimeHate");
    this.mcHugeLargeUseful = this.isCategoryUseful("_mcHugeLarge");
  }

  boolean hoboPowerUseful() {
    return this.hoboPowerUseful;
  }

  int carriedFamiliarsNeeded() {
    return this.carriedFamiliarsNeeded;
  }

  boolean cardNeeded() {
    return this.cardNeeded;
  }

  Map<Modeable, Boolean> modeablesNeeded() {
    return this.modeablesNeeded;
  }

  Disposition evaluate(int itemId, CheckedItem item, Modeable modeable)
      throws MaximizerInterruptedException {
    if (this.setEvaluator.isUsefulOutfitPiece(itemId)) {
      this.setEvaluator.retainOutfitPiece(item);
      if (item.getCount() == 0) {
        return Disposition.REJECT;
      }
    }

    if (KoLCharacter.hasEquipped(item) && this.requirements.current()) {
      item.automaticFlag = true;
    }

    Modifiers modifiers = ModifierDatabase.getItemModifiers(itemId);
    if (modifiers == null) {
      modifiers = new Modifiers();
    }
    String classType = modifiers.getString(StringModifier.CLASS);
    boolean wrongClass =
        !classType.isEmpty() && !classType.equals(KoLCharacter.getAscensionClassName());

    if (modifiers.getBoolean(BooleanModifier.SINGLE)) {
      item.singleFlag = true;
    }
    this.recordSpecialtyNeed(itemId, modeable);

    if (itemId == ItemPool.VAMPYRIC_CLOAKE) {
      modifiers = new Modifiers(modifiers);
      modifiers.applyVampyricCloakeModifiers();
    }

    if (this.evaluator.requiresEquipment(item)) {
      item.automaticFlag = true;
      item.requiredFlag = true;
      return Disposition.RANKED;
    }

    switch (this.evaluator.checkConstraints(modifiers)) {
      case VIOLATES:
        return Disposition.REJECT;
      case MEETS:
        item.automaticFlag = true;
        return Disposition.RANKED;
    }

    if (this.isSetOrCategoryRelevant(modifiers, wrongClass)) {
      item.automaticFlag = true;
      return Disposition.RANKED;
    }
    if (modifiers.hasUnarmedBonus() && this.hasUsefulUnarmedBonus(itemId, item)) {
      item.conditionalFlag = true;
      item.automaticFlag = true;
      return Disposition.RANKED;
    }

    if (this.hasChangeableContents(itemId)) {
      return Disposition.RANKED;
    }
    if (modeable != null) {
      if (!this.requirements.forcedModeables().get(modeable).isEmpty()) {
        item.automaticFlag = true;
      }
      return Disposition.RANKED;
    }

    String intrinsic = modifiers.getString(StringModifier.INTRINSIC_EFFECT);
    if (!intrinsic.isEmpty()) {
      Modifiers withIntrinsic = new Modifiers();
      withIntrinsic.add(modifiers);
      withIntrinsic.add(ModifierDatabase.getModifiers(ModifierType.EFFECT, intrinsic));
      modifiers = withIntrinsic;
    }

    double delta =
        this.evaluator.getScore(modifiers, Map.of(Slot.NONE, item), Map.of()) - nullScore;
    if (delta < 0.0) {
      return Disposition.CATALOG_ONLY;
    }
    if (delta == 0.0
        && !(KoLCharacter.hasEquipped(item) && this.requirements.current())
        && (item.availability().initial() == 0 || item.automaticFlag)) {
      return Disposition.CATALOG_ONLY;
    }
    if (modifiers.getRawBitmap(BitmapModifier.MUTEX) != 0) {
      item.conditionalFlag = true;
    }
    return Disposition.RANKED;
  }

  private void recordSpecialtyNeed(int itemId, Modeable modeable) {
    if (((itemId == ItemPool.HATSEAT
                && this.requirements.slots().getOrDefault(Slot.CROWNOFTHRONES, 0) >= 0)
            || (itemId == ItemPool.BUDDY_BJORN
                && this.requirements.slots().getOrDefault(Slot.BUDDYBJORN, 0) >= 0))
        && KoLCharacter.getPath().canUseFamiliars()) {
      this.carriedFamiliarsNeeded++;
    }
    if (itemId == ItemPool.CARD_SLEEVE
        && this.requirements.slots().getOrDefault(Slot.CARDSLEEVE, 0) >= 0) {
      this.cardNeeded = true;
    }
    if (modeable == null) {
      return;
    }

    List<Integer> slotWeightings =
        switch (modeable.getSlot()) {
          case ACCESSORY1 ->
              List.of(
                  this.requirements.slots().getOrDefault(Slot.ACCESSORY1, 0),
                  this.requirements.slots().getOrDefault(Slot.ACCESSORY2, 0),
                  this.requirements.slots().getOrDefault(Slot.ACCESSORY3, 0));
          case OFFHAND ->
              List.of(
                  this.requirements.slots().getOrDefault(Slot.OFFHAND, 0),
                  this.requirements.slots().getOrDefault(Slot.FAMILIAR, 0));
          default -> List.of(this.requirements.slots().getOrDefault(modeable.getSlot(), 0));
        };
    this.modeablesNeeded.put(modeable, slotWeightings.stream().anyMatch(value -> value >= 0));
  }

  private boolean isSetOrCategoryRelevant(Modifiers modifiers, boolean wrongClass) {
    return (this.hoboPowerUseful && modifiers.getDouble(DoubleModifier.HOBO_POWER) > 0.0)
        || (this.smithsnessUseful
            && !wrongClass
            && modifiers.getDouble(DoubleModifier.SMITHSNESS) > 0.0)
        || (this.brimstoneUseful && modifiers.getRawBitmap(BitmapModifier.BRIMSTONE) != 0)
        || (this.cloathingUseful && modifiers.getRawBitmap(BitmapModifier.CLOATHING) != 0)
        || (this.slimeHateUseful && modifiers.getDouble(DoubleModifier.SLIME_HATES_IT) > 0.0)
        || (this.mcHugeLargeUseful && modifiers.getRawBitmap(BitmapModifier.MCHUGELARGE) != 0)
        || (this.requirements.clownosity() > 0
            && modifiers.getRawBitmap(BitmapModifier.CLOWNINESS) != 0)
        || (this.requirements.raveosity() > 0
            && modifiers.getRawBitmap(BitmapModifier.RAVEOSITY) != 0)
        || (this.requirements.surgeonosity() > 0
            && modifiers.getRawBitmap(BitmapModifier.SURGEONOSITY) != 0)
        || (this.requirements.stinkycheese() > 0
            && modifiers.getRawBitmap(BitmapModifier.STINKYCHEESE) != 0)
        || this.setEvaluator.isUsefulSynergyPiece(modifiers);
  }

  private boolean hasUsefulUnarmedBonus(int itemId, CheckedItem item) {
    Modifiers modifiers = new Modifiers(ModifierDatabase.getItemModifiers(itemId));
    ExpressionOverrides overrides = new ExpressionOverrides();
    overrides.setUnarmed(true);
    modifiers.recalculateExpressions(overrides);
    return this.evaluator.getScore(modifiers, Map.of(Slot.NONE, item), Modeable.getStateMap())
        > this.nullScore;
  }

  private boolean hasChangeableContents(int itemId) {
    return ((itemId == ItemPool.HATSEAT || itemId == ItemPool.BUDDY_BJORN)
            && KoLCharacter.getPath().canUseFamiliars())
        || itemId == ItemPool.CARD_SLEEVE
        || itemId == ItemPool.THE_ETERNITY_CODPIECE;
  }

  private boolean isCategoryUseful(String category) {
    Modifiers modifiers = ModifierDatabase.getModifiers(ModifierType.MAX_CAT, category);
    return modifiers != null && this.evaluator.getScore(modifiers) - this.nullScore > 0.0;
  }
}
