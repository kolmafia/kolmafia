package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifierCollection;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EffectAvailability;
import net.sourceforge.kolmafia.session.EquipmentManager;

@SuppressWarnings("incomplete-switch")
public class Evaluator {
  @Deprecated public boolean failed;
  boolean exceeded;
  private Evaluator tiebreaker;
  private final CodpieceEvaluator codpieceEvaluator = new CodpieceEvaluator(this);
  private final DoubleModifierCollection weight = new DoubleModifierCollection();
  private final EnumSet<DoubleModifier> explicitScoreModifiers =
      EnumSet.noneOf(DoubleModifier.class);
  private Map<DoubleModifier, Double> min;
  private Map<DoubleModifier, Double> max;
  private List<ScoreTerm> activeScoreModifiers = List.of();
  private boolean predictsDerivedModifiers;
  private double totalMin, totalMax;
  private int dump = 0;
  private int clownosity = 0;
  private int raveosity = 0;
  private int surgeonosity = 0;
  private int stinkycheese = 0;
  private int beeosity = 2;
  private final EnumSet<BooleanModifier> booleanMask = EnumSet.noneOf(BooleanModifier.class);
  private final Set<BooleanModifier> booleanValue = EnumSet.noneOf(BooleanModifier.class);
  private final List<FamiliarData> familiars = new ArrayList<>();
  private final List<FamiliarData> carriedFamiliars = new ArrayList<>();
  // Some modeables are forced based on certain expressions appearing in a maximize call
  // For example, if you request "sea" the Crown of Ed will always pick fish. This does pose
  // an issue if the maximizer would choose the SCUBA gear to provide water-breathing, as it would
  // not consider a different mode for the Crown. e.g. "maximize sea, ml" would not consider the
  // "bear" mode for the hat. Something for someone to fix in the future.
  private final Map<Modeable, String> forcedModeables = Modeable.getStringMap(m -> "");

  /** if slots[i] >= 0 then equipment of type i can be considered for maximization */
  private final EnumMap<Slot, Integer> slots = new EnumMap<>(Slot.class);

  private String weaponType = null;
  private int hands = 0;
  int melee = 0; // +/-2 or higher: require, +/-1: disallow other type
  private boolean effective = false;
  private boolean requireClub = false;
  private boolean requireShield = false;
  private boolean requireUtensil = false;
  private boolean requireSword = false;
  private boolean requireKnife = false;
  private boolean requireAccordion = false;
  private boolean noTiebreaker = false;
  private boolean current =
      !KoLCharacter.canInteract() || Preferences.getBoolean("maximizerAlwaysCurrent");
  private final Set<String> posOutfits = new HashSet<>();
  private final Set<String> negOutfits = new HashSet<>();
  private final Set<AdventureResult> posEquip = new HashSet<>();
  private final Set<AdventureResult> negEquip = new HashSet<>();
  private final Map<AdventureResult, ItemBonus> bonuses = new HashMap<>();
  private final List<BonusFunction> bonusFunc = new ArrayList<>();

  record BonusFunction(Function<AdventureResult, Double> bonusFunction, Double weight) {}

