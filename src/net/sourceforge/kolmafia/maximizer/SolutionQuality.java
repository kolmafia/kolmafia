package net.sourceforge.kolmafia.maximizer;

import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;

/**
 * Authoritative total ordering for complete maximizer solutions.
 *
 * <p>The field and comparison order is user-visible behavior: feasibility and requested score win
 * first, followed by lower shared-resource use and progressively weaker tiebreakers. Search,
 * candidate ranking, and the brute-force oracle must not invent a different ordering. {@link
 * MaximizerLoadout#compareTo(MaximizerLoadout)} duplicates its first inexpensive comparisons as a
 * fast path before constructing the complete quality.
 */
record SolutionQuality(
    boolean feasible,
    double score,
    ResourceUsage resourceUsage,
    int itemDroppers,
    int meatDroppers,
    double tiebreaker,
    int rolloverEffects,
    int breakables,
    int simplicity,
    AttachmentQuality attachment)
    implements Comparable<SolutionQuality> {

  record AttachmentQuality(
      boolean buyable, ResourceUsage resourceUsage, boolean inInventory, boolean initial) {}

  static SolutionQuality from(
      EvaluationOutcome outcome,
      ResourceUsage resourceUsage,
      boolean useTiebreaker,
      double tiebreaker,
      int simplicity,
      Map<Slot, AdventureResult> equipment,
      AttachmentQuality attachment) {
    int rolloverEffects = 0;
    int breakables = 0;
    int itemDroppers = 0;
    int meatDroppers = 0;
    for (var equip : equipment.values()) {
      if (equip == null) continue;
      Modifiers modifiers = ModifierDatabase.getItemModifiers(equip.getItemId());
      if (modifiers == null) continue;
      if (modifiers.hasString(StringModifier.ROLLOVER_EFFECT)) rolloverEffects++;
      if (modifiers.getBoolean(BooleanModifier.BREAKABLE)) breakables++;
      if (modifiers.getBoolean(BooleanModifier.DROPS_ITEMS)) itemDroppers++;
      if (modifiers.getBoolean(BooleanModifier.DROPS_MEAT)) meatDroppers++;
    }

    return new SolutionQuality(
        !outcome.failed(),
        outcome.score(),
        resourceUsage,
        useTiebreaker ? itemDroppers : 0,
        useTiebreaker ? meatDroppers : 0,
        tiebreaker,
        useTiebreaker ? rolloverEffects : 0,
        breakables,
        simplicity,
        attachment);
  }

  @Override
  public int compareTo(SolutionQuality other) {
    int comparison = Boolean.compare(this.feasible, other.feasible);
    if (comparison != 0) return comparison;

    comparison = Double.compare(this.score, other.score);
    if (comparison != 0) return comparison;

    comparison = this.resourceUsage.compareTo(other.resourceUsage);
    if (comparison != 0) return comparison;

    comparison = Integer.compare(this.itemDroppers, other.itemDroppers);
    if (comparison != 0) return comparison;

    comparison = Integer.compare(this.meatDroppers, other.meatDroppers);
    if (comparison != 0) return comparison;

    comparison = Double.compare(this.tiebreaker, other.tiebreaker);
    if (comparison != 0) return comparison;

    comparison = Integer.compare(this.rolloverEffects, other.rolloverEffects);
    if (comparison != 0) return comparison;

    comparison = Integer.compare(other.breakables, this.breakables);
    if (comparison != 0) return comparison;

    comparison = Integer.compare(this.simplicity, other.simplicity);
    if (comparison != 0) return comparison;

    if (this.attachment == null || other.attachment == null) return 0;

    comparison = Boolean.compare(other.attachment.buyable, this.attachment.buyable);
    if (comparison != 0) return comparison;

    comparison = Boolean.compare(this.attachment.inInventory, other.attachment.inInventory);
    if (comparison != 0) return comparison;

    comparison = Boolean.compare(this.attachment.initial, other.attachment.initial);
    if (comparison != 0) return comparison;

    return this.attachment.resourceUsage.compareTo(other.attachment.resourceUsage);
  }
}
