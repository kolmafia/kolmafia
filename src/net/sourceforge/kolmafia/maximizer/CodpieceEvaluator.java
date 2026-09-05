package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.request.EquipmentRequest;

/** Builds evaluator-specific bounds and candidate ordering for Eternity Codpiece gems. */
final class CodpieceEvaluator {
  record Context(
      List<Evaluator.ScoreTerm> scoreModifiers,
      List<Evaluator.ScoreTerm> tiebreakerScoreModifiers,
      double totalMin,
      int clownosity,
      int raveosity,
      int surgeonosity,
      int stinkycheese,
      Set<BooleanModifier> booleanMask,
      Set<BooleanModifier> booleanValue,
      boolean noTiebreaker,
      boolean hasItemBonuses) {}

  record CandidateScore(double score, double tiebreaker) {}

  private final Evaluator evaluator;
  private final Map<Integer, CandidateScore> candidateScores = new HashMap<>();
  private final Map<Modifiers, Boolean> supportedTiebreakerModifiers = new IdentityHashMap<>();
  private final Map<Modifiers[], CodpieceScoreBound.ContributionBasis> contributionBases =
      new IdentityHashMap<>();

  CodpieceEvaluator(Evaluator evaluator) {
    this.evaluator = evaluator;
  }

  CodpieceScoreBound createTheoreticalCodpieceScoreUpperBound(
      Modifiers baseline, Modifiers[] gemModifiers, int[] remaining, int slotCount) {
    return this.createTheoreticalCodpieceScoreUpperBound(
        baseline, gemModifiers, remaining, slotCount, null, null, null, null);
  }

  private static boolean canMeetHardRequirements(
      Context context, Modifiers baseline, Modifiers[] gemModifiers, int[] remaining) {
    if (maximumBitmapValue(baseline, gemModifiers, remaining, BitmapModifier.CLOWNINESS)
            < context.clownosity()
        || maximumBitmapValue(baseline, gemModifiers, remaining, BitmapModifier.RAVEOSITY)
            < context.raveosity()
        || maximumBitmapValue(baseline, gemModifiers, remaining, BitmapModifier.SURGEONOSITY)
            < context.surgeonosity()) {
      return false;
    }

    for (BooleanModifier modifier : context.booleanMask()) {
      if (context.booleanValue().contains(modifier)) {
        if (!baseline.getBoolean(modifier)
            && !candidateProvidesBoolean(gemModifiers, remaining, modifier)) {
          return false;
        }
      } else if (baseline.getBoolean(modifier)) {
        return false;
      }
    }
    return true;
  }

  private static int maximumBitmapValue(
      Modifiers baseline, Modifiers[] gemModifiers, int[] remaining, BitmapModifier modifier) {
    int bits = baseline.getRawBitmap(modifier);
    for (int i = 0; i < gemModifiers.length; i++) {
      if (remaining[i] > 0 && gemModifiers[i] != null) {
        bits |= gemModifiers[i].getRawBitmap(modifier);
      }
    }
    int value = Integer.bitCount(bits);
    return modifier == BitmapModifier.CLOWNINESS ? value * 25 : value;
  }

  private static boolean candidateProvidesBoolean(
      Modifiers[] gemModifiers, int[] remaining, BooleanModifier modifier) {
    for (int i = 0; i < gemModifiers.length; i++) {
      if (remaining[i] > 0 && gemModifiers[i] != null && gemModifiers[i].getBoolean(modifier)) {
        return true;
      }
    }
    return false;
  }

