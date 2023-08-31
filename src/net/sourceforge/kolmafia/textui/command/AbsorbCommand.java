package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class AbsorbCommand extends AbstractCommand {
  public AbsorbCommand() {
    this.usage = "[X] <item> - absorb item(s).";
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (!KoLCharacter.inNoobcore()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You are not in a Gelatinous Noob run");
      return;
    }

    if (parameters.length() == 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "No items specified.");
      return;
    }

    if (KoLCharacter.getAbsorbs() >= KoLCharacter.getAbsorbsLimit()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Cannot absorb items at present.");
      return;
    }

    AdventureResult match = ItemFinder.getFirstMatchingItem(parameters, Match.ABSORB);
    if (match == null) {
      // for backwards compatibility: previously no message was displayed, so do not error here
      KoLmafia.updateDisplay("What item is " + parameters + "?");
      return;
    }

    int itemId = match.getItemId();
    int count = match.getCount();

    // If not in inventory, try to retrieve it (if it's in inventory, doesn't matter if outside
    // Standard)
    if (match.getCount(KoLConstants.inventory) < count && !InventoryManager.retrieveItem(match)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Item not accessible.");
      return;
    }

    var request = new GenericRequest("inventory.php?absorb=" + itemId + "&ajax=1", false);
    int previousAbsorbs = KoLCharacter.getAbsorbs();

    // Absorb the item(s)
    for (int i = 0; i < count; i++) {
      RequestThread.postRequest(request);
    }

    // Parse the charpane for updated absorb info
    RequestThread.postRequest(new CharPaneRequest());
    // update "Hatter" daily deed
    if (ItemDatabase.isHat(itemId)) {
      PreferenceListenerRegistry.firePreferenceChanged("(hats)");
    }

    int failedAbsorbs = count - (KoLCharacter.getAbsorbs() - previousAbsorbs);
    count -= failedAbsorbs;

    if (count > 0) {
      KoLmafia.updateDisplay(
          "Absorbed "
              + (count > 1 ? count + " " + ItemDatabase.getPluralName(itemId) : match.getName()));
    }

    if (failedAbsorbs > 0) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          "Failed to absorb "
              + (failedAbsorbs > 1
                  ? failedAbsorbs + " " + ItemDatabase.getPluralName(itemId)
                  : match.getName()));
    }
  }
}
