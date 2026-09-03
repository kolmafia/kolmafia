package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.request.StandardRequest;

final class CarriedFamiliarSelector {
  private static final FamiliarSlotGroup CROWN = FamiliarSlotGroup.CROWN;

  record Selection(
      List<FamiliarData> candidates,
      FamiliarData best,
      FamiliarData secondBest,
      FamiliarData lockedCrown,
      FamiliarData lockedBjorn) {}

  private CarriedFamiliarSelector() {}

  static Selection select(
      int needed,
      boolean crownLocked,
      boolean bjornLocked,
      CharacterSnapshot character,
      EquipScope equipScope,
      long maxPrice,
      PriceLevel priceLevel) {
    FamiliarData best = bjornLocked ? FamiliarData.NO_FAMILIAR : KoLCharacter.getBjorned();
    FamiliarData secondBest = crownLocked ? FamiliarData.NO_FAMILIAR : KoLCharacter.getEnthroned();
    FamiliarData lockedBjorn = bjornLocked ? KoLCharacter.getBjorned() : null;
    FamiliarData lockedCrown = crownLocked ? KoLCharacter.getEnthroned() : null;

    if (best == FamiliarData.NO_FAMILIAR && secondBest != FamiliarData.NO_FAMILIAR) {
      best = secondBest;
      secondBest = FamiliarData.NO_FAMILIAR;
    }
    CheckedItem crown =
        secondBest != FamiliarData.NO_FAMILIAR || needed > 0
            ? new CheckedItem(CROWN.parentItemId(), equipScope, maxPrice, priceLevel)
            : null;
    if (secondBest != FamiliarData.NO_FAMILIAR
        && speculation(secondBest, crown).compareTo(speculation(best, crown)) > 0) {
      FamiliarData swap = best;
      best = secondBest;
      secondBest = swap;
    }

    if (needed == 0) {
      return new Selection(List.of(), best, secondBest, lockedCrown, lockedBjorn);
    }

    MaximizerSpeculation bestSpec = speculation(best, crown);
    MaximizerSpeculation secondBestSpec = speculation(secondBest, crown);
    for (FamiliarData familiar : KoLCharacter.usableFamiliars()) {
      if (!eligible(familiar, best, lockedCrown, lockedBjorn, character)) {
        continue;
      }

      MaximizerSpeculation speculation = speculation(familiar, crown);
      if (speculation.compareTo(bestSpec) > 0) {
        secondBestSpec = bestSpec.clone();
        bestSpec = speculation.clone();
        secondBest = best;
        best = familiar;
      } else if (speculation.compareTo(secondBestSpec) > 0) {
        secondBestSpec = speculation.clone();
        secondBest = familiar;
      }
    }

    List<FamiliarData> candidates = new ArrayList<>(2);
    candidates.add(best);
    if (needed > 1) {
      candidates.add(secondBest);
    }
    return new Selection(List.copyOf(candidates), best, secondBest, lockedCrown, lockedBjorn);
  }

  private static boolean eligible(
      FamiliarData familiar,
      FamiliarData best,
      FamiliarData lockedCrown,
      FamiliarData lockedBjorn,
      CharacterSnapshot character) {
    return familiar != null
        && familiar != FamiliarData.NO_FAMILIAR
        && familiar.canCarry()
        && StandardRequest.isAllowed(RestrictedItemType.FAMILIARS, familiar.getRace())
        && !familiar.equals(KoLCharacter.getFamiliar())
        && !familiar.equals(lockedCrown)
        && !familiar.equals(lockedBjorn)
        && !familiar.equals(best)
        && character.resourceUsage(familiar.getRace()).isZero();
  }

  private static MaximizerSpeculation speculation(FamiliarData familiar, CheckedItem crown) {
    MaximizerSpeculation speculation = new MaximizerSpeculation();
    speculation.attachment = crown;
    speculation.equipment.put(Slot.HAT, crown);
    CROWN.put(speculation, Slot.CROWNOFTHRONES, familiar);
    speculation.setUnscored();
    return speculation;
  }
}