  CodpieceScoreBound createTheoreticalCodpieceScoreUpperBound(
      Modifiers baseline,
      Modifiers[] gemModifiers,
      int[] remaining,
      int slotCount,
      Map<Slot, AdventureResult> equipment,
      Map<Modeable, String> modeables,
      List<CheckedItem> gems,
      CodpiecePruning.FamiliarScoreContributions familiarScoreContributions) {
    Context context = this.evaluator.codpieceContext();
    if (CodpiecePruning.forceExhaustiveForTests) {
      return null;
    }
    if (context.hasItemBonuses() && (equipment == null || modeables == null || gems == null)) {
      return null;
    }

    boolean hasOnlySupportedTiebreakerModifiers =
        this.hasOnlySupportedCodpieceTiebreakerModifiers(gemModifiers);
    for (var scoreModifier : context.scoreModifiers()) {
      var modifier = scoreModifier.modifier();
      if (!CodpiecePruning.supportsScoreTerm(
          modifier,
          scoreModifier.weight(),
          baseline,
          gemModifiers,
          familiarScoreContributions,
          hasOnlySupportedTiebreakerModifiers)) {
        return null;
      }
    }

    boolean baselineRollover = baseline.hasString(StringModifier.ROLLOVER_EFFECT);
    boolean candidateRollover =
        !baselineRollover
            && Arrays.stream(gemModifiers)
                .anyMatch(
                    modifiers ->
                        modifiers != null && modifiers.hasString(StringModifier.ROLLOVER_EFFECT));
    boolean candidateBitmapScore = this.hasCandidateBitmapScore(context, gemModifiers, remaining);
    double fixedScore =
        context.stinkycheese()
                * maximumBitmapValue(baseline, gemModifiers, remaining, BitmapModifier.STINKYCHEESE)
            + Math.min(
                maximumBitmapValue(baseline, gemModifiers, remaining, BitmapModifier.CLOWNINESS),
                context.clownosity())
            + Math.min(
                maximumBitmapValue(baseline, gemModifiers, remaining, BitmapModifier.RAVEOSITY),
                context.raveosity())
            + Math.min(
                maximumBitmapValue(baseline, gemModifiers, remaining, BitmapModifier.SURGEONOSITY),
                context.surgeonosity());
    double[] itemContributions = new double[gemModifiers.length];
    if (equipment != null) {
      for (AdventureResult item : equipment.values()) {
        fixedScore += this.evaluator.getItemScore(item, modeables);
      }
      double emptySlotScore = this.evaluator.getItemScore(EquipmentRequest.UNEQUIP, modeables);
      for (int i = 0; i < gems.size(); i++) {
        itemContributions[i] = this.evaluator.getItemScore(gems.get(i), modeables) - emptySlotScore;
      }
    }
    return new CodpieceScoreBound(
        context.scoreModifiers(),
        this.contributionBases.computeIfAbsent(
            gemModifiers,
            modifiers ->
                new CodpieceScoreBound.ContributionBasis(context.scoreModifiers(), modifiers)),
        baseline,
        gemModifiers,
        remaining,
        slotCount,
        context.totalMin(),
        fixedScore + (baselineRollover || candidateRollover ? 0.01f : 0.0),
        !candidateRollover && !candidateBitmapScore,
        false,
        itemContributions,
        familiarScoreContributions,
        canMeetHardRequirements(context, baseline, gemModifiers, remaining));
  }

  private boolean hasCandidateBitmapScore(
      Context context, Modifiers[] gemModifiers, int[] remaining) {
    for (int i = 0; i < gemModifiers.length; i++) {
      Modifiers modifiers = gemModifiers[i];
      if (remaining[i] > 0
          && modifiers != null
          && ((context.stinkycheese() > 0
                  && modifiers.getRawBitmap(BitmapModifier.STINKYCHEESE) != 0)
              || (context.clownosity() > 0
                  && modifiers.getRawBitmap(BitmapModifier.CLOWNINESS) != 0)
              || (context.raveosity() > 0 && modifiers.getRawBitmap(BitmapModifier.RAVEOSITY) != 0)
              || (context.surgeonosity() > 0
                  && modifiers.getRawBitmap(BitmapModifier.SURGEONOSITY) != 0))) {
        return true;
      }
    }
    return false;
  }

  EnumSet<DoubleModifier> familiarDependentScoreModifiers() {
    Context context = this.evaluator.codpieceContext();
    var modifiers = EnumSet.noneOf(DoubleModifier.class);
    List<Evaluator.ScoreTerm> scoreModifiers = new ArrayList<>(context.scoreModifiers());
    if (!context.noTiebreaker()) {
      scoreModifiers.addAll(context.tiebreakerScoreModifiers());
    }
    for (var scoreModifier : scoreModifiers) {
      if (scoreModifier.modifier() == DoubleModifier.ITEMDROP
          || scoreModifier.modifier() == DoubleModifier.MEATDROP
          || scoreModifier.modifier() == DoubleModifier.EXPERIENCE
          || scoreModifier.modifier() == DoubleModifier.MUS_EXPERIENCE
          || scoreModifier.modifier() == DoubleModifier.MYS_EXPERIENCE
          || scoreModifier.modifier() == DoubleModifier.MOX_EXPERIENCE) {
        modifiers.add(scoreModifier.modifier());
      }
    }
    return modifiers;
  }

