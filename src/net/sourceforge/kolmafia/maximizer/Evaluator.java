package net.sourceforge.kolmafia.maximizer;

import static net.sourceforge.kolmafia.maximizer.MaximizerTermRegistry.IntegerSetting.BEEOSITY;
import static net.sourceforge.kolmafia.maximizer.MaximizerTermRegistry.IntegerSetting.CLOWNOSITY;
import static net.sourceforge.kolmafia.maximizer.MaximizerTermRegistry.IntegerSetting.RAVEOSITY;
import static net.sourceforge.kolmafia.maximizer.MaximizerTermRegistry.IntegerSetting.STINKYCHEESE;
import static net.sourceforge.kolmafia.maximizer.MaximizerTermRegistry.IntegerSetting.SURGEONOSITY;

import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.session.EffectAvailability;

@SuppressWarnings("incomplete-switch")
public class Evaluator {
  @Deprecated public boolean failed;
  boolean exceeded;
  private final Evaluator tiebreaker;
  private final CodpieceEvaluator codpieceEvaluator = new CodpieceEvaluator(this);
  private final MaximizerTermRegistry terms;
  private List<ScoreTerm> activeScoreModifiers = List.of();
  private boolean predictsDerivedModifiers;

  record ScoreTerm(DoubleModifier modifier, double weight, double min, double max) {}

  private static final String TIEBREAKER =
      "1 familiar weight, 1 familiar experience, 1 initiative, 5 exp, 1 item, 1 meat, 0.1 DA 1000 max, 1 DR, 0.5 all res, -10 mana cost, 1.0 mus, 0.5 mys, 1.0 mox, 1.5 mainstat, 1 HP, 1 MP, 1 weapon damage, 1 ranged damage, 1 spell damage, 1 cold damage, 1 hot damage, 1 sleaze damage, 1 spooky damage, 1 stench damage, 1 cold spell damage, 1 hot spell damage, 1 sleaze spell damage, 1 spooky spell damage, 1 stench spell damage, -1 fumble, 1 HP regen max, 3 MP regen max, 1 critical hit percent, 0.1 food drop, 0.1 booze drop, 0.1 hat drop, 0.1 weapon drop, 0.1 offhand drop, 0.1 shirt drop, 0.1 pants drop, 0.1 accessory drop, 1 DB combat damage, 0.1 sixgun damage";
  // Equipment slots, that aren't the primary slot of any item type,
  // that are repurposed here (rather than making the array bigger).
  // Watches have to be handled specially because only one can be
  // used - otherwise, they'd fill up the list, leaving no room for
  // any non-watches to put in the other two acc slots.
  // 1-handed weapons have to be ranked separately due to the following
  // possibility: all of your best weapons are 2-hand, but you've got
  // a really good off-hand, better than any weapon.  There would
  // otherwise be no suitable weapons to go with that off-hand.
  static final Slot OFFHAND_MELEE = Slot.ACCESSORY2;
  static final Slot OFFHAND_RANGED = Slot.ACCESSORY3;
  static final Slot WEAPON_1H = Slot.STICKER3;

  // Slots starting with EquipmentSlot.ALL_SLOTS are equipment
  // for other familiars being considered.

  static Slot toUseSlot(Slot slot) {
    return switch (slot) {
      case /* Evaluator.OFFHAND_MELEE */ ACCESSORY2, /* Evaluator.OFFHAND_RANGED */ ACCESSORY3 ->
          Slot.OFFHAND;
      case /* Evaluator.WEAPON_1H */ STICKER3 -> Slot.WEAPON;
      default -> slot;
    };
  }

  private Evaluator() {
    this.tiebreaker = null;
    this.terms = new MaximizerTermRegistry();
  }

  public Evaluator(String expr) {
    Evaluator tiebreaker = new Evaluator();
    this.tiebreaker = tiebreaker;
    MaximizerExpressionParser.parse(Evaluator.TIEBREAKER, tiebreaker.terms);
    tiebreaker.initializeScoreModifiers();

    this.terms = new MaximizerTermRegistry(tiebreaker.terms);
    MaximizerExpressionParser.parse(expr, this.terms);
    this.initializeScoreModifiers();
  }

