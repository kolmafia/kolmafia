package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
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
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifierCollection;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

/**
 * The terms of one maximizer expression.
 *
 * <p>The static tables map each keyword onto the {@link Action} that applies it, so every
 * registration owns its own setting behaviour. An instance holds the values those actions write,
 * and answers the questions {@link Evaluator} asks about them.
 */
final class MaximizerTermRegistry {
  record ParsedTerm(String keyword, double weight, boolean explicitWeight) {}

  enum IntegerSetting {
    DUMP(0),
    HANDS(0),
    MELEE(0),
    CLOWNOSITY(0),
    RAVEOSITY(0),
    SURGEONOSITY(0),
    STINKYCHEESE(0),
    BEEOSITY(2);

    private final int defaultValue;

    IntegerSetting(int defaultValue) {
      this.defaultValue = defaultValue;
    }
  }

  private enum WeaponRequirement {
    CLUB,
    SHIELD,
    UTENSIL,
    SWORD,
    KNIFE,
    ACCORDION
  }

  /** What a registered keyword does to the expression it appears in. */
  @FunctionalInterface
  private interface Action {
    void apply(MaximizerTermRegistry terms, ParsedTerm term);

    default Action andThen(Action next) {
      return (terms, term) -> {
        this.apply(terms, term);
        next.apply(terms, term);
      };
    }
  }

  private record Alias(Predicate<String> matches, Action action) {}

  private record Rewrite(Predicate<String> matches, UnaryOperator<String> rewrite) {}

  private record ItemBonus(double base, Map<String, Double> modes) {}

  private record BonusFunction(Function<AdventureResult, Double> bonusFunction, double weight) {}

  private static final class Requirements<T> {
    private final Set<T> required = new HashSet<>();
    private final Set<T> excluded = new HashSet<>();

    void add(T value, double weight) {
      (weight > 0.0 ? this.required : this.excluded).add(value);
    }
  }

  private static final Map<String, Action> EXACT_DIRECTIVES = new LinkedHashMap<>();
  private static final List<Map.Entry<String, Action>> PREFIX_DIRECTIVES = new ArrayList<>();
  private static final Map<String, Action> EXACT_TERMS = new LinkedHashMap<>();
  private static final List<Alias> ALIAS_TERMS = new ArrayList<>();
  private static final List<Rewrite> REWRITES = new ArrayList<>();
  private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");

