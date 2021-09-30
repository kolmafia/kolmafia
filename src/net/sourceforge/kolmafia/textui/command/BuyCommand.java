package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BuyCommand extends AbstractCommand {
  public BuyCommand() {
    this.usage =
        " [using storage] <item> [@ <limit>] [, <another>]... - buy from NPC store or the Mall.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    try (Checkpoint checkpoint = new Checkpoint()) {
      BuyCommand.buy(parameters);
    }
  }

  public static void buy(String parameters) {
    boolean interact = KoLCharacter.canInteract();
    boolean storage = false;
    boolean mall = false;

    String usingStorage = "using storage ";
    if (parameters.startsWith(usingStorage)) {
      storage = true;
      parameters = parameters.substring(usingStorage.length()).trim();
    }

    String fromMall = "from mall ";
    if (parameters.startsWith(fromMall)) {
      mall = true;
      parameters = parameters.substring(fromMall.length()).trim();
    }

    if (interact && storage) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          "You cannot purchase using storage unless you are in Hardcore or Ronin");
      return;
    }

    String[] itemNames = parameters.split("\\s*,\\s*");

    for (String itemName : itemNames) {
      if (itemName.startsWith("0 ")) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR,
            "Purchasing 0 of an item produces surprising results, if deliberate, purchase number in inventory or don't buy!");
        return;
      }

      String[] pieces = itemName.split("@");
      AdventureResult match = ItemFinder.getFirstMatchingItem(pieces[0]);
      if (match == null) {
        return;
      }

      int priceLimit = pieces.length < 2 ? 0 : StringUtilities.parseInt(pieces[1]);

      ArrayList<PurchaseRequest> results =
          // Cheapest from Mall or NPC stores
          (interact && !mall)
              ? StoreManager.searchMall(match)
              :
              // Mall stores only
              (storage || mall)
                  ? StoreManager.searchOnlyMall(match)
                  :
                  // NPC stores only
                  StoreManager.searchNPCs(match);

      KoLmafia.makePurchases(
          results, results.toArray(new PurchaseRequest[0]), match.getCount(), false, priceLimit);

      if (interact && !storage) {
        StoreManager.updateMallPrice(match, results);
      }
    }
  }
}