  private void initializeScoreModifiers() {
    this.activeScoreModifiers = this.terms.scoreTerms();
    this.predictsDerivedModifiers =
        this.activeScoreModifiers.stream()
            .anyMatch(term -> Evaluator.predictsDerived(term.modifier()));
  }

  private static boolean predictsDerived(DoubleModifier modifier) {
    return modifier == DoubleModifier.MUS
        || modifier == DoubleModifier.MYS
        || modifier == DoubleModifier.MOX
        || modifier == DoubleModifier.HP
        || modifier == DoubleModifier.MP;
  }

  List<ScoreTerm> incrementalCodpieceScoreTerms() {
    if (this.terms.hasNonModifierScore()) return null;
    List<ScoreTerm> terms =
        this.activeScoreModifiers.stream().filter(term -> term.weight() != 0.0).toList();
    return terms.stream()
            .allMatch(term -> CodpieceModifierSafety.supportsIncrementalScore(term.modifier()))
        ? terms
        : null;
  }

  public double getScore(
      Modifiers mods, Map<Slot, AdventureResult> equipment, Map<Modeable, String> modeables) {
    var outcome = this.evaluate(mods, equipment, modeables);
    this.failed = outcome.failed();
    this.exceeded = outcome.exceeded();
    return outcome.score();
  }

  public EvaluationOutcome evaluate(Modifiers mods) {
    return this.evaluate(mods, Map.of(), Map.of());
  }

  EvaluationOutcome evaluate(
      Modifiers mods, Map<Slot, AdventureResult> equipment, Map<Modeable, String> modeables) {
    var predicted = this.predictsDerivedModifiers ? mods.predict() : null;

    boolean failed = false;
    double score = 0.0;
    for (var scoreModifier : this.activeScoreModifiers) {
      var mod = scoreModifier.modifier();
      double weight = scoreModifier.weight();
      double min = scoreModifier.min();
      double val = scoreValue(mod, mods, predicted);
      double max = scoreModifier.max();
      if (val < min) failed = true;
      score += weight * Math.min(val, max);
    }
    int stinkycheese = this.terms.integer(STINKYCHEESE);
    if (stinkycheese > 0) {
      int val = mods.getBitmap(BitmapModifier.STINKYCHEESE);
      score += stinkycheese * val;
    }
    score += this.terms.equipmentBonus(equipment.values(), modeables);
    // Add fudge factor for Rollover Effect
    if (mods.hasString(StringModifier.ROLLOVER_EFFECT)) {
      score += 0.01f;
    }
    if (score < this.terms.totalMin()) failed = true;
    boolean exceeded = score >= this.terms.totalMax();
    // Score bitmap objectives 1:1 up to the requested target, which must be reached.
    int clownosity = this.terms.integer(CLOWNOSITY);
    if (clownosity > 0) {
      int osity = mods.getBitmap(BitmapModifier.CLOWNINESS);
      score += Math.min(osity, clownosity);
      if (osity < clownosity) failed = true;
    }
    int raveosity = this.terms.integer(RAVEOSITY);
    if (raveosity > 0) {
      int osity = mods.getBitmap(BitmapModifier.RAVEOSITY);
      score += Math.min(osity, raveosity);
      if (osity < raveosity) failed = true;
    }
    int surgeonosity = this.terms.integer(SURGEONOSITY);
    if (surgeonosity > 0) {
      int osity = mods.getBitmap(BitmapModifier.SURGEONOSITY);
      score += Math.min(osity, surgeonosity);
      if (osity < surgeonosity) failed = true;
    }
    if (!failed && !this.terms.booleansSatisfied(mods)) {
      failed = true;
    }
    return new EvaluationOutcome(score, failed, exceeded);
  }

  public double getScore(Modifiers mods) {
    return this.getScore(mods, Map.of(), Map.of());
  }

