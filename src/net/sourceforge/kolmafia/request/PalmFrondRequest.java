package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public class PalmFrondRequest extends MultiUseRequest {
  public static final AdventureResult MANUAL = ItemPool.get(ItemPool.WEAVING_MANUAL);

  public PalmFrondRequest(final int itemId) {
    super(itemId);
  }

  @Override
  public void run() {
    // Make sure you have a weaving manual

    if (!KoLConstants.inventory.contains(PalmFrondRequest.MANUAL)) {
      // You can currently weave even if you don't have the
      // manual. I've reported a bug, so that may change...
    }

    super.run();
  }

  @Override
  public void processResults() {
    // "You can't figure out what to do with this thing. Maybe you
    //  should mess with more than one of them at a time."

    // "You can't weave anything out of that quantity of palm
    //  fronds."

    if (this.responseText.indexOf("You can't") != -1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You can't make that item.");
      return;
    }
  }
}