  static {
    suffixRewrite(" res", " resistance");
    suffixRewrite(" dmg percent", " damage percent");
    suffixRewrite(" dmg", " damage");
    suffixRewrite(" exp", " experience");
    prefixRewrite("organ", ignored -> "organ capacity");

    directive("min", MaximizerTermRegistry::setMinimum);
    directive("max", MaximizerTermRegistry::setMaximum);
    directive("dump", integerSetting(IntegerSetting.DUMP));
    prefixDirective("hand", integerSetting(IntegerSetting.HANDS));
    prefixDirective(
        "tie", flag(term -> term.weight() < 0.0, (terms, value) -> terms.noTiebreaker = value));
    prefixDirective(
        "current",
        flag((terms, value) -> terms.current = value)
            .andThen((terms, term) -> terms.forceCurrent = true));
    prefixDirective("type ", text(5, (terms, value) -> terms.weaponType = value));
    directive("club", weaponRequirement(WeaponRequirement.CLUB));
    directive(
        "shield",
        weaponRequirement(WeaponRequirement.SHIELD).andThen(MaximizerTermRegistry::prepareShield));
    directive("utensil", weaponRequirement(WeaponRequirement.UTENSIL));
    directive("sword", weaponRequirement(WeaponRequirement.SWORD));
    directive("knife", weaponRequirement(WeaponRequirement.KNIFE));
    directive("accordion", weaponRequirement(WeaponRequirement.ACCORDION));
    directive("melee", integerSetting(IntegerSetting.MELEE, term -> (int) (term.weight() * 2.0)));
    directive("effective", flag((terms, value) -> terms.effective = value));
    directive("empty", MaximizerTermRegistry::setEmpty);
    directive(
        "clownosity",
        integerSetting(
            IntegerSetting.CLOWNOSITY,
            term -> term.explicitWeight() ? (int) term.weight() * 25 : 100));
    directive(
        "raveosity",
        integerSetting(
            IntegerSetting.RAVEOSITY, term -> term.explicitWeight() ? (int) term.weight() : 7));
    directive(
        "surgeonosity",
        integerSetting(
            IntegerSetting.SURGEONOSITY, term -> term.explicitWeight() ? (int) term.weight() : 5));
    directive("beeosity", integerSetting(IntegerSetting.BEEOSITY));
    directive("stinkycheese", integerSetting(IntegerSetting.STINKYCHEESE));
    alias("stinky cheese", "stinkycheese");
    directive("sea", MaximizerTermRegistry::setSea);
    prefixDirective("equip ", MaximizerTermRegistry::setEquipment);
    prefixDirective("bonus ", MaximizerTermRegistry::setBonus);
    prefixDirective("letter", MaximizerTermRegistry::setLetterBonus);
    directive("number", MaximizerTermRegistry::setNumberBonus);
    directive("plumber", MaximizerTermRegistry::setPlumber);
    directive("cold plumber", MaximizerTermRegistry::setColdPlumber);
    prefixDirective("outfit", MaximizerTermRegistry::setOutfit);
    prefixDirective("switch ", MaximizerTermRegistry::setFamiliar);

    term(
        "all resistance",
        aggregate(
            DoubleModifier.COLD_RESISTANCE,
            DoubleModifier.HOT_RESISTANCE,
            DoubleModifier.SLEAZE_RESISTANCE,
            DoubleModifier.SPOOKY_RESISTANCE,
            DoubleModifier.STENCH_RESISTANCE));
    term(
        "elemental damage",
        aggregate(
            DoubleModifier.COLD_DAMAGE,
            DoubleModifier.HOT_DAMAGE,
            DoubleModifier.SLEAZE_DAMAGE,
            DoubleModifier.SPOOKY_DAMAGE,
            DoubleModifier.STENCH_DAMAGE));
    term("hp regen", aggregate(0.5, DoubleModifier.HP_REGEN_MIN, DoubleModifier.HP_REGEN_MAX));
    term("mp regen", aggregate(0.5, DoubleModifier.MP_REGEN_MIN, DoubleModifier.MP_REGEN_MAX));
    term("passive damage", aggregate(DoubleModifier.DAMAGE_AURA, DoubleModifier.THORNS));
    term(
        "organ capacity",
        aggregate(
            DoubleModifier.STOMACH_CAPACITY,
            DoubleModifier.LIVER_CAPACITY,
            DoubleModifier.SPLEEN_CAPACITY));

    term("init", modifier(DoubleModifier.INITIATIVE));
    term("hp", modifier(DoubleModifier.HP));
    term("mp", modifier(DoubleModifier.MP));
    term("da", modifier(DoubleModifier.DAMAGE_ABSORPTION));
    term("dr", modifier(DoubleModifier.DAMAGE_REDUCTION));
    term("ml", modifier(DoubleModifier.MONSTER_LEVEL));
    term(
        "ocrs",
        modifier(DoubleModifier.RANDOM_MONSTER_MODIFIERS)
            .andThen(dropTiebreaker())
            .andThen(ignoreBees()));

    pattern("^mus(cle)? exp(erience)? perc(ent(age)?)?", DoubleModifier.MUS_EXPERIENCE_PCT);
    pattern("^mus(cle)? exp(erience)?", DoubleModifier.MUS_EXPERIENCE);
    pattern("^mus(cle)? perc(ent(age)?)?", DoubleModifier.MUS_PCT);
    pattern(
        "^mys(t(ical(ity)?)?)? exp(erience)? perc(ent(age)?)?", DoubleModifier.MYS_EXPERIENCE_PCT);
    pattern("^mys(t(ical(ity)?)?)? exp(erience)?", DoubleModifier.MYS_EXPERIENCE);
    pattern("^mys(t(ical(ity)?)?)? perc(ent(age)?)?", DoubleModifier.MYS_PCT);
    pattern("^mox(ie)? exp(erience)? perc(ent(age)?)?", DoubleModifier.MOX_EXPERIENCE_PCT);
    pattern("^mox(ie)? exp(erience)?", DoubleModifier.MOX_EXPERIENCE);
    pattern("^mox(ie)? perc(ent(age)?)?", DoubleModifier.MOX_PCT);

    prefixTerm("mus", modifier(DoubleModifier.MUS));
    prefixTerm("mys", modifier(DoubleModifier.MYS));
    prefixTerm("mox", modifier(DoubleModifier.MOX));
    prefixTerm(
        "main", (terms, term) -> terms.scoreModifier(DoubleModifier.primeStat(), term.weight()));
    prefixTerm("com", modifier(DoubleModifier.COMBAT_RATE).andThen(matchUnderwaterCombatRate()));
    prefixTerm("item", modifier(DoubleModifier.ITEMDROP));
    prefixTerm("meat", modifier(DoubleModifier.MEATDROP));
    prefixTerm("adv", modifier(DoubleModifier.ADVENTURES).andThen(ignoreBees()));
    prefixTerm("fites", modifier(DoubleModifier.PVP_FIGHTS).andThen(ignoreBees()));
    prefixTerm("exp", modifier(DoubleModifier.EXPERIENCE));
    prefixTerm("crit", modifier(DoubleModifier.CRITICAL_PCT));
    prefixTerm("spell crit", modifier(DoubleModifier.SPELL_CRITICAL_PCT));
    prefixTerm("sprinkle", modifier(DoubleModifier.SPRINKLES));
    prefixTerm("stomach", modifier(DoubleModifier.STOMACH_CAPACITY));
    prefixTerm("liver", modifier(DoubleModifier.LIVER_CAPACITY));
    prefixTerm("spleen", modifier(DoubleModifier.SPLEEN_CAPACITY));
  }

