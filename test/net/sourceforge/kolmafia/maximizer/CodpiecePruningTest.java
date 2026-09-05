package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withOverrideModifiers;
import static internal.helpers.Player.withPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CodpiecePruningTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("CodpiecePruningTest");
  }

  private static double estimate(
      Evaluator evaluator,
      Modifiers baseline,
      Modifiers[] gemModifiers,
      int[] remaining,
      int slotCount) {
    var upperBound =
        evaluator.createTheoreticalCodpieceScoreUpperBound(
            baseline, gemModifiers, remaining, slotCount);
    return upperBound == null
        ? Double.POSITIVE_INFINITY
        : upperBound.estimate(0, remaining, slotCount);
  }

  @Test
  void includesUnselectedItemScoresInBound() {
    var gemModifiers = new Modifiers[] {new Modifiers(), new Modifiers()};
    int[] remaining = {1, 1};
    var upperBound =
        new CodpieceScoreBound(
            List.of(),
            new CodpieceScoreBound.ContributionBasis(List.of(), gemModifiers),
            new Modifiers(),
            gemModifiers,
            remaining,
            1,
            Double.NEGATIVE_INFINITY,
            0.0,
            true,
            false,
            new double[] {0.0, 100.0},
            null,
            true);

    assertThat(upperBound.estimate(0, remaining, 1), equalTo(100.0));
  }

  @Test
  void boundsMultipleIndependentModifiers() {
    var evaluator = new Evaluator("damage reduction 10 max, 2 pool skill, -tie");
    var baseline = new Modifiers();
    baseline.setDouble(DoubleModifier.DAMAGE_REDUCTION, 1.0);
    baseline.setDouble(DoubleModifier.POOL_SKILL, 1.0);
    var damageReductionGem = new Modifiers();
    damageReductionGem.setDouble(DoubleModifier.DAMAGE_REDUCTION, 5.0);
    var poolSkillGem = new Modifiers();
    poolSkillGem.setDouble(DoubleModifier.POOL_SKILL, 1.0);

    assertThat(
        estimate(
            evaluator,
            baseline,
            new Modifiers[] {damageReductionGem, poolSkillGem},
            new int[] {2, 2},
            2),
        equalTo(16.0));
    int[] remaining = {2, 2};
    var upperBound =
        evaluator.createTheoreticalCodpieceScoreUpperBound(
            baseline, new Modifiers[] {damageReductionGem, poolSkillGem}, remaining, 2);
    upperBound.select(0);
    remaining[0]--;
    assertThat(upperBound.estimate(0, remaining, 1), equalTo(14.0));
    remaining[0]++;
    upperBound.deselect(0);
    upperBound.select(1);
    remaining[1]--;
    assertThat(upperBound.estimate(1, remaining, 1), equalTo(7.0));

    assertThat(
        estimate(
            evaluator,
            baseline,
            new Modifiers[] {damageReductionGem, poolSkillGem},
            new int[] {1, 2},
            2),
        equalTo(12.0));
    var negativePoolSkillGem = new Modifiers();
    negativePoolSkillGem.setDouble(DoubleModifier.POOL_SKILL, -2.0);
    assertThat(
        estimate(
            new Evaluator("-pool skill, -tie"),
            baseline,
            new Modifiers[] {negativePoolSkillGem, poolSkillGem},
            new int[] {1, 2},
            2),
        equalTo(1.0));
  }

  @ParameterizedTest
  @CsvSource({
    "smithsness, SMITHSNESS",
    "critical hit percent, CRITICAL_PCT",
    "stomach capacity, STOMACH_CAPACITY",
    "familiar weight cap, FAMILIAR_WEIGHT_CAP"
  })
  void boundsDirectNumericModifiers(String expression, DoubleModifier modifier) {
    var baseline = new Modifiers();
    baseline.setDouble(modifier, 5.0);

    assertThat(
        estimate(
            new Evaluator(expression + ", -tie"),
            baseline,
            new Modifiers[] {new Modifiers()},
            new int[] {1},
            1),
        equalTo(5.0));
  }

  @Test
  void fallsBackForUnmodeledDirectModifierGem() {
    var gem = new Modifiers();
    gem.setDouble(DoubleModifier.SMITHSNESS, 1.0);

    assertThat(
        new Evaluator("smithsness")
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(), new Modifiers[] {gem}, new int[] {1}, 1),
        nullValue());
  }

  @Test
  void boundsMinimumsAndSaturation() {
    var baseline = new Modifiers();
    baseline.setDouble(DoubleModifier.DAMAGE_REDUCTION, 1.0);
    baseline.setDouble(DoubleModifier.POOL_SKILL, 1.0);
    var damageReductionGem = new Modifiers();
    damageReductionGem.setDouble(DoubleModifier.DAMAGE_REDUCTION, 5.0);
    var poolSkillGem = new Modifiers();
    poolSkillGem.setDouble(DoubleModifier.POOL_SKILL, 1.0);
    var gemModifiers = new Modifiers[] {damageReductionGem, poolSkillGem};

    int[] remaining = {2, 2};
    var upperBound =
        new Evaluator("damage reduction 6 min, 2 pool skill, -tie")
            .createTheoreticalCodpieceScoreUpperBound(baseline, gemModifiers, remaining, 2);
    assertTrue(upperBound.canMeetMinimum(0, remaining, 2, upperBound.estimate(0, remaining, 2)));
    upperBound.select(1);
    remaining[1]--;
    assertFalse(upperBound.canMeetMinimum(1, remaining, 1, upperBound.estimate(1, remaining, 1)));

    remaining = new int[] {2, 2};
    upperBound =
        new Evaluator("pool skill, 6 damage reduction min, -tie")
            .createTheoreticalCodpieceScoreUpperBound(baseline, gemModifiers, remaining, 2);
    assertTrue(upperBound.canMeetMinimum(0, remaining, 2, upperBound.estimate(0, remaining, 2)));

    remaining = new int[] {2, 2};
    upperBound =
        new Evaluator("18 min, damage reduction, 2 pool skill, -tie")
            .createTheoreticalCodpieceScoreUpperBound(baseline, gemModifiers, remaining, 2);
    assertFalse(upperBound.canMeetMinimum(0, remaining, 2, upperBound.estimate(0, remaining, 2)));

    remaining = new int[] {2, 2};
    upperBound =
        new Evaluator("damage reduction 10 max, -tie")
            .createTheoreticalCodpieceScoreUpperBound(baseline, gemModifiers, remaining, 2);
    upperBound.select(0);
    remaining[0]--;
    assertFalse(upperBound.isScoreSaturated(0, remaining, upperBound.estimate(0, remaining, 1)));
    upperBound.select(0);
    remaining[0]--;
    assertTrue(upperBound.isScoreSaturated(0, remaining, upperBound.estimate(0, remaining, 0)));
  }

  @Test
  void boundsDefaultTiebreaker() {
    var baseline = new Modifiers();
    var unsupportedGem = new Modifiers();
    unsupportedGem.setBoolean(BooleanModifier.COLD_IMMUNITY, true);
    assertThat(
        new Evaluator("adventures, 5 max")
            .createTheoreticalCodpieceTiebreakerUpperBound(
                baseline, new Modifiers[] {unsupportedGem}, new int[] {2}, 2),
        nullValue());

    var monsterLevelGem =
        ModifierDatabase.getModifiers(
            ModifierType.ETERNITY_CODPIECE, ItemPool.get("Tuesday's ruby").getItemId());
    var muscleGem =
        ModifierDatabase.getModifiers(
            ModifierType.ETERNITY_CODPIECE, ItemPool.get("crystallized memory").getItemId());
    var evaluator = new Evaluator("adventures, 5 max");
    int[] remaining = {2, 2};
    var upperBound =
        evaluator.createTheoreticalCodpieceTiebreakerUpperBound(
            baseline, new Modifiers[] {monsterLevelGem, muscleGem}, remaining, 2);
    var achievable = new Modifiers(baseline);
    achievable.add(monsterLevelGem);
    achievable.add(muscleGem);

    assertThat(upperBound, not(nullValue()));
    assertTrue(upperBound.estimate(0, remaining, 2) >= evaluator.getTiebreaker(achievable));
  }

  @Test
  void supportsEveryRealGemForModeledPrimaryScores() {
    var gemModifiers =
        ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE).stream()
            .filter(entry -> entry.getKey().isInt())
            .map(
                entry ->
                    ModifierDatabase.getModifiers(
                        ModifierType.ETERNITY_CODPIECE, entry.getKey().getIntValue()))
            .toArray(Modifiers[]::new);
    int[] remaining = new int[gemModifiers.length];
    Arrays.fill(remaining, 5);

    assertThat(
        new Evaluator("adventures, 5 max")
            .createTheoreticalCodpieceTiebreakerUpperBound(
                new Modifiers(), gemModifiers, remaining, 5),
        nullValue());
    assertThat(
        new Evaluator("adventures, 5 max")
            .createTheoreticalCodpieceTiebreakerUpperBound(
                new Modifiers(),
                gemModifiers,
                remaining,
                5,
                new CodpiecePruning.FamiliarScoreContributions(-1, Map.of())),
        not(nullValue()));
    for (String expression :
        List.of(
            "+combat",
            "-combat",
            "cold damage",
            "cold resistance",
            "cold spell damage",
            "familiar weight",
            "hot damage",
            "hot resistance",
            "hot spell damage",
            "initiative",
            "mana cost",
            "ranged damage",
            "sleaze damage",
            "sleaze resistance",
            "sleaze spell damage",
            "spell damage",
            "spooky damage",
            "spooky resistance",
            "spooky spell damage",
            "stench damage",
            "stench resistance",
            "stench spell damage",
            "thorns",
            "weapon damage")) {
      assertThat(
          expression,
          new Evaluator(expression)
              .createTheoreticalCodpieceScoreUpperBound(
                  new Modifiers(), gemModifiers, remaining, 5),
          not(nullValue()));
    }
  }

  @Test
  void supportsReportedRealWorldScoreExpression() {
    var gemModifiers =
        ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE).stream()
            .filter(entry -> entry.getKey().isInt())
            .map(
                entry ->
                    ModifierDatabase.getModifiers(
                        ModifierType.ETERNITY_CODPIECE, entry.getKey().getIntValue()))
            .toArray(Modifiers[]::new);
    int[] remaining = new int[gemModifiers.length];
    Arrays.fill(remaining, 5);

    var evaluator =
        new Evaluator(
            "5 item drop, 0.05 meat drop, 0.5 init 575 max, 0.1 da 1000 max, dr, "
                + "0.5 all res, 1.5 Mainstat, Moxie, 0.4 hp, 0.2 mp 1000 max, 3 HP Regen, "
                + "0.25 Spell Damage, 1.75 Spell Damage Percent, 2 Familiar Weight, "
                + "5 Familiar Experience, 5 Experience, 2.5 Mysticality Experience Percent, "
                + "49 Booze Drop 1900 max, 40 Pasta Thrall Experience, -fumble, +250");

    assertThat(
        evaluator.createTheoreticalCodpieceScoreUpperBound(
            new Modifiers(),
            gemModifiers,
            remaining,
            5,
            null,
            null,
            null,
            new CodpiecePruning.FamiliarScoreContributions(-1, Map.of())),
        not(nullValue()));
  }

  @ParameterizedTest
  @CsvSource({"item drop", "meat drop"})
  void fallsBackForIndirectPrimaryScores(String expression) {
    var familiarGem = new Modifiers();
    familiarGem.setDouble(DoubleModifier.FAMILIAR_WEIGHT, 1.0);
    assertThat(
        new Evaluator(expression)
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(), new Modifiers[] {familiarGem}, new int[] {1}, 1),
        nullValue());
  }

  @ParameterizedTest
  @CsvSource({"item drop,ITEMDROP", "meat drop,MEATDROP"})
  void boundsFamiliarDependentScoresInBothDirections(String expression, DoubleModifier modifier) {
    var familiarGem = new Modifiers();
    familiarGem.setDouble(DoubleModifier.FAMILIAR_WEIGHT, 1.0);
    var contributions =
        new CodpiecePruning.FamiliarScoreContributions(
            0, Map.of(modifier, new CodpiecePruning.ContributionRange(-3.0, 7.0)));
    var upperBound =
        new Evaluator(expression)
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(),
                new Modifiers[] {familiarGem},
                new int[] {1},
                1,
                null,
                null,
                null,
                contributions);

    assertThat(upperBound, not(nullValue()));
    assertTrue(upperBound.estimate(0, new int[] {1}, 1) >= 107.0);
    var negativeUpperBound =
        new Evaluator("-1 " + expression)
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(),
                new Modifiers[] {familiarGem},
                new int[] {1},
                1,
                null,
                null,
                null,
                contributions);
    assertThat(negativeUpperBound, not(nullValue()));
    assertTrue(negativeUpperBound.estimate(0, new int[] {1}, 1) >= -97.0);
  }

  @Test
  void boundsExperienceWithFamiliarWeightGems() {
    var familiarGem = new Modifiers();
    familiarGem.setDouble(DoubleModifier.FAMILIAR_WEIGHT, 1.0);

    assertThat(
        new Evaluator("experience")
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(), new Modifiers[] {familiarGem}, new int[] {1}, 1),
        nullValue());

    assertThat(
        new Evaluator("experience")
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(),
                new Modifiers[] {familiarGem},
                new int[] {1},
                1,
                null,
                null,
                null,
                new CodpiecePruning.FamiliarScoreContributions(-1, Map.of())),
        not(nullValue()));

    assertThat(
        new Evaluator("experience")
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(),
                new Modifiers[] {familiarGem},
                new int[] {1},
                1,
                null,
                null,
                null,
                new CodpiecePruning.FamiliarScoreContributions(
                    0,
                    Map.of(
                        DoubleModifier.EXPERIENCE,
                        new CodpiecePruning.ContributionRange(1.0, 1.0)))),
        not(nullValue()));
  }

  @Test
  void boundsExperienceCrossTermsByMaximumMarginal() {
    var baseline = new Modifiers();
    baseline.setDouble(DoubleModifier.MONSTER_LEVEL, 10.0);
    var monsterLevelGem = new Modifiers();
    monsterLevelGem.setDouble(DoubleModifier.MONSTER_LEVEL, 5.0);
    var experienceGem = new Modifiers();
    experienceGem.setDouble(DoubleModifier.primeStatExp(), 2.0);
    var gemModifiers = new Modifiers[] {monsterLevelGem, experienceGem};
    int[] remaining = {5, 5};
    var evaluator = new Evaluator("experience");
    var upperBound =
        evaluator.createTheoreticalCodpieceScoreUpperBound(baseline, gemModifiers, remaining, 5);
    double achievable = Double.NEGATIVE_INFINITY;
    for (int monsterLevelCopies = 0; monsterLevelCopies <= 5; monsterLevelCopies++) {
      for (int experienceCopies = 0;
          experienceCopies <= 5 - monsterLevelCopies;
          experienceCopies++) {
        var modifiers = new Modifiers(baseline);
        modifiers.setDouble(
            DoubleModifier.MONSTER_LEVEL,
            baseline.getDouble(DoubleModifier.MONSTER_LEVEL)
                + monsterLevelCopies * monsterLevelGem.getDouble(DoubleModifier.MONSTER_LEVEL));
        modifiers.setDouble(
            DoubleModifier.primeStatExp(),
            baseline.getDouble(DoubleModifier.primeStatExp())
                + experienceCopies * experienceGem.getDouble(DoubleModifier.primeStatExp()));
        achievable = Math.max(achievable, evaluator.getScore(modifiers));
      }
    }

    assertThat(upperBound, not(nullValue()));
    assertTrue(upperBound.estimate(0, remaining, 5) >= achievable);
  }

  @ParameterizedTest
  @CsvSource({"muscle experience", "mysticality experience", "moxie experience"})
  void boundsStatExperienceAdjustedByMonsterLevel(String expression) {
    var monsterLevelGem = new Modifiers();
    monsterLevelGem.setDouble(DoubleModifier.MONSTER_LEVEL, 5.0);

    assertThat(
        new Evaluator(expression)
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(), new Modifiers[] {monsterLevelGem}, new int[] {1}, 1),
        not(nullValue()));
  }

  @Test
  void fallsBackForTooManyExperienceInputs() {
    var gemModifiers = new Modifiers[9];
    Arrays.setAll(
        gemModifiers,
        ignored -> {
          var modifiers = new Modifiers();
          modifiers.setDouble(DoubleModifier.MONSTER_LEVEL, 1.0);
          return modifiers;
        });
    int[] remaining = new int[gemModifiers.length];
    Arrays.fill(remaining, 1);

    assertThat(
        new Evaluator("experience")
            .createTheoreticalCodpieceScoreUpperBound(new Modifiers(), gemModifiers, remaining, 5),
        nullValue());
  }

  @ParameterizedTest
  @CsvSource({"experience", "item drop", "meat drop"})
  void boundsIndirectPrimaryScoresWithoutFamiliarGems(String expression) {
    assertThat(
        new Evaluator(expression)
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(), new Modifiers[] {new Modifiers()}, new int[] {1}, 1),
        not(nullValue()));
  }

  @ParameterizedTest
  @CsvSource({
    "2 clownosity, CLOWNINESS, 50",
    "4 raveosity, RAVEOSITY, 4",
    "3 surgeonosity, SURGEONOSITY, 3",
    "2 stinky cheese, STINKYCHEESE, 3"
  })
  void includesInvariantBitmapInPrimaryScore(
      String expression, BitmapModifier modifier, int value) {
    var evaluator = new Evaluator("initiative, " + expression + ", -tie");
    var baseline = new Modifiers();
    baseline.setDouble(DoubleModifier.INITIATIVE, 10.0);
    baseline.setBitmap(modifier, value);
    int[] remaining = {1};
    var upperBound =
        evaluator.createTheoreticalCodpieceScoreUpperBound(
            baseline, new Modifiers[] {new Modifiers()}, remaining, 1);

    assertThat(upperBound, not(nullValue()));
    assertThat(
        upperBound.estimate(0, remaining, 1),
        equalTo(new Evaluator("initiative, " + expression + ", -tie").getScore(baseline)));
  }

  @Test
  void boundsFamiliarWeightAsPrimaryScore() {
    var baseline = new Modifiers();
    baseline.setDouble(DoubleModifier.FAMILIAR_WEIGHT, 10.0);
    baseline.setDouble(DoubleModifier.HIDDEN_FAMILIAR_WEIGHT, 2.0);
    baseline.setDouble(DoubleModifier.FAMILIAR_WEIGHT_PCT, -1.0);
    var firstGem = new Modifiers();
    firstGem.setDouble(DoubleModifier.FAMILIAR_WEIGHT, 5.0);
    var secondGem = new Modifiers();
    secondGem.setDouble(DoubleModifier.FAMILIAR_WEIGHT, 3.0);
    var evaluator = new Evaluator("familiar weight, -tie");
    int[] remaining = {1, 1};
    var upperBound =
        evaluator.createTheoreticalCodpieceScoreUpperBound(
            baseline, new Modifiers[] {firstGem, secondGem}, remaining, 2);
    var achievable = new Modifiers(baseline);
    achievable.setDouble(DoubleModifier.FAMILIAR_WEIGHT, 18.0);

    assertThat(upperBound, not(nullValue()));
    assertTrue(upperBound.estimate(0, remaining, 2) >= evaluator.getScore(achievable));

    var percentGem = new Modifiers();
    percentGem.setDouble(DoubleModifier.FAMILIAR_WEIGHT_PCT, 10.0);
    assertThat(
        evaluator.createTheoreticalCodpieceScoreUpperBound(
            baseline, new Modifiers[] {percentGem}, new int[] {1}, 1),
        nullValue());
  }

  @Test
  void boundsInitiativeAsPrimaryScore() {
    var baseline = new Modifiers();
    baseline.setDouble(DoubleModifier.INITIATIVE, 20.0);
    baseline.setDouble(DoubleModifier.INITIATIVE_PENALTY, -10.0);
    var firstGem = new Modifiers();
    firstGem.setDouble(DoubleModifier.INITIATIVE, 15.0);
    var secondGem = new Modifiers();
    secondGem.setDouble(DoubleModifier.INITIATIVE, 5.0);
    var evaluator = new Evaluator("initiative, -tie");
    int[] remaining = {1, 1};
    var upperBound =
        evaluator.createTheoreticalCodpieceScoreUpperBound(
            baseline, new Modifiers[] {firstGem, secondGem}, remaining, 2);
    var achievable = new Modifiers(baseline);
    achievable.setDouble(DoubleModifier.INITIATIVE, 40.0);

    assertThat(upperBound, not(nullValue()));
    assertTrue(upperBound.estimate(0, remaining, 2) >= evaluator.getScore(achievable));

    var penaltyGem = new Modifiers();
    penaltyGem.setDouble(DoubleModifier.INITIATIVE_PENALTY, 10.0);
    assertThat(
        evaluator.createTheoreticalCodpieceScoreUpperBound(
            baseline, new Modifiers[] {penaltyGem}, new int[] {1}, 1),
        nullValue());
  }

  @Test
  void boundsResistanceImmunityAsPrimaryScore() {
    var baseline = new Modifiers();
    baseline.setBoolean(BooleanModifier.COLD_IMMUNITY, true);
    var gem = new Modifiers();
    gem.setDouble(DoubleModifier.COLD_RESISTANCE, -5.0);
    var evaluator = new Evaluator("cold resistance, -tie");
    int[] remaining = {1};
    var upperBound =
        evaluator.createTheoreticalCodpieceScoreUpperBound(
            baseline, new Modifiers[] {gem}, remaining, 1);
    var achievable = new Modifiers(baseline);
    achievable.setDouble(DoubleModifier.COLD_RESISTANCE, -5.0);

    assertThat(upperBound, not(nullValue()));
    assertTrue(upperBound.estimate(0, remaining, 1) >= evaluator.getScore(achievable));
  }

  @Test
  void boundsNegativeExperienceAsPrimaryScore() {
    var baseline = new Modifiers();
    baseline.setDouble(DoubleModifier.MONSTER_LEVEL, 10.0);
    var monsterLevelGem = new Modifiers();
    monsterLevelGem.setDouble(DoubleModifier.MONSTER_LEVEL, -5.0);
    var experienceGem = new Modifiers();
    experienceGem.setDouble(DoubleModifier.primeStatExp(), -2.0);
    var gemModifiers = new Modifiers[] {monsterLevelGem, experienceGem};
    int[] remaining = {5, 5};
    var evaluator = new Evaluator("-1 experience, -tie");
    var upperBound =
        evaluator.createTheoreticalCodpieceScoreUpperBound(baseline, gemModifiers, remaining, 5);
    double achievable = Double.NEGATIVE_INFINITY;
    for (int monsterLevelCopies = 0; monsterLevelCopies <= 5; monsterLevelCopies++) {
      for (int experienceCopies = 0;
          experienceCopies <= 5 - monsterLevelCopies;
          experienceCopies++) {
        var modifiers = new Modifiers(baseline);
        modifiers.setDouble(
            DoubleModifier.MONSTER_LEVEL,
            baseline.getDouble(DoubleModifier.MONSTER_LEVEL)
                + monsterLevelCopies * monsterLevelGem.getDouble(DoubleModifier.MONSTER_LEVEL));
        modifiers.setDouble(
            DoubleModifier.primeStatExp(),
            baseline.getDouble(DoubleModifier.primeStatExp())
                + experienceCopies * experienceGem.getDouble(DoubleModifier.primeStatExp()));
        achievable = Math.max(achievable, evaluator.getScore(modifiers));
      }
    }

    assertThat(upperBound, not(nullValue()));
    assertTrue(upperBound.estimate(0, remaining, 5) >= achievable);
  }

  @Test
  void boundsEnchantmentCountAsPrimaryScore() {
    var gem = new Modifiers();
    gem.setDouble(DoubleModifier.ENCHANTMENT_COUNT, 2.0);
    int[] remaining = {2};
    var upperBound =
        new Evaluator("enchantment count, -tie")
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(), new Modifiers[] {gem}, remaining, 2);

    assertThat(upperBound, not(nullValue()));
    assertTrue(upperBound.estimate(0, remaining, 2) >= 4.0);
  }

  @ParameterizedTest
  @CsvSource({
    "hat / pants drop,HAT_PANTS_DROP,HATDROP,PANTSDROP",
    "maximum hp / mp,MAXIMUM_HP_MP,HP,MP"
  })
  void boundsMinimumDerivedScores(
      String expression,
      DoubleModifier scoreModifier,
      DoubleModifier firstComponent,
      DoubleModifier secondComponent) {
    var baseline = new Modifiers();
    baseline.setDouble(firstComponent, 10.0);
    baseline.setDouble(secondComponent, 20.0);
    var firstGem = new Modifiers();
    firstGem.setDouble(firstComponent, 15.0);
    var secondGem = new Modifiers();
    secondGem.setDouble(secondComponent, -5.0);
    var gemModifiers = new Modifiers[] {firstGem, secondGem};
    int[] remaining = {1, 1};

    for (String sign : List.of("", "-1 ")) {
      var evaluator = new Evaluator(sign + expression + ", -tie");
      var upperBound =
          evaluator.createTheoreticalCodpieceScoreUpperBound(baseline, gemModifiers, remaining, 2);
      double achievable = Double.NEGATIVE_INFINITY;
      for (int first = 0; first <= 1; first++) {
        for (int second = 0; second <= 1; second++) {
          var modifiers = new Modifiers(baseline);
          modifiers.setDouble(firstComponent, 10.0 + first * 15.0);
          modifiers.setDouble(secondComponent, 20.0 - second * 5.0);
          achievable = Math.max(achievable, evaluator.getScore(modifiers));
        }
      }

      assertThat(upperBound, not(nullValue()));
      assertTrue(upperBound.estimate(0, remaining, 2) >= achievable);
      assertThat(baseline.getDouble(scoreModifier), equalTo(10.0));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "all attributes,ALL_ATTRIBUTES,MUS,MYS,MOX",
    "all attributes percent,ALL_ATTRIBUTES_PCT,MUS_PCT,MYS_PCT,MOX_PCT"
  })
  void boundsThreeComponentCombinedScores(
      String expression,
      DoubleModifier scoreModifier,
      DoubleModifier firstComponent,
      DoubleModifier secondComponent,
      DoubleModifier thirdComponent) {
    var baseline = new Modifiers();
    baseline.setDouble(firstComponent, 10.0);
    baseline.setDouble(secondComponent, 20.0);
    baseline.setDouble(thirdComponent, 30.0);
    var gem = new Modifiers();
    gem.setDouble(firstComponent, 5.0);
    gem.setDouble(secondComponent, -5.0);
    int[] remaining = {1};

    for (String sign : List.of("", "-1 ")) {
      var evaluator = new Evaluator(sign + expression + ", -tie");
      var upperBound =
          evaluator.createTheoreticalCodpieceScoreUpperBound(
              baseline, new Modifiers[] {gem}, remaining, 1);
      var achievable = new Modifiers(baseline);
      achievable.setDouble(firstComponent, 15.0);
      achievable.setDouble(secondComponent, 15.0);

      assertThat(upperBound, not(nullValue()));
      assertTrue(upperBound.estimate(0, remaining, 1) >= evaluator.getScore(achievable));
      assertThat(baseline.getDouble(scoreModifier), equalTo(10.0));
    }
  }

  @Test
  void boundsCombinedScoresThatCrossZero() {
    var negativeBaseline = new Modifiers();
    negativeBaseline.setDouble(DoubleModifier.HP, -1.0);
    negativeBaseline.setDouble(DoubleModifier.MP, -100.0);
    negativeBaseline.addDouble(DoubleModifier.MAXIMUM_HP_MP, 1.0, ModifierType.GENERATED, "test");
    negativeBaseline.addDouble(DoubleModifier.MAXIMUM_HP_MP, -1.0, ModifierType.GENERATED, "test");
    var positiveGem = new Modifiers();
    positiveGem.setDouble(DoubleModifier.HP, 2.0);
    positiveGem.setDouble(DoubleModifier.MP, 2.0);
    var straddlingBaseline = new Modifiers();
    straddlingBaseline.setDouble(DoubleModifier.HP, -100.0);
    straddlingBaseline.setDouble(DoubleModifier.MP, 1.0);
    var negativeGem = new Modifiers();
    negativeGem.setDouble(DoubleModifier.MP, -2.0);

    assertTrue(
        estimate(
                new Evaluator("maximum hp / mp, -tie"),
                negativeBaseline,
                new Modifiers[] {positiveGem},
                new int[] {1},
                1)
            >= 0.0);
    assertTrue(
        estimate(
                new Evaluator("-1 maximum hp / mp, -tie"),
                straddlingBaseline,
                new Modifiers[] {negativeGem},
                new int[] {1},
                1)
            >= 100.0);
  }

  @Test
  void doesNotUseNegativeObjectiveBoundsForMinimumRequirements() {
    var baseline = new Modifiers();
    baseline.setDouble(DoubleModifier.HP, -100.0);
    baseline.setDouble(DoubleModifier.MP, 1.0);
    var negativeGem = new Modifiers();
    negativeGem.setDouble(DoubleModifier.MP, -2.0);
    var positiveGem = new Modifiers();
    positiveGem.setDouble(DoubleModifier.MP, 2.0);
    var gemModifiers = new Modifiers[] {negativeGem, positiveGem};
    int[] remaining = {1, 1};
    var upperBound =
        new Evaluator("-1 maximum hp / mp 0 min, -tie")
            .createTheoreticalCodpieceScoreUpperBound(baseline, gemModifiers, remaining, 2);

    assertThat(upperBound, not(nullValue()));
    upperBound.select(0);
    remaining[0]--;
    assertTrue(upperBound.canMeetMinimum(1, remaining, 1, upperBound.estimate(1, remaining, 1)));
  }

  @Test
  void usesZeroWeightObjectiveBoundsForMinimumRequirements() {
    var negativeGem = new Modifiers();
    negativeGem.setDouble(DoubleModifier.DAMAGE_REDUCTION, -5.0);
    int[] remaining = {1};
    var upperBound =
        new Evaluator("0 damage reduction 5 min, -tie")
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(), new Modifiers[] {negativeGem}, remaining, 1);

    assertThat(upperBound, not(nullValue()));
    assertFalse(upperBound.canMeetMinimum(0, remaining, 1, upperBound.estimate(0, remaining, 1)));
  }

  @ParameterizedTest
  @CsvSource({
    "hat / pants drop, HAT_PANTS_DROP",
    "maximum hp / mp, MAXIMUM_HP_MP",
    "all attributes, ALL_ATTRIBUTES",
    "all attributes percent, ALL_ATTRIBUTES_PCT",
  })
  void fallsBackForExplicitMinimumDerivedGem(String expression, DoubleModifier modifier) {
    var gem = new Modifiers();
    gem.setDouble(modifier, 1.0);

    for (String sign : List.of("", "-1 ")) {
      assertThat(
          new Evaluator(sign + expression + ", -tie")
              .createTheoreticalCodpieceScoreUpperBound(
                  new Modifiers(), new Modifiers[] {gem}, new int[] {1}, 1),
          nullValue());
    }
  }

  @ParameterizedTest
  @CsvSource({
    "familiar weight percent,FAMILIAR_WEIGHT_PCT,-10,-20,-5",
    "muscle limit,MUS_LIMIT,100,50,200"
  })
  void boundsLowestValueScores(
      String expression,
      DoubleModifier modifier,
      double baselineValue,
      double firstValue,
      double secondValue) {
    var baseline = new Modifiers();
    baseline.setDouble(modifier, baselineValue);
    var firstGem = new Modifiers();
    firstGem.setDouble(modifier, firstValue);
    var secondGem = new Modifiers();
    secondGem.setDouble(modifier, secondValue);
    var gemModifiers = new Modifiers[] {firstGem, secondGem};
    int[] remaining = {1, 1};

    for (String sign : List.of("", "-1 ")) {
      var upperBound =
          new Evaluator(sign + expression + ", -tie")
              .createTheoreticalCodpieceScoreUpperBound(baseline, gemModifiers, remaining, 2);
      assertThat(upperBound, not(nullValue()));
      double bestValue =
          modifier == DoubleModifier.FAMILIAR_WEIGHT_PCT
              ? Math.min(0.0, Math.min(baselineValue, Math.min(firstValue, secondValue)))
              : Math.min(baselineValue, Math.min(firstValue, secondValue));
      assertTrue(
          upperBound.estimate(0, remaining, 2)
              >= (sign.isEmpty() ? Math.max(baselineValue, bestValue) : -bestValue));
    }
  }

  @Test
  void boundsPrismaticDamage() {
    var baseline = new Modifiers();
    for (DoubleModifier modifier :
        List.of(
            DoubleModifier.COLD_DAMAGE,
            DoubleModifier.HOT_DAMAGE,
            DoubleModifier.SLEAZE_DAMAGE,
            DoubleModifier.SPOOKY_DAMAGE,
            DoubleModifier.STENCH_DAMAGE)) {
      baseline.setDouble(modifier, 10.0);
    }
    var gem = new Modifiers();
    gem.setDouble(DoubleModifier.COLD_DAMAGE, 5.0);
    gem.setDouble(DoubleModifier.HOT_DAMAGE, -2.0);
    int[] remaining = {1};

    for (String sign : List.of("", "-1 ")) {
      var evaluator = new Evaluator(sign + "prismatic damage, -tie");
      var upperBound =
          evaluator.createTheoreticalCodpieceScoreUpperBound(
              baseline, new Modifiers[] {gem}, remaining, 1);
      var achievable = new Modifiers(baseline);
      achievable.setDouble(DoubleModifier.COLD_DAMAGE, 15.0);
      achievable.setDouble(DoubleModifier.HOT_DAMAGE, 8.0);

      assertThat(upperBound, not(nullValue()));
      assertTrue(upperBound.estimate(0, remaining, 1) >= evaluator.getScore(achievable));
    }
  }

  @Test
  void classifiesEveryNumericObjectiveInBothDirections() {
    var baseline = new Modifiers();
    var gemModifiers = new Modifiers[] {new Modifiers()};
    var familiarContributions = new CodpiecePruning.FamiliarScoreContributions(-1, Map.of());
    var unsupported =
        Arrays.stream(DoubleModifier.values())
            .filter(
                modifier ->
                    !CodpiecePruning.supportsScoreTerm(
                            modifier, 1.0, baseline, gemModifiers, familiarContributions)
                        || !CodpiecePruning.supportsScoreTerm(
                            modifier, -1.0, baseline, gemModifiers, familiarContributions))
            .toList();

    assertThat(unsupported, equalTo(List.of()));
  }

  @ParameterizedTest
  @CsvSource({
    "MUS, MUS_PCT, BUFFED_MUS, FLOOR_BUFFED_MUSCLE, Mysticality",
    "MYS, MYS_PCT, BUFFED_MYS, FLOOR_BUFFED_MYST, Muscle",
    "MOX, MOX_PCT, BUFFED_MOX, FLOOR_BUFFED_MOXIE, Mysticality"
  })
  void boundsDerivedStats(
      DoubleModifier flat,
      DoubleModifier percent,
      DerivedModifier derived,
      StringModifier floor,
      String floorValue) {
    String expression = flat.getName();
    var baseline = new Modifiers();
    var flatGem = new Modifiers();
    flatGem.setDouble(flat, 5.0);
    var percentGem = new Modifiers();
    percentGem.setDouble(percent, 11.0);
    double baseStatUpperBound =
        Math.max(
            KoLCharacter.getBaseMuscle(),
            Math.max(KoLCharacter.getBaseMysticality(), KoLCharacter.getBaseMoxie()));
    assertThat(
        estimate(
            new Evaluator(expression + ", -tie"),
            baseline,
            new Modifiers[] {flatGem, percentGem},
            new int[] {1, 1},
            2),
        equalTo(baseline.predict().get(derived) + 5.0 + Math.ceil(baseStatUpperBound * 0.11)));
    assertThat(
        estimate(
            new Evaluator("-" + expression + ", -tie"),
            baseline,
            new Modifiers[] {flatGem},
            new int[] {1},
            1),
        equalTo((double) -baseline.predict().get(derived)));

    baseline.setString(floor, floorValue);
    assertThat(
        estimate(
            new Evaluator(expression + ", -tie"),
            baseline,
            new Modifiers[] {flatGem},
            new int[] {1},
            1),
        equalTo(Double.POSITIVE_INFINITY));
  }

  @ParameterizedTest
  @CsvSource({
    "muscle, MUS",
    "maximum hp, HP",
    "maximum mp, MP",
  })
  void fallsBackForNegativeDerivedStatInputs(String expression, DoubleModifier modifier) {
    var gem = new Modifiers();
    gem.setDouble(modifier, -1.0);

    assertThat(
        new Evaluator("-1 " + expression + ", -tie")
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(), new Modifiers[] {gem}, new int[] {1}, 1),
        nullValue());
  }

  @Test
  void derivedBoundsDoNotSaturate() {
    var gem = new Modifiers();
    gem.setDouble(DoubleModifier.MUS, 5.0);
    var upperBound =
        new Evaluator("mus 10 max, -tie")
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(), new Modifiers[] {gem}, new int[] {1}, 1);

    assertFalse(
        upperBound.isScoreSaturated(0, new int[] {1}, upperBound.estimate(0, new int[] {1}, 1)));
  }

  @Test
  void boundsHitPointsAndManaPoints() {
    var baseline = new Modifiers();
    var muscleGem = new Modifiers();
    muscleGem.setDouble(DoubleModifier.MUS, 5.0);
    var mysticalityGem = new Modifiers();
    mysticalityGem.setDouble(DoubleModifier.MYS, 5.0);
    var moxieGem = new Modifiers();
    moxieGem.setDouble(DoubleModifier.MOX, 5.0);
    var hitPointGem = new Modifiers();
    hitPointGem.setDouble(DoubleModifier.HP, 20.0);
    var hitPointPercentGem = new Modifiers();
    hitPointPercentGem.setDouble(DoubleModifier.HP_PCT, 20.0);
    var hitPointUpperBound =
        new Evaluator("maximum hp, -tie")
            .createTheoreticalCodpieceScoreUpperBound(
                baseline,
                new Modifiers[] {hitPointGem, hitPointPercentGem, muscleGem},
                new int[] {1, 1, 1},
                3);
    assertThat(hitPointUpperBound, not(nullValue()));
    assertTrue(
        hitPointUpperBound.estimate(0, new int[] {1, 1, 1}, 3)
            >= baseline.predict().get(DerivedModifier.BUFFED_HP) + 20.0);
    assertFalse(
        hitPointUpperBound.isScoreSaturated(
            0, new int[] {1, 1, 1}, hitPointUpperBound.estimate(0, new int[] {1, 1, 1}, 3)));
    assertThat(
        estimate(
            new Evaluator("-maximum hp, -tie"),
            baseline,
            new Modifiers[] {hitPointGem},
            new int[] {1},
            1),
        equalTo((double) -baseline.predict().get(DerivedModifier.BUFFED_HP)));

    var manaPointGem = new Modifiers();
    manaPointGem.setDouble(DoubleModifier.MP, 20.0);
    var manaPointPercentGem = new Modifiers();
    manaPointPercentGem.setDouble(DoubleModifier.MP_PCT, 20.0);
    var manaPointUpperBound =
        new Evaluator("maximum mp, -tie")
            .createTheoreticalCodpieceScoreUpperBound(
                baseline,
                new Modifiers[] {manaPointGem, manaPointPercentGem, mysticalityGem, moxieGem},
                new int[] {1, 1, 1, 1},
                4);
    assertThat(manaPointUpperBound, not(nullValue()));
    assertTrue(
        manaPointUpperBound.estimate(0, new int[] {1, 1, 1, 1}, 4)
            >= baseline.predict().get(DerivedModifier.BUFFED_MP) + 20.0);
    assertFalse(
        manaPointUpperBound.isScoreSaturated(
            0, new int[] {1, 1, 1, 1}, manaPointUpperBound.estimate(0, new int[] {1, 1, 1, 1}, 4)));
    assertThat(
        estimate(
            new Evaluator("-maximum mp, -tie"),
            baseline,
            new Modifiers[] {manaPointGem},
            new int[] {1},
            1),
        equalTo((double) -baseline.predict().get(DerivedModifier.BUFFED_MP)));
  }

  @Test
  void boundsRolloverEffectsWithoutSaturation() {
    var baseline = new Modifiers();
    baseline.setDouble(DoubleModifier.DAMAGE_REDUCTION, 1.0);
    var rolloverGem = new Modifiers();
    rolloverGem.setString(StringModifier.ROLLOVER_EFFECT, "test");
    var upperBound =
        new Evaluator("damage reduction, -tie")
            .createTheoreticalCodpieceScoreUpperBound(
                baseline, new Modifiers[] {rolloverGem}, new int[] {1}, 1);

    assertEquals(
        baseline.getDouble(DoubleModifier.DAMAGE_REDUCTION) + 0.01,
        upperBound.estimate(0, new int[] {1}, 1),
        0.0001);
    assertFalse(
        upperBound.isScoreSaturated(0, new int[] {1}, upperBound.estimate(0, new int[] {1}, 1)));
  }

  @Test
  void boundsBooleanContributionsByRemainingCopies() {
    int dropper = ItemPool.get("unblemished pearl").getItemId();
    int ordinary = ItemPool.get("Baseball Diamond").getItemId();
    var cleanups =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, dropper, "Drops Items"),
            withOverrideModifiers(ModifierType.ITEM, ordinary, ""));

    try (cleanups) {
      var gems =
          List.of(
              new CheckedItem(dropper, EquipScope.SPECULATE_INVENTORY, 0, PriceLevel.DONT_CHECK),
              new CheckedItem(ordinary, EquipScope.SPECULATE_INVENTORY, 0, PriceLevel.DONT_CHECK));
      var upperBound =
          new CodpiecePruning.BooleanUpperBound(
              gems, new int[] {2, 2}, 2, BooleanModifier.DROPS_ITEMS);

      assertThat(upperBound.estimateAdditional(0, new int[] {2, 2}, 2), equalTo(2));
      assertThat(upperBound.estimateAdditional(0, new int[] {1, 2}, 2), equalTo(1));
      assertThat(upperBound.estimateAdditional(1, new int[] {1, 2}, 2), equalTo(0));
    }
  }

  @Test
  void calculatesSharedScoreValues() {
    var modifiers = new Modifiers();
    modifiers.setDouble(DoubleModifier.FAMILIAR_WEIGHT, 10.0);
    modifiers.setDouble(DoubleModifier.HIDDEN_FAMILIAR_WEIGHT, 4.0);
    modifiers.setDouble(DoubleModifier.FAMILIAR_WEIGHT_PCT, -1.0);
    modifiers.setDouble(DoubleModifier.INITIATIVE, 20.0);
    modifiers.setDouble(DoubleModifier.INITIATIVE_PENALTY, -5.0);
    modifiers.setDouble(DoubleModifier.MANA_COST, -3.0);
    modifiers.setDouble(DoubleModifier.STACKABLE_MANA_COST, -2.0);
    modifiers.setDouble(DoubleModifier.ITEMDROP, 25.0);
    modifiers.setDouble(DoubleModifier.ITEMDROP_PENALTY, -10.0);
    modifiers.setDouble(DoubleModifier.SPORADIC_ITEMDROP, 5.0);
    modifiers.setDouble(DoubleModifier.MEATDROP, 30.0);
    modifiers.setDouble(DoubleModifier.MEATDROP_PENALTY, -5.0);
    modifiers.setDouble(DoubleModifier.SPORADIC_MEATDROP, 10.0);
    modifiers.setDouble(DoubleModifier.MEAT_BONUS, 10000.0);
    modifiers.setDouble(DoubleModifier.WEAPON_DAMAGE, 4.0);
    modifiers.setDouble(DoubleModifier.WEAPON_DAMAGE_PCT, 6.0);
    modifiers.setDouble(DoubleModifier.DAMAGE_AURA, 7.0);
    modifiers.setDouble(DoubleModifier.SPORADIC_DAMAGE_AURA, 3.0);
    modifiers.setDouble(DoubleModifier.COLD_RESISTANCE, 8.0);
    modifiers.setBoolean(BooleanModifier.COLD_VULNERABILITY, true);

    var expected =
        Map.ofEntries(
            Map.entry(DoubleModifier.FAMILIAR_WEIGHT, 7.0),
            Map.entry(DoubleModifier.INITIATIVE, 15.0),
            Map.entry(DoubleModifier.MANA_COST, -5.0),
            Map.entry(DoubleModifier.ITEMDROP, 120.0),
            Map.entry(DoubleModifier.MEATDROP, 136.0),
            Map.entry(DoubleModifier.WEAPON_DAMAGE, 10.0),
            Map.entry(DoubleModifier.DAMAGE_AURA, 10.0),
            Map.entry(DoubleModifier.COLD_RESISTANCE, -92.0));

    var predicted = modifiers.predict();
    for (var entry : expected.entrySet()) {
      assertEquals(
          entry.getValue(),
          Evaluator.scoreValue(entry.getKey(), modifiers, predicted),
          0.0001,
          entry.getKey().getName());
    }
  }

  @Test
  void checksBooleanRequirementsAgainstRemainingGems() {
    var booleanGem = new Modifiers();
    booleanGem.setBoolean(BooleanModifier.DROPS_ITEMS, true);

    int[] remaining = {1};
    var attainable =
        new Evaluator("drops items, -tie")
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(), new Modifiers[] {booleanGem}, remaining, 1);
    assertTrue(attainable.canMeetMinimum(0, remaining, 1, attainable.estimate(0, remaining, 1)));

    remaining = new int[] {0};
    var unattainable =
        new Evaluator("drops items, -tie")
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(), new Modifiers[] {booleanGem}, remaining, 1);
    assertFalse(
        unattainable.canMeetMinimum(0, remaining, 1, unattainable.estimate(0, remaining, 1)));

    var forbiddenBaseline = new Modifiers();
    forbiddenBaseline.setBoolean(BooleanModifier.DROPS_ITEMS, true);
    int[] forbiddenRemaining = {1};
    var forbidden =
        new Evaluator("-1 drops items, -tie")
            .createTheoreticalCodpieceScoreUpperBound(
                forbiddenBaseline, new Modifiers[] {new Modifiers()}, forbiddenRemaining, 1);
    assertFalse(
        forbidden.canMeetMinimum(
            0, forbiddenRemaining, 1, forbidden.estimate(0, forbiddenRemaining, 1)));
  }

  @Test
  void treatsCandidateBitmapScoresAsInexact() {
    var gem = new Modifiers();
    gem.setBitmap(BitmapModifier.SURGEONOSITY, 1);
    int[] remaining = {1};
    var upperBound =
        new Evaluator("1 surgeonosity, -tie")
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(), new Modifiers[] {gem}, remaining, 1);

    assertThat(upperBound.estimate(0, remaining, 1), equalTo(1.0));
    assertFalse(upperBound.isScoreSaturated(0, remaining, 1.0));
  }

  @Test
  void preservesExplicitCombinedModifierValues() {
    var baseline = new Modifiers();
    baseline.setDouble(DoubleModifier.MAXIMUM_HP_MP, 5.0);
    var gem = new Modifiers();
    gem.setDouble(DoubleModifier.HP, 100.0);
    gem.setDouble(DoubleModifier.MP, 100.0);

    assertThat(
        estimate(
            new Evaluator("maximum hp / mp, -tie"),
            baseline,
            new Modifiers[] {gem},
            new int[] {1},
            1),
        equalTo(5.0));
  }

  @Test
  void boundsSourceExperienceWithAllStatTuning() {
    var cleanups = new Cleanups(withPath(Path.THE_SOURCE), withClass(AscensionClass.SEAL_CLUBBER));
    var baseline = new Modifiers();
    baseline.setDouble(DoubleModifier.MONSTER_LEVEL, 12.0);
    baseline.setDouble(DoubleModifier.EXPERIENCE, 3.0);
    baseline.setString(StringModifier.STAT_TUNING, "Muscle (all)");
    var gem = new Modifiers();
    gem.setDouble(DoubleModifier.MUS_EXPERIENCE, 2.0);

    try (cleanups) {
      for (String expression : List.of("experience, -tie", "muscle experience, -tie")) {
        var evaluator = new Evaluator(expression);
        var upperBound =
            evaluator.createTheoreticalCodpieceScoreUpperBound(
                baseline, new Modifiers[] {gem}, new int[] {1}, 1);
        var achievable = new Modifiers(baseline);
        achievable.setDouble(
            DoubleModifier.MUS_EXPERIENCE,
            baseline.getDouble(DoubleModifier.MUS_EXPERIENCE)
                + gem.getDouble(DoubleModifier.MUS_EXPERIENCE));

        assertThat(upperBound, not(nullValue()));
        assertTrue(upperBound.estimate(0, new int[] {1}, 1) >= evaluator.getScore(achievable));
      }
    }
  }

  @Test
  void estimatesDamageAuraFromNonzeroCandidateIndex() {
    var firstGem = new Modifiers();
    firstGem.setDouble(DoubleModifier.DAMAGE_AURA, 7.0);
    firstGem.setDouble(DoubleModifier.SPORADIC_DAMAGE_AURA, 3.0);
    var secondGem = new Modifiers();
    secondGem.setDouble(DoubleModifier.DAMAGE_AURA, 2.0);
    secondGem.setDouble(DoubleModifier.SPORADIC_DAMAGE_AURA, 1.0);
    int[] remaining = {1, 1};
    var upperBound =
        new Evaluator("damage aura, -tie")
            .createTheoreticalCodpieceScoreUpperBound(
                new Modifiers(), new Modifiers[] {firstGem, secondGem}, remaining, 1);

    assertEquals(3.0, upperBound.estimate(1, remaining, 1), 0.001);
  }

  @Test
  void ignoresNonpositiveLimitContributions() {
    var gem = new Modifiers();
    gem.setDouble(DoubleModifier.MUS_LIMIT, -10.0);

    assertThat(
        estimate(
            new Evaluator("muscle limit, -tie"),
            new Modifiers(),
            new Modifiers[] {gem},
            new int[] {1},
            1),
        equalTo(0.0));
  }
}
