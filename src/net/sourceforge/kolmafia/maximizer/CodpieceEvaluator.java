package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;

/** Scores and orders Eternity Codpiece gem candidates for the evaluator's shortlist. */
final class CodpieceEvaluator {
  private static final ItemSlotGroup CODPIECE = ItemSlotGroup.ETERNITY_CODPIECE;

  record Context(
      List<Evaluator.ScoreTerm> scoreModifiers,
      List<Evaluator.ScoreTerm> tiebreakerScoreModifiers,
      boolean noTiebreaker) {}

  record CandidateScore(double score, double tiebreaker) {}

  record Candidates(List<CheckedItem> catalog, List<CheckedItem> ranked) {}

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

  Candidates compileCandidates(
      EquipScope equipScope,
      long maxPrice,
      PriceLevel priceLevel,
      double nullScore,
      double nullTiebreaker) {
    List<CheckedItem> catalog = new ArrayList<>();
    List<CheckedItem> ranked = new ArrayList<>();
    boolean usesFamiliarDependentScore = !this.familiarDependentScoreModifiers().isEmpty();

    for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
      if (!entry.getKey().isInt()) {
        continue;
      }

      int gemId = entry.getKey().getIntValue();
      CheckedItem gem = new CheckedItem(gemId, equipScope, maxPrice, priceLevel, true);
      if (gem.getCount() == 0 || this.evaluator.excludesEquipment(gem)) {
        continue;
      }
      if (this.evaluator.requiresEquipment(gem)) {
        gem.automaticFlag = true;
        gem.requiredFlag = true;
      }

      Modifiers modifiers = CODPIECE.modifiers(gem);
      switch (this.evaluator.checkConstraints(modifiers)) {
        case VIOLATES:
          continue;
        case MEETS:
          gem.automaticFlag = true;
      }

      catalog.add(gem);
      var candidate = this.scoreCandidate(modifiers, gem, nullScore, nullTiebreaker);
      boolean currentlySlotted = KoLCharacter.inCodpiece(gem) && this.evaluator.currentOnly();
      if ((candidate.score() < 0.0 || (candidate.score() == 0.0 && candidate.tiebreaker() <= 0.0))
          && !gem.automaticFlag
          && !(usesFamiliarDependentScore
              && CodpieceModifierSafety.affectsFamiliarCalculation(modifiers))
          && !currentlySlotted) {
        continue;
      }
      if (currentlySlotted) {
        gem.automaticFlag = true;
      }

      ranked.add(gem);
    }

    return new Candidates(List.copyOf(catalog), List.copyOf(ranked));
  }

  boolean prepareAccessoryCandidates(
      List<CheckedItem> gems, List<CheckedItem> accessories, boolean anyGemSlotEnabled) {
    if (gems.isEmpty()) {
      return false;
    }

    boolean codpieceCanExpandAccessoryPool = false;
    for (CheckedItem item : accessories) {
      if (!CODPIECE.isParent(item.getItemId())) {
        continue;
      }
      item.automaticFlag = true;
      codpieceCanExpandAccessoryPool = item.getCount() > 0 && anyGemSlotEnabled;
    }
    if (!codpieceCanExpandAccessoryPool) {
      return false;
    }

    for (CheckedItem item : accessories) {
      if (KoLCharacter.hasEquipped(item)) {
        item.automaticFlag = true;
      }
    }
    return true;
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