  private final DoubleModifierCollection weight = new DoubleModifierCollection();
  private final Map<DoubleModifier, Double> min = new EnumMap<>(DoubleModifier.class);
  private final Map<DoubleModifier, Double> max = new EnumMap<>(DoubleModifier.class);
  private final EnumMap<IntegerSetting, Integer> integers = new EnumMap<>(IntegerSetting.class);
  private double totalMin = Double.NEGATIVE_INFINITY;
  private double totalMax = Double.POSITIVE_INFINITY;
  private final EnumSet<BooleanModifier> booleanMask = EnumSet.noneOf(BooleanModifier.class);
  private final Set<BooleanModifier> booleanValue = EnumSet.noneOf(BooleanModifier.class);
  private final List<FamiliarData> familiars = new ArrayList<>();
  // Some modeables are forced based on certain expressions appearing in a maximize call
  // For example, if you request "sea" the Crown of Ed will always pick fish. This does pose
  // an issue if the maximizer would choose the SCUBA gear to provide water-breathing, as it would
  // not consider a different mode for the Crown. e.g. "maximize sea, ml" would not consider the
  // "bear" mode for the hat. Something for someone to fix in the future.
  private final Map<Modeable, String> forcedModeables = Modeable.getStringMap(m -> "");

  /** if slots[i] >= 0 then equipment of type i can be considered for maximization */
  private final EnumMap<Slot, Integer> slots = new EnumMap<>(Slot.class);

  private String weaponType = null;
  private boolean effective = false;
  private final EnumSet<WeaponRequirement> weaponRequirements =
      EnumSet.noneOf(WeaponRequirement.class);
  private boolean noTiebreaker = false;
  private boolean current =
      !KoLCharacter.canInteract() || Preferences.getBoolean("maximizerAlwaysCurrent");
  private final Requirements<String> outfits = new Requirements<>();
  private final Requirements<AdventureResult> equipment = new Requirements<>();
  private final Map<AdventureResult, ItemBonus> bonuses = new HashMap<>();
  private final List<BonusFunction> bonusFunc = new ArrayList<>();

  // Only meaningful while an expression is being parsed. The modifier a "min" or "max" applies to
  // is whichever one the expression last mentioned, so it deliberately outlives its own term.
  private DoubleModifier index;
  private boolean hadFamiliar;
  private boolean forceCurrent;
  private int equipBeeosity;
  private int outfitBeeosity;
  private int slotThreshold;
  private boolean failed;