  CodpieceScoreBound createTheoreticalCodpieceTiebreakerUpperBound(
      Modifiers baseline, Modifiers[] gemModifiers, int[] remaining, int slotCount) {
    return this.createTheoreticalCodpieceTiebreakerUpperBound(
        baseline, gemModifiers, remaining, slotCount, null);
  }

  CodpieceScoreBound createTheoreticalCodpieceTiebreakerUpperBound(
      Modifiers baseline,
      Modifiers[] gemModifiers,
      int[] remaining,
      int slotCount,
      CodpiecePruning.FamiliarScoreContributions familiarScoreContributions) {
    Context context = this.evaluator.codpieceContext();
    if (context.noTiebreaker()) {
      return null;
    }
    for (Modifiers modifiers : gemModifiers) {
      if (!this.hasOnlySupportedCodpieceTiebreakerModifiers(modifiers)) {
        return null;
      }
    }

    List<Evaluator.ScoreTerm> supported = new ArrayList<>();
    double fixedScore = this.evaluator.getTiebreaker(baseline);
    Map<DerivedModifier, Integer> predicted = null;
    for (var scoreModifier : context.tiebreakerScoreModifiers()) {
      if (!CodpiecePruning.supportsTiebreakerTerm(
          scoreModifier.modifier(),
          scoreModifier.weight(),
          baseline,
          gemModifiers,
          familiarScoreContributions)) {
        if ((scoreModifier.modifier() == DoubleModifier.ITEMDROP
                || scoreModifier.modifier() == DoubleModifier.MEATDROP
                || scoreModifier.modifier() == DoubleModifier.EXPERIENCE)
            && Arrays.stream(gemModifiers).anyMatch(CodpiecePruning::affectsFamiliarCalculation)) {
          return null;
        }
        continue;
      }
      supported.add(scoreModifier);
      DoubleModifier modifier = scoreModifier.modifier();
      if ((modifier == DoubleModifier.MUS
              || modifier == DoubleModifier.MYS
              || modifier == DoubleModifier.MOX
              || modifier == DoubleModifier.HP
              || modifier == DoubleModifier.MP)
          && predicted == null) {
        predicted = baseline.predict();
      }
      fixedScore -=
          scoreModifier.weight()
              * Math.min(Evaluator.scoreValue(modifier, baseline, predicted), scoreModifier.max());
    }

    return new CodpieceScoreBound(
        supported,
        new CodpieceScoreBound.ContributionBasis(supported, gemModifiers),
        baseline,
        gemModifiers,
        remaining,
        slotCount,
        Double.NEGATIVE_INFINITY,
        fixedScore,
        true,
        true,
        new double[gemModifiers.length],
        familiarScoreContributions,
        true);
  }

  private boolean hasOnlySupportedCodpieceTiebreakerModifiers(Modifiers[] modifiers) {
    for (Modifiers candidate : modifiers) {
      if (!this.hasOnlySupportedCodpieceTiebreakerModifiers(candidate)) {
        return false;
      }
    }
    return true;
  }

  private boolean hasOnlySupportedCodpieceTiebreakerModifiers(Modifiers modifiers) {
    return this.supportedTiebreakerModifiers.computeIfAbsent(
        modifiers, CodpiecePruning::hasOnlySupportedTiebreakerModifiers);
  }

  CandidateScore scoreCandidate(
      Modifiers modifiers, CheckedItem gem, double nullScore, double nullTiebreaker) {
    double score =
        this.evaluator.getScore(modifiers, Map.of(Slot.CODPIECE1, gem), Map.of()) - nullScore;
    double tiebreaker = this.evaluator.getTiebreaker(modifiers) - nullTiebreaker;
    var candidate = new CandidateScore(score, tiebreaker);
    this.candidateScores.put(gem.getItemId(), candidate);
    return candidate;
  }

  List<CheckedItem> prioritize(List<CheckedItem> gems) {
    return gems.stream()
        .sorted(
            Comparator.comparingDouble(
                    (CheckedItem gem) -> this.candidateScores.get(gem.getItemId()).score())
                .thenComparingDouble(gem -> this.candidateScores.get(gem.getItemId()).tiebreaker())
                .reversed())
        .toList();
  }
}
