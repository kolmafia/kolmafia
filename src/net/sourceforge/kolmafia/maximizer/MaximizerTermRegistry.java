package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifierCollection;

final class MaximizerTermRegistry {
  record ParsedTerm(String keyword, double weight, boolean explicitWeight) {}

  @FunctionalInterface
  private interface Directive {
    void apply(Evaluator.ParseState state, ParsedTerm term);
  }

  record Contribution(DoubleModifier modifier, double factor) {}

  record Definition(
      List<Contribution> contributions,
      DoubleModifier primaryModifier,
      boolean disablesTiebreaker,
      boolean disablesBeeosity,
      boolean includesUnderwaterCombatRate) {
    void apply(DoubleModifierCollection weights, double weight) {
      this.contributions.forEach(
          contribution -> weights.set(contribution.modifier(), weight * contribution.factor()));
    }
  }

  private record Entry(Predicate<String> matches, Supplier<Definition> definition) {}

  private record Rewrite(Predicate<String> matches, UnaryOperator<String> rewrite) {}

  private static final Map<String, Supplier<Definition>> EXACT = new LinkedHashMap<>();
  private static final List<Entry> ALIASES = new ArrayList<>();
  private static final Map<String, Directive> EXACT_DIRECTIVES = new LinkedHashMap<>();
  private static final List<Map.Entry<String, Directive>> PREFIX_DIRECTIVES = new ArrayList<>();
  private static final List<Rewrite> REWRITES = new ArrayList<>();

  static {
    suffixRewrite(" res", " resistance");
    suffixRewrite(" dmg percent", " damage percent");
    suffixRewrite(" dmg", " damage");
    suffixRewrite(" exp", " experience");
    prefixRewrite("organ", ignored -> "organ capacity");

    directive("min", Evaluator.ParseState::setMinimum);
    directive("max", Evaluator.ParseState::setMaximum);
    directive("dump", Evaluator.ParseState::setDump);
    prefixDirective("hand", Evaluator.ParseState::setHands);
    prefixDirective("tie", Evaluator.ParseState::setTiebreaker);
    prefixDirective("current", Evaluator.ParseState::setCurrent);
    prefixDirective("type ", Evaluator.ParseState::setWeaponType);
    directive("club", Evaluator.ParseState::requireClub);
    directive("shield", Evaluator.ParseState::requireShield);
    directive("utensil", Evaluator.ParseState::requireUtensil);
    directive("sword", Evaluator.ParseState::requireSword);
    directive("knife", Evaluator.ParseState::requireKnife);
    directive("accordion", Evaluator.ParseState::requireAccordion);
    directive("melee", Evaluator.ParseState::setMelee);
    directive("effective", Evaluator.ParseState::setEffective);
    directive("empty", Evaluator.ParseState::setEmpty);
    directive("clownosity", Evaluator.ParseState::setClownosity);
    directive("raveosity", Evaluator.ParseState::setRaveosity);
    directive("surgeonosity", Evaluator.ParseState::setSurgeonosity);
    directive("beeosity", Evaluator.ParseState::setBeeosity);
    directive("stinkycheese", Evaluator.ParseState::setStinkycheese);
    directiveAlias("stinky cheese", "stinkycheese");
    directive("sea", Evaluator.ParseState::setSea);
    prefixDirective("equip ", Evaluator.ParseState::setEquipment);
    prefixDirective("bonus ", Evaluator.ParseState::setBonus);
    prefixDirective("letter", Evaluator.ParseState::setLetterBonus);
    directive("number", Evaluator.ParseState::setNumberBonus);
    directive("plumber", Evaluator.ParseState::setPlumber);
    directive("cold plumber", Evaluator.ParseState::setColdPlumber);
    prefixDirective("outfit", Evaluator.ParseState::setOutfit);
    prefixDirective("switch ", Evaluator.ParseState::setFamiliar);

    exact(
        "all resistance",
        aggregate(
            DoubleModifier.COLD_RESISTANCE,
            DoubleModifier.HOT_RESISTANCE,
            DoubleModifier.SLEAZE_RESISTANCE,
            DoubleModifier.SPOOKY_RESISTANCE,
            DoubleModifier.STENCH_RESISTANCE));
    exact(
        "elemental damage",
        aggregate(
            DoubleModifier.COLD_DAMAGE,
            DoubleModifier.HOT_DAMAGE,
            DoubleModifier.SLEAZE_DAMAGE,
            DoubleModifier.SPOOKY_DAMAGE,
            DoubleModifier.STENCH_DAMAGE));
    exact("hp regen", aggregate(0.5, DoubleModifier.HP_REGEN_MIN, DoubleModifier.HP_REGEN_MAX));
    exact("mp regen", aggregate(0.5, DoubleModifier.MP_REGEN_MIN, DoubleModifier.MP_REGEN_MAX));
    exact("passive damage", aggregate(DoubleModifier.DAMAGE_AURA, DoubleModifier.THORNS));
    exact(
        "organ capacity",
        aggregate(
            DoubleModifier.STOMACH_CAPACITY,
            DoubleModifier.LIVER_CAPACITY,
            DoubleModifier.SPLEEN_CAPACITY));

    exact("init", modifier(DoubleModifier.INITIATIVE));
    exact("hp", modifier(DoubleModifier.HP));
    exact("mp", modifier(DoubleModifier.MP));
    exact("da", modifier(DoubleModifier.DAMAGE_ABSORPTION));
    exact("dr", modifier(DoubleModifier.DAMAGE_REDUCTION));
    exact("ml", modifier(DoubleModifier.MONSTER_LEVEL));
    exact("ocrs", definition(DoubleModifier.RANDOM_MONSTER_MODIFIERS, true, true, false));

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

    prefix("mus", modifier(DoubleModifier.MUS));
    prefix("mys", modifier(DoubleModifier.MYS));
    prefix("mox", modifier(DoubleModifier.MOX));
    prefix("main", () -> modifier(DoubleModifier.primeStat()));
    prefix("com", definition(DoubleModifier.COMBAT_RATE, false, false, true));
    prefix("item", modifier(DoubleModifier.ITEMDROP));
    prefix("meat", modifier(DoubleModifier.MEATDROP));
    prefix("adv", definition(DoubleModifier.ADVENTURES, false, true, false));
    prefix("fites", definition(DoubleModifier.PVP_FIGHTS, false, true, false));
    prefix("exp", modifier(DoubleModifier.EXPERIENCE));
    prefix("crit", modifier(DoubleModifier.CRITICAL_PCT));
    prefix("spell crit", modifier(DoubleModifier.SPELL_CRITICAL_PCT));
    prefix("sprinkle", modifier(DoubleModifier.SPRINKLES));
    prefix("stomach", modifier(DoubleModifier.STOMACH_CAPACITY));
    prefix("liver", modifier(DoubleModifier.LIVER_CAPACITY));
    prefix("spleen", modifier(DoubleModifier.SPLEEN_CAPACITY));
  }

