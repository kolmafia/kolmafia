package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.session.EquipmentManager;

final class CardSleeveSelector {
  private static final int FIRST_CARD = 4967;
  private static final int LAST_CARD = 5007;

  private CardSleeveSelector() {}

  static CheckedItem select(
      boolean needed, EquipScope equipScope, long maxPrice, PriceLevel priceLevel) {
    if (!needed) {
      return null;
    }

    AdventureResult equippedCard = EquipmentManager.getEquipment(Slot.CARDSLEEVE);
    List<CheckedItem> candidates = new ArrayList<>();
    for (int itemId = FIRST_CARD; itemId <= LAST_CARD; itemId++) {
      CheckedItem card = new CheckedItem(itemId, equipScope, maxPrice, priceLevel);
      if (card.getCount() > 0 || (equippedCard != null && itemId == equippedCard.getItemId())) {
        candidates.add(card);
      }
    }

    MaximizerLoadout baseline = new MaximizerLoadout();
    var group = ItemSlotGroup.CARD_SLEEVE;
    CheckedItem sleeve = new CheckedItem(group.parentItemId(), equipScope, maxPrice, priceLevel);
    MaximizerLoadout best =
        MaximizerLoadout.bestOf(
            baseline,
            candidates,
            (loadout, card) -> {
              loadout.attachment = sleeve;
              loadout.equipment.put(Slot.OFFHAND, sleeve);
              group.put(loadout, Slot.CARDSLEEVE, card);
            });
    return best == baseline ? null : (CheckedItem) group.get(best, Slot.CARDSLEEVE);
  }
}
