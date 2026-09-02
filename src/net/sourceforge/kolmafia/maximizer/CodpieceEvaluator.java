package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;

/** Scores and orders Eternity Codpiece gem candidates for the evaluator's shortlist. */
final class CodpieceEvaluator {
  record Context(
      List<Evaluator.ScoreTerm> scoreModifiers,
      List<Evaluator.ScoreTerm> tiebreakerScoreModifiers,
      boolean noTiebreaker) {}

  record CandidateScore(double score, double tiebreaker) {}

  private final Evaluator evaluator;
  private final Map<Integer, CandidateScore> candidateScores = new HashMap<>();

  CodpieceEvaluator(Evaluator evaluator) {
    this.evaluator = evaluator;
  }

  /**
   * Score modifiers whose value depends on the familiar, and which therefore cannot be judged from
   * a gem's standalone contribution alone.
   */
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