  private MaximizerTermRegistry() {}

  static boolean applyDirective(Evaluator.ParseState state, ParsedTerm term) {
    Directive exact = EXACT_DIRECTIVES.get(term.keyword());
    if (exact != null) {
      exact.apply(state, term);
      return true;
    }
    for (var entry : PREFIX_DIRECTIVES) {
      if (term.keyword().startsWith(entry.getKey())) {
        entry.getValue().apply(state, term);
        return true;
      }
    }
    return false;
  }

  static Definition find(String keyword) {
    Supplier<Definition> exact = EXACT.get(keyword);
    if (exact != null) {
      return exact.get();
    }
    return ALIASES.stream()
        .filter(entry -> entry.matches().test(keyword))
        .findFirst()
        .map(entry -> entry.definition().get())
        .orElse(null);
  }

  static String normalize(String keyword) {
    return REWRITES.stream()
        .filter(rewrite -> rewrite.matches().test(keyword))
        .findFirst()
        .map(rewrite -> rewrite.rewrite().apply(keyword))
        .orElse(keyword);
  }

  private static void exact(String keyword, Definition definition) {
    exact(keyword, () -> definition);
  }

  private static void directive(String keyword, Directive directive) {
    EXACT_DIRECTIVES.put(keyword, directive);
  }

  private static void directiveAlias(String alias, String canonical) {
    Directive directive = EXACT_DIRECTIVES.get(canonical);
    if (directive == null) {
      throw new IllegalArgumentException("Unknown canonical maximizer term: " + canonical);
    }
    directive(alias, directive);
  }

  private static void prefixDirective(String prefix, Directive directive) {
    PREFIX_DIRECTIVES.add(Map.entry(prefix, directive));
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

  private static void exact(String keyword, Supplier<Definition> definition) {
    EXACT.put(keyword, definition);
  }

  private static void prefix(String prefix, Definition definition) {
    prefix(prefix, () -> definition);
  }

  private static void prefix(String prefix, Supplier<Definition> definition) {
    ALIASES.add(new Entry(keyword -> keyword.startsWith(prefix), definition));
  }

  private static void pattern(String regex, DoubleModifier modifier) {
    Pattern pattern = Pattern.compile(regex);
    ALIASES.add(new Entry(keyword -> pattern.matcher(keyword).find(), () -> modifier(modifier)));
  }

  private static Definition modifier(DoubleModifier modifier) {
    return definition(modifier, false, false, false);
  }

  private static Definition definition(
      DoubleModifier modifier,
      boolean disablesTiebreaker,
      boolean disablesBeeosity,
      boolean includesUnderwaterCombatRate) {
    return new Definition(
        List.of(new Contribution(modifier, 1.0)),
        modifier,
        disablesTiebreaker,
        disablesBeeosity,
        includesUnderwaterCombatRate);
  }

  private static Definition aggregate(DoubleModifier... modifiers) {
    return aggregate(1.0, modifiers);
  }

  private static Definition aggregate(double factor, DoubleModifier... modifiers) {
    return new Definition(
        java.util.Arrays.stream(modifiers)
            .map(modifier -> new Contribution(modifier, factor))
            .toList(),
        null,
        false,
        false,
        false);
  }
}