  static double scoreValue(
      DoubleModifier modifier, Modifiers modifiers, Map<DerivedModifier, Integer> predicted) {
    return switch (modifier) {
      case MUS -> predicted.get(DerivedModifier.BUFFED_MUS);
      case MYS -> predicted.get(DerivedModifier.BUFFED_MYS);
      case MOX -> predicted.get(DerivedModifier.BUFFED_MOX);
      case HP -> predicted.get(DerivedModifier.BUFFED_HP);
      case MP -> predicted.get(DerivedModifier.BUFFED_MP);
      case FAMILIAR_WEIGHT ->
          (modifiers.getDouble(DoubleModifier.FAMILIAR_WEIGHT)
                  + modifiers.getDouble(DoubleModifier.HIDDEN_FAMILIAR_WEIGHT))
              * (modifiers.getDouble(DoubleModifier.FAMILIAR_WEIGHT_PCT) < 0.0 ? 0.5 : 1.0);
      case INITIATIVE ->
          modifiers.getDouble(DoubleModifier.INITIATIVE)
              + Math.min(0.0, modifiers.getDouble(DoubleModifier.INITIATIVE_PENALTY));
      case MANA_COST ->
          modifiers.getDouble(DoubleModifier.MANA_COST)
              + modifiers.getDouble(DoubleModifier.STACKABLE_MANA_COST);
      case ITEMDROP ->
          modifiers.getDouble(DoubleModifier.ITEMDROP)
              + 100.0
              + Math.min(0.0, modifiers.getDouble(DoubleModifier.ITEMDROP_PENALTY))
              + modifiers.getDouble(DoubleModifier.SPORADIC_ITEMDROP);
      case MEATDROP ->
          modifiers.getDouble(DoubleModifier.MEATDROP)
              + 100.0
              + Math.min(0.0, modifiers.getDouble(DoubleModifier.MEATDROP_PENALTY))
              + modifiers.getDouble(DoubleModifier.SPORADIC_MEATDROP)
              + modifiers.getDouble(DoubleModifier.MEAT_BONUS) / 10000.0;
      case WEAPON_DAMAGE ->
          modifiers.getDouble(DoubleModifier.WEAPON_DAMAGE)
              + modifiers.getDouble(DoubleModifier.WEAPON_DAMAGE_PCT);
      case RANGED_DAMAGE ->
          modifiers.getDouble(DoubleModifier.RANGED_DAMAGE)
              + modifiers.getDouble(DoubleModifier.RANGED_DAMAGE_PCT);
      case SPELL_DAMAGE ->
          modifiers.getDouble(DoubleModifier.SPELL_DAMAGE)
              + modifiers.getDouble(DoubleModifier.SPELL_DAMAGE_PCT);
      case DAMAGE_AURA ->
          modifiers.getDouble(DoubleModifier.DAMAGE_AURA)
              + modifiers.getDouble(DoubleModifier.SPORADIC_DAMAGE_AURA);
      case THORNS ->
          modifiers.getDouble(DoubleModifier.THORNS)
              + modifiers.getDouble(DoubleModifier.SPORADIC_THORNS);
      case COLD_RESISTANCE ->
          resistanceValue(
              modifiers,
              DoubleModifier.COLD_RESISTANCE,
              BooleanModifier.COLD_IMMUNITY,
              BooleanModifier.COLD_VULNERABILITY);
      case HOT_RESISTANCE ->
          resistanceValue(
              modifiers,
              DoubleModifier.HOT_RESISTANCE,
              BooleanModifier.HOT_IMMUNITY,
              BooleanModifier.HOT_VULNERABILITY);
      case SLEAZE_RESISTANCE ->
          resistanceValue(
              modifiers,
              DoubleModifier.SLEAZE_RESISTANCE,
              BooleanModifier.SLEAZE_IMMUNITY,
              BooleanModifier.SLEAZE_VULNERABILITY);
      case SPOOKY_RESISTANCE ->
          resistanceValue(
              modifiers,
              DoubleModifier.SPOOKY_RESISTANCE,
              BooleanModifier.SPOOKY_IMMUNITY,
              BooleanModifier.SPOOKY_VULNERABILITY);
      case STENCH_RESISTANCE ->
          resistanceValue(
              modifiers,
              DoubleModifier.STENCH_RESISTANCE,
              BooleanModifier.STENCH_IMMUNITY,
              BooleanModifier.STENCH_VULNERABILITY);
      case EXPERIENCE -> experienceValue(modifiers);
      default -> modifiers.getDouble(modifier);
    };
  }

