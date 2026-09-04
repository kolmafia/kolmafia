package net.sourceforge.kolmafia.maximizer;

import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.request.EquipmentRequest;

/**
 * Lazily cached score and comparison state for a {@link MaximizerLoadout}.
 *
 * <p>Equipment or mode mutation must invalidate this object through {@link
 * MaximizerLoadout#setUnscored()}. Primary score, feasibility, shared-resource use, tiebreaker, and
 * simplicity are calculated once and combined into the authoritative {@link SolutionQuality}.
 */
final class LoadoutEvaluation {
  private boolean scored;
  private boolean tiebreakered;
  private boolean failed;
  private boolean exceeded;
  private double score;
  private double tiebreaker;
  private int simplicity;
  private ResourceUsage resourceUsage = ResourceUsage.EMPTY;

  private LoadoutEvaluation() {}

  private LoadoutEvaluation(LoadoutEvaluation source) {
    this.scored = source.scored;
    this.tiebreakered = source.tiebreakered;
    this.failed = source.failed;
    this.exceeded = source.exceeded;
    this.score = source.score;
    this.tiebreaker = source.tiebreaker;
    this.simplicity = source.simplicity;
    this.resourceUsage = source.resourceUsage;
  }

  static LoadoutEvaluation empty() {
    return new LoadoutEvaluation();
  }

  LoadoutEvaluation copy() {
    return new LoadoutEvaluation(this);
  }

  void invalidate() {
    this.scored = false;
    this.tiebreakered = false;
  }

  boolean isScored() {
    return this.scored;
  }

  boolean failed() {
    return this.failed;
  }

  void setFailed(boolean failed) {
    this.failed = failed;
  }

  boolean exceeded() {
    return this.exceeded;
  }

  ResourceUsage resourceUsage() {
    return this.resourceUsage;
  }

  double score(
      Modifiers modifiers, Map<Slot, AdventureResult> equipment, Map<Modeable, String> modeables) {
    if (this.scored) {
      return this.score;
    }

    var character = Maximizer.character();
    this.resourceUsage = character.resourceUsage(equipment);
    var outcome =
        Maximizer.evaluator()
            .evaluateComplete(
                modifiers,
                equipment,
                modeables,
                character.resourcesExceeded(this.resourceUsage),
                character.allowedMutexViolations());
    this.score = outcome.score();
    this.failed = outcome.failed();
    this.exceeded = outcome.exceeded();
    this.scored = true;
    return this.score;
  }

  double tiebreaker(Modifiers modifiers, Map<Slot, AdventureResult> equipment) {
    if (this.tiebreakered) {
      return this.tiebreaker;
    }

    this.tiebreaker = Maximizer.evaluator().getTiebreaker(modifiers);
    this.tiebreakered = true;
    this.simplicity = simplicity(equipment);
    return this.tiebreaker;
  }

  SolutionQuality quality(
      Modifiers modifiers,
      Map<Slot, AdventureResult> equipment,
      Map<Modeable, String> modeables,
      CheckedItem attachment) {
    this.score(modifiers, equipment, modeables);
    this.tiebreaker(modifiers, equipment);

    SolutionQuality.AttachmentQuality attachmentQuality =
        attachment == null
            ? null
            : new SolutionQuality.AttachmentQuality(
                attachment.isBuyable(),
                Maximizer.character().resourceUsage(attachment.getName()),
                attachment.availability().inventory() > 0,
                attachment.availability().initial() > 0);

    return SolutionQuality.from(
        new EvaluationOutcome(this.score, this.failed, this.exceeded),
        this.resourceUsage,
        Maximizer.evaluator().isUsingTiebreaker(),
        this.tiebreaker,
        this.simplicity,
        equipment,
        attachmentQuality);
  }

  private static int simplicity(Map<Slot, AdventureResult> equipment) {
    int simplicity = 0;
    var currentEquipment = Maximizer.character().currentEquipment();
    for (var slot : SlotSet.ALL_SLOTS) {
      AdventureResult item = equipment.get(slot);
      if (item == null) {
        item = EquipmentRequest.UNEQUIP;
      }
      if (currentEquipment.get(slot).equals(item)) {
        simplicity += 2;
      } else if (item.equals(EquipmentRequest.UNEQUIP)) {
        simplicity += slot == Slot.WEAPON ? -1 : 1;
      }
    }
    if (Maximizer.evaluator().isWeaponTypeRequired()) {
      AdventureResult weapon = equipment.get(Slot.WEAPON);
      if (weapon != null && !weapon.equals(EquipmentRequest.UNEQUIP)) {
        simplicity += 3;
      }
    }
    if (Maximizer.evaluator().isShieldRequired()) {
      AdventureResult offhand = equipment.get(Slot.OFFHAND);
      if (offhand != null && !offhand.equals(EquipmentRequest.UNEQUIP)) {
        simplicity += 3;
      }
    }
    return simplicity;
  }
}