  /** Terms of an expression that inherits no minimums or maximums. */
  MaximizerTermRegistry() {
    for (var mod : DoubleModifier.DOUBLE_MODIFIERS) {
      this.min.put(mod, Double.NEGATIVE_INFINITY);
      this.max.put(mod, Double.POSITIVE_INFINITY);
    }
  }

  /** Terms of an expression that starts from the minimums and maximums of the tiebreaker. */
  MaximizerTermRegistry(MaximizerTermRegistry tiebreaker) {
    this.min.putAll(tiebreaker.min);
    this.max.putAll(tiebreaker.max);
  }

  // Parsing

  /** Applies one parsed term. Returns false if the expression cannot be used. */
  boolean apply(ParsedTerm term) {
    Action directive = directiveFor(term.keyword());
    if (directive != null) {
      directive.apply(this, term);
      return !this.failed;
    }

    String keyword = term.keyword();
    Slot slot = EquipmentRequest.slotNumber(keyword);
    if (SlotSet.ALL_SLOTS.contains(slot)) {
      this.slots.merge(slot, (int) term.weight(), Integer::sum);
      return true;
    }

    this.index = DoubleModifier.byCaselessName(keyword);
    if (this.index == null) {
      keyword = normalize(keyword);
      this.index = DoubleModifier.byCaselessName(keyword);
    }

    if (this.index == null) {
      BooleanModifier modifier = BooleanModifier.byCaselessName(keyword);
      if (modifier != null) {
        this.requireBoolean(modifier, term.weight() > 0.0);
        return true;
      }
    }

    if (this.index == null) {
      Action registered = termFor(keyword);
      if (registered != null) {
        registered.apply(this, term);
        return true;
      }
    }

    if (this.index != null) {
      this.scoreModifier(this.index, term.weight());
      return true;
    }

    KoLmafia.updateDisplay(MafiaState.ERROR, "Unrecognized keyword: " + keyword);
    return false;
  }