  record ItemBonus(double base, Map<String, Double> modes) {}

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
    this.totalMin = Double.NEGATIVE_INFINITY;
    this.totalMax = Double.POSITIVE_INFINITY;
  }

  public Evaluator(String expr) {
    this();

    Evaluator tiebreaker = new Evaluator();
    this.tiebreaker = tiebreaker;
    tiebreaker.min = new EnumMap<>(DoubleModifier.class);
    tiebreaker.max = new EnumMap<>(DoubleModifier.class);
    for (var mod : DoubleModifier.DOUBLE_MODIFIERS) {
      tiebreaker.min.put(mod, Double.NEGATIVE_INFINITY);
      tiebreaker.max.put(mod, Double.POSITIVE_INFINITY);
    }
    tiebreaker.parse(Evaluator.TIEBREAKER);
    tiebreaker.initializeScoreModifiers();

    this.min = new EnumMap<>(tiebreaker.min);
    this.max = new EnumMap<>(tiebreaker.max);
    this.parse(expr);
    this.initializeScoreModifiers();
  }

  private void initializeScoreModifiers() {
    var active = new ArrayList<ScoreTerm>();
    this.predictsDerivedModifiers = false;
    for (var modifier : DoubleModifier.DOUBLE_MODIFIERS) {
      double weight = this.weight.getDouble(modifier);
      double min = this.min.get(modifier);
      if (weight == 0.0 && min == Double.NEGATIVE_INFINITY) {
        continue;
      }

      active.add(new ScoreTerm(modifier, weight, min, this.max.get(modifier)));
      if (modifier == DoubleModifier.MUS
          || modifier == DoubleModifier.MYS
          || modifier == DoubleModifier.MOX
          || modifier == DoubleModifier.HP
          || modifier == DoubleModifier.MP) {
        this.predictsDerivedModifiers = true;
      }
    }
    this.activeScoreModifiers = List.copyOf(active);
  }

  final class ParseState {
    private DoubleModifier index;
    private boolean hadFamiliar;
    private boolean forceCurrent;
    private int equipBeeosity;
    private int outfitBeeosity;
    private boolean failed;

    void setMinimum(MaximizerTermRegistry.ParsedTerm term) {
      if (this.index != null) {
        min.put(this.index, term.weight());
      } else {
        totalMin = term.weight();
      }
    }

    void setMaximum(MaximizerTermRegistry.ParsedTerm term) {
      if (this.index != null) {
        max.put(this.index, term.weight());
      } else {
        totalMax = term.weight();
      }
    }

    void setDump(MaximizerTermRegistry.ParsedTerm term) {
      dump = (int) term.weight();
    }

    void setHands(MaximizerTermRegistry.ParsedTerm term) {
      hands = (int) term.weight();
    }

    void setTiebreaker(MaximizerTermRegistry.ParsedTerm term) {
      noTiebreaker = term.weight() < 0.0;
    }

    void setCurrent(MaximizerTermRegistry.ParsedTerm term) {
      current = term.weight() > 0.0;
      this.forceCurrent = true;
    }

    void setWeaponType(MaximizerTermRegistry.ParsedTerm term) {
      weaponType = term.keyword().substring(5).trim();
    }

    void requireClub(MaximizerTermRegistry.ParsedTerm term) {
      requireClub = term.weight() > 0.0;
    }

    void requireShield(MaximizerTermRegistry.ParsedTerm term) {
      requireShield = term.weight() > 0.0;
      if (forcedModeables.get(Modeable.UMBRELLA).isEmpty()) {
        forcedModeables.put(Modeable.UMBRELLA, "forward-facing");
      }
      hands = 1;
    }

    void requireUtensil(MaximizerTermRegistry.ParsedTerm term) {
      requireUtensil = term.weight() > 0.0;
    }

    void requireSword(MaximizerTermRegistry.ParsedTerm term) {
      requireSword = term.weight() > 0.0;
    }

    void requireKnife(MaximizerTermRegistry.ParsedTerm term) {
      requireKnife = term.weight() > 0.0;
    }

    void requireAccordion(MaximizerTermRegistry.ParsedTerm term) {
      requireAccordion = term.weight() > 0.0;
    }

    void setMelee(MaximizerTermRegistry.ParsedTerm term) {
      melee = (int) (term.weight() * 2.0);
    }

    void setEffective(MaximizerTermRegistry.ParsedTerm term) {
      effective = term.weight() > 0.0;
    }

    void setEmpty(MaximizerTermRegistry.ParsedTerm term) {
      for (var slot : SlotSet.ALL_SLOTS) {
        slots.merge(
            slot,
            ((int) term.weight())
                * (EquipmentManager.getEquipment(slot).equals(EquipmentRequest.UNEQUIP) ? 1 : -1),
            Integer::sum);
      }
    }

    void setClownosity(MaximizerTermRegistry.ParsedTerm term) {
      clownosity = term.explicitWeight() ? (int) term.weight() * 25 : 100;
    }

    void setRaveosity(MaximizerTermRegistry.ParsedTerm term) {
      raveosity = term.explicitWeight() ? (int) term.weight() : 7;
    }

    void setSurgeonosity(MaximizerTermRegistry.ParsedTerm term) {
      surgeonosity =
          term.explicitWeight() ? (int) term.weight() : (KoLCharacter.isTorsoAware() ? 5 : 4);
    }

    void setBeeosity(MaximizerTermRegistry.ParsedTerm term) {
      beeosity = (int) term.weight();
    }

    void setStinkycheese(MaximizerTermRegistry.ParsedTerm term) {
      stinkycheese = (int) term.weight();
    }

    void setSea(MaximizerTermRegistry.ParsedTerm term) {
      var adventureUnderwater =
          EnumSet.of(BooleanModifier.ADVENTURE_UNDERWATER, BooleanModifier.UNDERWATER_FAMILIAR);
      booleanMask.addAll(adventureUnderwater);
      booleanValue.addAll(adventureUnderwater);
      this.index = null;
      if (forcedModeables.get(Modeable.EDPIECE).isEmpty()) {
        forcedModeables.put(Modeable.EDPIECE, "fish");
      }
    }

    void setEquipment(MaximizerTermRegistry.ParsedTerm term) {
      var match =
          ItemFinder.getFirstMatchingItemWithMode(term.keyword().substring(6).trim(), Match.EQUIP);
      if (match == null || (match.modeable() != null && !forceModeable(match, match.mode()))) {
        this.failed = true;
        return;
      }
      if (term.weight() > 0.0) {
        posEquip.add(match.item());
        this.equipBeeosity += KoLCharacter.getBeeosity(match.item().getName());
      } else {
        negEquip.add(match.item());
      }
    }

    void setBonus(MaximizerTermRegistry.ParsedTerm term) {
      var match =
          ItemFinder.getFirstMatchingItemWithMode(term.keyword().substring(6).trim(), Match.EQUIP);
      if (match == null) {
        this.failed = true;
        return;
      }
      if (match.mode() == null) {
        var existing = bonuses.get(match.item());
        var modes = existing == null ? new HashMap<String, Double>() : existing.modes();
        bonuses.put(match.item(), new ItemBonus(term.weight(), modes));
      } else {
        bonuses
            .computeIfAbsent(match.item(), k -> new ItemBonus(0.0, new HashMap<>()))
            .modes()
            .put(match.mode(), term.weight());
      }
    }

    void setLetterBonus(MaximizerTermRegistry.ParsedTerm term) {
      String letters = term.keyword().substring(6).trim();
      if (letters.isEmpty()) {
        bonusFunc.add(new BonusFunction(LetterBonus::letterBonus, term.weight()));
      } else {
        bonusFunc.add(new BonusFunction(ar -> LetterBonus.letterBonus(ar, letters), term.weight()));
      }
    }

    void setNumberBonus(MaximizerTermRegistry.ParsedTerm term) {
      bonusFunc.add(new BonusFunction(LetterBonus::numberBonus, term.weight()));
    }

    void setPlumber(MaximizerTermRegistry.ParsedTerm term) {
      if (!KoLCharacter.isPlumber()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You are not a Plumber");
        this.failed = true;
        return;
      }
      AdventureResult item = EquipmentManager.getBestPlumberTool(KoLCharacter.getPrimeIndex());
      if (item == null) {
        item = EquipmentManager.getBestPlumberTool(-1);
      }
      posEquip.add(item);
    }

    void setColdPlumber(MaximizerTermRegistry.ParsedTerm term) {
      if (!KoLCharacter.isPlumber()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You are not a Plumber");
        this.failed = true;
        return;
      }
      AdventureResult item = EquipmentManager.getBestPlumberTool(1);
      if (item == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have an appropriate flower to wield");
        this.failed = true;
        return;
      }
      posEquip.add(item);
      posEquip.add(ItemPool.get(ItemPool.FROSTY_BUTTON));
    }

    void setOutfit(MaximizerTermRegistry.ParsedTerm term) {
      String name = term.keyword().substring(6).trim();
      if (name.isEmpty()) {
        name = KoLCharacter.currentStringModifier(StringModifier.OUTFIT);
      }
      SpecialOutfit outfit = EquipmentManager.getMatchingOutfit(name);
      if (outfit == null || outfit.getOutfitId() <= 0) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown or custom outfit: " + name);
        this.failed = true;
        return;
      }
      if (term.weight() > 0.0) {
        posOutfits.add(outfit.getName());
        int bees = 0;
        for (AdventureResult piece : outfit.getPieces()) {
          bees += KoLCharacter.getBeeosity(piece.getName());
        }
        this.outfitBeeosity = Math.max(this.outfitBeeosity, bees);
      } else {
        negOutfits.add(outfit.getName());
      }
    }

    void setFamiliar(MaximizerTermRegistry.ParsedTerm term) {
      if (KoLCharacter.inPokefam()) {
        return;
      }
      String name = term.keyword().substring(7).trim();
      int id = FamiliarDatabase.getFamiliarId(name);
      if (id == -1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown familiar: " + name);
        this.failed = true;
        return;
      }
      if (this.hadFamiliar && term.weight() < 0.0) return;
      FamiliarData familiar = KoLCharacter.usableFamiliar(id);
      if (familiar == null && term.weight() > 1.0) {
        familiar = new FamiliarData(id);
        familiar.setWeight((int) term.weight());
      }
      this.hadFamiliar = familiar != null;
      if (familiar != null
          && !familiar.equals(KoLCharacter.getFamiliar())
          && familiar.canEquip()
          && !familiars.contains(familiar)) {
        familiars.add(familiar);
      }
    }

    boolean apply(MaximizerTermRegistry.ParsedTerm term) {
      if (MaximizerTermRegistry.applyDirective(this, term)) {
        return !this.failed;
      }

      String keyword = term.keyword();
      double weight = term.weight();
      Slot slot = EquipmentRequest.slotNumber(keyword);
      if (SlotSet.ALL_SLOTS.contains(slot)) {
        slots.merge(slot, (int) weight, Integer::sum);
        return true;
      }

      this.index = DoubleModifier.byCaselessName(keyword);
      if (this.index == null) {
        keyword = MaximizerTermRegistry.normalize(keyword);
        this.index = DoubleModifier.byCaselessName(keyword);
      }

      if (this.index == null) {
        BooleanModifier modifier = BooleanModifier.byCaselessName(keyword);
        if (modifier != null) {
          booleanMask.add(modifier);
          if (weight > 0.0) {
            booleanValue.add(modifier);
          }
          return true;
        }
      }

      if (this.index == null) {
        var definition = MaximizerTermRegistry.find(keyword);
        if (definition != null) {
          definition.apply(Evaluator.this.weight, weight);
          this.index = definition.primaryModifier();
          if (definition.disablesTiebreaker()) {
            noTiebreaker = true;
          }
          if (definition.disablesBeeosity()) {
            beeosity = 999;
          }
          if (definition.includesUnderwaterCombatRate()
              && AdventureDatabase.isUnderwater(Modifiers.currentLocation)) {
            Evaluator.this.weight.set(DoubleModifier.UNDERWATER_COMBAT_RATE, weight);
          }
          if (this.index != null) {
            explicitScoreModifiers.add(this.index);
          }
          return true;
        }
      }

      if (this.index != null) {
        explicitScoreModifiers.add(this.index);
        Evaluator.this.weight.set(this.index, weight);
        return true;
      }

      KoLmafia.updateDisplay(MafiaState.ERROR, "Unrecognized keyword: " + keyword);
      return false;
    }

    void finish() {
      if (!this.forceCurrent && noTiebreaker) {
        current = true;
      }
      beeosity = Math.max(Math.max(beeosity, this.equipBeeosity), this.outfitBeeosity);
    }
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean forceModeable(ItemFinder.ItemWithMode modeable, String mode) {
    String existing = forcedModeables.get(modeable.modeable());
    if (!existing.isEmpty() && !existing.equals(mode)) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          "Conflicting modes requested for "
              + modeable.item().getName()
              + ": "
              + existing
              + " vs "
              + mode);
      return false;
    }
    forcedModeables.put(modeable.modeable(), mode);
    return true;
  }

  private void parse(String expr) {
    ParseState state = new ParseState();
    if (!MaximizerExpressionParser.parse(expr, state)) {
      return;
    }

    // Make sure indirect sources have at least a little weight;
    addFudge(
        DoubleModifier.EXPERIENCE,
        DoubleModifier.MONSTER_LEVEL,
        DoubleModifier.MONSTER_LEVEL_PERCENT,
        DoubleModifier.MUS_EXPERIENCE,
        DoubleModifier.MYS_EXPERIENCE,
        DoubleModifier.MOX_EXPERIENCE,
        DoubleModifier.MUS_EXPERIENCE_PCT,
        DoubleModifier.MYS_EXPERIENCE_PCT,
        DoubleModifier.MOX_EXPERIENCE_PCT,
        DoubleModifier.VOLLEYBALL_WEIGHT,
        DoubleModifier.SOMBRERO_WEIGHT,
        DoubleModifier.VOLLEYBALL_EFFECTIVENESS,
        DoubleModifier.SOMBRERO_EFFECTIVENESS,
        DoubleModifier.SOMBRERO_BONUS);

    addFudge(
        DoubleModifier.ITEMDROP,
        DoubleModifier.FOODDROP,
        DoubleModifier.BOOZEDROP,
        DoubleModifier.HATDROP,
        DoubleModifier.WEAPONDROP,
        DoubleModifier.OFFHANDDROP,
        DoubleModifier.SHIRTDROP,
        DoubleModifier.PANTSDROP,
        DoubleModifier.ACCESSORYDROP,
        DoubleModifier.CANDYDROP,
        DoubleModifier.GEARDROP,
        DoubleModifier.FAIRY_WEIGHT,
        DoubleModifier.FAIRY_EFFECTIVENESS,
        DoubleModifier.SPORADIC_ITEMDROP,
        DoubleModifier.PICKPOCKET_CHANCE);

    addFudge(
        DoubleModifier.MEATDROP,
        DoubleModifier.LEPRECHAUN_WEIGHT,
        DoubleModifier.LEPRECHAUN_EFFECTIVENESS,
        DoubleModifier.SPORADIC_MEATDROP,
        DoubleModifier.MEAT_BONUS);

    addFudge(DoubleModifier.DAMAGE_AURA, DoubleModifier.SPORADIC_DAMAGE_AURA);
    addFudge(DoubleModifier.THORNS, DoubleModifier.SPORADIC_THORNS);
  }

  private void addFudge(DoubleModifier source, DoubleModifier... extras) {
    final double fudge = this.weight.getDouble(source) * 0.0001f;
    if (fudge > 0) {
      for (var extra : extras) {
        this.weight.increment(extra, fudge);
      }
    }
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
    if (this.stinkycheese > 0) {
      int val = mods.getBitmap(BitmapModifier.STINKYCHEESE);
      score += this.stinkycheese * val;
    }
    if (!this.bonuses.isEmpty() || !this.bonusFunc.isEmpty()) {
      for (AdventureResult item : equipment.values()) {
        score += this.getItemScore(item, modeables);
      }
    }
    // Add fudge factor for Rollover Effect
    if (mods.hasString(StringModifier.ROLLOVER_EFFECT)) {
      score += 0.01f;
    }
    if (score < this.totalMin) failed = true;
    boolean exceeded = score >= this.totalMax;
    // Score bitmap objectives 1:1 up to the requested target, which must be reached.
    if (this.clownosity > 0) {
      int osity = mods.getBitmap(BitmapModifier.CLOWNINESS);
      score += Math.min(osity, this.clownosity);
      if (osity < this.clownosity) failed = true;
    }
    if (this.raveosity > 0) {
      int osity = mods.getBitmap(BitmapModifier.RAVEOSITY);
      score += Math.min(osity, this.raveosity);
      if (osity < this.raveosity) failed = true;
    }
    if (this.surgeonosity > 0) {
      int osity = mods.getBitmap(BitmapModifier.SURGEONOSITY);
      score += Math.min(osity, this.surgeonosity);
      if (osity < this.surgeonosity) failed = true;
    }
    if (!failed
        && !this.booleanMask.isEmpty()
        && !mods.getBooleans(this.booleanMask).equals(this.booleanValue)) {
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
        this.noTiebreaker);
  }

  double getItemScore(AdventureResult item, Map<Modeable, String> modeables) {
    double score = 0.0;
    ItemBonus itemBonus = this.bonuses.get(item);
    if (itemBonus != null) {
      score += itemBonus.base();
      var modeable = Modeable.find(item);
      if (modeable != null) {
        var mode = modeables.get(modeable);
        if (mode != null) {
          score += itemBonus.modes().getOrDefault(mode, 0.0);
        }
      }
    }
    for (BonusFunction func : this.bonusFunc) {
      score += func.bonusFunction.apply(item) * func.weight;
    }
    return score;
  }

  EvaluationOutcome evaluateComplete(
      Modifiers mods,
      Map<Slot, AdventureResult> equipment,
      Map<Modeable, String> modeables,
      boolean resourceLimitExceeded,
      int allowedMutexViolations) {
    var outcome = this.evaluate(mods, equipment, modeables);
    boolean failed = this.failsEquipment(mods, equipment, outcome.failed());
    if (resourceLimitExceeded) {
      failed = true;
    }
    if ((mods.getRawBitmap(BitmapModifier.MUTEX_VIOLATIONS) & ~allowedMutexViolations) != 0) {
      failed = true;
    }
    return new EvaluationOutcome(outcome.score(), failed, outcome.exceeded());
  }

  private boolean failsEquipment(
      Modifiers mods, Map<Slot, AdventureResult> equipment, boolean scoreFailed) {
    boolean failed = scoreFailed;
    boolean outfitSatisfied = this.posOutfits.isEmpty();
    boolean equipSatisfied = this.posEquip.isEmpty();
    if (!failed && !this.posEquip.isEmpty()) {
      equipSatisfied = true;
      for (AdventureResult item : this.posEquip) {
        if (!this.hasEquipped(equipment, item)) {
          equipSatisfied = false;
          break;
        }
      }
    }
    if (!failed && this.negEquip.stream().anyMatch(item -> this.hasEquipped(equipment, item))) {
      failed = true;
    }
    if (!failed) {
      String outfit = mods.getString(StringModifier.OUTFIT);
      if (this.negOutfits.contains(outfit)) {
        failed = true;
      } else {
        outfitSatisfied = this.posOutfits.contains(outfit) || this.posOutfits.isEmpty();
      }
    }
    if (!outfitSatisfied || !equipSatisfied) {
      failed = true;
    }
    return failed;
  }

  int beeosityLimit() {
    return this.beeosity;
  }

  private boolean hasEquipped(Map<Slot, AdventureResult> equipment, AdventureResult item) {
    if (KoLCharacter.hasEquipped(equipment, item)) {
      return true;
    }

    boolean wearingCodpiece =
        KoLCharacter.hasEquipped(
            equipment, ItemPool.get(ItemPool.THE_ETERNITY_CODPIECE), SlotSet.ACCESSORY_SLOTS);
    return wearingCodpiece
        && EquipmentDatabase.isCodpieceGem(item.getItemId())
        && KoLCharacter.hasEquipped(equipment, item, SlotSet.CODPIECE_SLOTS);
  }

  boolean slotEnabled(Slot slot) {
    int threshold = this.slots.values().stream().anyMatch(value -> value > 0) ? 1 : 0;
    return this.slots.getOrDefault(slot, 0) >= threshold;
  }

  double getTiebreaker(Modifiers mods) {
    if (this.noTiebreaker) return 0.0;
    return this.tiebreaker.getScore(mods);
  }

  boolean isUsingTiebreaker() {
    return !this.noTiebreaker;
  }

  boolean areScoreModifiersSaturated(Modifiers modifiers) {
    Map<DerivedModifier, Integer> predicted = null;
    for (ScoreTerm term : this.activeScoreModifiers) {
      DoubleModifier modifier = term.modifier();
      if ((modifier == DoubleModifier.MUS
              || modifier == DoubleModifier.MYS
              || modifier == DoubleModifier.MOX
              || modifier == DoubleModifier.HP
              || modifier == DoubleModifier.MP)
          && predicted == null) {
        predicted = modifiers.predict();
      }
      double value = scoreValue(modifier, modifiers, predicted);
      if ((term.weight() > 0.0 && value < term.max())
          || (term.weight() < 0.0 && value > term.min())) {
        return false;
      }
    }
    return true;
  }

  boolean isWeaponTypeRequired() {
    return this.requireClub
        || this.requireUtensil
        || this.requireSword
        || this.requireKnife
        || this.requireAccordion;
  }

  boolean isShieldRequired() {
    return this.requireShield;
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
    if (mods == null) return Constraint.IRRELEVANT;
    EnumSet<BooleanModifier> bools = mods.getBooleans(this.booleanMask);
    if (!this.booleanValue.containsAll(bools)) return Constraint.VIOLATES;
    if (!bools.isEmpty()) return Constraint.MEETS;
    return Constraint.IRRELEVANT;
  }

  boolean requiresEquipment(AdventureResult item) {
    return this.posEquip.contains(item);
  }

  boolean excludesEquipment(AdventureResult item) {
    return this.negEquip.contains(item);
  }

  boolean currentOnly() {
    return this.current;
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
    CharacterSnapshot character = Maximizer.character();
    double nullScore = this.getScore(new Modifiers());
    double nullTiebreaker = this.getTiebreaker(new Modifiers());

    EquipmentSetEvaluator setEvaluator =
        new EquipmentSetEvaluator(
            this,
            this.posOutfits,
            this.negOutfits,
            equipScope,
            maxPrice,
            priceLevel,
            this.dump,
            nullScore);
    Map<Integer, Boolean> usefulOutfits = setEvaluator.usefulOutfits();
    Map<AdventureResult, AdventureResult> outfitPieces = setEvaluator.outfitPieces();

    var ordinaryCandidates =
        new OrdinaryCandidateCompiler(
                this,
                character,
                setEvaluator,
                new OrdinaryCandidateCompiler.Options(
                    this.familiars,
                    this.slots,
                    this.forcedModeables,
                    this.current,
                    this.clownosity,
                    this.raveosity,
                    this.surgeonosity,
                    this.stinkycheese,
                    new EquipmentCandidateSlotter.Requirements(
                        this.hands,
                        this.melee,
                        this.weaponType,
                        this.requireShield,
                        this.requireClub,
                        this.requireUtensil,
                        this.requireSword,
                        this.requireKnife,
                        this.requireAccordion,
                        this.effective),
                    this.weight.getDouble(DoubleModifier.ITEMDROP) > 0,
                    this.weight.getDouble(DoubleModifier.EXPERIENCE) > 0
                        || this.weight.getDouble(DoubleModifier.MUS_EXPERIENCE) > 0
                        || this.weight.getDouble(DoubleModifier.MYS_EXPERIENCE) > 0
                        || this.weight.getDouble(DoubleModifier.MOX_EXPERIENCE) > 0,
                    equipScope,
                    maxPrice,
                    priceLevel,
                    nullScore))
            .compile();
    SlotList<CheckedItem> catalog = ordinaryCandidates.catalog();
    SlotList<CheckedItem> ranked = ordinaryCandidates.ranked();

    var codpieceCandidates =
        this.codpieceEvaluator.compileCandidates(
            equipScope, maxPrice, priceLevel, nullScore, nullTiebreaker);
    catalog.get(Slot.CODPIECE1).addAll(codpieceCandidates.catalog());
    ranked.get(Slot.CODPIECE1).addAll(codpieceCandidates.ranked());
    boolean codpieceCanExpandAccessoryPool =
        this.codpieceEvaluator.prepareAccessoryCandidates(
            codpieceCandidates.ranked(),
            ranked.get(Slot.ACCESSORY1),
            SlotSet.CODPIECE_SLOTS.stream().anyMatch(this::slotEnabled));

    var carriedFamiliarSelection =
        CarriedFamiliarSelector.select(
            ordinaryCandidates.carriedFamiliarsNeeded(),
            this.slots.getOrDefault(Slot.CROWNOFTHRONES, 0) < 0,
            this.slots.getOrDefault(Slot.BUDDYBJORN, 0) < 0,
            character,
            equipScope,
            maxPrice,
            priceLevel);
    this.carriedFamiliars.addAll(carriedFamiliarSelection.candidates());
    FamiliarData useCrownFamiliar = carriedFamiliarSelection.lockedCrown();
    FamiliarData useBjornFamiliar = carriedFamiliarSelection.lockedBjorn();

    CheckedItem bestCard =
        CardSleeveSelector.select(
            ordinaryCandidates.cardNeeded(), equipScope, maxPrice, priceLevel);
    AdventureResult useCard = null;

    Map<Modeable, String> bestModes =
        ModeableSelector.select(
            ordinaryCandidates.modeablesNeeded(),
            forcedModeables,
            equipScope,
            maxPrice,
            priceLevel);
    CandidateSpeculationFactory speculationFactory =
        new CandidateSpeculationFactory(
            ordinaryCandidates.carriedFamiliarsNeeded(),
            carriedFamiliarSelection,
            bestCard,
            bestModes);
    var speculationCompilation =
        speculationFactory.compile(
            ranked, catalog, this.familiars, equipScope, maxPrice, priceLevel);
    SlotList<MaximizerSpeculation> speculationList = speculationCompilation.speculations();
    useCard = speculationCompilation.card();
    int catalogCandidateCount = speculationCompilation.catalogCount();

    setEvaluator.evaluate(speculationList);

    var shortlist =
        new CandidateShortlistCompiler(
                this.familiars, character, equipScope, maxPrice, priceLevel, this.dump)
            .compile(ranked, speculationList, codpieceCanExpandAccessoryPool);
    SlotList<CheckedItem> automatic = shortlist.candidates();
    Maximizer.recordCandidateCounts(catalogCandidateCount, shortlist.candidateCount());

    new EquipmentSearchRunner(
            this.familiars,
            this.carriedFamiliars,
            usefulOutfits,
            outfitPieces,
            new EquipmentSearchRunner.Options(
                this.slots,
                bestModes,
                useCard,
                useCrownFamiliar,
                useBjornFamiliar,
                maxPrice,
                priceLevel,
                exhaustive))
        .run(automatic, catalog);
  }

  List<CheckedItem> prioritizeCodpieceGems(List<CheckedItem> gems) {
    return this.codpieceEvaluator.prioritize(gems);
  }
}
