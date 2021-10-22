package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.RabbitHoleManager;

public class HatterCommand extends AbstractCommand {
  public HatterCommand() {
    this.usage =
        " [hat] - List effects you can get by wearing available hats at the hatter's tea party, or get a buff with a hat.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (parameters.length() < 1) {
      RabbitHoleManager.hatCommand();
      return;
    }

    if (!RabbitHoleManager.teaPartyAvailable()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have already attended a Tea Party today.");
      return;
    }

    String hat = parameters;

    try {
      int len = Integer.parseInt(parameters);

      RabbitHoleManager.getHatBuff(len);
    } catch (NumberFormatException e) {
      List<AdventureResult> hats = EquipmentManager.getEquipmentLists()[EquipmentManager.HAT];
      AdventureResult[] matches = ItemFinder.getMatchingItemList(hat, false, hats);

      // TODO: ItemFinder will just return a 0-length array if too many matches.  It would be nice
      // if the "too many matches" error message worked.
      if (matches.length > 1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "[" + hat + "] has too many matches.");
        return;
      }

      if (matches.length == 0) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have a " + hat + " for a hat.");
        return;
      }

      RabbitHoleManager.getHatBuff(AdventureResult.pseudoItem(matches[0].toString()));
    }
  }
}