  private static double resistanceValue(
      Modifiers modifiers,
      DoubleModifier resistance,
      BooleanModifier immunity,
      BooleanModifier vulnerability) {
    if (modifiers.getBoolean(immunity)) {
      return 100.0;
    }
    double value = modifiers.getDouble(resistance);
    return modifiers.getBoolean(vulnerability) ? value - 100.0 : value;
  }

  private static double experienceValue(Modifiers modifiers) {
    double baseExperience =
        KoLCharacter.estimatedBaseExp(
            modifiers.getDouble(DoubleModifier.MONSTER_LEVEL)
                * (1.0 + modifiers.getDouble(DoubleModifier.MONSTER_LEVEL_PERCENT) / 100.0));
    double experiencePercent = modifiers.getDouble(DoubleModifier.primeStatExpPercent()) / 100.0;
    double experience = modifiers.getDouble(DoubleModifier.primeStatExp());
    return ((baseExperience + experience) * (1.0 + experiencePercent)) / 2.0;
  }

  CodpieceEvaluator.Context codpieceContext() {
    return new CodpieceEvaluator.Context(
        this.activeScoreModifiers,
        this.tiebreaker == null ? List.of() : this.tiebreaker.activeScoreModifiers,
        !this.terms.usesTiebreaker());
  }

  EvaluationOutcome evaluateComplete(
      Modifiers mods,
      Map<Slot, AdventureResult> equipment,
      Map<Modeable, String> modeables,
      boolean resourceLimitExceeded,
      int allowedMutexViolations) {
    var outcome = this.evaluate(mods, equipment, modeables);
    boolean failed = outcome.failed() || !this.terms.equipmentSatisfied(mods, equipment);
    if (resourceLimitExceeded) {
      failed = true;
    }
    if ((mods.getRawBitmap(BitmapModifier.MUTEX_VIOLATIONS) & ~allowedMutexViolations) != 0) {
      failed = true;
    }
    return new EvaluationOutcome(outcome.score(), failed, outcome.exceeded());
  }

  int beeosityLimit() {
    return this.terms.integer(BEEOSITY);
  }

  boolean slotEnabled(Slot slot) {
    return this.terms.slotEnabled(slot);
  }

  double getTiebreaker(Modifiers mods) {
    if (!this.terms.usesTiebreaker()) return 0.0;
    return this.tiebreaker.getScore(mods);
  }

  boolean isUsingTiebreaker() {
    return this.terms.usesTiebreaker();
  }

  boolean isWeaponTypeRequired() {
    return this.terms.isWeaponTypeRequired();
  }

  boolean isShieldRequired() {
    return this.terms.isShieldRequired();
  }

  /** Whether the expression forbids leaving the weapon slot empty. */
  boolean forbidsUnarmed() {
    return this.terms.forbidsUnarmed();
  }

  enum Constraint {
    /** Item violates a constraint, don't use it */
    VIOLATES,
    /** Item not relevant to any constraints */
    IRRELEVANT,
    /** Item meets a constraint, give it special handling */
    MEETS
  }

  Constraint checkConstraints(Modifiers mods) {
    return this.terms.checkConstraints(mods);
  }

  boolean requiresEquipment(AdventureResult item) {
    return this.terms.requiresEquipment(item);
  }

  boolean excludesEquipment(AdventureResult item) {
    return this.terms.excludesEquipment(item);
  }

  boolean currentOnly() {
    return this.terms.currentOnly();
  }

  @Deprecated
  public static boolean cannotGainEffect(int effectId) {
    return EffectAvailability.cannotGain(effectId);
  }

  void enumerateEquipment(EquipScope equipScope, int maxPrice, PriceLevel priceLevel)
      throws MaximizerInterruptedException {
    this.enumerateEquipment(equipScope, maxPrice, priceLevel, false);
  }

  void enumerateEquipment(
      EquipScope equipScope, int maxPrice, PriceLevel priceLevel, boolean exhaustive)
      throws MaximizerInterruptedException {
    EquipmentSearchRunner.compileAndRun(
        this, this.terms, this.codpieceEvaluator, equipScope, maxPrice, priceLevel, exhaustive);
  }

  List<CheckedItem> prioritizeCodpieceGems(List<CheckedItem> gems) {
    return this.codpieceEvaluator.prioritize(gems);
  }
}