  /** Settles the values that depend on the expression as a whole. */
  void finish() {
    if (!this.forceCurrent && this.noTiebreaker) {
      this.current = true;
    }
    this.slotThreshold = this.slots.values().stream().anyMatch(value -> value > 0) ? 1 : 0;
    this.integers.put(
        IntegerSetting.BEEOSITY,
        Math.max(
            Math.max(this.integer(IntegerSetting.BEEOSITY), this.equipBeeosity),
            this.outfitBeeosity));

    // Make sure indirect sources have at least a little weight;
    this.addFudge(
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

    this.addFudge(
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

    this.addFudge(
        DoubleModifier.MEATDROP,
        DoubleModifier.LEPRECHAUN_WEIGHT,
        DoubleModifier.LEPRECHAUN_EFFECTIVENESS,
        DoubleModifier.SPORADIC_MEATDROP,
        DoubleModifier.MEAT_BONUS);

    this.addFudge(DoubleModifier.DAMAGE_AURA, DoubleModifier.SPORADIC_DAMAGE_AURA);
    this.addFudge(DoubleModifier.THORNS, DoubleModifier.SPORADIC_THORNS);
  }

  private void addFudge(DoubleModifier source, DoubleModifier... extras) {
    final double fudge = this.weight.getDouble(source) * 0.0001f;
    if (fudge > 0) {
      for (var extra : extras) {
        this.weight.increment(extra, fudge);
      }
    }
  }

  // What each registered term does

  private void setMinimum(ParsedTerm term) {
    if (this.index != null) {
      this.min.put(this.index, term.weight());
    } else {
      this.totalMin = term.weight();
    }
  }

  private void setMaximum(ParsedTerm term) {
    if (this.index != null) {
      this.max.put(this.index, term.weight());
    } else {
      this.totalMax = term.weight();
    }
  }

  private void prepareShield(ParsedTerm term) {
    if (this.forcedModeables.get(Modeable.UMBRELLA).isEmpty()) {
      this.forcedModeables.put(Modeable.UMBRELLA, "forward-facing");
    }
    this.integers.put(IntegerSetting.HANDS, 1);
  }

  private void setEmpty(ParsedTerm term) {
    for (var slot : SlotSet.ALL_SLOTS) {
      this.slots.merge(
          slot,
          ((int) term.weight())
              * (EquipmentManager.getEquipment(slot).equals(EquipmentRequest.UNEQUIP) ? 1 : -1),
          Integer::sum);
    }
  }

  private void setSea(ParsedTerm term) {
    var adventureUnderwater =
        EnumSet.of(BooleanModifier.ADVENTURE_UNDERWATER, BooleanModifier.UNDERWATER_FAMILIAR);
    this.booleanMask.addAll(adventureUnderwater);
    this.booleanValue.addAll(adventureUnderwater);
    this.index = null;
    if (this.forcedModeables.get(Modeable.EDPIECE).isEmpty()) {
      this.forcedModeables.put(Modeable.EDPIECE, "fish");
    }
  }

  private void setEquipment(ParsedTerm term) {
    var match =
        ItemFinder.getFirstMatchingItemWithMode(term.keyword().substring(6).trim(), Match.EQUIP);
    if (match == null || (match.modeable() != null && !this.forceModeable(match, match.mode()))) {
      this.failed = true;
      return;
    }
    this.equipment.add(match.item(), term.weight());
    if (term.weight() > 0.0) {
      this.equipBeeosity += KoLCharacter.getBeeosity(match.item().getName());
    }
  }

  private void setBonus(ParsedTerm term) {
    var match =
        ItemFinder.getFirstMatchingItemWithMode(term.keyword().substring(6).trim(), Match.EQUIP);
    if (match == null) {
      this.failed = true;
      return;
    }
    if (match.mode() == null) {
      var existing = this.bonuses.get(match.item());
      var modes = existing == null ? new HashMap<String, Double>() : existing.modes();
      this.bonuses.put(match.item(), new ItemBonus(term.weight(), modes));
    } else {
      this.bonuses
          .computeIfAbsent(match.item(), k -> new ItemBonus(0.0, new HashMap<>()))
          .modes()
          .put(match.mode(), term.weight());
    }
  }

  private void setLetterBonus(ParsedTerm term) {
    String letters = term.keyword().substring(6).trim();
    if (letters.isEmpty()) {
      this.bonusFunc.add(new BonusFunction(MaximizerTermRegistry::letterBonus, term.weight()));
    } else {
      this.bonusFunc.add(new BonusFunction(item -> letterBonus(item, letters), term.weight()));
    }
  }

  private void setNumberBonus(ParsedTerm term) {
    this.bonusFunc.add(new BonusFunction(MaximizerTermRegistry::numberBonus, term.weight()));
  }

  private void setPlumber(ParsedTerm term) {
    if (!KoLCharacter.isPlumber()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You are not a Plumber");
      this.failed = true;
      return;
    }
    AdventureResult item = EquipmentManager.getBestPlumberTool(KoLCharacter.getPrimeIndex());
    if (item == null) {
      item = EquipmentManager.getBestPlumberTool(-1);
    }
    this.equipment.required.add(item);
  }

  private void setColdPlumber(ParsedTerm term) {
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
    this.equipment.required.add(item);
    this.equipment.required.add(ItemPool.get(ItemPool.FROSTY_BUTTON));
  }

  private void setOutfit(ParsedTerm term) {
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
    this.outfits.add(outfit.getName(), term.weight());
    if (term.weight() > 0.0) {
      int bees = 0;
      for (AdventureResult piece : outfit.getPieces()) {
        bees += KoLCharacter.getBeeosity(piece.getName());
      }
      this.outfitBeeosity = Math.max(this.outfitBeeosity, bees);
    }
  }

  private void setFamiliar(ParsedTerm term) {
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
        && !this.familiars.contains(familiar)) {
      this.familiars.add(familiar);
    }
  }

  private void scoreModifier(DoubleModifier modifier, double weight) {
    this.index = modifier;
    this.weight.set(modifier, weight);
  }

  private void scoreAggregate(List<DoubleModifier> modifiers, double factor, double weight) {
    modifiers.forEach(modifier -> this.weight.set(modifier, weight * factor));
    this.index = null;
  }

  private void requireBoolean(BooleanModifier modifier, boolean required) {
    this.booleanMask.add(modifier);
    if (required) {
      this.booleanValue.add(modifier);
    }
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean forceModeable(ItemFinder.ItemWithMode modeable, String mode) {
    String existing = this.forcedModeables.get(modeable.modeable());
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
    this.forcedModeables.put(modeable.modeable(), mode);
    return true;
  }

  // What the terms mean to the rest of the maximizer

  /** The modifiers that contribute to a score, with the bounds each of them must respect. */
  List<Evaluator.ScoreTerm> scoreTerms() {
    var active = new ArrayList<Evaluator.ScoreTerm>();
    for (var modifier : DoubleModifier.DOUBLE_MODIFIERS) {
      double weight = this.weight.getDouble(modifier);
      double min = this.min.get(modifier);
      if (weight == 0.0 && min == Double.NEGATIVE_INFINITY) {
        continue;
      }
      active.add(new Evaluator.ScoreTerm(modifier, weight, min, this.max.get(modifier)));
    }
    return List.copyOf(active);
  }

  double totalMin() {
    return this.totalMin;
  }

  double totalMax() {
    return this.totalMax;
  }

  int integer(IntegerSetting setting) {
    return this.integers.getOrDefault(setting, setting.defaultValue);
  }

  boolean usesTiebreaker() {
    return !this.noTiebreaker;
  }

  boolean currentOnly() {
    return this.current;
  }

  List<FamiliarData> familiars() {
    return this.familiars;
  }

  Map<Modeable, String> forcedModeables() {
    return this.forcedModeables;
  }

  EnumMap<Slot, Integer> slots() {
    return this.slots;
  }

  Set<String> posOutfits() {
    return this.outfits.required;
  }

  Set<String> negOutfits() {
    return this.outfits.excluded;
  }

  boolean slotEnabled(Slot slot) {
    return this.slots.getOrDefault(slot, 0) >= this.slotThreshold;
  }

  EquipmentCandidateSlotter.Requirements requirements() {
    return new EquipmentCandidateSlotter.Requirements(
        this.integer(IntegerSetting.HANDS),
        this.integer(IntegerSetting.MELEE),
        this.weaponType,
        this.weaponRequirements.contains(WeaponRequirement.SHIELD),
        this.weaponRequirements.contains(WeaponRequirement.CLUB),
        this.weaponRequirements.contains(WeaponRequirement.UTENSIL),
        this.weaponRequirements.contains(WeaponRequirement.SWORD),
        this.weaponRequirements.contains(WeaponRequirement.KNIFE),
        this.weaponRequirements.contains(WeaponRequirement.ACCORDION),
        this.effective);
  }

  boolean isWeaponTypeRequired() {
    return this.weaponRequirements.stream()
        .anyMatch(requirement -> requirement != WeaponRequirement.SHIELD);
  }

  boolean isShieldRequired() {
    return this.weaponRequirements.contains(WeaponRequirement.SHIELD);
  }

  /** Whether a weapon of a particular type is mandatory, which rules out fighting unarmed. */
  boolean forbidsUnarmed() {
    int melee = this.integer(IntegerSetting.MELEE);
    return melee < -1 || melee > 1;
  }

  boolean itemDropUseful() {
    return this.weight.getDouble(DoubleModifier.ITEMDROP) > 0;
  }

  boolean experienceUseful() {
    return this.weight.getDouble(DoubleModifier.EXPERIENCE) > 0
        || this.weight.getDouble(DoubleModifier.MUS_EXPERIENCE) > 0
        || this.weight.getDouble(DoubleModifier.MYS_EXPERIENCE) > 0
        || this.weight.getDouble(DoubleModifier.MOX_EXPERIENCE) > 0;
  }

  boolean hasNonModifierScore() {
    return this.integer(IntegerSetting.CLOWNOSITY) > 0
        || this.integer(IntegerSetting.RAVEOSITY) > 0
        || this.integer(IntegerSetting.SURGEONOSITY) > 0
        || this.integer(IntegerSetting.STINKYCHEESE) > 0
        || !this.bonuses.isEmpty()
        || !this.bonusFunc.isEmpty();
  }

  boolean requiresEquipment(AdventureResult item) {
    return this.equipment.required.contains(item);
  }

  boolean excludesEquipment(AdventureResult item) {
    return this.equipment.excluded.contains(item);
  }

  boolean booleansSatisfied(Modifiers mods) {
    return this.booleanMask.isEmpty()
        || mods.getBooleans(this.booleanMask).equals(this.booleanValue);
  }

  Evaluator.Constraint checkConstraints(Modifiers mods) {
    if (mods == null) return Evaluator.Constraint.IRRELEVANT;
    EnumSet<BooleanModifier> bools = mods.getBooleans(this.booleanMask);
    if (!this.booleanValue.containsAll(bools)) return Evaluator.Constraint.VIOLATES;
    if (!bools.isEmpty()) return Evaluator.Constraint.MEETS;
    return Evaluator.Constraint.IRRELEVANT;
  }

  /**
   * Whether required items are present, enabled slots omit forbidden items, and outfit terms hold.
   */
  boolean equipmentSatisfied(Modifiers mods, Map<Slot, AdventureResult> equipment) {
    if (!this.equipment.required.stream().allMatch(item -> hasEquipped(equipment, equipment, item)))
      return false;
    if (!this.equipment.excluded.isEmpty()) {
      var enabledEquipment = new EnumMap<Slot, AdventureResult>(Slot.class);
      equipment.forEach(
          (slot, item) -> {
            if (this.slotEnabled(slot)) enabledEquipment.put(slot, item);
          });
      if (this.equipment.excluded.stream()
          .anyMatch(item -> hasEquipped(enabledEquipment, equipment, item))) return false;
    }
    String outfit = mods.getString(StringModifier.OUTFIT);
    return !this.outfits.excluded.contains(outfit)
        && (this.outfits.required.isEmpty() || this.outfits.required.contains(outfit));
  }

  private static boolean hasEquipped(
      Map<Slot, AdventureResult> enabledEquipment,
      Map<Slot, AdventureResult> allEquipment,
      AdventureResult item) {
    if (KoLCharacter.hasEquipped(enabledEquipment, item)) {
      return true;
    }

    var codpiece = ItemSlotGroup.ETERNITY_CODPIECE;
    boolean wearingCodpiece =
        SlotSet.ACCESSORY_SLOTS.stream()
            .map(allEquipment::get)
            .anyMatch(parent -> parent != null && codpiece.isParent(parent.getItemId()));
    return wearingCodpiece
        && codpiece.accepts(item.getItemId())
        && codpiece.slots().stream().anyMatch(slot -> item.equals(enabledEquipment.get(slot)));
  }

  /** The bonus that "bonus", "letter" and "number" terms award the given equipment. */
  double equipmentBonus(Iterable<AdventureResult> equipment, Map<Modeable, String> modeables) {
    if (this.bonuses.isEmpty() && this.bonusFunc.isEmpty()) {
      return 0.0;
    }
    double score = 0.0;
    for (AdventureResult item : equipment) {
      score += this.itemBonus(item, modeables);
    }
    return score;
  }

  private double itemBonus(AdventureResult item, Map<Modeable, String> modeables) {
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
      score += func.bonusFunction().apply(item) * func.weight();
    }
    return score;
  }

  // Registration

  static String normalize(String keyword) {
    return REWRITES.stream()
        .filter(rewrite -> rewrite.matches().test(keyword))
        .findFirst()
        .map(rewrite -> rewrite.rewrite().apply(keyword))
        .orElse(keyword);
  }

  private static Action directiveFor(String keyword) {
    Action exact = EXACT_DIRECTIVES.get(keyword);
    if (exact != null) {
      return exact;
    }
    for (var entry : PREFIX_DIRECTIVES) {
      if (keyword.startsWith(entry.getKey())) {
        return entry.getValue();
      }
    }
    return null;
  }

  private static Action termFor(String keyword) {
    Action exact = EXACT_TERMS.get(keyword);
    if (exact != null) {
      return exact;
    }
    return ALIAS_TERMS.stream()
        .filter(entry -> entry.matches().test(keyword))
        .findFirst()
        .map(Alias::action)
        .orElse(null);
  }

  private static void directive(String keyword, Action action) {
    EXACT_DIRECTIVES.put(keyword, action);
  }

  private static void prefixDirective(String prefix, Action action) {
    PREFIX_DIRECTIVES.add(Map.entry(prefix, action));
  }

  private static void term(String keyword, Action action) {
    EXACT_TERMS.put(keyword, action);
  }

  private static void prefixTerm(String prefix, Action action) {
    ALIAS_TERMS.add(new Alias(keyword -> keyword.startsWith(prefix), action));
  }

  private static void pattern(String regex, DoubleModifier modifier) {
    Pattern pattern = Pattern.compile(regex);
    ALIAS_TERMS.add(new Alias(keyword -> pattern.matcher(keyword).find(), modifier(modifier)));
  }

  /** Registers a keyword that means exactly what an already registered keyword means. */
  private static void alias(String alias, String canonical) {
    Action directive = EXACT_DIRECTIVES.get(canonical);
    if (directive != null) {
      directive(alias, directive);
      return;
    }
    Action term = EXACT_TERMS.get(canonical);
    if (term == null) {
      throw new IllegalArgumentException("Unknown canonical maximizer term: " + canonical);
    }
    term(alias, term);
  }

  private static Action modifier(DoubleModifier modifier) {
    return (terms, term) -> terms.scoreModifier(modifier, term.weight());
  }

  private static Action integerSetting(IntegerSetting setting) {
    return integerSetting(setting, term -> (int) term.weight());
  }

  private static Action integerSetting(IntegerSetting setting, ToIntFunction<ParsedTerm> value) {
    return (terms, term) -> terms.integers.put(setting, value.applyAsInt(term));
  }

  private static Action weaponRequirement(WeaponRequirement requirement) {
    return flag(
        (terms, required) -> {
          if (required) terms.weaponRequirements.add(requirement);
          else terms.weaponRequirements.remove(requirement);
        });
  }

  private static Action flag(BiConsumer<MaximizerTermRegistry, Boolean> setter) {
    return flag(term -> term.weight() > 0.0, setter);
  }

  private static Action flag(
      Predicate<ParsedTerm> value, BiConsumer<MaximizerTermRegistry, Boolean> setter) {
    return (terms, term) -> setter.accept(terms, value.test(term));
  }

  private static Action text(int prefixLength, BiConsumer<MaximizerTermRegistry, String> setter) {
    return (terms, term) -> setter.accept(terms, term.keyword().substring(prefixLength).trim());
  }

  private static Action aggregate(DoubleModifier... modifiers) {
    return aggregate(1.0, modifiers);
  }

  private static Action aggregate(double factor, DoubleModifier... modifiers) {
    List<DoubleModifier> contributions = Arrays.stream(modifiers).toList();
    return (terms, term) -> terms.scoreAggregate(contributions, factor, term.weight());
  }

  private static Action dropTiebreaker() {
    return (terms, term) -> terms.noTiebreaker = true;
  }

  private static Action ignoreBees() {
    return (terms, term) -> terms.integers.put(IntegerSetting.BEEOSITY, 999);
  }

  private static Action matchUnderwaterCombatRate() {
    return (terms, term) -> {
      if (AdventureDatabase.isUnderwater(Modifiers.currentLocation)) {
        terms.weight.set(DoubleModifier.UNDERWATER_COMBAT_RATE, term.weight());
      }
    };
  }

  private static void suffixRewrite(String suffix, String replacement) {
    REWRITES.add(
        new Rewrite(
            keyword -> keyword.endsWith(suffix),
            keyword -> keyword.substring(0, keyword.length() - suffix.length()) + replacement));
  }

  private static void prefixRewrite(String prefix, UnaryOperator<String> rewrite) {
    REWRITES.add(new Rewrite(keyword -> keyword.startsWith(prefix), rewrite));
  }

  static double letterBonus(AdventureResult item) {
    return item == null || item.getItemId() < 0 ? 0 : item.getDataName().length();
  }

  static double letterBonus(AdventureResult item, String letters) {
    return item == null || item.getItemId() < 0
        ? 0
        : Pattern.compile(letters, Pattern.CASE_INSENSITIVE)
            .matcher(item.getDataName())
            .results()
            .count();
  }

  static double numberBonus(AdventureResult item) {
    return item == null || item.getItemId() < 0
        ? 0
        : NUMBER_PATTERN.matcher(item.getDataName()).results().count();
  }
}
