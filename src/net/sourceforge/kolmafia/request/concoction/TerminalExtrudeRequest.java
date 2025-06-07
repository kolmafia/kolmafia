package net.sourceforge.kolmafia.request.concoction;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.request.TerminalRequest;

public class TerminalExtrudeRequest extends CreateItemRequest {
  public TerminalExtrudeRequest(final Concoction conc) {
    super("choice.php", conc);
  }

  @Override
  public void run() {
    if (!KoLmafia.permitsContinue() || this.getQuantityNeeded() <= 0) {
      return;
    }

    String creation = this.getName();
    String output = null;

    switch (creation) {
      case "hacked gibson" -> output = "extrude -f booze.ext";
      case "browser cookie" -> output = "extrude -f food.ext";
      case "software bug" -> output = "extrude -f familiar.ext";
      case "Source shades" -> output = "extrude -f goggles.ext";
      case "Source terminal CRAM chip" -> output = "extrude -f cram.ext";
      case "Source terminal DRAM chip" -> output = "extrude -f dram.ext";
      case "Source terminal GRAM chip" -> output = "extrude -f gram.ext";
      case "Source terminal PRAM chip" -> output = "extrude -f pram.ext";
      case "Source terminal SPAM chip" -> output = "extrude -f spam.ext";
      case "Source terminal TRAM chip" -> output = "extrude -f tram.ext";
      default -> KoLmafia.updateDisplay(MafiaState.ERROR, "Cannot create " + creation);
    }

    KoLmafia.updateDisplay("Creating " + this.getQuantityNeeded() + " " + creation + "...");

    while (this.getQuantityNeeded() > 0 && KoLmafia.permitsContinue()) {
      this.beforeQuantity = this.createdItem.getCount(KoLConstants.inventory);
      RequestThread.postRequest(new TerminalRequest(output));
      int createdQuantity = this.createdItem.getCount(KoLConstants.inventory) - this.beforeQuantity;

      if (createdQuantity == 0) {
        if (KoLmafia.permitsContinue()) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "Creation failed, no results detected.");
        }

        return;
      }

      KoLmafia.updateDisplay("Successfully created " + creation + " (" + createdQuantity + ")");
      this.quantityNeeded -= createdQuantity;
    }
  }
}
