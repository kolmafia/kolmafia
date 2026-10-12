package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Player.withAdjustmentsRecalculated;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withOutfit;
import static internal.helpers.Player.withPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class MaximizerExpressionTest {
  @Test
  void defaultsWeightToOne() {
    var expression = new MaximizerExpression();

    expression.parse("muscle");

    assertThat(expression.weight, hasEntry(DoubleModifier.MUS, 1.0));
  }

  @Test
  void parsesDecimalWeight() {
    var expression = new MaximizerExpression();

    expression.parse("2.5 muscle");

    assertThat(expression.weight, hasEntry(DoubleModifier.MUS, 2.5));
  }

  @Test
  void parsesNegativeWeight() {
    var expression = new MaximizerExpression();

    expression.parse("-2 muscle");

    assertThat(expression.weight, hasEntry(DoubleModifier.MUS, -2.0));
  }

  @Test
  void parsesExplicitPositiveWeight() {
    var expression = new MaximizerExpression();

    expression.parse("+3 muscle");

    assertThat(expression.weight, hasEntry(DoubleModifier.MUS, 3.0));
  }

  @Test
  void stripsQuotesFromKeyword() {
    var expression = new MaximizerExpression();

    expression.parse("\"muscle\"");

    assertThat(expression.weight, hasEntry(DoubleModifier.MUS, 1.0));
  }

  @Test
  void canonicalizesKeyword() {
    var expression = new MaximizerExpression();

    expression.parse("mus");

    assertThat(expression.weight, hasEntry(DoubleModifier.MUS, 1.0));
  }

  @Test
  void separatesCommaDelimitedTerms() {
    var expression = new MaximizerExpression();

    expression.parse("2 muscle, 3 mysticality");

    assertThat(expression.weight, hasEntry(DoubleModifier.MUS, 2.0));
    assertThat(expression.weight, hasEntry(DoubleModifier.MYS, 3.0));
  }

  @Test
  void separatesImplicitlyDelimitedTerms() {
    var expression = new MaximizerExpression();

    expression.parse("2 muscle 3 mysticality");

    assertThat(expression.weight, hasEntry(DoubleModifier.MUS, 2.0));
    assertThat(expression.weight, hasEntry(DoubleModifier.MYS, 3.0));
  }

  @Test
  void preservesDirectiveOperandsDuringCanonicalization() {
    var expression = new MaximizerExpression();

    expression.parse("type mus");

    assertThat(expression.weaponType, is("mus"));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "elemental damage | Cold Damage",
        "any resistance | Cold Resistance",
        "ele resistance | Cold Resistance",
        "elemental resistance | Cold Resistance",
        "organ capacity | Stomach Capacity",
        "crit | Critical Hit Percent",
        "spell crit | Spell Critical Percent",
        "sprinkle | Sprinkle Drop",
        "stomach | Stomach Capacity",
        "liver | Liver Capacity",
        "spleen | Spleen Capacity",
        "ocrs | Random Monster Modifiers",
        "weapon dmg percent | Weapon Damage Percent",
        "organ | Stomach Capacity",
        "mys exp perc | Mysticality Experience Percent",
        "myst exp | Mysticality Experience",
        "mystical perc | Mysticality Percent",
        "mys exp | Mysticality Experience",
        "mys perc | Mysticality Percent",
        "mox exp perc | Moxie Experience Percent",
        "mox exp | Moxie Experience",
        "mox perc | Moxie Percent",
        "critical | Critical Hit Percent",
        "spell critical | Spell Critical Percent",
        "\"item drop\" | Item Drop"
      })
  void recognizesModifierAliases(String alias, String modifierName) {
    var expression = new MaximizerExpression();

    expression.parse(alias);

    assertThat(expression.weight, hasEntry(DoubleModifier.byCaselessName(modifierName), 1.0));
  }

  @Test
  void recognizesEquipmentSlots() {
    var expression = new MaximizerExpression();

    expression.parse("2 hat");

    assertThat(expression.slots, hasEntry(Slot.HAT, 2));
  }

  @Test
  void recognizesBooleanModifiers() {
    var expression = new MaximizerExpression();

    expression.parse("softcore only");

    assertThat(expression.booleanMask.contains(BooleanModifier.SOFTCORE), is(true));
    assertThat(expression.booleanValue.contains(BooleanModifier.SOFTCORE), is(true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"2 hand", "2 handed", "2 hands"})
  void recognizesHandDirectiveAliases(String directive) {
    var expression = new MaximizerExpression();

    expression.parse(directive);

    assertThat(expression.hands, is(2));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-tie", "-tiebreaker"})
  void recognizesTiebreakerDirectiveAliases(String directive) {
    var expression = new MaximizerExpression();

    expression.parse(directive);

    assertThat(expression.noTiebreaker, is(true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"stinkycheese", "stinky cheese"})
  void recognizesStinkyCheeseDirective(String directive) {
    var expression = new MaximizerExpression();

    expression.parse(directive);

    assertThat(expression.stinkycheese, is(1));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "nonsense | Unrecognized keyword: nonsense",
        "nonsense 3 min | Unrecognized keyword: nonsense",
        "handicap | Unrecognized keyword: handicap",
        "tier | Unrecognized keyword: tier",
        "currently | Unrecognized keyword: currently",
        "letterhead | Unrecognized keyword: letterhead",
        "outfitter | Unrecognized keyword: outfitter",
        "mainline | Unrecognized keyword: mainline",
        "comedy | Unrecognized keyword: comedy",
        "advice | Unrecognized keyword: advice",
        "fiteswhatever | Unrecognized keyword: fiteswhatever",
        "organic | Unrecognized keyword: organic",
        "muscular | Unrecognized keyword: muscular",
        "mysterious | Unrecognized keyword: mysterious",
        "moxious | Unrecognized keyword: moxious",
        "itemized | Unrecognized keyword: itemized",
        "meatball | Unrecognized keyword: meatball",
        "expensive | Unrecognized keyword: expensive",
        "criticality | Unrecognized keyword: criticality",
        "sprinkler | Unrecognized keyword: sprinkler",
        "stomachache | Unrecognized keyword: stomachache",
        "liverish | Unrecognized keyword: liverish",
        "spleenful | Unrecognized keyword: spleenful",
        "cold residue | Unrecognized keyword: cold residue",
        "cold res foo | Unrecognized keyword: cold res foo",
        "weapon dmgx | Unrecognized keyword: weapon dmgx",
        "hand foo | Unrecognized keyword: hand foo",
        "hands foo | Unrecognized keyword: hands foo",
        "tie foo | Unrecognized keyword: tie foo",
        "tiebreaker foo | Unrecognized keyword: tiebreaker foo",
        "current foo | Unrecognized keyword: current foo",
        "item,, | Unable to interpret: ,"
      })
  void reportsInvalidSyntax(String syntax, String error) {
    var expression = new MaximizerExpression();

    expression.parse(syntax);

    assertThat(KoLmafia.lastMessage, is(error));
  }

  @ParameterizedTest
  @ValueSource(strings = {"type", "equip", "bonus", "modbonus", "switch"})
  void reportsMissingDirectiveOperands(String directive) {
    var expression = new MaximizerExpression();

    expression.parse(directive);

    assertThat(KoLmafia.lastMessage, is("Directive '" + directive + "' requires an operand"));
  }

  @Test
  void bindsConsecutiveLimitsToPreviousModifier() {
    var expression = new MaximizerExpression();

    expression.parse("muscle, 2 min, 3 max");

    assertThat(expression.min, hasEntry(DoubleModifier.MUS, 2.0));
    assertThat(expression.max, hasEntry(DoubleModifier.MUS, 3.0));
  }

  @ParameterizedTest
  @CsvSource({
    "clowniness, CLOWNINESS, 100.0, 100.0",
    "raveosity, RAVEOSITY, 7.0, 7.0",
    "surgeonosity, SURGEONOSITY, 1.0, 5.0"
  })
  void appliesDefaultOsityLimits(
      String keyword, BitmapModifier modifier, double minimum, double maximum) {
    var expression = new MaximizerExpression();

    expression.parse(keyword);

    assertThat(expression.min, hasEntry(modifier, minimum));
    assertThat(expression.max, hasEntry(modifier, maximum));
  }

  @Test
  void explicitLimitOverridesDefaultOsityMinimum() {
    var expression = new MaximizerExpression();

    expression.parse("clownosity, 4 min");

    assertThat(expression.min, hasEntry(BitmapModifier.CLOWNINESS, 4.0));
    assertThat(expression.max, hasEntry(BitmapModifier.CLOWNINESS, 100.0));
  }

  @Test
  void appliesLimitsAtStartToTotalScore() {
    var expression = new MaximizerExpression();

    expression.parse("5 min, 7 max, item");

    assertThat(expression.totalMin, is(5.0));
    assertThat(expression.totalMax, is(7.0));
    assertThat(expression.weight, hasEntry(DoubleModifier.ITEMDROP, 1.0));
  }

  @Test
  void parsesModifierLimitsWithoutCommas() {
    var expression = new MaximizerExpression();

    expression.parse("1 DR 20 min 30 max");

    assertThat(expression.weight, hasEntry(DoubleModifier.DAMAGE_REDUCTION, 1.0));
    assertThat(expression.min, hasEntry(DoubleModifier.DAMAGE_REDUCTION, 20.0));
    assertThat(expression.max, hasEntry(DoubleModifier.DAMAGE_REDUCTION, 30.0));
  }

  @Test
  void rejectsInvalidLimitBeforeApplyingEarlierTerms() {
    var expression = new MaximizerExpression();

    expression.parse("muscle, hat, 3 min");

    assertThat(expression.weight.isEmpty(), is(true));
  }

  @ParameterizedTest
  @CsvSource({
    "max, max must follow a modifier or appear at the start of the expression; preceding term was 'hat'",
    "min, min must follow a modifier or appear at the start of the expression; preceding term was 'hat'"
  })
  void rejectsLimitsAfterNonModifierTerms(String limit, String error) {
    var expression = new MaximizerExpression();

    expression.parse("2 da, hat, 3 " + limit + ", -tie");

    assertThat(KoLmafia.lastMessage, is(error));
  }

  @Test
  void seaDoesNotProvideModifierForFollowingLimit() {
    var expression = new MaximizerExpression();

    expression.parse("muscle, sea, 3 min");

    assertThat(
        KoLmafia.lastMessage,
        is(
            "min must follow a modifier or appear at the start of the expression; preceding term was 'sea'"));
    assertThat(expression.min, hasEntry(DoubleModifier.MUS, Double.NEGATIVE_INFINITY));
    assertThat(expression.booleanMask.isEmpty(), is(true));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "outfit not an outfit | Unknown or custom outfit: not an outfit",
        "outfit stinky cheese | Unknown or custom outfit: stinky cheese",
        "switch not a familiar | Unknown familiar: not a familiar"
      })
  void reportsInvalidDirectiveOperands(String expressionText, String error) {
    var expression = new MaximizerExpression();

    expression.parse(expressionText);

    assertThat(KoLmafia.lastMessage, is(error));
  }

  @Test
  void outfitWithoutNameUsesCurrentOutfit() {
    try (var cleanups =
        new Cleanups(withOutfit(OutfitPool.WAR_FRAT_OUTFIT), withAdjustmentsRecalculated())) {
      String currentOutfit = KoLCharacter.currentStringModifier(StringModifier.OUTFIT);
      var expression = new MaximizerExpression();

      expression.parse("outfit");

      assertThat(expression.posOutfits.contains(currentOutfit), is(true));
    }
  }

  @Test
  void positiveFamiliarSwitchTakesPriorityOverNegativeSwitch() {
    try (var cleanups =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
            withFamiliarInTerrarium(FamiliarPool.TRICK_TOT))) {
      var expression = new MaximizerExpression();

      expression.parse("switch Left-Hand Man, -switch Trick-or-Treating Tot");

      assertThat(expression.familiars.size(), is(1));
      assertThat(expression.familiars.getFirst().getId(), is(FamiliarPool.LEFT_HAND));
    }
  }

  @Test
  void negativeFamiliarSwitchIsUsedWhenPositiveSwitchIsUnavailable() {
    try (var cleanups = withFamiliarInTerrarium(FamiliarPool.TRICK_TOT)) {
      var expression = new MaximizerExpression();

      expression.parse("switch Left-Hand Man, -switch Trick-or-Treating Tot");

      assertThat(expression.familiars.size(), is(1));
      assertThat(expression.familiars.getFirst().getId(), is(FamiliarPool.TRICK_TOT));
    }
  }

  @Test
  void weightedPositiveSwitchCannotUseUnownedFamiliar() {
    var expression = new MaximizerExpression();

    expression.parse("2 switch Baby Gravy Fairy");

    assertThat(expression.familiars.isEmpty(), is(true));
  }

  @Test
  void appliesSimpleDirectives() {
    var expression = new MaximizerExpression();

    expression.parse(
        "2 dump, tie, -tie, current, -current, -club, club, -shield, shield, -utensil, utensil, -sword, sword, -knife, knife, -accordion, accordion, melee, -2 melee, -effective, effective");

    assertThat(expression.dump, is(2));
    assertThat(expression.noTiebreaker, is(true));
    assertThat(expression.current, is(false));
    assertThat(expression.requireClub, is(true));
    assertThat(expression.requireShield, is(true));
    assertThat(expression.requireUtensil, is(true));
    assertThat(expression.requireSword, is(true));
    assertThat(expression.requireKnife, is(true));
    assertThat(expression.requireAccordion, is(true));
    assertThat(expression.melee, is(-4));
    assertThat(expression.effective, is(true));
  }

  @Test
  void reportsInvalidBooleanModifierBonus() {
    var expression = new MaximizerExpression();

    expression.parse("modbonus not a modifier");

    assertThat(KoLmafia.lastMessage, is("No boolean modifier found for: not a modifier"));
  }

  @Test
  void plainItemBonusPreservesModeBonuses() {
    var expression = new MaximizerExpression();

    expression.parse("10 bonus backup camera (meat), 5 bonus backup camera");

    var bonus = expression.bonuses.get(ItemPool.get("backup camera"));
    assertThat(bonus.base(), is(5.0));
    assertThat(bonus.modes(), hasEntry("meat", 10.0));
  }

  @ParameterizedTest
  @CsvSource({
    "PASTAMANCER, heavy hammer",
    "PASTAMANCER, hammer",
    "PASTAMANCER, fire flower",
    "SEAL_CLUBBER, bonfire flower",
    "SEAL_CLUBBER, fancy boots",
    "SEAL_CLUBBER, heavy hammer",
    "SEAL_CLUBBER, hammer",
    "SEAL_CLUBBER, fire flower",
    "DISCO_BANDIT, work boots"
  })
  void plumberFallsBackToAvailableTool(AscensionClass ascensionClass, String itemName) {
    try (var cleanups =
        new Cleanups(
            withClass(ascensionClass),
            withPath(Path.PATH_OF_THE_PLUMBER),
            withEquippableItem(itemName))) {
      var expression = new MaximizerExpression();

      expression.parse("plumber");

      assertThat(expression.posEquip.contains(ItemPool.get(itemName)), is(true));
    }
  }

  @ParameterizedTest
  @EnumSource(
      value = AscensionClass.class,
      names = {"SEAL_CLUBBER", "PASTAMANCER", "DISCO_BANDIT"})
  void plumberFallsBackToWorkBootsWhenNoToolsAreAvailable(AscensionClass ascensionClass) {
    try (var cleanups =
        new Cleanups(withClass(ascensionClass), withPath(Path.PATH_OF_THE_PLUMBER))) {
      var expression = new MaximizerExpression();

      expression.parse("plumber");

      assertThat(expression.posEquip.contains(ItemPool.get(ItemPool.WORK_BOOTS)), is(true));
    }
  }
}
