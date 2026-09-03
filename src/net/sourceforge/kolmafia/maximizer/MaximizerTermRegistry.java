package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifierCollection;

final class MaximizerTermRegistry {
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

  private static final Map<String, Supplier<Definition>> EXACT = new LinkedHashMap<>();
  private static final List<Entry> ALIASES = new ArrayList<>();

  static {
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

  private static void exact(String keyword, Definition definition) {
    exact(keyword, () -> definition);
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
