package net.sourceforge.kolmafia.maximizer;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.Speculation;
import net.sourceforge.kolmafia.equipment.Slot;

public class MaximizerLoadout extends Speculation
    implements Comparable<MaximizerLoadout>, Cloneable {
  private LoadoutEvaluation evaluation = LoadoutEvaluation.empty();
  public CheckedItem attachment;
  CodpieceSearchState codpiece = new CodpieceSearchState(this);

  @Override
  public MaximizerLoadout clone() {
    try {
      MaximizerLoadout copy = (MaximizerLoadout) super.clone();
      copy.equipment = this.equipment.clone();
      copy.setModeables(new EnumMap<>(this.getModeables()));
      if (this.mods != null) {
        copy.mods = new Modifiers(this.mods);
      }
      copy.evaluation = this.evaluation.copy();
      // A clone is a frozen candidate: it must not retain an in-progress Codpiece search.
      copy.codpiece = new CodpieceSearchState(copy);
      return copy;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  @Override
  public Modifiers calculate() {
    var result = this.codpiece.calculateModifiers();
    if (result == null) {
      return super.calculate();
    }
    this.mods =
        KoLCharacter.applyAdjustmentSuffix(
            false, result.modifiers(), result.fightMods(), this.equipment, this.getEffects(), true);
    this.calculated = true;
    return this.mods;
  }

  @Override
  public String toString() {
    if (this.attachment != null) {
      return this.attachment.getInstance((int) this.getScore()).toString();
    }
    return super.toString();
  }

  public void setUnscored() {
    this.evaluation.invalidate();
    this.calculated = false;
  }

  public double getScore() {
    if (!this.calculated) {
      Maximizer.recordScoreCalculation();
      this.calculate();
    }
    return this.evaluation.score(this.mods, this.equipment, this.getModeables());
  }

  public double getTiebreaker() {
    if (!this.calculated) this.calculate();
    return this.evaluation.tiebreaker(this.mods, this.equipment);
  }

  @Override
  public int compareTo(MaximizerLoadout o) {
    if (o == null) return 1;

    int comparison = Double.compare(this.getScore(), o.getScore());
    if (this.failed() != o.failed()) return this.failed() ? -1 : 1;
    if (comparison != 0) return comparison;

    comparison = this.evaluation.resourceUsage().compareTo(o.evaluation.resourceUsage());
    if (comparison != 0) return comparison;

    return this.quality().compareTo(o.quality());
  }

  SolutionQuality quality() {
    this.getScore();
    this.getTiebreaker();
    return this.evaluation.quality(this.mods, this.equipment, this.getModeables(), this.attachment);
  }

  boolean isScored() {
    return this.evaluation.isScored();
  }

  public boolean failed() {
    return this.evaluation.failed();
  }

  boolean exceeded() {
    return this.evaluation.exceeded();
  }

  void markFailed() {
    this.evaluation.setFailed(true);
  }

  void clearFailure() {
    this.evaluation.setFailed(false);
  }

  // Remember which equipment slots were null, so that this
  // state can be restored later.
  EnumMap<Slot, AdventureResult> mark() {
    return this.equipment.clone();
  }

  void restore(EnumMap<Slot, AdventureResult> mark) {
    this.equipment.putAll(mark);
  }

  /** Used by {@link CodpieceSearchState} to build the late-adjustment prefix it caches. */
  KoLCharacter.AdjustmentPrefix recalculateCodpiecePrefix(Map<Slot, AdventureResult> equipment) {
    return KoLCharacter.recalculateAdjustmentsPrefix(
        false,
        this.getMindControlLevel(),
        equipment,
        this.getEffects(),
        this.getFamiliar(),
        this.getEnthroned(),
        this.getBjorned(),
        this.getCustom(),
        this.getHorsery(),
        this.getBoomBox(),
        this.getModeables(),
        true);
  }

  /** Applies each candidate to its own clone of baseline and returns the best-scoring one. */
  public static <T> MaximizerLoadout bestOf(
      MaximizerLoadout baseline, Iterable<T> candidates, BiConsumer<MaximizerLoadout, T> mutator) {
    MaximizerLoadout best = baseline;
    for (T candidate : candidates) {
      MaximizerLoadout loadout = baseline.clone();
      mutator.accept(loadout, candidate);
      loadout.setUnscored(); // clone() may carry baseline's cached score
      if (loadout.compareTo(best) > 0) {
        best = loadout;
      }
    }
    return best;
  }
}
